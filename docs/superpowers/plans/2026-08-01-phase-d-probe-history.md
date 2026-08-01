# Phase D Probe History Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Record online/offline probe events for ~30 days; show Timeline in map dialog and table on a dedicated history page.

**Architecture:** Independent `probeHistoryStore` (localStorage `ruoyi.scene.probeHistory`); append from `probeStore.setMonitoring(true)` and `globalMonitorRuntime` flips; RuoYi-shaped `probeHistory` API; Index dialog + `/cesium/probe-history` page.

**Tech Stack:** Vue 2 + Element UI + localStorage mock

**Spec:** `docs/superpowers/specs/2026-08-01-phase-d-probe-history-design.md`

## Global Constraints

- Storage key: `ruoyi.scene.probeHistory`
- Event: `{ id, deviceId, type: online|offline, at }`
- Retain 30 days; MAX_EVENTS = 5000
- Write on start-monitor -> online and online<->offline flips; never on stop -> unknown
- API shape `{ code, msg, data }`
- UTF-8 BOM for Chinese docs; JS UI strings prefer \uXXXX
- No MySQL / charts / topology

---

## File Structure

| Path | Role |
| --- | --- |
| `ruoyi-ui/src/utils/scene/probeHistoryStore.js` | Persist/query/prune history |
| `ruoyi-ui/tests/cesium/probe-history-store.test.js` | Node tests |
| `ruoyi-ui/src/api/scene/probeHistory.js` | list / getDevice APIs |
| `ruoyi-ui/src/utils/scene/probeStore.js` | Append online on setMonitoring(true) |
| `ruoyi-ui/src/utils/scene/globalMonitorRuntime.js` | Append on flips |
| `ruoyi-ui/src/api/scene/device.js` | removeDeviceHistory on delete |
| `ruoyi-ui/src/views/cesium/Index.vue` | Monitor detail Timeline dialog |
| `ruoyi-ui/src/views/cesium/probeHistory/index.vue` | History table page |
| `ruoyi-ui/src/router/index.js` | Register route |

---

### Task 1: probeHistoryStore + tests

**Files:**
- Create: `ruoyi-ui/src/utils/scene/probeHistoryStore.js`
- Create: `ruoyi-ui/tests/cesium/probe-history-store.test.js`

**Produces:**
- `STORAGE_KEY = 'ruoyi.scene.probeHistory'`
- `MAX_EVENTS = 5000`
- `RETENTION_MS = 30 * 24 * 60 * 60 * 1000`
- `appendProbeEvent({ deviceId, type, at? }, storage?) -> event|null`
- `listProbeEvents({ deviceId?, from?, to? }, storage?) -> Event[]` (at desc)
- `removeDeviceHistory(deviceId, storage?)`
- Dual ESM/CJS export

- [ ] Write Node tests (append, sort, prune 30d, max 5000, remove device)
- [ ] Implement store; `node tests/cesium/probe-history-store.test.js` passes

### Task 2: API + write hooks + delete cleanup

**Files:**
- Create: `ruoyi-ui/src/api/scene/probeHistory.js`
- Modify: `probeStore.js` setMonitoring(true)
- Modify: `globalMonitorRuntime.js` handleProbeChanges
- Modify: `api/scene/device.js` delDevice

- [ ] `listProbeHistory(query)` / `getDeviceProbeHistory(deviceId)` with deviceName
- [ ] Append `online` when monitoring starts; append on status flips
- [ ] Call `removeDeviceHistory` when device deleted

### Task 3: Map Timeline dialog

**Files:**
- Modify: `ruoyi-ui/src/views/cesium/Index.vue`

- [ ] Per-device button + dialog: summary + `el-timeline` + empty state

### Task 4: History page + router

**Files:**
- Create: `ruoyi-ui/src/views/cesium/probeHistory/index.vue`
- Modify: `ruoyi-ui/src/router/index.js`

- [ ] Route `/cesium/probe-history`, menu title via \u escapes
- [ ] Device filter + table; support `?deviceId=`

### Task 5: Verify

- [ ] Node tests green
- [ ] Manual: start monitor, wait for flip, open dialog + history page, delete device clears rows
