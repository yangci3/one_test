const assert = require('assert')
const fs = require('fs')
const path = require('path')

function run() {
  const src = fs.readFileSync(path.join(__dirname, '../../src/api/scene/topology.js'), 'utf8')
  assert.ok(src.includes("import request from '@/utils/request'"))
  assert.ok(src.includes('export function getTopologyGraph'))
  assert.ok(src.includes('export function getLinkDetail'))
  assert.ok(src.includes("url: '/scene/topology/graph'"))
  assert.ok(src.includes("'/scene/topology/edge/'"))
  assert.ok(!src.includes("from '@/api/scene/device'"))
  assert.ok(!src.includes("from '@/api/scene/buildings'"))
  console.log('topology-api.test.js OK')
}

run()
