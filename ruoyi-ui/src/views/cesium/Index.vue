<template>
  <div class="cesium-page">
    <div class="cesium-stage">
      <div ref="cesiumContainer" class="cesium-container" />
      <div v-if="loadError" class="cesium-error">{{ loadError }}</div>
      <div v-if="loading" class="cesium-loading">加载场景中...</div>
      <div
        v-if="hoverSummaryVisible && hoverSummary"
        class="cesium-hover-summary"
        :style="hoverSummaryStyle"
      >
        <div class="title">{{ hoverSummary.name }}</div>
        <div v-if="canProbe" class="counts">
          监控中 {{ hoverSummary.monitoringCount }} ·
          在线 {{ hoverSummary.onlineCount }} ·
          离线 {{ hoverSummary.offlineCount }}
        </div>
        <ul v-if="hoverSummary.deviceNames.length" class="device-names">
          <li v-for="(n, idx) in hoverSummary.deviceNames" :key="idx">{{ n }}</li>
        </ul>
        <div v-if="hoverSummary.moreCount > 0" class="more">等 {{ hoverSummary.moreCount }} 台</div>
        <div v-else-if="!hoverSummary.deviceNames.length" class="more">暂无设备</div>
      </div>
    </div>
    <aside class="cesium-side">
      <div class="cesium-tools">
        <el-button size="mini" type="primary" :disabled="!ready" @click="onFlyHome">回到中心点</el-button>
        <el-button size="mini" :disabled="!ready" @click="onSaveHome">保存中心点</el-button>
        <el-button size="mini" :disabled="!ready" @click="listOpen = !listOpen">建筑列表</el-button>
        <el-button v-if="canProbeEdit" size="mini" type="success" :disabled="!ready" @click="onStartAll">开始全部监控</el-button>
        <el-button v-if="canProbeEdit" size="mini" type="warning" :disabled="!ready" @click="onStopAll">关闭全部监控</el-button>
        <el-button
          v-if="linkModeDeviceId"
          size="mini"
          type="danger"
          plain
          :disabled="!ready"
          @click="clearLinkMode"
        >清除链路</el-button>
      </div>
      <div class="cesium-toggle">
        <span>显示大雁塔对照</span>
        <el-switch
          v-model="showDayanta"
          :disabled="!ready || dayantaLoading"
          @change="onDayantaToggle"
        />
      </div>
      <div v-if="canProbe" class="cesium-toggle">
        <span>告警声音（关闭可停止告警）</span>
        <el-switch
          v-model="alertSoundOn"
          :disabled="!ready"
          @change="onAlertMuteChange"
        />
      </div>
      <div v-if="canProbe" class="cesium-toggle">
        <span>告警弹窗提示</span>
        <el-switch
          v-model="alertPopupOn"
          :disabled="!ready"
          @change="onAlertPopupChange"
        />
      </div>
      <div class="cesium-home">当前中心点：{{ homeCenterText }}</div>
      <el-divider />
      <div v-if="listOpen" class="cesium-building-list">
        <ul v-if="buildings.length">
          <li
            v-for="b in buildings"
            :key="b.id"
            :class="{ active: selected && selected.id === b.id }"
            @click="onSelectBuilding(b)"
          >
            <span class="name">{{ b.name }}</span>
            <span class="id">{{ b.id }}</span>
          </li>
        </ul>
        <div v-else class="cesium-empty">暂无建筑</div>
      </div>
      <el-divider v-if="listOpen" />
      <div v-if="selected">
        <h3>{{ selected.name }}</h3>
        <p>{{ selected.description }}</p>
        <p>本地坐标：{{ selectedLocalText }}</p>
        <el-divider />
        <div class="cesium-device-panel">
          <div class="cesium-device-header">
            <h4>本建筑设备</h4>
            <el-button type="primary" size="mini" icon="el-icon-plus" @click="handleAddDevice" v-hasPermi="['scene:device:add']">新增设备</el-button>
          </div>
          <ul v-if="buildingDevices.length" class="cesium-device-list">
            <li v-for="d in buildingDevices" :key="d.id">
              <div class="cesium-device-meta">
                <div v-if="canProbe || canProbeEdit" class="cesium-device-status-row">
                  <span v-if="canProbe" class="status-dot" :class="probeStatusClass(d.id)" />
                  <span v-if="canProbe" class="status-text">{{ probeStatusLabel(d.id) }}</span>
                  <el-switch
                    v-if="canProbeEdit"
                    :value="isDeviceMonitoring(d.id)"
                    :disabled="!ready"
                    @change="val => onToggleDeviceMonitor(d, val)"
                  />
                </div>
                <span class="name">{{ d.name }}</span>
                <span class="ip">{{ d.ip }}</span>
                <span v-if="d.mac" class="mac">{{ d.mac }}</span>
                <span class="type">{{ typeLabel(d.type) }}</span>
              </div>
              <div class="cesium-device-actions">
                <el-button v-if="canProbe" type="text" size="mini" @click="openProbeDetail(d)">监控详情</el-button>
                <el-button type="text" size="mini" @click="toggleLinkMode(d)">
                  {{ linkModeDeviceId === d.id ? '清除链路' : '显示网络链路' }}
                </el-button>
                <el-button type="text" size="mini" icon="el-icon-edit" @click="handleUpdateDevice(d)" v-hasPermi="['scene:device:edit']">编辑</el-button>
                <el-button type="text" size="mini" icon="el-icon-delete" @click="handleDeleteDevice(d)" v-hasPermi="['scene:device:remove']">删除</el-button>
              </div>
            </li>
          </ul>
          <div v-else class="cesium-empty">该建筑暂无设备</div>
        </div>
      </div>
      <div v-else class="cesium-empty">点击建筑查看信息</div>
    </aside>

    <el-dialog
      v-dialogDrag
      v-dialogDragWidth
      v-dialogDragHeight
      :title="probeDetailTitle"
      :visible.sync="probeDetailOpen"
      width="480px"
      append-to-body
    >
      <div v-if="probeDetailDevice" class="probe-detail-summary">
        <div>IP：{{ probeDetailDevice.ip || '—' }}</div>
        <div>类型：{{ typeLabel(probeDetailDevice.type) }}</div>
        <div v-if="canProbe">状态：{{ probeStatusLabel(probeDetailDevice.id) }}</div>
      </div>
      <el-timeline v-if="probeDetailEvents.length" class="probe-detail-timeline">
        <el-timeline-item
          v-for="ev in probeDetailEvents"
          :key="ev.id"
          :type="ev.type === 'offline' ? 'warning' : 'success'"
          :timestamp="formatProbeEventTime(ev.at)"
          placement="top"
        >
          {{ ev.type === 'offline' ? '离线' : '上线' }}
        </el-timeline-item>
      </el-timeline>
      <div v-else class="cesium-empty">近 30 天暂无上下线记录</div>
    </el-dialog>

    <el-dialog
      v-dialogDrag
      v-dialogDragWidth
      v-dialogDragHeight
      title="链路详情"
      :visible.sync="linkDetailOpen"
      width="480px"
      append-to-body
      custom-class="cesium-link-detail-dialog"
    >
      <div v-if="linkDetail" class="probe-detail-summary">
        <div class="probe-detail-label">上游</div>
        <div>{{ linkDetail.from.name }}（{{ linkDetail.from.ip }}）</div>
        <div>{{ typeLabel(linkDetail.from.type) }} · {{ linkDetail.from.buildingName || linkDetail.from.buildingId || '—' }}</div>
        <el-divider />
        <div class="probe-detail-label">下游</div>
        <div>{{ linkDetail.to.name }}（{{ linkDetail.to.ip }}）</div>
        <div>{{ typeLabel(linkDetail.to.type) }} · {{ linkDetail.to.buildingName || linkDetail.to.buildingId || '—' }}</div>
      </div>
    </el-dialog>

    <el-dialog
      v-dialogDrag
      v-dialogDragWidth
      v-dialogDragHeight
      :title="deviceTitle"
      :visible.sync="deviceOpen"
      width="520px"
      append-to-body
    >
      <el-form ref="deviceForm" :model="deviceForm" :rules="deviceRules" label-width="100px">
        <el-form-item label="设备名称" prop="name">
          <el-input v-model="deviceForm.name" placeholder="请输入设备名称" />
        </el-form-item>
        <el-form-item label="IP地址" prop="ip">
          <el-input v-model="deviceForm.ip" placeholder="请输入IP地址" />
        </el-form-item>
        <el-form-item label="MAC\u5730\u5740" prop="mac">
          <el-input v-model="deviceForm.mac" placeholder="请输入MAC\u5730\u5740" />
        </el-form-item>
        <el-form-item label="设备类型" prop="type">
          <el-select v-model="deviceForm.type" placeholder="请选择设备类型" style="width: 100%">
            <el-option
              v-for="item in typeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="所属建筑" prop="buildingId">
          <el-input :value="selected ? selected.name : ''" disabled />
        </el-form-item>
        <el-form-item label="上级设备" prop="parentDeviceId">
          <el-select v-model="deviceForm.parentDeviceId" placeholder="可选，跨建筑上级" clearable style="width: 100%">
            <el-option
              v-for="item in parentOptions"
              :key="item.id"
              :label="item.name + ' (' + item.ip + ')'"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="deviceForm.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitDeviceForm">确 定</el-button>
        <el-button @click="cancelDeviceForm">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { createLocalViewer } from '@/utils/cesium/createLocalViewer'
import { createSceneFrame } from '@/utils/cesium/localFrame'
import { createPlaceholderScene } from '@/utils/cesium/placeholderScene'
import { loadTileset, alignTilesetToLocalFrame } from '@/utils/cesium/loadTileset'
import {
  attachBuildingPick,
  selectBuilding,
  flyToBuilding,
  clearBuildingHighlight,
  resolveBuildingPick
} from '@/utils/cesium/buildingPick'
import { getHomeCenter, saveHomeCenter, pickCanvasCenterLocal, flyToHomeCenter } from '@/utils/cesium/homeCenter'
import sceneConfig from '@/utils/cesium/sceneConfig'
import { getBuildings } from '@/api/scene/buildings'
import { listDevices, getDevice, addDevice, updateDevice, delDevice } from '@/api/scene/device'
import { listProbeStatus, getAlertMuted } from '@/api/scene/probe'
import { getDeviceProbeHistory } from '@/api/scene/probeHistory'
import { getMonitorSettings } from '@/api/scene/monitorSettings'
import { getTopologyGraph, getLinkDetail } from '@/api/scene/topology'
import { aggregateAllBuildings } from '@/utils/cesium/buildingStatus'
import { createLinkOverlay, LINK_HIGHLIGHT_COLOR } from '@/utils/cesium/linkOverlay'
import {
  bootstrapGlobalMonitor,
  startAllGlobalMonitoring,
  stopAllGlobalMonitoring,
  startDeviceGlobalMonitoring,
  stopDeviceGlobalMonitoring,
  setGlobalAlertMuted,
  setGlobalAlertPopupEnabled,
  restartGlobalMonitorEngine,
  getGlobalMonitorUpdateEventName
} from '@/utils/scene/globalMonitorRuntime'
import { hasSceneProbeQuery, hasSceneProbeEdit } from '@/utils/scene/sceneAuth'

const MONITOR_SETTINGS_EVENT = 'ruoyi-monitor-settings-changed'
const GLOBAL_MONITOR_EVENT = getGlobalMonitorUpdateEventName()

const TYPE_LABELS = {
  router: '路由器',
  switch: '交换机',
  terminal: '终端',
  other: '其他'
}

function formatXYZ(xyz) {
  if (!xyz || xyz.length < 3) return '—'
  return xyz.map(function (n) { return Number(n).toFixed(1) }).join(', ')
}

function formatLocal(local) {
  if (!local) return '—'
  return [local.x, local.y, local.z].map(function (n) { return Number(n).toFixed(1) }).join(', ')
}

export default {
  name: 'CesiumIndex',
  data() {
    return {
      loading: true,
      ready: false,
      loadError: '',
      viewer: null,
      frame: null,
      compareTileset: null,
      placeholderApi: null,
      pickApi: null,
      buildings: [],
      selected: null,
      selectedLocal: null,
      homeCenterText: '',
      showDayanta: !!(sceneConfig && sceneConfig.dayantaCompareDefault),
      dayantaLoading: false,
      listOpen: false,
      buildingDevices: [],
      allDevices: [],
      typeOptions: [
        { value: 'router', label: '路由器' },
        { value: 'switch', label: '交换机' },
        { value: 'terminal', label: '终端' },
        { value: 'other', label: '其他' }
      ],
      deviceOpen: false,
      deviceTitle: '',
      deviceForm: {},
      deviceRules: {
        name: [
          { required: true, message: '设备名称不能为空', trigger: 'blur' }
        ],
        ip: [
          { required: true, message: 'IP地址不能为空', trigger: 'blur' }
        ],
        type: [
          { required: true, message: '设备类型不能为空', trigger: 'change' }
        ],
        buildingId: [
          { required: true, message: '所属建筑不能为空', trigger: 'change' }
        ]
      },
      probeById: {},
      alertSoundOn: true,
      alertPopupOn: true,
      monitorSettings: null,
      probeDetailOpen: false,
      probeDetailDevice: null,
      probeDetailEvents: [],
      probeDetailTitle: '监控详情',
      linkModeDeviceId: null,
      linkGraph: null,
      linkOverlay: null,
      linkDetailOpen: false,
      linkDetail: null,
      hoverHandler: null,
      hoverTimer: null,
      hoverPendingId: null,
      hoverSummaryVisible: false,
      hoverSummary: null,
      hoverSummaryPos: { x: 0, y: 0 }
    }
  },
  computed: {
    canProbe() {
      return hasSceneProbeQuery()
    },
    canProbeEdit() {
      return hasSceneProbeEdit()
    },
    selectedLocalText() {
      return formatLocal(this.selectedLocal)
    },
    parentOptions() {
      const currentId = this.deviceForm && this.deviceForm.id
      return this.allDevices.filter(d => d.id !== currentId)
    },
    hoverSummaryStyle() {
      const pos = this.hoverSummaryPos || { x: 0, y: 0 }
      return {
        left: (pos.x + 16) + 'px',
        top: (pos.y + 16) + 'px'
      }
    }
  },
  watch: {
    selected(val) {
      if (val && val.id) {
        this.loadBuildingDevices()
      } else {
        this.buildingDevices = []
      }
    }
  },
  async mounted() {
    try {
      if (!window.Cesium) {
        this.loadError = 'Cesium 未就绪'
        this.loading = false
        return
      }

      const viewer = createLocalViewer(this.$refs.cesiumContainer)
      this.viewer = viewer
      viewer.scene.pickTranslucentDepth = true

      const frame = createSceneFrame(window.Cesium, sceneConfig.frameOriginDegrees)
      this.frame = frame

      const res = await getBuildings()
      this.buildings = (res && res.data) || []

      this.placeholderApi = createPlaceholderScene(viewer, frame, this.buildings, sceneConfig)

      this.pickApi = attachBuildingPick(viewer, {
        frame: frame,
        getBuildingsList: () => this.buildings,
        onSelect: (hit, local) => {
          this.selected = hit
          this.selectedLocal = local
        },
        onClear: () => {
          this.selected = null
          this.selectedLocal = null
        },
        pickMaxDistance: sceneConfig.pickMaxDistance
      })
      this.attachHoverSummary(viewer, frame)

      flyToHomeCenter(viewer, frame, sceneConfig)

      this.homeCenterText = formatXYZ(getHomeCenter(undefined, sceneConfig))
      this.ready = true
      this.loading = false
      this.linkOverlay = createLinkOverlay(viewer, frame, {
        getBuildings: () => this.buildings,
        onSelectEdge: edgeId => this.openLinkDetail(edgeId)
      })
      if (this.canProbe) {
        bootstrapGlobalMonitor()
        this.bindMonitorSettingsListener()
        this.bindGlobalMonitorListener()
        this.applyMonitorSettings({ restartEngine: false })
        this.refreshProbeUi()
      } else {
        this.loadAllDevices()
      }
    } catch (err) {
      this.loadError = (err && err.message) ? err.message : String(err)
      this.loading = false
    }
  },
  activated() {
    if (!this.ready) return
    if (this.canProbe) {
      this.applyMonitorSettings({ restartEngine: true })
      this.refreshProbeUi()
    } else {
      this.loadAllDevices()
    }
  },
  beforeDestroy() {
    // Do NOT stop global probe engine or alert audio here — monitoring is app-wide.
    this.unbindMonitorSettingsListener()
    this.unbindGlobalMonitorListener()
    if (this.linkOverlay && typeof this.linkOverlay.destroy === 'function') {
      this.linkOverlay.destroy()
      this.linkOverlay = null
    }
    this.clearHoverSummary(true)
    if (this.hoverHandler && typeof this.hoverHandler.destroy === 'function') {
      this.hoverHandler.destroy()
      this.hoverHandler = null
    }
    if (this.placeholderApi && typeof this.placeholderApi.destroy === 'function') {
      this.placeholderApi.destroy()
      this.placeholderApi = null
    }
    if (this.pickApi && typeof this.pickApi.destroy === 'function') {
      this.pickApi.destroy()
      this.pickApi = null
    }
    this.removeCompareTileset()
    if (this.viewer) {
      clearBuildingHighlight(this.viewer)
      this.viewer.destroy()
      this.viewer = null
    }
  },
  methods: {
    attachHoverSummary(viewer, frame) {
      const Cesium = window.Cesium
      if (!Cesium || !viewer || !frame) return
      if (this.hoverHandler && typeof this.hoverHandler.destroy === 'function') {
        this.hoverHandler.destroy()
      }
      const handler = new Cesium.ScreenSpaceEventHandler(viewer.scene.canvas)
      this.hoverHandler = handler
      handler.setInputAction((movement) => {
        const end = movement && movement.endPosition
        if (!end) {
          this.clearHoverSummary()
          return
        }
        this.hoverSummaryPos = { x: end.x, y: end.y }
        const hit = resolveBuildingPick(
          viewer,
          frame,
          end,
          this.buildings,
          sceneConfig.pickMaxDistance
        )
        if (!hit || !hit.id) {
          this.clearHoverSummary()
          return
        }
        if (this.hoverSummaryVisible && this.hoverSummary && this.hoverSummary.buildingId === hit.id) {
          return
        }
        if (this.hoverSummaryVisible) {
          this.hoverSummaryVisible = false
          this.hoverSummary = null
        }
        this.hoverPendingId = hit.id
        if (this.hoverTimer) {
          clearTimeout(this.hoverTimer)
          this.hoverTimer = null
        }
        const delay = this.getHoverSummaryDelayMs()
        const buildingId = hit.id
        this.hoverTimer = setTimeout(() => {
          this.hoverTimer = null
          if (this.hoverPendingId !== buildingId) return
          this.showHoverSummary(hit)
        }, delay)
      }, Cesium.ScreenSpaceEventType.MOUSE_MOVE)
    },
    getHoverSummaryDelayMs() {
      const fromSettings = this.monitorSettings && this.monitorSettings.hoverSummaryDelayMs
      if (typeof fromSettings === 'number' && fromSettings >= 100) {
        return fromSettings
      }
      return (sceneConfig && sceneConfig.hoverSummaryDelayMs) || 500
    },
    buildHoverSummary(building) {
      if (!building) return null
      const devices = (this.allDevices || []).filter(d => d && d.buildingId === building.id)
      let monitoringCount = 0
      let onlineCount = 0
      let offlineCount = 0
      devices.forEach(d => {
        const probe = this.getProbe(d.id)
        if (!probe.monitoring) return
        monitoringCount += 1
        if (probe.status === 'online') onlineCount += 1
        else if (probe.status === 'offline') offlineCount += 1
      })
      const names = devices.map(d => d.name || d.id)
      const deviceNames = names.slice(0, 5)
      const moreCount = names.length > 5 ? (names.length - 5) : 0
      return {
        buildingId: building.id,
        name: building.name || building.id,
        monitoringCount,
        onlineCount,
        offlineCount,
        deviceNames,
        moreCount
      }
    },
    showHoverSummary(building) {
      this.hoverSummary = this.buildHoverSummary(building)
      this.hoverSummaryVisible = !!this.hoverSummary
    },
    clearHoverSummary(destroying) {
      if (this.hoverTimer) {
        clearTimeout(this.hoverTimer)
        this.hoverTimer = null
      }
      this.hoverPendingId = null
      this.hoverSummaryVisible = false
      if (!destroying) {
        this.hoverSummary = null
      }
    },
    bindMonitorSettingsListener() {
      if (!this.canProbe) return
      if (typeof window === 'undefined') return
      if (this._onMonitorSettingsChanged) return
      this._onMonitorSettingsChanged = () => {
        this.applyMonitorSettings({ restartEngine: true })
      }
      window.addEventListener(MONITOR_SETTINGS_EVENT, this._onMonitorSettingsChanged)
    },
    unbindMonitorSettingsListener() {
      if (typeof window === 'undefined' || !this._onMonitorSettingsChanged) return
      window.removeEventListener(MONITOR_SETTINGS_EVENT, this._onMonitorSettingsChanged)
      this._onMonitorSettingsChanged = null
    },
    bindGlobalMonitorListener() {
      if (!this.canProbe) return
      if (typeof window === 'undefined') return
      this.unbindGlobalMonitorListener()
      this._onGlobalMonitorUpdated = (e) => {
        if (this.ready) this.refreshProbeUi()
        if (this.probeDetailOpen && this.probeDetailDevice && this.probeDetailDevice.id) {
          const detail = e && e.detail
          if (detail && Array.isArray(detail.events) && detail.events.length > 0) {
            this.reloadProbeDetailEvents({ silent: true })
          }
        }
      }
      window.addEventListener(GLOBAL_MONITOR_EVENT, this._onGlobalMonitorUpdated)
    },
    unbindGlobalMonitorListener() {
      if (typeof window === 'undefined' || !this._onGlobalMonitorUpdated) return
      window.removeEventListener(GLOBAL_MONITOR_EVENT, this._onGlobalMonitorUpdated)
      this._onGlobalMonitorUpdated = null
    },
    applyMonitorSettings(options) {
      if (!this.canProbe) return Promise.resolve()
      const restartEngine = !!(options && options.restartEngine)
      return getMonitorSettings().then(res => {
        if (!res || res.code !== 200 || !res.data) {
          return getAlertMuted().then(muteRes => {
            if (muteRes && muteRes.code === 200) {
              this.alertSoundOn = !muteRes.data
            }
          }).catch(() => {})
        }
        this.monitorSettings = res.data
        this.alertSoundOn = !res.data.alertMuted
        this.alertPopupOn = res.data.alertPopupEnabled !== false
        if (restartEngine) {
          restartGlobalMonitorEngine()
        }
        return this.refreshProbeUi()
      }).catch(() => {})
    },
    refreshProbeUi() {
      if (!this.canProbe) {
        return this.loadAllDevices()
      }
      return Promise.all([
        listProbeStatus({}),
        listDevices({})
      ]).then(([probeRes, deviceRes]) => {
        const map = {}
        const list = (probeRes && probeRes.data) || []
        list.forEach(p => {
          if (p && p.deviceId) {
            map[p.deviceId] = p
          }
        })
        this.probeById = map
        if (deviceRes && deviceRes.code === 200) {
          this.allDevices = deviceRes.data || []
        }
        if (this.placeholderApi && typeof this.placeholderApi.setBuildingColor === 'function') {
          const colorsMap =
            (this.monitorSettings && this.monitorSettings.buildingStatusColors) || undefined
          const levels = aggregateAllBuildings(this.buildings, this.allDevices, this.probeById)
          Object.keys(levels).forEach(id => {
            this.placeholderApi.setBuildingColor(id, levels[id], colorsMap)
          })
          if (this.linkModeDeviceId && this.linkGraph) {
            this.applyLinkBuildingHighlight()
          }
        }
        return map
      }).catch(() => {
        return this.probeById
      })
    },
    resetBuildingColorsToDefault() {
      if (!this.placeholderApi || typeof this.placeholderApi.setBuildingColor !== 'function') return
      ;(this.buildings || []).forEach(b => {
        if (b && b.id) {
          this.placeholderApi.setBuildingColor(b.id, 'default')
        }
      })
    },
    applyLinkBuildingHighlight() {
      if (!this.placeholderApi || !this.linkGraph) return
      const color = LINK_HIGHLIGHT_COLOR
      const seen = {}
      ;(this.linkGraph.nodes || []).forEach(n => {
        if (!n.buildingId || seen[n.buildingId]) return
        seen[n.buildingId] = true
        this.placeholderApi.setBuildingColor(n.buildingId, color)
      })
    },
    clearLinkMode() {
      this.linkModeDeviceId = null
      this.linkGraph = null
      if (this.linkOverlay) this.linkOverlay.clear()
      if (this.canProbe) {
        this.refreshProbeUi()
      } else {
        this.resetBuildingColorsToDefault()
      }
    },
    toggleLinkMode(device) {
      if (!device || !device.id) return
      if (this.linkModeDeviceId === device.id) {
        this.clearLinkMode()
        return
      }
      getTopologyGraph({ focusDeviceId: device.id }).then(res => {
        if (!res || res.code !== 200 || !res.data) {
          this.$modal.msgError((res && res.msg) || '加载链路失败')
          return
        }
        this.linkModeDeviceId = device.id
        this.linkGraph = res.data
        if (this.linkOverlay) this.linkOverlay.setGraph(res.data)
        if (this.canProbe) {
          this.refreshProbeUi()
        } else {
          this.resetBuildingColorsToDefault()
          this.applyLinkBuildingHighlight()
        }
      }).catch(() => {
        this.$modal.msgError('加载链路失败')
      })
    },
    openLinkDetail(edgeId) {
      getLinkDetail(edgeId).then(res => {
        if (!res || res.code !== 200 || !res.data) {
          this.$modal.msgError((res && res.msg) || '加载详情失败')
          return
        }
        this.linkDetail = res.data
        this.linkDetailOpen = true
      }).catch(() => {
        this.$modal.msgError('加载详情失败')
      })
    },
    getProbe(deviceId) {
      return (this.probeById && this.probeById[deviceId]) || {
        deviceId: deviceId,
        monitoring: false,
        status: 'unknown',
        lastChangeAt: null
      }
    },
    isDeviceMonitoring(deviceId) {
      return !!this.getProbe(deviceId).monitoring
    },
    probeStatusClass(deviceId) {
      const probe = this.getProbe(deviceId)
      if (!probe.monitoring) return 'unknown'
      if (probe.status === 'online') return 'online'
      if (probe.status === 'offline') return 'offline'
      return 'unknown'
    },
    probeStatusLabel(deviceId) {
      const probe = this.getProbe(deviceId)
      if (!probe.monitoring || probe.status === 'unknown') return '未监控'
      if (probe.status === 'online') return '在线'
      if (probe.status === 'offline') return '离线'
      return '未监控'
    },
    deviceNameById(deviceId) {
      const fromAll = this.allDevices.find(d => d.id === deviceId)
      if (fromAll && fromAll.name) return fromAll.name
      const fromBuilding = this.buildingDevices.find(d => d.id === deviceId)
      if (fromBuilding && fromBuilding.name) return fromBuilding.name
      return deviceId
    },
    onStartAll() {
      startAllGlobalMonitoring().then(res => {
        if (!res || res.code !== 200) {
          this.$modal.msgError((res && res.msg) || '操作失败')
          return
        }
        this.refreshProbeUi()
      })
    },
    onStopAll() {
      stopAllGlobalMonitoring().then(res => {
        if (!res || res.code !== 200) {
          this.$modal.msgError((res && res.msg) || '操作失败')
          return
        }
        this.refreshProbeUi()
      })
    },
    onToggleDeviceMonitor(device, monitoring) {
      if (!device || !device.id) return
      const request = monitoring
        ? startDeviceGlobalMonitoring(device.id)
        : stopDeviceGlobalMonitoring(device.id)
      request.then(res => {
        if (!res || res.code !== 200) {
          this.$modal.msgError((res && res.msg) || '操作失败')
          this.refreshProbeUi()
          return
        }
        this.refreshProbeUi()
      })
    },
    onAlertMuteChange(soundOn) {
      setGlobalAlertMuted(!soundOn).then(res => {
        if (res && res.code === 200) {
          this.alertSoundOn = !res.data
        }
      }).catch(() => {})
    },
    onAlertPopupChange(popupOn) {
      setGlobalAlertPopupEnabled(!!popupOn).then(res => {
        if (res && res.code === 200) {
          this.alertPopupOn = !!res.data
        } else {
          this.alertPopupOn = !popupOn
          this.$modal.msgError((res && res.msg) || '操作失败')
        }
      }).catch(() => {
        this.alertPopupOn = !popupOn
      })
    },
    formatProbeEventTime(at) {
      if (at == null) return '—'
      try {
        const d = new Date(at)
        const pad = n => (n < 10 ? '0' + n : '' + n)
        return (
          d.getFullYear() +
          '-' +
          pad(d.getMonth() + 1) +
          '-' +
          pad(d.getDate()) +
          ' ' +
          pad(d.getHours()) +
          ':' +
          pad(d.getMinutes()) +
          ':' +
          pad(d.getSeconds())
        )
      } catch (e) {
        return String(at)
      }
    },
    openProbeDetail(device) {
      if (!device || !device.id) return
      this.probeDetailDevice = device
      this.probeDetailTitle = '监控详情 - ' + (device.name || device.id)
      this.probeDetailEvents = []
      this.probeDetailOpen = true
      this.reloadProbeDetailEvents()
    },
    reloadProbeDetailEvents(options) {
      const silent = !!(options && options.silent)
      const device = this.probeDetailDevice
      if (!device || !device.id) return
      getDeviceProbeHistory(device.id).then(res => {
        if (!this.probeDetailOpen || !this.probeDetailDevice || this.probeDetailDevice.id !== device.id) {
          return
        }
        if (!res || res.code !== 200 || !res.data) {
          if (!silent) {
            this.$modal.msgError((res && res.msg) || '加载失败')
          }
          return
        }
        this.probeDetailEvents = res.data.events || []
      }).catch(() => {
        if (!silent) {
          this.$modal.msgError('加载失败')
        }
      })
    },
    onSelectHandlers() {
      return {
        onSelect: (hit, local) => {
          this.selected = hit
          this.selectedLocal = local
        }
      }
    },
    onSelectBuilding(building) {
      if (!this.viewer || !this.frame || !building) return
      selectBuilding(this.viewer, this.frame, building, this.onSelectHandlers())
      flyToBuilding(this.viewer, this.frame, building, sceneConfig)
    },
    async onDayantaToggle(val) {
      if (!val) {
        this.removeCompareTileset()
        return
      }
      if (!this.viewer || this.compareTileset) return
      this.dayantaLoading = true
      try {
        // Keep this.frame (scene ENU). Compare tileset is visual-only.
        const result = await loadTileset(this.viewer, { skipFly: true })
        const tileset = result && result.tileset
        this.compareTileset = tileset
        // Place Dayanta beside campus so it is visible in the same view (not raw ECEF).
        if (tileset && this.frame && window.Cesium) {
          const offset = (sceneConfig && sceneConfig.dayantaCompareOffset) || [180, 0, 0]
          alignTilesetToLocalFrame(window.Cesium, tileset, this.frame, offset)
        }
        if (tileset && window.Cesium && window.Cesium.Cesium3DTileStyle) {
          try {
            tileset.style = new window.Cesium.Cesium3DTileStyle({
              color: "color('white', 0.55)"
            })
          } catch (styleErr) {
            // optional translucency; ignore style failures
          }
        }
        this.$message.success('大雁塔已放到厂区东侧对照（半透明）')
        // Pull camera back so both campus and Dayanta fit
        if (this.viewer && this.frame) {
          flyToHomeCenter(this.viewer, this.frame, sceneConfig)
        }
      } catch (err) {
        this.removeCompareTileset()
        this.showDayanta = false
        const msg = (err && err.message) ? err.message : String(err)
        this.$message.error('大雁塔对照加载失败：' + msg)
      } finally {
        this.dayantaLoading = false
      }
    },
    removeCompareTileset() {
      const tileset = this.compareTileset
      this.compareTileset = null
      if (!tileset) return
      try {
        if (this.viewer && this.viewer.scene && this.viewer.scene.primitives) {
          this.viewer.scene.primitives.remove(tileset)
        }
        if (typeof tileset.destroy === 'function') {
          tileset.destroy()
        }
      } catch (e) {
        // ignore cleanup errors
      }
    },
    onFlyHome() {
      if (!this.viewer || !this.frame) return
      flyToHomeCenter(this.viewer, this.frame, sceneConfig)
    },
    onSaveHome() {
      if (!this.viewer || !this.frame) return
      const local = pickCanvasCenterLocal(this.viewer, this.frame)
      if (!local) {
        this.$message.warning('未拾取到地表/模型，未保存')
        return
      }
      const saved = saveHomeCenter([local.x, local.y, local.z], undefined, sceneConfig)
      this.homeCenterText = formatXYZ(saved)
      this.$message.success('中心点已保存')
    },
    typeLabel(type) {
      return TYPE_LABELS[type] || type || '-'
    },
    loadBuildingDevices() {
      if (!this.selected || !this.selected.id) {
        this.buildingDevices = []
        return Promise.resolve([])
      }
      return listDevices({ buildingId: this.selected.id }).then(response => {
        if (response.code !== 200) {
          this.buildingDevices = []
          this.$modal.msgError(response.msg || '查询失败')
          return []
        }
        this.buildingDevices = response.data || []
        return this.buildingDevices
      })
    },
    loadAllDevices() {
      return listDevices({}).then(response => {
        if (response.code === 200) {
          this.allDevices = response.data || []
        } else {
          this.allDevices = []
        }
        return this.allDevices
      })
    },
    resetDeviceForm() {
      this.deviceForm = {
        id: undefined,
        name: undefined,
        ip: undefined,
        mac: '',
        type: undefined,
        buildingId: undefined,
        parentDeviceId: undefined,
        remark: undefined
      }
      this.resetForm('deviceForm')
    },
    cancelDeviceForm() {
      this.deviceOpen = false
      this.resetDeviceForm()
    },
    handleAddDevice() {
      if (!this.selected || !this.selected.id) return
      this.resetDeviceForm()
      this.loadAllDevices().then(() => {
        this.deviceForm.buildingId = this.selected.id
        this.deviceOpen = true
        this.deviceTitle = '新增设备'
      })
    },
    handleUpdateDevice(row) {
      if (!this.selected || !this.selected.id) return
      this.resetDeviceForm()
      this.loadAllDevices().then(() => {
        return getDevice(row.id)
      }).then(response => {
        if (response.code !== 200) {
          this.$modal.msgError(response.msg || '查询失败')
          return
        }
        this.deviceForm = {
          id: response.data.id,
          name: response.data.name,
          ip: response.data.ip,
          mac: response.data.mac || '',
          type: response.data.type,
          buildingId: this.selected.id,
          parentDeviceId: response.data.parentDeviceId || undefined,
          remark: response.data.remark
        }
        this.deviceOpen = true
        this.deviceTitle = '编辑设备'
      })
    },
    submitDeviceForm() {
      this.$refs['deviceForm'].validate(valid => {
        if (!valid) {
          return
        }
        if (!this.selected || !this.selected.id) {
          this.$modal.msgError('请先选择建筑')
          return
        }
        const payload = {
          id: this.deviceForm.id,
          name: this.deviceForm.name,
          ip: this.deviceForm.ip,
          mac: this.deviceForm.mac || '',
          type: this.deviceForm.type,
          buildingId: this.selected.id,
          parentDeviceId: this.deviceForm.parentDeviceId || null,
          remark: this.deviceForm.remark || ''
        }
        const request = payload.id ? updateDevice(payload) : addDevice(payload)
        request.then(response => {
          if (response.code !== 200) {
            this.$modal.msgError(response.msg || '操作失败')
            return
          }
          this.$modal.msgSuccess(payload.id ? '修改成功' : '新增成功')
          this.deviceOpen = false
          this.loadBuildingDevices()
          this.refreshProbeUi()
        })
      })
    },
    handleDeleteDevice(row) {
      const name = row.name
      const id = row.id
      this.$confirm('是否确认删除设备"' + name + '"？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        return delDevice(id)
      }).then(response => {
        if (response.code !== 200) {
          this.$modal.msgError(response.msg || '删除失败')
          return
        }
        this.loadBuildingDevices()
        this.refreshProbeUi()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.cesium-page {
  display: flex;
  height: calc(100vh - 84px);
  width: 100%;
  overflow: hidden;
  background: #1e1e22;
}

.cesium-stage {
  flex: 1;
  position: relative;
  min-width: 0;
}

.cesium-container {
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
}

.cesium-hover-summary {
  position: absolute;
  z-index: 3;
  min-width: 160px;
  max-width: 240px;
  padding: 10px 12px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.72);
  color: #fff;
  font-size: 12px;
  line-height: 1.45;
  pointer-events: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.35);
}

.cesium-hover-summary .title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 4px;
}

.cesium-hover-summary .counts {
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: 6px;
}

.cesium-hover-summary .device-names {
  list-style: none;
  margin: 0;
  padding: 0;
}

.cesium-hover-summary .device-names li {
  padding: 1px 0;
  color: rgba(255, 255, 255, 0.92);
}

.cesium-hover-summary .more {
  margin-top: 4px;
  color: rgba(255, 255, 255, 0.65);
}

.cesium-loading,
.cesium-error {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  z-index: 2;
  padding: 12px 16px;
  border-radius: 4px;
  color: #fff;
  font-size: 14px;
  pointer-events: none;
}

.cesium-loading {
  background: rgba(0, 0, 0, 0.55);
}

.cesium-error {
  background: rgba(180, 40, 40, 0.85);
  max-width: 80%;
  text-align: center;
  pointer-events: auto;
}

.cesium-side {
  width: 320px;
  flex-shrink: 0;
  padding: 16px;
  box-sizing: border-box;
  background: #fff;
  border-left: 1px solid #ebeef5;
  overflow: auto;
}

.cesium-tools {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.cesium-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  font-size: 13px;
  color: #606266;
}

.cesium-home {
  margin-top: 12px;
  font-size: 13px;
  color: #606266;
}

.cesium-building-list ul {
  list-style: none;
  margin: 0;
  padding: 0;
}

.cesium-building-list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 10px;
  margin-bottom: 4px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  color: #303133;
  background: #f5f7fa;
}

.cesium-building-list li:hover {
  background: #ecf5ff;
}

.cesium-building-list li.active {
  background: #409eff;
  color: #fff;
}

.cesium-building-list li .id {
  font-size: 12px;
  opacity: 0.75;
  margin-left: 8px;
}

.cesium-side h3 {
  margin: 0 0 8px;
  font-size: 16px;
  color: #303133;
}

.cesium-side p {
  margin: 0 0 8px;
  font-size: 13px;
  line-height: 1.5;
  color: #606266;
}

.cesium-empty {
  font-size: 13px;
  color: #909399;
}

.cesium-device-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.cesium-device-header h4 {
  margin: 0;
  font-size: 14px;
  color: #303133;
}

.cesium-device-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.cesium-device-list li {
  padding: 8px 10px;
  margin-bottom: 6px;
  border-radius: 4px;
  background: #f5f7fa;
}

.cesium-device-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 13px;
  color: #303133;
}

.cesium-device-status-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.cesium-device-status-row .status-text {
  flex: 1;
  font-size: 12px;
  color: #606266;
}

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #c0c4cc;
  flex-shrink: 0;
}

.status-dot.online {
  background: #67c23a;
}

.status-dot.offline {
  background: #f56c6c;
}

.status-dot.unknown {
  background: #c0c4cc;
}

.cesium-device-meta .ip,
.cesium-device-meta .mac,
.cesium-device-meta .type {
  font-size: 12px;
  color: #909399;
}

.probe-detail-summary {
  margin-bottom: 12px;
  line-height: 1.7;
  color: #606266;
  font-size: 13px;
}
.probe-detail-label {
  font-weight: 600;
  font-size: 13px;
  color: #303133;
}
.probe-detail-timeline {
  max-height: 360px;
  overflow: auto;
  padding-left: 4px;
}
.cesium-device-actions {
  margin-top: 4px;
}
</style>

<style>
.cesium-link-detail-dialog {
  font-size: 13px;
}
.cesium-link-detail-dialog .el-dialog__title {
  font-size: 16px;
  line-height: 1.4;
}
.cesium-link-detail-dialog .el-dialog__body {
  font-size: 13px;
  color: #606266;
}
</style>
