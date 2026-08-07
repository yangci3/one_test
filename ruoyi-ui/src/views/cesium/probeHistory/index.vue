<template>
  <div class="app-container">
    <el-form :inline="true" size="small" class="probe-history-filter">
      <el-form-item :label="labels.device">
        <el-select
          v-model="query.deviceId"
          clearable
          filterable
          :placeholder="labels.allDevices"
          style="width: 240px"
          @change="loadList"
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
        <el-button type="primary" icon="el-icon-refresh" @click="loadList">{{ labels.refresh }}</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column :label="labels.time" prop="at" min-width="170">
        <template slot-scope="scope">
          {{ formatTime(scope.row.at) }}
        </template>
      </el-table-column>
      <el-table-column :label="labels.deviceName" prop="deviceName" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column :label="labels.ip" prop="ip" min-width="120" />
      <el-table-column :label="labels.event" prop="type" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.type === 'offline' ? 'warning' : 'success'" size="mini">
            {{ scope.row.type === 'offline' ? labels.offline : labels.online }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
    <div v-if="!loading && !list.length" class="probe-history-empty">{{ labels.empty }}</div>
  </div>
</template>

<script>
import { listDevices } from '@/api/scene/device'
import { listProbeHistory } from '@/api/scene/probeHistory'
import { getGlobalMonitorUpdateEventName } from '@/utils/scene/globalMonitorRuntime'

const GLOBAL_MONITOR_EVENT = getGlobalMonitorUpdateEventName()

export default {
  name: 'CesiumProbeHistory',
  data() {
    return {
      loading: false,
      list: [],
      deviceOptions: [],
      query: {
        deviceId: undefined
      },
      labels: {
        device: '\u8bbe\u5907',
        allDevices: '\u5168\u90e8\u8bbe\u5907',
        refresh: '\u5237\u65b0',
        time: '\u65f6\u95f4',
        deviceName: '\u8bbe\u5907\u540d\u79f0',
        ip: 'IP',
        event: '\u4e8b\u4ef6',
        online: '\u4e0a\u7ebf',
        offline: '\u79bb\u7ebf',
        empty: '\u8fd1 30 \u5929\u6682\u65e0\u4e0a\u4e0b\u7ebf\u8bb0\u5f55'
      }
    }
  },
  created() {
    const q = this.$route && this.$route.query
    if (q && q.deviceId) {
      this.query.deviceId = q.deviceId
    }
    this.loadDevices().then(() => this.loadList())
  },
  mounted() {
    this._onGlobalMonitorUpdated = (e) => {
      const detail = e && e.detail
      if (detail && Array.isArray(detail.events) && detail.events.length > 0) {
        this.loadList({ silent: true })
      }
    }
    if (typeof window !== 'undefined') {
      window.addEventListener(GLOBAL_MONITOR_EVENT, this._onGlobalMonitorUpdated)
    }
  },
  beforeDestroy() {
    if (typeof window !== 'undefined' && this._onGlobalMonitorUpdated) {
      window.removeEventListener(GLOBAL_MONITOR_EVENT, this._onGlobalMonitorUpdated)
    }
    this._onGlobalMonitorUpdated = null
  },
  watch: {
    '$route.query.deviceId'(val) {
      this.query.deviceId = val || undefined
      this.loadList()
    }
  },
  methods: {
    formatTime(at) {
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
    loadDevices() {
      return listDevices({}).then(res => {
        if (res && res.code === 200) {
          this.deviceOptions = res.data || []
        }
      }).catch(() => {})
    },
    loadList(options) {
      const silent = !!(options && options.silent)
      if (!silent) {
        this.loading = true
      }
      const query = {}
      if (this.query.deviceId) {
        query.deviceId = this.query.deviceId
      }
      return listProbeHistory(query).then(res => {
        if (!res || res.code !== 200) {
          if (!silent) {
            this.$message.error((res && res.msg) || '\u52a0\u8f7d\u5931\u8d25')
          }
          if (!silent) this.list = []
          return
        }
        this.list = res.data || []
      }).catch(() => {
        if (!silent) {
          this.$message.error('\u52a0\u8f7d\u5931\u8d25')
          this.list = []
        }
      }).finally(() => {
        if (!silent) {
          this.loading = false
        }
      })
    }
  }
}
</script>

<style scoped>
.probe-history-filter {
  margin-bottom: 12px;
}
.probe-history-empty {
  margin-top: 16px;
  color: #909399;
  text-align: center;
}
</style>
