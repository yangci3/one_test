import request from '@/utils/request'
import { edgeId } from '@/utils/scene/topologyGraph'

export function getTopologyGraph(query) {
  return request({
    url: '/scene/topology/graph',
    method: 'get',
    params: query || {}
  })
}

function resolveEdgeId(edgeIdOrObj) {
  if (!edgeIdOrObj) return ''
  if (typeof edgeIdOrObj === 'string') return edgeIdOrObj
  if (edgeIdOrObj.edgeId) return edgeIdOrObj.edgeId
  if (edgeIdOrObj.fromDeviceId && edgeIdOrObj.toDeviceId) {
    return edgeId(edgeIdOrObj.fromDeviceId, edgeIdOrObj.toDeviceId)
  }
  return ''
}

export function getLinkDetail(edgeIdOrObj) {
  const id = resolveEdgeId(edgeIdOrObj)
  return request({
    url: '/scene/topology/edge/' + encodeURIComponent(id),
    method: 'get'
  })
}
