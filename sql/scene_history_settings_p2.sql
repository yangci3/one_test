-- Phase P2: scene probe event history and per-user monitor settings
-- Database: ry-vue

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

-- P1.5 has scene:settings:edit F button (2218); add history list F button if missing
insert into sys_menu values('2220', '历史查询', '2205', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'scene:history:list', '#', 'admin', sysdate(), '', null, '');

insert into sys_role_menu values ('1', '2220');
insert into sys_role_menu values ('100', '2220');
