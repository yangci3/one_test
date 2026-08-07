# 阶段 P2：探测历史入库 + 监控设置按用户入库

**日期：** 2026-08-07  
**状态：** 已实现  
**路线图：** `docs/superpowers/specs/2026-07-31-factory-scene-network-roadmap.md`  
**依赖：** P1 后端模拟探测；P1.5 场景权限分层  
**实现计划：** [docs/superpowers/plans/2026-08-07-phase-p2-history-settings.md](docs/superpowers/plans/2026-08-07-phase-p2-history-settings.md)  
**实现分支：** `feature/phase-p2-history-settings`（commits `89d9b9a`..`6bce947`）  
**关联：** `docs/superpowers/specs/2026-08-01-phase-d-probe-history-design.md`；`docs/superpowers/specs/2026-08-01-network-monitor-settings-design.md`

## 1. 目标

将上下线历史与监控设置从浏览器 localStorage 迁到 MySQL：

- **历史：** 后端在探测状态翻转（与 start 置 online）时写入；前端只查询  
- **设置：** 按登录用户各存一份；首次默认，保存后跨终端保留  
- **自定义告警音频：** 仍本机 localStorage（不跟账号跨机）

**落地策略：方案 1**

**出口标准**

1. 开启监控后翻转/启动 online 事件出现在监控历史与监控详情；换浏览器仍在  
2. 历史裁剪：>30 天删除；且全库总条数 >50000 时删最旧  
3. 用户 A 保存设置后，换机同账号登录，入库项一致；自定义歌仅原机有  
4. 新用户首次打开设置 = 默认值  
5. `net_viewer` 无历史/设置菜单；不调对应 API  
6. 前端不再以 localStorage 为历史主路径  

**不做**

- localStorage 历史/设置自动迁移  
- 自定义音频入库或文件上传  
- 真 ICMP ping（P4）、换 tiles（P3）  
- 将多用户的 `probeIntervalMs` 反写后端 `@Scheduled` 周期（避免互相覆盖）  

## 2. 决策摘要

| 项 | 选择 |
| --- | --- |
| 历史写入 | 仅后端（调度翻转 + start 置 online） |
| 旧 localStorage 历史 | 不迁移 |
| 历史保留 | 30 天 + 全库最多 50000 条 |
| 设置范围 | 按 `user_id`；首次默认；保存后持久 |
| 自定义音频 | 仅本机 localStorage |
| `probeIntervalMs` | 仅影响前端轮询；后端翻转周期仍 `scene.probe.interval-ms` |
| 权限 | `scene:history:list`；`scene:settings:query|edit`（P1.5 菜单） |

## 3. 数据模型

### 3.1 `scene_probe_event`

```sql
create table if not exists scene_probe_event (
  event_id    varchar(64)  not null,
  device_id   varchar(64)  not null,
  event_type  varchar(16)  not null,
  event_at    bigint(20)   not null,
  create_time datetime     default null,
  primary key (event_id),
  key idx_scene_probe_event_device_at (device_id, event_at),
  key idx_scene_probe_event_at (event_at)
) engine=innodb comment='scene probe online/offline events';
```

API JSON（与阶段 D 对齐）：

```json
{
  "id": "ph-1720000000000-a1b2",
  "deviceId": "dev-core-sw1",
  "type": "offline",
  "at": 1720000000000,
  "deviceName": "Core SW1"
}
```

### 3.2 `scene_monitor_settings`

```sql
create table if not exists scene_monitor_settings (
  user_id               bigint(20)   not null,
  alert_muted           char(1)      not null default '0',
  alert_volume          decimal(4,3) not null default 0.700,
  beep_preset           varchar(16)  not null default 'default',
  probe_interval_ms     int          not null default 5000,
  hover_summary_delay_ms int         not null default 500,
  alert_popup_enabled   char(1)      not null default '1',
  alert_sound_mode      varchar(16)  not null default 'preset',
  building_status_colors text,
  update_time           datetime     default null,
  primary key (user_id)
) engine=innodb comment='per-user scene monitor settings';
```

`building_status_colors` 存 JSON 字符串（四态 rgba）。  
**不存** `customOfflineAudio` / `customOnlineAudio` / 文件名。

## 4. 后端行为

### 4.1 历史写入

在 `SceneProbeServiceImpl`（或等价）：

- `tick` 翻转成功更新 status 时：insert `online`/`offline`  
- `start` / `startAll` 置 `online` 时：insert `online`  
- `stop` 置 `unknown`：不写  

写入后调用裁剪：

1. `delete where event_at < nowMs - 30d`  
2. `count(*)`；若 > 50000，按 `event_at` 升序删最旧直至 ≤ 50000  

设备软删时：同步 `delete from scene_probe_event where device_id=?`。

### 4.2 REST

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| GET | `/scene/probe/history` | `scene:history:list` |
| GET | `/scene/probe/history/{deviceId}` | `scene:history:list` |
| GET | `/scene/monitor/settings` | `scene:settings:query` |
| PUT | `/scene/monitor/settings` | `scene:settings:edit` |
| POST | `/scene/monitor/settings/reset` | `scene:settings:edit` |

历史查询默认时间窗 = 近 30 天；可 `deviceId`/`from`/`to`。  
设置 GET：无行则返回内置默认 JSON（不自动 insert）。  
PUT：校验后 upsert。  
reset：删行或写默认并返回默认。

## 5. 前端行为

### 5.1 历史

- `api/scene/probeHistory.js` 改 `request`  
- `globalMonitorRuntime` 移除 `appendProbeEvent`  
- Index 监控详情、probeHistory 页：UI 不变；保留基于监控更新事件的刷新  
- `probeHistoryStore` 不再为运行时主路径  

### 5.2 设置

- `api/scene/monitorSettings.js` 改 `request`  
- 加载：后端字段 + 本机自定义音频字段合并  
- 保存：仅提交可入库字段；自定义音频写 localStorage  
- 恢复默认：调后端 reset + **清空本机自定义音频**  
- `probeIntervalMs` 仅用于前端轮询间隔重启  

## 6. SQL 与菜单

- 新文件 `sql/scene_history_settings_p2.sql`：两表 DDL  
- P1.5 已含历史/设置菜单与权限字；若缺 F 按钮则补全，不重建目录  

## 7. 测试

### 7.1 人工验收清单

| # | 项 | 状态 |
| --- | --- | --- |
| 1 | 执行 sql/scene_history_settings_p2.sql | done |
| 2 | 开启监控；历史在 DB 与 UI；换浏览器可见 | done |
| 3 | 保存设置；同用户换机非音频项一致 | done |
| 4 | 自定义音频仅上传机器 | done |
| 5 | viewer 无菜单 / 直访页 404（无动态路由；等价于无权限） | done |

### 7.2 原测试步骤

1. 执行 SQL；admin/网络管理员登录  
2. start 监控，等待翻转；历史页与详情有记录  
3. 换浏览器登录：历史仍在  
4. 保存设置；换机/浏览器同用户：入库项一致；自定义歌不同步  
5. 新用户：设置为默认  
6. viewer：无菜单；直调 API 应 403  

## 8. 风险

| 风险 | 缓解 |
| --- | --- |
| 前端仍写 localStorage 历史 | 删除 append 调用；code review |
| 多用户改间隔打乱后端调度 | `probeIntervalMs` 不反写调度 |
| 自定义音频跨终端丢失 | 已在决策中声明；文档/设置页可提示 |

## 9. 自审

- [x] 无 TBD  
- [x] 与已确认选项一致（后端写历史、不迁移、D 裁剪、音频 A、设置按用户）  
- [x] 不含 P3/P4  
- [x] 权限与 P1.5 对齐  
