const assert = require('assert')
const fs = require('fs')
const path = require('path')

const deviceStore = require('../../src/utils/scene/deviceStore')
const probeStore = require('../../src/utils/scene/probeStore')

function makeMem() {
  return {
    _d: {},
    getItem(k) { return this._d[k] ?? null },
    setItem(k, v) { this._d[k] = String(v) }
  }
}

/**
 * Store-level smoke mirroring probe.js / delDevice hook.
 * probe.js uses webpack `@/` imports and cannot be required in Node
 * (same constraint as device.js / device-api.test.js).
 */
async function run() {
  const mem = makeMem()
  const devices = deviceStore.loadDevices(mem)
  assert.ok(devices.length > 0)

  // startAll / stopAll via setMonitoring over loadDevices
  devices.forEach(d => probeStore.setMonitoring(d.id, true, mem))
  let probes = devices.map(d => probeStore.getDeviceProbe(d.id, mem))
  assert.ok(probes.every(p => p.monitoring === true && p.status === 'online'))

  devices.forEach(d => probeStore.setMonitoring(d.id, false, mem))
  probes = devices.map(d => probeStore.getDeviceProbe(d.id, mem))
  assert.ok(probes.every(p => p.monitoring === false && p.status === 'unknown'))

  // listProbeStatus filter by buildingId
  const target = devices.find(d => d.buildingId)
  probeStore.setMonitoring(target.id, true, mem)
  const filtered = devices
    .filter(d => d.buildingId === target.buildingId)
    .map(d => probeStore.getDeviceProbe(d.id, mem))
  assert.ok(filtered.some(p => p.deviceId === target.id && p.monitoring === true))
  assert.ok(filtered.every(p => {
    const dev = devices.find(d => d.id === p.deviceId)
    return dev && dev.buildingId === target.buildingId
  }))

  // mute helpers
  assert.strictEqual(probeStore.getAlertMuted(mem), false)
  probeStore.setAlertMuted(true, mem)
  assert.strictEqual(probeStore.getAlertMuted(mem), true)

  // delete hook: removeDevice then removeDeviceProbe
  const doomed = { ...target, id: 'dev-probe-del-' + Date.now(), ip: '192.168.99.99', name: 'probe-del' }
  assert.strictEqual(deviceStore.upsertDevice(doomed, mem).ok, true)
  probeStore.setMonitoring(doomed.id, true, mem)
  assert.strictEqual(probeStore.getDeviceProbe(doomed.id, mem).monitoring, true)
  assert.strictEqual(deviceStore.removeDevice(doomed.id, mem).ok, true)
  probeStore.removeDeviceProbe(doomed.id, mem)
  const after = probeStore.getDeviceProbe(doomed.id, mem)
  assert.strictEqual(after.monitoring, false)
  assert.strictEqual(after.status, 'unknown')

  // source-level: device API uses backend request wiring
  const deviceApiSrc = fs.readFileSync(
    path.join(__dirname, '../../src/api/scene/device.js'),
    'utf8'
  )
  assert.ok(deviceApiSrc.includes("import request from '@/utils/request'"))
  assert.ok(deviceApiSrc.includes("url: '/scene/device/list'"))
  assert.ok(deviceApiSrc.includes("url: '/scene/device/' + id"))

  // probe.js exports present
  const probeApiSrc = fs.readFileSync(
    path.join(__dirname, '../../src/api/scene/probe.js'),
    'utf8'
  )
  ;[
    'startAllMonitoring',
    'stopAllMonitoring',
    'startDeviceMonitoring',
    'stopDeviceMonitoring',
    'listProbeStatus',
    'getAlertMuted',
    'setAlertMuted'
  ].forEach(name => {
    assert.ok(probeApiSrc.includes('export function ' + name), 'missing export ' + name)
  })

  console.log('probe-api.test.js PASS')
}

run().catch(err => {
  console.error(err)
  process.exit(1)
})
