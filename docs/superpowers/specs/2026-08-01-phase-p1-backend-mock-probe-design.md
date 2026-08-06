# 阶段 P1：后端模拟探测设计

**日期：** 2026-08-01  
**状态：** 已实现；计划见 docs/superpowers/plans/2026-08-01-phase-p1-backend-mock-probe.md；浏览器手测待确认  
**路线图：** `docs/superpowers/specs/2026-07-31-factory-scene-network-roadmap.md`  
**依赖：** 阶段 B2′（`scene_device`）；阶段 C 前端监控闭环  
**关联：** `docs/superpowers/specs/2026-08-01-phase-c-min-monitor-loop-design.md`

## 1. 目标

将探测名单与在线/离线状态迁到若依后端；探测仍为**模拟翻转**（非真 ping）。前端告警、建筑着色、地图开关等 UI 尽量不变。

**落地策略**

- 表 `scene_probe_state` 存储每设备监控开关与状态
- 后端定时任务对 `monitoring=1` 的设备做概率翻转
- REST `/scene/probe/*` 与现有 `probe.js` 导出对齐
- 前端 `probe.js` 改 `request`；`mockProbeEngine` / `probeStore` 不再为运行时主路径

**出口标准**

- 开启全部/单设备监控后，状态来自后端；刷新后仍保持
- 模拟掉线/恢复可触发前端告警与建筑着色
- 新增设备（MySQL）可被监控
- UI 交互与阶段 C 一致

**不做**

- 真 ICMP ping（P4）
- 探测历史/监控设置落库（P2）
- SNMP、换 tiles

## 2. 决策摘要

| 项 | 选择 |
| --- | --- |
| 状态存储 | MySQL `scene_probe_state` |
| 翻转引擎 | 后端 `@Scheduled` |
| 周期 | 默认 5s；`scene.probe.interval-ms` |
| 概率 | 掉线 ~15%，恢复 ~40% |
| 告警/历史 | 前端轮询 diff 触发 |
| 静音 | 仍前端 localStorage（P2 再迁） |

## 3. 数据模型

### 3.1 API JSON

```json
{
  "deviceId": "dev-core-sw1",
  "monitoring": true,
  "status": "online",
  "lastChangeAt": 1720000000000
}
```

| status | 含义 |
| --- | --- |
| unknown | 未监控 |
| online | 在线 |
| offline | 离线 |

### 3.2 表 `scene_probe_state`

```sql
create table scene_probe_state (
  device_id       varchar(64)  not null,
  monitoring      char(1)      not null default '0',
  status          varchar(16)  not null default 'unknown',
  last_change_at  bigint(20)   default null,
  update_time     datetime,
  primary key (device_id)
) engine=innodb comment='scene probe state';
```

设备软删时同步删除 probe 行。

## 4. REST

| 方法 | 路径 |
| --- | --- |
| POST | `/scene/probe/startAll` |
| POST | `/scene/probe/stopAll` |
| POST | `/scene/probe/start/{deviceId}` |
| POST | `/scene/probe/stop/{deviceId}` |
| GET | `/scene/probe/list` |

- `list` 全量数组；可 `buildingId`
- 启动：monitoring=1, status=online
- 关闭：monitoring=0, status=unknown
- 静音 API 本阶段仍走前端
- 权限：`scene:probe:query` / `scene:probe:edit`（隐藏菜单，不覆盖 cesium 路由）

## 5. 定时任务

- 每 interval-ms 刷新 monitoring=1 且设备未删除的行
- online->offline p=0.15；offline->online p=0.40
- 仅变化时写 last_change_at

## 6. 前端

| 文件 | 变更 |
| --- | --- |
| `probe.js` | `request` |
| `globalMonitorRuntime.js` | 轮询 list + diff；停 mock 引擎 |
| probeStore / mockProbeEngine | 不再主路径 |

轮询间隔 2–5s（或跟随 probeIntervalMs）。

## 7. 测试

1. 建表
2. start 后 list online；F5 仍在
3. 定时任务可见翻转
4. 地图着色/告警
5. 新设备可监控

## 8. 风险

| 风险 | 缓解 |
| --- | --- |
| 双引擎 | 禁用前端 mock start |
| 轮询延迟 | 间隔 <= 后端 tick |
| 设备已删 | join 活跃设备 |

## 9. 自审

- [x] 无 TBD
- [x] 不含真 ping / 历史落库
- [x] API 套接已写明
- [x] 依赖 B2

