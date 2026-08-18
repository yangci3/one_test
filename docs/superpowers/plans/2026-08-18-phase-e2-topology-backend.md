# Phase E2 Topology API Backendization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move topology graph and link-detail APIs from frontend mock to Spring, sourced from MySQL `scene_device.parent_device_id`, without changing map/topology UI behavior.

**Architecture:** Pure `SceneTopologyGraph` builds nodes/edges and focus subgraphs. `SceneTopologyServiceImpl` loads undeleted devices, enriches `buildingName` from classpath JSON, and exposes two GETs via `SceneTopologyController`. Frontend `topology.js` becomes thin `request` wrappers. Probe status stays on `/scene/probe/list`.

**Tech Stack:** Spring MVC + `@PreAuthorize`, MyBatis `SceneDeviceMapper`, Fastjson2 for building names, Vue `request`, existing Node assert tests

**Spec:** `docs/superpowers/specs/2026-08-18-phase-e2-topology-backend-design.md`

## Global Constraints

- Stay on current git branch (do not create a new branch)
- Git executable: `D:\gitgit\Git\bin\git.exe`
- Docs and this plan: UTF-8 with BOM
- No new SQL tables, no `scene:topology:*` menus, no SNMP, no P3 tiles
- Edges only from `parentDeviceId` (parent -> child); `edge.id` = `edge-{fromDeviceId}-{toDeviceId}`
- Topology JSON must use frontend names: `id` / `name` / `type` (never dump `SceneDevice` as-is)
- Do not put probe online/offline on topology responses
- Permission on both endpoints: `scene:device:list`
- Missing focus: `ServiceException("\u8bbe\u5907\u4e0d\u5b58\u5728", 500)`
- Missing/invalid edge: `ServiceException("\u94fe\u8def\u4e0d\u5b58\u5728", 500)`
- Device IDs contain `-`; never split `edgeId` on the first hyphen
- No JUnit in this repo: Java checks use a `main` self-check class; frontend uses `node tests/cesium/*.js`
- Do not change `Index.vue` / `topology/index.vue` interaction; keep `layoutLevels` in `topologyGraph.js`
- Do not commit secrets (`application-druid.yml`) or the untracked full RuoYi tree

---

## File Structure

| Path | Role |
| --- | --- |
| `ruoyi-system/.../domain/SceneTopologyNode.java` | Node VO |
| `ruoyi-system/.../domain/SceneTopologyEdge.java` | Edge VO |
| `ruoyi-system/.../domain/SceneTopologyGraphVo.java` | `{ nodes, edges }` |
| `ruoyi-system/.../domain/SceneTopologyLinkVo.java` | `{ edgeId, from, to }` |
| `ruoyi-system/.../service/topology/SceneTopologyGraph.java` | Pure graph helpers |
| `ruoyi-system/src/test/java/.../SceneTopologyGraphSelfCheck.java` | `main` assertions |
| `ruoyi-system/src/main/resources/scene/buildings-names.json` | id/name catalog |
| `ruoyi-system/.../service/topology/SceneBuildingNameCatalog.java` | Load JSON; `nameOf` |
| `ruoyi-system/.../service/ISceneTopologyService.java` | getGraph / getLink |
| `ruoyi-system/.../service/impl/SceneTopologyServiceImpl.java` | Devices + graph + names |
| `ruoyi-admin/.../controller/scene/SceneTopologyController.java` | Two GETs |
| `ruoyi-ui/src/api/scene/topology.js` | `request` wrappers |
| `ruoyi-ui/tests/cesium/topology-api.test.js` | Source URL assertions |

Packages: VOs in `com.ruoyi.system.domain`; graph helpers in `com.ruoyi.system.service.topology`.

---

### Task 1: Pure topology graph

**Files:**
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/domain/SceneTopologyNode.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/domain/SceneTopologyEdge.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/domain/SceneTopologyGraphVo.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/domain/SceneTopologyLinkVo.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/service/topology/SceneTopologyGraph.java`
- Create: `ruoyi-system/src/test/java/com/ruoyi/system/service/topology/SceneTopologyGraphSelfCheck.java`

**Interfaces:**
- `SceneTopologyGraph.edgeId(String fromDeviceId, String toDeviceId): String` -> `edge-{from}-{to}`
- `SceneTopologyGraph.buildGraph(List<SceneTopologyNode> nodes): SceneTopologyGraphVo`
- `SceneTopologyGraph.focusSubgraph(SceneTopologyGraphVo graph, String focusDeviceId): SceneTopologyGraphVo`
- `SceneTopologyGraph.parseEdgeKey(String edgeId, Collection<String> deviceIds): String[]` `{from,to}` or null
- Skip blank ids; no edge if parent blank/missing/self; ancestor walk uses visited set (no throw on cycle)
- `parseEdgeKey` matches by computing `edgeId(from,to)` over distinct id pairs; do not split on first `-`
- Missing focus in `focusSubgraph`: empty lists (service turns unknown focus into 500)

VO fields:
- Node: `id`, `name`, `ip`, `type`, `buildingId`, `buildingName`, `parentDeviceId`
- Edge: `id`, `fromDeviceId`, `toDeviceId`
- GraphVo: never-null `nodes`, `edges` lists
- LinkVo: `edgeId`, `from`, `to`

- [ ] **Step 1: Write VO getters/setters.**
- [ ] **Step 2: Write `SceneTopologyGraphSelfCheck.main`** (port of `ruoyi-ui/tests/cesium/topology-graph.test.js` plus isolate/self-parent and hyphen parse). Assertions:
  - sample 7 nodes / 6 edges; `dev-a-sw1` parent `dev-core-sw1`; edge id `edge-dev-core-sw1-dev-a-sw1`
  - focus `dev-a-sw1` node ids exactly `{dev-a-sw1,dev-a-term1,dev-core-sw1}`; no `dev-b-sw1`
  - focus `dev-core-sw1` has 7 nodes; focus `missing` is empty
  - orphan + missing parent + self parent => 0 edges, 3 nodes
  - `parseEdgeKey("edge-dev-core-sw1-dev-a-sw1")` -> core-sw1, a-sw1; bad string -> null
  - print `SceneTopologyGraphSelfCheck OK`
- [ ] **Step 3: Compile/run and confirm failure** (`SceneTopologyGraph` missing):

```
mvn -pl ruoyi-system -am test-compile -DskipTests
```

- [ ] **Step 4: Implement `SceneTopologyGraph`** (final class, private ctor, no Spring). Port `topologyGraph.js` `edgeId`/`buildGraph`/`focusSubgraph` and `topology.js` `parseEdgeKey` double loop.
- [ ] **Step 5: Run self-check until OK:**

```
java -cp ruoyi-system/target/test-classes;ruoyi-system/target/classes com.ruoyi.system.service.topology.SceneTopologyGraphSelfCheck
```

PowerShell `-cp` uses `;`. Expected: `SceneTopologyGraphSelfCheck OK`
- [ ] **Step 6: Commit** `Add scene topology graph helpers and self-check.`
  Git: `D:\gitgit\Git\bin\git.exe`. Add only the six Java files listed above.

---

### Task 2: Building names, service, controller

**Files:**
- Create: `ruoyi-system/src/main/resources/scene/buildings-names.json`
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/service/topology/SceneBuildingNameCatalog.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/service/ISceneTopologyService.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/service/impl/SceneTopologyServiceImpl.java`
- Create: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/scene/SceneTopologyController.java`

**Interfaces:**
- `SceneBuildingNameCatalog.nameOf(String buildingId)`: blank -> `""`; known -> name; unknown -> `buildingId`
- Classpath `/scene/buildings-names.json` UTF-8 array (id/name only):

```json
[
  {"id":"bldg-core","name":"核心机房"},
  {"id":"bldg-a","name":"车间 A"},
  {"id":"bldg-b","name":"车间 B"},
  {"id":"bldg-c","name":"办公楼"},
  {"id":"bldg-d","name":"配电室"}
]
```

Parse with `com.alibaba.fastjson2.JSON`. Load once in constructor via `getResourceAsStream("/scene/buildings-names.json")`. Missing file -> empty map.
- `ISceneTopologyService.getGraph(String focusDeviceId): SceneTopologyGraphVo`
- `ISceneTopologyService.getLink(String edgeId): SceneTopologyLinkVo`
- Devices: `sceneDeviceMapper.selectSceneDeviceList(new SceneDevice())`
- Map: `deviceId`->id, name or id, ip or `""`, `deviceType` or `"other"`, buildingId, parentDeviceId, buildingName via catalog
- `getGraph`: if focus not blank and id absent, `throw new ServiceException("\u8bbe\u5907\u4e0d\u5b58\u5728", 500)`; else focus subgraph or full
- `getLink`: parse; if null or edge not in `buildGraph().getEdges()`, `throw new ServiceException("\u94fe\u8def\u4e0d\u5b58\u5728", 500)`

Controller:

```java
@RestController
@RequestMapping("/scene/topology")
public class SceneTopologyController extends BaseController
{
    @Autowired
    private ISceneTopologyService topologyService;

    @PreAuthorize("@ss.hasPermi('scene:device:list')")
    @GetMapping("/graph")
    public AjaxResult graph(@RequestParam(required = false) String focusDeviceId)
    {
        return success(topologyService.getGraph(focusDeviceId));
    }

    @PreAuthorize("@ss.hasPermi('scene:device:list')")
    @GetMapping("/edge/{edgeId}")
    public AjaxResult edge(@PathVariable String edgeId)
    {
        return success(topologyService.getLink(edgeId));
    }
}
```

No probe fields. No buildings REST API. No new menus.

- [ ] **Step 1: Implement JSON + catalog + service + controller.**
- [ ] **Step 2: Compile** `mvn -pl ruoyi-admin -am compile -DskipTests` (BUILD SUCCESS)
- [ ] **Step 3: Commit** `Add scene topology REST graph and link endpoints.`

---

### Task 3: Frontend request wrappers

**Files:**
- Modify: `ruoyi-ui/src/api/scene/topology.js`
- Create: `ruoyi-ui/tests/cesium/topology-api.test.js`
- Do not modify `Index.vue` or `views/cesium/topology/index.vue`
- Keep `utils/scene/topologyGraph.js` (`layoutLevels` still used by SVG; keep `buildGraph` for existing unit test)

Replace `topology.js` with:

```javascript
import request from '@/utils/request'
import { edgeId } from '@/utils/scene/topologyGraph'

export function getTopologyGraph(query) {
  return request({
    url: '/scene/topology/graph',
    method: 'get',
    params: query || {}
  })
}

function resolveEdgeId(edgeIdOrObj) {
  if (!edgeIdOrObj) return ''
  if (typeof edgeIdOrObj === 'string') return edgeIdOrObj
  if (edgeIdOrObj.edgeId) return edgeIdOrObj.edgeId
  if (edgeIdOrObj.fromDeviceId && edgeIdOrObj.toDeviceId) {
    return edgeId(edgeIdOrObj.fromDeviceId, edgeIdOrObj.toDeviceId)
  }
  return ''
}

export function getLinkDetail(edgeIdOrObj) {
  const id = resolveEdgeId(edgeIdOrObj)
  return request({
    url: '/scene/topology/edge/' + encodeURIComponent(id),
    method: 'get'
  })
}
```

Must not import `listDevices` or `getBuildings`.

`topology-api.test.js` reads source (same style as `probe-api.test.js`) and asserts:
- `import request from '@/utils/request'`
- exports `getTopologyGraph` and `getLinkDetail`
- urls `/scene/topology/graph` and `/scene/topology/edge/`
- does not import `@/api/scene/device` or `@/api/scene/buildings`

- [ ] **Step 1: Write test, run `node ruoyi-ui/tests/cesium/topology-api.test.js`, confirm fail.**
- [ ] **Step 2: Replace `topology.js`.**
- [ ] **Step 3: Run** `node ruoyi-ui/tests/cesium/topology-api.test.js` and `node ruoyi-ui/tests/cesium/topology-graph.test.js` (both OK)
- [ ] **Step 4: Commit** `Switch scene topology API client to backend request.`

---

### Task 4: Spec status

**Files:**
- Modify: `docs/superpowers/specs/2026-08-18-phase-e2-topology-backend-design.md` (keep UTF-8 BOM)

Set **status** line to: 已实现；计划见 docs/superpowers/plans/2026-08-18-phase-e2-topology-backend.md；浏览器手测待确认

Hand-test after backend restart:
1. Map show-network-links still highlights buildings and draws cross-building lines
2. Click a line: from/to names and building names appear
3. Topology page full graph; related mode hides unrelated branches
4. Node colors still come from `/scene/probe/list`
5. User without `scene:device:list` gets 403 on `/scene/topology/graph`

- [ ] **Step 1: Update spec status (UTF-8 BOM).**
- [ ] **Step 2: Commit** `Mark Phase E2 topology backend spec code-complete.`

---

## Self-Review (plan vs spec)

| Spec item | Task |
| --- | --- |
| GET graph +/- focusDeviceId | T2 |
| GET edge/{edgeId} | T2 |
| parentDeviceId edges, stable edge.id | T1 |
| buildingName from classpath JSON | T2 |
| no probe status on topology | T2, T3 |
| scene:device:list | T2 |
| frontend request wrappers, UI unchanged | T3 |
| hyphen device ids parse | T1 |
| device-not-found / link-not-found code 500 | T2 |
| no link table / no new menus | Global Constraints |

No TBD/TODO placeholders. Types: `SceneTopologyGraph` methods used by Task 2 match Task 1.

---

## Execution Handoff

Plan saved to `docs/superpowers/plans/2026-08-18-phase-e2-topology-backend.md`.

User already chose **Subagent-Driven** execution.
