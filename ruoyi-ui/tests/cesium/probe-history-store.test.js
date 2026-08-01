const assert = require('assert')
const store = require('../../src/utils/scene/probeHistoryStore')

function mem() {
  return {
    _d: {},
    getItem(k) {
      return Object.prototype.hasOwnProperty.call(this._d, k) ? this._d[k] : null
    },
    setItem(k, v) {
      this._d[k] = String(v)
    }
  }
}

function run() {
  const m = mem()
  const t0 = Date.now()
  // Use past timestamps so default to=now does not exclude them.
  const a = store.appendProbeEvent({ deviceId: 'dev-a', type: 'online', at: t0 - 3000 }, m)
  assert.ok(a && a.id)
  assert.strictEqual(a.type, 'online')
  store.appendProbeEvent({ deviceId: 'dev-a', type: 'offline', at: t0 - 2000 }, m)
  store.appendProbeEvent({ deviceId: 'dev-b', type: 'online', at: t0 - 1000 }, m)

  const all = store.listProbeEvents({}, m)
  assert.strictEqual(all.length, 3)
  assert.ok(all[0].at >= all[1].at)

  const onlyA = store.listProbeEvents({ deviceId: 'dev-a' }, m)
  assert.strictEqual(onlyA.length, 2)
  assert.ok(onlyA.every(e => e.deviceId === 'dev-a'))

  // Stale event outside 30d is pruned from list
  const oldAt = t0 - store.RETENTION_MS - 1000
  store.appendProbeEvent({ deviceId: 'dev-a', type: 'offline', at: oldAt }, m)
  const afterPrune = store.listProbeEvents({ deviceId: 'dev-a' }, m)
  assert.ok(afterPrune.every(e => e.at >= t0 - store.RETENTION_MS))

  store.removeDeviceHistory('dev-a', m)
  assert.strictEqual(store.listProbeEvents({ deviceId: 'dev-a' }, m).length, 0)
  assert.ok(store.listProbeEvents({}, m).some(e => e.deviceId === 'dev-b'))

  // MAX_EVENTS prune keeps newest
  const m2 = mem()
  const base = Date.now() - store.MAX_EVENTS - 50
  for (let i = 0; i < store.MAX_EVENTS + 20; i++) {
    store.appendProbeEvent(
      { deviceId: 'dev-x', type: i % 2 === 0 ? 'online' : 'offline', at: base + i },
      m2
    )
  }
  const capped = store.listProbeEvents({}, m2)
  assert.ok(capped.length <= store.MAX_EVENTS)
  assert.strictEqual(capped[0].at, base + store.MAX_EVENTS + 19)

  assert.strictEqual(store.appendProbeEvent({ deviceId: 'x', type: 'bad' }, mem()), null)
  console.log('probe-history-store.test.js OK')
}

run()
