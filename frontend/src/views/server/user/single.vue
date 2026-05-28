<template> 
  <div class="app-container batch-record-page">
    <el-card shadow="never" body-class="search-card">
      <el-form
        ref="queryFormRef"
        :model="queryForm"
        :inline="true"
        v-show="showSearch"
        label-width="72px"
      >
        <el-form-item label="查询号码" prop="phoneNumber">
          <el-input
            v-model="queryForm.phoneNumber"
            placeholder="请输入查询号码"
            clearable
            style="width: 240px"
            maxlength="11"
            show-word-limit
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item class="single-search-actions">
          <el-button type="primary" icon="Search" @click="handleQuery" :loading="queryLoading">
            搜索
          </el-button>
          <el-button icon="Refresh" @click="handleReset">重置</el-button>
          <el-button type="primary" plain icon="List" @click="goSingleRecord">查询记录</el-button>
          <el-button type="primary" plain icon="Plus" @click="goBatch">批量查询</el-button>
          <el-button type="warning" plain icon="Download" @click="handleExport" :disabled="!hasResults">
            导出
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="mt10 batch-record-table-card">
      <el-row :gutter="10" class="mb8" align="middle">
        <el-col :xs="24" :sm="12" :md="10" class="single-toolbar-sort">
          <span class="sort-label">排序：</span>
          <el-radio-group v-model="resultSortMode" size="small">
            <el-radio-button value="fixed">固定顺序</el-radio-button>
            <el-radio-button value="markedFirst">有标记优先</el-radio-button>
          </el-radio-group>
          <el-tag type="info" size="small" class="platform-count-tag">
            共 {{ queryResults.length }} 个平台
          </el-tag>
        </el-col>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="handleToolbarRefresh" />
      </el-row>

      <div class="single-query-results-table-wrap">
        <el-table
          :data="displayedQueryResults"
          border
          stripe
          size="small"
          table-layout="fixed"
          class="task-manage-table single-query-results-table"
          v-loading="queryLoading"
        >
          <el-table-column
            label="平台"
            prop="platformName"
            min-width="20"
            align="center"
            show-overflow-tooltip
            :resizable="false"
          >
            <template #default="scope">
              <span class="platform-name">{{ scope.row.platformName }}</span>
            </template>
          </el-table-column>

          <el-table-column label="状态" width="110" align="center" :resizable="false">
            <template #default="scope">
              <el-tag
                :type="getStatusType(scope.row)"
                size="small"
                effect="light"
                class="single-query-status-tag"
              >
                {{ getStatusText(scope.row) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column
            label="结果"
            prop="result"
            align="left"
            show-overflow-tooltip
            :resizable="false"
          >
            <template #default="scope">
              <span v-if="scope.row.status === 'unqueried'" class="unqueried-text">未查询</span>
              <span v-else class="result-text">{{ getResultText(scope.row) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- 历史记录对话框 -->
    <el-dialog 
      v-model="historyDialogVisible" 
      :title="`${selectedPlatform?.platformName} - 历史查询记录`" 
      width="75%"
      append-to-body
      destroy-on-close
    >
      <el-table v-loading="historyLoading" :data="historyList" border stripe size="small" class="task-manage-table">
        <el-table-column label="序号" type="index" width="52" align="center" />
        <el-table-column label="查询号码" prop="phone" min-width="120" align="center" show-overflow-tooltip />
        <el-table-column label="查询状态" min-width="88" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.requestStatus === '0' ? 'success' : 'danger'" size="small">
              {{ scope.row.requestStatus === '0' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="响应时间(ms)" prop="requestTime" min-width="110" align="center" />
        <el-table-column label="查询时间" prop="createTime" min-width="158" align="center">
          <template #default="scope">
            {{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}
          </template>
        </el-table-column>
        <el-table-column label="查询结果" prop="results" min-width="160" show-overflow-tooltip>
          <template #default="scope">
            <span v-if="scope.row.results">
              {{ scope.row.results.length > 50 ? scope.row.results.substring(0, 50) + '...' : scope.row.results }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页组件 -->
      <pagination 
        v-show="historyTotal > 0" 
        :total="historyTotal" 
        v-model:page="historyQueryParams.pageNum" 
        v-model:limit="historyQueryParams.pageSize"
        @pagination="getHistoryList"
        :page-sizes="[30, 50, 100]"
        :default-page-size="30"
      />
      
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="handleExportHistory" :disabled="historyList.length === 0">导出历史</el-button>
          <el-button @click="historyDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="SingleApi">
import useUserStore from '@/store/modules/user'
import { listTemplate, getTemplate } from "@/api/server/template";
import { listPlatformConfig, getPlatformConfig,listPlatformConfigUser } from "@/api/server/platformConfig";
import { submitBatchQueryOptimized, getBatchTaskStatus, getBatchTaskResults } from "@/api/server/asyncBatchApi";
import { listApiRecord } from "@/api/server/apiRecord";
import { useRoute, useRouter } from "vue-router";

const { proxy } = getCurrentInstance();

const userStore = useUserStore();
const route = useRoute();
const router = useRouter();
const showSearch = ref(true);

// 响应式数据
const queryForm = ref({
  phoneNumber: ''
});

/** 展示用：全部启用的平台 */
const availablePlatforms = ref([]);
/** 执行用：当前用户可用的平台（模板关联） */
const userAvailablePlatforms = ref([]);
const queryResults = ref([]);
/** 固定顺序：与接口返回/平台列表一致；有标记优先：status 为 yes 或 yes- 前缀的排前 */
const resultSortMode = ref('fixed');
const queryLoading = ref(false);
const queryFormRef = ref(null);

/** 与批量页一致：提交优化批量接口时仅传数字 */
function normalizePhoneForApi(val) {
  if (val == null || val === '') return '';
  return String(val).replace(/[^\d]/g, '');
}

/** 单次查询是否已离开页面，用于中止轮询 */
const singleQueryAbort = ref(false);

// 历史记录相关
const historyDialogVisible = ref(false);
const historyLoading = ref(false);
const historyList = ref([]);
const selectedPlatform = ref(null);
const historyTotal = ref(0);
const historyQueryParams = ref({
  pageNum: 1,
  pageSize: 10,
  platformId: null
});

// 模板相关
const templateLoading = ref(false);
const userTemplateIds = ref([]);

// 初始化查询结果列表，显示所有平台为"未查询"状态
function initializeQueryResults() {
  queryResults.value = availablePlatforms.value.map(platform => ({
    platformId: platform.id,
    platformName: platform.platformName,
    success: false,
    status: 'unqueried', // 新增状态字段
    data: null,
    platformResults: [],
    responseTime: 0,
    timestamp: null
  }));
}

/** 接口返回的 status 含 no 时按「无」展示（yes / yes- 不含 no，排除） */
function statusIndicatesNone(status) {
  if (status == null || status === '') return false;
  const s = String(status).trim();
  if (s === '无') return true;
  const lower = s.toLowerCase();
  if (lower === 'yes' || lower.startsWith('yes-')) return false;
  return /no/i.test(s);
}

function isResultRowMarked(row) {
  if (row.status === 'unqueried') return false;
  const pr = row.data?.platformResults;
  if (!pr?.length) return false;
  const status = pr[0]?.status;
  if (status == null || status === '') return false;
  return status === 'yes' || String(status).startsWith('yes-');
}

// 获取状态类型
function getStatusType(row) {
  if (row.status === 'unqueried') return 'info';
  if (!row.data || !row.data.platformResults || row.data.platformResults.length === 0) return 'danger';
  
  const platformResult = row.data.platformResults[0];
  const status = platformResult.status;

  if (statusIndicatesNone(status)) return 'success'; // 无 / 含 no
  if (status === 'yes' || String(status).startsWith('yes-')) return 'danger'; // 有标记
  return 'danger';
}

// 获取状态文本
function getStatusText(row) {
  if (row.status === 'unqueried') return '未查询';
  if (!row.data || !row.data.platformResults || row.data.platformResults.length === 0) return '查询错误';
  
  const platformResult = row.data.platformResults[0];
  const status = platformResult.status;

  if (statusIndicatesNone(status)) return '无标记';
  if (status === 'yes' || String(status).startsWith('yes-')) return '有标记';
  return '未知';
}

// 获取实际结果文本
function getResultText(row) {
  if (row.status === 'unqueried') return '未查询';
  if (!row.data || !row.data.platformResults || row.data.platformResults.length === 0) return '查询错误';
  
  const platformResult = row.data.platformResults[0];
  const status = platformResult.status;

  if (statusIndicatesNone(status)) return '无';
  if (status === 'yes') return '有标记';
  if (String(status).startsWith('yes-')) {
    const detailInfo = String(status).substring(4);
    return detailInfo || '有标记';
  }
  return status || '未知';
}

// 获取用户模板信息
async function fetchUserTemplateInfo() {
  // 先拉全量启用平台，确保页面默认展示所有平台
  await fetchAvailablePlatforms();
  initializeQueryResults();

  if (!userStore.relTemplate) {
    console.log('用户未关联模板');
    userAvailablePlatforms.value = [];
    return;
  }
  
  templateLoading.value = true;
  try {
    // 调用 getTemplate 获取模板详细信息
    const response = await getTemplate(userStore.relTemplate);
    console.log('模板信息:', response);
    
    if (response.data && response.data.templateInfo) {
      // 解析 templateInfo 字符串 "[1, 7, 2, 8]" 为数组
      let templateIds = [];
      try {
        templateIds = JSON.parse(response.data.templateInfo);
        console.log('解析后的模板ID数组:', templateIds);
        
        if (Array.isArray(templateIds) && templateIds.length > 0) {
          userTemplateIds.value = templateIds;
          // 根据模板ID获取对应的平台配置
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
  } finally {
    templateLoading.value = false;
  }
}

// 根据模板ID获取平台配置
async function fetchPlatformsByTemplateIds(templateIds) {
  try {
    // 调用 listPlatformConfigUser 接口，传递 Integer[] ids 参数
    const response = await listPlatformConfigUser({
      ids: templateIds
    });
    
    console.log('平台配置响应:', response);
    
    if (response.rows && Array.isArray(response.rows)) {
      // 只保留启用状态的平台（仅用于实际执行）
      const validPlatforms = response.rows.filter(platform => platform.status === '0');
      userAvailablePlatforms.value = validPlatforms;
      console.log('用户可用平台列表:', userAvailablePlatforms.value);
      
      if (validPlatforms.length === 0) {
        proxy.$modal.msgWarning('当前模板下暂无可用的API平台');
      }
    } else {
      console.warn('平台配置响应格式异常:', response);
      userAvailablePlatforms.value = [];
    }
  } catch (error) {
    console.error('获取平台配置失败:', error);
    proxy.$modal.msgError('获取平台配置失败');
    userAvailablePlatforms.value = [];
  }
}

// 计算属性
const hasResults = computed(() => {
  return queryResults.value.some(result => result && (result.data || result.error));
});

const displayedQueryResults = computed(() => {
  const list = queryResults.value;
  if (!list.length || resultSortMode.value === 'fixed') {
    return list;
  }
  return [...list].sort((a, b) => {
    const ma = isResultRowMarked(a) ? 0 : 1;
    const mb = isResultRowMarked(b) ? 0 : 1;
    return ma - mb;
  });
});

// 格式化时间显示
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

// 格式化URL显示
function formatUrl(url) {
  if (!url) return '-';
  try {
    const urlObj = new URL(url);
    return urlObj.hostname;
  } catch {
    return url.length > 20 ? url.substring(0, 20) + '...' : url;
  }
}

// 获取平台查询结果
function getPlatformResult(platformId) {
  return queryResults.value.find(result => result.platformId === platformId);
}

// 查询可用模板和平台
async function fetchAvailablePlatforms() {
  try {
    // 获取启用的平台配置
    const platformResponse = await listPlatformConfig({ 
      pageNum: 1, 
      pageSize: 100,
      status: '0' // 只获取启用的平台
    });
    
    availablePlatforms.value = platformResponse.rows || [];
  } catch (error) {
    console.error('获取平台配置失败:', error);
    proxy.$modal.msgError('获取平台配置失败');
    availablePlatforms.value = [];
  }
}

/** 将优化批量接口返回的一条 ApiResult 转为单页表格行（data 保留完整 platformResults 供状态/结果列使用） */
function mapBatchApiResultToTableRow(result, serverTaskId) {
  let responseData = result.data;
  if (responseData && typeof responseData === 'string') {
    try {
      responseData = JSON.parse(responseData);
    } catch {
      responseData = null;
    }
  }
  let isSuccess = Boolean(result.success);
  if (responseData && typeof responseData === 'object' && 'success' in responseData) {
    isSuccess = responseData.success === true;
  }
  return {
    platformId: result.platformId,
    platformName: result.platformName,
    taskId: serverTaskId,
    success: isSuccess,
    data: responseData ?? null,
    error: result.error || (responseData && responseData.success === false ? responseData.message : null),
    responseTime: result.responseTime || 0,
    timestamp: result.timestamp ? new Date(result.timestamp) : new Date()
  };
}

/** 轮询任务直至完成，再拉取全量结果（与优化批量后端一致） */
async function waitForOptimizedBatchResults(taskId) {
  const maxWaitMs = 180000;
  const intervalMs = 1500;
  const start = Date.now();
  while (Date.now() - start < maxWaitMs) {
    if (singleQueryAbort.value) {
      throw new Error('已离开页面，查询已中止');
    }
    const res = await getBatchTaskStatus(taskId, { silent: true });
    const stPayload = res?.data ?? res;
    const stInner = stPayload?.data ?? stPayload;
    const status = stInner?.status ?? stPayload?.status;
    if (status === 'COMPLETED') {
      const fr = await getBatchTaskResults(taskId, { silent: false });
      return extractBatchTaskResultsList(fr);
    }
    if (status === 'FAILED' || status === 'CANCELLED') {
      const msg = stInner?.errorMessage ?? stPayload?.errorMessage ?? '任务失败';
      throw new Error(msg);
    }
    await new Promise((r) => setTimeout(r, intervalMs));
  }
  throw new Error('查询超时，请稍后在查询记录中查看');
}

/** 兼容 AjaxResult：results 可能在 data 或 data.data 下 */
function extractBatchTaskResultsList(response) {
  const body = response?.data ?? response;
  if (Array.isArray(body?.results)) return body.results;
  if (Array.isArray(body?.data?.results)) return body.data.results;
  return [];
}

/** 提交任务返回的 taskId */
function extractTaskIdFromSubmit(response) {
  const d = response?.data ?? response;
  return d?.taskId ?? d?.data?.taskId;
}

// 执行查询（走优化版批量：服务端并行外呼 + 批量落库，与 batch.vue 同源）
async function handleQuery() {
  if (!queryForm.value.phoneNumber) {
    proxy.$modal.msgError('请输入查询号码');
    return;
  }

  const phoneRegex = /^(1[3-9]\d{9}|(0\d{2,3}-?)?\d{7,8}|400-?\d{7}|800-?\d{7}|1[0-9]{1,4})$/;
  if (!phoneRegex.test(queryForm.value.phoneNumber.replace(/\s|-/g, ''))) {
    proxy.$modal.msgError('请输入正确的查询号码格式');
    return;
  }

  if (userAvailablePlatforms.value.length === 0) {
    proxy.$modal.msgError('当前用户暂无可用的API平台');
    return;
  }

  const phone = normalizePhoneForApi(queryForm.value.phoneNumber);
  if (!phone) {
    proxy.$modal.msgError('请输入有效的查询号码');
    return;
  }

  const batchRequests = [];
  for (const platform of userAvailablePlatforms.value) {
    batchRequests.push({
      queryType: "2",
      platformId: platform.id,
      platformName: platform.platformName,
      url: platform.url,
      requestIntervalMs: platform.requestIntervalMs,
      timeoutMs: platform.timeoutMs,
      retryCount: platform.retryCount,
      preActionType: platform.preActionType,
      preActionConfig: platform.preActionConfig,
      headersTemplate: platform.headersTemplate,
      concurrencyLimit: platform.concurrencyLimit,
      phoneNumber: phone
    });
  }

  queryLoading.value = true;
  queryResults.value = [];
  singleQueryAbort.value = false;

  try {
    const response = await submitBatchQueryOptimized(batchRequests);
    const taskId = extractTaskIdFromSubmit(response);
    if (!taskId) {
      throw new Error('未获取到任务ID');
    }

    const rawResults = await waitForOptimizedBatchResults(taskId);
    const byPlatform = new Map();
    for (const r of rawResults) {
      const key = r.platformId != null ? String(r.platformId) : '';
      byPlatform.set(key, r);
    }

    const rows = [];
    for (const platform of availablePlatforms.value) {
      const key = String(platform.id);
      const r = byPlatform.get(key);
      if (r) {
        rows.push(mapBatchApiResultToTableRow(r, taskId));
      } else {
        rows.push({
          platformId: platform.id,
          platformName: platform.platformName,
          taskId,
          success: false,
          status: 'unqueried',
          error: null,
          data: null,
          platformResults: [],
          responseTime: 0,
          timestamp: new Date()
        });
      }
    }
    queryResults.value = rows;
    proxy.$modal.msgSuccess('查询完成');
  } catch (error) {
    console.error('查询失败:', error);
    const msg = error?.message || error?.msg || '未知错误';
    proxy.$modal.msgError('查询失败：' + msg);
  } finally {
    queryLoading.value = false;
  }
}

// 重置查询
function handleReset() {
  queryForm.value.phoneNumber = '';
  // 重新初始化为"未查询"状态，而不是清空
  initializeQueryResults();
  if (queryFormRef.value) {
    queryFormRef.value.resetFields();
  }
}

// 导出查询结果
function handleExport() {
  if (!hasResults.value) {
    proxy.$modal.msgWarning('暂无查询结果可导出');
    return;
  }

  const taskId = queryResults.value.find((row) => row?.taskId)?.taskId;
  if (!taskId) {
    proxy.$modal.msgWarning('未获取到任务ID，无法导出');
    return;
  }
  
  try {
    proxy.download('server/apiRecord/export', {
      phone: queryForm.value.phoneNumber,
      queryType: 2,
      taskId
    }, `API查询结果_${queryForm.value.phoneNumber}_${new Date().getTime()}.xlsx`);
    proxy.$modal.msgSuccess('导出成功');
  } catch (error) {
    console.error('导出失败:', error);
    proxy.$modal.msgError('导出失败');
  }
}
  // // 准备导出数据（与当前表格排序一致）
  // const exportData = displayedQueryResults.value
  //   .filter(result => result && (result.data || result.error))
  //   .map(result => ({
  //     platformName: result.platformName,
  //     phoneNumber: queryForm.value.phoneNumber,
  //     queryStatus: result.success ? '成功' : '失败',
  //     responseTime: result.responseTime ? `${result.responseTime}ms` : '-',
  //     queryResult: result.success ? JSON.stringify(result.data) : result.error,
  //     queryTime: result.timestamp ? new Date(result.timestamp).toLocaleString() : '-'
  //   }));
  
  // 使用后端导出接口
//   proxy.download('server/apiRecord/export', {
//     exportData: JSON.stringify(exportData),
//     phone: queryForm.value.phoneNumber
//   }, `API查询结果_${queryForm.value.phoneNumber}_${new Date().getTime()}.xlsx`);
// }

// 显示详情对话框
function showDetails(result) {
  // 可以在这里显示更详细的信息，比如响应时间、详细数据等
  const details = `
平台名称: ${result.platformName}
查询号码: ${queryForm.value.phoneNumber}
查询状态: ${result.success ? '成功' : '失败'}
响应时间: ${result.responseTime ? result.responseTime + 'ms' : '-'}
查询时间: ${result.timestamp ? new Date(result.timestamp).toLocaleString() : '-'}
${result.data ? `详细数据: ${JSON.stringify(result.data, null, 2)}` : ''}
${result.error ? `错误信息: ${result.error}` : ''}
  `;
  
  proxy.$alert(details, `${result.platformName} - 查询详情`, {
    confirmButtonText: '确定',
    type: result.success ? 'success' : 'error',
    customClass: 'details-dialog'
  });
}

// 显示历史记录
function showHistory(platform) {
  selectedPlatform.value = platform;
  historyQueryParams.value.platformId = platform.id;
  historyQueryParams.value.pageNum = 1;
  historyDialogVisible.value = true;
  getHistoryList();
}

// 获取历史记录数据（支持分页）
async function getHistoryList() {
  historyLoading.value = true;
  try {
    const response = await listApiRecord(historyQueryParams.value);
    historyList.value = response.rows || [];
    historyTotal.value = response.total || 0;
  } catch (error) {
    console.error('获取历史记录失败:', error);
    proxy.$modal.msgError('获取历史记录失败');
  } finally {
    historyLoading.value = false;
  }
}

// 获取历史记录数据（保持兼容性）
async function fetchHistoryData(platformId) {
  historyQueryParams.value.platformId = platformId;
  historyQueryParams.value.pageNum = 1;
  await getHistoryList();
}

// 导出历史记录
function handleExportHistory() {
  if (historyList.value.length === 0) {
    proxy.$modal.msgWarning('暂无历史记录可导出');
    return;
  }
  
  // 准备导出数据
  const exportData = historyList.value.map(record => ({
    平台名称: selectedPlatform.value?.platformName || '',
    查询号码: record.phone || '',
    查询状态: record.requestStatus === '0' ? '成功' : '失败',
    响应时间: record.requestTime ? `${record.requestTime}ms` : '-',
    查询时间: record.createTime ? parseTime(record.createTime, '{y}-{m}-{d} {h}:{i}:{s}') : '-',
    查询结果: record.results || '-'
  }));
  
  // 使用后端导出接口
  proxy.download('server/apiRecord/export', {
    exportData: JSON.stringify(exportData),
    platformId: selectedPlatform.value?.id
  }, `${selectedPlatform.value?.platformName}_历史记录_${new Date().getTime()}.xlsx`);
}

function resolveSiblingPagePath(suffix) {
  const p = route.path || "";
  if (new RegExp(`single/?$`, "i").test(p)) {
    return p.replace(/single\/?$/i, suffix);
  }
  const i = p.lastIndexOf("/");
  if (i > 0) {
    return `${p.slice(0, i)}/${suffix}`;
  }
  return `/${suffix}`;
}

/** 与后台菜单 component 一致，用于 meta.componentView 精确匹配 */
const SINGLE_RECORD_COMPONENT_VIEW = "server/user/singleRecord";

/** 从已注册路由解析「单条查询记录」页：优先后端 component 路径，再按 path 后缀兜底 */
function findSingleRecordRoutePath() {
  const byView = router
    .getRoutes()
    .find((r) => r.meta?.componentView === SINGLE_RECORD_COMPONENT_VIEW);
  if (byView?.path) return byView.path;
  const re = /\/(singleRecord|single-record|singlerecord)\/?$/i;
  const hit = router.getRoutes().find((r) => typeof r.path === "string" && re.test(r.path));
  return hit?.path ?? null;
}

function goSingleRecord() {
  const tries = [];
  const resolved = findSingleRecordRoutePath();
  if (resolved) tries.push(() => router.push(resolved));
  if (router.hasRoute("SingleRecord")) tries.push(() => router.push({ name: "SingleRecord" }));
  const s1 = resolveSiblingPagePath("singleRecord");
  const s2 = resolveSiblingPagePath("single-record");
  if (s1 && s1 !== resolved) tries.push(() => router.push(s1));
  if (s2 && s2 !== resolved && s2 !== s1) tries.push(() => router.push(s2));

  const run = (i) => {
    if (i >= tries.length) {
      proxy?.$modal?.msgWarning?.("无法打开查询记录页，请确认菜单中已配置「单条查询记录」路由");
      return;
    }
    tries[i]().catch(() => run(i + 1));
  };
  run(0);
}

/** 与后台菜单 component 一致（批量查询页） */
const BATCH_COMPONENT_VIEW = "server/user/batch";

function findBatchRoutePath() {
  const byView = router.getRoutes().find((r) => r.meta?.componentView === BATCH_COMPONENT_VIEW);
  if (byView?.path) return byView.path;
  const re = /\/(batch|batch-query|batchquery)\/?$/i;
  const hit = router.getRoutes().find((r) => typeof r.path === "string" && re.test(r.path));
  return hit?.path ?? null;
}

function goBatch() {
  const tries = [];
  const resolved = findBatchRoutePath();
  if (resolved) tries.push(() => router.push(resolved));
  if (router.hasRoute("BatchApi")) tries.push(() => router.push({ name: "BatchApi" }));
  const s1 = resolveSiblingPagePath("batch");
  const s2 = resolveSiblingPagePath("batch-query");
  if (s1 && s1 !== resolved) tries.push(() => router.push(s1));
  if (s2 && s2 !== resolved && s2 !== s1) tries.push(() => router.push(s2));

  const run = (i) => {
    if (i >= tries.length) {
      proxy?.$modal?.msgWarning?.("无法打开批量查询页，请确认菜单中已配置「批量查询」路由");
      return;
    }
    tries[i]().catch(() => run(i + 1));
  };
  run(0);
}

async function handleToolbarRefresh() {
  await fetchUserTemplateInfo();
}

// 页面加载时获取用户模板信息
onMounted(() => {
  fetchUserTemplateInfo();
});

onUnmounted(() => {
  singleQueryAbort.value = true;
});

</script>

<style scoped>
.batch-record-page {
  width: 100%;
}

.single-search-actions :deep(.el-form-item__content) {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.task-manage-table {
  width: 100%;
}

.batch-record-table-card :deep(.el-card__body) {
  padding-top: 12px;
}

.task-manage-table :deep(.el-table td.el-table__cell),
.task-manage-table :deep(.el-table th.el-table__cell) {
  padding: 12px 10px;
}

.task-manage-table :deep(.el-table .cell) {
  line-height: 1.5;
  font-size: 12px;
}

/* 容器查询：字号随表格区域宽度在 10px～12px 间变化 */
.single-query-results-table-wrap {
  container-type: inline-size;
  width: 100%;
}

/* 平台结果表：固定行高 50px；列 resizable=false 禁用表头左右拖宽；字体随容器宽度自适应 */
.task-manage-table.single-query-results-table :deep(.el-table thead .el-table__cell) {
  height: 50px !important;
  min-height: 50px !important;
  max-height: 50px !important;
  padding: 0 10px !important;
  box-sizing: border-box !important;
  vertical-align: middle !important;
}

.task-manage-table.single-query-results-table :deep(.el-table__body td.el-table__cell) {
  height: 50px !important;
  min-height: 50px !important;
  max-height: 50px !important;
  padding: 0 10px !important;
  box-sizing: border-box !important;
  vertical-align: middle !important;
}

.task-manage-table.single-query-results-table :deep(.el-table thead .cell),
.task-manage-table.single-query-results-table :deep(.el-table__body .cell) {
  font-size: clamp(13px, 0.85cqw + 11px, 16px) !important;
  line-height: 22px !important;
}

.task-manage-table.single-query-results-table :deep(.el-table__body .cell) {
  max-height: 50px !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
  white-space: nowrap !important;
}

.task-manage-table.single-query-results-table :deep(.platform-name),
.task-manage-table.single-query-results-table :deep(.result-text),
.task-manage-table.single-query-results-table :deep(.unqueried-text) {
  font-size: inherit !important;
  line-height: inherit !important;
}

.task-manage-table.single-query-results-table :deep(.single-query-status-tag) {
  font-size: inherit !important;
  line-height: 22px !important;
  height: auto !important;
  padding: 0 8px !important;
}

.single-toolbar-sort {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.sort-label {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  white-space: nowrap;
}

.platform-count-tag {
  margin-left: 4px;
}

.platform-name {
  font-weight: 500;
  color: var(--el-text-color-primary);
  font-size: 12px;
}

.result-text {
  color: var(--el-text-color-regular);
  font-size: 12px;
}

.error-text {
  color: var(--el-color-danger);
  font-size: 12px;
}

.unqueried-text {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-style: italic;
}

:deep(.details-dialog .el-message-box__content) {
  white-space: pre-line;
  font-family: monospace;
  font-size: 13px;
  line-height: 1.5;
  max-height: 400px;
  overflow-y: auto;
}

.mb8 {
  margin-bottom: 8px;
}

.mt10 {
  margin-top: 10px;
}
</style>