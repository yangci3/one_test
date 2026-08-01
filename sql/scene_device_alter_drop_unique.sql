-- Phase B2 fix: drop table-level UNIQUE on ip/mac (soft-delete reuse)
-- Uniqueness enforced in service via checkIpUnique/checkMacUnique (del_flag=0)
-- Database: ry-vue

ALTER TABLE scene_device DROP INDEX uk_scene_device_ip;
ALTER TABLE scene_device DROP INDEX uk_scene_device_mac;
