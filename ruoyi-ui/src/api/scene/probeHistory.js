import { loadDevices } from '@/utils/scene/deviceStore'
import * as historyStore from '@/utils/scene/probeHistoryStore'

function deviceMetaById(deviceId) {
  const devices = loadDevices()
  const found = devices.find(d => d && d.id === deviceId)
  if (!found) {
    return { deviceName: deviceId, ip: '' }
  }
  return {
    deviceName: found.name || deviceId,
    ip: found.ip || ''
  }
}

export function listProbeHistory(query) {
  return Promise.resolve().then(() => {
    const events = historyStore.listProbeEvents(query || {})
    const data = events.map(ev => {
      const meta = deviceMetaById(ev.deviceId)
      return {
        ...ev,
        deviceName: meta.deviceName,
        ip: meta.ip
      }
    })
    return { code: 200, msg: 'success', data }
  })
}

export function getDeviceProbeHistory(deviceId) {
  return Promise.resolve().then(() => {
    if (!deviceId) {
      return { code: 500, msg: '\u8bbe\u5907ID\u4e0d\u80fd\u4e3a\u7a7a', data: null }
    }
    const meta = deviceMetaById(deviceId)
    const events = historyStore.listProbeEvents({ deviceId })
    return {
      code: 200,
      msg: 'success',
      data: {
        deviceId,
        deviceName: meta.deviceName,
        ip: meta.ip,
        events
      }
    }
  })
}
