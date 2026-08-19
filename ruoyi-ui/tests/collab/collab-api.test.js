const assert = require('assert')
const fs = require('fs')
const path = require('path')

/**
 * collab API modules use webpack `@/` imports and cannot be required in Node;
 * verify exports and backend URL wiring from source.
 */
function readApi(name) {
  return fs.readFileSync(
    path.join(__dirname, '../../src/api/collab/' + name + '.js'),
    'utf8'
  )
}

function run() {
  const userSrc = readApi('user')
  assert.ok(userSrc.includes("import request from '@/utils/request'"))
  assert.ok(userSrc.includes('export function listCandidates'))
  assert.ok(userSrc.includes("url: '/collab/user/candidates'"))

  const docSrc = readApi('doc')
  assert.ok(docSrc.includes("import request from '@/utils/request'"))
  ;['listDoc', 'getDoc', 'addDoc', 'updateDoc'].forEach(name => {
    assert.ok(docSrc.includes('export function ' + name), 'missing export ' + name)
  })
  assert.ok(docSrc.includes("url: '/collab/doc/list'"))
  assert.ok(docSrc.includes("url: '/collab/doc/' + docId"))
  assert.ok(docSrc.includes("url: '/collab/doc'"))
  assert.ok(docSrc.includes("method: 'post'"))
  assert.ok(docSrc.includes("method: 'put'"))

  const taskSrc = readApi('task')
  assert.ok(taskSrc.includes("import request from '@/utils/request'"))
  ;[
    'listTask',
    'listMyTask',
    'getTask',
    'addTask',
    'saveDraft',
    'submitAssignment',
    'extendDeadline',
    'allowResubmit',
    'listUnsubmitted',
    'mergeTask'
  ].forEach(name => {
    assert.ok(taskSrc.includes('export function ' + name), 'missing export ' + name)
  })
  assert.ok(taskSrc.includes("url: '/collab/task/list'"))
  assert.ok(taskSrc.includes("url: '/collab/task/mine'"))
  assert.ok(taskSrc.includes("url: '/collab/task/' + taskId"))
  assert.ok(taskSrc.includes("'/assignment/' + assignmentId + '/draft'"))
  assert.ok(taskSrc.includes("'/assignment/' + assignmentId + '/submit'"))
  assert.ok(taskSrc.includes("'/deadline'"))
  assert.ok(taskSrc.includes("'/allow-resubmit'"))
  assert.ok(taskSrc.includes("'/unsubmitted'"))
  assert.ok(taskSrc.includes("'/merge'"))

  console.log('collab-api.test.js PASS')
}

run()
