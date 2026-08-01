const STORAGE_KEY = 'ruoyi.scene.devices'

const IP_REGEX = /^\d{1,3}(\.\d{1,3}){3}$/
const MAC_REGEX = /^(?:[0-9A-F]{2}[:-]){5}[0-9A-F]{2}$/i
const VALID_TYPES = ['router', 'switch', 'terminal', 'other']

export function normalizeMac(input) {
  if (input == null || String(input).trim() === '') {
    return null
  }
  const trimmed = String(input).trim()
  if (!MAC_REGEX.test(trimmed)) {
    return null
  }
  return trimmed.replace(/-/g, ':').toUpperCase()
}

export function isValidMac(input) {
  if (input == null || String(input).trim() === '') {
    return true
  }
  return MAC_REGEX.test(String(input).trim())
}

function loadSeedDevices() {
  try {
    return require('../api/scene/devices.seed.json')
  } catch (e) {
    return [
      { id: 'dev-core-sw1', name: '\u6838\u5fc3\u4ea4\u6362\u673a', ip: '192.168.1.1', type: 'switch', buildingId: 'bldg-core', parentDeviceId: null, remark: '' },
      { id: 'dev-core-router1', name: '\u6838\u5fc3\u8def\u7531', ip: '192.168.1.254', type: 'router', buildingId: 'bldg-core', parentDeviceId: 'dev-core-sw1', remark: '' },
      { id: 'dev-a-sw1', name: '\u4ea4\u6362\u673a A', ip: '192.168.10.1', type: 'switch', buildingId: 'bldg-a', parentDeviceId: 'dev-core-sw1', remark: '' },
      { id: 'dev-a-term1', name: '\u7ec8\u7aef A1', ip: '192.168.10.11', type: 'terminal', buildingId: 'bldg-a', parentDeviceId: 'dev-a-sw1', remark: '' },
      { id: 'dev-b-sw1', name: '\u4ea4\u6362\u673a B', ip: '192.168.20.1', type: 'switch', buildingId: 'bldg-b', parentDeviceId: 'dev-core-sw1', remark: '' },
      { id: 'dev-c-term1', name: '\u7ec8\u7aef C1', ip: '192.168.30.11', type: 'terminal', buildingId: 'bldg-c', parentDeviceId: 'dev-core-sw1', remark: '' },
      { id: 'dev-d-router1', name: '\u5165\u53e3\u8def\u7531', ip: '192.168.0.1', type: 'router', buildingId: 'bldg-d', parentDeviceId: 'dev-core-router1', remark: '' }
    ]
  }
}

function defaultStorage() {
  if (typeof localStorage !== 'undefined') {
    return localStorage
  }
  return null
}

function cloneList(list) {
  return list.map(d => ({ ...d }))
}

export function loadDevices(storage) {
  const store = storage || defaultStorage()
  if (!store) {
    return cloneList(loadSeedDevices())
  }
  const raw = store.getItem(STORAGE_KEY)
  if (!raw) {
    const seeded = cloneList(loadSeedDevices())
    saveDevices(seeded, store)
    return seeded
  }
  try {
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : cloneList(loadSeedDevices())
  } catch (e) {
    const seeded = cloneList(loadSeedDevices())
    saveDevices(seeded, store)
    return seeded
  }
}

export function saveDevices(list, storage) {
  const store = storage || defaultStorage()
  if (!store) {
    return
  }
  store.setItem(STORAGE_KEY, JSON.stringify(list))
}

export function wouldCreateCycle(id, parentId, list) {
  if (!parentId) {
    return false
  }
  if (parentId === id) {
    return true
  }
  const byId = new Map(list.map(d => [d.id, d]))
  let current = parentId
  const visited = new Set()
  while (current) {
    if (current === id) {
      return true
    }
    if (visited.has(current)) {
      return true
    }
    visited.add(current)
    const node = byId.get(current)
    current = node ? node.parentDeviceId : null
  }
  return false
}

export function validateDevice(device, list, options = {}) {
  const { isUpdate = false } = options
  if (!device || typeof device !== 'object') {
    return { ok: false, msg: '\u8bbe\u5907\u6570\u636e\u65e0\u6548' }
  }
  if (!isUpdate && !device.id) {
    return { ok: false, msg: '\u8bbe\u5907 id \u4e0d\u80fd\u4e3a\u7a7a' }
  }
  if (!device.name || !String(device.name).trim()) {
    return { ok: false, msg: '\u8bbe\u5907\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a' }
  }
  if (!device.ip || !IP_REGEX.test(device.ip)) {
    return { ok: false, msg: 'IP \u683c\u5f0f\u4e0d\u6b63\u786e' }
  }
  if (!VALID_TYPES.includes(device.type)) {
    return { ok: false, msg: '\u8bbe\u5907\u7c7b\u578b\u65e0\u6548' }
  }
  if (!device.buildingId) {
    return { ok: false, msg: '\u6240\u5c5e\u5efa\u7b51\u4e0d\u80fd\u4e3a\u7a7a' }
  }
  if (device.parentDeviceId && device.parentDeviceId === device.id) {
    return { ok: false, msg: '\u4e0d\u80fd\u5c06\u81ea\u8eab\u8bbe\u4e3a\u4e0a\u7ea7\u8bbe\u5907' }
  }
  const duplicateIp = list.some(d => d.ip === device.ip && d.id !== device.id)
  if (duplicateIp) {
    return { ok: false, msg: 'IP \u5df2\u5b58\u5728' }
  }
  if (device.parentDeviceId) {
    if (!list.some(d => d.id === device.parentDeviceId)) {
      return { ok: false, msg: '\u4e0a\u7ea7\u8bbe\u5907\u4e0d\u5b58\u5728' }
    }
    if (wouldCreateCycle(device.id, device.parentDeviceId, list)) {
      return { ok: false, msg: '\u4e0a\u7ea7\u8bbe\u5907\u5f62\u6210\u73af\u8def' }
    }
  }
  return { ok: true, msg: '' }
}

export function upsertDevice(device, storage) {
  const list = loadDevices(storage)
  const index = list.findIndex(d => d.id === device.id)
  const isUpdate = index >= 0
  const workingList = isUpdate
    ? list.map((d, i) => (i === index ? { ...d, ...device } : d))
    : [...list, { ...device }]
  const candidate = workingList.find(d => d.id === device.id)
  const validation = validateDevice(candidate, workingList, { isUpdate })
  if (!validation.ok) {
    return { ok: false, msg: validation.msg, list }
  }
  saveDevices(workingList, storage)
  return { ok: true, msg: '\u64cd\u4f5c\u6210\u529f', list: workingList }
}

export function removeDevice(id, storage) {
  const list = loadDevices(storage)
  if (!list.some(d => d.id === id)) {
    return { ok: false, msg: '\u8bbe\u5907\u4e0d\u5b58\u5728', list }
  }
  const next = list
    .filter(d => d.id !== id)
    .map(d => (d.parentDeviceId === id ? { ...d, parentDeviceId: null } : d))
  saveDevices(next, storage)
  return { ok: true, msg: '\u64cd\u4f5c\u6210\u529f', list: next }
}

const api = {
  STORAGE_KEY,
  loadDevices,
  saveDevices,
  wouldCreateCycle,
  validateDevice,
  upsertDevice,
  removeDevice,
  normalizeMac,
  isValidMac
}

export default api

if (typeof module !== 'undefined' && module.exports) {
  module.exports = api
  module.exports.STORAGE_KEY = STORAGE_KEY
  module.exports.loadDevices = loadDevices
  module.exports.saveDevices = saveDevices
  module.exports.wouldCreateCycle = wouldCreateCycle
  module.exports.validateDevice = validateDevice
  module.exports.upsertDevice = upsertDevice
  module.exports.removeDevice = removeDevice
  module.exports.normalizeMac = normalizeMac
  module.exports.isValidMac = isValidMac
}
