# 阶段 E2：拓扑/链路 API 后端化

**日期：** 2026-08-18  
**状态：** 已实现；计划见 docs/superpowers/plans/2026-08-18-phase-e2-topology-backend.md；浏览器手测待确认  
**路线图：** `docs/superpowers/specs/2026-07-31-factory-scene-network-roadmap.md`  
**依赖：** 阶段 E 前端链路/拓扑 UI；阶段 B2′ `scene_device`（含 `parent_device_id`）  
**关联：** `docs/superpowers/specs/2026-08-01-phase-e-topology-link-design.md`（前端 mock，已实现）

## 1. 目标

把阶段 E 里前端本地拼图的拓扑 API 迁到后端，与 MySQL 设备库同源。地图「显示网络链路」与「网络拓扑」页仍共用同一套图，**交互与着色行为不变**。

**落地策略：方案 A（独立 TopologyController + TopologyService，图算法在 Java）**

**出口标准**

1. `GET /scene/topology/graph`：无 `focusDeviceId` 返回全图；有则返回邻域子图（焦点 + 全部祖先 + 全部子孙）
2. `GET /scene/topology/edge/{edgeId}`：返回链路两端设备（含 `buildingName`）
3. 边仍由 `parentDeviceId` 推导（父 → 子）；`edge.id` = `edge-{fromDeviceId}-{toDeviceId}`
4. 节点由后端填充 `buildingName`；拓扑接口**不**返回 probe 在线状态
5. 权限复用 `scene:device:list`；无该权限 403
6. 前端 `api/scene/topology.js` 改为 `request(...)`；`Index.vue` / `topology/index.vue` 不改交互

**不做**

- 独立链路表、边 CRUD、SNMP、物理布线
- 拓扑接口内嵌 online/offline（继续用 `/scene/probe/list`）
- 新建 `scene:topology:*` 菜单权限
- 楼栋 CRUD / 把 `buildings.json` 整包迁成业务 API（本阶段只把 **id→name** 放到后端供拼名）
- 改地图连线样式、同楼边策略、拓扑 SVG `layoutLevels`
- 换厂区 tiles（P3）

## 2. 决策摘要

| 项 | 选择 |
| --- | --- |
| 边来源 | `parentDeviceId` 推导，不新建边表 |
| API | 图 + 边详情（对齐现 `topology.js`） |
| 节点状态 | 不放入拓扑响应；前端继续 `listProbeStatus` |
| 楼栋名 | 后端拼；楼栋目录用 classpath 静态 JSON（与现 `buildings.json` 的 id/name 一致） |
| 权限 | `@PreAuthorize` `scene:device:list` |
| 模块 | 新建 Controller + Service，不挂到 DeviceController |
| 图算法 | Java 实现对拍现 `topologyGraph.js` 的 `buildGraph` / `focusSubgraph` |
| 前端布局 | `layoutLevels` 仍留在前端 |

## 3. 数据与响应形状

与阶段 E 对齐，字段名用前端已有名称（`id` / `name` / `type`），不要直接把 `SceneDevice` 的 `deviceId`/`deviceName`/`deviceType` 吐出去。

### 3.1 图

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "nodes": [
      {
        "id": "dev-a-sw1",
        "name": "...",
        "ip": "...",
        "type": "switch",
        "buildingId": "bldg-a",
        "buildingName": "车间 A",
        "parentDeviceId": "dev-core-sw1"
      }
    ],
    "edges": [
      {
        "id": "edge-dev-core-sw1-dev-a-sw1",
        "fromDeviceId": "dev-core-sw1",
        "toDeviceId": "dev-a-sw1"
      }
    ]
  }
}
```

规则：

- 只包含 `del_flag = '0'` 的设备
- 边方向：父 → 子（`parentDeviceId` 指向父）
- 无父、父不存在、父=自己：不生成边
- 构图时若遇 `parent` 环：向上走祖先时用 visited 切断（与现 JS 一致），不抛 500
- `buildingName`：按 `buildingId` 查静态目录；查不到则回退为 `buildingId`；无 `buildingId` 则为 `""`

### 3.2 邻域子图

给定 `focusDeviceId`：

1. 节点：焦点 + 沿 `parentDeviceId` 向上的全部祖先 + 沿边向下的全部子孙
2. 边：两端均在上述节点集内
3. 焦点设备不存在（不在未删除设备中）：`code=500`，`msg=设备不存在`，`data=null`（对齐现前端 mock）

### 3.3 链路详情

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "edgeId": "edge-dev-core-sw1-dev-a-sw1",
    "from": { "id": "...", "name": "...", "ip": "...", "type": "...", "buildingId": "...", "buildingName": "...", "parentDeviceId": "..." },
    "to":   { "id": "...", "name": "...", "ip": "...", "type": "...", "buildingId": "...", "buildingName": "...", "parentDeviceId": "..." }
  }
}
```

- 边必须是「当前设备库能推导出的边」；否则 `code=500`，`msg=链路不存在`
- `edge.id` 里设备 ID 自身含 `-`（如 `dev-a-sw1`），**禁止**按第一个 `-` 切开；解析时对照已知设备 ID 匹配（与现 `parseEdgeKey` 同策略）

## 4. API

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/scene/topology/graph` | `scene:device:list` | 可选 `focusDeviceId` |
| GET | `/scene/topology/edge/{edgeId}` | `scene:device:list` | 链路详情 |

统一 `{ code, msg, data }`。登录态与其它 `/scene/**` 相同。

前端封装保持函数名不变：

- `getTopologyGraph({ focusDeviceId? })` → `GET /scene/topology/graph`
- `getLinkDetail(edgeIdOrObj)` → 先得到 `edgeId` 字符串，再 `GET /scene/topology/edge/{edgeId}`（调用方目前传的都是字符串）

## 5. 模块划分

**后端（新建）**

| 单元 | 职责 |
| --- | --- |
| `SceneTopologyController` | 两个 GET；`@PreAuthorize("scene:device:list")` |
| `ISceneTopologyService` / Impl | 读设备、构图、邻域、边详情、拼楼栋名 |
| `SceneTopologyGraph`（纯函数） | `buildGraph` / `focusSubgraph` / `edgeId` / 解析 edgeId；无 Spring / 无 DB |
| 拓扑 VO | `SceneTopologyNode` / `SceneTopologyEdge` / `SceneTopologyGraphVo` / `SceneTopologyLinkVo` |
| 楼栋名目录 | classpath 静态 JSON（id→name，与 `ruoyi-ui/src/api/scene/buildings.json` 一致）；不新建表 |

读设备：复用 `SceneDeviceMapper.selectSceneDeviceList`（或等价未删除列表），不改设备表结构。

**前端**

| 单元 | 变化 |
| --- | --- |
| `api/scene/topology.js` | 改为 `request`；去掉 `listDevices` + 本地构图 |
| `utils/scene/topologyGraph.js` | **保留** `layoutLevels`（拓扑页 SVG 仍用）；`buildGraph`/`focusSubgraph` 可留作对照测试，运行时 API 不再调用 |
| `Index.vue` / `topology/index.vue` | 不改交互；仍并行 `listProbeStatus` 上色 |
| `api/scene/buildings.js` | 地图建筑盒子仍用；拓扑拼名不再依赖它 |

## 6. 错误与边界

| 情况 | 行为 |
| --- | --- |
| 未登录 | 401（现有安全链） |
| 无 `scene:device:list` | 403 |
| `focusDeviceId` 不存在 | 500 / `设备不存在` |
| `edgeId` 无法解析或不对应真实父子边 | 500 / `链路不存在` |
| 全库无设备 | 200，空 `nodes`/`edges` |
| 孤立节点（无边） | 仍出现在 nodes 中 |
| 未知 `buildingId` | `buildingName` = `buildingId` |

不在拓扑接口里吞掉 probe 异常：probe 失败只影响状态点，不影响出图。

## 7. 测试要点

- Java：seed 设备生成稳定 `edge.id`；缺父不生成边；自环不生成边
- Java：focus 子图含祖先+子孙，不含无关分支；未知 focus → 业务 500
- Java：含 `-` 的设备 ID 能正确解析 `edge-{from}-{to}`
- Java：`buildingName` 命中静态目录；未知 id 回退
- 前端：`topology.js` 请求 URL/方法与上表一致
- 手测：地图链路模式、拓扑页全量/相关、点边详情；节点红绿仍来自 probe

## 8. 下一步

用户确认本规格后，编写：  
`docs/superpowers/plans/2026-08-18-phase-e2-topology-backend.md`
