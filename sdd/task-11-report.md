# Task 11 Report — Task Management Page

**Status:** PASS  
**Commit:** feat(collab): add task management page

## Summary

Added `ruoyi-ui/src/views/collab/task/index.vue` for `collab_admin` collaboration task management.

### Features

| Area | Implementation |
| --- | --- |
| Search | Title + taskStatus filters → `listTask` |
| Table | title, docTitle (enriched from `listDoc`), deadlineAt, taskStatus, 详情/管理 actions |
| Create dialog | Doc select (`listDoc` + `getDoc` for sections), title, datetime deadline, fixed 提交版 mode, multi-select candidates via `listCandidates` only, per-user section checkboxes from `listSectionIds(contentHtml)` |
| Detail drawer | Task/doc metadata, assignments table with `parseScopeJson` for scope display |
| Manage mode | Extend deadline (`extendDeadline`), unsubmitted list (`listUnsubmitted`), allow resubmit per row (`allowResubmit`), merge summary (`mergeTask`) |
| Permissions | `v-hasPermi` on create (`collab:task:add`) and manage actions (`collab:task:edit`) |
| Layout | `app-container` + inline search + toolbar (aligned with doc/device patterns) |

### API imports

- `@/api/collab/doc`: `listDoc`, `getDoc`
- `@/api/collab/task`: `listTask`, `getTask`, `addTask`, `extendDeadline`, `allowResubmit`, `listUnsubmitted`, `mergeTask`
- `@/api/collab/user`: `listCandidates`
- `@/utils/collab/sectionScope`: `listSectionIds`, `parseScopeJson`

### Verification

- Linter: no issues on `index.vue`
- Manual UI verify with `collab_admin` deferred to Task 13 (no running backend in this task)

## Concerns

1. **List API non-paginated** — backend returns full task list; docTitle enriched client-side from cached doc list (may be stale until refresh).
2. **Candidate display for historical users** — assignment user names resolved from current `listCandidates`; users removed from collab_member role show raw userId.
3. **Unsubmitted list empty before deadline** — backend returns `[]` until past deadline; UI shows empty table (expected).
4. **API export name** — task brief referenced `listCollabCandidates`; actual export is `listCandidates` from `@/api/collab/user`.
5. **No task edit/delete** — scope is create + deadline/resubmit/merge admin ops only.
