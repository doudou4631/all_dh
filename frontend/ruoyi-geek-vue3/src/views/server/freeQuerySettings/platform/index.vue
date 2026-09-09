<template>
  <div class="app-container platform-page">
    <div class="page-toolbar">
      <div>
        <h2>免费平台管理</h2>
        <span class="platform-count">{{ platforms.length }} 个数据源</span>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="getList">刷新</el-button>
    </div>

    <div class="platform-table" v-loading="loading">
      <div class="platform-table__head">
        <span>优先级</span>
        <span>平台</span>
        <span>数据来源</span>
        <span>状态</span>
        <span>操作</span>
      </div>

      <div
        v-for="(item, index) in platforms"
        :key="item.id"
        class="platform-row"
        :class="{ 'platform-row--dragging': dragIndex === index, 'platform-row--disabled': item.status === '1' }"
        @dragover.prevent
        @drop="handleDrop(index)"
      >
        <div class="priority-cell">
          <button
            class="drag-handle"
            type="button"
            draggable="true"
            title="拖动调整优先级"
            @dragstart="handleDragStart($event, index)"
            @dragend="dragIndex = null"
            v-hasPermi="['freeQuery:platform:sort']"
          >
            <el-icon><Rank /></el-icon>
          </button>
          <strong>{{ index + 1 }}</strong>
        </div>

        <div class="platform-cell">
          <img :src="iconUrl(item.icon)" :alt="item.platformName" class="platform-icon" />
          <div>
            <strong>{{ item.platformName }}</strong>
            <el-tag v-if="item.globalStatus === '1'" type="danger" effect="plain" size="small">全局停用</el-tag>
          </div>
        </div>

        <div class="description-cell">{{ item.sourceDescription || '-' }}</div>

        <div class="status-cell">
          <el-switch
            v-model="item.status"
            active-value="0"
            inactive-value="1"
            inline-prompt
            active-text="启用"
            inactive-text="停用"
            :disabled="item.globalStatus === '1'"
            @change="handleStatusChange(item)"
            v-hasPermi="['freeQuery:platform:status']"
          />
        </div>

        <div class="action-cell">
          <el-tooltip content="上移" placement="top">
            <el-button :icon="ArrowUp" circle text :disabled="index === 0" @click="movePlatform(index, -1)"
              v-hasPermi="['freeQuery:platform:sort']" />
          </el-tooltip>
          <el-tooltip content="下移" placement="top">
            <el-button :icon="ArrowDown" circle text :disabled="index === platforms.length - 1" @click="movePlatform(index, 1)"
              v-hasPermi="['freeQuery:platform:sort']" />
          </el-tooltip>
          <el-button link type="primary" :icon="Edit" @click="openEdit(item)"
            v-hasPermi="['freeQuery:platform:edit']">编辑</el-button>
        </div>
      </div>

      <el-empty v-if="!loading && platforms.length === 0" description="暂无平台" />
    </div>

    <el-dialog v-model="editOpen" title="编辑平台展示" width="520px" append-to-body destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="rules" label-width="92px">
        <el-form-item label="平台名称">
          <el-input :model-value="editForm.platformName" disabled />
        </el-form-item>
        <el-form-item label="平台图标" prop="icon">
          <el-select v-model="editForm.icon" style="width: 100%">
            <el-option v-for="option in iconOptions" :key="option.value" :label="option.label" :value="option.value">
              <div class="icon-option">
                <img :src="option.url" :alt="option.label" />
                <span>{{ option.label }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="数据来源" prop="sourceDescription">
          <el-input
            v-model="editForm.sourceDescription"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请输入数据来源说明"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="FreeQueryPlatform">
import { ArrowDown, ArrowUp, Edit, Rank, Refresh } from '@element-plus/icons-vue'
import {
  changeFreeQueryPlatformStatus,
  listFreeQueryPlatforms,
  sortFreeQueryPlatforms,
  updateFreeQueryPlatformDisplay
} from '@/api/server/freeQueryPlatform'
import tdxIcon from '@/assets/images/free-query-platform/tdx.png'
import sllIcon from '@/assets/images/free-query-platform/sll.png'
import txIcon from '@/assets/images/free-query-platform/tx.png'
import bdIcon from '@/assets/images/free-query-platform/bd.png'
import ltIcon from '@/assets/images/free-query-platform/lt.png'
import ydhighIcon from '@/assets/images/free-query-platform/ydhigh.png'
import dhbIcon from '@/assets/images/free-query-platform/dhb.png'
import sghmtIcon from '@/assets/images/free-query-platform/sghmt.png'
import xmIcon from '@/assets/images/free-query-platform/xm.png'
import fallbackIcon from '@/assets/images/free-query-platform/114.png'

const { proxy } = getCurrentInstance()
const loading = ref(false)
const saving = ref(false)
const editOpen = ref(false)
const dragIndex = ref(null)
const platforms = ref([])
const editForm = reactive({ id: null, platformName: '', icon: '', sourceDescription: '' })

const iconMap = {
  'tdx.png': tdxIcon,
  'sll.png': sllIcon,
  'tx.png': txIcon,
  'bd.png': bdIcon,
  'lt.png': ltIcon,
  'ydhigh.png': ydhighIcon,
  'dhb.png': dhbIcon,
  'sghmt.png': sghmtIcon,
  'xm.png': xmIcon,
  '114.png': fallbackIcon
}

const iconOptions = [
  { label: '泰迪熊', value: 'tdx.png' },
  { label: '360', value: 'sll.png' },
  { label: '腾讯', value: 'tx.png' },
  { label: '百度', value: 'bd.png' },
  { label: '联通', value: 'lt.png' },
  { label: '移动高频', value: 'ydhigh.png' },
  { label: '电话邦', value: 'dhb.png' },
  { label: '搜狗', value: 'sghmt.png' },
  { label: '小米', value: 'xm.png' },
  { label: '通用', value: '114.png' }
].map(item => ({ ...item, url: iconMap[item.value] }))

const rules = {
  icon: [{ required: true, message: '请选择平台图标', trigger: 'change' }],
  sourceDescription: [{ required: true, message: '请输入数据来源说明', trigger: 'blur' }]
}

async function getList() {
  loading.value = true
  try {
    const response = await listFreeQueryPlatforms()
    platforms.value = response.data || []
  } finally {
    loading.value = false
  }
}

function iconUrl(icon) {
  return iconMap[icon] || fallbackIcon
}

function handleDragStart(event, index) {
  dragIndex.value = index
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', String(index))
}

function handleDrop(targetIndex) {
  const sourceIndex = dragIndex.value
  dragIndex.value = null
  if (sourceIndex === null || sourceIndex === targetIndex) return
  reorder(sourceIndex, targetIndex)
}

function movePlatform(index, offset) {
  const target = index + offset
  if (target < 0 || target >= platforms.value.length) return
  reorder(index, target)
}

async function reorder(sourceIndex, targetIndex) {
  const previous = [...platforms.value]
  const next = [...platforms.value]
  const [moved] = next.splice(sourceIndex, 1)
  next.splice(targetIndex, 0, moved)
  platforms.value = next
  try {
    await sortFreeQueryPlatforms(next.map(item => item.id))
    platforms.value.forEach((item, index) => { item.priority = index + 1 })
    proxy.$modal.msgSuccess('优先级已更新')
  } catch (error) {
    platforms.value = previous
    throw error
  }
}

async function handleStatusChange(item) {
  try {
    await changeFreeQueryPlatformStatus(item.id, item.status)
    proxy.$modal.msgSuccess(item.status === '0' ? '平台已启用' : '平台已停用')
  } catch (error) {
    item.status = item.status === '0' ? '1' : '0'
    throw error
  }
}

function openEdit(item) {
  Object.assign(editForm, {
    id: item.id,
    platformName: item.platformName,
    icon: item.icon || '114.png',
    sourceDescription: item.sourceDescription || ''
  })
  editOpen.value = true
  nextTick(() => proxy.$refs.editFormRef?.clearValidate())
}

function submitEdit() {
  proxy.$refs.editFormRef.validate(async valid => {
    if (!valid) return
    saving.value = true
    try {
      await updateFreeQueryPlatformDisplay({
        id: editForm.id,
        icon: editForm.icon,
        sourceDescription: editForm.sourceDescription
      })
      const item = platforms.value.find(platform => platform.id === editForm.id)
      if (item) {
        item.icon = editForm.icon
        item.sourceDescription = editForm.sourceDescription
      }
      editOpen.value = false
      proxy.$modal.msgSuccess('平台展示已更新')
    } finally {
      saving.value = false
    }
  })
}

getList()
</script>

<style scoped>
.page-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-toolbar h2 {
  margin: 0 0 4px;
  font-size: 20px;
  line-height: 28px;
  letter-spacing: 0;
}

.platform-count {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.platform-table {
  min-height: 180px;
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-bg-color);
}

.platform-table__head,
.platform-row {
  display: grid;
  grid-template-columns: 110px minmax(190px, 1.1fr) minmax(240px, 2fr) 150px 170px;
  align-items: center;
  column-gap: 16px;
  min-width: 860px;
  padding: 0 18px;
  box-sizing: border-box;
}

.platform-table__head {
  height: 48px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
  font-size: 13px;
  font-weight: 600;
}

.platform-row {
  min-height: 78px;
  border-top: 1px solid var(--el-border-color-lighter);
  transition: background-color .16s, opacity .16s;
}

.platform-row:hover {
  background: var(--el-fill-color-lighter);
}

.platform-row--dragging {
  opacity: .45;
  background: var(--el-color-primary-light-9);
}

.platform-row--disabled .platform-icon {
  filter: grayscale(1);
  opacity: .55;
}

.priority-cell,
.platform-cell,
.action-cell {
  display: flex;
  align-items: center;
}

.priority-cell {
  gap: 12px;
}

.priority-cell strong {
  width: 24px;
  color: var(--el-text-color-regular);
  text-align: center;
}

.drag-handle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--el-text-color-secondary);
  cursor: grab;
  font-size: 18px;
}

.drag-handle:active { cursor: grabbing; }

.platform-cell {
  gap: 12px;
  min-width: 0;
}

.platform-cell > div {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

.platform-icon {
  flex: 0 0 42px;
  width: 42px;
  height: 42px;
  object-fit: contain;
}

.description-cell {
  min-width: 0;
  color: var(--el-text-color-regular);
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.status-cell {
  min-width: 80px;
}

.action-cell {
  justify-content: flex-end;
  white-space: nowrap;
}

.icon-option {
  display: flex;
  align-items: center;
  gap: 10px;
}

.icon-option img {
  width: 26px;
  height: 26px;
  object-fit: contain;
}

@media (max-width: 920px) {
  .platform-table { overflow-x: auto; }
}
</style>
