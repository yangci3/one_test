<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="任务标题" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入任务标题"
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
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="assignmentList">
      <el-table-column label="任务标题" align="center" prop="title" :show-overflow-tooltip="true" />
      <el-table-column label="截止时间" align="center" prop="deadlineAt" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.deadlineAt) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="提交状态" align="center" prop="submitStatus" width="110">
        <template slot-scope="scope">
          <span>{{ submitStatusLabel(scope.row.submitStatus) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="100">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            :icon="canEditRow(scope.row) ? 'el-icon-edit' : 'el-icon-view'"
            @click="handleOpen(scope.row)"
          >{{ canEditRow(scope.row) ? '编辑' : '查看' }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-dialogDrag
      v-dialogDragWidth
      v-dialogDragHeight
      :title="editorTitle"
      :visible.sync="editorOpen"
      width="780px"
      append-to-body
      @close="closeEditor"
    >
      <div v-loading="editorLoading">
        <template v-if="editorDetail">
          <el-descriptions :column="2" border size="small" class="mb16">
            <el-descriptions-item label="任务标题">{{ editorDetail.task.title }}</el-descriptions-item>
            <el-descriptions-item label="文档标题">{{ editorDetail.docTitle || '-' }}</el-descriptions-item>
            <el-descriptions-item label="截止时间">{{ parseTime(editorDetail.task.deadlineAt) }}</el-descriptions-item>
            <el-descriptions-item label="提交状态">{{ submitStatusLabel(myAssignment.submitStatus) }}</el-descriptions-item>
            <el-descriptions-item label="编辑区块" :span="2">{{ formatScope(myAssignment.scopeJson) }}</el-descriptions-item>
          </el-descriptions>
          <editor v-model="editorContent" :min-height="300" :read-only="!editorEditable" />
        </template>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button
          type="primary"
          :disabled="!editorEditable"
          :loading="savingDraft"
          @click="handleSaveDraft"
          v-hasPermi="['collab:task:submit']"
        >保存草稿</el-button>
        <el-button
          type="success"
          :disabled="!editorEditable"
          :loading="submitting"
          @click="handleSubmit"
          v-hasPermi="['collab:task:submit']"
        >提交</el-button>
        <el-button @click="editorOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listMyTask, getTask, saveDraft, submitAssignment } from '@/api/collab/task'
import { extractEditableHtml, parseScopeJson } from '@/utils/collab/sectionScope'

const SUBMIT_STATUS_OPTIONS = [
  { value: 'editing', label: '编辑中' },
  { value: 'submitted', label: '已提交' },
  { value: 'overdue', label: '已逾期' },
  { value: 'resubmit_allowed', label: '允许补交' }
]

export default {
  name: 'CollabTaskMine',
  data() {
    return {
      loading: true,
      showSearch: true,
      assignmentList: [],
      queryParams: {
        title: undefined
      },
      editorOpen: false,
      editorLoading: false,
      editorDetail: null,
      myAssignment: null,
      editorContent: '',
      currentTaskId: null,
      savingDraft: false,
      submitting: false
    }
  },
  computed: {
    editorEditable() {
      if (!this.editorDetail || !this.myAssignment) {
        return false
      }
      return this.canEditAssignment(this.myAssignment, this.editorDetail.task)
    },
    editorTitle() {
      return this.editorEditable ? '编辑协作内容' : '查看协作内容'
    }
  },
  created() {
    this.getList()
  },
  methods: {
    submitStatusLabel(status) {
      const item = SUBMIT_STATUS_OPTIONS.find(opt => opt.value === status)
      return item ? item.label : status || '-'
    },
    formatScope(scopeJson) {
      const ids = parseScopeJson(scopeJson)
      return ids.length > 0 ? ids.join(', ') : '-'
    },
    currentUserId() {
      const id = this.$store.getters.id
      return id != null && id !== '' ? Number(id) : null
    },
    findMyAssignment(assignments) {
      const userId = this.currentUserId()
      if (userId == null) {
        return null
      }
      return (assignments || []).find(a => Number(a.userId) === userId) || null
    },
    isPastDeadline(deadlineAt) {
      if (!deadlineAt) {
        return false
      }
      return new Date(deadlineAt).getTime() < Date.now()
    },
    canEditAssignment(assignment, task) {
      if (!assignment || !task) {
        return false
      }
      const status = assignment.submitStatus
      if (status === 'submitted') {
        return false
      }
      if (status === 'resubmit_allowed') {
        return true
      }
      return status === 'editing' && !this.isPastDeadline(task.deadlineAt)
    },
    canEditRow(row) {
      return this.canEditAssignment(
        { submitStatus: row.submitStatus },
        { deadlineAt: row.deadlineAt }
      )
    },
    resolveEditorContent(detail, assignment) {
      const sectionIds = parseScopeJson(assignment.scopeJson)
      if (assignment.draftContent) {
        return assignment.draftContent
      }
      if (assignment.contentSnapshot && !this.canEditAssignment(assignment, detail.task)) {
        return assignment.contentSnapshot
      }
      const fullHtml = detail.contentHtml || detail.docContentHtml || ''
      return extractEditableHtml(fullHtml, sectionIds)
    },
    enrichTaskRow(task, detailRes) {
      const assignment = detailRes.code === 200
        ? this.findMyAssignment((detailRes.data && detailRes.data.assignments) || [])
        : null
      return {
        taskId: task.taskId,
        title: task.title,
        deadlineAt: task.deadlineAt,
        assignmentId: assignment ? assignment.assignmentId : null,
        submitStatus: assignment ? assignment.submitStatus : '-',
        scopeJson: assignment ? assignment.scopeJson : null
      }
    },
    getList() {
      this.loading = true
      listMyTask(this.queryParams).then(response => {
        if (response.code !== 200) {
          this.assignmentList = []
          this.$modal.msgError(response.msg || '查询失败')
          return
        }
        const tasks = response.data || []
        if (tasks.length === 0) {
          this.assignmentList = []
          return
        }
        return Promise.all(tasks.map(task => getTask(task.taskId).then(detailRes => this.enrichTaskRow(task, detailRes))))
          .then(rows => {
            this.assignmentList = rows
          })
      }).catch(() => {
        this.assignmentList = []
      }).finally(() => {
        this.loading = false
      })
    },
    handleQuery() {
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleOpen(row) {
      if (!row.taskId) {
        return
      }
      this.currentTaskId = row.taskId
      this.editorOpen = true
      this.editorLoading = true
      this.editorDetail = null
      this.myAssignment = null
      this.editorContent = ''

      getTask(row.taskId).then(response => {
        if (response.code !== 200) {
          this.$modal.msgError(response.msg || '加载任务失败')
          this.editorOpen = false
          return
        }
        this.editorDetail = response.data
        this.myAssignment = this.findMyAssignment(this.editorDetail.assignments)
        if (!this.myAssignment) {
          this.$modal.msgError('未找到您的指派信息')
          this.editorOpen = false
          return
        }
        this.editorContent = this.resolveEditorContent(this.editorDetail, this.myAssignment)
        this.syncListRow(row.taskId)
      }).catch(() => {
        this.$modal.msgError('加载任务失败')
        this.editorOpen = false
      }).finally(() => {
        this.editorLoading = false
      })
    },
    syncListRow(taskId) {
      const index = this.assignmentList.findIndex(row => row.taskId === taskId)
      if (index < 0 || !this.myAssignment) {
        return
      }
      this.$set(this.assignmentList, index, {
        ...this.assignmentList[index],
        assignmentId: this.myAssignment.assignmentId,
        submitStatus: this.myAssignment.submitStatus
      })
    },
    closeEditor() {
      this.editorDetail = null
      this.myAssignment = null
      this.editorContent = ''
      this.currentTaskId = null
    },
    handleSaveDraft() {
      if (!this.editorEditable || !this.currentTaskId || !this.myAssignment) {
        return
      }
      this.savingDraft = true
      saveDraft(this.currentTaskId, this.myAssignment.assignmentId, {
        draftContent: this.editorContent
      }).then(response => {
        if (response.code !== 200) {
          this.$modal.msgError(response.msg || '保存失败')
          return
        }
        this.$modal.msgSuccess('草稿已保存')
      }).catch(() => {}).finally(() => {
        this.savingDraft = false
      })
    },
    handleSubmit() {
      if (!this.editorEditable || !this.currentTaskId || !this.myAssignment) {
        return
      }
      this.$confirm('提交后将无法继续编辑（除非管理员允许补交），确认提交？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.submitting = true
        return submitAssignment(this.currentTaskId, this.myAssignment.assignmentId, {
          content: this.editorContent
        })
      }).then(response => {
        if (!response) {
          return
        }
        if (response.code !== 200) {
          this.$modal.msgError(response.msg || '提交失败')
          return
        }
        this.$modal.msgSuccess('提交成功')
        this.editorOpen = false
        this.getList()
      }).catch(() => {}).finally(() => {
        this.submitting = false
      })
    }
  }
}
</script>

<style scoped>
.mb16 {
  margin-bottom: 16px;
}
</style>
