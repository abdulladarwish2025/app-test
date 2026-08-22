import assert from 'node:assert/strict';
import test from 'node:test';
import { GatewayStore } from '../src/store.mjs';

const GB = 1024 * 1024 * 1024;

function createStore() {
  const store = new GatewayStore(':memory:');
  store.seedDemoDevices();
  return store;
}

test('quota exhaustion blocks a device and bonus opens it again', (t) => {
  const store = createStore();
  t.after(() => store.close());
  const original = store.getDevice('laptop-omar');

  const blocked = store.applyUsage('laptop-omar', original.remainingBytes + 1, 0);
  assert.equal(blocked.quotaBlocked, true);
  assert.equal(blocked.paused, true);

  const reopened = store.addBonus('laptop-omar', GB);
  assert.equal(reopened.quotaBlocked, false);
  assert.equal(reopened.paused, false);
});

test('counter reconciliation counts only monotonic deltas in the same boot session', (t) => {
  const store = createStore();
  t.after(() => store.close());
  const before = store.getDevice('phone-mariam').usedBytes;

  const baseline = store.reconcileCounters('phone-mariam', 'boot-a', 1_000, 500);
  assert.equal(baseline.downloadDelta, 0);
  assert.equal(baseline.uploadDelta, 0);

  const delta = store.reconcileCounters('phone-mariam', 'boot-a', 1_600, 750);
  assert.equal(delta.downloadDelta, 600);
  assert.equal(delta.uploadDelta, 250);
  assert.equal(store.getDevice('phone-mariam').usedBytes, before + 850);

  const newBoot = store.reconcileCounters('phone-mariam', 'boot-b', 20, 10);
  assert.equal(newBoot.downloadDelta, 0);
  assert.equal(newBoot.uploadDelta, 0);
});

test('daily reset is idempotent for the same local date', (t) => {
  const store = createStore();
  t.after(() => store.close());
  store.initializeUsageDate('2026-08-22');

  assert.equal(store.resetForDate('2026-08-22'), false);
  assert.equal(store.resetForDate('2026-08-23'), true);
  assert.equal(store.getDevice('phone-mariam').usedBytes, 0);
  assert.equal(store.resetForDate('2026-08-23'), false);
});
