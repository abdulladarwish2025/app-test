import assert from 'node:assert/strict';
import test from 'node:test';
import { createApiServer } from '../src/api.mjs';
import { SimulatorEnforcer } from '../src/enforcement/simulator.mjs';
import { GatewayStore } from '../src/store.mjs';

test('API requires a token and applies pause commands', async (t) => {
  const store = new GatewayStore(':memory:');
  store.seedDemoDevices();
  const enforcer = new SimulatorEnforcer();
  const config = { adminToken: 'secret', mode: 'simulator' };
  const server = createApiServer({ store, enforcer, config });
  server.listen(0, '127.0.0.1');
  await new Promise((resolve) => server.once('listening', resolve));
  t.after(() => {
    server.close();
    store.close();
  });
  const { port } = server.address();

  const unauthorized = await fetch(`http://127.0.0.1:${port}/api/v1/devices`);
  assert.equal(unauthorized.status, 401);

  const paused = await fetch(`http://127.0.0.1:${port}/api/v1/devices/phone-mariam/pause`, {
    method: 'POST',
    headers: { Authorization: 'Bearer secret', 'Content-Type': 'application/json' },
    body: '{}'
  });
  assert.equal(paused.status, 200);
  const device = await paused.json();
  assert.equal(device.paused, true);
  assert.equal(enforcer.isBlocked(device.ipAddress), true);
});
