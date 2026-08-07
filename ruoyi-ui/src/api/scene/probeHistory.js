import request from '@/utils/request'

export function listProbeHistory(query) {
  return request({
    url: '/scene/probe/history',
    method: 'get',
    params: query || {}
  })
}

export function getDeviceProbeHistory(deviceId) {
  if (!deviceId) {
    return Promise.resolve({ code: 500, msg: '\u8bbe\u5907ID\u4e0d\u80fd\u4e3a\u7a7a', data: null })
  }
  return request({
    url: '/scene/probe/history/' + deviceId,
    method: 'get'
  })
}
