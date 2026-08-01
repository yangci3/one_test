import request from '@/utils/request'

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
  })
}
