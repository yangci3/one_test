const assert = require('assert')
const g = require('../../src/utils/scene/topologyGraph')

const devices = [
  { id: 'dev-core-sw1', name: 'core', ip: '1', type: 'switch', buildingId: 'bldg-core', parentDeviceId: null },
  { id: 'dev-core-router1', name: 'r', ip: '2', type: 'router', buildingId: 'bldg-core', parentDeviceId: 'dev-core-sw1' },
  { id: 'dev-a-sw1', name: 'a', ip: '3', type: 'switch', buildingId: 'bldg-a', parentDeviceId: 'dev-core-sw1' },
  { id: 'dev-a-term1', name: 't', ip: '4', type: 'terminal', buildingId: 'bldg-a', parentDeviceId: 'dev-a-sw1' },
  { id: 'dev-b-sw1', name: 'b', ip: '5', type: 'switch', buildingId: 'bldg-b', parentDeviceId: 'dev-core-sw1' },
  { id: 'dev-c-term1', name: 'c', ip: '6', type: 'terminal', buildingId: 'bldg-c', parentDeviceId: 'dev-core-sw1' },
  { id: 'dev-d-router1', name: 'd', ip: '7', type: 'router', buildingId: 'bldg-d', parentDeviceId: 'dev-core-router1' }
]

function run() {
  const graph = g.buildGraph(devices)
  assert.strictEqual(graph.nodes.length, 7)
  assert.strictEqual(graph.edges.length, 6)
  const e = graph.edges.find(x => x.toDeviceId === 'dev-a-sw1')
  assert.strictEqual(e.fromDeviceId, 'dev-core-sw1')
  assert.strictEqual(e.id, 'edge-dev-core-sw1-dev-a-sw1')

  // Focus on A switch: ancestors core-sw1, descendants a-term1; NOT b-sw1 branch only? b is sibling under core - NOT descendant of a-sw1
  // ancestors of a-sw1: core-sw1; descendants: a-term1; self: a-sw1
  // Should NOT include b-sw1, c-term1, core-router1, d-router1
  const sub = g.focusSubgraph(graph, 'dev-a-sw1')
  const ids = sub.nodes.map(n => n.id).sort()
  assert.deepStrictEqual(ids, ['dev-a-sw1', 'dev-a-term1', 'dev-core-sw1'].sort())
  assert.ok(sub.edges.every(ed => ids.includes(ed.fromDeviceId) && ids.includes(ed.toDeviceId)))
  assert.ok(!ids.includes('dev-b-sw1'))

  // Focus core: almost all
  const subCore = g.focusSubgraph(graph, 'dev-core-sw1')
  assert.strictEqual(subCore.nodes.length, 7)

  const layout = g.layoutLevels(graph)
  assert.ok(layout.positions['dev-core-sw1'].level === 0)
  assert.ok(layout.positions['dev-a-term1'].level >= 2)

  assert.strictEqual(g.focusSubgraph(graph, 'missing').nodes.length, 0)
  console.log('topology-graph.test.js OK')
}

run()
