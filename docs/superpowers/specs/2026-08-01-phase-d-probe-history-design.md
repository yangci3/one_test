# 阶段 D：监控历史与详情（前端 mock）设计

**日期：** 2026-08-01  
**状态：** 已实现；计划见 docs/superpowers/plans/2026-08-01-phase-d-probe-history.md；浏览器手测待确认  
**路线图：** `docs/superpowers/specs/2026-07-31-factory-scene-network-roadmap.md`  
**依赖：** 阶段 C 最小监控闭环 + 全局监控运行时（`globalMonitorRuntime`）；阶段 B1 设备主数据

## 1. 目标

在无 MySQL、无真 ping 的前提下，记录设备监控期间的上下线事件，并提供可演示的查询界面：

- 地图侧栏「监控详情」弹窗（近 30 天 **时间轴**）
- 独立菜单页「监控历史」（近 30 天 **表格** + 设备筛选）

**落地策略：方案 1（独立事件日志 store）**

- `localStorage` key=`ruoyi.scene.probeHistory`，与 `probe` 运行态、`devices`、`monitorSettings` 分离
- API 形状预留若依；本阶段不落库；后续可整段替换为 `request('/scene/probe/history')`

**出口标准**

- mock 探测产生的 `online↔offline` 可被查询
- 启动监控写入一条 `online` 起点事件
- 地图弹窗时间轴与历史页表格数据同源
- 删除设备时同步清理该设备历史
- 刷新后近 30 天内记录仍在（未清浏览器存储）

**不做（本阶段）**

- MySQL / B2、真 ICMP ping
- 统计图表、导出 Excel
- 链路 / 拓扑（阶段 E）
- 关闭监控写 `unknown` 历史事件

## 2. 决策摘要

| 项 | 选择 |
| --- | --- |
| 存储 | `ruoyi.scene.probeHistory`（前端） |
| 后端切换 | API 形状先 mock，再换若依 |
| 地图入口 | 设备行「监控详情」→ 弹窗 Timeline |
| 独立页 | 菜单「监控历史」→ 表格 + 设备筛选 |
| 时间窗 | 默认近 30 天（查询与落盘裁剪一致） |
| 容量保护 | 全局最多约 5000 条；超限删最旧 |

## 3. 数据模型

单条事件：

```json
{
  "id": "ph-1720000000000-a1b2",
  "deviceId": "dev-core-sw1",
  "type": "offline",
  "at": 1720000000000
}
```

| 字段 | 说明 |
| --- | --- |
| `id` | 唯一 id（时间戳 + 短随机即可） |
| `deviceId` | 设备 id，与 B1 一致 |
| `type` | 仅 `online` / `offline` |
| `at` | 事件发生时间，毫秒时间戳 |

存储结构建议：

```json
{
  "events": [
    { "id": "ph-...", "deviceId": "dev-core-sw1", "type": "offline", "at": 1720000000000 }
  ]
}
```

**裁剪规则（写入后执行）**

1. 删除 `at < now - 30天` 的事件  
2. 若仍超过 `MAX_EVENTS`（**5000**），按 `at` 升序删除最旧直至 ≤ 上限  

## 4. 写入时机

由全局监控路径统一写入（避免地图页销毁后漏记）：

| 场景 | 是否写入 | 事件类型 |
| --- | --- | --- |
| 启动监控（单台/全部）且状态变为 `online` | 是 | `online` |
| mock 引擎 `online → offline` | 是 | `offline` |
| mock 引擎 `offline → online` | 是 | `online` |
| 关闭监控 → `unknown` | **否** | — |
| 未监控设备 | **否** | — |

实现挂钩点：

- `globalMonitorRuntime` 的 `handleProbeChanges`（状态翻转）
- `probeStore.setMonitoring(true, …)` 或 API `startDevice` / `startAll` 成功后首次置 `online` 处（与现有 Phase C 行为对齐，保证只写一次 `online` 起点）

同一毫秒多设备翻转：每条事件独立 `id`，均可写入。

## 5. 模块职责

| 路径 | 职责 |
| --- | --- |
| `ruoyi-ui/src/utils/scene/probeHistoryStore.js` | 读写 localStorage；append；按设备/时间查询；裁剪；删设备清理 |
| `ruoyi-ui/src/api/scene/probeHistory.js` | `listProbeHistory` / `getDeviceProbeHistory`；返回 `{ code, msg, data }` |
| `ruoyi-ui/src/utils/scene/globalMonitorRuntime.js`（及必要的 probe API） | 状态翻转 / 启动监控时调用 append |
| `ruoyi-ui/src/views/cesium/Index.vue` | 设备行「监控详情」按钮 + Timeline 弹窗 |
| `ruoyi-ui/src/views/cesium/probeHistory/index.vue` | 独立历史页：筛选 + 表格 |
| `ruoyi-ui/src/router/index.js` | 注册 `/cesium/probe-history` |
| `ruoyi-ui/src/utils/scene/deviceStore.js`（或 device API 删除路径） | 删除设备时 `removeDeviceHistory(deviceId)` |
| Node 小测（可选） | 裁剪、30 天过滤、append 顺序 |

## 6. API（前端 mock）

均返回 Promise，形状与若依一致。

### 6.1 `listProbeHistory(query)`

```js
query = {
  deviceId?: string,
  from?: number,  // 默认 now - 30d
  to?: number     // 默认 now
}
// data: Array<event & { deviceName?: string }> 按 at 倒序
```

独立页表格使用；可附带 `deviceName`（由 API 层用 `deviceStore` 填充，便于展示）。

### 6.2 `getDeviceProbeHistory(deviceId)`

```js
// data: { deviceId, deviceName?, events: Event[] }  // events 近 30 天，at 倒序
```

地图弹窗使用。

后续切换：两函数内部改为 `request({ url: '/scene/probe/history', ... })`，参数与字段保持不变。

## 7. UI 规格

### 7.1 地图「监控详情」

- 位置：选中建筑后，设备列表每行操作区增加「监控详情」
- 弹窗标题：`监控详情 - {设备名}`
- 摘要行：IP、类型、当前监控状态（只读）
- 主体：`el-timeline`，节点文案如「离线 / 上线」+ 本地化时间
- 无数据：空状态提示「近 30 天暂无上下线记录」

### 7.2 独立页「监控历史」

- 菜单：三维地图下「监控历史」（与设备管理、网络监控设置并列）
- 筛选：设备下拉（全部 + 各设备）、可选不再加日期控件（默认近 30 天即可）
- 表格列：时间、设备名称、IP、事件（上线/离线）
- 支持路由 query：`?deviceId=xxx` 预选设备（便于从地图跳转时扩展；本阶段弹窗为主，跳转可选）

## 8. 错误与边界

- localStorage 配额失败：append 失败时尽量裁剪后再试一次；仍失败则静默跳过本次写入（不阻断监控与告警）
- 设备已删但历史仍在：独立页展示时 `deviceName` 回退为 `deviceId`；正常删除路径应已清理
- 时区：展示用浏览器本地时间格式化

## 9. 测试要点

- append 后 `list` / `getDevice` 能查到，且按 `at` 倒序
- 超过 30 天与超过 5000 条会被裁剪
- 关闭监控不产生历史；启动监控产生 `online`
- 删除设备后该 `deviceId` 无残留事件
- 地图 Timeline 与历史页表格同源

## 10. 与后续阶段关系

| 后续 | 关系 |
| --- | --- |
| B2 / 真 ping | 仅替换写入源与 `probeHistory` API 实现；UI 可不动 |
| 阶段 E | 历史页不承担链路/拓扑；设备上下级仍来自 B1 |

## 11. 下一步

用户确认本规格文件后，编写实现计划：  
`docs/superpowers/plans/2026-08-01-phase-d-probe-history.md`

