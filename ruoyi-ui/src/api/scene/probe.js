import request from '@/utils/request'
import * as probeStore from '@/utils/scene/probeStore'

export function startDeviceMonitoring(deviceId) {
  return request({
    url: '/scene/probe/start/' + deviceId,
    method: 'post'
  })
}

export function stopDeviceMonitoring(deviceId) {
  return request({
    url: '/scene/probe/stop/' + deviceId,
    method: 'post'
  })
}

export function startAllMonitoring() {
  return request({
    url: '/scene/probe/startAll',
    method: 'post'
  })
}

export function stopAllMonitoring() {
  return request({
    url: '/scene/probe/stopAll',
    method: 'post'
  })
}

export function listProbeStatus(query) {
  return request({
    url: '/scene/probe/list',
    method: 'get',
    params: query || {}
  })
}

export function getAlertMuted() {
  return Promise.resolve().then(() => {
    return { code: 200, msg: 'success', data: probeStore.getAlertMuted() }
  })
}

export function setAlertMuted(muted) {
  return probeStore.setAlertMuted(muted).then(() => {
    return { code: 200, msg: 'success', data: probeStore.getAlertMuted() }
  })
}
