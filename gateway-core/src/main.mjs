import { createApiServer } from './api.mjs';
import { loadConfig } from './config.mjs';
import { NftablesEnforcer } from './enforcement/nftables.mjs';
import { SimulatorEnforcer, TrafficSimulator } from './enforcement/simulator.mjs';
import { GatewayStore } from './store.mjs';
import { DailyResetScheduler } from './time.mjs';

const config = loadConfig();
const store = new GatewayStore(config.databasePath);
const enforcer = config.mode === 'nftables' ? new NftablesEnforcer() : new SimulatorEnforcer();

if (config.mode === 'simulator') store.seedDemoDevices();
if (typeof enforcer.initialize === 'function') await enforcer.initialize();

const resetScheduler = new DailyResetScheduler(store, config.timeZone);
resetScheduler.initialize();

const server = createApiServer({ store, enforcer, config });
server.listen(config.port, config.host, () => {
  console.log(`NetQuota Gateway Core listening on http://${config.host}:${config.port}`);
  console.log(`Mode: ${config.mode}; time zone: ${config.timeZone}`);
  if (config.mode === 'simulator') console.log(`Demo admin token: ${config.adminToken}`);
});

const timers = [setInterval(() => resetScheduler.check(), 30_000)];
if (config.mode === 'simulator') {
  const simulator = new TrafficSimulator(store, enforcer);
  timers.push(setInterval(() => simulator.tick().catch(console.error), config.tickMilliseconds));
}

function shutdown(signal) {
  console.log(`Received ${signal}; saving state and stopping`);
  timers.forEach(clearInterval);
  server.close(() => {
    store.close();
    process.exit(0);
  });
  setTimeout(() => process.exit(1), 5_000).unref();
}

process.on('SIGINT', () => shutdown('SIGINT'));
process.on('SIGTERM', () => shutdown('SIGTERM'));
