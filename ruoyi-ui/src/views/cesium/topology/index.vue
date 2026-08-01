<template>
  <div class="app-container topology-page">
    <el-form :inline="true" size="small" class="topology-filter">
      <el-form-item :label="labels.mode">
        <el-radio-group v-model="mode" :disabled="selectMode" @change="onModeChange">
          <el-radio-button label="all">{{ labels.modeAll }}</el-radio-button>
          <el-radio-button label="focus">{{ labels.modeFocus }}</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="mode === 'focus'" :label="labels.device">
        <el-select
          v-model="focusDeviceId"
          filterable
          clearable
          :disabled="selectMode"
          :placeholder="labels.pickDevice"
          style="width: 260px"
          @change="loadGraph"
        >
          <el-option
            v-for="d in deviceOptions"
            :key="d.id"
            :label="d.name + ' (' + d.ip + ')'"
            :value="d.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-refresh" :disabled="selectMode" @click="loadGraph">{{ labels.refresh }}</el-button>
        <el-button v-if="!selectMode" type="success" plain @click="enterSelectMode">{{ labels.showDetail }}</el-button>
        <template v-else>
          <el-button type="primary" :disabled="!selectedIds.length" @click="confirmSelection">{{ labels.confirm }}</el-button>
          <el-button @click="cancelSelectMode">{{ labels.cancel }}</el-button>
          <span class="select-hint">{{ labels.selectHint }} ({{ selectedIds.length }})</span>
        </template>
      </el-form-item>
      <el-form-item class="topology-legend">
        <span class="legend-item"><i class="status-dot online" />{{ labels.statusOnline }}</span>
        <span class="legend-item"><i class="status-dot offline" />{{ labels.statusOffline }}</span>
        <span class="legend-item"><i class="status-dot unknown" />{{ labels.statusUnknown }}</span>
      </el-form-item>
    </el-form>

    <div v-loading="loading" class="topology-canvas-wrap">
      <svg
        v-if="graph.nodes.length"
        ref="topoSvg"
        class="topology-svg"
        :class="{ 'is-selecting': selectMode, 'is-dragging': !!dragId }"
        :viewBox="viewBoxStr"
        :width="svgSize.width"
        :height="svgSize.height"
        xmlns="http://www.w3.org/2000/svg"
        @mousemove="onSvgMouseMove"
        @mouseup="onSvgMouseUp"
        @mouseleave="onSvgMouseUp"
      >
        <line
          v-for="e in graph.edges"
          :key="e.id"
          class="topo-edge"
          :x1="pos(e.fromDeviceId).x"
          :y1="pos(e.fromDeviceId).y"
          :x2="pos(e.toDeviceId).x"
          :y2="pos(e.toDeviceId).y"
          @click.stop="onEdgeClick(e.id)"
        />
        <g
          v-for="n in graph.nodes"
          :key="n.id"
          class="topo-node"
          :class="{ selected: isSelected(n.id) }"
          @mousedown.stop="onNodeMouseDown($event, n.id)"
          @click.stop="onNodeClick(n.id)"
        >
          <rect
            :x="pos(n.id).x - 72"
            :y="pos(n.id).y - 32"
            width="144"
            height="64"
            rx="6"
            :class="'node-fill-' + nodeStatusKey(n.id)"
          />
          <!-- selection checkbox -->
          <rect
            v-if="selectMode"
            class="node-check-box"
            :x="pos(n.id).x + 48"
            :y="pos(n.id).y - 26"
            width="14"
            height="14"
            rx="2"
          />
          <path
            v-if="selectMode && isSelected(n.id)"
            class="node-check-mark"
            :d="checkPath(n.id)"
            fill="none"
          />
          <circle
            class="node-status-dot"
            :class="'fill-' + nodeStatusKey(n.id)"
            :cx="pos(n.id).x - 56"
            :cy="pos(n.id).y - 16"
            r="5"
          />
          <text class="node-name" :x="pos(n.id).x - 46" :y="pos(n.id).y - 12">{{ n.name }}</text>
          <text class="node-ip" :x="pos(n.id).x - 46" :y="pos(n.id).y + 4">{{ n.ip || labels.dash }}</text>
          <text
            class="node-status-text"
            :class="'text-' + nodeStatusKey(n.id)"
            :x="pos(n.id).x - 46"
            :y="pos(n.id).y + 20"
          >{{ nodeStatusLabel(n.id) }}</text>
        </g>
      </svg>
      <div v-else class="topology-empty">{{ emptyHint }}</div>
    </div>

    <el-dialog
      :title="labels.detailTitle"
      :visible.sync="detailOpen"
      width="480px"
      append-to-body
      custom-class="topology-link-dialog"
    >
      <div v-if="detail" class="topology-detail">
        <div class="topology-detail-label">{{ labels.upstream }}</div>
        <div>{{ detail.from.name }} ({{ detail.from.ip }})</div>
        <div>{{ typeLabel(detail.from.type) }} {{ labels.dot }} {{ detail.from.buildingName || detail.from.buildingId || labels.dash }}</div>
        <el-divider />
        <div class="topology-detail-label">{{ labels.downstream }}</div>
        <div>{{ detail.to.name }} ({{ detail.to.ip }})</div>
        <div>{{ typeLabel(detail.to.type) }} {{ labels.dot }} {{ detail.to.buildingName || detail.to.buildingId || labels.dash }}</div>
      </div>
    </el-dialog>

    <el-dialog
      :title="labels.deviceDetailTitle"
      :visible.sync="deviceDetailOpen"
      width="720px"
      append-to-body
      custom-class="topology-link-dialog"
    >
      <el-table :data="selectedDeviceRows" border size="small" max-height="420">
        <el-table-column :label="labels.colName" prop="name" min-width="110" show-overflow-tooltip />
        <el-table-column :label="labels.colIp" prop="ip" min-width="120" />
        <el-table-column :label="labels.colMac" prop="mac" min-width="140" show-overflow-tooltip />
        <el-table-column :label="labels.colType" prop="typeLabel" width="90" />
        <el-table-column :label="labels.colStatus" prop="statusLabel" width="90" />
        <el-table-column :label="labels.colBuilding" prop="buildingName" min-width="100" show-overflow-tooltip />
        <el-table-column :label="labels.colParent" prop="parentName" min-width="110" show-overflow-tooltip />
        <el-table-column :label="labels.colRemark" prop="remark" min-width="100" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { listDevices } from '@/api/scene/device'
import { listProbeStatus } from '@/api/scene/probe'
import { getTopologyGraph, getLinkDetail } from '@/api/scene/topology'
import { layoutLevels } from '@/utils/scene/topologyGraph'
import { getGlobalMonitorUpdateEventName } from '@/utils/scene/globalMonitorRuntime'

const TYPE_LABELS = {
  router: '\u8def\u7531\u5668',
  switch: '\u4ea4\u6362\u673a',
  terminal: '\u7ec8\u7aef',
  other: '\u5176\u4ed6'
}

const GLOBAL_MONITOR_EVENT = getGlobalMonitorUpdateEventName()
const NODE_HALF_W = 72
const NODE_HALF_H = 32

export default {
  name: 'CesiumTopology',
  data() {
    return {
      loading: false,
      mode: 'all',
      focusDeviceId: undefined,
      deviceOptions: [],
      graph: { nodes: [], edges: [] },
      positions: {},
      probeById: {},
      detailOpen: false,
      detail: null,
      selectMode: false,
      selectedIds: [],
      deviceDetailOpen: false,
      dragId: null,
      dragMoved: false,
      dragOffset: { x: 0, y: 0 },
      labels: {
        mode: '\u6a21\u5f0f',
        modeAll: '\u5168\u90e8\u8fde\u63a5',
        modeFocus: '\u4e0e\u9009\u4e2d\u76f8\u5173',
        device: '\u8bbe\u5907',
        pickDevice: '\u8bf7\u9009\u62e9\u8bbe\u5907',
        refresh: '\u5237\u65b0',
        showDetail: '\u663e\u793a\u8be6\u60c5',
        confirm: '\u786e\u8ba4',
        cancel: '\u53d6\u6d88',
        selectHint: '\u70b9\u51fb\u8bbe\u5907\u6846\u52fe\u9009',
        detailTitle: '\u94fe\u8def\u8be6\u60c5',
        deviceDetailTitle: '\u8bbe\u5907\u8be6\u60c5',
        upstream: '\u4e0a\u6e38',
        downstream: '\u4e0b\u6e38',
        emptyAll: '\u6682\u65e0\u62d3\u6251\u6570\u636e',
        emptyFocus: '\u8bf7\u9009\u62e9\u8bbe\u5907\u67e5\u770b\u76f8\u5173\u62d3\u6251',
        dot: '\u00b7',
        dash: '\u2014',
        statusOnline: '\u6b63\u5e38',
        statusOffline: '\u65ad\u5f00',
        statusUnknown: '\u672a\u76d1\u63a7',
        colName: '\u8bbe\u5907\u540d\u79f0',
        colIp: 'IP',
        colMac: 'MAC',
        colType: '\u7c7b\u578b',
        colStatus: '\u72b6\u6001',
        colBuilding: '\u6240\u5c5e\u5efa\u7b51',
        colParent: '\u4e0a\u7ea7\u8bbe\u5907',
        colRemark: '\u5907\u6ce8',
        needSelect: '\u8bf7\u5148\u52fe\u9009\u8bbe\u5907'
      }
    }
  },
  computed: {
    emptyHint() {
      if (this.mode === 'focus' && !this.focusDeviceId) {
        return this.labels.emptyFocus
      }
      return this.labels.emptyAll
    },
    svgSize() {
      const vals = Object.keys(this.positions).map(id => this.positions[id])
      if (!vals.length) {
        return { minX: 0, minY: 0, width: 800, height: 400 }
      }
      let minX = Infinity
      let minY = Infinity
      let maxX = -Infinity
      let maxY = -Infinity
      vals.forEach(p => {
        minX = Math.min(minX, p.x - NODE_HALF_W)
        minY = Math.min(minY, p.y - NODE_HALF_H)
        maxX = Math.max(maxX, p.x + NODE_HALF_W)
        maxY = Math.max(maxY, p.y + NODE_HALF_H)
      })
      const pad = 48
      return {
        minX: minX - pad,
        minY: minY - pad,
        width: Math.max(maxX - minX + pad * 2, 320),
        height: Math.max(maxY - minY + pad * 2, 240)
      }
    },
    viewBoxStr() {
      const s = this.svgSize
      return s.minX + ' ' + s.minY + ' ' + s.width + ' ' + s.height
    },
    selectedDeviceRows() {
      return this.selectedIds.map(id => this.buildDeviceRow(id)).filter(Boolean)
    }
  },
  created() {
    const q = this.$route && this.$route.query
    if (q && q.deviceId) {
      this.mode = 'focus'
      this.focusDeviceId = q.deviceId
    }
    this.bindMonitorListener()
    this.loadDevices().then(() => this.loadGraph())
  },
  beforeDestroy() {
    this.unbindMonitorListener()
  },
  watch: {
    '$route.query.deviceId'(val) {
      if (val) {
        this.mode = 'focus'
        this.focusDeviceId = val
        this.loadGraph()
      }
    }
  },
  methods: {
    typeLabel(type) {
      return TYPE_LABELS[type] || TYPE_LABELS.other
    },
    pos(id) {
      return this.positions[id] || { x: 0, y: 0 }
    },
    isSelected(id) {
      return this.selectedIds.indexOf(id) >= 0
    },
    checkPath(id) {
      const x = this.pos(id).x + 50
      const y = this.pos(id).y - 24
      return 'M ' + x + ' ' + (y + 6) + ' L ' + (x + 4) + ' ' + (y + 10) + ' L ' + (x + 10) + ' ' + (y + 2)
    },
    nodeStatusKey(deviceId) {
      const probe = this.probeById[deviceId]
      if (!probe || !probe.monitoring) return 'unknown'
      if (probe.status === 'online') return 'online'
      if (probe.status === 'offline') return 'offline'
      return 'unknown'
    },
    nodeStatusLabel(deviceId) {
      const key = this.nodeStatusKey(deviceId)
      if (key === 'online') return this.labels.statusOnline
      if (key === 'offline') return this.labels.statusOffline
      return this.labels.statusUnknown
    },
    parentName(parentId) {
      if (!parentId) return this.labels.dash
      const found = this.deviceOptions.find(d => d.id === parentId)
      return found && found.name ? found.name : parentId
    },
    buildDeviceRow(id) {
      const fromGraph = (this.graph.nodes || []).find(n => n.id === id)
      const fromList = this.deviceOptions.find(d => d.id === id)
      if (!fromGraph && !fromList) return null
      const src = fromList || fromGraph
      return {
        id,
        name: src.name || id,
        ip: src.ip || '',
        mac: src.mac || '',
        typeLabel: this.typeLabel(src.type),
        statusLabel: this.nodeStatusLabel(id),
        buildingName: (fromGraph && fromGraph.buildingName) || src.buildingId || this.labels.dash,
        parentName: this.parentName(src.parentDeviceId || (fromGraph && fromGraph.parentDeviceId)),
        remark: (fromList && fromList.remark) || ''
      }
    },
    clientToSvg(evt) {
      const svg = this.$refs.topoSvg
      if (!svg || typeof svg.createSVGPoint !== 'function') {
        return { x: 0, y: 0 }
      }
      const pt = svg.createSVGPoint()
      pt.x = evt.clientX
      pt.y = evt.clientY
      const ctm = svg.getScreenCTM()
      if (!ctm) return { x: 0, y: 0 }
      const p = pt.matrixTransform(ctm.inverse())
      return { x: p.x, y: p.y }
    },
    onNodeMouseDown(evt, id) {
      if (evt.button != null && evt.button !== 0) return
      if (this.selectMode) return
      const p = this.clientToSvg(evt)
      const cur = this.pos(id)
      this.dragId = id
      this.dragMoved = false
      this.dragOffset = { x: p.x - cur.x, y: p.y - cur.y }
    },
    onSvgMouseMove(evt) {
      if (!this.dragId) return
      const p = this.clientToSvg(evt)
      const next = {
        x: p.x - this.dragOffset.x,
        y: p.y - this.dragOffset.y
      }
      const prev = this.pos(this.dragId)
      if (Math.abs(next.x - prev.x) > 1 || Math.abs(next.y - prev.y) > 1) {
        this.dragMoved = true
      }
      this.$set(this.positions, this.dragId, next)
    },
    onSvgMouseUp() {
      this.dragId = null
    },
    onNodeClick(id) {
      if (this.dragMoved) {
        this.dragMoved = false
        return
      }
      if (this.selectMode) {
        const idx = this.selectedIds.indexOf(id)
        if (idx >= 0) {
          this.selectedIds.splice(idx, 1)
        } else {
          this.selectedIds.push(id)
        }
      }
    },
    onEdgeClick(edgeId) {
      if (this.selectMode || this.dragId) return
      this.openDetail(edgeId)
    },
    enterSelectMode() {
      this.selectMode = true
      this.selectedIds = []
      this.dragId = null
    },
    cancelSelectMode() {
      this.selectMode = false
      this.selectedIds = []
    },
    confirmSelection() {
      if (!this.selectedIds.length) {
        this.$message.warning(this.labels.needSelect)
        return
      }
      this.deviceDetailOpen = true
    },
    bindMonitorListener() {
      if (typeof window === 'undefined' || this._onMonitorUpdated) return
      this._onMonitorUpdated = () => {
        this.refreshProbeOnly()
      }
      window.addEventListener(GLOBAL_MONITOR_EVENT, this._onMonitorUpdated)
    },
    unbindMonitorListener() {
      if (typeof window === 'undefined' || !this._onMonitorUpdated) return
      window.removeEventListener(GLOBAL_MONITOR_EVENT, this._onMonitorUpdated)
      this._onMonitorUpdated = null
    },
    refreshProbeOnly() {
      return listProbeStatus({}).then(res => {
        const map = {}
        ;((res && res.data) || []).forEach(p => {
          if (p && p.deviceId) map[p.deviceId] = p
        })
        this.probeById = map
      }).catch(() => {})
    },
    onModeChange() {
      if (this.mode === 'all') {
        this.loadGraph()
      } else if (this.focusDeviceId) {
        this.loadGraph()
      } else {
        this.graph = { nodes: [], edges: [] }
        this.positions = {}
      }
    },
    loadDevices() {
      return listDevices({}).then(res => {
        if (res && res.code === 200) {
          this.deviceOptions = res.data || []
        }
      }).catch(() => {})
    },
    loadGraph() {
      if (this.mode === 'focus' && !this.focusDeviceId) {
        this.graph = { nodes: [], edges: [] }
        this.positions = {}
        return Promise.resolve()
      }
      this.loading = true
      const query = this.mode === 'focus' ? { focusDeviceId: this.focusDeviceId } : {}
      return Promise.all([
        getTopologyGraph(query),
        listProbeStatus({})
      ]).then(([res, probeRes]) => {
        const map = {}
        ;((probeRes && probeRes.data) || []).forEach(p => {
          if (p && p.deviceId) map[p.deviceId] = p
        })
        this.probeById = map

        if (!res || res.code !== 200 || !res.data) {
          this.$message.error((res && res.msg) || '\u52a0\u8f7d\u5931\u8d25')
          this.graph = { nodes: [], edges: [] }
          this.positions = {}
          return
        }
        this.graph = res.data
        const layout = layoutLevels(res.data)
        const shifted = {}
        Object.keys(layout.positions).forEach(id => {
          const p = layout.positions[id]
          // Keep previous drag position if same node still present
          if (this.positions[id]) {
            shifted[id] = { x: this.positions[id].x, y: this.positions[id].y }
          } else {
            shifted[id] = { x: p.x + 400, y: p.y + 60 }
          }
        })
        this.positions = shifted
        // Drop selection for nodes no longer visible
        this.selectedIds = this.selectedIds.filter(id => !!shifted[id])
      }).catch(() => {
        this.$message.error('\u52a0\u8f7d\u5931\u8d25')
      }).finally(() => {
        this.loading = false
      })
    },
    openDetail(edgeId) {
      getLinkDetail(edgeId).then(res => {
        if (!res || res.code !== 200 || !res.data) {
          this.$message.error((res && res.msg) || '\u52a0\u8f7d\u5931\u8d25')
          return
        }
        this.detail = res.data
        this.detailOpen = true
      }).catch(() => {
        this.$message.error('\u52a0\u8f7d\u5931\u8d25')
      })
    }
  }
}
</script>

<style scoped>
.topology-filter {
  margin-bottom: 12px;
}
.select-hint {
  margin-left: 8px;
  font-size: 12px;
  color: #e6a23c;
}
.topology-legend {
  margin-left: 8px;
}
.legend-item {
  display: inline-flex;
  align-items: center;
  margin-right: 12px;
  font-size: 12px;
  color: #606266;
}
.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 4px;
}
.status-dot.online {
  background: #67c23a;
}
.status-dot.offline {
  background: #f56c6c;
}
.status-dot.unknown {
  background: #909399;
}
.topology-canvas-wrap {
  min-height: 420px;
  border: 1px solid #ebeef5;
  background: #fafafa;
  overflow: auto;
  scrollbar-gutter: stable;
}
.topology-svg {
  display: block;
  max-width: none;
  flex-shrink: 0;
  user-select: none;
}
.topology-svg.is-dragging {
  cursor: grabbing;
}
.topology-svg.is-selecting .topo-node {
  cursor: pointer;
}
.topo-edge {
  stroke: #9b59d0;
  stroke-width: 3;
  cursor: pointer;
}
.topo-edge:hover {
  stroke: #7a3eb0;
  stroke-width: 4;
}
.topo-node {
  cursor: grab;
}
.topo-node rect {
  fill: #ffffff;
  stroke: #9b59d0;
  stroke-width: 2;
}
.topo-node.selected rect {
  stroke: #409eff;
  stroke-width: 2.5;
  fill: #ecf5ff;
}
.topo-node rect.node-fill-online {
  stroke: #67c23a;
}
.topo-node.selected rect.node-fill-online {
  stroke: #409eff;
}
.topo-node rect.node-fill-offline {
  stroke: #f56c6c;
}
.topo-node.selected rect.node-fill-offline {
  stroke: #409eff;
}
.topo-node rect.node-fill-unknown {
  stroke: #909399;
}
.topo-node.selected rect.node-fill-unknown {
  stroke: #409eff;
}
.node-check-box {
  fill: #ffffff;
  stroke: #409eff;
  stroke-width: 1.5;
  pointer-events: none;
}
.node-check-mark {
  stroke: #409eff;
  stroke-width: 2;
  pointer-events: none;
}
.topo-node .node-status-dot.fill-online {
  fill: #67c23a;
}
.topo-node .node-status-dot.fill-offline {
  fill: #f56c6c;
}
.topo-node .node-status-dot.fill-unknown {
  fill: #909399;
}
.topo-node .node-name {
  fill: #303133;
  font-size: 12px;
  text-anchor: start;
  pointer-events: none;
}
.topo-node .node-ip {
  fill: #606266;
  font-size: 11px;
  text-anchor: start;
  pointer-events: none;
}
.topo-node .node-status-text {
  fill: #909399;
  font-size: 11px;
  text-anchor: start;
  pointer-events: none;
}
.topo-node .node-status-text.text-online {
  fill: #67c23a;
}
.topo-node .node-status-text.text-offline {
  fill: #f56c6c;
}
.topo-node .node-status-text.text-unknown {
  fill: #909399;
}
.topology-empty {
  padding: 48px;
  text-align: center;
  color: #909399;
  font-size: 13px;
}
.topology-detail {
  line-height: 1.7;
  color: #606266;
  font-size: 13px;
}
.topology-detail-label {
  font-weight: 600;
  font-size: 13px;
  color: #303133;
}
</style>

<style>
.topology-link-dialog {
  font-size: 13px;
}
.topology-link-dialog .el-dialog__title {
  font-size: 16px;
  line-height: 1.4;
}
.topology-link-dialog .el-dialog__body {
  font-size: 13px;
  color: #606266;
}
</style>
