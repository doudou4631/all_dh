<template> 
  <div class="app-container batch-record-page">
      <el-card shadow="never" body-class="search-card">
        <el-form ref="queryRef" :model="taskHistoryQueryParams" :inline="true" v-show="showSearch" label-width="72px">
          <el-form-item label="日期">
            <el-date-picker
              v-model="dateRange"
              value-format="YYYY-MM-DD"
              type="daterange"
              range-separator="-"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              style="width: 240px"
            />
          </el-form-item>
          <el-form-item label="关键词" prop="taskId">
            <el-input
              v-model="taskHistoryQueryParams.keyword"
              placeholder="查询批次、手机号等"
              clearable
              style="width: 220px"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="never" class="mt10 batch-record-table-card">
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete">删除</el-button>
          </el-col>
          <!-- <el-col :span="1.5">
            <el-button type="warning" plain icon="Download" @click="handleExportTaskHistory" :disabled="taskHistoryList.length === 0">
              导出
            </el-button>
          </el-col> -->
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getTaskHistoryList" />
        </el-row>

        <el-table
          v-loading="taskHistoryLoading"
          :data="taskHistoryList"
          border
          stripe
          size="small"
          class="task-manage-table"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="48" align="center" />
          <el-table-column label="查询标识" align="center" prop="id" min-width="80" show-overflow-tooltip />
          <el-table-column label="查询批次" align="center" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="task-id-ellipsis">{{ row.taskId || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="上传条数" min-width="80" align="center">
            <template #default="{ row }">
              {{ formatTaskTotal(row) }}
            </template>
          </el-table-column>
          <el-table-column label="计费条数" min-width="80" align="center">
            <template #default="{ row }">
              {{ formatTaskBillingCount(row) }}
            </template>
          </el-table-column>
          <el-table-column label="进度" min-width="100" align="center">
            <template #default="{ row }">
              <el-tooltip :content="`${computeTaskProgressPercent(row)}%`" placement="top">
                <div class="task-progress-cell task-progress-cell--fluid">
                  <el-progress
                    :percentage="computeTaskProgressPercent(row)"
                    :status="row.status === 'COMPLETED' ? 'success' : undefined"
                    :stroke-width="5"
                    :show-text="false"
                  />
                </div>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="状态" min-width="96" align="center">
            <template #default="{ row }">
              <el-tag :type="getTaskHistoryStatusType(row.status)" size="small">
                {{ getTaskHistoryStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="成功" min-width="72" align="center">
            <template #default="{ row }">
              {{ formatTaskSuccessCount(row) }}
            </template>
          </el-table-column>
          <el-table-column label="失败" min-width="72" align="center">
            <template #default="{ row }">
              {{ formatTaskFailCount(row) }}
            </template>
          </el-table-column>
          <el-table-column label="创建时间" min-width="158" align="center">
            <template #default="{ row }">
              {{ formatTaskCreateTime(row) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="132" align="center" class-name="small-padding fixed-width">
            <template #default="{ row }">
              <el-button
                v-if="row.taskId"
                link
                type="primary"
                @click="openTaskDetailByRow(row)"
              >
                结果
              </el-button>
              <el-button
                v-if="row.taskId && row.status === 'COMPLETED'"
                link
                type="primary"
                @click="exportTaskResults(row)"
              >
                下载
              </el-button>
              <span v-if="!row.taskId" class="text-muted">—</span>
            </template>
          </el-table-column>
        </el-table>

        <pagination
          v-show="taskHistoryTotal > 0"
          :total="taskHistoryTotal"
          v-model:page="taskHistoryQueryParams.pageNum"
          v-model:limit="taskHistoryQueryParams.pageSize"
          @pagination="getTaskHistoryList"
          :page-sizes="[10, 20, 50]"
          :default-page-size="10"
        />
      </el-card>

    <!-- 任务详情（按 taskId 拉取关联结果列表） -->
    <el-dialog
      v-model="taskDetailDialogVisible"
      :title="taskDetailTitle"
      width="75%"
      append-to-body
      destroy-on-close
      @closed="resetTaskDetailDialog"
    >
      <div v-loading="taskDetailLoading" class="task-detail-body">
        <div class="results-summary" v-if="taskDetailTaskId">
          <div class="task-id-display">任务ID: #{{ taskDetailTaskId }}</div>
        </div>
        <div class="results-table-container" v-if="taskDetailGroupedResults.length">
          <el-table
            :data="taskDetailGroupedResults"
            border
            stripe
            size="small"
            class="task-results-table"
            max-height="calc(100vh - 200px)"
          >
            <el-table-column label="号码" min-width="112" align="center" show-overflow-tooltip>
              <template #default="{ row }">
                {{ formatPhoneDisplay(row.phoneNumber) }}
              </template>
            </el-table-column>
            <el-table-column
              v-for="platform in availablePlatforms"
              :key="platform.id"
              :label="platform.platformName"
              :min-width="76"
              align="center"
              show-overflow-tooltip
            >
              <template #default="scope">
                {{ getPlatformStatus(scope.row, platform.platformName) }}
              </template>
            </el-table-column>
          </el-table>
        </div>
        <el-empty v-else-if="!taskDetailLoading" description="暂无明细数据" />
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="taskDetailDialogVisible = false">关闭</el-button>
          <el-button
            v-if="taskDetailTaskId && taskDetailQueryResults.length"
            type="primary"
            @click="exportTaskResults({ taskId: taskDetailTaskId })"
          >
            导出查询结果
          </el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup name="BatchApi">
import useUserStore from '@/store/modules/user'
import { getTemplate } from "@/api/server/template";
import { listPlatformConfigUser } from "@/api/server/platformConfig";
import { getBatchTaskResults } from "@/api/server/asyncBatchApi";
import { listBatchTaskRecord, exportBatchTaskRecord, delBatchTaskRecord } from "@/api/server/batchTaskRecord";
import { useRoute, useRouter } from "vue-router";
import { saveAs } from "file-saver";
import * as XLSX from "xlsx-js-style";

const { proxy } = getCurrentInstance();
const userStore = useUserStore();
const route = useRoute();
const router = useRouter();

const availablePlatforms = ref([]);

// 任务列表（首页主表，listBatchTaskRecord）
const taskHistoryList = ref([]);
const taskHistoryLoading = ref(false);
const taskHistoryTotal = ref(0);
const showSearch = ref(true);
const dateRange = ref([]);
const ids = ref([]);
const multiple = ref(true);
const taskHistoryQueryParams = ref({
  pageNum: 1,
  pageSize: 10,
  keyword: undefined,
  params: {}
});

// 任务详情弹窗（按 taskId 查询关联结果）
const taskDetailDialogVisible = ref(false);
const taskDetailTaskId = ref(null);
const taskDetailLoading = ref(false);
const taskDetailQueryResults = ref([]);

const taskDetailTitle = computed(() =>
  taskDetailTaskId.value ? `任务详情 #${taskDetailTaskId.value}` : '任务详情'
);

const taskDetailGroupedResults = computed(() => {
  if (!taskDetailQueryResults.value.length) return [];
  const grouped = {};
  taskDetailQueryResults.value.forEach(result => {
    if (!grouped[result.phoneNumber]) {
      grouped[result.phoneNumber] = {};
    }
    // 兼容：同一平台可能存在「中文平台名」与「platform code」不一致的情况
    // - 后端常返回 data.platform 作为 code（如 tengxun/baidu/sanliuling/360）
    // - 前端表头用 platform.platformName（中文），因此需要把结果同时写入多个 key，渲染时再兜底读取
    const dataPlatform = result?.data?.platform ? String(result.data.platform) : '';
    const namePlatform = result?.platformName ? String(result.platformName) : '';
    const derivedCode = namePlatform ? getPlatformCode(namePlatform) : '';

    const ensure = (k) => {
      if (!k) return;
      grouped[result.phoneNumber][k] = result.data;
    };

    if (result.data) {
      ensure(dataPlatform);
      ensure(derivedCode);
      // 允许用平台名本身作为 key（极端情况下后端把 platform 存中文名）
      ensure(namePlatform);
      // 360 平台历史兼容：有的返回 360，有的返回 sanliuling
      if (dataPlatform === '360') ensure('sanliuling');
      if (dataPlatform === 'sanliuling') ensure('360');
      if (derivedCode === '360') ensure('sanliuling');
      if (derivedCode === 'sanliuling') ensure('360');
    }
  });
  return Object.keys(grouped).map(phoneNumber => ({
    phoneNumber,
    ...grouped[phoneNumber]
  }));
});

async function fetchUserTemplateInfo() {
  if (!userStore.relTemplate) {
    return;
  }
  try {
    const response = await getTemplate(userStore.relTemplate);
    if (response.data && response.data.templateInfo) {
      try {
        const templateIds = JSON.parse(response.data.templateInfo);
        if (Array.isArray(templateIds) && templateIds.length > 0) {
          await fetchPlatformsByTemplateIds(templateIds);
        }
      } catch (parseError) {
        console.error('解析模板信息失败:', parseError);
        proxy.$modal.msgError('模板信息格式错误');
      }
    }
  } catch (error) {
    console.error('获取模板信息失败:', error);
    proxy.$modal.msgError('获取模板信息失败');
  }
}

async function fetchPlatformsByTemplateIds(templateIds) {
  try {
    const response = await listPlatformConfigUser({
      ids: templateIds
    });
    if (response.rows && Array.isArray(response.rows)) {
      const validPlatforms = response.rows.filter(platform => platform.status === '0');
      availablePlatforms.value = validPlatforms;
      if (validPlatforms.length === 0) {
        proxy.$modal.msgWarning('当前模板下暂无可用的API平台');
      }
    } else {
      availablePlatforms.value = [];
    }
  } catch (error) {
    console.error('获取平台配置失败:', error);
    proxy.$modal.msgError('获取平台配置失败');
  }
}

function getPlatformCode(platformName) {
  const platformMap = {
    '腾讯': 'tengxun',
    '腾讯平台': 'tengxun',
    '百度': 'baidu',
    '百度平台': 'baidu',
    '360': 'sanliuling',
    '360平台': '360',
    '360平台查询': '360',
    '360手机卫士': 'sanliuling',
    '电话邦': 'dianhuabang',
    '小米电话': 'xiaomi',
    '小米手机': 'xiaomi',
    '联通安全管家': 'ltgj',
    '联通': 'ltgj',
    '联通管家': 'ltgj',
    '中国联通': 'ltgj',
    '搜狗': 'sghmt',
    '搜狗号码通': 'sghmt',
    '移动高频': 'yidonggaopin',
    '泰迪熊': 'taidixiong',
    '泰迪熊平台': 'taidixiong'
  };
  return platformMap[platformName] || platformName.toLowerCase();
}

/**
 * 任务详情结果展示：不出现裸 yes/no；含 yes 时只展示 yes/yes- 之后内容；含 no 时展示「-」
 * 凡文案中含「无标记」均展示为「-」（与导出 Excel 一致）
 */
function formatTaskDetailStatusDisplay(st) {
  if (st == null || String(st).trim() === '') {
    return '-';
  }
  const s = String(st).trim();
  if (s.includes('无标记')) {
    return '-';
  }
  const lower = s.toLowerCase();
  if (lower.includes('yes')) {
    const idx = lower.indexOf('yes');
    let tail = s.slice(idx + 3);
    if (tail.startsWith('-')) {
      tail = tail.slice(1);
    }
    tail = tail.trim();
    return tail || '有标记';
  }
  if (lower.includes('no')) {
    return '-';
  }
  if (s === '无') {
    return '-';
  }
  switch (s) {
    case 'risk':
      return '风险';
    case 'normal':
      return '正常';
    case 'unknown':
      return '未知';
    default:
      return s;
  }
}

function getPlatformStatus(row, platformName) {
  const platformCode = getPlatformCode(platformName);
  const candidates = [
    platformCode,
    platformName,
    platformCode === '360' ? 'sanliuling' : null,
    platformCode === 'sanliuling' ? '360' : null
  ].filter(Boolean);

  for (const key of candidates) {
    const platformData = row?.[key];
    if (platformData && platformData.status != null && platformData.status !== '') {
      return formatTaskDetailStatusDisplay(platformData.status);
    }
  }
  return '-';
}

function normalizeBatchResultItem(result) {
  let responseData = null;
  let isSuccess = false;
  let errorMessage = null;
  if (result.data) {
    if (typeof result.data === 'string') {
      try {
        responseData = JSON.parse(result.data);
      } catch (e) {
        responseData = result.data;
      }
    } else {
      responseData = result.data;
    }
    if (responseData && typeof responseData === 'object') {
      if (responseData.success === true) {
        isSuccess = true;
      } else if (responseData.success === false) {
        isSuccess = false;
        errorMessage = responseData.message || '查询失败';
      }
    }
  }
  const normalizedData =
    responseData && responseData.platformResults && Array.isArray(responseData.platformResults)
      ? responseData.platformResults[0]
      : (isSuccess ? responseData : null);
  return {
    phoneNumber: result.phoneNumber,
    platformId: result.platformId,
    platformName: result.platformName,
    success: isSuccess,
    data: normalizedData,
    error: errorMessage || result.error,
    responseTime: result.responseTime || 0,
    timestamp: result.timestamp || new Date()
  };
}

function parseTime(time, pattern) {
  if (!time) return '';
  const date = new Date(time);
  const format = pattern || '{y}-{m}-{d} {h}:{i}:{s}';
  const formatObj = {
    y: date.getFullYear(),
    m: date.getMonth() + 1,
    d: date.getDate(),
    h: date.getHours(),
    i: date.getMinutes(),
    s: date.getSeconds(),
    a: date.getDay()
  };
  const time_str = format.replace(/{(y|m|d|h|i|s|a)+}/g, (result, key) => {
    let value = formatObj[key];
    if (key === 'a') return ['一', '二', '三', '四', '五', '六', '日'][value - 1];
    if (result.length > 0 && value < 10) {
      value = '0' + value;
    }
    return value || 0;
  });
  return time_str;
}

/** 展示号码时去掉中间的连字符（固话常见 区号-号码 格式） */
function formatPhoneDisplay(val) {
  if (val == null || val === '') return '—'
  return String(val).replace(/-/g, '')
}

function getTaskHistoryStatusText(status) {
  switch (status) {
    case 'RUNNING':
      return '执行中';
    case 'COMPLETED':
      return '已完成';
    case 'FAILED':
      return '失败';
    case 'CANCELLED':
      return '已取消';
    default:
      return '未知状态';
  }
}

function getTaskHistoryStatusType(status) {
  switch (status) {
    case 'RUNNING':
      return 'warning';
    case 'COMPLETED':
      return 'success';
    case 'FAILED':
      return 'danger';
    case 'CANCELLED':
      return 'info';
    default:
      return 'info';
  }
}

function resolveBatchPagePath() {
  const p = route.path || "";
  if (/batchRecord/i.test(p)) {
    return p.replace(/batchRecord\/?$/i, "batch");
  }
  const i = p.lastIndexOf("/");
  if (i > 0) {
    return `${p.slice(0, i)}/batch`;
  }
  return "/batch";
}

function goBatchNumbers() {
  router.push(resolveBatchPagePath());
}

function goBatchFile() {
  router.push({ path: resolveBatchPagePath(), query: { source: "file" } });
}

function handleQuery() {
  taskHistoryQueryParams.value.pageNum = 1;
  getTaskHistoryList();
}

function resetQuery() {
  dateRange.value = [];
  taskHistoryQueryParams.value.keyword = undefined;
  taskHistoryQueryParams.value.pageNum = 1;
  taskHistoryQueryParams.value.pageSize = 10;
  taskHistoryQueryParams.value.params = {};
  getTaskHistoryList();
}

function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id).filter((id) => id != null && id !== "");
  multiple.value = !ids.value.length;
}

function normalizeDeleteIds(raw) {
  return raw
    .map((id) => Number(id))
    .filter((n) => Number.isInteger(n) && n > 0);
}

function handleDelete() {
  if (!ids.value.length) {
    proxy.$modal.msgWarning("请先选择要删除的记录");
    return;
  }
  const idList = normalizeDeleteIds(ids.value);
  if (!idList.length) {
    proxy.$modal.msgWarning("所选记录主键 id 无效，无法删除（需为数字 Long）");
    return;
  }
  proxy.$modal
    .confirm("是否确认删除选中的任务记录？")
    .then(() => delBatchTaskRecord(idList))
    .then(() => {
      getTaskHistoryList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
}

function formatTaskBillingCount(row) {
  const phoneN = resolveQueriedPhoneCount(row);
  if (phoneN != null) {
    return phoneN;
  }
  const v = row.billCount ?? row.billingCount ?? row.chargeCount ?? row.feeCount;
  if (v != null && v !== "") {
    return v;
  }
  return formatTaskTotal(row);
}

/** 批量记录页仅保留“批量查询页面触发”的任务，排除单次查询记录 */
function isBatchTriggeredRecord(row) {
  if (!row || typeof row !== 'object') return false;

  const toLower = (v) => String(v ?? '').trim().toLowerCase();
  const hasSingleMark = (s) => /single|单次|单条/.test(s);
  const hasBatchMark = (s) => /batch|批量/.test(s);

  // 约定：queryType = 1 为批量，2 为单次
  const queryTypeRaw = row.queryType ?? row.type;
  if (queryTypeRaw != null && String(queryTypeRaw).trim() !== '') {
    const q = String(queryTypeRaw).trim();
    if (q === '1') return true;
    if (q === '2') return false;
  }

  const typeText = toLower(row.queryType ?? row.type ?? row.execType ?? row.requestType);
  if (typeText) {
    if (hasSingleMark(typeText)) return false;
    if (hasBatchMark(typeText)) return true;
  }

  const sourceText = toLower(row.source ?? row.fromPage ?? row.triggerSource ?? row.origin);
  if (sourceText) {
    if (hasSingleMark(sourceText)) return false;
    if (hasBatchMark(sourceText)) return true;
  }

  const tagText = toLower(row.remark ?? row.taskName ?? row.taskDesc ?? row.description);
  if (tagText) {
    if (hasSingleMark(tagText)) return false;
    if (hasBatchMark(tagText)) return true;
  }

  // 兜底：批量任务通常有 taskId；单次记录多为无 taskId 的明细
  return Boolean(String(row.taskId ?? '').trim());
}

// 获取任务列表（首页表格）
async function getTaskHistoryList() {
  taskHistoryLoading.value = true;
  try {
    const response = await listBatchTaskRecord(
      proxy.addDateRange(taskHistoryQueryParams.value, dateRange.value)
    );

    if (response.code === 200) {
      // 批量记录页：仅展示批量查询页面触发的记录，排除单次查询
      const rows = Array.isArray(response.rows) ? response.rows : [];
      const filtered = rows.filter((r) => isBatchTriggeredRecord(r));
      taskHistoryList.value = filtered;
      // 后端 total 可能包含单次记录，这里按当前页过滤结果计数，避免列表与总数明显不一致
      taskHistoryTotal.value = typeof response.total === 'number' ? Math.min(response.total, filtered.length) : filtered.length;
    } else {
      proxy.$modal.msgError(response.msg || '获取任务列表失败');
    }
  } catch (error) {
    console.error('获取任务列表失败:', error);
    proxy.$modal.msgError('获取任务列表失败');
  } finally {
    taskHistoryLoading.value = false;
  }
}

/** 被查询的手机号码数量（与上传/计费/成功失败统计口径一致时优先用此值） */
function resolveQueriedPhoneCount(row) {
  const direct =
    row.phoneCount ??
    row.totalPhoneCount ??
    row.numberCount ??
    row.queryPhoneCount ??
    row.phoneTotal;
  if (direct != null && direct !== '') {
    const n = Number(direct);
    if (Number.isFinite(n) && n >= 0) {
      return n;
    }
  }
  const raw = row.phoneNumbers;
  if (raw == null || raw === '') return null;
  if (Array.isArray(raw)) return raw.length;
  if (typeof raw === 'string') {
    const parts = raw.split(/[,，;\n\r]+/).map(s => s.trim()).filter(Boolean);
    return parts.length || null;
  }
  return null;
}

function formatTaskTotal(row) {
  const phoneN = resolveQueriedPhoneCount(row);
  const n =
    phoneN ??
    row.totalCount ??
    row.totalNum ??
    row.total ??
    row.queryTotal;
  return n != null && n !== '' ? n : '—';
}

function formatTaskSuccessCount(row) {
  const v =
    row.phoneSuccessCount ??
    row.successPhoneCount ??
    row.numberSuccessCount ??
    row.successCount ??
    row.successNum ??
    row.success ??
    row.okCount;
  return v != null && v !== '' ? v : '—';
}

function formatTaskFailCount(row) {
  const v =
    row.phoneFailCount ??
    row.failPhoneCount ??
    row.numberFailCount ??
    row.failCount ??
    row.failedCount ??
    row.failNum ??
    row.errorCount;
  return v != null && v !== '' ? v : '—';
}

function formatTaskCreateTime(row) {
  const t = row.createTime ?? row.startTime ?? row.createDate;
  return t ? parseTime(t, '{y}/{m}/{d} {h}:{i}:{s}') : '—';
}

function resolveTaskTotalNumeric(row) {
  const phoneN = resolveQueriedPhoneCount(row);
  const n =
    phoneN ??
    row.totalCount ??
    row.totalNum ??
    row.total ??
    row.queryTotal;
  const num = Number(n);
  return Number.isFinite(num) && num > 0 ? num : null;
}

function resolveTaskSuccessFailNumeric(row) {
  const ok =
    row.phoneSuccessCount ??
    row.successPhoneCount ??
    row.numberSuccessCount ??
    row.successCount ??
    row.successNum ??
    row.success ??
    row.okCount;
  const fail =
    row.phoneFailCount ??
    row.failPhoneCount ??
    row.numberFailCount ??
    row.failCount ??
    row.failedCount ??
    row.failNum ??
    row.errorCount;
  return {
    ok: Number.isFinite(Number(ok)) ? Number(ok) : 0,
    fail: Number.isFinite(Number(fail)) ? Number(fail) : 0
  };
}

function computeTaskProgressPercent(row) {
  const explicit = row.progressPercent ?? row.progress;
  if (explicit != null && explicit !== '' && !Number.isNaN(Number(explicit))) {
    return Math.min(100, Math.max(0, Math.round(Number(explicit))));
  }
  const total = resolveTaskTotalNumeric(row);
  const { ok, fail } = resolveTaskSuccessFailNumeric(row);
  const done = ok + fail;
  if (total != null && total > 0) {
    let denom = total;
    if (done > total) {
      denom = total * Math.max(1, Math.ceil(done / total));
    }
    return Math.min(100, Math.round((done / denom) * 100));
  }
  if (row.status === 'COMPLETED') return 100;
  if (row.status === 'FAILED' || row.status === 'CANCELLED') return 100;
  return 0;
}

async function openTaskDetailByRow(row) {
  if (!row?.taskId) {
    proxy.$modal.msgWarning('任务ID不存在');
    return;
  }
  taskDetailTaskId.value = row.taskId;
  taskDetailDialogVisible.value = true;
  taskDetailLoading.value = true;
  taskDetailQueryResults.value = [];
  try {
    // silent：避免与 axios 拦截器重复弹错；失败时在下方统一提示（含后端 msg，如「任务不存在」）
    const response = await getBatchTaskResults(row.taskId, { silent: true });
    const list = response?.data?.results;
    if (Array.isArray(list) && list.length > 0) {
      taskDetailQueryResults.value = list.map(normalizeBatchResultItem);
    } else {
      proxy.$modal.msgWarning('该任务暂无明细数据');
    }
  } catch (error) {
    console.error('获取任务明细失败:', error);
    const tip =
      error && typeof error === 'object' && 'message' in error && error.message
        ? String(error.message)
        : '获取任务明细失败';
    proxy.$modal.msgError(tip);
  } finally {
    taskDetailLoading.value = false;
  }
}

function resetTaskDetailDialog() {
  taskDetailTaskId.value = null;
  taskDetailQueryResults.value = [];
}

// 导出单个任务结果
async function exportTaskResults(taskRecord) {
  if (!taskRecord.taskId) {
    proxy.$modal.msgWarning('任务ID不存在');
    return;
  }
  
  try {
    const response = await getBatchTaskResults(taskRecord.taskId, { silent: true });
    const list = response?.data?.results;
    if (!Array.isArray(list) || list.length === 0) {
      proxy.$modal.msgWarning('该任务暂无明细数据，无法导出');
      return;
    }

    // 归一化结果（与弹窗展示一致）
    const normalized = list.map(normalizeBatchResultItem);

    // 同一手机号多平台返回：取最早 timestamp 作为「查询时间」
    const phoneEarliestTime = new Map();
    for (const r of normalized) {
      const phone = String(r?.phoneNumber ?? '').trim();
      if (!phone) continue;
      const t = r?.timestamp ? new Date(r.timestamp).getTime() : NaN;
      if (!Number.isFinite(t)) continue;
      const prev = phoneEarliestTime.get(phone);
      if (prev == null || t < prev) phoneEarliestTime.set(phone, t);
    }

    // 按手机号聚合到一行，并为平台结果写入多 key（code/中文名）以便 getPlatformStatus 读取
    const grouped = {};
    for (const result of normalized) {
      const phone = String(result?.phoneNumber ?? '').trim();
      if (!phone) continue;
      if (!grouped[phone]) grouped[phone] = {};

      const dataPlatform = result?.data?.platform ? String(result.data.platform) : '';
      const namePlatform = result?.platformName ? String(result.platformName) : '';
      const derivedCode = namePlatform ? getPlatformCode(namePlatform) : '';

      const ensure = (k) => {
        if (!k) return;
        grouped[phone][k] = result.data;
      };

      if (result.data) {
        ensure(dataPlatform);
        ensure(derivedCode);
        ensure(namePlatform);
        if (dataPlatform === '360') ensure('sanliuling');
        if (dataPlatform === 'sanliuling') ensure('360');
        if (derivedCode === '360') ensure('sanliuling');
        if (derivedCode === 'sanliuling') ensure('360');
      }
    }

    const platforms = Array.isArray(availablePlatforms.value) ? availablePlatforms.value : [];
    const platformNames = platforms.map((p) => String(p?.platformName ?? '')).filter(Boolean);
    const header = ['手机号码', '查询时间', ...platformNames];

    const phones = Object.keys(grouped).sort((a, b) => a.localeCompare(b, 'zh-CN'));
    const aoa = [header];
    for (const phoneNumber of phones) {
      const rowObj = { phoneNumber, ...grouped[phoneNumber] };
      const t = phoneEarliestTime.get(phoneNumber);
      const base = [
        formatPhoneDisplay(phoneNumber),
        t != null ? parseTime(t, '{y}/{m}/{d} {h}:{i}:{s}') : '-'
      ];
      const platformCells = platformNames.map((name) => {
        const v = getPlatformStatus(rowObj, name);
        return v == null || String(v).trim() === '' ? '-' : String(v);
      });
      aoa.push([...base, ...platformCells]);
    }

    const ws = XLSX.utils.aoa_to_sheet(aoa);
    // 表头浅灰底色
    const headerStyle = {
      font: { bold: true },
      alignment: { vertical: 'center', horizontal: 'center', wrapText: true },
      fill: { patternType: 'solid', fgColor: { rgb: 'F2F2F2' } },
      border: {
        top: { style: 'thin', color: { rgb: 'D9D9D9' } },
        bottom: { style: 'thin', color: { rgb: 'D9D9D9' } },
        left: { style: 'thin', color: { rgb: 'D9D9D9' } },
        right: { style: 'thin', color: { rgb: 'D9D9D9' } }
      }
    };
    for (let c = 0; c < header.length; c += 1) {
      const addr = XLSX.utils.encode_cell({ r: 0, c });
      if (ws[addr]) ws[addr].s = headerStyle;
    }
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, '查询结果');
    const wbout = XLSX.write(wb, { bookType: 'xlsx', type: 'array' });
    const filename = `批量查询结果_${taskRecord.taskId}.xlsx`;
    saveAs(
      new Blob([wbout], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }),
      filename
    );

    proxy.$modal.msgSuccess('导出成功');
  } catch (error) {
    console.error('导出失败:', error);
    proxy.$modal.msgError('导出失败');
  }
}

// 导出任务历史
function handleExportTaskHistory() {
  if (taskHistoryList.value.length === 0) {
    proxy.$modal.msgWarning('暂无任务记录可导出');
    return;
  }
  
  exportBatchTaskRecord(proxy.addDateRange(taskHistoryQueryParams.value, dateRange.value)).then(response => {
    // 创建下载链接
    const blob = new Blob([response], { 
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' 
    });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `任务历史记录_${new Date().getTime()}.xlsx`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
    
    proxy.$modal.msgSuccess('导出成功');
  }).catch(error => {
    console.error('导出失败:', error);
    proxy.$modal.msgError('导出失败');
  });
}

// 页面加载时初始化
onMounted(() => {
  fetchUserTemplateInfo();
  getTaskHistoryList();
});
</script>

<style scoped>
.batch-record-page {
  width: 100%;
}

.task-manage-table {
  width: 100%;
}

.batch-record-table-card :deep(.el-card__body) {
  padding-top: 12px;
}

.task-manage-table :deep(.el-table td.el-table__cell),
.task-manage-table :deep(.el-table th.el-table__cell) {
  padding: 5px 6px;
}

.task-manage-table :deep(.el-table .cell) {
  line-height: 1.35;
  font-size: 12px;
}

.task-id-ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

.task-progress-cell {
  padding: 1px 2px 0;
  max-width: 72px;
  margin: 0 auto;
  cursor: help;
}

.task-progress-cell--fluid {
  max-width: none;
  width: 100%;
  box-sizing: border-box;
}

.task-progress-cell :deep(.el-progress) {
  width: 100%;
}

.task-detail-body {
  min-height: 120px;
}

.text-muted {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.mb8 {
  margin-bottom: 8px;
}

/* 间距样式 */
.mt10 {
  margin-top: 10px;
}

.results-summary {
  margin-bottom: 10px;
  text-align: left;
}

.results-summary .task-id-display {
  font-size: 13px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 4px;
}

.results-table-container {
  margin-bottom: 8px;
  overflow-x: auto;
}

.task-results-table :deep(.el-table td.el-table__cell),
.task-results-table :deep(.el-table th.el-table__cell) {
  padding: 4px 5px;
}

.task-results-table :deep(.el-table .cell) {
  line-height: 1.3;
  font-size: 12px;
}

.task-results-table :deep(.el-table th .cell) {
  font-size: 11px;
}

.results-table-container :deep(.el-table th .cell) {
  white-space: nowrap;
}

</style>