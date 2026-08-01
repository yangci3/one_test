import { listDevices } from '@/api/scene/device'
import * as historyStore from '@/utils/scene/probeHistoryStore'

function metaFromDevices(devices, deviceId) {
  const found = (devices || []).find(d => d && d.id === deviceId)
  if (!found) {
    return { deviceName: deviceId, ip: '' }
  }
  return {
    deviceName: found.name || deviceId,
    ip: found.ip || ''
  }
}

export function listProbeHistory(query) {
  return listDevices({}).then(res => {
    const devices = (res && res.code === 200 && res.data) || []
    const events = historyStore.listProbeEvents(query || {})
    const data = events.map(ev => {
      const meta = metaFromDevices(devices, ev.deviceId)
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
  if (!deviceId) {
    return Promise.resolve({ code: 500, msg: '\u8bbe\u5907ID\u4e0d\u80fd\u4e3a\u7a7a', data: null })
  }
  return listDevices({}).then(res => {
    const devices = (res && res.code === 200 && res.data) || []
    const meta = metaFromDevices(devices, deviceId)
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
