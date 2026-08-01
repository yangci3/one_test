import { listDevices } from '@/api/scene/device'
import * as probeStore from '@/utils/scene/probeStore'

function devicesFromRes(res) {
  if (res && res.code === 200 && Array.isArray(res.data)) {
    return res.data
  }
  return []
}

export function startDeviceMonitoring(deviceId) {
  return Promise.resolve().then(() => {
    if (!deviceId) {
      return { code: 500, msg: '\u8bbe\u5907ID\u4e0d\u80fd\u4e3a\u7a7a', data: null }
    }
    probeStore.setMonitoring(deviceId, true)
    return { code: 200, msg: 'success', data: probeStore.getDeviceProbe(deviceId) }
  })
}

export function stopDeviceMonitoring(deviceId) {
  return Promise.resolve().then(() => {
    if (!deviceId) {
      return { code: 500, msg: '\u8bbe\u5907ID\u4e0d\u80fd\u4e3a\u7a7a', data: null }
    }
    probeStore.setMonitoring(deviceId, false)
    return { code: 200, msg: 'success', data: probeStore.getDeviceProbe(deviceId) }
  })
}

export function startAllMonitoring() {
  return listDevices({}).then(res => {
    const devices = devicesFromRes(res)
    devices.forEach(d => {
      if (d && d.id) {
        probeStore.setMonitoring(d.id, true)
      }
    })
    return {
      code: 200,
      msg: 'success',
      data: devices.map(d => probeStore.getDeviceProbe(d.id))
    }
  })
}

export function stopAllMonitoring() {
  return listDevices({}).then(res => {
    const devices = devicesFromRes(res)
    devices.forEach(d => {
      if (d && d.id) {
        probeStore.setMonitoring(d.id, false)
      }
    })
    return {
      code: 200,
      msg: 'success',
      data: devices.map(d => probeStore.getDeviceProbe(d.id))
    }
  })
}

export function listProbeStatus(query) {
  return listDevices(query || {}).then(res => {
    const devices = devicesFromRes(res)
    const data = devices.map(d => probeStore.getDeviceProbe(d.id))
    return { code: 200, msg: 'success', data }
  })
}

export function getAlertMuted() {
  return Promise.resolve().then(() => {
    return { code: 200, msg: 'success', data: probeStore.getAlertMuted() }
  })
}

export function setAlertMuted(muted) {
  return Promise.resolve().then(() => {
    probeStore.setAlertMuted(muted)
    return { code: 200, msg: 'success', data: probeStore.getAlertMuted() }
  })
}
