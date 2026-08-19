# Task 9 Report ！ Frontend API + Section Utils

**Status:** PASS  
**Commit:** feat(collab): add frontend API wrappers and section utils

## Summary

Added Collab M1 frontend API wrappers mirroring backend controllers and pure HTML section utilities for scoped editing.

### API modules (`@/utils/request`)

| Module | Exports | Backend paths |
| --- | --- | --- |
| `api/collab/user.js` | `listCandidates` | `GET /collab/user/candidates` |
| `api/collab/doc.js` | `listDoc`, `getDoc`, `addDoc`, `updateDoc` | `GET /collab/doc/list`, `GET /collab/doc/{id}`, `POST/PUT /collab/doc` |
| `api/collab/task.js` | `listTask`, `listMyTask`, `getTask`, `addTask`, `saveDraft`, `submitAssignment`, `extendDeadline`, `allowResubmit`, `listUnsubmitted`, `mergeTask` | All CollabTaskController endpoints including draft, submit, deadline, allow-resubmit, unsubmitted, merge |

### Section utils (`utils/collab/sectionScope.js`)

- **parseScopeJson** ！ parses `scope_json.sectionIds` array (string or object)
- **listSectionIds** ！ finds unique `data-sec-id` values in document HTML
- **extractEditableHtml** ！ returns full section div blocks for allowed section ids (mirrors backend `CollabSectionHelper.extractSections`)

### Verification

```
node ruoyi-ui/tests/collab/section-scope.test.js   # PASS
node ruoyi-ui/tests/collab/collab-api.test.js      # PASS
```

## Concerns

1. **fromSec/toSec scope** ！ `parseScopeJson` only supports `sectionIds` array; range-style scope from design spec is not expanded client-side (M1 backend also uses sectionIds only).
2. **No runtime API smoke** ！ collab-api test uses source inspection; live HTTP verification deferred to Task 13.
3. **Nested div edge cases** ！ `extractEditableHtml` uses div-depth matching aligned with backend helper; deeply malformed HTML may still mis-bound sections.
