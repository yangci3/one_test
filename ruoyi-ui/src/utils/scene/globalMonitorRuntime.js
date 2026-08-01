import { Notification } from 'element-ui'
import { createMockProbeEngine } from './mockProbeEngine'
import { alertBeep, unlockAlertAudio, stopAlertBeep } from './alertBeep'
import { listDevices } from '@/api/scene/device'
import { getDeviceProbe, listProbeDevices, getAlertMuted } from './probeStore'
import { loadMonitorSettings, saveMonitorSettings } from './monitorSettingsStore'
import { appendProbeEvent } from './probeHistoryStore'
import {
  startAllMonitoring,
  stopAllMonitoring,
  startDeviceMonitoring,
  stopDeviceMonitoring,
  setAlertMuted as apiSetAlertMuted
} from '@/api/scene/probe'

const UPDATE_EVENT = 'ruoyi-global-monitor-updated'
const SETTINGS_EVENT = 'ruoyi-monitor-settings-changed'
const OFFLINE_ALARM_GAP_MS = 2500

let engine = null
let bootstrapped = false
let offlineAlarmTimer = null
let offlineAlarmActive = false
let deviceCache = []

function emitUpdate(detail) {
  if (typeof window === 'undefined') return
  window.dispatchEvent(new CustomEvent(UPDATE_EVENT, { detail: detail || {} }))
}

function refreshDeviceCache() {
  return listDevices({}).then(res => {
    deviceCache = (res && res.code === 200 && Array.isArray(res.data)) ? res.data : []
    return deviceCache
  }).catch(() => {
    return deviceCache
  })
}

function deviceNameById(deviceId) {
  const found = deviceCache.find(d => d && d.id === deviceId)
  return found && found.name ? found.name : deviceId
}

function clearOfflineAlarmTimer() {
  if (offlineAlarmTimer != null) {
    clearTimeout(offlineAlarmTimer)
    offlineAlarmTimer = null
  }
}

function stopPersistentAlarm() {
  offlineAlarmActive = false
  clearOfflineAlarmTimer()
  stopAlertBeep()
}

function scheduleOfflineAlarmTick() {
  clearOfflineAlarmTimer()
  if (!offlineAlarmActive) return
  offlineAlarmTimer = setTimeout(function () {
    if (!offlineAlarmActive) return
    if (getAlertMuted()) {
      stopPersistentAlarm()
      return
    }
    if (!anyMonitoredOffline()) {
      stopPersistentAlarm()
      return
    }
    alertBeep({ kind: 'offline' })
    scheduleOfflineAlarmTick()
  }, OFFLINE_ALARM_GAP_MS)
}

function startPersistentOfflineAlarm() {
  if (getAlertMuted()) {
    stopPersistentAlarm()
    return
  }
  offlineAlarmActive = true
  alertBeep({ kind: 'offline' })
  scheduleOfflineAlarmTick()
}

function anyMonitoredOffline() {
  const probes = listProbeDevices()
  for (let i = 0; i < probes.length; i++) {
    const p = probes[i]
    if (p && p.monitoring && p.status === 'offline') return true
  }
  return false
}

function isAlertPopupEnabled() {
  try {
    return loadMonitorSettings().alertPopupEnabled !== false
  } catch (e) {
    return true
  }
}

function closeAllAlertPopups() {
  try {
    if (typeof Notification.closeAll === 'function') {
      Notification.closeAll()
    }
  } catch (e) {
    /* swallow */
  }
}

function notifyStatus(offline, name) {
  if (!isAlertPopupEnabled()) return
  try {
    Notification({
      title: offline ? '\u8bbe\u5907\u79bb\u7ebf' : '\u8bbe\u5907\u4e0a\u7ebf',
      message: offline
        ? ('\u8bbe\u5907 ' + name + ' \u5df2\u79bb\u7ebf\uff08\u58f0\u97f3\u6301\u7eed\u5230\u5173\u95ed\u544a\u8b66\uff09')
        : ('\u8bbe\u5907 ' + name + ' \u5df2\u4e0a\u7ebf'),
      type: offline ? 'warning' : 'success',
      duration: 0,
      position: 'top-right',
      showClose: true
    })
  } catch (e) {
    /* swallow */
  }
}

function handleProbeChanges(events) {
  const list = Array.isArray(events) ? events : []
  let sawOffline = false
  let sawOnline = false
  list.forEach(ev => {
    if (!ev || !ev.monitoring) return
    const flipped =
      (ev.from === 'online' && ev.to === 'offline') ||
      (ev.from === 'offline' && ev.to === 'online')
    if (!flipped) return
    try {
      appendProbeEvent({
        deviceId: ev.deviceId,
        type: ev.to === 'offline' ? 'offline' : 'online'
      })
    } catch (e) {
      /* history must not block alerts */
    }
    const name = deviceNameById(ev.deviceId)
    const offline = ev.to === 'offline'
    notifyStatus(offline, name)
    if (offline) {
      sawOffline = true
    } else {
      sawOnline = true
    }
  })

  if (sawOffline || anyMonitoredOffline()) {
    startPersistentOfflineAlarm()
  } else if (sawOnline) {
    stopPersistentAlarm()
    if (!getAlertMuted()) {
      alertBeep({ kind: 'online' })
    }
  }

  emitUpdate({ events: list })
}

function ensureEngine() {
  if (!engine) {
    engine = createMockProbeEngine({
      onChange: handleProbeChanges
    })
  }
  if (typeof engine.isRunning === 'function') {
    if (!engine.isRunning()) engine.start()
  } else if (typeof engine.start === 'function') {
    engine.start()
  }
  return engine
}

function anyDeviceMonitoring() {
  for (let i = 0; i < deviceCache.length; i++) {
    const d = deviceCache[i]
    if (!d || !d.id) continue
    if (getDeviceProbe(d.id).monitoring) return true
  }
  const probes = listProbeDevices()
  return probes.some(p => p && p.monitoring)
}

function bindAudioUnlockHooks() {
  if (typeof window === 'undefined') return
  const unlock = function () {
    unlockAlertAudio()
  }
  window.addEventListener('pointerdown', unlock, { passive: true })
  window.addEventListener('keydown', unlock, { passive: true })
  if (typeof document !== 'undefined') {
    document.addEventListener('visibilitychange', function () {
      if (document.visibilityState === 'visible') {
        unlockAlertAudio()
      }
    })
  }
}

export function bootstrapGlobalMonitor() {
  if (bootstrapped) return
  bootstrapped = true
  bindAudioUnlockHooks()
  if (typeof window !== 'undefined') {
    window.addEventListener(SETTINGS_EVENT, function () {
      if (getAlertMuted()) {
        stopPersistentAlarm()
      }
      if (engine && typeof engine.restart === 'function' && engine.isRunning()) {
        engine.restart()
      }
      emitUpdate({ reason: 'settings' })
    })
  }
  refreshDeviceCache().then(() => {
    if (anyDeviceMonitoring()) {
      ensureEngine()
    }
  })
}

export function isGlobalMonitorRunning() {
  return !!(engine && typeof engine.isRunning === 'function' && engine.isRunning())
}

export function restartGlobalMonitorEngine() {
  if (!engine) return
  if (typeof engine.restart === 'function') {
    engine.restart()
  }
}

export function startAllGlobalMonitoring() {
  unlockAlertAudio()
  return refreshDeviceCache().then(() => startAllMonitoring()).then(res => {
    if (res && res.code === 200) {
      ensureEngine()
      emitUpdate({ reason: 'startAll' })
    }
    return res
  })
}

export function stopAllGlobalMonitoring() {
  return stopAllMonitoring().then(res => {
    if (res && res.code === 200) {
      stopPersistentAlarm()
      if (engine && typeof engine.stop === 'function') {
        engine.stop()
      }
      emitUpdate({ reason: 'stopAll' })
    }
    return res
  })
}

export function startDeviceGlobalMonitoring(deviceId) {
  unlockAlertAudio()
  return refreshDeviceCache().then(() => startDeviceMonitoring(deviceId)).then(res => {
    if (res && res.code === 200) {
      ensureEngine()
      emitUpdate({ reason: 'startOne', deviceId })
    }
    return res
  })
}

export function stopDeviceGlobalMonitoring(deviceId) {
  return stopDeviceMonitoring(deviceId).then(res => {
    if (res && res.code === 200) {
      if (!anyDeviceMonitoring() && engine && typeof engine.stop === 'function') {
        stopPersistentAlarm()
        engine.stop()
      }
      emitUpdate({ reason: 'stopOne', deviceId })
    }
    return res
  })
}

export function setGlobalAlertMuted(muted) {
  if (muted) {
    stopPersistentAlarm()
  } else {
    unlockAlertAudio()
  }
  return apiSetAlertMuted(muted).then(res => {
    emitUpdate({ reason: 'mute', muted: !!(res && res.data) })
    return res
  })
}

export function getAlertPopupEnabled() {
  return isAlertPopupEnabled()
}

export function setGlobalAlertPopupEnabled(enabled) {
  const next = enabled !== false
  return Promise.resolve().then(() => {
    const settings = loadMonitorSettings()
    settings.alertPopupEnabled = next
    const result = saveMonitorSettings(settings)
    if (!result.ok) {
      return { code: 500, msg: result.msg || 'failed', data: null }
    }
    if (!next) {
      closeAllAlertPopups()
    }
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent(SETTINGS_EVENT))
    }
    emitUpdate({ reason: 'popup', alertPopupEnabled: next })
    return { code: 200, msg: 'success', data: next }
  })
}

export function getGlobalMonitorUpdateEventName() {
  return UPDATE_EVENT
}

export default {
  bootstrapGlobalMonitor,
  startAllGlobalMonitoring,
  stopAllGlobalMonitoring,
  startDeviceGlobalMonitoring,
  stopDeviceGlobalMonitoring,
  setGlobalAlertMuted,
  getAlertPopupEnabled,
  setGlobalAlertPopupEnabled,
  restartGlobalMonitorEngine,
  isGlobalMonitorRunning,
  getGlobalMonitorUpdateEventName
}
