const PROBE_STORAGE_KEY = 'ruoyi.scene.probe'
const SETTINGS_STORAGE_KEY = 'ruoyi.scene.monitorSettings'
const CUSTOM_AUDIO_STORAGE_KEY = 'ruoyi.scene.monitorSettings.customAudio'
const LOCAL_AUDIO_KEYS = ['customOfflineAudio', 'customOnlineAudio', 'customOfflineName', 'customOnlineName']

// Mirror ruoyi-ui/src/utils/cesium/sceneConfig.js defaults for Node + webpack.
const SCENE_CONFIG_DEFAULTS = {
  probeIntervalMs: 5000,
  hoverSummaryDelayMs: 500,
  buildingStatusColors: {
    default: { r: 0, g: 1, b: 1, a: 0.45 },
    green: { r: 0.2, g: 0.85, b: 0.3, a: 0.55 },
    yellow: { r: 0.95, g: 0.85, b: 0.2, a: 0.55 },
    red: { r: 0.95, g: 0.25, b: 0.2, a: 0.55 }
  }
}

const BEEP_PRESETS = ['soft', 'default', 'sharp']
const SOUND_MODES = ['preset', 'custom']
const COLOR_KEYS = ['default', 'green', 'yellow', 'red']
export const MAX_CUSTOM_AUDIO_BYTES = 1024 * 1024
const AUDIO_DATA_URL_RE = /^data:audio\/(mpeg|mp3|wav|x-wav|ogg|webm);base64,/i

export function estimateDataUrlBytes(dataUrl) {
  if (!dataUrl || typeof dataUrl !== 'string') {
    return 0
  }
  const idx = dataUrl.indexOf(',')
  const b64 = idx >= 0 ? dataUrl.slice(idx + 1) : dataUrl
  return Math.floor(b64.length * 0.75)
}

function isBlankAudio(value) {
  return value == null || value === ''
}

function isAllowedAudioDataUrl(dataUrl) {
  if (isBlankAudio(dataUrl)) {
    return true
  }
  if (typeof dataUrl !== 'string' || !AUDIO_DATA_URL_RE.test(dataUrl)) {
    return false
  }
  return estimateDataUrlBytes(dataUrl) <= MAX_CUSTOM_AUDIO_BYTES
}

function defaultStorage() {
  if (typeof localStorage !== 'undefined') {
    return localStorage
  }
  return null
}

function cloneColor(color) {
  return {
    r: color.r,
    g: color.g,
    b: color.b,
    a: color.a
  }
}

function cloneColors(colors) {
  const out = {}
  COLOR_KEYS.forEach(key => {
    out[key] = cloneColor(colors[key])
  })
  return out
}

function sceneDefaults() {
  let cfg = SCENE_CONFIG_DEFAULTS
  if (typeof require !== 'undefined') {
    try {
      const mod = require('../cesium/sceneConfig')
      cfg = mod.default || mod
    } catch (e) {
      // use SCENE_CONFIG_DEFAULTS
    }
  }
  return {
    probeIntervalMs: cfg.probeIntervalMs ?? SCENE_CONFIG_DEFAULTS.probeIntervalMs,
    hoverSummaryDelayMs: cfg.hoverSummaryDelayMs ?? SCENE_CONFIG_DEFAULTS.hoverSummaryDelayMs,
    buildingStatusColors: cloneColors(
      cfg.buildingStatusColors || SCENE_CONFIG_DEFAULTS.buildingStatusColors
    )
  }
}

export function defaultMonitorSettings() {
  const fromScene = sceneDefaults()
  return {
    alertMuted: false,
    alertPopupEnabled: true,
    alertVolume: 0.7,
    beepPreset: 'default',
    alertSoundMode: 'preset',
    customOfflineAudio: null,
    customOnlineAudio: null,
    customOfflineName: '',
    customOnlineName: '',
    probeIntervalMs: fromScene.probeIntervalMs,
    hoverSummaryDelayMs: fromScene.hoverSummaryDelayMs,
    buildingStatusColors: fromScene.buildingStatusColors
  }
}

function isValidColor(color) {
  if (!color || typeof color !== 'object') {
    return false
  }
  return ['r', 'g', 'b', 'a'].every(ch => {
    const v = color[ch]
    return typeof v === 'number' && v >= 0 && v <= 1
  })
}

function asInteger(n, fallback) {
  const v = typeof n === 'number' ? n : Number(n)
  if (!Number.isFinite(v)) {
    return fallback
  }
  return Math.round(v)
}

export function validateMonitorSettings(settings) {
  if (!settings || typeof settings !== 'object') {
    return { ok: false, msg: 'invalid settings' }
  }
  const vol = settings.alertVolume
  if (typeof vol !== 'number' || vol < 0 || vol > 1) {
    return { ok: false, msg: 'alertVolume must be between 0 and 1' }
  }
  if (!BEEP_PRESETS.includes(settings.beepPreset)) {
    return { ok: false, msg: 'beepPreset must be soft, default, or sharp' }
  }
  if (!SOUND_MODES.includes(settings.alertSoundMode)) {
    return { ok: false, msg: 'alertSoundMode must be preset or custom' }
  }
  if (!isAllowedAudioDataUrl(settings.customOfflineAudio)) {
    return { ok: false, msg: 'customOfflineAudio must be mp3/wav/ogg <= 1MB' }
  }
  if (!isAllowedAudioDataUrl(settings.customOnlineAudio)) {
    return { ok: false, msg: 'customOnlineAudio must be mp3/wav/ogg <= 1MB' }
  }
  if (settings.customOfflineName != null && typeof settings.customOfflineName !== 'string') {
    return { ok: false, msg: 'customOfflineName must be a string' }
  }
  if (settings.customOnlineName != null && typeof settings.customOnlineName !== 'string') {
    return { ok: false, msg: 'customOnlineName must be a string' }
  }
  const probeIntervalMs = asInteger(settings.probeIntervalMs, NaN)
  if (!Number.isFinite(probeIntervalMs) || probeIntervalMs < 1000) {
    return { ok: false, msg: 'probeIntervalMs must be an integer >= 1000' }
  }
  const hoverSummaryDelayMs = asInteger(settings.hoverSummaryDelayMs, NaN)
  if (!Number.isFinite(hoverSummaryDelayMs) || hoverSummaryDelayMs < 100) {
    return { ok: false, msg: 'hoverSummaryDelayMs must be an integer >= 100' }
  }
  const colors = settings.buildingStatusColors
  if (!colors || typeof colors !== 'object') {
    return { ok: false, msg: 'buildingStatusColors required' }
  }
  for (const key of COLOR_KEYS) {
    if (!isValidColor(colors[key])) {
      return { ok: false, msg: `buildingStatusColors.${key} must have r,g,b,a in [0,1]` }
    }
  }
  return { ok: true, msg: 'success' }
}

function normalizeMonitorSettings(raw) {
  const defaults = defaultMonitorSettings()
  if (!raw || typeof raw !== 'object') {
    return { ...defaults, buildingStatusColors: cloneColors(defaults.buildingStatusColors) }
  }
  const colors = {}
  COLOR_KEYS.forEach(key => {
    const src = raw.buildingStatusColors && raw.buildingStatusColors[key]
    colors[key] = isValidColor(src)
      ? cloneColor(src)
      : cloneColor(defaults.buildingStatusColors[key])
  })
  const offlineAudio = isAllowedAudioDataUrl(raw.customOfflineAudio)
    ? (isBlankAudio(raw.customOfflineAudio) ? null : raw.customOfflineAudio)
    : null
  const onlineAudio = isAllowedAudioDataUrl(raw.customOnlineAudio)
    ? (isBlankAudio(raw.customOnlineAudio) ? null : raw.customOnlineAudio)
    : null
  return {
    alertMuted: !!raw.alertMuted,
    // Default on; only explicit false disables popup toasts.
    alertPopupEnabled: raw.alertPopupEnabled !== false,
    alertVolume: typeof raw.alertVolume === 'number' ? raw.alertVolume : defaults.alertVolume,
    beepPreset: BEEP_PRESETS.includes(raw.beepPreset) ? raw.beepPreset : defaults.beepPreset,
    alertSoundMode: SOUND_MODES.includes(raw.alertSoundMode) ? raw.alertSoundMode : defaults.alertSoundMode,
    customOfflineAudio: offlineAudio,
    customOnlineAudio: onlineAudio,
    customOfflineName: typeof raw.customOfflineName === 'string' ? raw.customOfflineName : '',
    customOnlineName: typeof raw.customOnlineName === 'string' ? raw.customOnlineName : '',
    probeIntervalMs: (() => {
      const v = asInteger(raw.probeIntervalMs, defaults.probeIntervalMs)
      return v >= 1000 ? v : defaults.probeIntervalMs
    })(),
    hoverSummaryDelayMs: (() => {
      const v = asInteger(raw.hoverSummaryDelayMs, defaults.hoverSummaryDelayMs)
      return v >= 100 ? v : defaults.hoverSummaryDelayMs
    })(),
    buildingStatusColors: colors
  }
}

function stripLocalAudio(settings) {
  const copy = { ...settings }
  LOCAL_AUDIO_KEYS.forEach(key => {
    delete copy[key]
  })
  return copy
}

function readProbeAlertMuted(probeStorage) {
  const store = probeStorage || defaultStorage()
  if (!store) {
    return false
  }
  const raw = store.getItem(PROBE_STORAGE_KEY)
  if (!raw) {
    return false
  }
  try {
    const parsed = JSON.parse(raw)
    return !!parsed.alertMuted
  } catch (e) {
    return false
  }
}

function readCustomAudio(storage) {
  const store = storage || defaultStorage()
  if (!store) {
    return { customOfflineAudio: null, customOnlineAudio: null, customOfflineName: '', customOnlineName: '' }
  }
  const raw = store.getItem(CUSTOM_AUDIO_STORAGE_KEY)
  if (!raw) {
    return { customOfflineAudio: null, customOnlineAudio: null, customOfflineName: '', customOnlineName: '' }
  }
  try {
    const parsed = JSON.parse(raw)
    const offline = isAllowedAudioDataUrl(parsed.customOfflineAudio)
      ? (isBlankAudio(parsed.customOfflineAudio) ? null : parsed.customOfflineAudio)
      : null
    const online = isAllowedAudioDataUrl(parsed.customOnlineAudio)
      ? (isBlankAudio(parsed.customOnlineAudio) ? null : parsed.customOnlineAudio)
      : null
    return {
      customOfflineAudio: offline,
      customOnlineAudio: online,
      customOfflineName: typeof parsed.customOfflineName === 'string' ? parsed.customOfflineName : '',
      customOnlineName: typeof parsed.customOnlineName === 'string' ? parsed.customOnlineName : ''
    }
  } catch (e) {
    return { customOfflineAudio: null, customOnlineAudio: null, customOfflineName: '', customOnlineName: '' }
  }
}

function mergeCustomAudio(settings, storage) {
  const audio = readCustomAudio(storage)
  return {
    ...settings,
    customOfflineAudio: audio.customOfflineAudio,
    customOnlineAudio: audio.customOnlineAudio,
    customOfflineName: audio.customOfflineName,
    customOnlineName: audio.customOnlineName
  }
}

function persistSettings(settings, storage) {
  const store = storage || defaultStorage()
  if (!store) {
    return { ok: true }
  }
  try {
    store.setItem(SETTINGS_STORAGE_KEY, JSON.stringify(stripLocalAudio(settings)))
    return { ok: true }
  } catch (e) {
    return {
      ok: false,
      msg: 'storage quota exceeded; use smaller audio files (<=1MB each)'
    }
  }
}

export function loadCustomAudio(storage) {
  return readCustomAudio(storage)
}

export function saveCustomAudio(audio, storage) {
  const store = storage || defaultStorage()
  if (!store) {
    return { ok: true }
  }
  const offline = audio && isAllowedAudioDataUrl(audio.customOfflineAudio)
    ? (isBlankAudio(audio.customOfflineAudio) ? null : audio.customOfflineAudio)
    : null
  const online = audio && isAllowedAudioDataUrl(audio.customOnlineAudio)
    ? (isBlankAudio(audio.customOnlineAudio) ? null : audio.customOnlineAudio)
    : null
  const payload = {
    customOfflineAudio: offline,
    customOnlineAudio: online,
    customOfflineName: audio && typeof audio.customOfflineName === 'string' ? audio.customOfflineName : '',
    customOnlineName: audio && typeof audio.customOnlineName === 'string' ? audio.customOnlineName : ''
  }
  try {
    store.setItem(CUSTOM_AUDIO_STORAGE_KEY, JSON.stringify(payload))
    return { ok: true }
  } catch (e) {
    return { ok: false, msg: 'storage quota exceeded; use smaller audio files (<=1MB each)' }
  }
}

export function clearCustomAudio(storage) {
  const store = storage || defaultStorage()
  if (!store) {
    return
  }
  try {
    store.removeItem(CUSTOM_AUDIO_STORAGE_KEY)
  } catch (e) {
    // ignore
  }
}

export function loadMonitorSettings(storage, probeStorage) {
  const store = storage || defaultStorage()
  if (!store) {
    return normalizeMonitorSettings(null)
  }
  const raw = store.getItem(SETTINGS_STORAGE_KEY)
  if (!raw) {
    const initial = defaultMonitorSettings()
    initial.alertMuted = readProbeAlertMuted(probeStorage || store)
    const normalized = normalizeMonitorSettings(initial)
    const merged = mergeCustomAudio(normalized, store)
    persistSettings(merged, store)
    return merged
  }
  try {
    const parsed = JSON.parse(raw)
    const normalized = normalizeMonitorSettings(parsed)
    return mergeCustomAudio(normalized, store)
  } catch (e) {
    const initial = defaultMonitorSettings()
    initial.alertMuted = readProbeAlertMuted(probeStorage || store)
    const normalized = normalizeMonitorSettings(initial)
    const merged = mergeCustomAudio(normalized, store)
    persistSettings(merged, store)
    return merged
  }
}

export function saveMonitorSettings(settings, storage) {
  const prepared = settings && typeof settings === 'object'
    ? {
        ...settings,
        probeIntervalMs: asInteger(settings.probeIntervalMs, settings.probeIntervalMs),
        hoverSummaryDelayMs: asInteger(settings.hoverSummaryDelayMs, settings.hoverSummaryDelayMs)
      }
    : settings
  const validation = validateMonitorSettings(prepared)
  if (!validation.ok) {
    return { ok: false, msg: validation.msg, settings: null }
  }
  const normalized = normalizeMonitorSettings(prepared)
  const persisted = persistSettings(normalized, storage)
  if (!persisted.ok) {
    return { ok: false, msg: persisted.msg, settings: null }
  }
  const audio = saveCustomAudio({
    customOfflineAudio: normalized.customOfflineAudio,
    customOnlineAudio: normalized.customOnlineAudio,
    customOfflineName: normalized.customOfflineName,
    customOnlineName: normalized.customOnlineName
  }, storage)
  if (!audio.ok) {
    return { ok: false, msg: audio.msg, settings: null }
  }
  return { ok: true, msg: 'success', settings: normalized }
}

export function resetMonitorSettings(storage) {
  const defaults = normalizeMonitorSettings(defaultMonitorSettings())
  persistSettings(defaults, storage)
  clearCustomAudio(storage)
  return defaults
}

const api = {
  SETTINGS_STORAGE_KEY,
  CUSTOM_AUDIO_STORAGE_KEY,
  MAX_CUSTOM_AUDIO_BYTES,
  defaultMonitorSettings,
  loadMonitorSettings,
  saveMonitorSettings,
  validateMonitorSettings,
  resetMonitorSettings,
  loadCustomAudio,
  saveCustomAudio,
  clearCustomAudio,
  estimateDataUrlBytes
}

export default api

if (typeof module !== 'undefined' && module.exports) {
  module.exports = api
  module.exports.SETTINGS_STORAGE_KEY = SETTINGS_STORAGE_KEY
  module.exports.CUSTOM_AUDIO_STORAGE_KEY = CUSTOM_AUDIO_STORAGE_KEY
  module.exports.MAX_CUSTOM_AUDIO_BYTES = MAX_CUSTOM_AUDIO_BYTES
  module.exports.defaultMonitorSettings = defaultMonitorSettings
  module.exports.loadMonitorSettings = loadMonitorSettings
  module.exports.saveMonitorSettings = saveMonitorSettings
  module.exports.validateMonitorSettings = validateMonitorSettings
  module.exports.resetMonitorSettings = resetMonitorSettings
  module.exports.loadCustomAudio = loadCustomAudio
  module.exports.saveCustomAudio = saveCustomAudio
  module.exports.clearCustomAudio = clearCustomAudio
  module.exports.estimateDataUrlBytes = estimateDataUrlBytes
}
