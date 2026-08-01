# 阶段 B2′：设备主数据 MySQL 入库 + MAC 设计

**日期：** 2026-08-01  
**状态：** 已实现；计划见 docs/superpowers/plans/2026-08-01-phase-b2-device-mysql.md；浏览器手测待确认  
**路线图：** `docs/superpowers/specs/2026-07-31-factory-scene-network-roadmap.md`  
**依赖：** 阶段 B1（前端 mock 设备）；本机 MySQL 库 `ry-vue`（`application-druid.yml`）  
**关联：** `docs/superpowers/specs/2026-08-01-phase-b-device-master-data-design.md` 第 11 节 B2 预留

## 1. 目标

将设备主数据从浏览器 `localStorage` 迁移到若依后端 + MySQL，并增加 **MAC 地址** 字段，为后续局域网真 ping、多端一致数据打底。

**落地策略**

- 表 `scene_device` + 标准若依 CRUD（domain/mapper/service/controller）
- 前端 `device.js` 改为 `request`；**设备管理页 / 地图弹窗 / 拓扑读设备列表的 UI 结构不变**，仅增 MAC 展示与表单项
- API 外部 JSON 仍用 camelCase 业务字段名（与 B1 一致）

**出口标准**

- 执行 SQL 后库中有种子设备（含示例 MAC）
- 设备管理页：增删改查；IP / MAC（有值时）唯一校验生效
- 地图与管理页数据一致；刷新后仍来自数据库
- 监控 / 拓扑 / 历史仍能通过 `listDevices` 拿到设备（源改为后端）

**不做（本阶段）**

- 探测状态、历史、监控设置落库（留 P1/P2）
- 真 ICMP ping（留 P4）
- 换厂区 tiles（留 P3）
- SNMP、边表 CRUD

## 2. 决策摘要

| 项 | 选择 |
| --- | --- |
| 范围 | 仅设备主数据 + MAC |
| 主键 | 字符串 `device_id`，兼容现有 `dev-*` |
| 删除 | 软删 `del_flag`（0 正常 / 2 删除） |
| MAC | 可空；有值则规范化并全局唯一 |
| 校验权威 | 后端；前端可作表单预检 |
| 菜单 | SQL 写入 `sys_menu` + 权限字；与现有 constantRoutes 并存 |
| localStorage | 切换后不再作设备主源；可提示清除 `ruoyi.scene.devices` |

## 3. 数据模型

### 3.1 API JSON

```json
{
  "id": "dev-core-sw1",
  "name": "核心交换机",
  "ip": "192.168.1.1",
  "mac": "AA:BB:CC:DD:EE:01",
  "type": "switch",
  "buildingId": "bldg-core",
  "parentDeviceId": null,
  "remark": ""
}
```

| 字段 | 说明 |
| --- | --- |
| id | 字符串；新增可由前端生成 `dev-` + 时间戳 |
| name | 必填 |
| ip | 必填；IPv4；未删除行全局唯一 |
| mac | 可空；有值规范为 `AA:BB:CC:DD:EE:FF`；全局唯一 |
| type | router / switch / terminal / other |
| buildingId | 必填 |
| parentDeviceId | 可空；无环 |
| remark | 可空 |

### 3.2 表 `scene_device`

```sql
create table scene_device (
  device_id         varchar(64)   not null                   comment '设备ID',
  device_name       varchar(100)  not null                   comment '设备名称',
  ip                varchar(45)   not null                   comment 'IP地址',
  mac               varchar(17)   default null               comment 'MAC地址',
  device_type       varchar(20)   not null                   comment '设备类型',
  building_id       varchar(64)   not null                   comment '所属建筑ID',
  parent_device_id  varchar(64)   default null               comment '上级设备ID',
  del_flag          char(1)       default '0'                comment '删除标志（0存在 2删除）',
  create_by         varchar(64)   default ''                 comment '创建者',
  create_time       datetime                                 comment '创建时间',
  update_by         varchar(64)   default ''                 comment '更新者',
  update_time       datetime                                 comment '更新时间',
  remark            varchar(500)  default null               comment '备注',
  primary key (device_id),
  unique key uk_scene_device_ip (ip),
  unique key uk_scene_device_mac (mac)
) engine=innodb comment = '场景设备主数据';
```

**MAC 唯一与空值：** MySQL 多行 `mac = NULL` 不违反 UNIQUE；空串转 NULL。

**种子：** 与 B1 七台一致，示例 MAC 自 `AA:BB:CC:DD:EE:01` 递增。

## 4. 后端架构

```text
ruoyi-system/.../domain/SceneDevice.java
ruoyi-system/.../mapper/SceneDeviceMapper.java
ruoyi-system/.../resources/mapper/scene/SceneDeviceMapper.xml
ruoyi-system/.../service/ISceneDeviceService.java
ruoyi-system/.../service/impl/SceneDeviceServiceImpl.java
ruoyi-admin/.../controller/scene/SceneDeviceController.java
sql/scene_device.sql
```

### 4.1 REST

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| GET | `/scene/device/list` | `scene:device:list` |
| GET | `/scene/device/{deviceId}` | `scene:device:query` |
| POST | `/scene/device` | `scene:device:add` |
| PUT | `/scene/device` | `scene:device:edit` |
| DELETE | `/scene/device/{deviceIds}` | `scene:device:remove` |

- `list` 支持 buildingId/name/ip/type/mac；仅 del_flag=0
- **建议：** `AjaxResult.success(list)` 全量数组，与现有 `listDevices` 套接
- Domain 内部 deviceId/deviceName；对外映射 id/name/type/buildingId/parentDeviceId/mac

### 4.2 校验

1. name/ip/type/buildingId 必填
2. IP 格式与唯一
3. MAC 空->NULL；非空规范+格式+唯一
4. parent 存在、非自指、无环
5. 软删；子设备 parent 置 NULL

## 5. 前端

| 文件 | 变更 |
| --- | --- |
| `api/scene/device.js` | 改 request；保持导出函数名 |
| `views/cesium/device/index.vue` | 表单/表格增 MAC |
| `views/cesium/Index.vue` | 弹窗增 MAC |
| `views/cesium/topology/index.vue` | 详情表增 MAC |
| `utils/scene/deviceStore.js` | 校验可留作单测；不再主存储 |
| `devices.seed.json` | 补 MAC（对照） |

## 6. 菜单与权限

- 权限字：`scene:device:list|query|add|edit|remove`
- admin 角色赋予全部 `scene:device:*`，避免 403
- 菜单 SQL 与现有三维地图 constantRoutes 并存

## 7. 测试要点

1. 执行 `sql/scene_device.sql`
2. 设备管理可见种子
3. MAC/IP 校验
4. 上级与环
5. 删除与子 parent
6. 地图与管理页一致；F5
7. 监控能加载后端设备名单

## 8. 与总计划

| 后续 | 依赖 |
| --- | --- |
| P1 后端模拟探测 | 读 scene_device |
| P2 历史/设置 | 设备 id |
| P4 真 ping | 同上 |
| P5 迁移说明书 | 含本 SQL |

## 9. 风险

| 风险 | 缓解 |
| --- | --- |
| 403 | admin 赋权 + 登录会话 |
| MAC 空串 | 转 NULL |
| 双源 | 禁用 localStorage 主写入 |
| 字段名 | DTO 映射 camelCase |

## 10. 自审

- [x] 无 TBD
- [x] 仅增 mac
- [x] 不含探测/历史/真 ping
- [x] list 返回形状已写明

