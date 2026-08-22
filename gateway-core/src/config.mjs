import path from 'node:path';

function readPositiveInteger(value, fallback) {
  const parsed = Number.parseInt(value ?? '', 10);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
}

export function loadConfig(env = process.env) {
  const mode = env.NETQUOTA_MODE === 'nftables' ? 'nftables' : 'simulator';
  const adminToken = env.NETQUOTA_ADMIN_TOKEN || 'netquota-demo';

  if (mode === 'nftables' && adminToken === 'netquota-demo') {
    throw new Error('NETQUOTA_ADMIN_TOKEN must be changed before nftables mode can start');
  }

  return {
    mode,
    host: env.NETQUOTA_HOST || '0.0.0.0',
    port: readPositiveInteger(env.NETQUOTA_PORT, 8787),
    databasePath: env.NETQUOTA_DATABASE || path.resolve('data', 'netquota.db'),
    adminToken,
    timeZone: env.NETQUOTA_TIME_ZONE || 'Africa/Cairo',
    tickMilliseconds: readPositiveInteger(env.NETQUOTA_TICK_MS, 2_000)
  };
}
