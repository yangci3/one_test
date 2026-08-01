<template>
  <div class="app-container topology-page">
    <el-form :inline="true" size="small" class="topology-filter">
      <el-form-item :label="labels.mode">
        <el-radio-group v-model="mode" @change="onModeChange">
          <el-radio-button label="all">{{ labels.modeAll }}</el-radio-button>
          <el-radio-button label="focus">{{ labels.modeFocus }}</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="mode === 'focus'" :label="labels.device">
        <el-select
          v-model="focusDeviceId"
          filterable
          clearable
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
        <el-button type="primary" icon="el-icon-refresh" @click="loadGraph">{{ labels.refresh }}</el-button>
      </el-form-item>
    </el-form>

    <div v-loading="loading" class="topology-canvas-wrap">
      <svg
        v-if="graph.nodes.length"
        class="topology-svg"
        :viewBox="viewBox"
        xmlns="http://www.w3.org/2000/svg"
      >
        <line
          v-for="e in graph.edges"
          :key="e.id"
          class="topo-edge"
          :x1="pos(e.fromDeviceId).x"
          :y1="pos(e.fromDeviceId).y"
          :x2="pos(e.toDeviceId).x"
          :y2="pos(e.toDeviceId).y"
          @click.stop="openDetail(e.id)"
        />
        <g
          v-for="n in graph.nodes"
          :key="n.id"
          class="topo-node"
          @click.stop="focusDeviceId = n.id; mode = 'focus'; loadGraph()"
        >
          <rect
            :x="pos(n.id).x - 56"
            :y="pos(n.id).y - 18"
            width="112"
            height="36"
            rx="6"
          />
          <text :x="pos(n.id).x" :y="pos(n.id).y + 5">{{ n.name }}</text>
        </g>
      </svg>
      <div v-else class="topology-empty">{{ emptyHint }}</div>
    </div>

    <el-dialog
      :title="labels.detailTitle"
      :visible.sync="detailOpen"
      width="480px"
      append-to-body
    >
      <div v-if="detail" class="topology-detail">
        <div><strong>{{ labels.upstream }}</strong></div>
        <div>{{ detail.from.name }} ({{ detail.from.ip }})</div>
        <div>{{ typeLabel(detail.from.type) }} ?? {{ detail.from.buildingName || detail.from.buildingId || '??' }}</div>
        <el-divider />
        <div><strong>{{ labels.downstream }}</strong></div>
        <div>{{ detail.to.name }} ({{ detail.to.ip }})</div>
        <div>{{ typeLabel(detail.to.type) }} ?? {{ detail.to.buildingName || detail.to.buildingId || '??' }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listDevices } from '@/api/scene/device'
import { getTopologyGraph, getLinkDetail } from '@/api/scene/topology'
import { layoutLevels } from '@/utils/scene/topologyGraph'

const TYPE_LABELS = {
  router: '\u8def\u7531\u5668',
  switch: '\u4ea4\u6362\u673a',
  terminal: '\u7ec8\u7aef',
  other: '\u5176\u4ed6'
}

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
      detailOpen: false,
      detail: null,
      labels: {
        mode: '\u6a21\u5f0f',
        modeAll: '\u5168\u90e8\u8fde\u63a5',
        modeFocus: '\u4e0e\u9009\u4e2d\u76f8\u5173',
        device: '\u8bbe\u5907',
        pickDevice: '\u8bf7\u9009\u62e9\u8bbe\u5907',
        refresh: '\u5237\u65b0',
        detailTitle: '\u94fe\u8def\u8be6\u60c5',
        upstream: '\u4e0a\u6e38',
        downstream: '\u4e0b\u6e38',
        emptyAll: '\u6682\u65e0\u62d3\u6251\u6570\u636e',
        emptyFocus: '\u8bf7\u9009\u62e9\u8bbe\u5907\u67e5\u770b\u76f8\u5173\u62d3\u6251'
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
    viewBox() {
      const vals = Object.keys(this.positions).map(id => this.positions[id])
      if (!vals.length) return '0 0 800 400'
      let minX = Infinity
      let minY = Infinity
      let maxX = -Infinity
      let maxY = -Infinity
      vals.forEach(p => {
        minX = Math.min(minX, p.x)
        minY = Math.min(minY, p.y)
        maxX = Math.max(maxX, p.x)
        maxY = Math.max(maxY, p.y)
      })
      const pad = 80
      return (minX - pad) + ' ' + (minY - pad) + ' ' + (maxX - minX + pad * 2) + ' ' + (maxY - minY + pad * 2)
    }
  },
  created() {
    const q = this.$route && this.$route.query
    if (q && q.deviceId) {
      this.mode = 'focus'
      this.focusDeviceId = q.deviceId
    }
    this.loadDevices().then(() => this.loadGraph())
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
      return getTopologyGraph(query).then(res => {
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
          shifted[id] = { x: p.x + 400, y: p.y + 40 }
        })
        this.positions = shifted
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
.topology-canvas-wrap {
  min-height: 420px;
  border: 1px solid #ebeef5;
  background: #fafafa;
  overflow: auto;
}
.topology-svg {
  width: 100%;
  min-height: 420px;
  display: block;
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
.topo-node rect {
  fill: #ffffff;
  stroke: #9b59d0;
  stroke-width: 2;
  cursor: pointer;
}
.topo-node text {
  fill: #303133;
  font-size: 12px;
  text-anchor: middle;
  pointer-events: none;
}
.topology-empty {
  padding: 48px;
  text-align: center;
  color: #909399;
}
.topology-detail {
  line-height: 1.7;
  color: #606266;
  font-size: 13px;
}
</style>
