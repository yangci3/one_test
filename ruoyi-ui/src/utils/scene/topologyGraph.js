/**
 * Pure topology helpers: devices -> nodes/edges, focus subgraph, level layout.
 */

export function edgeId(fromDeviceId, toDeviceId) {
  return 'edge-' + fromDeviceId + '-' + toDeviceId
}

export function buildGraph(devices) {
  const list = Array.isArray(devices) ? devices.filter(d => d && d.id) : []
  const byId = {}
  list.forEach(d => {
    byId[d.id] = d
  })

  const nodes = list.map(d => ({
    id: d.id,
    name: d.name || d.id,
    ip: d.ip || '',
    type: d.type || 'other',
    buildingId: d.buildingId || null,
    parentDeviceId: d.parentDeviceId || null
  }))

  const edges = []
  const seen = {}
  list.forEach(d => {
    const parentId = d.parentDeviceId
    if (!parentId || !byId[parentId]) return
    if (parentId === d.id) return
    const id = edgeId(parentId, d.id)
    if (seen[id]) return
    seen[id] = true
    edges.push({
      id,
      fromDeviceId: parentId,
      toDeviceId: d.id
    })
  })

  return { nodes, edges }
}

function childrenMap(edges) {
  const map = {}
  edges.forEach(e => {
    if (!map[e.fromDeviceId]) map[e.fromDeviceId] = []
    map[e.fromDeviceId].push(e.toDeviceId)
  })
  return map
}

function parentMap(nodes) {
  const map = {}
  nodes.forEach(n => {
    map[n.id] = n.parentDeviceId || null
  })
  return map
}

/**
 * Focus subgraph: ancestors + self + all descendants; edges with both ends in set.
 */
export function focusSubgraph(graph, focusDeviceId) {
  const empty = { nodes: [], edges: [] }
  if (!graph || !focusDeviceId) return empty
  const nodes = graph.nodes || []
  const edges = graph.edges || []
  const nodeById = {}
  nodes.forEach(n => {
    nodeById[n.id] = n
  })
  if (!nodeById[focusDeviceId]) return empty

  const keep = {}
  keep[focusDeviceId] = true

  // Ancestors
  const parents = parentMap(nodes)
  let cur = parents[focusDeviceId]
  const guard = {}
  while (cur && nodeById[cur] && !guard[cur]) {
    guard[cur] = true
    keep[cur] = true
    cur = parents[cur]
  }

  // Descendants
  const kids = childrenMap(edges)
  const stack = [focusDeviceId]
  while (stack.length) {
    const id = stack.pop()
    const list = kids[id] || []
    list.forEach(cid => {
      if (keep[cid]) return
      keep[cid] = true
      stack.push(cid)
    })
  }

  const outNodes = nodes.filter(n => keep[n.id])
  const outEdges = edges.filter(e => keep[e.fromDeviceId] && keep[e.toDeviceId])
  return { nodes: outNodes, edges: outEdges }
}

/**
 * Assign depth levels for layout. Roots (no parent in graph) at level 0.
 * Returns { levels: string[][], positions: { [id]: { x, y, level, index } } }
 */
export function layoutLevels(graph, options) {
  const opts = options || {}
  const hGap = opts.hGap != null ? opts.hGap : 160
  const vGap = opts.vGap != null ? opts.vGap : 90
  const nodes = (graph && graph.nodes) || []
  const edges = (graph && graph.edges) || []
  if (!nodes.length) {
    return { levels: [], positions: {} }
  }

  const ids = {}
  nodes.forEach(n => {
    ids[n.id] = true
  })
  const parents = {}
  const kids = {}
  nodes.forEach(n => {
    kids[n.id] = []
    const p = n.parentDeviceId
    parents[n.id] = p && ids[p] ? p : null
  })
  edges.forEach(e => {
    if (!ids[e.fromDeviceId] || !ids[e.toDeviceId]) return
    kids[e.fromDeviceId].push(e.toDeviceId)
  })

  const depth = {}
  function depthOf(id, stack) {
    if (depth[id] != null) return depth[id]
    if (stack[id]) {
      depth[id] = 0
      return 0
    }
    stack[id] = true
    const p = parents[id]
    depth[id] = p ? depthOf(p, stack) + 1 : 0
    delete stack[id]
    return depth[id]
  }
  nodes.forEach(n => depthOf(n.id, {}))

  const levelBuckets = {}
  nodes.forEach(n => {
    const lv = depth[n.id] || 0
    if (!levelBuckets[lv]) levelBuckets[lv] = []
    levelBuckets[lv].push(n.id)
  })
  const maxLv = Math.max.apply(null, Object.keys(levelBuckets).map(Number).concat([0]))
  const levels = []
  for (let i = 0; i <= maxLv; i++) {
    const row = (levelBuckets[i] || []).slice().sort()
    levels.push(row)
  }

  const positions = {}
  levels.forEach((row, li) => {
    const width = Math.max(row.length - 1, 0) * hGap
    row.forEach((id, idx) => {
      positions[id] = {
        x: idx * hGap - width / 2,
        y: li * vGap,
        level: li,
        index: idx
      }
    })
  })

  return { levels, positions }
}

const api = {
  edgeId,
  buildGraph,
  focusSubgraph,
  layoutLevels
}

export default api

if (typeof module !== 'undefined' && module.exports) {
  module.exports = api
  module.exports.edgeId = edgeId
  module.exports.buildGraph = buildGraph
  module.exports.focusSubgraph = focusSubgraph
  module.exports.layoutLevels = layoutLevels
}
