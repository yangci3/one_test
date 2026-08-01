import * as store from '@/utils/scene/deviceStore'
import * as probeStore from '@/utils/scene/probeStore'
import { removeDeviceHistory } from '@/utils/scene/probeHistoryStore'

function filterDevices(list, query = {}) {
  let result = list
  const { buildingId, name, ip, type } = query || {}
  if (buildingId) {
    result = result.filter(d => d.buildingId === buildingId)
  }
  if (name != null && String(name).trim() !== '') {
    const needle = String(name).trim()
    result = result.filter(d => d.name && d.name.includes(needle))
  }
  if (ip != null && String(ip).trim() !== '') {
    const needle = String(ip).trim()
    result = result.filter(d => d.ip && d.ip.includes(needle))
  }
  if (type) {
    result = result.filter(d => d.type === type)
  }
  return result
}

export function listDevices(query) {
  const list = filterDevices(store.loadDevices(), query)
  return Promise.resolve({
    code: 200,
    msg: '\u67e5\u8be2\u6210\u529f',
    data: list.map(d => ({ ...d }))
  })
}

export function getDevice(id) {
  const device = store.loadDevices().find(d => d.id === id)
  if (!device) {
    return Promise.resolve({ code: 500, msg: '\u8bbe\u5907\u4e0d\u5b58\u5728' })
  }
  return Promise.resolve({
    code: 200,
    msg: '\u67e5\u8be2\u6210\u529f',
    data: { ...device }
  })
}

export function addDevice(data) {
  const device = { ...data }
  if (!device.id) {
    device.id = 'dev-' + Date.now()
  }
  const result = store.upsertDevice(device)
  if (!result.ok) {
    return Promise.resolve({ code: 500, msg: result.msg })
  }
  const saved = result.list.find(d => d.id === device.id)
  return Promise.resolve({
    code: 200,
    msg: '\u64cd\u4f5c\u6210\u529f',
    data: saved ? { ...saved } : { ...device }
  })
}

export function updateDevice(data) {
  if (!data || !data.id) {
    return Promise.resolve({ code: 500, msg: '\u8bbe\u5907 id \u4e0d\u80fd\u4e3a\u7a7a' })
  }
  const exists = store.loadDevices().some(d => d.id === data.id)
  if (!exists) {
    return Promise.resolve({ code: 500, msg: '\u8bbe\u5907\u4e0d\u5b58\u5728' })
  }
  const result = store.upsertDevice(data)
  if (!result.ok) {
    return Promise.resolve({ code: 500, msg: result.msg })
  }
  const saved = result.list.find(d => d.id === data.id)
  return Promise.resolve({
    code: 200,
    msg: '\u64cd\u4f5c\u6210\u529f',
    data: saved ? { ...saved } : { ...data }
  })
}

export function delDevice(id) {
  const result = store.removeDevice(id)
  if (!result.ok) {
    return Promise.resolve({ code: 500, msg: result.msg })
  }
  probeStore.removeDeviceProbe(id)
  try {
    removeDeviceHistory(id)
  } catch (e) {
    /* optional cleanup */
  }
  return Promise.resolve({
    code: 200,
    msg: '\u64cd\u4f5c\u6210\u529f',
    data: null
  })
}
