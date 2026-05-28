<template>
  <div class="app-container">
    <el-card shadow="never" body-class="search-card">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item label="平台名称" prop="platformName">
          <el-input v-model="queryParams.platformName" placeholder="请输入平台名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 160px;">
            <el-option v-for="dict in sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-card shadow="never" class="mt10">
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button type="primary" plain icon="Plus" @click="handleAdd"
            v-hasPermi="['server:platformConfig:add']">新增</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate"
            v-hasPermi="['server:platformConfig:edit']">修改</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete"
            v-hasPermi="['server:platformConfig:remove']">删除</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="warning" plain icon="Download" @click="handleExport"
            v-hasPermi="['server:platformConfig:export']">导出</el-button>
        </el-col>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="platformConfigList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="序号" align="center" prop="id" type="index" width="80" />
        <!-- <el-table-column label="平台ID" align="center" prop="platformId" /> -->
        <el-table-column label="平台名称" align="center" prop="platformName" />
        <el-table-column label="平台URL" align="center" prop="url"  width="400"/>
        <!-- <el-table-column label="请求间隔(ms)" align="center" prop="requestIntervalMs" width="120" />
        <el-table-column label="超时时间(ms)" align="center" prop="timeoutMs" width="120" />
        <el-table-column label="重试次数" align="center" prop="retryCount" width="100" />
        <el-table-column label="并发限制" align="center" prop="concurrencyLimit" width="100" /> -->
        <el-table-column label="前置操作" align="center" prop="preActionType" >
          <template #default="scope">
            <span>{{ ['无', '获取Token'][Number(scope.row.preActionType)] }}</span>
          </template>
        </el-table-column>
        <el-table-column label="显示顺序" align="center" prop="sort"  width="100"/>
        <el-table-column label="状态" align="center" prop="status">
          <template #default="scope">
            <el-switch v-model="scope.row.status" :active-value="'0'" :inactive-value="'1'"
              @change="handleStatusChange(scope.row)" v-hasPermi="['server:platformConfig:edit']" />
          </template>
        </el-table-column>
        <el-table-column label="备注" align="center" prop="remark" width="200" />
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)"
              v-hasPermi="['server:platformConfig:edit']">修改</el-button>
            <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)"
              v-hasPermi="['server:platformConfig:remove']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize" @pagination="getList" />
    </el-card>

    <!-- 添加或修改查询平台url配置对话框 -->
    <el-dialog :title="title" v-model="open" width="800px" append-to-body>
      <el-form ref="platformConfigRef" :model="form" :rules="rules" label-width="100px">
        <!-- <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="平台ID" prop="platformId">
              <el-input v-model="form.platformId" placeholder="请输入平台ID" />
            </el-form-item>
          </el-col>
        </el-row> -->
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="平台名称" prop="platformName">
              <el-input v-model="form.platformName" placeholder="请输入平台名称" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio v-for="dict in sys_normal_disable" :key="dict.value" :label="dict.value">{{ dict.label
                }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
           <el-col :span="6">
            <el-form-item label="显示顺序" prop="sort">
              <el-input-number 
                v-model="form.sort" 
                :min="0" 
                :max="999" 
                :step="1"
                controls-position="right"
                placeholder="请输入显示顺序" 
                style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="平台URL" prop="url">
              <el-input v-model="form.url" type="textarea" placeholder="请输入内容" rows="2" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
             <el-form-item label="前置操作类型" prop="preActionType">
              <el-select v-model="form.preActionType" placeholder="请选择前置操作类型" style="width: 100%;">
                <el-option label="无" value="0" />
                <el-option label="获取Token" value="1" />
                <!-- <el-option label="获取Token" value="2" /> -->
                <!-- <el-option label="执行JS" value="3" /> -->
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <!-- <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="请求间隔(ms)" prop="requestIntervalMs">
              <el-input-number v-model="form.requestIntervalMs" :min="0" :step="100" placeholder="默认1000ms"
                style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="超时时间(ms)" prop="timeoutMs">
              <el-input-number v-model="form.timeoutMs" :min="1000" :step="500" placeholder="请输入超时时间"
                style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="重试次数" prop="retryCount">
              <el-input-number v-model="form.retryCount" :min="0" :max="10" :step="1" placeholder="请输入重试次数"
                style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row> -->
        <el-row :gutter="20">
          <!-- <el-col :span="8">
            <el-form-item label="并发限制" prop="concurrencyLimit">
              <el-input-number v-model="form.concurrencyLimit" :min="1" :step="1" placeholder="请输入并发限制"
                style="width: 100%;" />
            </el-form-item>
          </el-col> -->
          <!-- <el-col :span="24">
            <el-form-item label="前置操作类型" prop="preActionType">
              <el-select v-model="form.preActionType" placeholder="请选择前置操作类型" style="width: 100%;">
                <el-option label="无" value="0" />
                <el-option label="获取Token" value="1" />
              </el-select>
            </el-form-item>
          </el-col> -->
        </el-row>
        <el-row :gutter="20" v-if="form.preActionType === '1'">
          <el-col :span="24">
            <el-form-item label="前置操作配置" prop="preActionConfig">
              <el-input v-model="form.preActionConfig" type="textarea" placeholder="JSON格式配置" rows="3" />
            </el-form-item>
          </el-col>
        </el-row>
        <!-- <el-row :gutter="20" v-if="form.preActionType === '1'">
          <el-col :span="24">
            <el-form-item label="请求头模板" prop="headersTemplate">
              <el-input v-model="form.headersTemplate" type="textarea" placeholder="JSON格式配置" rows="3" />
            </el-form-item>
          </el-col>
        </el-row> -->
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" rows="2" />
            </el-form-item>
          </el-col>
        </el-row>
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

<script setup name="PlatformConfig">
import { listPlatformConfig, getPlatformConfig, delPlatformConfig, addPlatformConfig, updatePlatformConfig } from "@/api/server/platformConfig";

const { proxy } = getCurrentInstance();
const { sys_normal_disable } = proxy.useDict('sys_normal_disable');

const platformConfigList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    platformId: null,
    platformName: null,
    url: null,
    status: null,
    sort: null
  },
  rules: {
    // platformId: [
    //   { required: true, message: "平台ID不能为空", trigger: "blur" }
    // ],
    platformName: [
      { required: true, message: "平台名称不能为空", trigger: "blur" }
    ],
    url: [
      { required: true, message: "平台URL不能为空", trigger: "blur" }
    ],
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询查询平台url配置列表 */
function getList() {
  loading.value = true;
  listPlatformConfig(queryParams.value).then(response => {
    platformConfigList.value = response.rows;
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
    platformId: null,
    platformName: null,
    url: null,
    requestIntervalMs: 1000, // 默认1秒
    timeoutMs: null,
    retryCount: 0,
    preActionType: '0', // 保持字符串类型与下拉选项一致
    preActionConfig: null,
    headersTemplate: null,
    concurrencyLimit: 1,
    sort: 0, // 默认显示顺序为0
    status: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  };
  proxy.resetForm("platformConfigRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
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
  title.value = "添加查询平台url配置";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value
  getPlatformConfig(_id).then(response => {
    // 确保前置操作类型为字符串类型，与下拉选项值类型一致
    const data = response.data;
    if (data.preActionType !== null && data.preActionType !== undefined) {
      data.preActionType = String(data.preActionType);
    }
    // 确保sort字段有正确的数值，如果为null或undefined则设为0
    if (data.sort === null || data.sort === undefined || data.sort === '') {
      data.sort = 0;
    } else {
      data.sort = Number(data.sort);
    }
    form.value = data;
    open.value = true;
    title.value = "修改查询平台url配置";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["platformConfigRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updatePlatformConfig(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addPlatformConfig(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}

/** 状态切换操作 */
function handleStatusChange(row) {
  updatePlatformConfig(row).then(response => {
    proxy.$modal.msgSuccess("状态修改成功");
  }).catch(() => {
    getList(); // 失败时重新加载数据恢复原始状态
    proxy.$modal.msgError("状态修改失败");
  });
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除查询平台url配置编号为"' + _ids + '"的数据项？').then(function () {
    return delPlatformConfig(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => { });
}



/** 导出按钮操作 */
function handleExport() {
  proxy.download('server/platformConfig/export', {
    ...queryParams.value
  }, `platformConfig_${new Date().getTime()}.xlsx`)
}

getList();
</script>
