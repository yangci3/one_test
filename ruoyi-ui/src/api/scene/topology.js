import { listDevices } from '@/api/scene/device'
import { buildGraph, focusSubgraph, edgeId } from '@/utils/scene/topologyGraph'
import { getBuildings } from '@/api/scene/buildings'

function buildingNameMap(buildings) {
  const map = {}
  ;(buildings || []).forEach(b => {
    if (b && b.id) map[b.id] = b.name || b.id
  })
  return map
}

function enrichNode(node, nameMap) {
  return {
    id: node.id,
    name: node.name,
    ip: node.ip,
    type: node.type,
    buildingId: node.buildingId,
    buildingName: node.buildingId ? (nameMap[node.buildingId] || node.buildingId) : '',
    parentDeviceId: node.parentDeviceId || null
  }
}

function devicesFromRes(res) {
  if (res && res.code === 200 && Array.isArray(res.data)) {
    return res.data
  }
  return []
}

export function getTopologyGraph(query) {
  return listDevices({}).then(deviceRes => {
    const devices = devicesFromRes(deviceRes)
    const full = buildGraph(devices)
    const focusDeviceId = query && query.focusDeviceId
    const graph = focusDeviceId ? focusSubgraph(full, focusDeviceId) : full
    if (focusDeviceId && !full.nodes.some(n => n.id === focusDeviceId)) {
      return { code: 500, msg: '\u8bbe\u5907\u4e0d\u5b58\u5728', data: null }
    }
    return getBuildings().then(res => {
      const buildings = (res && res.data) || []
      const nameMap = buildingNameMap(buildings)
      return {
        code: 200,
        msg: 'success',
        data: {
          nodes: graph.nodes.map(n => enrichNode(n, nameMap)),
          edges: graph.edges.slice()
        }
      }
    })
  })
}

function parseEdgeKey(edgeIdOrObj, devices) {
  if (!edgeIdOrObj) return null
  if (typeof edgeIdOrObj === 'string') {
    const prefix = 'edge-'
    if (!edgeIdOrObj.startsWith(prefix)) return null
    const rest = edgeIdOrObj.slice(prefix.length)
    for (let i = 0; i < devices.length; i++) {
      for (let j = 0; j < devices.length; j++) {
        if (i === j) continue
        const from = devices[i].id
        const to = devices[j].id
        if (edgeId(from, to) === edgeIdOrObj) {
          return { fromDeviceId: from, toDeviceId: to, edgeId: edgeIdOrObj }
        }
      }
    }
    for (let i = 0; i < devices.length; i++) {
      const from = devices[i].id
      const head = from + '-'
      if (rest.startsWith(head)) {
        return {
          fromDeviceId: from,
          toDeviceId: rest.slice(head.length),
          edgeId: edgeIdOrObj
        }
      }
    }
    return null
  }
  if (typeof edgeIdOrObj === 'object') {
    const fromDeviceId = edgeIdOrObj.fromDeviceId
    const toDeviceId = edgeIdOrObj.toDeviceId
    if (!fromDeviceId || !toDeviceId) return null
    return {
      fromDeviceId,
      toDeviceId,
      edgeId: edgeIdOrObj.edgeId || edgeId(fromDeviceId, toDeviceId)
    }
  }
  return null
}

export function getLinkDetail(edgeIdOrObj) {
  return listDevices({}).then(deviceRes => {
    const devices = devicesFromRes(deviceRes)
    const key = parseEdgeKey(edgeIdOrObj, devices)
    if (!key) {
      return { code: 500, msg: '\u94fe\u8def\u4e0d\u5b58\u5728', data: null }
    }
    const from = devices.find(d => d && d.id === key.fromDeviceId)
    const to = devices.find(d => d && d.id === key.toDeviceId)
    if (!from || !to) {
      return { code: 500, msg: '\u94fe\u8def\u4e0d\u5b58\u5728', data: null }
    }
    return getBuildings().then(res => {
      const nameMap = buildingNameMap((res && res.data) || [])
      return {
        code: 200,
        msg: 'success',
        data: {
          edgeId: key.edgeId,
          from: enrichNode(
            {
              id: from.id,
              name: from.name,
              ip: from.ip,
              type: from.type,
              buildingId: from.buildingId
            },
            nameMap
          ),
          to: enrichNode(
            {
              id: to.id,
              name: to.name,
              ip: to.ip,
              type: to.type,
              buildingId: to.buildingId
            },
            nameMap
          )
        }
      }
    })
  })
}
