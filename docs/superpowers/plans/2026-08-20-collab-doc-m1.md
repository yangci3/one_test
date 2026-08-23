# Collab M1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a RuoYi-native collaborative document module (submit mode, section scope, deadlines, resubmit) parallel to 3D network monitoring, with RBAC roles `collab_admin` / `collab_member`.

**Architecture:** MySQL tables `collab_*` in `ruoyi-system` with MyBatis mappers; REST controllers under `ruoyi-admin/.../controller/collab`; Vue pages under `views/collab/` with thin `api/collab/*.js` wrappers. Section editing uses HTML anchors `data-sec-id="sec-N"`; a pure `CollabSectionHelper` extracts/merges blocks. Candidate users come from `sys_user_role` joined to `collab_member` role - never trust frontend user lists.

**Tech Stack:** Spring Boot 2.x + `@PreAuthorize`, MyBatis, Vue 2 + Element UI, global Quill `<editor>`, Node assert tests, Java `main` self-checks

**Spec:** `docs/superpowers/specs/2026-08-19-collab-doc-m1-design.md`

## Global Constraints

- Stay on current git branch; do not create a new branch unless asked
- Docs and SQL with Chinese: UTF-8 with BOM
- Menu band 2300-2399; permission prefix `collab:*` (separate from `scene:*`)
- Roles: 102 `collab_admin`, 103 `collab_member` (scene uses 100/101)
- M1 mode: `submit` only - no WebSocket, no realtime, no spreadsheet / row_range
- Collaborator access Option A: must have `collab_member` role AND task assignment
- `GET /collab/user/candidates` returns only enabled users with `collab_member` role
- Assign validation: reject invalid user_id with ServiceException code 400
- After deadline: `submit_status` becomes `overdue`; block edit unless `resubmit_allowed`
- No global 403 suppression; pages without permission must not call collab APIs
- No JUnit: Java logic checks use `*SelfCheck.java` with `main`; frontend uses `node ruoyi-ui/tests/collab/*.test.js`
- Reuse existing Quill `ruoyi-ui/src/components/Editor/index.vue`; do not add OnlyOffice
- Do not commit secrets (`application-druid.yml`)

---

## File Structure

| Path | Role |
| --- | --- |
| `sql/collab_m1.sql` | Tables + menus + roles 102/103 |
| `ruoyi-system/.../domain/CollabDoc.java` | Document entity |
| `ruoyi-system/.../domain/CollabTask.java` | Task entity |
| `ruoyi-system/.../domain/CollabTaskAssignment.java` | Per-user assignment |
| `ruoyi-system/.../domain/CollabTaskDeadlineLog.java` | Deadline audit |
| `ruoyi-system/.../domain/vo/CollabUserCandidateVo.java` | Candidate VO |
| `ruoyi-system/.../domain/vo/CollabTaskCreateVo.java` | Task create payload |
| `ruoyi-system/.../mapper/Collab*.java` + `resources/mapper/collab/*.xml` | Persistence |
| `ruoyi-system/.../service/collab/CollabSectionHelper.java` | Section HTML helper |
| `ruoyi-system/src/test/java/.../CollabSectionHelperSelfCheck.java` | Helper tests |
| `ruoyi-system/.../service/ICollab*Service.java` + impl | Business logic |
| `ruoyi-admin/.../controller/collab/Collab*Controller.java` | REST endpoints |
| `ruoyi-ui/src/api/collab/*.js` | API wrappers |
| `ruoyi-ui/src/utils/collab/sectionScope.js` | Frontend section utils |
| `ruoyi-ui/tests/collab/*.test.js` | Node tests |
| `ruoyi-ui/src/views/collab/doc/index.vue` | Admin doc CRUD |
| `ruoyi-ui/src/views/collab/task/index.vue` | Admin task management |
| `ruoyi-ui/src/views/collab/task/mine.vue` | Member my-tasks |

---

### Task 1: Database schema and RBAC SQL

**Files:** Create `sql/collab_m1.sql`

**Interfaces:** Produces 4 tables, menus 2300-2319, roles 102/103

- [ ] **Step 1:** Write DDL for `collab_doc`, `collab_task`, `collab_task_assignment`, `collab_task_deadline_log` (design spec section 5). Chinese COMMENT strings; file UTF-8 BOM.

- [ ] **Step 2:** Insert menus 2300 (M), 2301-2303 (C), 2310+ (F). Mirror `sql/scene_rbac_p1_5.sql`.

- [ ] **Step 3:** Insert roles 102/103 and `sys_role_menu` (admin + collab_admin full band; collab_member: 2300, 2301, submit perm).

- [ ] **Step 4:** Run on `ry-vue`; verify tables and menus.

- [ ] **Step 5:** Commit `feat(collab): add M1 schema and RBAC menus`

---

### Task 2: Domain entities and MyBatis mappers

**Interfaces:**
- `CollabDocMapper.selectCollabDocById`, `insertCollabDoc`, `updateCollabDoc`, `selectCollabDocList`
- `CollabTaskMapper.selectCollabTaskById`, `insertCollabTask`, `selectCollabTaskList`, `updateCollabTaskDeadline`
- `CollabTaskAssignmentMapper.insertBatch`, `selectByTaskId`, `selectByTaskAndUser`, `updateAssignment`
- `CollabUserMapper.selectCollabMemberCandidates()` -> `List<CollabUserCandidateVo>`

- [ ] **Step 1:** Write entities + VOs
- [ ] **Step 2:** Write mapper XML (mirror SceneDeviceMapper)
- [ ] **Step 3:** `mvn -pl ruoyi-system -am compile -q`
- [ ] **Step 4:** Commit

---

### Task 3: Section HTML helper

**Interfaces:** `listSectionIds`, `extractSections`, `mergeSnapshots`, `ensureDefaultSection`

- [ ] **Step 1:** CollabSectionHelperSelfCheck.main (4 assertions)
- [ ] **Step 2:** Implement CollabSectionHelper
- [ ] **Step 3:** Run SelfCheck
- [ ] **Step 4:** Commit

---

### Task 4: User candidates API

- [ ] **Step 1:** GET `/collab/user/candidates` perm `collab:task:add`
- [ ] **Step 2:** `isCollabMember(Long userId)` for validation
- [ ] **Step 3:** Curl smoke test
- [ ] **Step 4:** Commit

---

### Task 5: Document CRUD API

Endpoints: GET list, GET /{id}, POST, PUT - perms `collab:doc:*`

- [ ] **Step 1:** Service + controller; `ensureDefaultSection` on save
- [ ] **Step 2:** Compile + smoke test
- [ ] **Step 3:** Commit

---

### Task 6: Task create, list, mine APIs

- [ ] **Step 1:** createTask validates doc, deadline, collab_member userIds
- [ ] **Step 2:** GET list, GET mine, GET detail, POST create
- [ ] **Step 3:** Smoke test
- [ ] **Step 4:** Commit

---

### Task 7: Draft, submit, deadline, resubmit, unsubmitted

- [ ] **Step 1:** overdue refresh + draft/submit with scope validation
- [ ] **Step 2:** extend deadline, allow resubmit, list unsubmitted
- [ ] **Step 3:** Integration smoke
- [ ] **Step 4:** Commit

---

### Task 8: Merge summary API

- [ ] **Step 1:** POST `/collab/task/{id}/merge`
- [ ] **Step 2:** Smoke test
- [ ] **Step 3:** Commit

---

### Task 9: Frontend API + section utils

- [ ] **Step 1:** api/collab/doc.js, task.js, user.js
- [ ] **Step 2:** sectionScope.js + node tests
- [ ] **Step 3:** Run tests PASS
- [ ] **Step 4:** Commit

---

### Task 10: Document management page

- [ ] **Step 1:** views/collab/doc/index.vue CRUD + editor
- [ ] **Step 2:** Add section button for data-sec-id blocks
- [ ] **Step 3:** Manual verify collab_admin
- [ ] **Step 4:** Commit

---

### Task 11: Task management page

- [ ] **Step 1:** List + create (candidates from API only)
- [ ] **Step 2:** Detail: unsubmitted, extend, resubmit, merge
- [ ] **Step 3:** Manual verify
- [ ] **Step 4:** Commit

---

### Task 12: My tasks page

- [ ] **Step 1:** views/collab/task/mine.vue list
- [ ] **Step 2:** Scoped editor dialog
- [ ] **Step 3:** Save/submit with guards
- [ ] **Step 4:** Manual verify collab_member
- [ ] **Step 5:** Commit

---

### Task 13: End-to-end verification

- [x] RBAC users A/B/C/D checks
- [x] Candidate list, section scope, deadline, resubmit, merge
- [x] SelfCheck + node tests

**Status (2026-08-23):** M1 code-complete. API smoke via `python scripts/collab_e2e_api.py`; frontend via `node ruoyi-ui/tests/collab/*.test.js`. Browser verified: merge content + open linked doc navigation.

---

## Spec Coverage

| Requirement | Task |
| --- | --- |
| Single login, dynamic menus | 1, 10-13 |
| Admin doc/task CRUD | 5, 6, 10, 11 |
| Candidates = collab_member only | 4, 11 |
| Member mine + section edit | 7, 12 |
| Deadline/resubmit/unsubmitted | 7, 11 |
| Merge summary | 8, 11 |
| submit mode only | 6 |
| No WebSocket/spreadsheet | Constraints |

---

**Plan saved to `docs/superpowers/plans/2026-08-20-collab-doc-m1.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - fresh subagent per task, review between tasks

**2. Inline Execution** - execute in this session with executing-plans checkpoints

**Which approach?**
