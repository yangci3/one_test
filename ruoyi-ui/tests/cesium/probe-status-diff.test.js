const assert = require('assert')
const { diffProbeSnapshots } = require('../../src/utils/scene/probeStatusDiff')

function probe(deviceId, monitoring, status) {
  return { deviceId, monitoring, status, lastChangeAt: Date.now() }
}

function prevEntry(deviceId, monitoring, status) {
  return { deviceId, monitoring, status, lastChangeAt: Date.now() }
}

// online -> offline yields event when monitoring
{
  const prev = { d1: prevEntry('d1', true, 'online') }
  const next = [probe('d1', true, 'offline')]
  const events = diffProbeSnapshots(prev, next)
  assert.strictEqual(events.length, 1)
  assert.deepStrictEqual(events[0], {
    deviceId: 'd1',
    from: 'online',
    to: 'offline',
    monitoring: true
  })
}

// offline -> online yields event when monitoring
{
  const prev = { d1: prevEntry('d1', true, 'offline') }
  const next = [probe('d1', true, 'online')]
  const events = diffProbeSnapshots(prev, next)
  assert.strictEqual(events.length, 1)
  assert.strictEqual(events[0].from, 'offline')
  assert.strictEqual(events[0].to, 'online')
}

// unknown -> online: no alert event
{
  const prev = {}
  const next = [probe('d1', true, 'online')]
  const events = diffProbeSnapshots(prev, next)
  assert.strictEqual(events.length, 0)
}

// unknown -> offline with monitoring: no alert (unknown transitions ignored)
{
  const prev = { d1: prevEntry('d1', true, 'unknown') }
  const next = [probe('d1', true, 'offline')]
  const events = diffProbeSnapshots(prev, next)
  assert.strictEqual(events.length, 0)
}

// monitoring false: no alert even if status flips
{
  const prev = { d1: prevEntry('d1', true, 'online') }
  const next = [probe('d1', false, 'offline')]
  const events = diffProbeSnapshots(prev, next)
  assert.strictEqual(events.length, 0)
}

// unchanged status: no event
{
  const prev = { d1: prevEntry('d1', true, 'online') }
  const next = [probe('d1', true, 'online')]
  const events = diffProbeSnapshots(prev, next)
  assert.strictEqual(events.length, 0)
}

console.log('probe-status-diff.test.js PASS')
