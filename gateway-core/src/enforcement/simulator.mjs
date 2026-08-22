export class SimulatorEnforcer {
  constructor() {
    this.blocked = new Set();
  }

  async setBlocked(ipAddress, blocked) {
    if (blocked) this.blocked.add(ipAddress);
    else this.blocked.delete(ipAddress);
  }

  isBlocked(ipAddress) {
    return this.blocked.has(ipAddress);
  }
}

export class TrafficSimulator {
  constructor(store, enforcer) {
    this.store = store;
    this.enforcer = enforcer;
  }

  async tick() {
    for (const device of this.store.listDevices()) {
      if (!device.online || device.paused) {
        await this.enforcer.setBlocked(device.ipAddress, device.paused);
        continue;
      }
      const download = randomInteger(90_000, 1_900_000);
      const upload = randomInteger(8_000, 140_000);
      const updated = this.store.applyUsage(device.id, download, upload, {
        downloadBps: download,
        uploadBps: upload
      });
      await this.enforcer.setBlocked(updated.ipAddress, updated.paused);
    }
  }
}

function randomInteger(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}
