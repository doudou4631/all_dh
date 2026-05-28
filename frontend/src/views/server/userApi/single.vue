<template> 
  <div class="app-container">
    <!-- 查询区域 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryForm" ref="queryFormRef" :inline="true" label-width="80px">
        <el-form-item label="查询号码" prop="phoneNumber">
          <el-input 
            v-model="queryForm.phoneNumber" 
            placeholder="请输入查询号码" 
            clearable 
            style="width: 200px;"
            maxlength="15"
            show-word-limit
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery" :loading="queryLoading">
            查询
          </el-button>
          <el-button icon="RefreshRight" @click="handleReset">重置</el-button>
          <el-button type="success" icon="Download" @click="handleExport" :disabled="!hasResults">
            导出结果
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- API卡片展示区域 -->
    <el-card shadow="never" class="mt10">
      <template #header>
        <div class="card-header">
          <span>API查询结果</span>
          <el-tag v-if="queryResults.length > 0" type="info">
            共 {{ queryResults.length }} 个平台
          </el-tag>
        </div>
      </template>
      
      <div v-loading="queryLoading" class="api-cards-container">
        <el-empty v-if="!queryLoading && availablePlatforms.length === 0" description="暂无可用的API平台" />
        
        <el-row :gutter="20" v-else>
          <el-col 
            v-for="platform in availablePlatforms" 
            :key="platform.id" 
            :xs="24" 
            :sm="12" 
            :md="8" 
            :lg="6"
            class="mb20"
          >
            <el-card 
              shadow="always" 
              class="api-card"
              :class="{ 'success': getPlatformResult(platform.id)?.success, 'error': getPlatformResult(platform.id)?.success === false }"
            >
              <template #header>
                <div class="api-card-header">
                  <span class="platform-name">{{ platform.platformName }}</span>
                  <div class="header-actions">
                    <el-tag 
                      v-if="getPlatformResult(platform.id)" 
                      :type="getPlatformResult(platform.id).success ? 'success' : 'danger'"
                      size="small"
                    >
                      {{ getPlatformResult(platform.id).success ? '成功' : '失败' }}
                    </el-tag>
                    <el-tag v-else type="info" size="small">待查询</el-tag>
                    <el-button 
                      link 
                      type="primary" 
                      size="small" 
                      @click="showHistory(platform)"
                      icon="Clock"
                    >
                      历史记录
                    </el-button>
                  </div>
                </div>
              </template>
              
              <div class="api-card-content">
                <el-descriptions :column="1" size="small">
                  <el-descriptions-item label="平台URL">
                    <el-tooltip :content="platform.url" placement="top">
                      <span class="url-text">{{ formatUrl(platform.url) }}</span>
                    </el-tooltip>
                  </el-descriptions-item>
                  <el-descriptions-item label="查询号码">
                    {{ queryForm.phoneNumber || '-' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="响应时间" v-if="getPlatformResult(platform.id)?.responseTime">
                    {{ getPlatformResult(platform.id).responseTime }}ms
                  </el-descriptions-item>
                </el-descriptions>
                
                <!-- 查询结果显示 -->
                <div v-if="getPlatformResult(platform.id)" class="result-section">
                  <el-divider content-position="left">查询结果</el-divider>
                  <div class="result-content">
                    <div v-if="getPlatformResult(platform.id).platformResults && getPlatformResult(platform.id).platformResults.length > 0">
                      <el-table :data="getPlatformResult(platform.id).platformResults" size="small" border>
                        <el-table-column label="状态" prop="status" />
                      </el-table>
                    </div>
                    <div v-else class="no-result">
                      <el-alert
                        title="查询失败：未返回有效的平台结果"
                        type="error"
                        :closable="false"
                        show-icon
                      />
                    </div>
                  </div>
                </div>
                
                <!-- 错误信息显示 -->
                <div v-if="getPlatformResult(platform.id)?.error" class="error-section">
                  <el-alert
                    :title="getPlatformResult(platform.id).error"
                    type="error"
                    :closable="false"
                    show-icon
                  />
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 历史记录对话框 -->
    <el-dialog 
      v-model="historyDialogVisible" 
      :title="`${selectedPlatform?.platformName} - 历史查询记录`" 
      width="800px" 
      append-to-body
    >
      <el-table v-loading="historyLoading" :data="historyList" border>
        <el-table-column label="序号" type="index" width="60" align="center" />
        <el-table-column label="查询号码" prop="phone" width="120" align="center" />
        <el-table-column label="查询状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.requestStatus === '0' ? 'success' : 'danger'" size="small">
              {{ scope.row.requestStatus === '0' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="响应时间(ms)" prop="requestTime" width="120" align="center" />
        <el-table-column label="查询时间" prop="createTime" width="160" align="center">
          <template #default="scope">
            {{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}
          </template>
        </el-table-column>
        <el-table-column label="查询结果" prop="results" show-overflow-tooltip>
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
import { singleApi } from "@/api/server/apiServer";
import { listApiRecord } from "@/api/server/apiRecord";

const { proxy } = getCurrentInstance();

const userStore = useUserStore();

// 响应式数据
const queryForm = ref({
  phoneNumber: ''
});

const availablePlatforms = ref([]);
const queryResults = ref([]);
const queryLoading = ref(false);
const queryFormRef = ref(null);

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

// 获取用户模板信息
async function fetchUserTemplateInfo() {
  if (!userStore.relTemplate) {
    console.log('用户未关联模板');
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
      // 只保留启用状态的平台
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

// 计算属性
const hasResults = computed(() => {
  return queryResults.value.some(result => result && (result.data || result.error));
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
  }
}

// 执行查询
async function handleQuery() {
  if (!queryForm.value.phoneNumber) {
    proxy.$modal.msgError('请输入查询号码');
    return;
  }
  
  // 验证查询号码格式
  const phoneRegex = /^(1[3-9]\d{9}|(0\d{2,3}-?)?\d{7,8}|400-?\d{7}|800-?\d{7}|1[0-9]{1,4})$/;
  if (!phoneRegex.test(queryForm.value.phoneNumber.replace(/\s|-/g, ''))) {
    proxy.$modal.msgError('请输入正确的查询号码格式');
    return;
  }
  
  if (availablePlatforms.value.length === 0) {
    proxy.$modal.msgError('暂无可用的API平台');
    return;
  }
  
  queryLoading.value = true;
  queryResults.value = []; // 清空之前的结果
  
  try {
    // 并发查询所有平台
    const promises = availablePlatforms.value.map(platform => querySinglePlatform(platform));
    const results = await Promise.allSettled(promises);
    
    // 处理结果
    results.forEach((result, index) => {
      const platform = availablePlatforms.value[index];
      if (result.status === 'fulfilled') {
        const responseData = result.value.data;
        const platformResults = responseData?.platformResults || [];
        const hasValidResults = platformResults.length > 0;
        
        queryResults.value.push({
          platformId: platform.id,
          platformName: platform.platformName,
          success: hasValidResults,
          data: responseData,
          platformResults: platformResults,
          responseTime: result.value.responseTime || 0,
          timestamp: new Date()
        });
      } else {
        queryResults.value.push({
          platformId: platform.id,
          platformName: platform.platformName,
          success: false,
          error: result.reason?.message || '查询失败',
          platformResults: [],
          timestamp: new Date()
        });
      }
    });
    
    proxy.$modal.msgSuccess('查询完成');
  } catch (error) {
    console.error('查询失败:', error);
    proxy.$modal.msgError('查询失败：' + (error.message || '未知错误'));
  } finally {
    queryLoading.value = false;
  }
}

// 查询单个平台
async function querySinglePlatform(platform) {
  const requestData = {
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
    phoneNumber: queryForm.value.phoneNumber
  };
  
  const startTime = Date.now();
  
  try {
    const response = await singleApi(requestData);
    const responseTime = Date.now() - startTime;
    
    return {
      data: response.data,
      responseTime
    };
  } catch (error) {
    const responseTime = Date.now() - startTime;
    throw {
      message: error.message || '请求失败',
      responseTime
    };
  }
}

// 重置查询
function handleReset() {
  queryForm.value.phoneNumber = '';
  queryResults.value = [];
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
  
  // 准备导出数据
  const exportData = queryResults.value
    .filter(result => result && (result.data || result.error))
    .map(result => ({
      platformName: result.platformName,
      phoneNumber: queryForm.value.phoneNumber,
      queryStatus: result.success ? '成功' : '失败',
      responseTime: result.responseTime ? `${result.responseTime}ms` : '-',
      queryResult: result.success ? JSON.stringify(result.data) : result.error,
      queryTime: result.timestamp ? new Date(result.timestamp).toLocaleString() : '-'
    }));
  
  // 使用后端导出接口
  proxy.download('server/apiRecord/export', {
    exportData: JSON.stringify(exportData),
    phoneNumber: queryForm.value.phoneNumber
  }, `API查询结果_${queryForm.value.phoneNumber}_${new Date().getTime()}.xlsx`);
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

/* API卡片容器 */
.api-cards-container {
  min-height: 200px;
}

/* API卡片样式 */
.api-card {
  height: 100%;
  transition: all 0.3s ease;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.api-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
  border-color: #409eff;
}

.api-card.success {
  border-color: #67c23a;
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f7ff 100%);
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.2);
}

.api-card.success:hover {
  box-shadow: 0 8px 20px rgba(103, 194, 58, 0.3);
}

.api-card.error {
  border-color: #f56c6c;
  background: linear-gradient(135deg, #fef0f0 0%, #fde2e2 100%);
  box-shadow: 0 4px 12px rgba(245, 108, 108, 0.2);
}

.api-card.error:hover {
  box-shadow: 0 8px 20px rgba(245, 108, 108, 0.3);
}

/* API卡片头部 */
.api-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.platform-name {
  font-weight: bold;
  font-size: 14px;
  color: #303133;
}

/* URL文本样式 */
.url-text {
  color: #409eff;
  cursor: pointer;
  font-family: monospace;
}

/* 卡片内容 */
.api-card-content {
  padding: 0;
}

/* 结果区域 */
.result-section {
  margin-top: 15px;
}

.result-content {
  max-height: 200px;
  overflow-y: auto;
  background-color: #f8f9fa;
  border-radius: 4px;
  padding: 10px;
}

.result-data {
  margin: 0;
  font-size: 12px;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-all;
}

.no-result {
  color: #909399;
  font-style: italic;
}

/* 错误区域 */
.error-section {
  margin-top: 10px;
}

/* 间距样式 */
.mt10 {
  margin-top: 10px;
}

.mb20 {
  margin-bottom: 20px;
}

/* 响应式布局 */
@media (max-width: 768px) {
  .api-cards-container .el-col {
    margin-bottom: 15px;
  }
}
</style>