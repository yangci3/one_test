import request from '@/utils/request'

export function listTask(query) {
  return request({
    url: '/collab/task/list',
    method: 'get',
    params: query
  })
}

export function listMyTask(query) {
  return request({
    url: '/collab/task/mine',
    method: 'get',
    params: query
  })
}

export function getTask(taskId) {
  return request({
    url: '/collab/task/' + taskId,
    method: 'get'
  })
}

export function addTask(data) {
  return request({
    url: '/collab/task',
    method: 'post',
    data: data
  })
}

export function saveDraft(taskId, assignmentId, data) {
  return request({
    url: '/collab/task/' + taskId + '/assignment/' + assignmentId + '/draft',
    method: 'put',
    data: data
  })
}

export function submitAssignment(taskId, assignmentId, data) {
  return request({
    url: '/collab/task/' + taskId + '/assignment/' + assignmentId + '/submit',
    method: 'post',
    data: data
  })
}

export function extendDeadline(taskId, data) {
  return request({
    url: '/collab/task/' + taskId + '/deadline',
    method: 'put',
    data: data
  })
}

export function allowResubmit(taskId, data) {
  return request({
    url: '/collab/task/' + taskId + '/allow-resubmit',
    method: 'put',
    data: data
  })
}

export function listUnsubmitted(taskId) {
  return request({
    url: '/collab/task/' + taskId + '/unsubmitted',
    method: 'get'
  })
}

export function mergeTask(taskId) {
  return request({
    url: '/collab/task/' + taskId + '/merge',
    method: 'post'
  })
}
