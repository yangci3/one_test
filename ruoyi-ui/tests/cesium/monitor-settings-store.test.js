const assert = require('assert')
const store = require('../../src/utils/scene/monitorSettingsStore')
function mem(){return{_d:{},getItem(k){return this._d[k]??null},setItem(k,v){this._d[k]=String(v)}}}
const m = mem()
const s = store.loadMonitorSettings(m)
assert.strictEqual(s.probeIntervalMs, 5000)
assert.strictEqual(s.hoverSummaryDelayMs, 500)
assert.strictEqual(s.beepPreset, 'default')
const bad = store.saveMonitorSettings({ ...s, probeIntervalMs: 100 }, m)
assert.strictEqual(bad.ok, false)
const ok = store.saveMonitorSettings({ ...s, alertVolume: 0.5, beepPreset: 'sharp' }, m)
assert.strictEqual(ok.ok, true)
assert.strictEqual(store.loadMonitorSettings(m).beepPreset, 'sharp')
const reset = store.resetMonitorSettings(m)
assert.strictEqual(reset.beepPreset, 'default')
assert.strictEqual(reset.alertSoundMode, 'preset')
assert.strictEqual(reset.customOfflineAudio, null)

const withMode = store.loadMonitorSettings(mem())
const customOk = store.saveMonitorSettings({
  ...withMode,
  alertSoundMode: 'custom',
  customOfflineAudio: 'data:audio/mpeg;base64,' + 'A'.repeat(100),
  customOfflineName: 'off.mp3',
  customOnlineAudio: null,
  customOnlineName: ''
}, mem())
assert.strictEqual(customOk.ok, true)
assert.strictEqual(customOk.settings.alertSoundMode, 'custom')

const tooBig = 'data:audio/mpeg;base64,' + 'A'.repeat(Math.floor(store.MAX_CUSTOM_AUDIO_BYTES / 0.75) + 100)
const customBad = store.saveMonitorSettings({
  ...withMode,
  alertSoundMode: 'custom',
  customOfflineAudio: tooBig
}, mem())
assert.strictEqual(customBad.ok, false)
console.log('monitor-settings-store.test.js PASS')
