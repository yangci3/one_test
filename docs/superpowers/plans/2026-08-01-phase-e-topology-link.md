# Phase E Topology + Link Overlay Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Shared topology graph from `parentDeviceId`; map link mode (highlight + polylines + detail dialog); SVG topology page with all/focus modes.

**Architecture:** Pure `topologyGraph` builds nodes/edges and focus subgraph; `api/scene/topology.js` is RuoYi-shaped; `linkOverlay` draws Cesium polylines between building centers; Index toggles link mode; topology page layouts levels in SVG.

**Tech Stack:** Vue 2 + Element UI + Cesium entities + SVG (no graph library)

**Spec:** `docs/superpowers/specs/2026-08-01-phase-e-topology-link-design.md`

## Global Constraints

- Edges from `parentDeviceId` (parent -> child); edge id `edge-{from}-{to}`
- Focus = ancestors + self + all descendants
- Same-building edges: no map polyline
- Link highlight color distinct from monitor RGB (purple)
- API `{ code, msg, data }`; UTF-8 BOM docs; JS Chinese via \uXXXX preferred
- No edge CRUD / G6 / MySQL

---

## File Structure

| Path | Role |
| --- | --- |
| `ruoyi-ui/src/utils/scene/topologyGraph.js` | build / focus / layout |
| `ruoyi-ui/tests/cesium/topology-graph.test.js` | Node tests |
| `ruoyi-ui/src/api/scene/topology.js` | getTopologyGraph / getLinkDetail |
| `ruoyi-ui/src/utils/cesium/linkOverlay.js` | polyline overlay + pick |
| `ruoyi-ui/src/views/cesium/Index.vue` | link mode + detail dialog |
| `ruoyi-ui/src/views/cesium/topology/index.vue` | SVG page |
| `ruoyi-ui/src/router/index.js` | `/cesium/topology` |

---

### Task 1: topologyGraph + tests

- [ ] Implement `buildGraph(devices)`, `focusSubgraph(graph, focusId)`, `layoutLevels(graph)`
- [ ] Node tests on seed-like devices

### Task 2: topology API

- [ ] `getTopologyGraph` / `getLinkDetail` with buildingName from buildings list

### Task 3: linkOverlay + Index

- [ ] Draw/clear polylines; click -> edgeId callback
- [ ] Device row buttons; purple building colors in link mode; restore on clear

### Task 4: topology page + router

- [ ] Mode toggle + SVG + detail dialog; `?deviceId=`

### Task 5: Verify

- [ ] `node tests/cesium/topology-graph.test.js`
