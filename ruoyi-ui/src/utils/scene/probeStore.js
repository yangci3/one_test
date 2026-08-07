import { loadMonitorSettings } from './monitorSettingsStore.js'
import { getMonitorSettings, saveMonitorSettings } from '@/api/scene/monitorSettings'

export const STORAGE_KEY = 'ruoyi.scene.probe'

const DEFAULT_STATE = {
  alertMuted: false,
  intervalMs: 5000,
  devices: {}
}

function defaultStorage() {
  if (typeof localStorage !== 'undefined') {
    return localStorage
  }
  return null
}

function cloneDevices(devices) {
  const out = {}
  Object.keys(devices || {}).forEach(id => {
    out[id] = { ...devices[id] }
  })
  return out
}

function cloneState(state) {
  return {
    alertMuted: !!state.alertMuted,
    intervalMs: state.intervalMs ?? DEFAULT_STATE.intervalMs,
    devices: cloneDevices(state.devices)
  }
}

function normalizeDevices(raw) {
  const devices = {}
  if (raw && raw.devices && typeof raw.devices === 'object') {
    Object.keys(raw.devices).forEach(id => {
      const entry = raw.devices[id]
      if (entry && typeof entry === 'object') {
        devices[id] = {
          monitoring: !!entry.monitoring,
          status: entry.status || 'unknown',
          lastChangeAt: entry.lastChangeAt ?? null
        }
      }
    })
  }
  return devices
}

function normalizeState(raw) {
  if (!raw || typeof raw !== 'object') {
    return cloneState(DEFAULT_STATE)
  }
  return {
    alertMuted: raw.alertMuted ?? DEFAULT_STATE.alertMuted,
    intervalMs: raw.intervalMs ?? DEFAULT_STATE.intervalMs,
    devices: normalizeDevices(raw)
  }
}

function applyPreferenceOverlay(state, storage) {
  try {
    const settings = loadMonitorSettings(storage, storage)
    state.alertMuted = !!settings.alertMuted
    state.intervalMs = settings.probeIntervalMs ?? DEFAULT_STATE.intervalMs
  } catch (e) {
    // keep probe-normalized preference fallbacks
  }
  return state
}

export function loadProbeState(storage) {
  const store = storage || defaultStorage()
  if (!store) {
    return applyPreferenceOverlay(cloneState(DEFAULT_STATE), store)
  }
  const raw = store.getItem(STORAGE_KEY)
  if (!raw) {
    const initial = cloneState(DEFAULT_STATE)
    saveProbeState(initial, store)
    return applyPreferenceOverlay(initial, store)
  }
  try {
    return applyPreferenceOverlay(normalizeState(JSON.parse(raw)), store)
  } catch (e) {
    const initial = cloneState(DEFAULT_STATE)
    saveProbeState(initial, store)
    return applyPreferenceOverlay(initial, store)
  }
}

export function saveProbeState(state, storage) {
  const store = storage || defaultStorage()
  if (!store) {
    return
  }
  // Preferences live in monitorSettings; probe JSON only owns the devices map.
  const devices = normalizeDevices(state && typeof state === 'object' ? state : {})
  store.setItem(STORAGE_KEY, JSON.stringify({ devices }))
}

function defaultDeviceProbe(deviceId) {
  return {
    deviceId,
    monitoring: false,
    status: 'unknown',
    lastChangeAt: null
  }
}

export function getDeviceProbe(deviceId, storage) {
  const state = loadProbeState(storage)
  const entry = state.devices[deviceId]
  if (!entry) {
    return defaultDeviceProbe(deviceId)
  }
  return {
    deviceId,
    monitoring: entry.monitoring,
    status: entry.status,
    lastChangeAt: entry.lastChangeAt
  }
}

export function setMonitoring(deviceId, monitoring, storage) {
  const state = loadProbeState(storage)
  const now = Date.now()
  if (monitoring) {
    state.devices[deviceId] = {
      monitoring: true,
      status: 'online',
      lastChangeAt: now
    }
    saveProbeState(state, storage)
  } else {
    state.devices[deviceId] = {
      monitoring: false,
      status: 'unknown',
      lastChangeAt: null
    }
    saveProbeState(state, storage)
  }
}

export function setStatus(deviceId, status, storage) {
  const state = loadProbeState(storage)
  const existing = state.devices[deviceId] || defaultDeviceProbe(deviceId)
  const prevStatus = existing.status
  const next = {
    monitoring: existing.monitoring,
    status,
    lastChangeAt: existing.lastChangeAt
  }
  if (status !== prevStatus) {
    next.lastChangeAt = Date.now()
  }
  state.devices[deviceId] = next
  saveProbeState(state, storage)
}

export function removeDeviceProbe(deviceId, storage) {
  const state = loadProbeState(storage)
  delete state.devices[deviceId]
  saveProbeState(state, storage)
}

export function setAlertMuted(muted, storage) {
  const next = !!muted
  return getMonitorSettings().then(res => {
    if (res.code !== 200) {
      return Promise.reject(new Error(res.msg || 'failed'))
    }
    const settings = res.data || loadMonitorSettings(storage, storage)
    settings.alertMuted = next
    return saveMonitorSettings(settings)
  }).then(res => {
    if (res.code !== 200) {
      return Promise.reject(new Error(res.msg || 'failed'))
    }
    return next
  })
}

export function getAlertMuted(storage) {
  const store = storage || defaultStorage()
  return !!loadMonitorSettings(store, store).alertMuted
}

export function listProbeDevices(storage) {
  const state = loadProbeState(storage)
  return Object.keys(state.devices).map(deviceId => ({
    deviceId,
    monitoring: state.devices[deviceId].monitoring,
    status: state.devices[deviceId].status,
    lastChangeAt: state.devices[deviceId].lastChangeAt
  }))
}

const api = {
  STORAGE_KEY,
  loadProbeState,
  saveProbeState,
  getDeviceProbe,
  setMonitoring,
  setStatus,
  removeDeviceProbe,
  setAlertMuted,
  getAlertMuted,
  listProbeDevices
}

export default api

if (typeof module !== 'undefined' && module.exports) {
  module.exports = api
  module.exports.STORAGE_KEY = STORAGE_KEY
  module.exports.loadProbeState = loadProbeState
  module.exports.saveProbeState = saveProbeState
  module.exports.getDeviceProbe = getDeviceProbe
  module.exports.setMonitoring = setMonitoring
  module.exports.setStatus = setStatus
  module.exports.removeDeviceProbe = removeDeviceProbe
  module.exports.setAlertMuted = setAlertMuted
  module.exports.getAlertMuted = getAlertMuted
  module.exports.listProbeDevices = listProbeDevices
}
