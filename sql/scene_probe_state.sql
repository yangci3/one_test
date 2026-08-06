-- Phase P1: scene probe state table and hidden permission menus
-- Database: ry-vue

create table if not exists scene_probe_state (
  device_id       varchar(64)  not null,
  monitoring      char(1)      not null default '0',
  status          varchar(16)  not null default 'unknown',
  last_change_at  bigint(20)   default null,
  update_time     datetime,
  primary key (device_id)
) engine=innodb comment='scene probe state';

delete from sys_role_menu where menu_id between 2110 and 2113;
delete from sys_menu where menu_id between 2110 and 2113;

-- Hidden permission-only menus (unique path/route_name; do not overwrite /cesium constantRoutes)
insert into sys_menu values('2110', 'Ì½²âÈ¨ÏÞ', '0', '6', 'scene-probe-perm', null, '', 'SceneProbePermRoot', 1, 0, 'M', '1', '0', '', 'lock', 'admin', sysdate(), '', null, 'È¨ÏÞÄ¿Â¼(Òþ²Ø)');
insert into sys_menu values('2111', 'Ì½²âÈ¨ÏÞ', '2110', '1', 'probe-perm', null, '', 'SceneProbePerm', 1, 0, 'C', '1', '0', 'scene:probe:query', '#', 'admin', sysdate(), '', null, 'È¨ÏÞÒ³(Òþ²Ø)');
insert into sys_menu values('2112', 'Ì½²â²éÑ¯', '2111', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'scene:probe:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2113', 'Ì½²â±à¼­', '2111', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'scene:probe:edit', '#', 'admin', sysdate(), '', null, '');

insert into sys_role_menu values ('1', '2110');
insert into sys_role_menu values ('1', '2111');
insert into sys_role_menu values ('1', '2112');
insert into sys_role_menu values ('1', '2113');
