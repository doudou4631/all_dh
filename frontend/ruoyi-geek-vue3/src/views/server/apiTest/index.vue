<template>
  <div class="app-container">
    <el-card shadow="never" body-class="search-card">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item label="请求状态" prop="requestStatus">
          <el-select v-model="queryParams.requestStatus" placeholder="请选择请求状态" clearable style="width: 160px;">
            <el-option v-for="dict in sys_common_status" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作用户" prop="createBy">
          <el-input v-model="queryParams.createBy" placeholder="请输入操作用户" clearable style="width: 240px;" />
        </el-form-item>
        <el-form-item label="平台名称" prop="platformId">
          <el-select v-model="queryParams.platformId" placeholder="请选择平台名称" clearable style="width: 160px;">
            <el-option v-for="platform in enabledPlatforms" :key="platform.id" :label="platform.platformName" :value="platform.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 左右各两个模块区域 -->
    <el-row :gutter="20" class="mt10">
      <!-- 左侧模块1：API列表 -->
      <el-col :span="12">
        <el-card shadow="never" class="api-list-card" v-loading="platformLoading">
          <template #header>
            <div class="card-header">
              <span>API列表</span>
            </div>
          </template>
          <div class="platform-list">
            <div v-for="item in platformConfigList" :key="item.id" class="platform-item"
              :class="{ active: selectedPlatform?.id === item.id }" @click="handlePlatformSelect(item)">
              <span class="platform-name">{{ item.platformName }}</span>
              <el-tag v-if="item.status === '0'" type="success" size="small">启用</el-tag>
              <el-tag v-else type="danger" size="small">禁用</el-tag>
            </div>
            <el-empty v-if="platformConfigList.length === 0" description="暂无平台数据" />
          </div>
        </el-card>
      </el-col>
      <!-- 右侧模块2：API调用参数 -->
      <el-col :span="12">
        <el-card shadow="never" class="api-params-card">
          <template #header>
            <div class="card-header">
              <span>{{ selectedPlatform ? selectedPlatform.platformName + ' - 调用参数' : 'API调用参数' }}</span>
            </div>
          </template>
          <div v-if="selectedPlatform" class="platform-params">
            <el-form label-width="100px" class="param-form">
              <el-form-item label="平台URL">
                <el-input v-model="selectedPlatform.url" readonly />
              </el-form-item>
              <!-- <el-form-item label="请求间隔">
                <el-input :value="selectedPlatform.requestIntervalMs + ' ms'"  />
              </el-form-item>
              <el-form-item label="超时时间">
                <el-input :value="selectedPlatform.timeoutMs + ' ms'"  />
              </el-form-item>
              <el-form-item label="重试次数">
                <el-input v-model="selectedPlatform.retryCount"  />
              </el-form-item>
              <el-form-item label="并发限制">
                <el-input v-model="selectedPlatform.concurrencyLimit"  />
              </el-form-item> -->
              <el-form-item label="前置操作">
                <el-input :value="['无', '获取Token'][Number(selectedPlatform.preActionType)]" readonly />
              </el-form-item>
              <el-form-item label="请求参数" v-if="selectedPlatform.preActionType !== 0">
                <el-input v-model="selectedPlatform.preActionConfig" type="textarea" :rows="4" placeholder="请输入请求参数(JSON格式)" readonly/>
              </el-form-item>
              <el-form-item label="查询号码" prop="phoneNumber">
                <el-input v-model="phoneNumber" placeholder="请输入查询号码" clearable />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" icon="Position" @click="handleApiTest">发送请求</el-button>
                <el-button icon="RefreshRight" @click="clearParams">清空</el-button>
              </el-form-item>
              
              <!-- 请求结果显示区域 -->
              <el-form-item v-if="getCurrentPlatformResult()">
                <el-card shadow="never" class="result-card">
                  <template #header>
                    <div class="result-header">
                      <span>查询结果</span>
                      <el-tag :type="getCurrentPlatformResult().data?.success ? 'success' : 'danger'">
                        {{ getCurrentPlatformResult().data?.success ? '成功' : '失败' }}
                      </el-tag>
                    </div>
                  </template>
                  <div class="result-content">
                    <el-descriptions :column="1" border>
                      <el-descriptions-item label="查询号码">{{ getCurrentPlatformResult().data?.phone || '-' }}</el-descriptions-item>
                    </el-descriptions>
                    <div v-if="getCurrentPlatformResult().data?.platformResults && getCurrentPlatformResult().data.platformResults.length > 0" class="platform-results">
                      <h4>查询结果</h4>
                      <el-table :data="getCurrentPlatformResult().data.platformResults" size="small">
                        <el-table-column label="平台名称" prop="platform" />
                        <el-table-column label="状态" prop="status" />
                      </el-table>
                    </div>
                  </div>
                </el-card>
              </el-form-item>
            </el-form>
          </div>
          <el-empty v-else description="请先选择左侧平台" />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="mt10">
      <!-- <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button type="primary" plain icon="Plus" @click="handleAdd"
            v-hasPermi="['server:apiRecord:add']">新增</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate"
            v-hasPermi="['server:apiRecord:edit']">修改</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete"
            v-hasPermi="['server:apiRecord:remove']">删除</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="warning" plain icon="Download" @click="handleExport"
            v-hasPermi="['server:apiRecord:export']">导出</el-button>
        </el-col>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row> -->

      <el-table v-loading="loading" :data="apiRecordList" @selection-change="handleSelectionChange">
        <!-- <el-table-column type="selection" width="55" align="center" /> -->
        <el-table-column label="序号" align="center" prop="id" type="index" width="50" />
        <el-table-column label="查询类型" align="center" prop="queryType">
          <template #default="scope">
            <dict-tag :options="use_exec_type" :value="scope.row.queryType" />
          </template>
        </el-table-column>
        <el-table-column label="平台名称" align="center" prop="platformName" />
        <el-table-column label="请求状态" align="center" prop="requestStatus">
          <template #default="scope">
            <dict-tag :options="sys_common_status" :value="scope.row.requestStatus" />
          </template>
        </el-table-column>
        <el-table-column label="请求耗时" align="center" prop="requestTime" />
        <el-table-column label="操作用户ID" align="center" prop="userId" />
        <el-table-column label="操作用户" align="center" prop="createBy" />
        <el-table-column label="查询号码" align="center" prop="phone" />
        <el-table-column label="请求参数" align="center" prop="requestParams" />
        <el-table-column label="响应结果" align="center" prop="responseResult" />
        <!-- <el-table-column label="备注" align="center" prop="remark" /> -->
        <!-- <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)"
              v-hasPermi="['server:apiRecord:edit']">修改</el-button>
            <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)"
              v-hasPermi="['server:apiRecord:remove']">删除</el-button>
          </template>
        </el-table-column> -->
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize" @pagination="getList" />
    </el-card>

    <!-- 添加或修改接口查询记录通用对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="apiRecordRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="查询类型" prop="queryType">
          <el-select v-model="form.queryType" placeholder="请选择查询类型">
            <el-option v-for="dict in use_exec_type" :key="dict.value" :label="dict.label"
              :value="dict.value"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="请求状态" prop="requestStatus">
          <el-radio-group v-model="form.requestStatus">
            <el-radio v-for="dict in sys_common_status" :key="dict.value" :label="dict.value">{{ dict.label
            }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="请求耗时" prop="requestTime">
          <el-input v-model="form.requestTime" placeholder="请输入请求耗时" />
        </el-form-item>
        <el-form-item label="操作用户ID" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入操作用户ID" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ApiRecord">
import { listApiRecord, getApiRecord, delApiRecord, addApiRecord, updateApiRecord } from "@/api/server/apiRecord";
import { listPlatformConfig } from "@/api/server/platformConfig";
import { singleApi } from "@/api/server/apiServer";


const { proxy } = getCurrentInstance();
const { use_exec_type, sys_common_status } = proxy.useDict('use_exec_type', 'sys_common_status');

const apiRecordList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

// 平台配置相关变量
const platformConfigList = ref([]);
const platformLoading = ref(false);
const selectedPlatform = ref(null);
const apiRequestParams = ref('');
const phoneNumber = ref('');
const apiResults = ref({}); // 存储每个平台的结果

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    queryType: null,
    requestStatus: null,
    requestTime: null,
    userId: null,
    requestParams: null,
    responseResult: null,
    platformId: null,
    createBy: null,
  },
  rules: {
    queryType: [
      { required: true, message: "查询类型不能为空", trigger: "change" }
    ],
    requestStatus: [
      { required: true, message: "请求状态不能为空", trigger: "change" }
    ],
    requestTime: [
      { required: true, message: "请求耗时不能为空", trigger: "blur" }
    ],
    userId: [
      { required: true, message: "操作用户ID不能为空", trigger: "blur" }
    ],
    requestParams: [
      { required: true, message: "请求参数不能为空", trigger: "blur" }
    ],
  }
});

const { queryParams, form, rules } = toRefs(data);

// 计算属性：获取启用的平台列表
const enabledPlatforms = computed(() => {
  return platformConfigList.value.filter(platform => platform.status === '0');
});

/** 查询接口查询记录通用列表 */
function getList() {
  loading.value = true;
  listApiRecord(queryParams.value).then(response => {
    apiRecordList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

// 取消按钮
function cancel() {
  open.value = false;
  reset();
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    queryType: null,
    requestStatus: null,
    requestTime: null,
    userId: null,
    requestParams: null,
    responseResult: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  };
  proxy.resetForm("apiRecordRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  // 清空所有平台的调测结果
  apiResults.value = {};
  handleQuery();
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加接口查询记录通用";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value
  getApiRecord(_id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改接口查询记录通用";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["apiRecordRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateApiRecord(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addApiRecord(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除接口查询记录通用编号为"' + _ids + '"的数据项？').then(function () {
    return delApiRecord(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => { });
}



/** 导出按钮操作 */
function handleExport() {
  proxy.download('server/apiRecord/export', {
    ...queryParams.value
  }, `apiRecord_${new Date().getTime()}.xlsx`)
}

/** 查询平台配置列表 */
function getPlatformList() {
  platformLoading.value = true;
  listPlatformConfig({ pageNum: 1, pageSize: 100 }).then(response => {
    platformConfigList.value = response.rows;
    platformLoading.value = false;
  }).catch(() => {
    platformLoading.value = false;
  });
}

/** 选择平台 */
function handlePlatformSelect(platform) {
  selectedPlatform.value = platform;
  // 清空之前的请求参数
  apiRequestParams.value = '';
}

/** 获取当前平台的结果 */
function getCurrentPlatformResult() {
  if (selectedPlatform.value?.id) {
    return apiResults.value[selectedPlatform.value.id];
  }
  return null;
}

/** 发送API测试请求 */
function handleApiTest() {
  if (!selectedPlatform.value) {
    proxy.$modal.msgError("请先选择一个平台");
    return;
  }
  if (!phoneNumber.value) {
    proxy.$modal.msgError("请输入查询号码");
    return;
  }
    // 构建请求参数
  const requestData = {
    platformId: selectedPlatform.value.id,
    platformName: selectedPlatform.value.platformName,
    url: selectedPlatform.value.url,
    requestIntervalMs: selectedPlatform.value.requestIntervalMs,
    timeoutMs: selectedPlatform.value.timeoutMs,
    retryCount: selectedPlatform.value.retryCount,
    preActionType: selectedPlatform.value.preActionType,
    preActionConfig: selectedPlatform.value.preActionConfig,
    headersTemplate: selectedPlatform.value.headersTemplate,
    concurrencyLimit: selectedPlatform.value.concurrencyLimit,
    phoneNumber: phoneNumber.value
  };
  
  // 调用API
  singleApi(requestData).then(response => {
    // 将结果存储到对应平台
    if (selectedPlatform.value?.id) {
      apiResults.value[selectedPlatform.value.id] = response;
    }
    proxy.$modal.msgSuccess("请求发送成功");
    // 刷新列表
    getList();
  }).catch(error => {
    proxy.$modal.msgError("请求发送失败：" + (error.message || "未知错误"));
  });
}

/** 清空参数 */
function clearParams() {
  apiRequestParams.value = '';
  phoneNumber.value = '';
  // 清空当前平台的结果
  if (selectedPlatform.value?.id) {
    apiResults.value[selectedPlatform.value.id] = null;
  }
}

getList();
getPlatformList();
</script>

<style scoped>
/* 卡片头部样式 */
.card-header {
  font-weight: bold;
  font-size: 16px;
}

/* API列表卡片样式 */
.api-list-card {
  height: 100%;
  min-height: 400px;
}

/* API参数卡片样式 */
.api-params-card {
  height: 100%;
}

/* 平台列表样式 */
.platform-list {
  max-height: 500px;
  overflow-y: auto;
}

/* 平台项样式 */
.platform-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  margin-bottom: 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid #e4e7ed;
}

.platform-item:hover {
  background-color: #f5f7fa;
  border-color: #c6e2ff;
}

.platform-item.active {
  background-color: #ecf5ff;
  border-color: #409eff;
}

/* 平台名称样式 */
.platform-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

/* 间距样式 */
.mt10 {
  margin-top: 10px;
}

/* 参数表单样式 */
.param-form :deep(.el-input__inner) {
  background-color: #f5f7fa;
}

/* 结果显示区域样式 */
.result-card {
  margin-top: 10px;
  width: 100%;
  background: linear-gradient(135deg, #f6f9fc 0%, #e9f3ff 100%);
  border: 1px solid #d4e4ff;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  background: linear-gradient(135deg, #4a90e2 0%, #357abd 100%);
  color: white;
  padding: 12px 16px;
  margin: -12px -16px 10px -16px;
  border-radius: 4px 4px 0 0;
}

.result-content {
  width: 100%;
}

.platform-results {
  margin-top: 15px;
  width: 100%;
  background-color: #f8fbff;
  border-radius: 6px;
  padding: 12px;
  border: 1px solid #e1f0ff;
}

.platform-results h4 {
  margin: 0 0 10px 0;
  font-size: 14px;
  font-weight: bold;
  color: #2c5aa0;
  border-bottom: 2px solid #4a90e2;
  padding-bottom: 5px;
}

/* 平台结果表格样式 */
.platform-results :deep(.el-table) {
  background-color: transparent;
}

.platform-results :deep(.el-table th) {
  background-color: #e1f0ff;
  color: #2c5aa0;
  font-weight: bold;
}

.platform-results :deep(.el-table td) {
  background-color: #f8fbff;
}

.platform-results :deep(.el-table--border) {
  border: 1px solid #d4e4ff;
}

.platform-results :deep(.el-table--border::after) {
  background-color: #d4e4ff;
}
</style>
