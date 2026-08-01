import request from '@/utils/request'
import * as probeStore from '@/utils/scene/probeStore'
import { removeDeviceHistory } from '@/utils/scene/probeHistoryStore'

export function listDevices(query) {
  return request({
    url: '/scene/device/list',
    method: 'get',
    params: query
  })
}

export function getDevice(id) {
  return request({
    url: '/scene/device/' + id,
    method: 'get'
  })
}

export function addDevice(data) {
  return request({
    url: '/scene/device',
    method: 'post',
    data: data
  })
}

export function updateDevice(data) {
  return request({
    url: '/scene/device',
    method: 'put',
    data: data
  })
}

export function delDevice(id) {
  return request({
    url: '/scene/device/' + id,
    method: 'delete'
  }).then(response => {
    if (response && response.code === 200) {
      probeStore.removeDeviceProbe(id)
      try {
        removeDeviceHistory(id)
      } catch (e) {
        /* optional cleanup */
      }
    }
    return response
  })
}
