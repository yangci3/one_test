# 阶段 E：网络链路 + 拓扑联动（前端 mock）设计

**日期：** 2026-08-01  
**状态：** 已实现；计划见 docs/superpowers/plans/2026-08-01-phase-e-topology-link.md；浏览器手测待确认  
**路线图：** `docs/superpowers/specs/2026-07-31-factory-scene-network-roadmap.md`  
**依赖：** 阶段 A 占位场景；阶段 B1 设备主数据（`parentDeviceId`）；阶段 C 监控闭环

## 1. 目标

地图与独立拓扑页共用同一套节点/边数据，实现可演示的链路与拓扑联动：

- 地图：选中设备→「显示网络链路」：相关建筑高亮 + 建筑中心连线 + 点击连线查详情
- 独立页「网络拓扑」：全部连接 / 与选中设备相关；SVG 轻量自绘

**落地策略：方案 1（共享拓扑模型 + 双视图）**

- 边先由 `parentDeviceId` 推导；API 形状预留若依，后续可换边表而 UI 不动
- 不引入 G6 / relation-graph 等重图库

**出口标准**

- 地图可进入/清除链路模式，相关建筑高亮且有连线
- 点击连线（或拓扑页边）可弹出链路详情
- 拓扑页可切换全量 / 选中邻域，与地图数据同源
- 支持 `?deviceId=` 预选相关模式

**不做（本阶段）**

- 边 CRUD UI、真物理布线、SNMP
- 重图库、B2 / MySQL、换厂区 tiles

## 2. 决策摘要

| 项 | 选择 |
| --- | --- |
| 范围 | 地图链路 + 独立拓扑页 |
| 边数据 | `parentDeviceId` 推导；预留边表 |
| 拓扑绘制 | SVG 轻量自绘 |
| 地图表现 | 高亮 + 连线 + 点线详情 |
| 邻域范围 | 焦点设备的**祖先链 + 全部子孙** |
| 链路高亮色 | 与监控红/黄/绿区分（如紫色半透明） |

## 3. 数据模型

### 3.1 节点 / 边

```json
{
  "nodes": [
    { "id": "dev-a-sw1", "name": "...", "ip": "...", "type": "switch", "buildingId": "bldg-a" }
  ],
  "edges": [
    { "id": "edge-dev-core-sw1-dev-a-sw1", "fromDeviceId": "dev-core-sw1", "toDeviceId": "dev-a-sw1" }
  ]
}
```

- 边方向：**父 → 子**（`parentDeviceId` 指向父，边为 parent -> child）
- `edge.id`：稳定规则 `edge-{fromDeviceId}-{toDeviceId}`
- 缺父、父不存在：不生成边

### 3.2 邻域子图

给定 `focusDeviceId`：

1. 收集节点：焦点自身 + 沿 `parentDeviceId` 向上的全部祖先 + 递归全部子孙
2. 边：两端均在上述节点集内的边

### 3.3 链路详情

```json
{
  "edgeId": "edge-...",
  "from": { "id", "name", "ip", "type", "buildingId", "buildingName" },
  "to": { "id", "name", "ip", "type", "buildingId", "buildingName" }
}
```

## 4. API（前端 mock）

| 方法 | 说明 |
| --- | --- |
| `getTopologyGraph({ focusDeviceId? })` | 无 focus：全图；有 focus：邻域子图 |
| `getLinkDetail(edgeId)` 或 `getLinkDetail({ fromDeviceId, toDeviceId })` | 链路详情 |

均返回 `{ code, msg, data }`。后续可换 `request('/scene/topology/...')`。

实现位置建议：

- `ruoyi-ui/src/utils/scene/topologyGraph.js` — 纯函数：设备列表 → nodes/edges、邻域过滤
- `ruoyi-ui/src/api/scene/topology.js` — Promise API + 填充 buildingName

## 5. 地图交互

- 入口：建筑设备行「显示网络链路」
- 进入链路模式：
  - 调 `getTopologyGraph({ focusDeviceId })`
  - 相关 `buildingId` 套用链路高亮色（暂时覆盖监控聚合色）
  - 按边的两端建筑 center 画 polyline（同建筑内设备边：可画短弧/或跳过仅保留详情入口；**本阶段：同楼边不画线，仅在详情/拓扑页可见**）
  - 点选 polyline → 链路详情弹窗
- 退出：「清除链路」或再次点同一按钮；移除 polyline，恢复监控聚合着色
- 链路模式下监控着色刷新：若仍处于链路模式，刷新后重施链路高亮（不被绿/红覆盖）

## 6. 拓扑页

- 路由：`/cesium/topology`，菜单「网络拓扑」
- 工具栏：模式（全部 / 与选中相关） + 设备下拉（相关模式必选）
- SVG：按层级排版（无父为根，子在下一层）；节点显示名称；边可点
- 详情弹窗与地图共用同一套字段
- Query：`?deviceId=xxx` 时自动切到「相关」并预选设备

## 7. 模块职责

| 路径 | 职责 |
| --- | --- |
| `utils/scene/topologyGraph.js` | buildGraph / focusSubgraph / layoutLevels |
| `api/scene/topology.js` | getTopologyGraph / getLinkDetail |
| `utils/cesium/linkOverlay.js`（或等价） | polyline 绘制/点选/清除；建筑高亮协调 |
| `views/cesium/Index.vue` | 链路模式入口、详情弹窗 |
| `views/cesium/topology/index.vue` | 拓扑页 SVG |
| `router/index.js` | 注册路由 |
| Node 测试 | 图构建、邻域过滤、无环 |

## 8. 错误与边界

- 焦点设备不存在：API 返 500 或空图 + 提示
- 无边设备（孤立节点）：仍可高亮其建筑，无连线
- 巡环 parent：B1 已禁；构图时若遇循环则中断并跳过该边

## 9. 测试要点

- seed 设备可生成稳定 edges
- focus 子图包含祖先与子孙，不包含无关分支
- 地图进出链路模式后着色恢复正常
- 拓扑页全量/相关切换与 API 一致

## 10. 下一步

用户确认本规格后，编写：  
`docs/superpowers/plans/2026-08-01-phase-e-topology-link.md`

