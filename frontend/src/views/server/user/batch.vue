<template> 
  <div class="app-container batch-record-page">
    <el-card shadow="never" class="batch-record-table-card">
      <div class="batch-toolbar mb8">
        <div class="batch-toolbar-btns">
          <el-button type="primary" plain icon="Plus" @click="openBatchQueryDialog">批量查询</el-button>
          <el-button type="primary" plain icon="Search" @click="goSingle">单次查询</el-button>
          <el-button type="primary" plain icon="List" @click="goBatchRecord">任务记录</el-button>
          <el-button plain icon="Clock" @click="showTaskHistory">历史任务</el-button>
          <el-button plain icon="Refresh" @click="refreshResults">刷新</el-button>
          <el-button plain icon="Delete" @click="clearAll">清空</el-button>
          <el-button type="primary" plain icon="View" @click="openViewResultsDialog" :disabled="queryResults.length === 0">
            查看结果
          </el-button>
          <el-button type="warning" plain icon="Download" @click="exportTaskResults({ taskId: getDefaultTaskId() })" :disabled="queryResults.length === 0">
            导出
          </el-button>
        </div>
        <div class="batch-toolbar-side">
          <right-toolbar v-model:showSearch="showSearch" @queryTable="handleToolbarRefresh" />
        </div>
      </div>

      <div v-show="showSearch" class="batch-main-panel">
        <div class="batch-section-head">
          <span class="batch-section-title">任务进度</span>
          <span v-if="currentTaskId" class="task-id-inline">流水 ID: {{ currentTaskId }}</span>
        </div>
        <div class="task-info batch-task-info">
          <div class="no-task" v-if="taskStatus === 'idle' && !currentTaskId">
            暂无任务
          </div>
          <div class="progress-section" v-if="taskStatus !== 'idle'">
            <div class="progress-container">
              <el-progress
                :percentage="batchPhoneProgress.percentage"
                :status="batchPhoneProgress.percentage === 100 ? 'success' : ''"
                :show-text="false"
                :stroke-width="6"
              />
            </div>
            <div class="progress-details">
              <div class="progress-stats">
                <span class="stat-item">已处理号码: {{ batchPhoneProgress.completed }}/{{ batchPhoneProgress.total }}</span>
                <!-- <span class="stat-item">成功: {{ queryProgress.successCount }}</span>
                <span class="stat-item">失败: {{ queryProgress.failedCount }}</span> -->
              </div>
              <div class="task-status">
                {{ getTaskStatusText() }}
              </div>
            </div>
          </div>
        </div>

        <div class="batch-section-head">
          <span class="batch-section-title">结果预览</span>
          <span class="batch-section-sub">共 {{ groupedResults.length }} 项</span>
        </div>
        <div class="results-preview" v-if="queryResults.length > 0">
          <div class="results-preview-table-wrap">
            <el-table
              :data="pagedGroupedResults"
              border
              stripe
              class="task-manage-table batch-result-table"
            >
              <el-table-column label="号码" min-width="100" align="center" show-overflow-tooltip>
                <template #default="{ row }">
                  {{ formatPhoneDisplay(row.phoneNumber) }}
                </template>
              </el-table-column>
              <el-table-column
                v-for="platform in availablePlatforms"
                :key="platform.id"
                :label="platform.platformName"
                class-name="batch-platform-cell"
                min-width="96"
                align="center"
              >
                <template #default="scope">
                  {{ getPlatformStatus(scope.row, platform.platformName) }}
                </template>
              </el-table-column>
            </el-table>
          </div>
          <pagination
            v-show="groupedResults.length > 0"
            class="results-preview-pagination"
            :total="groupedResults.length"
            v-model:page="previewQueryParams.pageNum"
            v-model:limit="previewQueryParams.pageSize"
            @pagination="handlePreviewPagination"
            :page-sizes="[5, 10, 20, 50]"
            :default-page-size="5"
          />
        </div>
        <div class="no-results" v-else>
          暂无查询结果
        </div>
      </div>
    </el-card>

    <!-- 结果详情对话框 -->
    <el-dialog 
      v-model="resultDialogVisible" 
      title="查询结果详情" 
      width="75%"
      append-to-body
      destroy-on-close
    >
      <div v-if="selectedResult">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="平台名称">{{ selectedResult.platformName }}</el-descriptions-item>
          <el-descriptions-item label="查询号码">{{ formatPhoneDisplay(selectedResult.phoneNumber) }}</el-descriptions-item>
          <el-descriptions-item label="查询状态">
            <el-tag :type="selectedResult.success ? 'success' : 'danger'">
              {{ selectedResult.success ? '成功' : '失败' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="响应时间">
            {{ selectedResult.responseTime ? selectedResult.responseTime + 'ms' : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="查询时间" :span="2">
            {{ parseTime(selectedResult.timestamp, '{y}-{m}-{d} {h}:{i}:{s}') }}
          </el-descriptions-item>
           <el-descriptions-item label="平台标记" :span="2">
            {{ formatPlatformStatusForDisplay(selectedResult.data?.status, selectedResult.data?.platform || getPlatformCode(selectedResult.platformName)) }}
          </el-descriptions-item>
        </el-descriptions>
        
        <div class="result-detail-section" v-if="selectedResult.data">
          <el-divider content-position="left">查询结果数据</el-divider>
          <el-input 
            type="textarea" 
            :rows="10" 
            :value="JSON.stringify(selectedResult.data, null, 2)"
            readonly
          />
        </div>
        
        <div class="error-detail-section" v-if="selectedResult.error">
          <el-divider content-position="left">错误信息</el-divider>
          <el-alert 
            :title="selectedResult.error" 
            type="error" 
            :closable="false" 
            show-icon 
          />
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="resultDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 任务历史对话框 -->
    <el-dialog 
      v-model="taskHistoryDialogVisible" 
      title="任务历史" 
      width="75%"
      append-to-body
      destroy-on-close
    >
      <div class="task-history-table-scroll">
      <el-table
        v-loading="taskHistoryLoading"
        :data="taskHistoryList"
        border
        stripe
        size="small"
        class="task-manage-table"
      >
        <el-table-column label="任务ID" prop="taskId" min-width="200" align="center" />
        <el-table-column label="任务状态" min-width="100" align="center">
          <template #default="scope">
            <el-tag 
              :type="getTaskHistoryStatusType(scope.row.status)" 
              size="small"
            >
              {{ getTaskHistoryStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="任务开始时间" min-width="158" align="center">
          <template #default="scope">
            {{ parseTime(scope.row.startTime, '{y}-{m}-{d} {h}:{i}:{s}') }}
          </template>
        </el-table-column>
        <el-table-column label="任务结束时间" min-width="158" align="center">
          <template #default="scope">
            {{ parseTime(scope.row.endTime, '{y}-{m}-{d} {h}:{i}:{s}') }}
          </template>
        </el-table-column>
        <el-table-column label="查询号码" prop="phoneNumbers" min-width="160" align="center" />
        <el-table-column label="任务结果" min-width="120" align="center">
          <template #default="scope">
            <div class="task-result-actions">
              <el-button 
                v-if="scope.row.status === 'COMPLETED' && scope.row.taskId"
                link 
                type="primary" 
                size="small" 
                @click="exportTaskResults(scope.row)"
              >
                下载结果
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      </div>
      
      <!-- 分页组件 -->
      <pagination 
        v-show="taskHistoryTotal > 0" 
        :total="taskHistoryTotal" 
        v-model:page="taskHistoryQueryParams.pageNum" 
        v-model:limit="taskHistoryQueryParams.pageSize"
        @pagination="getTaskHistoryList"
        :page-sizes="[10, 20, 50]"
        :default-page-size="10"
      />
      
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="handleExportTaskHistory" :disabled="taskHistoryList.length === 0">导出历史</el-button>
          <el-button @click="taskHistoryDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 批量查询对话框 -->
    <el-dialog 
      v-model="batchQueryDialogVisible" 
      title="固话批量查询" 
      width="700px"
      append-to-body
      @close="resetBatchQueryDialog"
    >
      <el-form :model="batchForm" ref="batchFormRef" label-width="90px">
        <el-form-item label="固话号码" prop="numberText">
          <el-input 
            v-model="batchForm.numberText" 
            type="textarea" 
            :rows="10"
            placeholder="请输入固话号码，支持多种格式混合输入，点击解析按钮提取有效号码"
          />
        </el-form-item>
        
        <div class="format-examples">
          支持的格式:
          <div>座机号: 010-12345678、021 8888 8888、075512345678</div>
          <div>手机号: 13812345678、189 1234 5678</div>
          <div>特殊号码: 400-123-4567、800-123-4567</div>
          <div>系统将自动过滤汉字、特殊符号，只保留有效号码格式</div>
        </div>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="extractNumbers" :loading="parseLoading">
            号码提取
          </el-button>
          <el-button @click="batchQueryDialogVisible = false">
            取消
          </el-button>
          <el-button @click="resetBatchQueryDialog">
            重置
          </el-button>
          <el-button type="success" @click="startQuery" :disabled="getValidNumbersCount() === 0 || queryLoading">
            批量查询
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 查看结果对话框 -->
    <el-dialog 
      v-model="viewResultsDialogVisible" 
      title="批量查询结果" 
      width="60%"
      append-to-body
      destroy-on-close
    >
      <div class="results-summary" v-if="currentTaskId">
        <div class="task-id-display">任务ID: #{{ currentTaskId }}</div>
        <el-progress 
          :percentage="batchPhoneProgress.percentage" 
          :status="batchPhoneProgress.percentage === 100 ? 'success' : ''"
          :show-text="false"
          :stroke-width="6"
        />
        <div class="progress-text">
          已处理号码: {{ batchPhoneProgress.completed }}/{{ batchPhoneProgress.total }} 
        </div>
      </div>
      
      <div class="results-table-container">
        <el-table
          class="batch-results-table batch-result-table"
          :data="groupedResults"
          border
          stripe
          max-height="calc(100vh - 220px)"
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
            class-name="batch-platform-cell"
            min-width="96"
            align="center"
          >
            <template #default="scope">
              {{ getPlatformStatus(scope.row, platform.platformName) }}
            </template>
          </el-table-column>
        </el-table>
      </div>
      
      <div class="results-count">
        查询结果 ({{ groupedResults.length }} 条)
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="viewResultsDialogVisible = false">关闭</el-button>
          <el-button type="primary" @click="exportTaskResults({ taskId: getDefaultTaskId() })" :disabled="queryResults.length === 0">
            导出查询结果
          </el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup name="BatchApi">
import useUserStore from '@/store/modules/user'
import { listTemplate, getTemplate } from "@/api/server/template";
import { listPlatformConfig, getPlatformConfig,listPlatformConfigUser } from "@/api/server/platformConfig";
import { singleApi, batchApi } from "@/api/server/apiServer";
import { submitBatchQuery, submitBatchQueryOptimized, getBatchTaskStatus, getBatchTaskResults, cancelBatchTask } from "@/api/server/asyncBatchApi";

/** true：走 /asyncBatchOpt；false：走原 /asyncBatch */
const USE_OPTIMIZED_BATCH_SUBMIT = true;
import { listApiRecord } from "@/api/server/apiRecord";
import { listBatchTaskRecord, exportBatchTaskRecord } from "@/api/server/batchTaskRecord";
import { useRoute, useRouter } from "vue-router";

const { proxy } = getCurrentInstance();
const userStore = useUserStore();
const route = useRoute();
const router = useRouter();
const showSearch = ref(true);

// 响应式数据
const batchForm = ref({
  numberText: ''
});

const parsedNumbers = ref([]);
const numberList = ref([]);
/** 展示用：全部启用平台 */
const availablePlatforms = ref([]);
/** 执行用：当前用户可用平台（模板关联） */
const userAvailablePlatforms = ref([]);
const queryResults = ref([]);
const queryLoading = ref(false);
const parseLoading = ref(false);
const batchFormRef = ref(null);

/** 提交后端用：仅数字，不含区号与号码之间的 - 或其它分隔符 */
function normalizePhoneForApi(val) {
  if (val == null || val === '') return ''
  return String(val).replace(/[^\d]/g, '')
}

// 查询进度
const queryProgress = ref({
  total: 0,
  completed: 0,
  successCount: 0,
  failedCount: 0,
  percentage: 0
});

// 视图模式
const viewMode = ref('platform');
const activePlatforms = ref([]);
const activeNumbers = ref([]);

// 任务相关
const currentTaskId = ref(null);
const taskStatus = ref('idle'); // idle, running, completed, failed, cancelled
const pollingTimer = ref(null);

/** 仅会话内有效：新标签页首次进入无批次号；提交批量查询后再写入 */
const BATCH_TASK_SESSION_KEY = 'batchTaskId';

function getDefaultTaskId() {
  return sessionStorage.getItem(BATCH_TASK_SESSION_KEY) || currentTaskId.value;
}

// 任务历史相关
const taskHistoryDialogVisible = ref(false);
const taskHistoryList = ref([]);
const taskHistoryLoading = ref(false);
const taskHistoryTotal = ref(0);
const taskHistoryQueryParams = ref({
  pageNum: 1,
  pageSize: 10
});

// 结果详情对话框
const resultDialogVisible = ref(false);
const selectedResult = ref(null);

// 批量查询对话框
const batchQueryDialogVisible = ref(false);

// 查看结果对话框
const viewResultsDialogVisible = ref(false);

// 打开查看结果弹窗（必要时按 taskId 拉取最新结果）
async function openViewResultsDialog() {
  viewResultsDialogVisible.value = true;

  const taskId = getDefaultTaskId();
  if (!taskId) return;

  // 如果当前还没有结果，优先按 taskId 拉一次
  if (!queryResults.value || queryResults.value.length === 0) {
    try {
      const response = await getBatchTaskResults(taskId, { silent: false });
      if (response?.data?.results && Array.isArray(response.data.results)) {
        queryResults.value = [];
        processBatchResults(response.data.results);
      }
    } catch (error) {
      console.error('打开查看结果弹窗时获取任务结果失败:', error);
    }
  }
}

// 模板相关
const templateLoading = ref(false);
const userTemplateIds = ref([]);

// 获取用户模板信息
async function fetchUserTemplateInfo() {
  // 先拉全量启用平台，确保页面默认展示所有平台
  await fetchAvailablePlatforms();

  if (!userStore.relTemplate) {
    console.log('用户未关联模板');
    userAvailablePlatforms.value = [];
    return;
  }
  
  templateLoading.value = true;
  try {
    const response = await getTemplate(userStore.relTemplate);
    console.log('模板信息:', response);
    
    if (response.data && response.data.templateInfo) {
      let templateIds = [];
      try {
        templateIds = JSON.parse(response.data.templateInfo);
        console.log('解析后的模板ID数组:', templateIds);
        
        if (Array.isArray(templateIds) && templateIds.length > 0) {
          userTemplateIds.value = templateIds;
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
    const response = await listPlatformConfigUser({
      ids: templateIds
    });
    
    console.log('平台配置响应:', response);
    
    if (response.rows && Array.isArray(response.rows)) {
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

// 查询可用模板和平台
async function fetchAvailablePlatforms() {
  try {
    const platformResponse = await listPlatformConfig({ 
      pageNum: 1, 
      pageSize: 100,
      status: '0'
    });
    
    availablePlatforms.value = platformResponse.rows || [];
  } catch (error) {
    console.error('获取平台配置失败:', error);
    proxy.$modal.msgError('获取平台配置失败');
    availablePlatforms.value = [];
  }
}

// 实时解析号码
function parseNumbersInRealTime() {
  if (!batchForm.value.numberText.trim()) {
    parsedNumbers.value = [];
    numberList.value = [];
    return;
  }
  
  try {
    // 提取所有可能的号码模式，过滤汉字、特殊符号等
    const text = batchForm.value.numberText;
    
    // 优化的号码提取策略，避免重复匹配
    let allNumbers = [];
    
    // 1. 首先匹配手机号（11位，1开头）
    const mobileMatches = text.match(/1[3-9]\d{9}/g) || [];
    allNumbers = allNumbers.concat(mobileMatches);
    
    // 2. 匹配400/800号码（避免与手机号重复）
    const specialMatches = text.match(/[48]00[-\s]?\d{7}/g) || [];
    allNumbers = allNumbers.concat(specialMatches);
    
    // 3. 匹配座机号（带区号，避免与手机号重复）
    // 更严格的座机号匹配：区号不能是00开头，且后面的号码不能以1开头（避免匹配手机号后8位）
    const landlineWithAreaMatches = text.match(/0[1-9]\d{1,2}[-\s]?(?![1])\d{7,8}/g) || [];
    allNumbers = allNumbers.concat(landlineWithAreaMatches);
    
    // 4. 最后匹配纯数字的7-8位号码（排除已经被匹配为手机号前缀的情况）
    const pureNumbers = text.match(/\b\d{7,8}\b/g) || [];
    // 过滤掉那些可能是手机号前缀的号码
    const filteredPureNumbers = pureNumbers.filter(num => {
      // 如果这个7-8位号码以1开头，且长度为8位，可能是手机号的前8位，需要排除
      if (num.startsWith('1') && num.length === 8) {
        // 检查文本中是否有包含这个8位数字的11位手机号
        const mobilePattern = new RegExp(num + '\\d{3}');
        return !mobilePattern.test(text);
      }
      return true;
    });
    allNumbers = allNumbers.concat(filteredPureNumbers);
    
    // 清理和标准化号码（仅保留数字，不在中间插入 -，与后端约定一致）
    const cleanedNumbers = allNumbers
      .map(num => {
        const cleanNum = num.replace(/[^\d]/g, '')
        if (cleanNum.length === 11 && cleanNum.startsWith('1')) {
          return cleanNum
        }
        if (cleanNum.length >= 10 && cleanNum.length <= 12) {
          return cleanNum
        }
        if (cleanNum.length >= 7 && cleanNum.length <= 8) {
          return cleanNum
        }
        if ((cleanNum.startsWith('400') || cleanNum.startsWith('800')) && cleanNum.length === 10) {
          return cleanNum
        }
        return null
      })
      .filter(num => num !== null)
      .filter((num, index, arr) => arr.indexOf(num) === index); // 去重
    
    // 限制最多200个号码
    const limitedNumbers = cleanedNumbers.slice(0, 200);
    
    parsedNumbers.value = limitedNumbers;
    numberList.value = limitedNumbers.map((num, index) => ({
      id: index + 1,
      number: num,
      status: 'pending'
    }));
    
    // 更新文本框内容为格式化后的号码
    if (limitedNumbers.length > 0) {
      batchForm.value.numberText = limitedNumbers.join('\n');
    }
    
  } catch (error) {
    console.error('实时解析号码失败:', error);
  }
}

// 获取有效号码数量
function getValidNumbersCount() {
  return parsedNumbers.value.length;
}

// 解析号码
function parseNumbers() {
  if (!batchForm.value.numberText.trim()) {
    proxy.$modal.msgError('请输入号码清单');
    return;
  }
  
  parseLoading.value = true;
  try {
    // 支持多种分隔符：逗号、空格、换行、分号
    const separators = /[,，\s\n\r;；]+/;
    const numbers = batchForm.value.numberText
      .split(separators)
      .map(num => num.trim())
      .filter(num => num.length > 0)
      .filter((num, index, arr) => arr.indexOf(num) === index); // 去重
    
    // 验证号码格式
    const phoneRegex = /^(1[3-9]\d{9}|(0\d{2,3}-?)?\d{7,8}|400-?\d{7}|800-?\d{7}|1[0-9]{1,4})$/;
    const validNumbers = numbers.filter(num => {
      const cleanNum = num.replace(/\s|-/g, '');
      return phoneRegex.test(cleanNum);
    });
    
    if (validNumbers.length === 0) {
      proxy.$modal.msgError('未找到有效的号码格式');
      return;
    }

    const normalizedList = Array.from(
      new Set(validNumbers.map((n) => normalizePhoneForApi(n)).filter((n) => n.length > 0))
    )
    if (normalizedList.length === 0) {
      proxy.$modal.msgError('未找到有效的号码格式');
      return;
    }

    const invalidCount = numbers.length - validNumbers.length;
    if (invalidCount > 0) {
      proxy.$modal.msgWarning(`已过滤 ${invalidCount} 个无效号码，保留 ${normalizedList.length} 个有效号码`);
    } else {
      proxy.$modal.msgSuccess(`成功解析 ${normalizedList.length} 个有效号码`);
    }

    parsedNumbers.value = normalizedList
    numberList.value = normalizedList.map((num, index) => ({
      id: index + 1,
      number: num,
      status: 'pending'
    }))
    
    // 重置查询进度
    resetQueryProgress();
    
  } catch (error) {
    console.error('解析号码失败:', error);
    proxy.$modal.msgError('解析号码失败');
  } finally {
    parseLoading.value = false;
  }
}

// 清空解析的号码
function clearParsedNumbers() {
  parsedNumbers.value = [];
}

// 移除单个解析的号码
function removeParsedNumber(index) {
  parsedNumbers.value.splice(index, 1);
}

// 清空号码
function clearNumbers() {
  batchForm.value.numberText = '';
  parsedNumbers.value = [];
  numberList.value = [];
  queryResults.value = [];
  resetQueryProgress();
  if (batchFormRef.value) {
    batchFormRef.value.resetFields();
  }
}

// 重置查询进度
function resetQueryProgress() {
  queryProgress.value = {
    total: 0,
    completed: 0,
    successCount: 0,
    failedCount: 0,
    percentage: 0
  };
}

// 打开批量查询对话框
function openBatchQueryDialog() {
  batchQueryDialogVisible.value = true;
}

// 重置批量查询对话框
function resetBatchQueryDialog() {
  batchForm.value.numberText = '';
  parsedNumbers.value = [];
  numberList.value = [];
  if (batchFormRef.value) {
    batchFormRef.value.resetFields();
  }
}

// 提取号码
function extractNumbers() {
  if (!batchForm.value.numberText.trim()) {
    proxy.$modal.msgError('请输入号码清单');
    return;
  }
  
  parseLoading.value = true;
  try {
    // 使用优化后的解析逻辑
    parseNumbersInRealTime();
    
    if (parsedNumbers.value.length === 0) {
      proxy.$modal.msgError('未找到有效的号码格式');
      return;
    }
    
    proxy.$modal.msgSuccess(`成功解析 ${parsedNumbers.value.length} 个有效号码`);
    
    // 重置查询进度
    resetQueryProgress();
    
  } catch (error) {
    console.error('解析号码失败:', error);
    proxy.$modal.msgError('解析号码失败');
  } finally {
    parseLoading.value = false;
  }
}

// 开始查询
function startQuery() {
  if (parsedNumbers.value.length === 0) {
    proxy.$modal.msgError('请先提取号码');
    return;
  }
  startBatchQuery();
  batchQueryDialogVisible.value = false;
}

// 刷新结果
function refreshResults() {
  const taskId = getDefaultTaskId();
  if (taskId && !currentTaskId.value) {
    currentTaskId.value = taskId;
  }

  if (taskId && taskStatus.value === 'running') {
    // 如果有正在运行的任务，重新获取状态
    startStatusPolling();
  } else if (taskId && taskStatus.value === 'completed') {
    // 如果任务已完成，重新获取最终结果
    fetchFinalResults();
  }
}

// 清空所有
function clearAll() {
  clearNumbers();
  currentTaskId.value = null;
  taskStatus.value = 'idle';
  stopPolling();
  sessionStorage.removeItem(BATCH_TASK_SESSION_KEY);
  localStorage.removeItem('batchTaskId');
}

/** 表格展示：不显示 yes- 前缀，接口 yes-详情 仅展示详情段；状态含 no 片段时显示为 -（前后非字母，避免误伤 normal） */
function formatPlatformStatusForDisplay(st, platformCode) {
  if (st === 'no' || st === '无') return '-';
  if (st === 'risk') return '风险';
  if (st === 'normal') return '正常';
  if (st === 'unknown') return '未知';
  const yesLike = st === 'yes' || String(st).trim().toLowerCase() === 'yes';
  if (yesLike) {
    if (platformCode === 'yidonggaopin') return '移动高频拦截';
    return '有结果';
  }
  const s = st == null ? '' : String(st);
  if (s.trim() === '') return '-';
  if (s === '无' || s === 'no') return '-';
  if (/(?:^|[^a-zA-Z])no(?:[^a-zA-Z]|$)/i.test(s)) return '-';
  if (s.startsWith('yes-')) {
    const rest = s.slice(4).trim();
    return rest || '有标记';
  }
  return s || '未知';
}

// 根据平台获取结果
function getResultByPlatform(row, platformName) {
  // 检查是否有platformResults数组
  if (row.data && row.data.platformResults && Array.isArray(row.data.platformResults)) {
    const platformResult = row.data.platformResults.find(p => p.platform === getPlatformCode(platformName));
    if (platformResult) {
      return formatPlatformStatusForDisplay(platformResult.status, platformResult.platform || getPlatformCode(platformName));
    }
  }
  // 兼容旧格式
  if (row.platformName === platformName) {
    return row.success ? '有结果' : '失败';
  }
  return '-';
}

// 获取平台代码映射
function getPlatformCode(platformName) {
  const platformMap = {
    '腾讯': 'tengxun',
    '腾讯平台': 'tengxun',
    '百度': 'baidu', 
    '百度平台': 'baidu',
    '360': 'sanliuling',
    '360平台': '360',
    '360平台查询': '360',
    '360手机卫士':'sanliuling',
    '电话邦': 'dianhuabang',
    '小米电话': 'xiaomi',
    '小米手机': 'xiaomi',
    '联通安全管家': 'ltgj',
    '联通': 'ltgj',
    '联通管家': 'ltgj',
    '中国联通': 'ltgj',
    '搜狗': 'sghmt',
    '搜狗号码通': 'sghmt',
    '移动高频':'yidonggaopin',
    '泰迪熊':'taidixiong',
    '泰迪熊平台':'taidixiong'
  };
  return platformMap[platformName] || platformName.toLowerCase();
}

// 获取平台状态（用于分组显示）
function getPlatformStatus(row, platformName) {
  const platformCode = getPlatformCode(platformName);
  const platformData = row[platformCode];
  if (platformData) {
    return formatPlatformStatusForDisplay(platformData.status, platformCode);
  }
  return '-';
}

// 计算属性：按电话号码分组的结果
const groupedResults = computed(() => {
  if (!queryResults.value.length) return [];
  
  // 按电话号码分组
  const grouped = {};
  queryResults.value.forEach(result => {
    if (!grouped[result.phoneNumber]) {
      grouped[result.phoneNumber] = {};
    }
    
    // 如果有platformResults，则按平台代码存储
    if (result.data && result.data.platform) {
      grouped[result.phoneNumber][result.data.platform] = result.data;
    }
  });
  
  // 转换为数组格式
  return Object.keys(grouped).map(phoneNumber => ({
    phoneNumber,
    ...grouped[phoneNumber]
  }));
});

// 结果预览分页
const previewQueryParams = ref({
  pageNum: 1,
  pageSize: 5
});

const pagedGroupedResults = computed(() => {
  const list = groupedResults.value || [];
  const page = Math.max(Number(previewQueryParams.value.pageNum) || 1, 1);
  const size = Math.max(Number(previewQueryParams.value.pageSize) || 5, 1);
  const start = (page - 1) * size;
  return list.slice(start, start + size);
});

function handlePreviewPagination() {
  const total = groupedResults.value.length;
  const size = Math.max(Number(previewQueryParams.value.pageSize) || 5, 1);
  const maxPage = Math.max(1, Math.ceil(total / size));
  if (previewQueryParams.value.pageNum > maxPage) {
    previewQueryParams.value.pageNum = maxPage;
  }
  if (previewQueryParams.value.pageNum < 1) {
    previewQueryParams.value.pageNum = 1;
  }
}

/** 按「查询号码」维度：总数为待查号码个数，已处理为各平台均已返回结果的号码数 */
const batchPhoneProgress = computed(() => {
  const platformCount = userAvailablePlatforms.value.length;
  const phones =
    parsedNumbers.value.length > 0
      ? parsedNumbers.value.map((p) => String(p))
      : [...new Set(queryResults.value.map((r) => String(r.phoneNumber ?? '')).filter(Boolean))];
  const phoneTotal = phones.length;
  if (phoneTotal === 0 || platformCount === 0) {
    return { completed: 0, total: 0, percentage: 0 };
  }
  const completed = phones.filter((phone) => {
    const ids = new Set(
      queryResults.value
        .filter((r) => String(r.phoneNumber) === phone)
        .map((r) => String(r.platformId))
    );
    return ids.size >= platformCount;
  }).length;
  const percentage = Math.min(100, Math.round((completed / phoneTotal) * 100));
  return { completed, total: phoneTotal, percentage };
});

// 获取号码状态
function getNumberStatus(number) {
  const results = queryResults.value.filter(r => r.phoneNumber === number);
  if (results.length === 0) {
    return { type: 'info', text: '待查询' };
  }
  
  const hasSuccess = results.some(r => r.success);
  const allCompleted = results.length === userAvailablePlatforms.value.length;
  
  if (!allCompleted) {
    return { type: 'warning', text: '查询中' };
  }
  
  return { type: hasSuccess ? 'success' : 'danger', text: hasSuccess ? '部分成功' : '全部失败' };
}

// 获取成功平台数量
function getSuccessCount(number) {
  return queryResults.value.filter(r => r.phoneNumber === number && r.success).length;
}

// 开始批量查询
async function startBatchQuery() {
  if (parsedNumbers.value.length === 0) {
    proxy.$modal.msgError('请先解析号码');
    return;
  }
  
  if (userAvailablePlatforms.value.length === 0) {
    proxy.$modal.msgError('当前用户暂无可用的API平台');
    return;
  }
  
  // 去重：避免同一平台/号码被重复加入导致 batchRequests 膨胀
  // - 平台按 platform.id 去重（防止模板拼接时出现重复平台行）
  // - 号码按值去重（防止解析/实时解析时出现重复）
  parsedNumbers.value = Array.from(
    new Set(parsedNumbers.value.map((p) => normalizePhoneForApi(p)).filter(Boolean))
  )
  userAvailablePlatforms.value = Array.from(
    new Map(
      userAvailablePlatforms.value.map(p => [String(p.id), p])
    ).values()
  );

  queryLoading.value = true;
  queryResults.value = [];
  resetQueryProgress();
  
  // 立即显示初始进度状态
  taskStatus.value = 'running';
  
  try {
    // 构建批量请求数组 - 每个号码和平台的组合作为一个请求对象
    const batchRequests = [];
    
    parsedNumbers.value.forEach((phoneNumber) => {
      const phone = normalizePhoneForApi(phoneNumber)
      if (!phone) return
      userAvailablePlatforms.value.forEach(platform => {
        batchRequests.push({
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
      });
    });
    
    if (true) { // 强制使用异步模式
      // 异步模式：提交任务并开始轮询
      console.log("batchRequests:::",batchRequests);
      const submitFn = USE_OPTIMIZED_BATCH_SUBMIT ? submitBatchQueryOptimized : submitBatchQuery;
      const response = await submitFn(batchRequests);
      if (response.data && response.data.taskId) {
        currentTaskId.value = response.data.taskId;
        proxy.$modal.msgSuccess('批量查询任务已提交，正在后台执行...');
        startStatusPolling();
      } else {
        throw new Error('提交任务失败，未获取到任务ID');
      }
    }
  } catch (error) {
    console.error('批量查询失败:', error);
    proxy.$modal.msgError('批量查询失败：' + (error.message || '未知错误'));
    taskStatus.value = 'failed';
  } finally {
    queryLoading.value = false;
  }
}

// 处理批量查询结果
function processBatchResults(results) {
  results.forEach(result => {
    // 解析响应数据
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
      
      // 根据响应结构判断成功状态
      if (responseData && typeof responseData === 'object') {
        if (responseData.success === true) {
          isSuccess = true;
        } else if (responseData.success === false) {
          isSuccess = false;
          errorMessage = responseData.message || '查询失败';
        }
      }
    }

    // 只负责写入结果，进度以服务端返回的 progress 为准（避免前端重复累加）
    const normalizedData =
      responseData && responseData.platformResults && Array.isArray(responseData.platformResults)
        ? responseData.platformResults[0]
        : (isSuccess ? responseData : null);

    queryResults.value.push({
      phoneNumber: result.phoneNumber,
      platformId: result.platformId,
      platformName: result.platformName,
      success: isSuccess,
      data: normalizedData,
      error: errorMessage || result.error,
      responseTime: result.responseTime || 0,
      timestamp: result.timestamp || new Date()
    });
  });
}

// 获取平台进度
function getPlatformProgress(platformId) {
  const platformResults = queryResults.value.filter(r => r.platformId === platformId);
  const total = parsedNumbers.value.length;
  const completed = platformResults.length;
  const successCount = platformResults.filter(r => r.success).length;
  const failedCount = completed - successCount;
  const percentage = total > 0 ? Math.round((completed / total) * 100) : 0;
  
  return { total, completed, successCount, failedCount, percentage };
}

// 获取号码进度
function getNumberProgress(number) {
  const numberResults = queryResults.value.filter(r => r.phoneNumber === number);
  const total = userAvailablePlatforms.value.length;
  const completed = numberResults.length;
  const successCount = numberResults.filter(r => r.success).length;
  const failedCount = completed - successCount;
  const percentage = total > 0 ? Math.round((completed / total) * 100) : 0;
  
  return { total, completed, successCount, failedCount, percentage };
}

// 获取平台查询结果
function getPlatformResults(platformId) {
  return queryResults.value
    .filter(r => r.platformId === platformId)
    .sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));
}

// 获取号码查询结果
function getNumberResults(number) {
  return queryResults.value
    .filter(r => r.phoneNumber === number)
    .sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));
}

// 显示结果详情
function showResultDetail(result) {
  selectedResult.value = result;
  resultDialogVisible.value = true;
}

// 计算属性
const hasQueryResults = computed(() => {
  return queryResults.value.length > 0;
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

/** 列表展示号码时去掉连字符（固话常见 区号-号码 格式） */
function formatPhoneDisplay(val) {
  if (val == null || val === '') return '—';
  return String(val).replace(/-/g, '');
}

// 导出查询结果（优先导出当前 taskId 的完整结果）
async function handleExportResults() {
  const taskId = getDefaultTaskId();
  let sourceResults = queryResults.value;

  if (taskId) {
    try {
      const response = await getBatchTaskResults(taskId, { silent: false });
      if (response?.data?.results && Array.isArray(response.data.results)) {
        sourceResults = response.data.results;
      }
    } catch (error) {
      console.error('按 taskId 获取导出结果失败，回退到本地结果:', error);
      proxy.$modal.msgWarning('获取任务完整结果失败，已使用当前页面结果导出');
    }
  }

  if (!sourceResults || sourceResults.length === 0) {
    proxy.$modal.msgWarning('暂无查询结果可导出');
    return;
  }
  
  // 准备导出数据
  const exportData = sourceResults.map(result => ({
    查询号码: result.phoneNumber || '',
    平台名称: result.platformName || '',
    查询状态: result.success ? '成功' : '失败',
    响应时间: result.responseTime ? `${result.responseTime}ms` : '-',
    查询时间: result.timestamp ? parseTime(result.timestamp, '{y}-{m}-{d} {h}:{i}:{s}') : '-',
    查询结果: result.success ? JSON.stringify(result.data) : result.error || '-'
  }));
  
  // 使用后端导出接口
  proxy.download('server/apiRecord/export', {
    exportData: JSON.stringify(exportData),
    batchQuery: true
  }, `批量查询结果_${taskId || new Date().getTime()}.xlsx`);
}

// 获取任务状态文本
function getTaskStatusText() {
  switch (taskStatus.value) {
    case 'idle':
      return '待机状态';
    case 'running':
      return '任务执行中';
    case 'completed':
      return '任务已完成';
    case 'failed':
      return '任务执行失败';
    case 'cancelled':
      return '任务已取消';
    default:
      return '未知状态';
  }
}

// 获取任务状态类型
function getTaskStatusType() {
  switch (taskStatus.value) {
    case 'idle':
      return 'info';
    case 'running':
      return 'warning';
    case 'completed':
      return 'success';
    case 'failed':
      return 'error';
    case 'cancelled':
      return 'info';
    default:
      return 'info';
  }
}

// 获取任务历史状态文本
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

// 获取任务历史状态类型
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

// 开始状态轮询
function startStatusPolling() {
  if (pollingTimer.value) {
    clearInterval(pollingTimer.value);
  }
  
  pollingTimer.value = setInterval(async () => {
    if (!currentTaskId.value) {
      stopPolling();
      return;
    }
    
    try {
      const response = await getBatchTaskStatus(currentTaskId.value);
      if (response.data) {
        const status = response.data.status;
        
        // 更新进度信息
        if (response.data.progress) {
          queryProgress.value = {
            ...queryProgress.value,
            ...response.data.progress
          };
        }
        
        // 处理部分结果
        if (response.data.results && response.data.results.length > 0) {
          // 只处理新的结果
          const existingResults = queryResults.value.map(r => 
            `${r.phoneNumber}-${r.platformId}`
          );
          const newResults = response.data.results.filter(r => 
            !existingResults.includes(`${r.phoneNumber}-${r.platformId}`)
          );
          
          if (newResults.length > 0) {
            processBatchResults(newResults);
          }
        }
        
        // 检查任务状态
        if (status === 'COMPLETED') {
          taskStatus.value = 'completed';
          stopPolling();
          queryLoading.value = false;
          proxy.$modal.msgSuccess('批量查询已完成');
          
          // 获取最终结果
          await fetchFinalResults();
        } else if (status === 'FAILED') {
          taskStatus.value = 'failed';
          stopPolling();
          queryLoading.value = false;
          proxy.$modal.msgError('批量查询失败');
        } else if (status === 'CANCELLED') {
          taskStatus.value = 'cancelled';
          stopPolling();
          queryLoading.value = false;
          proxy.$modal.msgWarning('批量查询已取消');
        }
      }
    } catch (error) {
      console.error('轮询任务状态失败:', error);
      
      // 检查是否是任务不存在的错误
      if (error.message && error.message.includes('任务不存在')) {
        console.log('任务已被清理，停止轮询');
        stopPolling();
        taskStatus.value = 'idle';
        queryLoading.value = false;
        currentTaskId.value = null;
        proxy.$modal.msgWarning('任务已过期或被清理，请重新提交查询');
      }
      // 其他错误继续轮询
    }
  }, 2000); // 每2秒轮询一次
}

// 停止轮询
function stopPolling() {
  if (pollingTimer.value) {
    clearInterval(pollingTimer.value);
    pollingTimer.value = null;
  }
}

// 获取最终结果（silent：静默请求与失败提示，用于本会话内恢复任务）
async function fetchFinalResults(silent = false) {
  if (!currentTaskId.value) return;
  
  try {
    const response = await getBatchTaskResults(currentTaskId.value, { silent });
    if (response.data && response.data.results) {
      queryResults.value = [];
      processBatchResults(response.data.results);
    }
  } catch (error) {
    console.error('获取最终结果失败:', error);
    
    // 检查是否是任务不存在的错误
    if (error.message && error.message.includes('任务不存在')) {
      console.log('获取最终结果时任务已被清理');
      currentTaskId.value = null;
      if (!silent) {
        proxy.$modal.notifyWarning('任务已过期，无法获取完整结果');
      }
    }
  }
}

// 取消批量任务
async function cancelCurrentTask() {
  if (!currentTaskId.value || taskStatus.value !== 'running') {
    proxy.$modal.msgWarning('当前没有正在执行的任务');
    return;
  }
  
  try {
    await cancelBatchTask(currentTaskId.value);
    taskStatus.value = 'cancelled';
    stopPolling();
    queryLoading.value = false;
    proxy.$modal.msgSuccess('任务已取消');
  } catch (error) {
    console.error('取消任务失败:', error);
    proxy.$modal.msgError('取消任务失败');
  }
}

function resolveSiblingPath(suffix) {
  const p = route.path || "";
  if (/batch\/?$/i.test(p)) {
    return p.replace(/batch\/?$/i, suffix);
  }
  const i = p.lastIndexOf("/");
  if (i > 0) {
    return `${p.slice(0, i)}/${suffix}`;
  }
  return `/${suffix}`;
}

/** 与后台菜单 component 一致（单条查询页） */
const SINGLE_COMPONENT_VIEW = "server/user/single";

function findSingleRoutePath() {
  const byView = router.getRoutes().find((r) => r.meta?.componentView === SINGLE_COMPONENT_VIEW);
  if (byView?.path) return byView.path;
  const re = /\/(single|single-query|singlequery)\/?$/i;
  const hit = router.getRoutes().find((r) => typeof r.path === "string" && re.test(r.path));
  return hit?.path ?? null;
}

function goSingle() {
  const tries = [];
  const resolved = findSingleRoutePath();
  if (resolved) tries.push(() => router.push(resolved));
  if (router.hasRoute("SingleApi")) tries.push(() => router.push({ name: "SingleApi" }));
  const s1 = resolveSiblingPath("single");
  const s2 = resolveSiblingPath("single-query");
  if (s1 && s1 !== resolved) tries.push(() => router.push(s1));
  if (s2 && s2 !== resolved && s2 !== s1) tries.push(() => router.push(s2));

  const run = (i) => {
    if (i >= tries.length) {
      proxy?.$modal?.msgWarning?.("无法打开单次查询页，请确认菜单中已配置「单条查询」路由");
      return;
    }
    tries[i]().catch(() => run(i + 1));
  };
  run(0);
}

function goBatchRecord() {
  router.push(resolveSiblingPath("batchRecord"));
}

async function handleToolbarRefresh() {
  await fetchUserTemplateInfo();
  refreshResults();
}

// 显示任务历史
function showTaskHistory() {
  taskHistoryDialogVisible.value = true;
  getTaskHistoryList();
}

// 获取任务历史记录
async function getTaskHistoryList() {
  taskHistoryLoading.value = true;
  try {
    // 使用正确的任务历史接口
    const response = await listBatchTaskRecord(taskHistoryQueryParams.value);
    
    if (response.code === 200) {
      taskHistoryList.value = response.rows || [];
      taskHistoryTotal.value = response.total || 0;
    } else {
      proxy.$modal.msgError(response.msg || '获取任务历史失败');
    }
  } catch (error) {
    console.error('获取任务历史失败:', error);
    proxy.$modal.msgError('获取任务历史失败');
  } finally {
    taskHistoryLoading.value = false;
  }
}

// 导出单个任务结果
function exportTaskResults(taskRecord) {
  console.log("taskRecord:",taskRecord);
  if (!taskRecord.taskId) {
    proxy.$modal.msgWarning('任务ID不存在');
    return;
  }
  
  try {
    // 调用后端导出接口，传递任务ID作为查询条件
    proxy.download('server/apiRecord/export', {
      taskId: taskRecord.taskId
    }, `批量查询结果_${taskRecord.taskId}.xlsx`);
    
    proxy.$modal.msgSuccess('导出成功');
  } catch (error) {
    console.error('导出失败:', error);
    proxy.$modal.msgError('导出失败');
  }
}

// 导出任务历史
function handleExportTaskHistory() {
  if (taskHistoryList.value.length === 0) {
    proxy.$modal.msgWarning('暂无任务历史可导出');
    return;
  }
  
  exportBatchTaskRecord(taskHistoryQueryParams.value).then(response => {
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

// 页面卸载时清理
onUnmounted(() => {
  stopPolling();
});

// 从会话缓存恢复任务（仅当本标签页曾提交过批量查询时 sessionStorage 才有 taskId）
async function restoreTaskFromCache() {
  const savedTaskId = getDefaultTaskId();
  if (!savedTaskId) return;

  currentTaskId.value = savedTaskId;

  try {
    const response = await getBatchTaskStatus(savedTaskId, { silent: true });
    const status = response?.data?.status;

    if (response?.data?.progress) {
      queryProgress.value = {
        ...queryProgress.value,
        ...response.data.progress
      };
    }

    if (response?.data?.results && response.data.results.length > 0) {
      queryResults.value = [];
      processBatchResults(response.data.results);
    }

    if (status === 'RUNNING') {
      taskStatus.value = 'running';
      startStatusPolling();
      proxy.$modal.notifyWarning('检测到进行中的批量查询任务，已自动恢复');
    } else if (status === 'COMPLETED') {
      taskStatus.value = 'completed';
      await fetchFinalResults(true);
      queryLoading.value = false;
      // proxy.$modal.notifySuccess('已恢复最近一次批量查询结果');
    } else if (status === 'FAILED') {
      taskStatus.value = 'failed';
    } else if (status === 'CANCELLED') {
      taskStatus.value = 'cancelled';
    } else {
      // 后端未返回有效状态，默认按运行中处理，避免丢任务
      taskStatus.value = 'running';
      startStatusPolling();
    }
  } catch (error) {
    console.error('恢复缓存任务失败:', error);
    taskStatus.value = 'idle';
    currentTaskId.value = null;
    sessionStorage.removeItem(BATCH_TASK_SESSION_KEY);
    localStorage.removeItem('batchTaskId');
  }
}

// 监听任务 ID：写入会话缓存；清空时移除（不再使用 localStorage 持久化批次号）
watch([currentTaskId, taskStatus], ([newTaskId]) => {
  if (newTaskId) {
    sessionStorage.setItem(BATCH_TASK_SESSION_KEY, newTaskId);
  } else {
    sessionStorage.removeItem(BATCH_TASK_SESSION_KEY);
    localStorage.removeItem('batchTaskId');
  }
});

watch(groupedResults, () => {
  handlePreviewPagination();
});

// 页面加载时初始化
onMounted(() => {
  // 历史版本曾把批次号存在 localStorage，避免首次进入用旧 id 请求导致「任务不存在」
  localStorage.removeItem('batchTaskId');
  fetchUserTemplateInfo();
  restoreTaskFromCache();
});
</script>

<style scoped>
.batch-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px 12px;
  width: 100%;
}

.batch-toolbar-btns {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  flex: 1 1 auto;
  min-width: 0;
}

.batch-toolbar-side {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-width: 0;
}

.batch-record-table-card :deep(.el-card__body) {
  padding-top: 12px;
}

.mb8 {
  margin-bottom: 8px;
}

.mt10 {
  margin-top: 10px;
}

.batch-main-panel {
  margin-top: 4px;
}

.batch-section-head {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin: 12px 0 8px;
}

.batch-section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.batch-section-sub {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.task-id-inline {
  font-size: 12px;
  color: var(--el-color-primary);
  font-weight: 500;
}

.task-manage-table {
  width: 100%;
}

/*
 * 结果列表专用表：去掉 size=small，并用 !important 覆盖全局 ruoyi.scss 里 .el-table th { font-size: 13px }
 * 注意：class 在 el-table 根节点上，不能用「外层 .task-manage-table :deep(.el-table .cell)」——内部没有嵌套 .el-table，原写法永远匹配不到 .cell
 */
.batch-result-table.el-table {
  font-size: 15px !important;
}
.batch-result-table.el-table :deep(th.el-table__cell),
.batch-result-table.el-table :deep(td.el-table__cell),
.batch-result-table.el-table :deep(.cell) {
  font-size: 15px !important;
}
.batch-result-table.el-table :deep(.el-tag) {
  font-size: 14px !important;
}

/*
 * fit=true + table-layout=fixed：表格铺满容器；剩余宽度按各列 min-width 比例分配。
 * 平台列 (.batch-platform-cell) 允许换行，长文案在列宽内完整展示，避免挤压重叠。
 */
.task-manage-table :deep(.el-table.el-table--small .cell),
.task-manage-table :deep(.el-table .cell),
.batch-results-table :deep(.el-table.el-table--small .cell),
.batch-results-table :deep(.el-table .cell) {
  text-overflow: clip !important;
}

.task-manage-table :deep(.el-table__body-wrapper),
.batch-results-table :deep(.el-table__body-wrapper) {
  overflow-x: auto !important;
}

.task-manage-table :deep(.el-table__body-wrapper .el-scrollbar__wrap),
.batch-results-table :deep(.el-table__body-wrapper .el-scrollbar__wrap) {
  overflow-x: auto !important;
}

.task-manage-table :deep(.el-table thead th.el-table__cell),
.batch-results-table :deep(.el-table thead th.el-table__cell) {
  white-space: nowrap !important;
  word-break: normal !important;
  overflow-wrap: normal !important;
  vertical-align: middle !important;
  min-width: 0 !important;
  line-height: 1.35 !important;
}

.task-manage-table :deep(.el-table thead .cell),
.batch-results-table :deep(.el-table thead .cell) {
  white-space: nowrap !important;
  word-break: normal !important;
  overflow-wrap: normal !important;
  box-sizing: border-box;
  width: 100% !important;
  max-width: 100% !important;
  overflow: hidden !important;
  text-overflow: clip !important;
  line-height: 1.25 !important;
}

.task-manage-table :deep(.el-table thead .el-tooltip__trigger),
.batch-results-table :deep(.el-table thead .el-tooltip__trigger) {
  display: block;
  box-sizing: border-box;
  width: 100% !important;
  max-width: 100% !important;
  white-space: nowrap !important;
  overflow: hidden !important;
  text-overflow: clip !important;
  word-break: normal !important;
}

.task-manage-table :deep(.el-table__body td.el-table__cell),
.batch-results-table :deep(.el-table__body td.el-table__cell) {
  vertical-align: middle !important;
  min-width: 0;
  white-space: nowrap !important;
}

.task-manage-table :deep(.el-table__body .cell),
.batch-results-table :deep(.el-table__body .cell) {
  white-space: nowrap !important;
  word-break: normal !important;
  overflow-wrap: normal !important;
  box-sizing: border-box;
  width: 100% !important;
  max-width: 100% !important;
  margin-inline: 0;
  overflow: hidden !important;
  text-overflow: clip !important;
  line-height: 1.5;
}

.task-manage-table :deep(.el-table__body .el-tooltip__trigger),
.batch-results-table :deep(.el-table__body .el-tooltip__trigger) {
  display: block;
  box-sizing: border-box;
  width: 100% !important;
  max-width: 100% !important;
  white-space: nowrap !important;
  overflow: hidden !important;
  text-overflow: clip !important;
  word-break: normal !important;
  overflow-wrap: normal !important;
  line-height: 1.5;
}

/* 各平台结果列：换行展示长文本 */
.task-manage-table :deep(.el-table thead th.batch-platform-cell),
.batch-results-table :deep(.el-table thead th.batch-platform-cell) {
  white-space: normal !important;
}

.task-manage-table :deep(.el-table thead th.batch-platform-cell .cell),
.batch-results-table :deep(.el-table thead th.batch-platform-cell .cell) {
  white-space: normal !important;
  word-break: break-word !important;
  overflow-wrap: anywhere !important;
  overflow: visible !important;
}

.task-manage-table :deep(.el-table__body td.batch-platform-cell),
.batch-results-table :deep(.el-table__body td.batch-platform-cell) {
  white-space: normal !important;
}

.task-manage-table :deep(.el-table__body td.batch-platform-cell .cell),
.batch-results-table :deep(.el-table__body td.batch-platform-cell .cell) {
  white-space: normal !important;
  word-break: break-word !important;
  overflow-wrap: anywhere !important;
  overflow: visible !important;
}

.task-manage-table :deep(.el-table__body td.batch-platform-cell .el-tooltip__trigger),
.batch-results-table :deep(.el-table__body td.batch-platform-cell .el-tooltip__trigger) {
  white-space: normal !important;
  word-break: break-word !important;
  overflow-wrap: anywhere !important;
  overflow: visible !important;
  width: 100% !important;
  max-width: 100% !important;
}

/* 状态列等：单个 Tag */
.task-manage-table :deep(.el-table__body .cell:has(.el-tag):not(:has(.task-result-actions))) {
  width: 100% !important;
  max-width: 100% !important;
  white-space: nowrap !important;
  overflow: hidden !important;
}

.task-manage-table :deep(.el-table__body .cell:has(.el-tag):not(:has(.task-result-actions)) .el-tooltip__trigger) {
  max-width: 100% !important;
  white-space: nowrap !important;
  overflow: hidden !important;
}

/* 纯操作列 */
.task-manage-table :deep(.el-table__body .cell:has(.task-result-actions)) {
  width: 100% !important;
  max-width: 100% !important;
  white-space: normal !important;
  overflow: visible !important;
}

.task-manage-table :deep(.el-table__body .cell:has(.task-result-actions) .el-tooltip__trigger) {
  max-width: 100% !important;
  white-space: nowrap !important;
}

.task-manage-table :deep(.task-result-actions) {
  white-space: nowrap;
}

.batch-task-info .task-info {
  text-align: center;
}

.batch-task-info .no-task {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  padding: 16px 0;
}

.batch-task-info .progress-section {
  padding: 8px 0 12px;
}

.batch-task-info .progress-container {
  margin-bottom: 12px;
}

.batch-task-info .progress-details {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-start;
  text-align: left;
}

.batch-task-info .progress-stats {
  display: flex;
  justify-content: flex-start;
  gap: 10px;
  flex-wrap: wrap;
  text-align: left;
}

.batch-task-info .stat-item {
  font-size: 12px;
  color: var(--el-text-color-regular);
  padding: 4px 10px;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  white-space: nowrap;
}

.batch-task-info .task-status {
  font-size: 13px;
  font-weight: 500;
  color: var(--el-color-primary);
}

.results-preview {
  margin-bottom: 8px;
  width: 100%;
  min-width: 0;
}

.results-preview-table-wrap {
  width: 100%;
  min-width: 0;
  overflow-x: auto;
}

.results-preview-pagination {
  margin-top: 8px;
}

.no-results {
  text-align: center;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  padding: 28px 0;
}

.progress-overview {
  margin-bottom: 20px;
}

.platform-progress,
.number-progress {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 15px;
}

.progress-text {
  font-size: 13px;
  color: var(--el-text-color-regular);
  white-space: nowrap;
}

.result-detail-section,
.error-detail-section {
  margin-top: 16px;
}

.task-result-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 4px;
}

.task-result-actions .el-button {
  min-width: 52px;
  padding: 4px 8px;
}

.number-list-section {
  margin-top: 16px;
}

.format-examples {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.format-examples div {
  margin-bottom: 5px;
}

.parsed-numbers-preview {
  margin-top: 10px;
}

.parsed-numbers-preview .numbers-count {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.parsed-numbers-preview .numbers-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.parsed-numbers-preview .number-tag {
  margin: 0;
  font-size: 13px;
}

.results-summary {
  margin-bottom: 12px;
  text-align: left;
}

.results-summary .task-id-display {
  font-size: 13px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 6px;
}

.results-summary .progress-text {
  margin: 8px 0 0;
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.results-table-container {
  margin-bottom: 8px;
  width: 100%;
  min-width: 0;
  max-height: calc(100vh - 220px);
  overflow: auto;
}

.task-history-table-scroll {
  max-height: 420px;
  overflow: auto;
}

@media (max-width: 768px) {
  .results-preview,
  .results-table-container {
    overflow-x: auto;
  }

  .task-manage-table :deep(.el-table__body-wrapper),
  .batch-results-table :deep(.el-table__body-wrapper) {
    overflow-x: auto !important;
  }
}

.results-count {
  text-align: center;
  font-size: 14px;
  font-weight: 600;
  color: #409eff;
  margin-top: 10px;
}

.el-collapse {
  border: none;
}

.el-collapse-item {
  margin-bottom: 10px;
}

@media (max-width: 768px) {
  .batch-toolbar-side {
    width: 100%;
    justify-content: flex-start;
  }

  .progress-overview .el-col {
    margin-bottom: 15px;
  }

  .platform-progress,
  .number-progress {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>