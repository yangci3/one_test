import request from '@/utils/request'
import * as store from '@/utils/scene/monitorSettingsStore'

const LOCAL_AUDIO_KEYS = ['customOfflineAudio', 'customOnlineAudio', 'customOfflineName', 'customOnlineName']

function extractLocalAudio(payload) {
  const audio = {}
  LOCAL_AUDIO_KEYS.forEach(key => {
    audio[key] = payload && payload[key] != null ? payload[key] : null
  })
  return audio
}

function mergeLocalAudio(serverSettings) {
  const audio = store.loadCustomAudio()
  const merged = { ...serverSettings }
  LOCAL_AUDIO_KEYS.forEach(key => {
    if (audio[key] != null) {
      merged[key] = audio[key]
    }
  })
  return merged
}

function stripLocalAudio(payload) {
  const serverPayload = { ...payload }
  LOCAL_AUDIO_KEYS.forEach(key => {
    delete serverPayload[key]
  })
  return serverPayload
}

export function getMonitorSettings() {
  return request({
    url: '/scene/monitor/settings',
    method: 'get'
  }).then(response => {
    if (response.code !== 200) {
      return response
    }
    const merged = mergeLocalAudio(response.data || {})
    store.saveMonitorSettings(merged)
    return { code: 200, msg: 'success', data: merged }
  }).catch(error => {
    const msg = (error && error.message) || '获取监控设置失败'
    return { code: 500, msg, data: null }
  })
}

export function saveMonitorSettings(payload) {
  const audio = extractLocalAudio(payload)
  const serverPayload = stripLocalAudio(payload)
  return request({
    url: '/scene/monitor/settings',
    method: 'put',
    data: serverPayload
  }).then(response => {
    if (response.code !== 200) {
      return Promise.reject(new Error(response.msg || '保存监控设置失败'))
    }
    const audioResult = store.saveCustomAudio(audio)
    if (!audioResult.ok) {
      return Promise.reject(new Error(audioResult.msg))
    }
    const merged = mergeLocalAudio(response.data || serverPayload)
    store.saveMonitorSettings(merged)
    return { code: 200, msg: 'success', data: merged }
  }).catch(error => {
    return Promise.reject(error)
  })
}

export function resetMonitorSettings() {
  return request({
    url: '/scene/monitor/settings/reset',
    method: 'post'
  }).then(response => {
    if (response.code !== 200) {
      return Promise.reject(new Error(response.msg || '重置监控设置失败'))
    }
    store.clearCustomAudio()
    const merged = response.data || {}
    store.saveMonitorSettings(merged)
    return { code: 200, msg: 'success', data: merged }
  }).catch(error => {
    return Promise.reject(error)
  })
}
