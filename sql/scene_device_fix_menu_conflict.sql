-- Fix duplicate sidebar "设备管理" / missing topology:
-- Backend menus used path=cesium and route_name=CesiumMap/CesiumDevice,
-- which overwrote constantRoutes children (topology, history, settings).
-- Keep permission buttons; hide directory+page and use unique route names/paths.

UPDATE sys_menu
SET menu_name = '场景权限',
    path = 'scene-perm',
    component = null,
    route_name = 'ScenePermRoot',
    visible = '1',
    perms = '',
    remark = '权限目录(隐藏，避免覆盖三维地图路由)'
WHERE menu_id = 2100;

UPDATE sys_menu
SET menu_name = '设备权限',
    path = 'device-perm',
    component = null,
    route_name = 'SceneDevicePerm',
    visible = '1',
    perms = 'scene:device:list',
    remark = '权限页(隐藏)'
WHERE menu_id = 2101;
