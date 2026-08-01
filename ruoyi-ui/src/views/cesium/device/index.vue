<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="设备名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入设备名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="IP地址" prop="ip">
        <el-input
          v-model="queryParams.ip"
          placeholder="请输入IP地址"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="MAC\u5730\u5740" prop="mac">
        <el-input
          v-model="queryParams.mac"
          placeholder="请输入MAC\u5730\u5740"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="设备类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="设备类型" clearable>
          <el-option
            v-for="item in typeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="所属建筑" prop="buildingId">
        <el-select v-model="queryParams.buildingId" placeholder="所属建筑" clearable>
          <el-option
            v-for="item in buildingOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
        >新增</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="deviceList">
      <el-table-column label="设备名称" align="center" prop="name" :show-overflow-tooltip="true" />
      <el-table-column label="IP地址" align="center" prop="ip" width="140" />
      <el-table-column label="MAC" align="center" prop="mac" width="160" :show-overflow-tooltip="true" />
      <el-table-column label="设备类型" align="center" prop="typeLabel" width="100" />
      <el-table-column label="所属建筑" align="center" prop="buildingName" :show-overflow-tooltip="true" />
      <el-table-column label="上级设备" align="center" prop="parentName" :show-overflow-tooltip="true" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-dialogDrag :title="title" :visible.sync="open" width="520px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="设备名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入设备名称" />
        </el-form-item>
        <el-form-item label="IP地址" prop="ip">
          <el-input v-model="form.ip" placeholder="请输入IP地址" />
        </el-form-item>
        <el-form-item label="MAC\u5730\u5740" prop="mac">
          <el-input v-model="form.mac" placeholder="请输入MAC\u5730\u5740" />
        </el-form-item>
        <el-form-item label="设备类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择设备类型" style="width: 100%">
            <el-option
              v-for="item in typeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="所属建筑" prop="buildingId">
          <el-select v-model="form.buildingId" placeholder="请选择所属建筑" style="width: 100%">
            <el-option
              v-for="item in buildingOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="上级设备" prop="parentDeviceId">
          <el-select v-model="form.parentDeviceId" placeholder="可选，跨建筑上级" clearable style="width: 100%">
            <el-option
              v-for="item in parentOptions"
              :key="item.id"
              :label="item.name + ' (' + item.ip + ')'"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listDevices, getDevice, addDevice, updateDevice, delDevice } from '@/api/scene/device'
import { getBuildings } from '@/api/scene/buildings'

const TYPE_LABELS = {
  router: '路由器',
  switch: '交换机',
  terminal: '终端',
  other: '其他'
}

export default {
  name: 'CesiumDevice',
  data() {
    return {
      loading: true,
      showSearch: true,
      deviceList: [],
      allDevices: [],
      buildingOptions: [],
      typeOptions: [
        { value: 'router', label: '路由器' },
        { value: 'switch', label: '交换机' },
        { value: 'terminal', label: '终端' },
        { value: 'other', label: '其他' }
      ],
      title: '',
      open: false,
      queryParams: {
        name: undefined,
        ip: undefined,
        mac: undefined,
        type: undefined,
        buildingId: undefined
      },
      form: {},
      rules: {
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
      }
    }
  },
  computed: {
    parentOptions() {
      const currentId = this.form && this.form.id
      return this.allDevices.filter(d => d.id !== currentId)
    }
  },
  created() {
    this.loadBuildings().then(() => {
      this.getList()
    })
  },
  methods: {
    typeLabel(type) {
      return TYPE_LABELS[type] || type || '-'
    },
    buildingName(buildingId) {
      const building = this.buildingOptions.find(b => b.id === buildingId)
      return building ? building.name : (buildingId || '-')
    },
    parentName(parentDeviceId) {
      if (!parentDeviceId) {
        return '-'
      }
      const parent = this.allDevices.find(d => d.id === parentDeviceId)
      return parent ? parent.name : parentDeviceId
    },
    enrichRows(list) {
      return list.map(d => ({
        ...d,
        typeLabel: this.typeLabel(d.type),
        buildingName: this.buildingName(d.buildingId),
        parentName: this.parentName(d.parentDeviceId)
      }))
    },
    loadBuildings() {
      return getBuildings().then(response => {
        if (response.code === 200) {
          this.buildingOptions = response.data || []
        }
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
    getList() {
      this.loading = true
      this.loadAllDevices().then(() => {
        return listDevices(this.queryParams)
      }).then(response => {
        if (response.code !== 200) {
          this.deviceList = []
          this.$modal.msgError(response.msg || '查询失败')
          return
        }
        this.deviceList = this.enrichRows(response.data || [])
      }).finally(() => {
        this.loading = false
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        id: undefined,
        name: undefined,
        ip: undefined,
        mac: '',
        type: undefined,
        buildingId: undefined,
        parentDeviceId: undefined,
        remark: undefined
      }
      this.resetForm('form')
    },
    handleQuery() {
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleAdd() {
      this.reset()
      this.loadAllDevices().then(() => {
        this.open = true
        this.title = '新增设备'
      })
    },
    handleUpdate(row) {
      this.reset()
      this.loadAllDevices().then(() => {
        return getDevice(row.id)
      }).then(response => {
        if (response.code !== 200) {
          this.$modal.msgError(response.msg || '查询失败')
          return
        }
        this.form = {
          id: response.data.id,
          name: response.data.name,
          ip: response.data.ip,
          mac: response.data.mac || '',
          type: response.data.type,
          buildingId: response.data.buildingId,
          parentDeviceId: response.data.parentDeviceId || undefined,
          remark: response.data.remark
        }
        this.open = true
        this.title = '修改设备'
      })
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) {
          return
        }
        const payload = {
          id: this.form.id,
          name: this.form.name,
          ip: this.form.ip,
          mac: this.form.mac || '',
          type: this.form.type,
          buildingId: this.form.buildingId,
          parentDeviceId: this.form.parentDeviceId || null,
          remark: this.form.remark || ''
        }
        const request = payload.id ? updateDevice(payload) : addDevice(payload)
        request.then(response => {
          if (response.code !== 200) {
            this.$modal.msgError(response.msg || '操作失败')
            return
          }
          this.$modal.msgSuccess(payload.id ? '修改成功' : '新增成功')
          this.open = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
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
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  }
}
</script>
