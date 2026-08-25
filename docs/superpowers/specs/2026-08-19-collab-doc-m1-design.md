# 阶段 Collab-M1：协作文档（提交版 + 权限下放）设计

**日期：** 2026-08-19  
**状态：** M1 已实现并验收（2026-08-23）；实现计划见 docs/superpowers/plans/2026-08-20-collab-doc-m1.md  
**依赖：** 若依标准登录/动态菜单/RBAC（对齐 P1.5 三维场景授权模式）  
**关联：** 三维网络监控模块（并列目录）；现有 Quill 富文本组件

## 1. 目标

在若依同一套登录页与前端下，新增与「三维网络监控」并列的「协作文档」能力：

- **协作管理员**（`collab_admin`）：创建文档与协作任务，指定协作者、可编辑范围、截止时间；查看未提交名单；延期/允许补交  
- **协作者**（`collab_member`）：仅在被指派的任务内，编辑被分配的文档区块，并提交  
- **无协作权限用户**：侧边栏无协作文档目录；不调用协作 API

**M1 落地策略：** 方案 1 分阶段自建 —— 先做 **富文本 + 提交版模式**；后做同时协作版与在线表格。

**M1 出口标准**

1. 所有用户仍从若依 **同一登录页** 进入；按角色动态挂载协作文档菜单（无独立协作登录站）  
2. `collab_admin` 可创建文档、下发任务；下发时用户选择列表 **仅包含已拥有协作者角色/权限的用户**  
3. `collab_member` 在「我的协作任务」中看到指派；只能编辑被分配的文档区块；可提交  
4. 截止后：管理员可查 **未提交人员**；可 **延长截止日期** 或 **允许补交**  
5. 截止后未补交者不能再编辑（除非被允许补交）  
6. `admin` 超级管理员保持全部权限

**不做（M1）**

- 独立协作登录页 / 微信登录 / 单机版客户端  
- **同时协作版**（多人同屏实时打字）；WebSocket/OT 放 M2  
- **在线表格** 及行级范围授权；放 M3  
- AI 写作 / 外挂 OnlyOffice  
- 全局忽略 403

## 2. 决策摘要

| 项 | 选择 |
| --- | --- |
| 登录入口 | 若依单一 SPA，同 `/login`；菜单按角色动态加载 |
| 与三维模块关系 | 并列顶级目录；角色独立（可同时拥有 `net_admin` 与 `collab_admin`） |
| 协作者准入 | **方案 A**：必须拥有 `collab_member` 角色（或等价权限）**且** 被任务指派 |
| 下发时可选人 | 仅列出 **所有具备协作者资格的用户**（`GET /collab/user/candidates`） |
| 文档类型 M1 | 富文本（Quill/HTML）；表格后续 M3 |
| 范围粒度 M1 | 文档 **区块/section**（段落锚点 `sec-*`） |
| 协作模式 M1 | 仅 **提交版** `submit` |
| 权限实现 | 动态菜单 + `@PreAuthorize` 权限字 + 前端 `v-hasPermi` |
| 编辑器 | 复用/扩展现有 Quill `Editor` 组件 |

## 3. 角色与权限下放

| 角色名 | roleKey | 说明 |
| --- | --- | --- |
| 超级管理员 | `admin` | 已有；全部协作权限 |
| 协作管理员 | `collab_admin` | 创建文档/任务、指派、截止管理、未提交查看、补交 |
| 协作者 | `collab_member` | 查看「我的任务」、在授权区块内编辑与提交 |
| 协作查看者（可选） | `collab_viewer` | M1 可省略；预留只读汇总版 |

**权限下放用法（与网络管理员相同机制）：**

- 系统管理 → 用户管理 → 分配角色：勾选 `collab_admin` / `collab_member`  
- 或 角色管理 → 自建角色 → 在菜单树勾选「协作文档」节点  
- 变更后需 **重新登录** 生效

**下发协作时的人员列表（已确认）：**

- `collab_admin` 在创建/编辑任务时调 `GET /collab/user/candidates`  
- 后端返回：具有 `collab:member:eligible`（或挂载 `collab_member` 角色）的 **启用用户** 列表（userId, userName, nickName, deptName）  
- **不包含** 无协作者资格的用户；即便在全系统用户表中存在

## 4. 菜单与权限字（预留号段 2300–2399）

### 4.1 可见菜单（M1）

| 菜单 | 组件 | collab_member | collab_admin |
| --- | --- | --- | --- |
| 协作文档（目录） | Layout | ✓ | ✓ |
| └ 我的协作任务 | `collab/task/mine` | ✓ | ✓ |
| └ 文档管理 | `collab/doc/index` | ✗ | ✓ |
| └ 协作任务管理 | `collab/task/index` | ✗ | ✓ |

### 4.2 权限字

| 权限字 | 含义 |
| --- | --- |
| `collab:doc:list` | 文档列表（管理员） |
| `collab:doc:query` | 文档详情 |
| `collab:doc:add` | 新建文档 |
| `collab:doc:edit` | 编辑文档（管理员全文） |
| `collab:task:list` | 任务列表（管理员） |
| `collab:task:mine` | 我的任务 |
| `collab:task:add` | 创建/下发任务 |
| `collab:task:edit` | 修改任务（截止、补交等） |
| `collab:task:submit` | 协作者提交自己范围 |
| `collab:member:eligible` | 标记具备协作者资格（用于候选人查询） |

## 5. 核心概念

### 5.1 文档（Document）

- `collab_doc`：标题、创建人、内容 HTML（Quill Delta 或 HTML 存储）、状态（草稿/已发布）  
- 内容内嵌 **区块标记**：编辑器保留 `data-sec-id="sec-1"` 等锚点（创建文档时至少 1 个区块；管理员可拆分段落）

### 5.2 协作任务（Task）

- 挂在一份 `doc_id` 上；由 `collab_admin` 创建  
- **协作模式** `mode`：M1 仅 `submit`（提交版）；M2 增加 `realtime`（同时协作版）  
- **截止时间** `deadline_at`；可延期（写 `collab_task_deadline_log`）  
- **状态** `task_status`：draft / open / closed

### 5.3 任务参与（Assignment）

每人一条 `collab_task_assignment`：

| 字段 | 说明 |
| --- | --- |
| `user_id` | 必须在候选人列表内 |
| `scope_type` | M1：`section`；M3 增加 `row_range` |
| `scope_json` | 例：`{"sectionIds":["sec-1","sec-2"]}` 或 `{"fromSec":"sec-2","toSec":"sec-3"}` |
| `submit_status` | `editing` / `submitted` / `overdue` / `resubmit_allowed` |
| `submitted_at` | 提交时间 |
| `content_snapshot` | 该用户提交的区块 HTML 快照（用于汇总） |

### 5.4 截止与补交

- `now > deadline_at` 且未提交 → `submit_status = overdue`；前端禁止编辑  
- 管理员 **延长截止日期**：更新 `deadline_at`，记录操作人与原新时间  
- 管理员 **允许补交**：对指定 `assignment_id` 设 `resubmit_allowed`，允许再次编辑并提交  
- **未提交名单**：`GET /collab/task/{id}/unsubmitted` → 超过截止仍为 `editing`/`overdue` 的用户

### 5.5 汇总版（M1 简化）

- 管理员手动点「生成汇总」：按区块合并各人 `content_snapshot` 到主文档（未提交区块保留原文或标记缺失）  
- 不做自动实时合并（属 M2 `realtime` 范围）

## 6. API 概览（M1）

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/collab/user/candidates` | `collab:task:add` | 所有具备协作者资格的用户 |
| GET | `/collab/doc/list` | `collab:doc:list` | 文档分页 |
| POST | `/collab/doc` | `collab:doc:add` | 新建 |
| GET | `/collab/doc/{id}` | `collab:doc:query` | 详情（按角色/任务过滤可见范围） |
| PUT | `/collab/doc/{id}` | `collab:doc:edit` | 管理员全文编辑 |
| GET | `/collab/task/list` | `collab:task:list` | 管理员任务列表 |
| GET | `/collab/task/mine` | `collab:task:mine` | 我的任务 |
| POST | `/collab/task` | `collab:task:add` | 创建任务+指派 |
| PUT | `/collab/task/{id}/deadline` | `collab:task:edit` | 延长截止 |
| PUT | `/collab/task/{id}/allow-resubmit` | `collab:task:edit` | 允许补交 |
| GET | `/collab/task/{id}/unsubmitted` | `collab:task:edit` | 未提交名单 |
| PUT | `/collab/task/{id}/assignment/{aid}/draft` | `collab:task:submit` | 协作者保存区块草稿 |
| POST | `/collab/task/{id}/assignment/{aid}/submit` | `collab:task:submit` | 协作者提交 |
| POST | `/collab/task/{id}/merge` | `collab:task:edit` | 生成汇总版 |

## 7. 前端页面（M1）

- **文档管理**：CRUD；内容编辑器支持区块划分（管理员）  
- **协作任务管理**：选文档 → 选模式（M1 仅提交版） → 选截止时间 → 从 **候选协作者列表** 勾选人员 → 为每人指定 section 范围  
- **我的任务**：打开任务 → 仅可编辑授权区块（其余只读） → 保存/提交  
- **未提交 / 延期 / 补交**：任务详情页管理员操作区

**区块只读实现（M1）：** Quill 对非授权 `data-sec-id` 区域设 `contenteditable=false` 蒙层或不加载到编辑区；仅渲染授权区块的 Delta/HTML。

## 8. 后端模块

| 单元 | 职责 |
| --- | --- |
| `CollabDocController` / `Service` | 文档 CRUD |
| `CollabTaskController` / `Service` | 任务、指派、截止、未提交、补交、汇总 |
| `CollabUserController` | `/collab/user/candidates` |
| Mapper + SQL | `collab_doc`、`collab_task`、`collab_task_assignment`、`collab_task_deadline_log` |

校验：指派时 `user_id` 必须通过 `collab_member` 角色校验；否则 400。

## 9. 后续阶段（预告）

| 阶段 | 内容 |
| --- | --- |
| **M2** | `mode=realtime`；WebSocket；同时协作版；在线成员列表；详见 `docs/superpowers/specs/2026-08-23-collab-doc-m2-design.md` |
| **M2.1** | 在线光标（E3） |
| **M3** | 在线表格；`scope_type=row_range`；表格提交版/同时协作 |

## 10. 测试要点

1. 仅 `collab_member`：有「我的任务」；无文档/任务管理菜单  
2. `collab_admin`：候选人列表仅含协作者角色用户  
3. 指派后协作者只能改指定 section  
4. 截止后未提交出现在未提交列表；补交后可再提交  
5. 无任何协作角色：无协作菜单、无 403 循环弹窗

## 11. 风险

| 风险 | 缓解 |
| --- | --- |
| Quill 区块只读复杂 | M1 用 section 锚点 + 单独编辑视图；避免全文同屏混编 |
| 候选人与角色不同步 | 后端以 `sys_user_role` + `collab_member` 联查为准，不信前端传入 |
| 三维与协作权限混乱 | `collab:*` 与 `scene:*` 前缀分离；角色独立可叠加 |

## 12. 自审

- [x] 登录入口：单一若依，无独立协作站  
- [x] 协作者方案 A + 下发时候选人=全部协作权限用户  
- [x] M1 范围：提交版 + section + 截止/补交/未提交  
- [x] 同时协作版与表格行权限明确后移  
- [x] 权限下放对齐 P1.5 动态菜单模式
