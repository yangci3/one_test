/**
 * Compare consecutive probe poll snapshots and emit alert-worthy status flips.
 * prevMap: { [deviceId]: { deviceId, monitoring, status, lastChangeAt } }
 * nextList: array of probe states from /scene/probe/list
 */
export function diffProbeSnapshots(prevMap, nextList) {
  const events = []
  const prev = prevMap || {}
  const list = Array.isArray(nextList) ? nextList : []

  list.forEach(next => {
    if (!next || !next.deviceId) return
    const deviceId = next.deviceId
    const prevEntry = prev[deviceId]
    const from = prevEntry ? (prevEntry.status || 'unknown') : 'unknown'
    const to = next.status || 'unknown'

    if (from === to) return
    if (!next.monitoring) return

    const flipped =
      (from === 'online' && to === 'offline') ||
      (from === 'offline' && to === 'online')
    if (!flipped) return

    events.push({
      deviceId,
      from,
      to,
      monitoring: true
    })
  })

  return events
}

const api = { diffProbeSnapshots }

export default api

if (typeof module !== 'undefined' && module.exports) {
  module.exports = api
  module.exports.diffProbeSnapshots = diffProbeSnapshots
}
