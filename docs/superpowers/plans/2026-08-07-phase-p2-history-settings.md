# Phase P2 History + Per-User Settings Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist probe online/offline events in MySQL (written by backend on flip/start) and persist monitor settings per login user; keep custom alert audio in browser localStorage only.

**Architecture:** Tables `scene_probe_event` + `scene_monitor_settings`. `SceneProbeServiceImpl` inserts events on status change and trims (30d + max 50000). REST for history query and settings get/put/reset. Frontend switches `probeHistory.js` / `monitorSettings.js` to `request`, stops `appendProbeEvent`, merges server settings with local custom audio.

**Tech Stack:** Spring Boot + MyBatis + MySQL + Vue 2 axios `request`

**Spec:** `docs/superpowers/specs/2026-08-07-phase-p2-history-settings-design.md`

## Global Constraints

- History write: backend only (scheduler flip + start -> online); no frontend append
- No localStorage history/settings auto-migration
- Retention: delete events older than 30 days; if count > 50000 delete oldest by `event_at`
- Settings keyed by `user_id`; first GET returns defaults without insert; save upserts
- Custom audio fields stay localStorage only (not in DB)
- `probeIntervalMs` affects frontend poll interval only; backend schedule stays `scene.probe.interval-ms`
- Perms: `scene:history:list`, `scene:settings:query`, `scene:settings:edit`
- Docs UTF-8 BOM; Git: `D:\gitgit\Git\bin\git.exe`
- Branch: create `feature/phase-p2-history-settings` from current HEAD (`feature/phase-p1.5-scene-rbac`)
- Do not implement P3/P4

---

## File Structure

| Path | Role |
| --- | --- |
| `sql/scene_history_settings_p2.sql` | DDL for both tables; optional missing F-button perms |
| `ruoyi-system/.../domain/SceneProbeEvent.java` | Event entity + JSON aliases |
| `ruoyi-system/.../mapper/SceneProbeEventMapper.java` + XML | insert/list/delete/trim helpers |
| `ruoyi-system/.../domain/SceneMonitorSettings.java` | Settings entity |
| `ruoyi-system/.../mapper/SceneMonitorSettingsMapper.java` + XML | select/upsert/delete by userId |
| `ruoyi-system/.../service/impl/SceneProbeServiceImpl.java` | insert event on flip/start; trim; device-delete cascade |
| `ruoyi-system/.../service/ISceneMonitorSettingsService.java` + impl | get/save/reset |
| `ruoyi-admin/.../controller/scene/SceneProbeController.java` | history GET endpoints |
| `ruoyi-admin/.../controller/scene/SceneMonitorSettingsController.java` | settings REST |
| `ruoyi-ui/src/api/scene/probeHistory.js` | request wrappers |
| `ruoyi-ui/src/api/scene/monitorSettings.js` | request wrappers |
| `ruoyi-ui/src/utils/scene/globalMonitorRuntime.js` | remove appendProbeEvent |
| `ruoyi-ui/src/utils/scene/monitorSettingsStore.js` | split server fields vs local custom audio helpers |
| `ruoyi-ui/src/views/cesium/monitorSettings/index.vue` | tip that custom audio is local-only (optional) |
| Spec status update | after hand-test |

---

### Task 1: SQL DDL

**Files:**
- Create: `sql/scene_history_settings_p2.sql`

**Interfaces:**
- Produces: tables `scene_probe_event`, `scene_monitor_settings` (exact DDL from spec section 3)

- [ ] **Step 1: Write SQL with UTF-8 BOM (Python)** using spec DDL verbatim.

- [ ] **Step 2: If P1.5 menus lack F buttons for `scene:history:list` / `scene:settings:edit`, append INSERT for those menu_ids in 2220+ band and bind to role_id 1 and 100. Do not recreate directory 2200.

- [ ] **Step 3: Commit**

```bash
D:\gitgit\Git\bin\git.exe add sql/scene_history_settings_p2.sql
D:\gitgit\Git\bin\git.exe commit -m "Add P2 scene probe event and monitor settings tables."
```

---

### Task 2: Probe event domain + mapper

**Files:**
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/domain/SceneProbeEvent.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/mapper/SceneProbeEventMapper.java`
- Create: `ruoyi-system/src/main/resources/mapper/scene/SceneProbeEventMapper.xml`

**Interfaces:**
- Produces JSON fields: `id` <- eventId, `deviceId`, `type` <- eventType, `at` <- eventAt (use `@JsonProperty` like SceneDevice)
- Mapper methods:
  - `int insertEvent(SceneProbeEvent e)`
  - `List<SceneProbeEvent> selectEvents(@Param("deviceId") String deviceId, @Param("from") Long from, @Param("to") Long to)`
  - `int deleteByDeviceId(String deviceId)`
  - `int deleteOlderThan(long eventAtBefore)`
  - `long countAll()`
  - `int deleteOldestBeyond(int keepCount)` - delete oldest rows so remaining <= keepCount

- [ ] **Step 1: Implement domain + mapper + XML** following existing `SceneProbeState` package style.

- [ ] **Step 2: Commit** `Add SceneProbeEvent domain and mapper.`

---

### Task 3: Write events from probe service + trim + device delete

**Files:**
- Modify: `ruoyi-system/.../service/impl/SceneProbeServiceImpl.java`
- Modify: device soft-delete path that deletes probe_state

**Interfaces:**
- Consumes: Task 2 mapper
- On each successful status change to online/offline: generate `eventId = "ph-" + now + "-" + random4`, insert
- After insert(s): trim - `deleteOlderThan(now - 30L*24*3600*1000)` then if `countAll() > 50000` delete oldest
- On device soft-delete: `deleteByDeviceId`

- [ ] **Step 1: Add private `recordEvent(deviceId, type)` + `trimEvents()` helpers.**

- [ ] **Step 2: Call from tick update path and start/startAll online path; never on stop/unknown.**

- [ ] **Step 3: Wire device delete cascade.**

- [ ] **Step 4: Commit** `Record probe history events on flip/start and trim retention.`

---

### Task 4: History REST + frontend probeHistory API

**Files:**
- Modify: `ruoyi-admin/.../controller/scene/SceneProbeController.java`
- Modify: `ruoyi-ui/src/api/scene/probeHistory.js`
- Modify: `ruoyi-ui/src/utils/scene/globalMonitorRuntime.js` - remove all `appendProbeEvent` imports/calls

**Interfaces:**
- `GET /scene/probe/history` - `@PreAuthorize scene:history:list` - params deviceId/from/to
- `GET /scene/probe/history/{deviceId}` - same perm
- Match existing frontend response contracts in `probeHistory.js` / Index.vue before coding
- Frontend wrappers use `request({ url, method: "get", params })`

- [ ] **Step 1: Read current frontend expected response; implement backend to match.**

- [ ] **Step 2: Add controller methods + service query methods.**

- [ ] **Step 3: Switch frontend API; remove append from globalMonitorRuntime.**

- [ ] **Step 4: Commit** `Expose probe history REST and stop frontend local history append.`

---

### Task 5: Monitor settings domain + service + REST

**Files:**
- Create domain/mapper/xml/service/controller for settings
- Controller base: `/scene/monitor/settings`

**Interfaces:**
- GET returns defaults if no row (char 0/1 <-> boolean in JSON)
- PUT body camelCase matching frontend settings minus custom audio fields
- POST `/reset` deletes row or writes defaults; returns defaults
- Perms: query on GET; edit on PUT/reset
- Validate: volume 0-1, probeIntervalMs >= 1000, hover >= 100, beepPreset soft|default|sharp, soundMode preset|custom

- [ ] **Step 1: Implement persistence layer + defaults matching `monitorSettingsStore` (without custom audio).**

- [ ] **Step 2: Controller + SecurityUtils.getUserId() for current user.**

- [ ] **Step 3: Commit** `Add per-user scene monitor settings API.`

---

### Task 6: Frontend monitorSettings merge local custom audio

**Files:**
- Modify: `ruoyi-ui/src/api/scene/monitorSettings.js`
- Modify: `ruoyi-ui/src/utils/scene/monitorSettingsStore.js`
- Modify: settings page optional local-only tip

**Interfaces:**
- `getMonitorSettings`: GET then merge custom audio + names from localStorage
- `saveMonitorSettings`: PUT server fields only; write custom audio to localStorage
- `resetMonitorSettings`: POST reset then clear local custom audio

- [ ] **Step 1: Implement API + merge helpers.**

- [ ] **Step 2: Ensure Index/sidebar mute and poll interval still work via existing callers.**

- [ ] **Step 3: Commit** `Wire monitor settings API with local-only custom audio.`

---

### Task 7: Verify + mark spec implemented

**Files:**
- Modify: `docs/superpowers/specs/2026-08-07-phase-p2-history-settings-design.md` status (UTF-8 BOM)

**Checklist:**
1. Apply `sql/scene_history_settings_p2.sql` on ry-vue
2. Start monitor; history in DB and UI; other browser same history
3. Save settings; other browser same user sees non-audio settings
4. Custom audio only on upload machine
5. viewer: no menus / 403 on API

- [ ] **Step 1: Hand-test (or document pending if DB/UI unavailable to agent).**

- [ ] **Step 2: Update spec status to implemented; link this plan.**

- [ ] **Step 3: Commit** `Mark P2 history/settings spec as implemented.`

---

## Self-Review (plan vs spec)

| Spec item | Task |
| --- | --- |
| scene_probe_event + trim 30d/50k | T1, T2, T3 |
| Backend write on flip/start | T3 |
| History REST + frontend query | T4 |
| Remove frontend append | T4 |
| Per-user settings table + REST | T1, T5 |
| Custom audio local only | T6 |
| probeIntervalMs frontend only | T6 + Global Constraints |
| Device delete cleans events | T3 |
| No P3/P4 / no migration | Global Constraints |

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-08-07-phase-p2-history-settings.md`.

**Two execution options:**

1. **Subagent-Driven (recommended)** - fresh subagent per task, review between tasks
2. **Inline Execution** - execute in this session with executing-plans checkpoints

Which approach?
