export const STORAGE_KEY = 'ruoyi.scene.probeHistory'
export const MAX_EVENTS = 5000
export const RETENTION_MS = 30 * 24 * 60 * 60 * 1000

function defaultStorage() {
  if (typeof localStorage !== 'undefined') {
    return localStorage
  }
  return null
}

function makeId(at) {
  const rand = Math.random().toString(36).slice(2, 6)
  return 'ph-' + at + '-' + rand
}

function isValidType(type) {
  return type === 'online' || type === 'offline'
}

function normalizeEvent(raw) {
  if (!raw || typeof raw !== 'object') return null
  if (!raw.deviceId || typeof raw.deviceId !== 'string') return null
  if (!isValidType(raw.type)) return null
  const at = typeof raw.at === 'number' && Number.isFinite(raw.at) ? raw.at : null
  if (at == null) return null
  const id = typeof raw.id === 'string' && raw.id ? raw.id : makeId(at)
  return { id, deviceId: raw.deviceId, type: raw.type, at }
}

function loadRaw(storage) {
  const store = storage || defaultStorage()
  if (!store) {
    return { events: [] }
  }
  const raw = store.getItem(STORAGE_KEY)
  if (!raw) {
    return { events: [] }
  }
  try {
    const parsed = JSON.parse(raw)
    const list = Array.isArray(parsed && parsed.events) ? parsed.events : []
    const events = []
    list.forEach(item => {
      const ev = normalizeEvent(item)
      if (ev) events.push(ev)
    })
    return { events }
  } catch (e) {
    return { events: [] }
  }
}

function pruneEvents(events, now) {
  const cutoff = now - RETENTION_MS
  let next = events.filter(ev => ev && ev.at >= cutoff)
  next.sort((a, b) => a.at - b.at)
  if (next.length > MAX_EVENTS) {
    next = next.slice(next.length - MAX_EVENTS)
  }
  return next
}

function persist(events, storage) {
  const store = storage || defaultStorage()
  if (!store) return { ok: true }
  try {
    store.setItem(STORAGE_KEY, JSON.stringify({ events }))
    return { ok: true }
  } catch (e) {
    return { ok: false, error: e }
  }
}

export function appendProbeEvent(input, storage) {
  if (!input || !input.deviceId || !isValidType(input.type)) {
    return null
  }
  const at =
    typeof input.at === 'number' && Number.isFinite(input.at) ? input.at : Date.now()
  const event = {
    id: typeof input.id === 'string' && input.id ? input.id : makeId(at),
    deviceId: String(input.deviceId),
    type: input.type,
    at
  }
  const state = loadRaw(storage)
  state.events.push(event)
  let pruned = pruneEvents(state.events, Date.now())
  let saved = persist(pruned, storage)
  if (!saved.ok) {
    // Drop oldest half and retry once (quota soft-fail).
    pruned = pruned.slice(Math.floor(pruned.length / 2))
    saved = persist(pruned, storage)
    if (!saved.ok) {
      return null
    }
  }
  return event
}

export function listProbeEvents(query, storage) {
  const q = query && typeof query === 'object' ? query : {}
  const now = Date.now()
  const from =
    typeof q.from === 'number' && Number.isFinite(q.from) ? q.from : now - RETENTION_MS
  const to = typeof q.to === 'number' && Number.isFinite(q.to) ? q.to : now
  const deviceId = q.deviceId ? String(q.deviceId) : null
  const state = loadRaw(storage)
  const pruned = pruneEvents(state.events, now)
  // Persist prune opportunistically when stale events exist.
  if (pruned.length !== state.events.length) {
    persist(pruned, storage)
  }
  const filtered = pruned.filter(ev => {
    if (deviceId && ev.deviceId !== deviceId) return false
    if (ev.at < from || ev.at > to) return false
    return true
  })
  filtered.sort((a, b) => b.at - a.at)
  return filtered
}

export function removeDeviceHistory(deviceId, storage) {
  if (!deviceId) return
  const state = loadRaw(storage)
  const next = state.events.filter(ev => ev.deviceId !== deviceId)
  persist(pruneEvents(next, Date.now()), storage)
}

const api = {
  STORAGE_KEY,
  MAX_EVENTS,
  RETENTION_MS,
  appendProbeEvent,
  listProbeEvents,
  removeDeviceHistory
}

export default api

if (typeof module !== 'undefined' && module.exports) {
  module.exports = api
  module.exports.STORAGE_KEY = STORAGE_KEY
  module.exports.MAX_EVENTS = MAX_EVENTS
  module.exports.RETENTION_MS = RETENTION_MS
  module.exports.appendProbeEvent = appendProbeEvent
  module.exports.listProbeEvents = listProbeEvents
  module.exports.removeDeviceHistory = removeDeviceHistory
}
