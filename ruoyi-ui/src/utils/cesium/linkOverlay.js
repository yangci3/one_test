/**
 * Cesium polyline overlay for inter-building topology edges.
 */

const LINK_ENTITY_PREFIX = 'topo-link-'

export const LINK_HIGHLIGHT_COLOR = { r: 0.62, g: 0.28, b: 0.95, a: 0.65 }

function buildingCenterLocal(buildings, buildingId) {
  const list = Array.isArray(buildings) ? buildings : []
  const b = list.find(x => x && x.id === buildingId)
  if (!b || !b.center || b.center.length < 3) return null
  return { x: b.center[0], y: b.center[1], z: b.center[2] }
}

/**
 * @returns {{ clear: function, setGraph: function, destroy: function }}
 */
export function createLinkOverlay(viewer, frame, options) {
  const Cesium = typeof window !== 'undefined' ? window.Cesium : undefined
  const opts = options || {}
  const getBuildings = opts.getBuildings || (() => [])
  const onSelectEdge = opts.onSelectEdge
  const entityIds = []
  let handler = null

  function clear() {
    if (!viewer) return
    for (let i = 0; i < entityIds.length; i++) {
      const e = viewer.entities.getById(entityIds[i])
      if (e) viewer.entities.remove(e)
    }
    entityIds.length = 0
  }

  function setGraph(graph) {
    clear()
    if (!Cesium || !viewer || !frame || !graph) return
    const buildings = getBuildings()
    const nodesById = {}
    ;(graph.nodes || []).forEach(n => {
      nodesById[n.id] = n
    })
    const edges = graph.edges || []
    edges.forEach(edge => {
      const from = nodesById[edge.fromDeviceId]
      const to = nodesById[edge.toDeviceId]
      if (!from || !to) return
      if (!from.buildingId || !to.buildingId) return
      // Same-building edges: skip map polyline (still available in topology page).
      if (from.buildingId === to.buildingId) return
      const c0 = buildingCenterLocal(buildings, from.buildingId)
      const c1 = buildingCenterLocal(buildings, to.buildingId)
      if (!c0 || !c1) return
      // Lift line slightly above ground boxes.
      const z0 = (c0.z || 0) + 8
      const z1 = (c1.z || 0) + 8
      const p0 = frame.localToWorld({ x: c0.x, y: c0.y, z: z0 })
      const p1 = frame.localToWorld({ x: c1.x, y: c1.y, z: z1 })
      const id = LINK_ENTITY_PREFIX + edge.id
      viewer.entities.add({
        id,
        name: edge.id,
        polyline: {
          positions: [p0, p1],
          width: 4,
          material: new Cesium.Color(
            LINK_HIGHLIGHT_COLOR.r,
            LINK_HIGHLIGHT_COLOR.g,
            LINK_HIGHLIGHT_COLOR.b,
            0.95
          ),
          clampToGround: false
        },
        properties: {
          edgeId: edge.id
        }
      })
      entityIds.push(id)
    })
  }

  function bindPick() {
    if (!Cesium || !viewer || handler) return
    handler = new Cesium.ScreenSpaceEventHandler(viewer.scene.canvas)
    handler.setInputAction(movement => {
      const picked = viewer.scene.pick(movement.position)
      if (!Cesium.defined(picked) || !picked.id) return
      const ent = picked.id
      const eid = ent.id
      if (typeof eid !== 'string' || eid.indexOf(LINK_ENTITY_PREFIX) !== 0) return
      const edgeId = eid.slice(LINK_ENTITY_PREFIX.length)
      if (typeof onSelectEdge === 'function') {
        onSelectEdge(edgeId)
      }
    }, Cesium.ScreenSpaceEventType.LEFT_CLICK)
  }

  function destroy() {
    clear()
    if (handler && typeof handler.destroy === 'function') {
      handler.destroy()
    }
    handler = null
  }

  bindPick()

  return {
    clear,
    setGraph,
    destroy,
    LINK_HIGHLIGHT_COLOR
  }
}

export default createLinkOverlay
