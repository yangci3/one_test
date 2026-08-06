const assert = require('assert')
const fs = require('fs')
const path = require('path')

const probeStore = require('../../src/utils/scene/probeStore')

function makeMem() {
  return {
    _d: {},
    getItem(k) { return this._d[k] ?? null },
    setItem(k, v) { this._d[k] = String(v) }
  }
}

/**
 * probe.js uses webpack `@/` imports and cannot be required in Node;
 * verify exports and backend URL wiring from source.
 */
async function run() {
  const mem = makeMem()

  // mute helpers still use probeStore localStorage
  assert.strictEqual(probeStore.getAlertMuted(mem), false)
  probeStore.setAlertMuted(true, mem)
  assert.strictEqual(probeStore.getAlertMuted(mem), true)

  const probeApiSrc = fs.readFileSync(
    path.join(__dirname, '../../src/api/scene/probe.js'),
    'utf8'
  )
  assert.ok(probeApiSrc.includes("import request from '@/utils/request'"))
  assert.ok(probeApiSrc.includes("import * as probeStore from '@/utils/scene/probeStore'"))

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

  assert.ok(probeApiSrc.includes("url: '/scene/probe/startAll'"))
  assert.ok(probeApiSrc.includes("url: '/scene/probe/stopAll'"))
  assert.ok(probeApiSrc.includes("url: '/scene/probe/start/' + deviceId"))
  assert.ok(probeApiSrc.includes("url: '/scene/probe/stop/' + deviceId"))
  assert.ok(probeApiSrc.includes("url: '/scene/probe/list'"))
  assert.ok(probeApiSrc.includes('probeStore.getAlertMuted()'))
  assert.ok(probeApiSrc.includes('probeStore.setAlertMuted(muted)'))
  assert.ok(!probeApiSrc.includes('listDevices'))

  const runtimeSrc = fs.readFileSync(
    path.join(__dirname, '../../src/utils/scene/globalMonitorRuntime.js'),
    'utf8'
  )
  assert.ok(!runtimeSrc.includes('createMockProbeEngine'))
  assert.ok(runtimeSrc.includes('diffProbeSnapshots'))
  assert.ok(runtimeSrc.includes('listProbeStatus'))

  console.log('probe-api.test.js PASS')
}

run().catch(err => {
  console.error(err)
  process.exit(1)
})
