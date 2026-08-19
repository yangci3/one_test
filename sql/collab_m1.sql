-- Collab M1: collaborative document schema, RBAC menus and roles
-- Database: ry-vue

-- PRECHECK: SELECT role_id, role_key FROM sys_role WHERE role_id IN (102,103) OR role_key IN ('collab_admin','collab_member');

-- 0. Idempotent cleanup for Collab M1 menu band and roles
DELETE FROM sys_role_menu WHERE menu_id BETWEEN 2300 AND 2399;
DELETE FROM sys_menu WHERE menu_id BETWEEN 2300 AND 2399;
DELETE FROM sys_role_menu WHERE role_id IN (102, 103);
DELETE FROM sys_role WHERE role_id IN (102, 103) OR role_key IN ('collab_admin', 'collab_member');

-- 1. Tables
DROP TABLE IF EXISTS collab_task_deadline_log;
DROP TABLE IF EXISTS collab_task_assignment;
DROP TABLE IF EXISTS collab_task;
DROP TABLE IF EXISTS collab_doc;

CREATE TABLE collab_doc (
  doc_id          bigint(20)      NOT NULL AUTO_INCREMENT    COMMENT '文档ID',
  title           varchar(200)    NOT NULL                   COMMENT '文档标题',
  content_html    longtext                                   COMMENT '文档HTML内容',
  status          char(1)         DEFAULT '0'                COMMENT '状态（0草稿 1已发布）',
  create_by       varchar(64)     DEFAULT ''                 COMMENT '创建者',
  create_time     datetime                                   COMMENT '创建时间',
  update_by       varchar(64)     DEFAULT ''                 COMMENT '更新者',
  update_time     datetime                                   COMMENT '更新时间',
  remark          varchar(500)    DEFAULT NULL               COMMENT '备注',
  PRIMARY KEY (doc_id)
) ENGINE=InnoDB COMMENT='协作文档';

CREATE TABLE collab_task (
  task_id         bigint(20)      NOT NULL AUTO_INCREMENT    COMMENT '任务ID',
  doc_id          bigint(20)      NOT NULL                   COMMENT '关联文档ID',
  title           varchar(200)    NOT NULL                   COMMENT '任务标题',
  mode            varchar(20)     DEFAULT 'submit'           COMMENT '协作模式（submit提交版）',
  deadline_at     datetime                                   COMMENT '截止时间',
  task_status     varchar(20)     DEFAULT 'draft'            COMMENT '任务状态（draft/open/closed）',
  create_by       varchar(64)     DEFAULT ''                 COMMENT '创建者',
  create_time     datetime                                   COMMENT '创建时间',
  update_by       varchar(64)     DEFAULT ''                 COMMENT '更新者',
  update_time     datetime                                   COMMENT '更新时间',
  remark          varchar(500)    DEFAULT NULL               COMMENT '备注',
  PRIMARY KEY (task_id),
  KEY idx_collab_task_doc_id (doc_id)
) ENGINE=InnoDB COMMENT='协作任务';

CREATE TABLE collab_task_assignment (
  assignment_id   bigint(20)      NOT NULL AUTO_INCREMENT    COMMENT '指派ID',
  task_id         bigint(20)      NOT NULL                   COMMENT '任务ID',
  user_id         bigint(20)      NOT NULL                   COMMENT '用户ID',
  scope_type      varchar(20)     DEFAULT 'section'          COMMENT '范围类型（section区块）',
  scope_json      varchar(2000)   DEFAULT NULL               COMMENT '范围JSON',
  submit_status   varchar(30)     DEFAULT 'editing'          COMMENT '提交状态（editing/submitted/overdue/resubmit_allowed）',
  submitted_at    datetime                                   COMMENT '提交时间',
  draft_content   longtext                                   COMMENT '区块草稿内容',
  content_snapshot longtext                                  COMMENT '提交区块HTML快照',
  create_time     datetime                                   COMMENT '创建时间',
  update_time     datetime                                   COMMENT '更新时间',
  PRIMARY KEY (assignment_id),
  UNIQUE KEY uk_task_user (task_id, user_id),
  KEY idx_assignment_task_id (task_id),
  KEY idx_assignment_user_id (user_id)
) ENGINE=InnoDB COMMENT='协作任务指派';

CREATE TABLE collab_task_deadline_log (
  log_id          bigint(20)      NOT NULL AUTO_INCREMENT    COMMENT '日志ID',
  task_id         bigint(20)      NOT NULL                   COMMENT '任务ID',
  old_deadline    datetime                                   COMMENT '原截止时间',
  new_deadline    datetime                                   COMMENT '新截止时间',
  operator_id     bigint(20)      NOT NULL                   COMMENT '操作人ID',
  create_time     datetime                                   COMMENT '创建时间',
  PRIMARY KEY (log_id),
  KEY idx_deadline_log_task_id (task_id)
) ENGINE=InnoDB COMMENT='协作任务截止延期日志';

-- 2. Visible directory and C menus (2300-2303)
INSERT INTO sys_menu VALUES('2300', '协作文档', '0', '6', 'collab', NULL, '', 'Collab', 1, 0, 'M', '0', '0', '', 'documentation', 'admin', sysdate(), '', NULL, '协作文档目录');
INSERT INTO sys_menu VALUES('2301', '我的协作任务', '2300', '1', 'task-mine', 'collab/task/mine', '', 'CollabTaskMine', 1, 0, 'C', '0', '0', 'collab:task:mine', 'form', 'admin', sysdate(), '', NULL, '我的协作任务');
INSERT INTO sys_menu VALUES('2302', '文档管理', '2300', '2', 'doc', 'collab/doc/index', '', 'CollabDoc', 1, 0, 'C', '0', '0', 'collab:doc:list', 'documentation', 'admin', sysdate(), '', NULL, '文档管理');
INSERT INTO sys_menu VALUES('2303', '协作任务管理', '2300', '3', 'task', 'collab/task/index', '', 'CollabTask', 1, 0, 'C', '0', '0', 'collab:task:list', 'list', 'admin', sysdate(), '', NULL, '协作任务管理');

-- 3. F button permissions (2310-2319)
INSERT INTO sys_menu VALUES('2310', '文档列表', '2302', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'collab:doc:list', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES('2311', '文档查询', '2302', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'collab:doc:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES('2312', '文档新增', '2302', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'collab:doc:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES('2313', '文档修改', '2302', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'collab:doc:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES('2314', '任务列表', '2303', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'collab:task:list', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES('2315', '我的任务', '2301', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'collab:task:mine', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES('2316', '任务新增', '2303', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'collab:task:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES('2317', '任务修改', '2303', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'collab:task:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES('2318', '任务提交', '2301', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'collab:task:submit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES('2319', '协作者资格', '2303', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'collab:member:eligible', '#', 'admin', sysdate(), '', NULL, '');

-- 4. Predefined roles
INSERT INTO sys_role(role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
VALUES
(102, '协作管理员', 'collab_admin', 12, '1', 1, 1, '0', '0', 'admin', sysdate(), 'Collab M1 full admin'),
(103, '协作者', 'collab_member', 13, '1', 1, 1, '0', '0', 'admin', sysdate(), 'Collab M1 member submit only');

-- 5. sys_role_menu: admin (1) and collab_admin (102) get all new menus
INSERT INTO sys_role_menu VALUES ('1', '2300');
INSERT INTO sys_role_menu VALUES ('1', '2301');
INSERT INTO sys_role_menu VALUES ('1', '2302');
INSERT INTO sys_role_menu VALUES ('1', '2303');
INSERT INTO sys_role_menu VALUES ('1', '2310');
INSERT INTO sys_role_menu VALUES ('1', '2311');
INSERT INTO sys_role_menu VALUES ('1', '2312');
INSERT INTO sys_role_menu VALUES ('1', '2313');
INSERT INTO sys_role_menu VALUES ('1', '2314');
INSERT INTO sys_role_menu VALUES ('1', '2315');
INSERT INTO sys_role_menu VALUES ('1', '2316');
INSERT INTO sys_role_menu VALUES ('1', '2317');
INSERT INTO sys_role_menu VALUES ('1', '2318');
INSERT INTO sys_role_menu VALUES ('1', '2319');

INSERT INTO sys_role_menu VALUES ('102', '2300');
INSERT INTO sys_role_menu VALUES ('102', '2301');
INSERT INTO sys_role_menu VALUES ('102', '2302');
INSERT INTO sys_role_menu VALUES ('102', '2303');
INSERT INTO sys_role_menu VALUES ('102', '2310');
INSERT INTO sys_role_menu VALUES ('102', '2311');
INSERT INTO sys_role_menu VALUES ('102', '2312');
INSERT INTO sys_role_menu VALUES ('102', '2313');
INSERT INTO sys_role_menu VALUES ('102', '2314');
INSERT INTO sys_role_menu VALUES ('102', '2315');
INSERT INTO sys_role_menu VALUES ('102', '2316');
INSERT INTO sys_role_menu VALUES ('102', '2317');
INSERT INTO sys_role_menu VALUES ('102', '2318');
INSERT INTO sys_role_menu VALUES ('102', '2319');

-- collab_member (103): directory + my tasks + submit-related F perms only
INSERT INTO sys_role_menu VALUES ('103', '2300');
INSERT INTO sys_role_menu VALUES ('103', '2301');
INSERT INTO sys_role_menu VALUES ('103', '2315');
INSERT INTO sys_role_menu VALUES ('103', '2318');
