import auth from '@/plugins/auth'

export function hasSceneProbeQuery() {
  return auth.hasPermi('scene:probe:query')
}

export function hasSceneProbeEdit() {
  return auth.hasPermi('scene:probe:edit')
}

export function hasSceneMapView() {
  return auth.hasPermi('scene:map:view')
}
