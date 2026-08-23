<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="文档标题" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入文档标题"
          clearable
          @keyup.enter.native="handleQuery"
        />
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
          v-hasPermi="['collab:doc:add']"
        >新增</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="docList">
      <el-table-column label="文档标题" align="center" prop="title" :show-overflow-tooltip="true" />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template slot-scope="scope">
          <span>{{ statusLabel(scope.row.status) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="120">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['collab:doc:edit']"
          >编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-dialogDrag
      v-dialogDragWidth
      v-dialogDragHeight
      :title="title"
      :visible.sync="open"
      width="780px"
      append-to-body
    >
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="文档标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入文档标题" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="内容">
          <el-button type="primary" plain size="mini" icon="el-icon-plus" @click="appendSection">添加区块</el-button>
          <editor v-model="form.contentHtml" :min-height="300" />
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
import { listDoc, getDoc, addDoc, updateDoc } from '@/api/collab/doc'
import { listSectionIds } from '@/utils/collab/sectionScope'

const STATUS_OPTIONS = [
  { value: '0', label: '草稿' },
  { value: '1', label: '已发布' }
]

export default {
  name: 'CollabDoc',
  data() {
    return {
      loading: true,
      showSearch: true,
      docList: [],
      title: '',
      open: false,
      statusOptions: STATUS_OPTIONS,
      queryParams: {
        title: undefined
      },
      form: {},
      rules: {
        title: [
          { required: true, message: '文档标题不能为空', trigger: 'blur' }
        ],
        status: [
          { required: true, message: '状态不能为空', trigger: 'change' }
        ]
      }
    }
  },
  created() {
    this.getList()
  },
  watch: {
    '$route.query': {
      handler(query) {
        if (query && query.docId) {
          this.openDocById(query.docId)
        }
      },
      deep: true
    }
  },
  methods: {
    statusLabel(status) {
      const item = STATUS_OPTIONS.find(opt => opt.value === String(status))
      return item ? item.label : status || '-'
    },
    getList() {
      this.loading = true
      listDoc(this.queryParams).then(response => {
        if (response.code !== 200) {
          this.docList = []
          this.$modal.msgError(response.msg || '查询失败')
          return
        }
        this.docList = response.data || []
        this.tryOpenFromQuery()
      }).catch(() => { this.docList = [] }).finally(() => {
        this.loading = false
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        docId: undefined,
        title: undefined,
        contentHtml: '',
        status: '0'
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
      this.open = true
      this.title = '新增文档'
    },
    handleUpdate(row) {
      this.openDocById(row.docId)
    },
    openDocById(docId) {
      if (!docId) {
        return
      }
      this.reset()
      getDoc(docId).then(response => {
        if (response.code !== 200) {
          this.$modal.msgError(response.msg || '查询失败')
          return
        }
        this.form = {
          docId: response.data.docId,
          title: response.data.title,
          contentHtml: response.data.contentHtml || '',
          status: response.data.status != null ? String(response.data.status) : '0'
        }
        this.open = true
        this.title = '编辑文档'
      }).catch(() => {})
    },
    tryOpenFromQuery() {
      const docId = this.$route.query.docId
      if (docId) {
        this.openDocById(docId)
      }
    },
    nextSectionId() {
      const ids = listSectionIds(this.form.contentHtml || '')
      let maxN = 0
      ids.forEach(id => {
        const match = /^sec-(\d+)$/.exec(id)
        if (match) {
          maxN = Math.max(maxN, parseInt(match[1], 10))
        }
      })
      return 'sec-' + (maxN + 1)
    },
    appendSection() {
      const secId = this.nextSectionId()
      const block = `<div data-sec-id="${secId}"><p><br></p></div>`
      this.form.contentHtml = (this.form.contentHtml || '') + block
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) {
          return
        }
        const payload = {
          docId: this.form.docId,
          title: this.form.title,
          contentHtml: this.form.contentHtml || '',
          status: this.form.status
        }
        const request = payload.docId ? updateDoc(payload) : addDoc(payload)
        request.then(response => {
          if (response.code !== 200) {
            this.$modal.msgError(response.msg || '操作失败')
            return
          }
          this.$modal.msgSuccess(payload.docId ? '修改成功' : '新增成功')
          this.open = false
          this.getList()
        }).catch(() => {})
      })
    }
  }
}
</script>
