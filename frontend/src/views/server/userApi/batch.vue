<template> 
  <div class="app-container">
    <!-- 号码输入区域 -->
    <el-card shadow="never" class="search-card">
      <template #header>
        <div class="card-header">
          <span>批量号码查询</span>
          <el-tag v-if="parsedNumbers.length > 0" type="info">
            已解析 {{ parsedNumbers.length }} 个号码
          </el-tag>
        </div>
      </template>
      
      <el-form :model="batchForm" ref="batchFormRef" label-width="100px">
        <el-form-item label="号码清单" prop="numberText">
          <el-input 
            v-model="batchForm.numberText" 
            type="textarea" 
            :rows="6"
            placeholder="请粘贴号码清单，支持用逗号、空格、换行分隔"
            show-word-limit
            maxlength="10000"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="MagicStick" @click="parseNumbers" :loading="parseLoading">
            解析号码
          </el-button>
          <el-button icon="Delete" @click="clearNumbers">清空</el-button>
          <el-button type="success" icon="Download" @click="handleExportResults" :disabled="!hasQueryResults">
            导出结果
          </el-button>
        </el-form-item>
      </el-form>
      
      <!-- 解析后的号码列表 -->
      <div class="number-list-section">
        <el-divider content-position="left">已解析号码列表</el-divider>
        <el-table :data="numberList" border max-height="300" size="small">
          <el-table-column label="序号" type="index" width="60" align="center" />
          <el-table-column label="号码" width="150" align="center" show-overflow-tooltip>
            <template #default="scope">
              {{ formatPhoneDisplay(scope.row.number) }}
            </template>
          </el-table-column>
          <el-table-column label="查询状态" width="100" align="center">
            <template #default="scope">
              <el-tag 
                :type="getNumberStatus(scope.row.number).type" 
                size="small"
              >
                {{ getNumberStatus(scope.row.number).text }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="成功平台" width="100" align="center">
            <template #default="scope">
              {{ getSuccessCount(scope.row.number) }}
            </template>
          </el-table-column>
          <el-table-column label="总平台" width="80" align="center">
            {{ availablePlatforms.length }}
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- 查询控制区域 -->
    <el-card shadow="never" class="mt10">
      <template #header>
        <div class="card-header">
          <span>批量查询控制</span>
          <div class="header-actions">
            <el-button 
              type="primary" 
              icon="VideoPlay" 
              @click="startBatchQuery" 
              :loading="queryLoading"
              :disabled="parsedNumbers.length === 0 || availablePlatforms.length === 0"
            >
              开始批量查询
            </el-button>
            <el-button 
              v-if="taskStatus === 'running'" 
              type="danger" 
              icon="Close"
              @click="cancelCurrentTask"
            >
              取消任务
            </el-button>
            <el-button 
              type="info" 
              icon="Clock" 
              @click="showTaskHistory"
            >
              任务历史
            </el-button>
          </div>
        </div>
      </template>
      
      <!-- 查询进度概览 -->
      <div class="progress-overview">
        <!-- 任务状态显示 -->
        <div class="task-status-section">
          <el-alert 
            :title="getTaskStatusText()" 
            :type="getTaskStatusType()" 
            :closable="false" 
            show-icon
            style="margin-bottom: 15px;"
          >
            <template #default>
              <div v-if="taskStatus === 'running'">
                <p>任务ID: {{ currentTaskId }}</p>
                <p>任务正在后台执行中，您可以切换页面，查询会继续进行。</p>
              </div>
              <div v-else-if="taskStatus === 'completed'">
                <p>任务已完成，所有查询结果已获取。</p>
              </div>
              <div v-else-if="taskStatus === 'failed'">
                <p>任务执行失败，请检查错误信息并重试。</p>
              </div>
              <div v-else-if="taskStatus === 'cancelled'">
                <p>任务已被取消。</p>
              </div>
            </template>
          </el-alert>
        </div>
        
        <el-row :gutter="20">
          <el-col :span="8">
            <el-statistic title="总体进度" :value="queryProgress.completed" :suffix="'/' + queryProgress.total">
              <template #suffix>
                <el-progress 
                  :percentage="queryProgress.percentage" 
                  :status="queryProgress.percentage === 100 ? 'success' : ''"
                  :show-text="false"
                  style="width: 100px; margin-left: 10px;"
                />
              </template>
            </el-statistic>
          </el-col>
          <el-col :span="8">
            <el-statistic title="成功查询" :value="queryProgress.successCount" />
          </el-col>
          <el-col :span="8">
            <el-statistic title="失败查询" :value="queryProgress.failedCount" />
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 查询结果区域 -->
    <el-card shadow="never" class="mt10">
      <template #header>
        <div class="card-header">
          <span>查询结果明细</span>
          <div class="header-actions">
            <el-button-group>
              <el-button 
                :type="viewMode === 'platform' ? 'primary' : ''" 
                size="small" 
                @click="viewMode = 'platform'"
              >
                按平台查看
              </el-button>
              <el-button 
                :type="viewMode === 'number' ? 'primary' : ''" 
                size="small" 
                @click="viewMode = 'number'"
              >
                按号码查看
              </el-button>
            </el-button-group>
          </div>
        </div>
      </template>
      
      <!-- 按平台查看 -->
      <div v-if="viewMode === 'platform'">
        <el-collapse v-model="activePlatforms">
          <el-collapse-item 
            v-for="platform in availablePlatforms" 
            :key="platform.id"
            :title="`${platform.platformName} (${getPlatformProgress(platform.id).completed}/${getPlatformProgress(platform.id).total})`"
            :name="platform.id"
          >
            <div class="platform-progress">
              <el-progress 
                :percentage="getPlatformProgress(platform.id).percentage"
                :status="getPlatformProgress(platform.id).percentage === 100 ? 'success' : ''"
              />
              <span class="progress-text">
                成功: {{ getPlatformProgress(platform.id).successCount }} | 
                失败: {{ getPlatformProgress(platform.id).failedCount }}
              </span>
            </div>
            
            <el-table :data="getPlatformResults(platform.id)" border size="small" max-height="400">
              <el-table-column label="号码" width="120" align="center" show-overflow-tooltip>
                <template #default="scope">
                  {{ formatPhoneDisplay(scope.row.phoneNumber) }}
                </template>
              </el-table-column>
              <el-table-column label="状态" width="80" align="center">
                <template #default="scope">
                  <el-tag 
                    :type="scope.row.success ? 'success' : 'danger'" 
                    size="small"
                  >
                    {{ scope.row.success ? '成功' : '失败' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="响应时间" width="100" align="center">
                <template #default="scope">
                  {{ scope.row.responseTime ? scope.row.responseTime + 'ms' : '-' }}
                </template>
              </el-table-column>
              <el-table-column label="查询时间" width="160" align="center">
                <template #default="scope">
                  {{ parseTime(scope.row.timestamp, '{y}-{m}-{d} {h}:{i}:{s}') }}
                </template>
              </el-table-column>
              <el-table-column label="平台标记" width="150" align="center">
                <template #default="scope">
                  {{ scope.row.data?.status || '-' }}
                </template>
              </el-table-column>
              <el-table-column label="查询结果" show-overflow-tooltip>
                <template #default="scope">
                  <div v-if="scope.row.success === true && scope.row.data">
                    <el-button link type="primary" size="small" @click="showResultDetail(scope.row)">
                      查看详情
                    </el-button>
                  </div>
                  <span v-else-if="scope.row.success === false">
                    {{ scope.row.data?.message || scope.row.error || '查询失败' }}
                  </span>
                  <span v-else>-</span>
                </template>
              </el-table-column>
            </el-table>
          </el-collapse-item>
        </el-collapse>
      </div>
      
      <!-- 按号码查看 -->
      <div v-else-if="viewMode === 'number'">
        <el-collapse v-model="activeNumbers">
          <el-collapse-item 
            v-for="number in parsedNumbers" 
            :key="number"
            :title="`${formatPhoneDisplay(number)} (${getNumberProgress(number).completed}/${getNumberProgress(number).total})`"
            :name="number"
          >
            <div class="number-progress">
              <el-progress 
                :percentage="getNumberProgress(number).percentage"
                :status="getNumberProgress(number).percentage === 100 ? 'success' : ''"
              />
              <span class="progress-text">
                成功: {{ getNumberProgress(number).successCount }} | 
                失败: {{ getNumberProgress(number).failedCount }}
              </span>
            </div>
            
            <el-table :data="getNumberResults(number)" border size="small" max-height="400">
              <el-table-column label="平台" prop="platformName" width="150" align="center" />
              <el-table-column label="状态" width="80" align="center">
                <template #default="scope">
                  <el-tag 
                    :type="scope.row.success ? 'success' : 'danger'" 
                    size="small"
                  >
                    {{ scope.row.success ? '成功' : '失败' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="响应时间" width="100" align="center">
                <template #default="scope">
                  {{ scope.row.responseTime ? scope.row.responseTime + 'ms' : '-' }}
                </template>
              </el-table-column>
              <el-table-column label="查询时间" width="160" align="center">
                <template #default="scope">
                  {{ parseTime(scope.row.timestamp, '{y}-{m}-{d} {h}:{i}:{s}') }}
                </template>
              </el-table-column>
              <el-table-column label="平台标记" width="150" align="center">
                <template #default="scope">
                  {{ scope.row.data?.status || '-' }}
                </template>
              </el-table-column>
              <el-table-column label="查询结果" show-overflow-tooltip>
                <template #default="scope">
                  <div v-if="scope.row.success === true && scope.row.data">
                    <el-button link type="primary" size="small" @click="showResultDetail(scope.row)">
                      查看详情
                    </el-button>
                  </div>
                  <span v-else-if="scope.row.success === false">
                    {{ scope.row.data?.message || scope.row.error || '查询失败' }}
                  </span>
                  <span v-else>-</span>
                </template>
              </el-table-column>
            </el-table>
          </el-collapse-item>
        </el-collapse>
      </div>
    </el-card>

    <!-- 结果详情对话框 -->
    <el-dialog 
      v-model="resultDialogVisible" 
      title="查询结果详情" 
      width="800px" 
      append-to-body
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
            {{ selectedResult.data?.status || '-' }}
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
      width="50%" 
      append-to-body
    >
      <el-table :data="taskHistoryList" border size="small" max-height="400">
        <el-table-column label="任务ID" prop="taskId" width="250" align="center" />
        <el-table-column label="任务状态" width="100" align="center">
          <template #default="scope">
            <el-tag 
              :type="getTaskHistoryStatusType(scope.row.status)" 
              size="small"
            >
              {{ getTaskHistoryStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="任务开始时间" width="160" align="center">
          <template #default="scope">
            {{ parseTime(scope.row.startTime, '{y}-{m}-{d} {h}:{i}:{s}') }}
          </template>
        </el-table-column>
        <el-table-column label="任务结束时间" width="160" align="center">
          <template #default="scope">
            {{ parseTime(scope.row.endTime, '{y}-{m}-{d} {h}:{i}:{s}') }}
          </template>
        </el-table-column>
        <el-table-column label="查询号码" prop="phoneNumbers" width="200" align="center" />
        <el-table-column label="任务结果" align="center" show-overflow-tooltip >
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

const { proxy } = getCurrentInstance();
const userStore = useUserStore();

// 响应式数据
const batchForm = ref({
  numberText: ''
});

const parsedNumbers = ref([]);
const numberList = ref([]);
const availablePlatforms = ref([]);
const queryResults = ref([]);
const queryLoading = ref(false);
const parseLoading = ref(false);
const batchFormRef = ref(null);

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

// 模板相关
const templateLoading = ref(false);
const userTemplateIds = ref([]);

// 获取用户模板信息
async function fetchUserTemplateInfo() {
  if (!userStore.relTemplate) {
    console.log('用户未关联模板');
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
      availablePlatforms.value = validPlatforms;
      console.log('可用平台列表:', availablePlatforms.value);
      
      if (validPlatforms.length === 0) {
        proxy.$modal.msgWarning('当前模板下暂无可用的API平台');
      }
    } else {
      console.warn('平台配置响应格式异常:', response);
      availablePlatforms.value = [];
    }
  } catch (error) {
    console.error('获取平台配置失败:', error);
    proxy.$modal.msgError('获取平台配置失败');
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
  }
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
    
    const invalidCount = numbers.length - validNumbers.length;
    if (invalidCount > 0) {
      proxy.$modal.msgWarning(`已过滤 ${invalidCount} 个无效号码，保留 ${validNumbers.length} 个有效号码`);
    } else {
      proxy.$modal.msgSuccess(`成功解析 ${validNumbers.length} 个有效号码`);
    }
    
    parsedNumbers.value = validNumbers;
    numberList.value = validNumbers.map((num, index) => ({
      id: index + 1,
      number: num,
      status: 'pending'
    }));
    
    // 重置查询进度
    resetQueryProgress();
    
  } catch (error) {
    console.error('解析号码失败:', error);
    proxy.$modal.msgError('解析号码失败');
  } finally {
    parseLoading.value = false;
  }
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

// 获取号码状态
function getNumberStatus(number) {
  const results = queryResults.value.filter(r => r.phoneNumber === number);
  if (results.length === 0) {
    return { type: 'info', text: '待查询' };
  }
  
  const hasSuccess = results.some(r => r.success);
  const allCompleted = results.length === availablePlatforms.value.length;
  
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
  
  if (availablePlatforms.value.length === 0) {
    proxy.$modal.msgError('暂无可用的API平台');
    return;
  }
  
  queryLoading.value = true;
  queryResults.value = [];
  resetQueryProgress();
  
  const totalQueries = parsedNumbers.value.length * availablePlatforms.value.length;
  queryProgress.value.total = totalQueries;
  
  try {
    // 构建批量请求数组 - 每个号码和平台的组合作为一个请求对象
    const batchRequests = [];
    
    parsedNumbers.value.forEach(phoneNumber => {
      availablePlatforms.value.forEach(platform => {
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
          phoneNumber: phoneNumber
        });
      });
    });
    
    if (true) { // 强制使用异步模式
      // 异步模式：提交任务并开始轮询
      const submitFn = USE_OPTIMIZED_BATCH_SUBMIT ? submitBatchQueryOptimized : submitBatchQuery;
      const response = await submitFn(batchRequests);
      if (response.data && response.data.taskId) {
        currentTaskId.value = response.data.taskId;
        taskStatus.value = 'running';
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
          
          // 解析platformResults数组，将每个平台结果作为单独的记录
          if (responseData.platformResults && Array.isArray(responseData.platformResults)) {
            responseData.platformResults.forEach(platformResult => {
              queryResults.value.push({
                phoneNumber: responseData.phone || result.phoneNumber,
                platformId: result.platformId,
                platformName: platformResult.platform || result.platformName,
                success: platformResult.status !== 'no', // 根据status字段判断成功
                data: platformResult,
                error: platformResult.status === 'no' ? '查询无结果' : null,
                responseTime: result.responseTime || 0,
                timestamp: result.timestamp || new Date()
              });
              
              // 更新进度
              queryProgress.value.completed++;
              if (platformResult.status !== 'no') {
                queryProgress.value.successCount++;
              } else {
                queryProgress.value.failedCount++;
              }
            });
            
            // 计算百分比
            queryProgress.value.percentage = Math.round((queryProgress.value.completed / queryProgress.value.total) * 100);
            return; // 跳过后续处理，避免重复添加
          }
        } else if (responseData.success === false) {
          isSuccess = false;
          errorMessage = responseData.message || '查询失败';
        }
      }
    }
    
    // 如果没有platformResults数组，按原来的逻辑处理
    queryResults.value.push({
      phoneNumber: result.phoneNumber,
      platformId: result.platformId,
      platformName: result.platformName,
      success: isSuccess,
      data: isSuccess ? responseData : null,
      error: errorMessage || result.error,
      responseTime: result.responseTime || 0,
      timestamp: result.timestamp || new Date()
    });
    
    // 更新进度
    queryProgress.value.completed++;
    if (isSuccess) {
      queryProgress.value.successCount++;
    } else {
      queryProgress.value.failedCount++;
    }
    
    // 计算百分比
    queryProgress.value.percentage = Math.round((queryProgress.value.completed / queryProgress.value.total) * 100);
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
  const total = availablePlatforms.value.length;
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

// 导出查询结果
function handleExportResults() {
  if (!hasQueryResults.value) {
    proxy.$modal.msgWarning('暂无查询结果可导出');
    return;
  }
  
  // 准备导出数据
  const exportData = queryResults.value.map(result => ({
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
  }, `批量查询结果_${new Date().getTime()}.xlsx`);
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

// 获取最终结果
async function fetchFinalResults() {
  if (!currentTaskId.value) return;
  
  try {
    const response = await getBatchTaskResults(currentTaskId.value);
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
      proxy.$modal.notifyWarning('任务已过期，无法获取完整结果');
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
  if (!taskRecord.taskId) {
    proxy.$modal.msgWarning('任务ID不存在');
    return;
  }
  
  try {
    // 调用后端导出接口，传递任务ID作为查询条件
    proxy.download('server/apiRecord/export', {
      taskId: taskRecord.taskId
    }, `批量查询结果_${taskRecord.taskId}}.xlsx`);
    
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

// 页面加载时检查是否有未完成的任务
onMounted(() => {
  fetchUserTemplateInfo();
  
  // 从localStorage恢复任务状态（可选）
  const savedTaskId = localStorage.getItem('batchTaskId');
  const savedTaskStatus = localStorage.getItem('batchTaskStatus');
  
  if (savedTaskId && savedTaskStatus === 'running') {
    currentTaskId.value = savedTaskId;
    taskStatus.value = 'running';
    startStatusPolling();
    proxy.$modal.notifyWarning('检测到未完成的批量查询任务，正在恢复状态...');
  }
});

// 监听任务状态变化，保存到localStorage
watch([currentTaskId, taskStatus], ([newTaskId, newStatus]) => {
  if (newTaskId && newStatus === 'running') {
    localStorage.setItem('batchTaskId', newTaskId);
    localStorage.setItem('batchTaskStatus', newStatus);
  } else {
    localStorage.removeItem('batchTaskId');
    localStorage.removeItem('batchTaskStatus');
  }
});

// 页面加载时获取用户模板信息
onMounted(() => {
  fetchUserTemplateInfo();
});
</script>

<style scoped>
/* 卡片头部样式 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  font-size: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 号码列表区域 */
.number-list-section {
  margin-top: 20px;
}

/* 进度概览 */
.progress-overview {
  margin-bottom: 20px;
}

/* 平台进度 */
.platform-progress, .number-progress {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 15px;
}

.progress-text {
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
}

/* 结果详情区域 */
.result-detail-section, .error-detail-section {
  margin-top: 20px;
}

/* 任务结果列样式 */
.task-result-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.task-result-actions .el-button {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 60px;
}

/* 间距样式 */
.mt10 {
  margin-top: 10px;
}

/* 折叠面板样式 */
.el-collapse {
  border: none;
}

.el-collapse-item {
  margin-bottom: 10px;
}

/* 表格样式 */
.el-table {
  font-size: 13px;
}

/* 响应式布局 */
@media (max-width: 768px) {
  .progress-overview .el-col {
    margin-bottom: 15px;
  }
  
  .platform-progress, .number-progress {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>