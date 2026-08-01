# Phase B2 Device MySQL + MAC Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist scene devices in MySQL (`scene_device` with MAC), expose `/scene/device` CRUD, switch frontend `device.js` to `request`, add MAC to UI.

**Architecture:** Standard RuoYi layers (domain/mapper/service/controller). JSON API keeps B1 camelCase (`id/name/ip/mac/type/buildingId/parentDeviceId`). Soft-delete + IP/MAC unique. Frontend constantRoutes unchanged; menu SQL grants admin perms.

**Tech Stack:** Spring Boot + MyBatis + MySQL (`ry-vue`) + Vue 2 + axios `request`

**Spec:** `docs/superpowers/specs/2026-08-01-phase-b2-device-mysql-design.md`

## Global Constraints

- Table `scene_device`; PK string `device_id`; soft delete `del_flag` 0/2
- MAC optional; empty -> NULL; normalize `AA:BB:CC:DD:EE:FF`; unique when set
- IP required unique among non-deleted rows
- Parent cycle check; delete clears children `parent_device_id`
- `GET /scene/device/list` returns `AjaxResult` with full array in `data` (not paginated TableDataInfo)
- JSON field names: id, name, ip, mac, type, buildingId, parentDeviceId, remark
- Permissions: `scene:device:list|query|add|edit|remove`
- Do NOT migrate probe/history/settings; no real ping
- Docs UTF-8 BOM; JS UI labels prefer \uXXXX if encoding risk
- DB: `jdbc:mysql://localhost:3306/ry-vue` via `application-druid.yml`

---

## File Structure

| Path | Role |
| --- | --- |
| `sql/scene_device.sql` | DDL + seed + menu + role_menu |
| `ruoyi-system/.../domain/SceneDevice.java` | Entity + Jackson aliases for API |
| `ruoyi-system/.../mapper/SceneDeviceMapper.java` | Mapper interface |
| `ruoyi-system/.../resources/mapper/scene/SceneDeviceMapper.xml` | SQL |
| `ruoyi-system/.../service/ISceneDeviceService.java` | Service API |
| `ruoyi-system/.../service/impl/SceneDeviceServiceImpl.java` | Validate + CRUD |
| `ruoyi-admin/.../controller/scene/SceneDeviceController.java` | REST |
| `ruoyi-ui/src/api/scene/device.js` | Switch to request |
| `ruoyi-ui/src/api/scene/devices.seed.json` | Add mac (reference) |
| `ruoyi-ui/src/utils/scene/deviceStore.js` | Keep validators for tests; stop as runtime source |
| `ruoyi-ui/src/views/cesium/device/index.vue` | MAC column/form |
| `ruoyi-ui/src/views/cesium/Index.vue` | MAC in device dialog |
| `ruoyi-ui/src/views/cesium/topology/index.vue` | MAC in detail table |
| `ruoyi-ui/tests/cesium/device-mac.test.js` | MAC normalize/validate helpers |

---

### Task 1: SQL schema, seed, menus

**Files:**
- Create: `sql/scene_device.sql`

**Interfaces:**
- Produces: table `scene_device`; 7 seed rows; menu ids `2100-2106`; admin role_menu for those ids

- [ ] **Step 1: Create `sql/scene_device.sql`**

Menu parent `2100` (directory M) for scene group; child `2101` device page component `cesium/device/index`; buttons `2102-2106` for list/query/add/edit/remove. Grant `sys_role_menu` for `role_id=1`.

```sql
drop table if exists scene_device;
create table scene_device (
  device_id         varchar(64)   not null comment 'device id',
  device_name       varchar(100)  not null,
  ip                varchar(45)   not null,
  mac               varchar(17)   default null,
  device_type       varchar(20)   not null,
  building_id       varchar(64)   not null,
  parent_device_id  varchar(64)   default null,
  del_flag          char(1)       default '0',
  create_by         varchar(64)   default '',
  create_time       datetime,
  update_by         varchar(64)   default '',
  update_time       datetime,
  remark            varchar(500)  default null,
  primary key (device_id),
  unique key uk_scene_device_ip (ip),
  unique key uk_scene_device_mac (mac)
) engine=innodb comment='scene device';

-- seed 7 devices mac AA:BB:CC:DD:EE:01 .. 07
-- ids: dev-core-sw1, dev-core-router1, dev-a-sw1, dev-a-term1,
--      dev-b-sw1, dev-c-term1, dev-d-router1
-- insert sys_menu 2100..2106 + sys_role_menu role_id=1
```

- [ ] **Step 2: Apply SQL to `ry-vue`**

Run via MySQL client against `ry-vue`. Expected: `select count(*) from scene_device` = 7.

- [ ] **Step 3: Commit**

```bash
git add sql/scene_device.sql
git commit -m "Add scene_device schema, seed, and menu SQL."
```

---

### Task 2: Domain + Mapper

**Files:**
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/domain/SceneDevice.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/mapper/SceneDeviceMapper.java`
- Create: `ruoyi-system/src/main/resources/mapper/scene/SceneDeviceMapper.xml`

**Interfaces:**
- Produces: `SceneDevice` with Jackson `@JsonProperty` mapping:
  - deviceId <-> id
  - deviceName <-> name
  - deviceType <-> type
  - buildingId, parentDeviceId, ip, mac, remark (same names)
- Mapper: `selectSceneDeviceList`, `selectSceneDeviceById`, `insertSceneDevice`, `updateSceneDevice`, `checkIpUnique`, `checkMacUnique`, `updateParentNullByParentId`, `deleteSceneDeviceByIds` (set del_flag=2)

- [ ] **Step 1: Implement `SceneDevice` extending `BaseEntity`**

Fields: `deviceId`, `deviceName`, `ip`, `mac`, `deviceType`, `buildingId`, `parentDeviceId`, `delFlag`.
Add convenience setters for GET query binding: `setName`->deviceName, `setType`->deviceType, `setId`->deviceId.

- [ ] **Step 2: Mapper interface + XML under `mapper/scene/`**

List filters: deviceName/ip/mac like; deviceType/buildingId eq; del_flag='0'.

```xml
<update id="deleteSceneDeviceByIds">
  update scene_device set del_flag = '2' where device_id in
  <foreach collection="array" item="id" open="(" separator="," close=")">#{id}</foreach>
</update>
```

- [ ] **Step 3: Commit**

```bash
git add ruoyi-system/src/main/java/com/ruoyi/system/domain/SceneDevice.java \
  ruoyi-system/src/main/java/com/ruoyi/system/mapper/SceneDeviceMapper.java \
  ruoyi-system/src/main/resources/mapper/scene/SceneDeviceMapper.xml
git commit -m "Add SceneDevice domain and MyBatis mapper."
```

---

### Task 3: Service validation + CRUD

**Files:**
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/service/ISceneDeviceService.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/service/impl/SceneDeviceServiceImpl.java`

**Interfaces:**
- `List<SceneDevice> selectSceneDeviceList(SceneDevice q)`
- `SceneDevice selectSceneDeviceById(String deviceId)`
- `int insertSceneDevice(SceneDevice d)`
- `int updateSceneDevice(SceneDevice d)`
- `int deleteSceneDeviceByIds(String[] deviceIds)`
- Throws `ServiceException` on validation failure

- [ ] **Step 1: MAC/IP helpers (private static)**

```java
private static final Pattern MAC = Pattern.compile("(?i)^([0-9A-F]{2}[:-]){5}([0-9A-F]{2})$");
private static final Pattern IP = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");
// blank mac -> null; valid -> AA:BB:CC:DD:EE:FF; invalid -> ServiceException
```

- [ ] **Step 2: validateDevice before insert/update**

1. name/ip/type/buildingId required
2. type in router|switch|terminal|other
3. IP format + unique (exclude self on update); del_flag=0 only
4. MAC normalize; unique if non-null
5. parent exists, not self, no cycle
6. insert: if deviceId blank, set `dev-` + System.currentTimeMillis()

- [ ] **Step 3: deleteSceneDeviceByIds**

Soft delete each id; call `updateParentNullByParentId(id)`.

- [ ] **Step 4: Commit**

```bash
git add ruoyi-system/src/main/java/com/ruoyi/system/service/ISceneDeviceService.java \
  ruoyi-system/src/main/java/com/ruoyi/system/service/impl/SceneDeviceServiceImpl.java
git commit -m "Add SceneDeviceService with IP/MAC/parent validation."
```

---

### Task 4: Controller

**Files:**
- Create: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/scene/SceneDeviceController.java`

**Interfaces:**
- Consumes: `ISceneDeviceService`
- REST `/scene/device`

- [ ] **Step 1: Implement controller**

```java
@RestController
@RequestMapping("/scene/device")
public class SceneDeviceController extends BaseController {
  @PreAuthorize("@ss.hasPermi('scene:device:list')")
  @GetMapping("/list")
  public AjaxResult list(SceneDevice query) {
    return success(deviceService.selectSceneDeviceList(query));
  }
  // getInfo / add / edit / remove: String ids, same PreAuthorize pattern as SysPost
}
```

GET query binding: convenience setters `setName`/`setType`/`setId` on entity (Task 2).

- [ ] **Step 2: Restart backend; smoke GET**

Login admin, `GET /scene/device/list` with token. Expected: code 200, data length 7.

- [ ] **Step 3: Commit**

```bash
git add ruoyi-admin/src/main/java/com/ruoyi/web/controller/scene/SceneDeviceController.java
git commit -m "Expose /scene/device REST CRUD."
```

---

### Task 5: Frontend API + MAC helpers tests

**Files:**
- Modify: `ruoyi-ui/src/api/scene/device.js`
- Modify: `ruoyi-ui/src/utils/scene/deviceStore.js`
- Modify: `ruoyi-ui/src/api/scene/devices.seed.json`
- Create: `ruoyi-ui/tests/cesium/device-mac.test.js`
- Modify: `ruoyi-ui/tests/cesium/device-store.test.js` and `device-api.test.js` as needed

**Interfaces:**
- Keep exports: `listDevices`, `getDevice`, `addDevice`, `updateDevice`, `delDevice`
- Add: `normalizeMac(input)` -> string|null; `isValidMac(input)` -> boolean (blank true)

- [ ] **Step 1: Write failing MAC tests in `device-mac.test.js`**

```js
const assert = require('assert')
const { normalizeMac, isValidMac } = require('../../src/utils/scene/deviceStore')
assert.strictEqual(normalizeMac('aa-bb-cc-dd-ee-ff'), 'AA:BB:CC:DD:EE:FF')
assert.strictEqual(normalizeMac(''), null)
assert.strictEqual(isValidMac(''), true)
assert.strictEqual(isValidMac('bad'), false)
```

- [ ] **Step 2: Implement helpers; run tests until PASS**

- [ ] **Step 3: Rewrite `device.js` to use request**

```js
import request from '@/utils/request'
export function listDevices(query) {
  return request({ url: '/scene/device/list', method: 'get', params: query })
}
export function getDevice(id) {
  return request({ url: '/scene/device/' + id, method: 'get' })
}
export function addDevice(data) {
  return request({ url: '/scene/device', method: 'post', data })
}
export function updateDevice(data) {
  return request({ url: '/scene/device', method: 'put', data })
}
export function delDevice(id) {
  return request({ url: '/scene/device/' + id, method: 'delete' })
}
```

Adjust unit tests that assumed localStorage API (mock request or narrow to store validators).

- [ ] **Step 4: Run node tests**

```bash
cd ruoyi-ui
node tests/cesium/device-mac.test.js
node tests/cesium/device-store.test.js
```

- [ ] **Step 5: Commit**

```bash
git add ruoyi-ui/src/api/scene/device.js ruoyi-ui/src/utils/scene/deviceStore.js \
  ruoyi-ui/src/api/scene/devices.seed.json ruoyi-ui/tests/cesium/
git commit -m "Switch device API to backend and add MAC helpers."
```

---

### Task 6: UI MAC fields

**Files:**
- Modify: `ruoyi-ui/src/views/cesium/device/index.vue`
- Modify: `ruoyi-ui/src/views/cesium/Index.vue`
- Modify: `ruoyi-ui/src/views/cesium/topology/index.vue`

- [ ] **Step 1: Device management page -- column + form + optional query for MAC**
- [ ] **Step 2: Map Index device dialog -- field `mac`**
- [ ] **Step 3: Topology detail table -- MAC column**
- [ ] **Step 4: Browser check**

1. Login admin; device page shows 7 rows with MAC
2. Edit MAC; refresh persists
3. Duplicate MAC shows error
4. Map devices + monitor start still work
5. Optional clear `localStorage` key `ruoyi.scene.devices`

- [ ] **Step 5: Commit**

```bash
git add ruoyi-ui/src/views/cesium/device/index.vue \
  ruoyi-ui/src/views/cesium/Index.vue \
  ruoyi-ui/src/views/cesium/topology/index.vue
git commit -m "Show and edit device MAC in scene UIs."
```

---

### Task 7: Verify + update spec status

**Files:**
- Modify: `docs/superpowers/specs/2026-08-01-phase-b2-device-mysql-design.md`

- [ ] **Step 1: Acceptance checklist from spec section 7**
- [ ] **Step 2: Set spec status to implemented; link this plan**
- [ ] **Step 3: Commit docs**

```bash
git add docs/superpowers/specs/2026-08-01-phase-b2-device-mysql-design.md \
  docs/superpowers/plans/2026-08-01-phase-b2-device-mysql.md
git commit -m "Mark Phase B2 plan complete / implemented."
```

---

## Plan self-review

- Spec coverage: table/MAC/REST/frontend/menus -- Tasks 1-7
- Out of scope omitted: probe/history/ping/tiles
- list = AjaxResult array -- Task 4
- Field mapping id/name/type -- Tasks 2 and 4
- Admin perms via role_menu -- Task 1

