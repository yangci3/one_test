import { Notification } from 'element-ui'
import { alertBeep, unlockAlertAudio, stopAlertBeep } from './alertBeep'
import { listDevices } from '@/api/scene/device'
import { getAlertMuted } from './probeStore'
import { loadMonitorSettings } from './monitorSettingsStore'
import { getMonitorSettings, saveMonitorSettings } from '@/api/scene/monitorSettings'
import { diffProbeSnapshots } from './probeStatusDiff'
import { getToken } from '@/utils/auth'
import { hasSceneProbeQuery } from '@/utils/scene/sceneAuth'
import {
  startAllMonitoring,
  stopAllMonitoring,
  startDeviceMonitoring,
  stopDeviceMonitoring,
  listProbeStatus,
  setAlertMuted as apiSetAlertMuted
} from '@/api/scene/probe'

const UPDATE_EVENT = 'ruoyi-global-monitor-updated'
const SETTINGS_EVENT = 'ruoyi-monitor-settings-changed'
const AUTH_EXPIRED_EVENT = 'ruoyi-auth-expired'
const OFFLINE_ALARM_GAP_MS = 2500
const DEVICE_CACHE_EVERY_N_POLLS = 10

let bootstrapped = false
let offlineAlarmTimer = null
let offlineAlarmActive = false
let deviceCache = []
let probeMap = {}
let pollTimer = null
let pollCount = 0

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

function listToProbeMap(list) {
  const map = {}
  if (!Array.isArray(list)) return map
  list.forEach(p => {
    if (p && p.deviceId) {
      map[p.deviceId] = {
        deviceId: p.deviceId,
        monitoring: !!p.monitoring,
        status: p.status || 'unknown',
        lastChangeAt: p.lastChangeAt ?? null
      }
    }
  })
  return map
}

function pollIntervalMs() {
  try {
    const ms = loadMonitorSettings().probeIntervalMs
    const base = typeof ms === 'number' && ms >= 1000 ? ms : 3000
    return Math.min(base, 5000)
  } catch (e) {
    return 3000
  }
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

function isCustomOfflineLoopMode() {
  try {
    const settings = loadMonitorSettings()
    return settings.alertSoundMode === 'custom' && !!settings.customOfflineAudio
  } catch (e) {
    return false
  }
}

function scheduleOfflineAlarmTick() {
  clearOfflineAlarmTimer()
  if (!offlineAlarmActive) return
  // 自定义离线音频用 loop 连续播放，勿按间隔反复从头播放导致只听前几秒
  if (isCustomOfflineLoopMode()) return
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
  if (isCustomOfflineLoopMode()) {
    alertBeep({ kind: 'offline', loop: true })
    return
  }
  alertBeep({ kind: 'offline' })
  scheduleOfflineAlarmTick()
}

function anyMonitoredOffline() {
  const ids = Object.keys(probeMap)
  for (let i = 0; i < ids.length; i++) {
    const p = probeMap[ids[i]]
    if (p && p.monitoring && p.status === 'offline') return true
  }
  return false
}

function anyDeviceMonitoring() {
  const ids = Object.keys(probeMap)
  for (let i = 0; i < ids.length; i++) {
    const p = probeMap[ids[i]]
    if (p && p.monitoring) return true
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

function stopPoll() {
  if (pollTimer != null) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function startPoll() {
  stopPoll()
  pollTimer = setInterval(function () {
    runPoll()
  }, pollIntervalMs())
}

function runPoll() {
  if (!getToken() || !hasSceneProbeQuery()) {
    stopPersistentAlarm()
    stopPoll()
    return Promise.resolve()
  }
  return listProbeStatus({}).then(res => {
    const list = (res && res.code === 200 && Array.isArray(res.data)) ? res.data : []
    const events = diffProbeSnapshots(probeMap, list)
    probeMap = listToProbeMap(list)
    if (events.length > 0) {
      handleProbeChanges(events)
    } else {
      emitUpdate({ reason: 'poll' })
    }
    pollCount++
    if (pollCount % DEVICE_CACHE_EVERY_N_POLLS === 0) {
      refreshDeviceCache()
    }
    if (!anyDeviceMonitoring()) {
      stopPoll()
    }
  }).catch(() => {
    /* swallow poll errors */
  })
}

function ensurePoll() {
  if (!pollTimer) {
    startPoll()
  }
  return runPoll()
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
  // 未登录或无探测查询权限时不要绑定监听/轮询（避免 401/403）
  if (!getToken() || !hasSceneProbeQuery()) {
    stopPersistentAlarm()
    stopPoll()
    return
  }
  if (!bootstrapped) {
    bootstrapped = true
    bindAudioUnlockHooks()
    if (typeof window !== 'undefined') {
      window.addEventListener(SETTINGS_EVENT, function () {
        if (getAlertMuted()) {
          stopPersistentAlarm()
        }
        if (pollTimer) {
          stopPoll()
          startPoll()
        }
        emitUpdate({ reason: 'settings' })
      })
      window.addEventListener(AUTH_EXPIRED_EVENT, function () {
        stopPersistentAlarm()
        stopPoll()
      })
    }
  }
  refreshDeviceCache().then(() => {
    ensurePoll()
  })
}

export function stopGlobalMonitorPoll() {
  stopPersistentAlarm()
  stopPoll()
}

export function resumeGlobalMonitorAfterLogin() {
  if (!getToken() || !hasSceneProbeQuery()) {
    stopPersistentAlarm()
    stopPoll()
    return
  }
  if (!bootstrapped) {
    bootstrapGlobalMonitor()
    return
  }
  refreshDeviceCache().then(() => {
    ensurePoll()
  })
}

export function isGlobalMonitorRunning() {
  return pollTimer != null
}

export function restartGlobalMonitorEngine() {
  if (pollTimer) {
    stopPoll()
    startPoll()
  }
}

export function startAllGlobalMonitoring() {
  unlockAlertAudio()
  return refreshDeviceCache().then(() => startAllMonitoring()).then(res => {
    if (res && res.code === 200) {
      return ensurePoll().then(() => {
        emitUpdate({ reason: 'startAll' })
        return res
      })
    }
    return res
  })
}

export function stopAllGlobalMonitoring() {
  return stopAllMonitoring().then(res => {
    if (res && res.code === 200) {
      stopPersistentAlarm()
      return runPoll().then(() => {
        stopPoll()
        emitUpdate({ reason: 'stopAll' })
        return res
      })
    }
    return res
  })
}

export function startDeviceGlobalMonitoring(deviceId) {
  unlockAlertAudio()
  return refreshDeviceCache().then(() => startDeviceMonitoring(deviceId)).then(res => {
    if (res && res.code === 200) {
      return ensurePoll().then(() => {
        emitUpdate({ reason: 'startOne', deviceId })
        return res
      })
    }
    return res
  })
}

export function stopDeviceGlobalMonitoring(deviceId) {
  return stopDeviceMonitoring(deviceId).then(res => {
    if (res && res.code === 200) {
      return runPoll().then(() => {
        if (!anyDeviceMonitoring()) {
          stopPersistentAlarm()
          stopPoll()
        }
        emitUpdate({ reason: 'stopOne', deviceId })
        return res
      })
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
  return getMonitorSettings().then(res => {
    if (res.code !== 200) {
      return { code: 500, msg: res.msg || 'failed', data: null }
    }
    const settings = res.data
    settings.alertPopupEnabled = next
    return saveMonitorSettings(settings)
  }).then(res => {
    if (res.code !== 200) {
      return res
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
  stopGlobalMonitorPoll,
  resumeGlobalMonitorAfterLogin,
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

