import fs from 'node:fs';
import path from 'node:path';
import { DatabaseSync } from 'node:sqlite';

export class GatewayStore {
  constructor(databasePath = ':memory:') {
    if (databasePath !== ':memory:') fs.mkdirSync(path.dirname(databasePath), { recursive: true });
    this.db = new DatabaseSync(databasePath);
    this.db.exec('PRAGMA journal_mode = WAL; PRAGMA synchronous = NORMAL; PRAGMA foreign_keys = ON;');
    this.#migrate();
  }

  close() {
    this.db.close();
  }

  seedDemoDevices() {
    const count = this.db.prepare('SELECT COUNT(*) AS count FROM devices').get().count;
    if (count > 0) return;

    const gb = 1024 * 1024 * 1024;
    const insert = this.db.prepare(`
      INSERT INTO devices (
        id, name, owner, kind, ip_address, mac_address, quota_bytes,
        used_bytes, download_bytes, upload_bytes, online, last_seen_at
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?)
    `);
    const now = new Date().toISOString();
    this.#transaction(() => {
      insert.run('phone-mariam', 'هاتف مريم', 'مريم', 'phone', '192.168.50.21', '8A:21:4F:10:31:90', 5 * gb, 3 * gb, 3 * gb - 90 * 1024 * 1024, 90 * 1024 * 1024, now);
      insert.run('tv-living', 'تلفزيون الصالة', 'المنزل', 'tv', '192.168.50.30', '72:9C:00:25:AF:11', 8 * gb, 2 * gb, 2 * gb - 20 * 1024 * 1024, 20 * 1024 * 1024, now);
      insert.run('laptop-omar', 'لابتوب عمر', 'عمر', 'laptop', '192.168.50.42', '3E:B2:18:4C:71:A0', 3 * gb, 512 * 1024 * 1024, 480 * 1024 * 1024, 32 * 1024 * 1024, now);
    });
  }

  listDevices() {
    return this.db.prepare('SELECT * FROM devices ORDER BY online DESC, name COLLATE NOCASE').all().map(toDevice);
  }

  getDevice(id) {
    const row = this.db.prepare('SELECT * FROM devices WHERE id = ?').get(id);
    return row ? toDevice(row) : null;
  }

  applyUsage(id, downloadDelta, uploadDelta, speeds = {}) {
    const safeDownload = nonNegativeInteger(downloadDelta);
    const safeUpload = nonNegativeInteger(uploadDelta);
    const now = new Date().toISOString();
    const result = this.db.prepare(`
      UPDATE devices
      SET used_bytes = used_bytes + ?,
          download_bytes = download_bytes + ?,
          upload_bytes = upload_bytes + ?,
          download_bps = ?,
          upload_bps = ?,
          online = 1,
          last_seen_at = ?,
          quota_blocked = CASE WHEN quota_bytes > 0 AND used_bytes + ? + ? >= quota_bytes THEN 1 ELSE quota_blocked END
      WHERE id = ?
    `).run(
      safeDownload + safeUpload,
      safeDownload,
      safeUpload,
      nonNegativeInteger(speeds.downloadBps),
      nonNegativeInteger(speeds.uploadBps),
      now,
      safeDownload,
      safeUpload,
      id
    );
    if (result.changes === 0) throw new NotFoundError(`Unknown device: ${id}`);
    return this.getDevice(id);
  }

  reconcileCounters(id, sessionId, downloadCounter, uploadCounter) {
    const download = nonNegativeInteger(downloadCounter);
    const upload = nonNegativeInteger(uploadCounter);
    const previous = this.db.prepare('SELECT * FROM counter_state WHERE device_id = ?').get(id);
    let downloadDelta = 0;
    let uploadDelta = 0;

    if (previous && previous.session_id === sessionId) {
      downloadDelta = download >= previous.download_counter ? download - previous.download_counter : 0;
      uploadDelta = upload >= previous.upload_counter ? upload - previous.upload_counter : 0;
    }

    let device;
    this.#transaction(() => {
      this.db.prepare(`
        INSERT INTO counter_state (device_id, session_id, download_counter, upload_counter, updated_at)
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT(device_id) DO UPDATE SET
          session_id = excluded.session_id,
          download_counter = excluded.download_counter,
          upload_counter = excluded.upload_counter,
          updated_at = excluded.updated_at
      `).run(id, sessionId, download, upload, new Date().toISOString());
      device = this.applyUsage(id, downloadDelta, uploadDelta);
    });
    return { device, downloadDelta, uploadDelta };
  }

  setManualPaused(id, paused, actor = 'admin') {
    this.#requireDevice(id);
    this.db.prepare('UPDATE devices SET manual_paused = ? WHERE id = ?').run(paused ? 1 : 0, id);
    this.addAudit(actor, id, paused ? 'pause' : 'resume', null);
    return this.getDevice(id);
  }

  addBonus(id, bytes, actor = 'admin') {
    this.#requireDevice(id);
    const safeBytes = positiveInteger(bytes, 'Bonus bytes');
    this.db.prepare(`
      UPDATE devices
      SET quota_bytes = quota_bytes + ?,
          quota_blocked = CASE WHEN used_bytes < quota_bytes + ? THEN 0 ELSE quota_blocked END
      WHERE id = ?
    `).run(safeBytes, safeBytes, id);
    this.addAudit(actor, id, 'bonus', JSON.stringify({ bytes: safeBytes }));
    return this.getDevice(id);
  }

  setQuota(id, bytes, actor = 'admin') {
    this.#requireDevice(id);
    const safeBytes = positiveInteger(bytes, 'Quota bytes');
    this.db.prepare(`
      UPDATE devices
      SET quota_bytes = ?, quota_blocked = CASE WHEN used_bytes >= ? THEN 1 ELSE 0 END
      WHERE id = ?
    `).run(safeBytes, safeBytes, id);
    this.addAudit(actor, id, 'set_quota', JSON.stringify({ bytes: safeBytes }));
    return this.getDevice(id);
  }

  resetForDate(localDate) {
    const previousDate = this.getMeta('usage_date');
    if (previousDate === localDate) return false;

    this.#transaction(() => {
      this.db.prepare(`
        UPDATE devices
        SET used_bytes = 0, download_bytes = 0, upload_bytes = 0,
            download_bps = 0, upload_bps = 0, quota_blocked = 0
      `).run();
      this.db.prepare('DELETE FROM counter_state').run();
      this.setMeta('usage_date', localDate);
      this.addAudit('system', null, 'daily_reset', JSON.stringify({ localDate }));
    });
    return true;
  }

  initializeUsageDate(localDate) {
    if (!this.getMeta('usage_date')) this.setMeta('usage_date', localDate);
  }

  getMeta(key) {
    return this.db.prepare('SELECT value FROM metadata WHERE key = ?').get(key)?.value ?? null;
  }

  setMeta(key, value) {
    this.db.prepare(`
      INSERT INTO metadata (key, value) VALUES (?, ?)
      ON CONFLICT(key) DO UPDATE SET value = excluded.value
    `).run(key, String(value));
  }

  addAudit(actor, deviceId, action, detail) {
    this.db.prepare(`
      INSERT INTO audit_events (actor, device_id, action, detail, created_at)
      VALUES (?, ?, ?, ?, ?)
    `).run(actor, deviceId, action, detail, new Date().toISOString());
  }

  listAudit(limit = 100) {
    return this.db.prepare('SELECT * FROM audit_events ORDER BY id DESC LIMIT ?').all(Math.min(Math.max(limit, 1), 500));
  }

  #requireDevice(id) {
    const device = this.getDevice(id);
    if (!device) throw new NotFoundError(`Unknown device: ${id}`);
    return device;
  }

  #transaction(operation) {
    this.db.exec('BEGIN IMMEDIATE');
    try {
      const result = operation();
      this.db.exec('COMMIT');
      return result;
    } catch (error) {
      this.db.exec('ROLLBACK');
      throw error;
    }
  }

  #migrate() {
    this.db.exec(`
      CREATE TABLE IF NOT EXISTS devices (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        owner TEXT NOT NULL DEFAULT 'غير محدد',
        kind TEXT NOT NULL DEFAULT 'device',
        ip_address TEXT NOT NULL UNIQUE,
        mac_address TEXT NOT NULL UNIQUE,
        quota_bytes INTEGER NOT NULL CHECK (quota_bytes > 0),
        used_bytes INTEGER NOT NULL DEFAULT 0 CHECK (used_bytes >= 0),
        download_bytes INTEGER NOT NULL DEFAULT 0 CHECK (download_bytes >= 0),
        upload_bytes INTEGER NOT NULL DEFAULT 0 CHECK (upload_bytes >= 0),
        download_bps INTEGER NOT NULL DEFAULT 0,
        upload_bps INTEGER NOT NULL DEFAULT 0,
        manual_paused INTEGER NOT NULL DEFAULT 0,
        quota_blocked INTEGER NOT NULL DEFAULT 0,
        online INTEGER NOT NULL DEFAULT 0,
        reset_at TEXT NOT NULL DEFAULT '00:00',
        last_seen_at TEXT
      );

      CREATE TABLE IF NOT EXISTS counter_state (
        device_id TEXT PRIMARY KEY REFERENCES devices(id) ON DELETE CASCADE,
        session_id TEXT NOT NULL,
        download_counter INTEGER NOT NULL,
        upload_counter INTEGER NOT NULL,
        updated_at TEXT NOT NULL
      );

      CREATE TABLE IF NOT EXISTS metadata (
        key TEXT PRIMARY KEY,
        value TEXT NOT NULL
      );

      CREATE TABLE IF NOT EXISTS audit_events (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        actor TEXT NOT NULL,
        device_id TEXT,
        action TEXT NOT NULL,
        detail TEXT,
        created_at TEXT NOT NULL
      );
    `);
  }
}

export class NotFoundError extends Error {}

function toDevice(row) {
  const quotaBytes = Number(row.quota_bytes);
  const usedBytes = Number(row.used_bytes);
  return {
    id: row.id,
    name: row.name,
    owner: row.owner,
    kind: row.kind,
    ipAddress: row.ip_address,
    macAddress: row.mac_address,
    quotaBytes,
    usedBytes,
    remainingBytes: Math.max(quotaBytes - usedBytes, 0),
    exhausted: quotaBytes > 0 && usedBytes >= quotaBytes,
    downloadBytes: Number(row.download_bytes),
    uploadBytes: Number(row.upload_bytes),
    downloadBps: Number(row.download_bps),
    uploadBps: Number(row.upload_bps),
    paused: Boolean(row.manual_paused || row.quota_blocked),
    manualPaused: Boolean(row.manual_paused),
    quotaBlocked: Boolean(row.quota_blocked),
    online: Boolean(row.online),
    resetAt: row.reset_at,
    lastSeenAt: row.last_seen_at
  };
}

function nonNegativeInteger(value) {
  const number = Number(value ?? 0);
  return Number.isFinite(number) && number > 0 ? Math.floor(number) : 0;
}

function positiveInteger(value, label) {
  const number = Number(value);
  if (!Number.isSafeInteger(number) || number <= 0) throw new RangeError(`${label} must be a positive integer`);
  return number;
}
