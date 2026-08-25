# 阶段 Collab-M2：协作文档（实时协作版）设计

**日期：** 2026-08-23  
**状态：** 待评审  
**依赖：** Collab-M1 已验收（`docs/superpowers/specs/2026-08-19-collab-doc-m1-design.md`）  
**关联：** Quill 富文本、`CollabSectionHelper`、若依 JWT 登录

## 0. 编码与字符集（强制）

与 M1 及仓库约定一致，避免中文乱码、SQL 注释损坏、接口消息乱码：

| 类型 | 要求 |
| --- | --- |
| 含中文的 Markdown 规格/计划 | **UTF-8 with BOM**（与 `collab_m1.sql`、M1 规格一致） |
| SQL 脚本（`sql/collab_m2.sql` 等） | **UTF-8 with BOM**；`COMMENT` 与菜单名用中文 |
| Java 源文件 | **UTF-8**（无 BOM）；`pom.xml` 已配置 `project.build.sourceEncoding=UTF-8`；业务异常文案用**中文源字符**，禁止 `\uXXXX` 转义堆砌 |
| Vue / JS | **UTF-8**（无 BOM）；`.editorconfig` 与现有 `collab` 页面一致 |
| WebSocket JSON 消息 | `Content-Type` 不适用；帧体为 **UTF-8** 编码 JSON；`nickName` 等字段原样传输 |
| Python 脚本 | 文件头 `# -*- coding: utf-8 -*-`；`stdout` 输出中文 |

**验收：** 用编辑器打开本文件与 `collab_m2.sql`，中文标题正常；`git diff` 不出现乱码替换字符。

## 1. 目标

在 M1「提交版」基础上，新增协作模式 **`realtime`（实时协作版）**：

- **协作管理员**（`collab_admin`）：可进入任务房间，**全文编辑**任意区块；正在编辑的区块对协作者 **只读（C3）**
- **协作者**（`collab_member`）：仅编辑被指派区块；他人区块只读；改动 **实时可见**
- **混合持久化（D3）**：编辑过程 **自动保存草稿**；截止或手动 **提交** 后锁定；管理员仍可 **生成汇总** 定稿
- **在场感（E2）**：在线成员列表 + 区块锁定提示；**不做** 光标同步（留 M2.1）

**M2 出口标准**

1. 创建任务时可选择 `mode=realtime`（与 `submit` 并存，互不影响既有任务）
2. 同一任务内 ≥2 用户同时打开编辑器时，区块内容在 2s 内同步到对方视图
3. 管理员聚焦某区块时，该区块协作者编辑器变为只读并显示锁定原因
4. 自动保存不替代提交：未提交者仍出现在「未提交名单」；截止后规则与 M1 一致
5. WebSocket 连接需 JWT 鉴权；无任务权限的用户无法加入房间
6. 单实例部署下功能完整；多实例扩展点文档化（Redis 广播，M2 不实现）

**不做（M2）**

- 协作者之间同区块同时打字（完整 OT/CRDT）——属 M2.1+ 或完整 A 模式
- 在线光标 / 选区同步（E3）——协议预留，实现放 M2.1
- 在线表格（M3）
- OnlyOffice / kkFileView
- 跨任务、跨文档的实时联动

## 2. 已确认决策摘要

| 项 | 选择 | 说明 |
| --- | --- | --- |
| 协作模型 | **C** | 管理员全文 + 协作者分区 |
| 区块冲突 | **C3** | 管理员正在编辑的区块，协作者只读 |
| 持久化 | **D3** | 实时自动保存草稿 + 提交锁定 + 管理员汇总 |
| 在场感 | **E2** | 在线列表 + 锁定提示；E3 后续 |
| 编辑器 | 复用 Quill `Editor` | 按区块拆分视图，与 M1 一致 |
| 传输 | Spring WebSocket + JSON | 房间粒度 = `taskId` |

## 3. 与 M1 的关系

| 能力 | submit（M1） | realtime（M2） |
| --- | --- | --- |
| 区块权限 | 是 | 是，不变 |
| 截止 / 补交 / 未提交 | 是 | 是，不变 |
| 手动提交 | 是 | 是 |
| 生成汇总 | 是 | 是（源数据见 §5.3） |
| 编辑同步 | 无（各自保存草稿） | WebSocket 广播 |
| 管理员全文 | 仅文档管理页 | 任务房间内可直接改 |
| 工作副本 | 无 | `collab_task.working_html` |

**兼容原则：** `mode=submit` 的任务 **零行为变化**；所有 M2 逻辑以 `mode=realtime` 为门控。

## 4. 架构

### 4.1 总体结构

```
Vue 实时编辑页
  +- Quill 区块编辑器（复用 sectionScope）
  +- CollabRealtimeClient（WebSocket）
  +- PresencePanel（在线成员）

ruoyi-admin
  +- CollabRealtimeWebSocketHandler
        +- JWT HandshakeInterceptor
        +- CollabRealtimeRoomManager（内存房间）
        +- ICollabRealtimeService（鉴权、持久化、广播）

ruoyi-system
  +- CollabTaskServiceImpl（扩展：working_html 初始化、realtime 保存）
```

### 4.2 技术选型对比（结论）

| 方案 | 优点 | 缺点 | M2 |
| --- | --- | --- | --- |
| **A. 原生 WebSocket + JSON** | 轻量、与若依 JWT 易集成、消息类型可演进 | 需自建房间管理 | **采用** |
| B. STOMP + SockJS | 生态成熟 | 依赖重、前端改造大 | 否 |
| C. 短轮询 HTTP | 实现简单 | 非实时、浪费连接 | 否 |

多实例：M2 仅 **单 JVM 内存房间**；规格注明后续用 Redis Pub/Sub 同步 `section.update` / `presence` 即可水平扩展。

### 4.3 认证

- 握手 URL：`ws(s)://{host}/ws/collab/task/{taskId}?token={jwt}`
- `CollabWebSocketAuthInterceptor` 解析 query `token`，复用 `TokenService.getLoginUser`
- 加入房间前校验：
  - `collab_admin` + `collab:task:edit`：管理员角色
  - 或该用户在 `collab_task_assignment` 中有记录 + `collab:task:submit` / `collab:task:mine`
- 任务 `mode` 必须为 `realtime`，且 `task_status=open`（已截止且未允许补交则拒绝）

## 5. 数据模型

### 5.1 表变更（`sql/collab_m2.sql`）

**`collab_task` 新增：**

| 列 | 类型 | 说明 |
| --- | --- | --- |
| `working_html` | longtext | 任务进行中组装稿；创建 realtime 任务时从 `collab_doc.content_html` 复制 |

**`collab_task.mode` 注释更新：** `submit` / `realtime`

无需新表。区块锁（C3）**M2 放内存**（`CollabRealtimeRoom`），断线自动释放该用户持有的锁。

### 5.2 工作副本 `working_html`

- 创建 `realtime` 任务：`working_html = doc.content_html`
- 实时编辑：按 `sectionId` 合并 patch 进 `working_html`（复用 `CollabSectionHelper.mergeSnapshots`）
- 同时更新：
  - 协作者：`collab_task_assignment.draft_content`（仅其授权区块拼接）
  - 管理员改全文：直接 patch `working_html` 对应区块
- 自动保存间隔：客户端 debounce **800ms**；服务端可对同一 `(taskId, sectionId)` 做 **300ms** 节流

### 5.3 提交与汇总（D3）

| 阶段 | 行为 |
| --- | --- |
| 编辑中 | `submit_status=editing`；`working_html` 持续更新 |
| 协作者点「提交」 | 从当前 `working_html` 提取其 scope 区块，写入 `content_snapshot`；`submit_status=submitted` |
| 截止 | 与 M1 相同，变为 `overdue`；WS 拒绝写操作 |
| 补交 | 与 M1 相同，变为 `resubmit_allowed` |
| 生成汇总 | 优先各 assignment 的 `content_snapshot`（已提交）；未提交区块从 `working_html` 取当前内容；仍缺则 `[unsubmitted:sec-x]`（与 M1 一致） |
| 汇总落库 | 写入 `collab_doc.content_html`；**不自动**覆盖 `working_html`（保留任务内现场） |

## 6. WebSocket 协议

### 6.1 消息信封

```json
{
  "type": "section.update",
  "taskId": 1,
  "ts": 1735689600000,
  "payload": { }
}
```

### 6.2 客户端 -> 服务端

| type | 说明 | payload |
| --- | --- | --- |
| `room.join` | 进入房间（连接后首条） | `{ "taskId": 1 }` |
| `section.focus` | 用户聚焦某区块（管理员触发 C3） | `{ "sectionId": "sec-1" }` |
| `section.blur` | 离开区块（释放 C3 锁） | `{ "sectionId": "sec-1" }` |
| `section.patch` | 区块内容变更 | `{ "sectionId": "sec-1", "html": "<p>...</p>", "baseVersion": 3 }` |

`baseVersion`：任务级单调递增版本（服务端下发），用于丢弃过期 patch（简易冲突处理，非 OT）。

### 6.3 服务端 -> 客户端

| type | 说明 |
| --- | --- |
| `room.snapshot` | 加入成功：全量 `working_html`、`version`、在线列表、当前锁定表 |
| `section.update` | 某区块已更新（广播给其他人） |
| `section.locked` | C3：区块被管理员占用 |
| `section.unlocked` | 区块锁释放 |
| `presence.changed` | 在线用户列表变化 |
| `error` | 业务错误（无权限、已截止等） |

### 6.4 M2.1 预留（E3，不实现）

| type | payload |
| --- | --- |
| `cursor.move` | `{ "sectionId", "index", "length" }` |

客户端忽略未知 `type`；服务端对 `cursor.move` 直接 ack 丢弃即可，直至 M2.1。

## 7. C3 管理员优先锁

### 7.1 规则

1. 仅 **管理员连接** 的 `section.focus` 可创建锁
2. 锁粒度：`taskId + sectionId`
3. 持有锁期间：协作者对该区块 **Quill 只读** + 顶栏提示「管理员 {nickName} 正在编辑此区块」
4. 管理员 `section.blur`、断开 WS、或 60s 无心跳：自动释放
5. 管理员编辑 **非协作者指派** 的区块：无协作者冲突，不显示协作者侧锁（仅广播内容）
6. 管理员与协作者 **不同区块**：互不影响，正常实时同步

### 7.2 与「完整 A 模式」的边界

当管理员与协作者 **同时改同一区块** 时，C3 **禁止协作者写入**，因此 **不需要 OT**。协作者仅接收 `section.update`。这是 C 相对 A 的核心简化。

## 8. API 变更（HTTP）

保留 M1 全部接口。新增 / 调整：

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/collab/task/{id}/working` | `collab:task:edit` 或任务成员 | 获取 `working_html`（WS 断线重连兜底） |
| POST | `/collab/task` | `collab:task:add` | body 允许 `"mode": "realtime"` |

**创建任务校验：** `mode=realtime` 时初始化 `working_html`；`submit` 不写入 `working_html`。

**WebSocket：** 非 REST，见 §4.3。

## 9. 前端

### 9.1 页面

| 入口 | 改动 |
| --- | --- |
| 协作任务管理 · 新建 | 协作模式增加「实时协作版」 |
| 协作任务管理 · 管理 | `mode=realtime` 显示「进入协作房间」 |
| 我的协作任务 | `mode=realtime` 打开 `views/collab/task/realtime.vue` |
| 新建 `realtime.vue` | 左侧：文档区块编辑；右侧：在线成员 + 锁定状态 |

`submit` 模式继续用现有弹窗编辑器（`mine.vue` / `index.vue`），不强制迁移。

### 9.2 组件

| 文件 | 职责 |
| --- | --- |
| `utils/collab/realtimeClient.js` | 连接、重连、心跳、消息分发 |
| `utils/collab/realtimeProtocol.js` | type 常量、信封构造 |
| `components/Collab/PresencePanel.vue` | 在线列表 |
| `components/Collab/SectionLockBanner.vue` | C3 锁定提示 |

### 9.3 同步逻辑

1. 本地 Quill `text-change` -> debounce -> `section.patch`
2. 收到 `section.update`：若该区块非本地聚焦，则 **静默** 更新 Quill（避免光标跳动）
3. 收到 `section.locked` / `unlocked`：切换 `readOnly`
4. 断线：指数退避重连；重连后发 `room.join` 拉 `room.snapshot`

### 9.4 代理配置

`vue.config.js` 增加 `/ws` -> 后端 WebSocket 代理（dev 环境）。

## 10. 后端模块

| 单元 | 职责 |
| --- | --- |
| `CollabWebSocketConfig` | 注册 `/ws/collab/task/{taskId}` |
| `CollabWebSocketAuthInterceptor` | JWT 校验 |
| `CollabRealtimeWebSocketHandler` | 消息路由 |
| `CollabRealtimeRoomManager` | 房间、presence、admin 锁 |
| `ICollabRealtimeService` | 权限、patch 持久化、版本号 |
| `CollabTaskServiceImpl` | 创建时初始化 `working_html`；汇总逻辑扩展 |

**版本号：** `CollabRealtimeRoom.docVersion` 内存递增；每次成功 `section.patch` +1，广播时带上。

## 11. 权限与菜单

不新增菜单。可选新增权限字（二选一，实现时择一）：

| 权限字 | 含义 |
| --- | --- |
| `collab:task:realtime` | 加入 realtime 房间（成员+管理员） |

**推荐：** 不新增权限字，复用 `collab:task:submit`（成员）与 `collab:task:edit`（管理员），减少 SQL 迁移。

## 12. 测试要点

### 12.1 自动化

- Java `CollabRealtimeRoomSelfCheck`：C3 锁获取/释放、版本丢弃旧 patch
- Node `ruoyi-ui/tests/collab/realtime-protocol.test.js`：消息序列化
- Python `scripts/collab_realtime_e2e.py`：双用户 WS：A 改 sec-1 -> B 收到 `section.update`；管理员 focus -> 协作者收到 `section.locked`

### 12.2 手工

1. 创建 `realtime` 任务，两浏览器（admin + collab1）同时进入
2. collab1 改 sec-1，admin 在 sec-2 可见同步
3. admin 聚焦 sec-1，collab1 的 sec-1 变只读
4. admin 离开 sec-1，collab1 恢复可写
5. 截止后双方无法 patch；补交后可继续
6. 提交 + 汇总后文档内容正确

## 13. 风险与缓解

| 风险 | 缓解 |
| --- | --- |
| Quill 远程更新导致光标丢失 | 仅更新非聚焦区块；聚焦区块用本地编辑优先 |
| 大 HTML patch 频繁 | debounce + 服务端节流；单区块传输 |
| WS 在若依网关/Nginx 后失效 | 文档注明 `proxy_set_header Upgrade`；dev 用 vue proxy |
| 管理员断线锁未释放 | 60s 心跳超时 + 断线清理 |
| 同文档多 realtime 任务 | M2 不禁止；`working_html` 按任务隔离；规格注明「一文档同时仅建议一个 open realtime 任务」 |

## 14. 实施分期

| 阶段 | 内容 |
| --- | --- |
| **M2** | C + C3 + D3 + E2 + WebSocket 全链路 |
| **M2.1** | E3 光标同步（`cursor.move`） |
| **M3** | 在线表格 |

## 15. 自审

- [x] 与 M1 决策一致：分区权限、截止、补交、汇总保留
- [x] C3 避免同区块 OT，边界已说明
- [x] D3 自动保存与提交锁定分离
- [x] E2 实现，E3 协议预留
- [x] 单实例可交付，扩展点已记录
- [x] `submit` 模式零回归
- [x] 编码约定已写入 §0