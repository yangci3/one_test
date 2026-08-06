-- Phase P1.5: scene RBAC menus and net_admin/net_viewer roles
-- Database: ry-vue

-- PRECHECK: SELECT role_id, role_key FROM sys_role WHERE role_id IN (100,101) OR role_key IN ('net_admin','net_viewer');

-- 1. Clean legacy hidden menus and conflicting cesium routes
DELETE FROM sys_role_menu WHERE menu_id BETWEEN 2100 AND 2113;
DELETE FROM sys_menu WHERE menu_id BETWEEN 2100 AND 2113;

DELETE rm FROM sys_role_menu rm
INNER JOIN sys_menu m ON rm.menu_id = m.menu_id
WHERE m.route_name IN ('CesiumMap', 'CesiumDevice') AND m.path = 'cesium';
DELETE FROM sys_menu WHERE route_name IN ('CesiumMap', 'CesiumDevice') AND path = 'cesium';

-- Idempotent cleanup for P1.5 menu band
DELETE FROM sys_role_menu WHERE menu_id BETWEEN 2200 AND 2299;
DELETE FROM sys_menu WHERE menu_id BETWEEN 2200 AND 2299;
DELETE FROM sys_role_menu WHERE role_id IN (100, 101);
DELETE FROM sys_role WHERE role_id IN (100, 101) OR role_key IN ('net_admin', 'net_viewer');

-- 2. Visible directory and C menus (2200-2205)
INSERT INTO sys_menu VALUES('2200', '三维地图', '0', '5', 'scene-map', NULL, '', 'SceneMap', 1, 0, 'M', '0', '0', '', 'map', 'admin', sysdate(), '', NULL, '三维地图目录');
INSERT INTO sys_menu VALUES('2201', '三维场景', '2200', '1', 'index', 'cesium/Index', '', 'CesiumScene', 1, 0, 'C', '0', '0', 'scene:map:view', 'international', 'admin', sysdate(), '', NULL, '三维场景');
INSERT INTO sys_menu VALUES('2202', '网络拓扑', '2200', '5', 'topology', 'cesium/topology/index', '', 'CesiumTopology', 1, 0, 'C', '0', '0', 'scene:topology:query', 'tree-table', 'admin', sysdate(), '', NULL, '网络拓扑');
INSERT INTO sys_menu VALUES('2203', '设备管理', '2200', '2', 'device', 'cesium/device/index', '', 'CesiumDevice', 1, 0, 'C', '0', '0', 'scene:device:list', 'server', 'admin', sysdate(), '', NULL, '设备管理');
INSERT INTO sys_menu VALUES('2204', '网络监控设置', '2200', '3', 'monitor-settings', 'cesium/monitorSettings/index', '', 'CesiumMonitorSettings', 1, 0, 'C', '0', '0', 'scene:settings:query', 'edit', 'admin', sysdate(), '', NULL, '网络监控设置');
INSERT INTO sys_menu VALUES('2205', '监控历史', '2200', '4', 'probe-history', 'cesium/probeHistory/index', '', 'CesiumProbeHistory', 1, 0, 'C', '0', '0', 'scene:history:list', 'time', 'admin', sysdate(), '', NULL, '监控历史');

-- 3. F button permissions (2210+)
INSERT INTO sys_menu VALUES('2210', '设备列表', '2203', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'scene:device:list', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES('2211', '设备查询', '2203', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'scene:device:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES('2212', '设备新增', '2203', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'scene:device:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES('2213', '设备修改', '2203', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'scene:device:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES('2214', '设备删除', '2203', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'scene:device:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES('2215', '拓扑编辑', '2202', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'scene:topology:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES('2216', '探测查询', '2201', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'scene:probe:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES('2217', '探测编辑', '2201', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'scene:probe:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES('2218', '设置编辑', '2204', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'scene:settings:edit', '#', 'admin', sysdate(), '', NULL, '');

-- 4. Predefined roles
INSERT INTO sys_role(role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
VALUES
(100, '网络管理员', 'net_admin', 10, '1', 1, 1, '0', '0', 'admin', sysdate(), 'P1.5 scene full'),
(101, '网络用户', 'net_viewer', 11, '1', 1, 1, '0', '0', 'admin', sysdate(), 'P1.5 scene static viewer');

-- 5. sys_role_menu: admin (1) and net_admin (100) get all new menus
INSERT INTO sys_role_menu VALUES ('1', '2200');
INSERT INTO sys_role_menu VALUES ('1', '2201');
INSERT INTO sys_role_menu VALUES ('1', '2202');
INSERT INTO sys_role_menu VALUES ('1', '2203');
INSERT INTO sys_role_menu VALUES ('1', '2204');
INSERT INTO sys_role_menu VALUES ('1', '2205');
INSERT INTO sys_role_menu VALUES ('1', '2210');
INSERT INTO sys_role_menu VALUES ('1', '2211');
INSERT INTO sys_role_menu VALUES ('1', '2212');
INSERT INTO sys_role_menu VALUES ('1', '2213');
INSERT INTO sys_role_menu VALUES ('1', '2214');
INSERT INTO sys_role_menu VALUES ('1', '2215');
INSERT INTO sys_role_menu VALUES ('1', '2216');
INSERT INTO sys_role_menu VALUES ('1', '2217');
INSERT INTO sys_role_menu VALUES ('1', '2218');

INSERT INTO sys_role_menu VALUES ('100', '2200');
INSERT INTO sys_role_menu VALUES ('100', '2201');
INSERT INTO sys_role_menu VALUES ('100', '2202');
INSERT INTO sys_role_menu VALUES ('100', '2203');
INSERT INTO sys_role_menu VALUES ('100', '2204');
INSERT INTO sys_role_menu VALUES ('100', '2205');
INSERT INTO sys_role_menu VALUES ('100', '2210');
INSERT INTO sys_role_menu VALUES ('100', '2211');
INSERT INTO sys_role_menu VALUES ('100', '2212');
INSERT INTO sys_role_menu VALUES ('100', '2213');
INSERT INTO sys_role_menu VALUES ('100', '2214');
INSERT INTO sys_role_menu VALUES ('100', '2215');
INSERT INTO sys_role_menu VALUES ('100', '2216');
INSERT INTO sys_role_menu VALUES ('100', '2217');
INSERT INTO sys_role_menu VALUES ('100', '2218');

-- net_viewer (101): scene + topology + read-only device list/query (no write/probe/settings/history menus)
INSERT INTO sys_role_menu VALUES ('101', '2200');
INSERT INTO sys_role_menu VALUES ('101', '2201');
INSERT INTO sys_role_menu VALUES ('101', '2202');
INSERT INTO sys_role_menu VALUES ('101', '2210');
INSERT INTO sys_role_menu VALUES ('101', '2211');
