# 阶段 P4：真 ICMP 探测（mock / icmp 可切换）

**日期：** 2026-08-13
**状态：** 代码已完成；人工验收待执行
**路线图：** `docs/superpowers/specs/2026-07-31-factory-scene-network-roadmap.md`
**依赖：** P1 后端模拟探测；P2 历史入库
**实现计划：** docs/superpowers/plans/2026-08-13-phase-p4-icmp-probe.md  
**关联：** `docs/superpowers/specs/2026-08-01-phase-p1-backend-mock-probe-design.md`

## 1. 目标

在保留现有 mock 翻转的前提下，增加基于设备 IP 的真实 ICMP 探测；通过配置在两种模式间切换，默认 mock，便于无厂区网时用公网 IP（如 8.8.8.8）联调。

**落地策略：方案 A（策略接口 + 配置切换）**

**出口标准**

1. `scene.probe.mode=mock` 时行为与 P1 一致（概率翻转 + 历史）
2. `mode=icmp` 时对合法 IP 执行 ping；连续 3 次失败 → offline；1 次成功 → online
3. 无/非法 IP：本 tick 跳过，不强制改写 status（保持 unknown 或不乱跳）
4. 状态实际变化时仍写入 `scene_probe_event`（沿用 P2）
5. 改配置并重启后端即可切换；前端探测 API / 告警 UI 无需为大改

**不做**

- 管理界面切换 mode
- SNMP（阶段 F）
- 换厂区 tiles（P3）
- 将用户 `probeIntervalMs` 反写后端调度周期

## 2. 决策摘要

| 项 | 选择 |
| --- | --- |
| 架构 | 策略接口 `SceneProbeReachability`：Mock / Icmp 两实现 |
| 默认模式 | `mock` |
| 切换方式 | `application.yml` 的 `scene.probe.mode`，重启生效 |
| Offline | 连续失败 ≥ 3（可配 `icmp.fail-threshold`） |
| Online 恢复 | 连续成功 ≥ 1（可配 `icmp.recover-threshold`） |
| 无效 IP | 跳过 ping，不强制改 status |
| ICMP 实现 | Windows：`ping -n 1 -w <timeoutMs> <ip>` |
| 失败计数 | 进程内内存；重启清零；不新建表 |
| 联调 | 可用公网 IP；回厂区后改设备 IP 即可 |

## 3. 架构

保留：

- `SceneProbeScheduler` → `ISceneProbeService.tick()`
- `scene_probe_state` / start·stop / P2 历史写入与裁剪

新增：

- `SceneProbeReachability`（或等价命名）
  - `MockProbeReachability`：现有 offline-prob / recover-prob 逻辑迁入
  - `IcmpProbeReachability`：执行系统 ping，返回 success/fail
- `tick()` 按 `mode` 选择策略；icmp 下维护 per-device 连续成功/失败计数后再决定是否改 status

前端：继续轮询 `/scene/probe/list`；不新增探测模式 API（本阶段）。

## 4. 配置

在 `scene.probe` 下：

| 键 | 默认 | 说明 |
| --- | --- | --- |
| `mode` | `mock` | `mock` 或 `icmp` |
| `interval-ms` | `5000` | 调度间隔（已有） |
| `offline-prob` | `0.15` | 仅 mock |
| `recover-prob` | `0.40` | 仅 mock |
| `icmp.timeout-ms` | `2000` | 单次 ping 超时（毫秒，映射到 ping -w） |
| `icmp.fail-threshold` | `3` | 连续失败 → offline |
| `icmp.recover-threshold` | `1` | 连续成功 → online |

非法 `mode` 值：启动或首次 tick 时回退 `mock` 并打日志（实现计划中写明）。

## 5. ICMP 与状态机

### 5.1 执行

- 命令形态（Windows）：`ping -n 1 -w <timeoutMs> <ip>`
- 根据进程退出码与/或输出判断通断
- 同一 tick 内对多设备：有限并发或串行（默认串行或小并发上限，避免打满进程）；实现计划选定一种并写测试注意点

### 5.2 规则（mode=icmp 且 monitoring=1）

| 条件 | 行为 |
| --- | --- |
| IP 空或非法 | 不 ping；本 tick 不改 status |
| ping 成功 | 失败计数=0；成功计数+1；若成功计数 ≥ recover-threshold 且 status ≠ online → online + 历史 |
| ping 失败 | 成功计数=0；失败计数+1；若失败计数 ≥ fail-threshold 且 status ≠ offline → offline + 历史 |
| start | 与现网一致：monitoring + 可置 online 并写 online 事件 |
| stop | unknown；不写历史 |

仅 status **实际变化** 时调用现有 `recordEvent` + trim。

## 6. 测试

1. `mode=mock`：随机翻转与历史仍正常
2. `mode=icmp` + IP=`8.8.8.8`（或 `1.1.1.1`）：可 online
3. IP 改为不可达：约 3 个调度周期后 offline，历史有记录
4. IP 清空：不因探测乱跳
5. 改回 `mock` 并重启：恢复模拟

## 7. 风险

| 风险 | 缓解 |
| --- | --- |
| 公网禁 ICMP / 不稳 | 文档说明选可用公共 DNS；阈值防抖 |
| Windows ping 解析差异 | 以退出码为主，输出为辅；手测清单 |
| tick 内设备过多拖慢调度 | 有限并发/串行；interval 可配 |
| 误配 icmp 无网导致大面积 offline | 默认 mock；阈值 |

## 8. 自审

- [x] 无 TBD
- [x] 与已确认选项一致（方案 A、默认 mock、N=3 / 恢复 1、无效 IP 跳过）
- [x] 不含 P3 / SNMP
- [x] 与 P1/P2 调度与历史路径对齐
