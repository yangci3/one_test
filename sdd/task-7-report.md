# Task 7 Report ！ Draft, Submit, Deadline, Resubmit

**Status:** PASS  
**Commit:** feat(collab): add submit, deadline, and resubmit flow

## Summary

Extended `CollabTaskServiceImpl` and `CollabTaskController` with five endpoints for the Collab M1 submit workflow:

| Method | Path | Permission |
| --- | --- | --- |
| PUT | `/collab/task/{taskId}/assignment/{assignmentId}/draft` | `collab:task:submit` |
| POST | `/collab/task/{taskId}/assignment/{assignmentId}/submit` | `collab:task:submit` |
| PUT | `/collab/task/{taskId}/deadline` | `collab:task:edit` |
| PUT | `/collab/task/{taskId}/allow-resubmit` | `collab:task:edit` |
| GET | `/collab/task/{taskId}/unsubmitted` | `collab:task:edit` |

Added request VOs: `CollabDraftVo`, `CollabSubmitVo`, `CollabDeadlineVo`, `CollabAllowResubmitVo`.

### Logic implemented

- **refreshOverdueStatuses** ！ when `now > deadline_at` and `submit_status = editing`, updates to `overdue` via `CollabTaskAssignmentMapper.updateAssignment`; invoked on detail, draft, submit, and unsubmitted queries.
- **canEdit** ！ `submitted` blocks edit; `resubmit_allowed` allows edit; otherwise requires `editing` and deadline not passed.
- **draft / submit** ！ assignee-only (`SecurityUtils.getUserId()` must match `assignment.userId`); HTML section ids validated as subset of `scope_json.sectionIds` using `CollabSectionHelper` + fastjson2.
- **submit** ！ sets `submit_status=submitted`, `submitted_at=now`, `content_snapshot=content`.
- **extendDeadline** ！ `CollabTaskMapper.updateCollabTaskDeadline` + `CollabTaskDeadlineLogMapper.insertDeadlineLog`.
- **allowResubmit** ！ sets `submit_status=resubmit_allowed`.
- **listUnsubmitted** ！ returns assignments with `editing` or `overdue` status after deadline has passed.

### Verification

```
mvn -pl ruoyi-admin -am compile -q   # exit 0
```

## Concerns

1. **updateAssignment always writes draft/snapshot columns** ！ mapper XML sets `draft_content` and `content_snapshot` unconditionally; service loads full assignment before update to avoid nulling preserved fields.
2. **Overdue status after deadline extension** ！ extending deadline does not reset `overdue` back to `editing`; admin must use allow-resubmit for those assignees.
3. **No integration smoke test** ！ compile-only verification in this task; curl/API smoke deferred to Task 13.
