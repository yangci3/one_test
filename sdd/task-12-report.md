# Task 12 Report — My Tasks Page

**Status:** PASS  
**Commit:** feat(collab): add my tasks page for collaborators

## Summary

Added `ruoyi-ui/src/views/collab/task/mine.vue` for `collab_member` (and admins with mine permission) to view and edit assigned collaboration tasks.

### Features

| Area | Implementation |
| --- | --- |
| List | `listMyTask` + per-task `getTask` enrichment for `assignmentId` / `submitStatus`; title search filter |
| Table | task title, deadlineAt, submitStatus (编辑中/已提交/已逾期/允许补交), 编辑/查看 action |
| Editor dialog | `getTask` detail; scoped HTML via `extractEditableHtml` + `parseScopeJson`; Quill `<editor>` with `:read-only` when not editable |
| Content resolution | Prefer `draftContent`, then read-only `contentSnapshot`, else `extractEditableHtml(detail.contentHtml \|\| detail.docContentHtml, sectionIds)` |
| Save / submit | `saveDraft` / `submitAssignment` with `collab:task:submit` perm guards; buttons disabled when submitted/overdue unless `resubmit_allowed` |
| Submit confirm | `$confirm` before POST submit |
| APIs avoided | No `collab/doc/*`, `listTask`, admin task ops |

### API imports

- `@/api/collab/task`: `listMyTask`, `getTask`, `saveDraft`, `submitAssignment`
- `@/utils/collab/sectionScope`: `extractEditableHtml`, `parseScopeJson`

### Verification

- Linter: no issues on `mine.vue`
- Manual UI verify with `collab_member` deferred to Task 13

## Concerns

1. **Mine list lacks assignment fields** — `GET /collab/task/mine` returns task rows only; page N+1 calls `getTask` to populate submitStatus (acceptable for M1 scale).
2. **Task detail missing doc HTML** — `CollabTaskDetailVo` has no `contentHtml`; first edit without draft yields empty editor until backend adds filtered doc content to task detail.
3. **Overdue after deadline extension** — backend does not revert `overdue` to `editing` when deadline is extended; only `resubmit_allowed` unlocks edit (aligned with Task 7).
4. **API export name** — brief referenced `listMyTasks`; actual export is `listMyTask`.
