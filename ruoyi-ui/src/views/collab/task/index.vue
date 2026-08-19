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
      <el-form-item label="任务状态" prop="taskStatus">
        <el-select v-model="queryParams.taskStatus" placeholder="任务状态" clearable>
          <el-option
            v-for="item in taskStatusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
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
          v-hasPermi="['collab:task:add']"
        >新建任务</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="taskList">
      <el-table-column label="任务标题" align="center" prop="title" :show-overflow-tooltip="true" />
      <el-table-column label="文档标题" align="center" prop="docTitle" :show-overflow-tooltip="true" />
      <el-table-column label="截止时间" align="center" prop="deadlineAt" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.deadlineAt) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="任务状态" align="center" prop="taskStatus" width="100">
        <template slot-scope="scope">
          <span>{{ taskStatusLabel(scope.row.taskStatus) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleDetail(scope.row)"
          >详情</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-setting"
            @click="handleManage(scope.row)"
            v-hasPermi="['collab:task:edit']"
          >管理</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-dialogDrag
      v-dialogDragWidth
      v-dialogDragHeight
      title="新建协作任务"
      :visible.sync="createOpen"
      width="720px"
      append-to-body
      @close="cancelCreate"
    >
      <el-form ref="createForm" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="关联文档" prop="docId">
          <el-select
            v-model="createForm.docId"
            placeholder="请选择文档"
            filterable
            style="width: 100%"
            @change="handleDocChange"
          >
            <el-option
              v-for="item in docOptions"
              :key="item.docId"
              :label="item.title"
              :value="item.docId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="任务标题" prop="title">
          <el-input v-model="createForm.title" placeholder="请输入任务标题" />
        </el-form-item>
        <el-form-item label="截止时间" prop="deadlineAt">
          <el-date-picker
            v-model="createForm.deadlineAt"
            type="datetime"
            placeholder="选择截止时间"
            value-format="yyyy-MM-dd HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="协作模式">
          <el-input value="提交版" disabled />
        </el-form-item>
        <el-form-item label="协作者" prop="selectedUserIds">
          <el-select
            v-model="createForm.selectedUserIds"
            multiple
            filterable
            placeholder="请选择协作者"
            style="width: 100%"
            @change="handleCandidatesChange"
          >
            <el-option
              v-for="item in candidateOptions"
              :key="item.userId"
              :label="candidateLabel(item)"
              :value="item.userId"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="createForm.selectedUserIds.length > 0" label="区块分配">
          <div v-if="sectionOptions.length === 0" class="section-hint">
            所选文档暂无区块，请先在文档管理中添加区块
          </div>
          <div v-for="userId in createForm.selectedUserIds" :key="userId" class="assign-block">
            <div class="assign-user">{{ userDisplayName(userId) }}</div>
            <el-checkbox-group v-model="createForm.userSections[userId]">
              <el-checkbox
                v-for="secId in sectionOptions"
                :key="secId"
                :label="secId"
              >{{ secId }}</el-checkbox>
            </el-checkbox-group>
          </div>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="createForm.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitCreate">确 定</el-button>
        <el-button @click="cancelCreate">取 消</el-button>
      </div>
    </el-dialog>

    <el-drawer
      :title="drawerTitle"
      :visible.sync="detailOpen"
      direction="rtl"
      size="720px"
      append-to-body
      @close="closeDetail"
    >
      <div v-loading="detailLoading" class="drawer-body">
        <template v-if="detail">
          <el-descriptions :column="2" border size="small" class="mb16">
            <el-descriptions-item label="任务标题">{{ detail.task.title }}</el-descriptions-item>
            <el-descriptions-item label="文档标题">{{ detail.docTitle || '-' }}</el-descriptions-item>
            <el-descriptions-item label="协作模式">{{ modeLabel(detail.task.mode) }}</el-descriptions-item>
            <el-descriptions-item label="任务状态">{{ taskStatusLabel(detail.task.taskStatus) }}</el-descriptions-item>
            <el-descriptions-item label="截止时间">{{ parseTime(detail.task.deadlineAt) }}</el-descriptions-item>
            <el-descriptions-item label="备注">{{ detail.task.remark || '-' }}</el-descriptions-item>
          </el-descriptions>

          <div class="section-title">参与指派</div>
          <el-table :data="detail.assignments" size="small" border class="mb16">
            <el-table-column label="协作者" align="center" min-width="120">
              <template slot-scope="scope">
                <span>{{ userDisplayName(scope.row.userId) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="区块范围" align="center" min-width="140">
              <template slot-scope="scope">
                <span>{{ formatScope(scope.row.scopeJson) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="提交状态" align="center" width="110">
              <template slot-scope="scope">
                <span>{{ submitStatusLabel(scope.row.submitStatus) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="提交时间" align="center" width="160">
              <template slot-scope="scope">
                <span>{{ scope.row.submittedAt ? parseTime(scope.row.submittedAt) : '-' }}</span>
              </template>
            </el-table-column>
          </el-table>

          <template v-if="manageMode">
            <div class="section-title">延长截止时间</div>
            <div class="deadline-row mb16">
              <el-date-picker
                v-model="extendDeadlineAt"
                type="datetime"
                placeholder="选择新截止时间"
                value-format="yyyy-MM-dd HH:mm:ss"
                size="small"
              />
              <el-button
                type="primary"
                size="small"
                icon="el-icon-time"
                @click="handleExtendDeadline"
                v-hasPermi="['collab:task:edit']"
              >延长截止</el-button>
            </div>

            <div class="section-title">未提交名单</div>
            <el-table :data="unsubmittedList" size="small" border class="mb16">
              <el-table-column label="协作者" align="center" min-width="120">
                <template slot-scope="scope">
                  <span>{{ userDisplayName(scope.row.userId) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="提交状态" align="center" width="110">
                <template slot-scope="scope">
                  <span>{{ submitStatusLabel(scope.row.submitStatus) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" align="center" width="120">
                <template slot-scope="scope">
                  <el-button
                    size="mini"
                    type="text"
                    icon="el-icon-refresh-left"
                    @click="handleAllowResubmit(scope.row)"
                    v-hasPermi="['collab:task:edit']"
                  >允许补交</el-button>
                </template>
              </el-table-column>
            </el-table>

            <div class="section-title">汇总操作</div>
            <el-button
              type="warning"
              plain
              size="small"
              icon="el-icon-document-copy"
              @click="handleMerge"
              v-hasPermi="['collab:task:edit']"
            >生成汇总版</el-button>
          </template>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import { listDoc, getDoc } from '@/api/collab/doc'
import {
  listTask,
  getTask,
  addTask,
  extendDeadline,
  allowResubmit,
  listUnsubmitted,
  mergeTask
} from '@/api/collab/task'
import { listCandidates } from '@/api/collab/user'
import { listSectionIds, parseScopeJson } from '@/utils/collab/sectionScope'

const TASK_STATUS_OPTIONS = [
  { value: 'draft', label: '草稿' },
  { value: 'open', label: '进行中' },
  { value: 'closed', label: '已关闭' }
]

const SUBMIT_STATUS_OPTIONS = [
  { value: 'editing', label: '编辑中' },
  { value: 'submitted', label: '已提交' },
  { value: 'overdue', label: '已逾期' },
  { value: 'resubmit_allowed', label: '允许补交' }
]

export default {
  name: 'CollabTask',
  data() {
    return {
      loading: true,
      showSearch: true,
      taskList: [],
      docOptions: [],
      candidateOptions: [],
      sectionOptions: [],
      createOpen: false,
      detailOpen: false,
      detailLoading: false,
      manageMode: false,
      drawerTitle: '任务详情',
      detail: null,
      currentTaskId: null,
      extendDeadlineAt: '',
      unsubmittedList: [],
      queryParams: {
        title: undefined,
        taskStatus: undefined
      },
      createForm: {
        docId: undefined,
        title: undefined,
        deadlineAt: undefined,
        remark: undefined,
        selectedUserIds: [],
        userSections: {}
      },
      createRules: {
        docId: [
          { required: true, message: '请选择关联文档', trigger: 'change' }
        ],
        title: [
          { required: true, message: '任务标题不能为空', trigger: 'blur' }
        ],
        deadlineAt: [
          { required: true, message: '请选择截止时间', trigger: 'change' }
        ],
        selectedUserIds: [
          { type: 'array', required: true, min: 1, message: '请至少选择一名协作者', trigger: 'change' }
        ]
      },
      taskStatusOptions: TASK_STATUS_OPTIONS
    }
  },
  created() {
    this.loadDocOptions()
    this.loadCandidates()
    this.getList()
  },
  methods: {
    taskStatusLabel(status) {
      const item = TASK_STATUS_OPTIONS.find(opt => opt.value === status)
      return item ? item.label : status || '-'
    },
    submitStatusLabel(status) {
      const item = SUBMIT_STATUS_OPTIONS.find(opt => opt.value === status)
      return item ? item.label : status || '-'
    },
    modeLabel(mode) {
      return mode === 'submit' ? '提交版' : (mode || '-')
    },
    candidateLabel(item) {
      const name = item.nickName || item.userName
      return item.deptName ? `${name}（${item.deptName}）` : name
    },
    userDisplayName(userId) {
      const user = this.candidateOptions.find(c => c.userId === userId)
      if (user) {
        return this.candidateLabel(user)
      }
      return userId != null ? String(userId) : '-'
    },
    formatScope(scopeJson) {
      const ids = parseScopeJson(scopeJson)
      return ids.length > 0 ? ids.join(', ') : '-'
    },
    loadDocOptions() {
      return listDoc({}).then(response => {
        if (response.code === 200) {
          this.docOptions = response.data || []
        }
      })
    },
    loadCandidates() {
      return listCandidates().then(response => {
        if (response.code === 200) {
          this.candidateOptions = response.data || []
        }
      })
    },
    docTitle(docId) {
      const doc = this.docOptions.find(d => d.docId === docId)
      return doc ? doc.title : (docId || '-')
    },
    enrichRows(list) {
      return list.map(t => ({
        ...t,
        docTitle: this.docTitle(t.docId)
      }))
    },
    getList() {
      this.loading = true
      listTask(this.queryParams).then(response => {
        if (response.code !== 200) {
          this.taskList = []
          this.$modal.msgError(response.msg || '查询失败')
          return
        }
        this.taskList = this.enrichRows(response.data || [])
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
    resetCreateForm() {
      this.createForm = {
        docId: undefined,
        title: undefined,
        deadlineAt: undefined,
        remark: undefined,
        selectedUserIds: [],
        userSections: {}
      }
      this.sectionOptions = []
      this.resetForm('createForm')
    },
    handleAdd() {
      this.resetCreateForm()
      Promise.all([this.loadDocOptions(), this.loadCandidates()]).then(() => {
        this.createOpen = true
      })
    },
    cancelCreate() {
      this.createOpen = false
      this.resetCreateForm()
    },
    handleDocChange(docId) {
      if (!docId) {
        this.sectionOptions = []
        this.createForm.userSections = {}
        return
      }
      getDoc(docId).then(response => {
        if (response.code !== 200) {
          this.$modal.msgError(response.msg || '加载文档失败')
          this.sectionOptions = []
          return
        }
        this.sectionOptions = listSectionIds(response.data.contentHtml || '')
        const next = {}
        this.createForm.selectedUserIds.forEach(userId => {
          next[userId] = this.createForm.userSections[userId] || []
        })
        this.createForm.userSections = next
      })
    },
    handleCandidatesChange(userIds) {
      const next = {}
      userIds.forEach(userId => {
        next[userId] = this.createForm.userSections[userId] || []
      })
      this.createForm.userSections = next
    },
    buildAssignments() {
      const assignments = []
      for (const userId of this.createForm.selectedUserIds) {
        const sectionIds = this.createForm.userSections[userId] || []
        if (sectionIds.length === 0) {
          return null
        }
        assignments.push({
          userId,
          scopeType: 'section',
          scopeJson: JSON.stringify({ sectionIds })
        })
      }
      return assignments
    },
    submitCreate() {
      this.$refs['createForm'].validate(valid => {
        if (!valid) {
          return
        }
        const assignments = this.buildAssignments()
        if (!assignments) {
          this.$modal.msgError('请为每位协作者至少选择一个区块')
          return
        }
        if (this.sectionOptions.length === 0) {
          this.$modal.msgError('所选文档暂无区块，无法创建任务')
          return
        }
        const payload = {
          docId: this.createForm.docId,
          title: this.createForm.title,
          mode: 'submit',
          deadlineAt: this.createForm.deadlineAt,
          remark: this.createForm.remark || '',
          assignments
        }
        addTask(payload).then(response => {
          if (response.code !== 200) {
            this.$modal.msgError(response.msg || '创建失败')
            return
          }
          this.$modal.msgSuccess('创建成功')
          this.createOpen = false
          this.resetCreateForm()
          this.getList()
        })
      })
    },
    openDetail(row, manage) {
      this.currentTaskId = row.taskId
      this.manageMode = manage
      this.drawerTitle = manage ? '任务管理' : '任务详情'
      this.detailOpen = true
      this.detailLoading = true
      this.detail = null
      this.unsubmittedList = []
      this.extendDeadlineAt = row.deadlineAt || ''

      Promise.all([
        getTask(row.taskId),
        manage ? listUnsubmitted(row.taskId) : Promise.resolve({ code: 200, data: [] }),
        this.loadCandidates()
      ]).then(([detailRes, unsubmittedRes]) => {
        if (detailRes.code !== 200) {
          this.$modal.msgError(detailRes.msg || '加载详情失败')
          this.detailOpen = false
          return
        }
        this.detail = detailRes.data
        if (this.detail && this.detail.task) {
          this.extendDeadlineAt = this.detail.task.deadlineAt || ''
        }
        if (unsubmittedRes.code === 200) {
          this.unsubmittedList = unsubmittedRes.data || []
        }
      }).finally(() => {
        this.detailLoading = false
      })
    },
    handleDetail(row) {
      this.openDetail(row, false)
    },
    handleManage(row) {
      this.openDetail(row, true)
    },
    closeDetail() {
      this.detail = null
      this.currentTaskId = null
      this.manageMode = false
      this.unsubmittedList = []
      this.extendDeadlineAt = ''
    },
    refreshDetail() {
      if (!this.currentTaskId) {
        return
      }
      this.detailLoading = true
      const requests = [getTask(this.currentTaskId)]
      if (this.manageMode) {
        requests.push(listUnsubmitted(this.currentTaskId))
      }
      Promise.all(requests).then(([detailRes, unsubmittedRes]) => {
        if (detailRes.code === 200) {
          this.detail = detailRes.data
          if (this.detail && this.detail.task) {
            this.extendDeadlineAt = this.detail.task.deadlineAt || ''
          }
        }
        if (unsubmittedRes && unsubmittedRes.code === 200) {
          this.unsubmittedList = unsubmittedRes.data || []
        }
        this.getList()
      }).finally(() => {
        this.detailLoading = false
      })
    },
    handleExtendDeadline() {
      if (!this.extendDeadlineAt) {
        this.$modal.msgError('请选择新的截止时间')
        return
      }
      extendDeadline(this.currentTaskId, { deadlineAt: this.extendDeadlineAt }).then(response => {
        if (response.code !== 200) {
          this.$modal.msgError(response.msg || '延长失败')
          return
        }
        this.$modal.msgSuccess('截止时间已更新')
        this.refreshDetail()
      })
    },
    handleAllowResubmit(row) {
      allowResubmit(this.currentTaskId, { assignmentId: row.assignmentId }).then(response => {
        if (response.code !== 200) {
          this.$modal.msgError(response.msg || '操作失败')
          return
        }
        this.$modal.msgSuccess('已允许补交')
        this.refreshDetail()
      })
    },
    handleMerge() {
      this.$confirm('确认将各协作者提交内容合并到主文档？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        return mergeTask(this.currentTaskId)
      }).then(response => {
        if (response.code !== 200) {
          this.$modal.msgError(response.msg || '汇总失败')
          return
        }
        this.$modal.msgSuccess('汇总版已生成')
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.section-hint {
  color: #909399;
  font-size: 13px;
}
.assign-block {
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
}
.assign-user {
  font-weight: 600;
  margin-bottom: 6px;
}
.drawer-body {
  padding: 0 4px 24px;
}
.section-title {
  font-weight: 600;
  margin: 16px 0 8px;
}
.mb16 {
  margin-bottom: 16px;
}
.deadline-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
