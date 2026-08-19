import request from '@/utils/request'

export function listCandidates() {
  return request({
    url: '/collab/user/candidates',
    method: 'get'
  })
}
