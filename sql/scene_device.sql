-- Phase B2: scene device master data, seed, menus
-- Database: ry-vue

drop table if exists scene_device;
create table scene_device (
  device_id         varchar(64)   not null comment 'device id',
  device_name       varchar(100)  not null,
  ip                varchar(45)   not null,
  mac               varchar(17)   default null,
  device_type       varchar(20)   not null,
  building_id       varchar(64)   not null,
  parent_device_id  varchar(64)   default null,
  del_flag          char(1)       default '0',
  create_by         varchar(64)   default '',
  create_time       datetime,
  update_by         varchar(64)   default '',
  update_time       datetime,
  remark            varchar(500)  default null,
  primary key (device_id)
) engine=innodb comment='scene device';

insert into scene_device (device_id, device_name, ip, mac, device_type, building_id, parent_device_id, del_flag, create_by, create_time, remark) values
('dev-core-sw1',     '核心交换机', '192.168.1.1',   'AA:BB:CC:DD:EE:01', 'switch',   'bldg-core', null,              '0', 'admin', sysdate(), ''),
('dev-core-router1', '核心路由',   '192.168.1.254', 'AA:BB:CC:DD:EE:02', 'router',   'bldg-core', 'dev-core-sw1',    '0', 'admin', sysdate(), ''),
('dev-a-sw1',        '交换机 A',   '192.168.10.1',  'AA:BB:CC:DD:EE:03', 'switch',   'bldg-a',    'dev-core-sw1',    '0', 'admin', sysdate(), ''),
('dev-a-term1',      '终端 A1',    '192.168.10.11', 'AA:BB:CC:DD:EE:04', 'terminal', 'bldg-a',    'dev-a-sw1',       '0', 'admin', sysdate(), ''),
('dev-b-sw1',        '交换机 B',   '192.168.20.1',  'AA:BB:CC:DD:EE:05', 'switch',   'bldg-b',    'dev-core-sw1',    '0', 'admin', sysdate(), ''),
('dev-c-term1',      '终端 C1',    '192.168.30.11', 'AA:BB:CC:DD:EE:06', 'terminal', 'bldg-c',    'dev-core-sw1',    '0', 'admin', sysdate(), ''),
('dev-d-router1',    '入口路由',   '192.168.0.1',   'AA:BB:CC:DD:EE:07', 'router',   'bldg-d',    'dev-core-router1','0', 'admin', sysdate(), '');

delete from sys_role_menu where menu_id between 2100 and 2106;
delete from sys_menu where menu_id between 2100 and 2106;

-- Hidden permission-only menus (unique path/route_name; do not overwrite /cesium constantRoutes)
insert into sys_menu values('2100', '场景权限', '0', '5', 'scene-perm', null, '', 'ScenePermRoot', 1, 0, 'M', '1', '0', '', 'lock', 'admin', sysdate(), '', null, '权限目录(隐藏)');
insert into sys_menu values('2101', '设备权限', '2100', '1', 'device-perm', null, '', 'SceneDevicePerm', 1, 0, 'C', '1', '0', 'scene:device:list', '#', 'admin', sysdate(), '', null, '权限页(隐藏)');
insert into sys_menu values('2102', '设备查询', '2101', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'scene:device:list',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2103', '设备详细', '2101', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'scene:device:query',  '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2104', '设备新增', '2101', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'scene:device:add',    '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2105', '设备修改', '2101', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'scene:device:edit',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2106', '设备删除', '2101', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'scene:device:remove', '#', 'admin', sysdate(), '', null, '');

insert into sys_role_menu values ('1', '2100');
insert into sys_role_menu values ('1', '2101');
insert into sys_role_menu values ('1', '2102');
insert into sys_role_menu values ('1', '2103');
insert into sys_role_menu values ('1', '2104');
insert into sys_role_menu values ('1', '2105');
insert into sys_role_menu values ('1', '2106');
