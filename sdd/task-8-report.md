# Task 8 Report ！ Merge Summary API

**Status:** PASS  
**Commit:** feat(collab): add task merge summary

## Summary

Added `POST /collab/task/{taskId}/merge` (`collab:task:edit`) to merge assignment snapshots into the linked document.

| Method | Path | Permission |
| --- | --- | --- |
| POST | `/collab/task/{taskId}/merge` | `collab:task:edit` |

### Logic implemented

- **mergeTaskSummary** ！ loads task, document, and all assignments; builds `sectionId ★ inner HTML` from each assignment's `content_snapshot` (parses `scope_json.sectionIds`, strips section wrapper via `CollabSectionHelper.extractSections`).
- **Unsubmitted sections** ！ for any section id in assignment scope but absent from the snapshot map, inserts `<p>[unsubmitted:sec-N]</p>`.
- **Merge** ！ `CollabSectionHelper.mergeSnapshots(doc.contentHtml, map)`; sections outside assignment scope keep original document content.
- **Persist** ！ updates `collab_doc.content_html` via `CollabDocMapper.updateCollabDoc`.

### Verification

```
mvn -pl ruoyi-admin -am compile -q   # exit 0
```

## Concerns

1. **Overlapping section scope** ！ if two assignments share a section id, the last processed assignment wins when building the update map.
2. **stripSectionWrapper heuristic** ！ inner HTML extraction uses first `>` / last `</div`; nested divs inside a section could truncate incorrectly (same limitation as manual wrapper stripping).
3. **No integration smoke test** ！ compile-only verification; curl/API smoke deferred to Task 13.
