<template>
  <div class="app-container" v-loading="loading">
    <el-form ref="form" :model="form" :rules="rules" label-width="140px" size="small" style="max-width: 720px">
      <el-divider content-position="left">{{ labels.alertSection }}</el-divider>
      <el-form-item :label="labels.alertSound">
        <el-switch v-model="alertSoundOn" :active-text="labels.on" :inactive-text="labels.off" />
      </el-form-item>
      <el-form-item :label="labels.alertPopup">
        <el-switch v-model="form.alertPopupEnabled" :active-text="labels.on" :inactive-text="labels.off" />
      </el-form-item>
      <el-form-item :label="labels.volume" prop="alertVolume">
        <el-slider
          v-model="form.alertVolume"
          :min="0"
          :max="1"
          :step="0.01"
          :format-tooltip="formatVolume"
          style="width: 280px; margin-left: 8px"
        />
      </el-form-item>
      <el-form-item :label="labels.soundMode" prop="alertSoundMode">
        <el-radio-group v-model="form.alertSoundMode">
          <el-radio label="preset">{{ labels.soundModePreset }}</el-radio>
          <el-radio label="custom">{{ labels.soundModeCustom }}</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="form.alertSoundMode === 'preset'" :label="labels.beepPreset" prop="beepPreset">
        <el-select v-model="form.beepPreset" :placeholder="labels.beepPresetPlaceholder" style="width: 200px">
          <el-option
            v-for="item in beepPresetOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-button type="primary" plain size="mini" style="margin-left: 12px" @click="handlePreview('offline')">{{ labels.preview }}</el-button>
        <el-button size="mini" style="margin-left: 8px" @click="handleStopPreview">{{ labels.stopPreview }}</el-button>
      </el-form-item>
      <template v-else>
        <el-form-item>
          <div class="audio-hint local-audio-tip">{{ labels.localAudioTip }}</div>
        </el-form-item>
        <el-form-item :label="labels.offlineAudio">
          <div class="audio-row">
            <input
              ref="offlineFile"
              type="file"
              accept="audio/mpeg,audio/mp3,audio/wav,audio/ogg,.mp3,.wav,.ogg"
              class="audio-file"
              @change="onPickAudio('offline', $event)"
            >
            <span class="audio-name">{{ form.customOfflineName || labels.noFile }}</span>
            <el-button size="mini" @click="$refs.offlineFile && $refs.offlineFile.click()">{{ labels.chooseFile }}</el-button>
            <el-button size="mini" :disabled="!form.customOfflineAudio" @click="clearAudio('offline')">{{ labels.clearFile }}</el-button>
            <el-button type="primary" plain size="mini" @click="handlePreview('offline')">{{ labels.previewOffline }}</el-button>
            <el-button size="mini" @click="handleStopPreview">{{ labels.stopPreview }}</el-button>
          </div>
          <div class="audio-hint">{{ labels.audioHint }}</div>
        </el-form-item>
        <el-form-item :label="labels.onlineAudio">
          <div class="audio-row">
            <input
              ref="onlineFile"
              type="file"
              accept="audio/mpeg,audio/mp3,audio/wav,audio/ogg,.mp3,.wav,.ogg"
              class="audio-file"
              @change="onPickAudio('online', $event)"
            >
            <span class="audio-name">{{ form.customOnlineName || labels.noFile }}</span>
            <el-button size="mini" @click="$refs.onlineFile && $refs.onlineFile.click()">{{ labels.chooseFile }}</el-button>
            <el-button size="mini" :disabled="!form.customOnlineAudio" @click="clearAudio('online')">{{ labels.clearFile }}</el-button>
            <el-button type="primary" plain size="mini" @click="handlePreview('online')">{{ labels.previewOnline }}</el-button>
            <el-button size="mini" @click="handleStopPreview">{{ labels.stopPreview }}</el-button>
          </div>
          <div class="audio-hint">{{ labels.audioHint }}</div>
        </el-form-item>
      </template>

      <el-divider content-position="left">{{ labels.probeSection }}</el-divider>
      <el-form-item :label="labels.probeInterval" prop="probeIntervalMs">
        <el-input-number
          v-model="form.probeIntervalMs"
          :min="1000"
          :step="500"
          controls-position="right"
        />
      </el-form-item>
      <el-form-item :label="labels.hoverDelay" prop="hoverSummaryDelayMs">
        <el-input-number
          v-model="form.hoverSummaryDelayMs"
          :min="100"
          :step="50"
          controls-position="right"
        />
      </el-form-item>

      <el-divider content-position="left">{{ labels.colorSection }}</el-divider>
      <el-form-item
        v-for="item in colorOptions"
        :key="item.key"
        :label="item.label"
      >
        <el-color-picker
          v-model="colorHex[item.key]"
          color-format="hex"
          :predefine="predefineColors"
          @active-change="val => onColorActiveChange(item.key, val)"
          @change="val => onColorChange(item.key, val)"
        />
        <span class="color-hex">{{ colorHex[item.key] }}</span>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="saving" @click="handleSave">{{ labels.save }}</el-button>
        <el-button :loading="resetting" @click="handleReset">{{ labels.reset }}</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { getMonitorSettings, saveMonitorSettings, resetMonitorSettings } from '@/api/scene/monitorSettings'
import { hexToRgba, rgbaToHexString, normalizePickerHex } from '@/utils/scene/colorRgba'
import { alertBeep, stopAlertBeep, setAlertBeepVolume, unlockAlertAudio } from '@/utils/scene/alertBeep'
import { MAX_CUSTOM_AUDIO_BYTES, estimateDataUrlBytes } from '@/utils/scene/monitorSettingsStore'

const COLOR_KEYS = ['default', 'green', 'yellow', 'red']
const MAX_AUDIO = MAX_CUSTOM_AUDIO_BYTES || (1024 * 1024)

function emptyForm() {
  return {
    alertMuted: false,
    alertPopupEnabled: true,
    alertVolume: 0.7,
    beepPreset: 'default',
    alertSoundMode: 'preset',
    customOfflineAudio: null,
    customOnlineAudio: null,
    customOfflineName: '',
    customOnlineName: '',
    probeIntervalMs: 5000,
    hoverSummaryDelayMs: 500,
    buildingStatusColors: {
      default: { r: 0, g: 1, b: 1, a: 0.45 },
      green: { r: 0.2, g: 0.85, b: 0.3, a: 0.55 },
      yellow: { r: 0.95, g: 0.85, b: 0.2, a: 0.55 },
      red: { r: 0.95, g: 0.25, b: 0.2, a: 0.55 }
    }
  }
}

export default {
  name: 'CesiumMonitorSettings',
  data() {
    return {
      loading: false,
      saving: false,
      resetting: false,
      form: emptyForm(),
      colorHex: {
        default: '#00FFFF',
        green: '#33D94D',
        yellow: '#F2D933',
        red: '#F24033'
      },
      colorAlpha: {
        default: 0.45,
        green: 0.55,
        yellow: 0.55,
        red: 0.55
      },
      labels: {
        alertSection: '\u544a\u8b66',
        alertSound: '\u544a\u8b66\u58f0\u97f3',
        alertPopup: '\u544a\u8b66\u5f39\u7a97\u63d0\u793a',
        on: '\u5f00',
        off: '\u5173',
        volume: '\u97f3\u91cf',
        soundMode: '\u97f3\u6e90\u6a21\u5f0f',
        soundModePreset: '\u5185\u7f6e\u9884\u8bbe',
        soundModeCustom: '\u81ea\u5b9a\u4e49\u6587\u4ef6',
        beepPreset: '\u63d0\u793a\u97f3\u9884\u8bbe',
        beepPresetPlaceholder: '\u8bf7\u9009\u62e9\u63d0\u793a\u97f3\u9884\u8bbe',
        preview: '\u8bd5\u542c',
        stopPreview: '\u505c\u6b62\u8bd5\u542c',
        previewOffline: '\u8bd5\u542c\u79bb\u7ebf\u97f3',
        previewOnline: '\u8bd5\u542c\u4e0a\u7ebf\u97f3',
        offlineAudio: '\u79bb\u7ebf\u63d0\u793a\u97f3',
        onlineAudio: '\u4e0a\u7ebf\u63d0\u793a\u97f3',
        chooseFile: '\u9009\u62e9\u6587\u4ef6',
        clearFile: '\u6e05\u9664',
        noFile: '\u672a\u4e0a\u4f20',
        audioHint: '\u652f\u6301 mp3/wav/ogg\uff0c\u5355\u6587\u4ef6\u22641MB',
        localAudioTip: '\u81ea\u5b9a\u4e49\u97f3\u9891\u4ec5\u4fdd\u5b58\u5728\u672c\u673a\u6d4f\u89c8\u5668\uff0c\u6362\u673a\u6216\u6e05\u9664\u7f13\u5b58\u540e\u9700\u91cd\u65b0\u4e0a\u4f20',
        probeSection: '\u63a2\u6d4b\u4e0e\u4ea4\u4e92',
        probeInterval: '\u63a2\u6d4b\u95f4\u9694(ms)',
        hoverDelay: '\u60ac\u505c\u5ef6\u8fdf(ms)',
        colorSection: '\u5efa\u7b51\u72b6\u6001\u8272',
        save: '\u4fdd\u5b58',
        reset: '\u6062\u590d\u9ed8\u8ba4'
      },
      beepPresetOptions: [
        { value: 'soft', label: '\u67d4\u548c' },
        { value: 'default', label: '\u9ed8\u8ba4' },
        { value: 'sharp', label: '\u6e05\u8106' }
      ],
      colorOptions: [
        { key: 'default', label: '\u9ed8\u8ba4\u8272' },
        { key: 'green', label: '\u7eff\u8272(\u6b63\u5e38)' },
        { key: 'yellow', label: '\u9ec4\u8272(\u544a\u8b66)' },
        { key: 'red', label: '\u7ea2\u8272(\u5f02\u5e38)' }
      ],
      predefineColors: [
        '#00FFFF',
        '#33D94D',
        '#F2D933',
        '#F24033',
        '#409EFF',
        '#67C23A',
        '#E6A23C',
        '#F56C6C',
        '#909399',
        '#000000',
        '#FFFFFF'
      ],
      rules: {
        alertVolume: [
          { required: true, message: '\u8bf7\u8bbe\u7f6e\u97f3\u91cf', trigger: 'change' }
        ],
        beepPreset: [
          { required: true, message: '\u8bf7\u9009\u62e9\u63d0\u793a\u97f3\u9884\u8bbe', trigger: 'change' }
        ],
        probeIntervalMs: [
          { required: true, message: '\u8bf7\u8f93\u5165\u63a2\u6d4b\u95f4\u9694', trigger: 'change' }
        ],
        hoverSummaryDelayMs: [
          { required: true, message: '\u8bf7\u8f93\u5165\u60ac\u505c\u5ef6\u8fdf', trigger: 'change' }
        ]
      }
    }
  },
  computed: {
    alertSoundOn: {
      get() {
        return !this.form.alertMuted
      },
      set(val) {
        this.form.alertMuted = !val
      }
    }
  },
  watch: {
    'form.alertVolume'(val) {
      setAlertBeepVolume(val)
    },
    'form.alertMuted'(muted) {
      if (muted) {
        stopAlertBeep()
      }
    }
  },
  created() {
    this.loadSettings()
  },
  methods: {
    formatVolume(val) {
      return Math.round(Number(val) * 100) + '%'
    },
    onColorActiveChange(key, val) {
      // Commit while the panel is open so Save cannot race an unconfirmed pick.
      if (val == null || val === '') return
      this.$set(this.colorHex, key, normalizePickerHex(val))
    },
    onColorChange(key, val) {
      if (val == null || val === '') return
      this.$set(this.colorHex, key, normalizePickerHex(val))
    },
    applySettings(settings) {
      const next = settings || emptyForm()
      this.form = {
        alertMuted: !!next.alertMuted,
        alertPopupEnabled: next.alertPopupEnabled !== false,
        alertVolume: typeof next.alertVolume === 'number' ? next.alertVolume : 0.7,
        beepPreset: next.beepPreset || 'default',
        alertSoundMode: next.alertSoundMode === 'custom' ? 'custom' : 'preset',
        customOfflineAudio: next.customOfflineAudio || null,
        customOnlineAudio: next.customOnlineAudio || null,
        customOfflineName: next.customOfflineName || '',
        customOnlineName: next.customOnlineName || '',
        probeIntervalMs: next.probeIntervalMs,
        hoverSummaryDelayMs: next.hoverSummaryDelayMs,
        buildingStatusColors: next.buildingStatusColors || emptyForm().buildingStatusColors
      }
      const colors = this.form.buildingStatusColors || {}
      const defaults = emptyForm().buildingStatusColors
      COLOR_KEYS.forEach(key => {
        const rgba = colors[key] || defaults[key]
        this.$set(this.colorHex, key, rgbaToHexString(rgba))
        this.$set(
          this.colorAlpha,
          key,
          typeof rgba.a === 'number' ? rgba.a : (defaults[key] && defaults[key].a) || 0.55
        )
      })
    },
    buildPayload() {
      const buildingStatusColors = {}
      const defaults = emptyForm().buildingStatusColors
      COLOR_KEYS.forEach(key => {
        const hex = normalizePickerHex(this.colorHex[key])
        const alpha =
          typeof this.colorAlpha[key] === 'number'
            ? this.colorAlpha[key]
            : (defaults[key] && defaults[key].a) || 0.55
        const rgba = hexToRgba(hex, alpha)
        // Round-trip through hex so stored RGB matches the picker exactly.
        buildingStatusColors[key] = hexToRgba(rgbaToHexString(rgba), rgba.a)
      })
      return {
        alertMuted: !!this.form.alertMuted,
        alertPopupEnabled: this.form.alertPopupEnabled !== false,
        alertVolume: Number(this.form.alertVolume),
        beepPreset: this.form.beepPreset,
        alertSoundMode: this.form.alertSoundMode === 'custom' ? 'custom' : 'preset',
        customOfflineAudio: this.form.customOfflineAudio || null,
        customOnlineAudio: this.form.customOnlineAudio || null,
        customOfflineName: this.form.customOfflineName || '',
        customOnlineName: this.form.customOnlineName || '',
        probeIntervalMs: Math.round(Number(this.form.probeIntervalMs)),
        hoverSummaryDelayMs: Math.round(Number(this.form.hoverSummaryDelayMs)),
        buildingStatusColors
      }
    },
    notifySettingsChanged() {
      if (typeof window !== 'undefined' && typeof window.dispatchEvent === 'function') {
        window.dispatchEvent(new CustomEvent('ruoyi-monitor-settings-changed'))
      }
    },
    clearAudio(kind) {
      if (kind === 'online') {
        this.form.customOnlineAudio = null
        this.form.customOnlineName = ''
        if (this.$refs.onlineFile) this.$refs.onlineFile.value = ''
      } else {
        this.form.customOfflineAudio = null
        this.form.customOfflineName = ''
        if (this.$refs.offlineFile) this.$refs.offlineFile.value = ''
      }
    },
    onPickAudio(kind, event) {
      const file = event && event.target && event.target.files && event.target.files[0]
      if (!file) return
      if (file.size > MAX_AUDIO) {
        this.$message.error('\u97f3\u9891\u6587\u4ef6\u4e0d\u80fd\u8d85\u8fc7 1MB')
        event.target.value = ''
        return
      }
      const reader = new FileReader()
      reader.onload = () => {
        const dataUrl = reader.result
        const bytes = typeof estimateDataUrlBytes === 'function'
          ? estimateDataUrlBytes(dataUrl)
          : file.size
        if (bytes > MAX_AUDIO) {
          this.$message.error('\u97f3\u9891\u6587\u4ef6\u4e0d\u80fd\u8d85\u8fc7 1MB')
          event.target.value = ''
          return
        }
        if (kind === 'online') {
          this.form.customOnlineAudio = dataUrl
          this.form.customOnlineName = file.name
        } else {
          this.form.customOfflineAudio = dataUrl
          this.form.customOfflineName = file.name
        }
      }
      reader.onerror = () => {
        this.$message.error('\u8bfb\u53d6\u97f3\u9891\u6587\u4ef6\u5931\u8d25')
        event.target.value = ''
      }
      reader.readAsDataURL(file)
    },
    loadSettings() {
      this.loading = true
      return getMonitorSettings().then(response => {
        if (response.code !== 200) {
          this.$message.error(response.msg || '\u52a0\u8f7d\u5931\u8d25')
          return
        }
        this.applySettings(response.data)
      }).catch(() => {
        this.$message.error('\u52a0\u8f7d\u5931\u8d25')
      }).finally(() => {
        this.loading = false
      })
    },
    handlePreview(kind) {
      unlockAlertAudio()
      alertBeep({
        volume: Number(this.form.alertVolume),
        preset: this.form.beepPreset,
        muted: false,
        kind: kind === 'online' ? 'online' : 'offline',
        soundMode: this.form.alertSoundMode,
        customOfflineAudio: this.form.customOfflineAudio,
        customOnlineAudio: this.form.customOnlineAudio
      })
    },
    handleStopPreview() {
      stopAlertBeep()
    },
    handleSave() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        this.saving = true
        const payload = this.buildPayload()
        saveMonitorSettings(payload).then(response => {
          if (response.code !== 200) {
            this.$message.error(response.msg || '\u4fdd\u5b58\u5931\u8d25')
            return
          }
          const saved = response.data || payload
          this.applySettings(saved)
          // Keep colorHex exactly from saved rgba (no picker drift).
          COLOR_KEYS.forEach(key => {
            const rgba = saved.buildingStatusColors && saved.buildingStatusColors[key]
            if (rgba) this.$set(this.colorHex, key, rgbaToHexString(rgba))
          })
          // Already persisted via PUT above — do not call setGlobalAlert* (second PUT
          // trips RuoYi repeatSubmit within 1s: "数据正在处理，请勿重复提交").
          this.notifySettingsChanged()
          if (!saved.alertMuted) unlockAlertAudio()
          this.$message.success('\u4fdd\u5b58\u6210\u529f')
        }).catch(() => {
          this.$message.error('\u4fdd\u5b58\u5931\u8d25')
        }).finally(() => {
          this.saving = false
        })
      })
    },
    handleReset() {
      this.$confirm('\u786e\u8ba4\u6062\u590d\u9ed8\u8ba4\u76d1\u63a7\u8bbe\u7f6e\uff1f', '\u63d0\u793a', {
        confirmButtonText: '\u786e\u5b9a',
        cancelButtonText: '\u53d6\u6d88',
        type: 'warning'
      }).then(() => {
        this.resetting = true
        return resetMonitorSettings().then(response => {
          if (response.code !== 200) {
            this.$message.error(response.msg || '\u6062\u590d\u9ed8\u8ba4\u5931\u8d25')
            return
          }
          this.applySettings(response.data)
          this.notifySettingsChanged()
          this.$message.success('\u5df2\u6062\u590d\u9ed8\u8ba4')
        }).catch(() => {
          this.$message.error('\u6062\u590d\u9ed8\u8ba4\u5931\u8d25')
        }).finally(() => {
          this.resetting = false
        })
      }).catch(() => {
        /* cancel */
      })
    }
  }
}
</script>

<style scoped>
.audio-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.audio-file {
  display: none;
}
.audio-name {
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #606266;
}
.audio-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}
.local-audio-tip {
  color: #E6A23C;
}
.color-hex {
  margin-left: 12px;
  color: #606266;
  font-family: Consolas, Monaco, monospace;
}
</style>
