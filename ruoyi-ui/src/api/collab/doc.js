import request from '@/utils/request'

export function listDoc(query) {
  return request({
    url: '/collab/doc/list',
    method: 'get',
    params: query
  })
}

export function getDoc(docId) {
  return request({
    url: '/collab/doc/' + docId,
    method: 'get'
  })
}

export function addDoc(data) {
  return request({
    url: '/collab/doc',
    method: 'post',
    data: data
  })
}

export function updateDoc(data) {
  return request({
    url: '/collab/doc',
    method: 'put',
    data: data
  })
}
