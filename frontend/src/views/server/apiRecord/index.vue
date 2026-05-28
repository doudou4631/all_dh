<template>
  <div class="app-container">
    <el-card shadow="never" body-class="search-card">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item label="查询类型" prop="queryType">
          <el-select v-model="queryParams.queryType" placeholder="请选择查询类型" clearable style="width: 160px;">
            <el-option v-for="dict in use_exec_type" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="请求状态" prop="requestStatus">
          <el-select v-model="queryParams.requestStatus" placeholder="请选择请求状态" clearable style="width: 160px;">
            <el-option v-for="dict in sys_common_status" :key="dict.value" :label="dict.label" :value="dict.value" />
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
        <!-- <el-col :span="1.5">
          <el-button type="primary" plain icon="Plus" @click="handleAdd"
            v-hasPermi="['server:apiRecord:add']">新增</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate"
            v-hasPermi="['server:apiRecord:edit']">修改</el-button>
        </el-col> -->
        <!-- <el-col :span="1.5">
          <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete"
            v-hasPermi="['server:apiRecord:remove']">删除</el-button>
        </el-col> -->
        <el-col :span="1.5">
          <el-button type="warning" plain icon="Download" @click="handleExport"
            v-hasPermi="['server:apiRecord:export']">导出</el-button>
        </el-col>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="apiRecordList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="序号" align="center" prop="id" type="index" width="50" />
        <el-table-column label="查询类型" align="center" prop="queryType">
          <template #default="scope">
            <dict-tag :options="use_exec_type" :value="scope.row.queryType" />
          </template>
        </el-table-column>
        <el-table-column label="请求状态" align="center" prop="requestStatus">
          <template #default="scope">
            <dict-tag :options="sys_common_status" :value="scope.row.requestStatus" />
          </template>
        </el-table-column>
        <!-- 查询平台 -->
        <el-table-column label="查询号码" align="center" prop="phone" />
        <el-table-column label="查询平台" align="center" prop="platformName" />
        <el-table-column label="请求耗时(ms)" align="center" prop="requestTime" />
        <el-table-column label="操作用户" align="center" prop="createBy" />
        <el-table-column label="操作时间" align="center" prop="createTime" />
        <el-table-column label="结果" align="center" prop="results" />
        <!-- <el-table-column label="请求参数" align="center" prop="requestParams" width="250">
          <template #default="scope">
            <el-tooltip v-if="scope.row.requestParams" :content="scope.row.requestParams" placement="top">
              <span>{{ scope.row.requestParams.substring(0, 30) + (scope.row.requestParams.length > 30 ? '...' : '') }}</span>
            </el-tooltip>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="响应结果" align="center" prop="responseResult" width="250">
          <template #default="scope">
            <el-tooltip v-if="scope.row.responseResult" :content="scope.row.responseResult" placement="top">
              <span>{{ scope.row.responseResult.substring(0, 30) + (scope.row.responseResult.length > 30 ? '...' : '') }}</span>
            </el-tooltip>
            <span v-else>-</span>
          </template>
        </el-table-column> -->
        <el-table-column label="备注" align="center" prop="remark" />
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
import { is } from "bpmn-js/lib/util/ModelUtil";

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

getList();
</script>
