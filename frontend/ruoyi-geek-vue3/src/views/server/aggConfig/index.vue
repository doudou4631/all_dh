<template>
  <div class="app-container">
    <el-card shadow="never" body-class="search-card">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item label="聚合名称" prop="aggregateName">
          <el-input v-model="queryParams.aggregateName" placeholder="请输入聚合名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="sid" prop="sid">
          <el-input v-model="queryParams.sid" placeholder="请输入sid" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="sKey" prop="sKey">
          <el-input v-model="queryParams.sKey" placeholder="请输入sKey" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
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
            v-hasPermi="['server:aggConfig:add']">新增</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate"
            v-hasPermi="['server:aggConfig:edit']">修改</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete"
            v-hasPermi="['server:aggConfig:remove']">删除</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="warning" plain icon="Download" @click="handleExport"
            v-hasPermi="['server:aggConfig:export']">导出</el-button>
        </el-col>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="aggConfigList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="序号" align="center" type="index" width="50" />
        <el-table-column label="聚合名称" align="center" prop="aggregateName" />
        <el-table-column label="sid" align="center" prop="sid" />
        <el-table-column label="sKey" align="center" prop="sKey" />
        <el-table-column label="其他通用配置参数" align="center" prop="configParams" />
        <el-table-column label="绑定模板" align="center" prop="configParams">
          <template #default="scope">
            <div v-if="scope.row.configParams">
              <el-tag size="small" style="margin: 2px;">
                {{ getTemplateName(scope.row.configParams) }}
              </el-tag>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" prop="status">
          <template #default="scope">
            <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column label="备注" align="center" prop="remark" />
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)"
              v-hasPermi="['server:aggConfig:edit']">修改</el-button>
            <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)"
              v-hasPermi="['server:aggConfig:remove']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize" @pagination="getList" />
    </el-card>

    <!-- 添加或修改聚合配置对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="aggConfigRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="聚合名称" prop="aggregateName">
          <el-input v-model="form.aggregateName" placeholder="请输入聚合名称" />
        </el-form-item>
        <el-form-item label="sid" prop="sid">
          <el-input v-model="form.sid" placeholder="请输入sid" />
        </el-form-item>
        <el-form-item label="sKey" prop="sKey">
          <el-input v-model="form.sKey" placeholder="请输入sKey" />
        </el-form-item>
        <el-form-item label="绑定模板" prop="configParams">
          <el-select v-model="form.configParams" placeholder="请选择模板" clearable>
            <el-option v-for="template in templateList" :key="template.id" :label="template.templateName"
              :value="template.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in sys_normal_disable" :key="dict.value" :label="dict.value">{{ dict.label
            }}</el-radio>
          </el-radio-group>
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

<script setup name="AggConfig">
import { listAggConfig, getAggConfig, delAggConfig, addAggConfig, updateAggConfig } from "@/api/server/aggConfig";
import { listTemplate } from "@/api/server/template";

const { proxy } = getCurrentInstance();
const { sys_normal_disable } = proxy.useDict('sys_normal_disable');

const aggConfigList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

// 模板选择相关状态
const templateList = ref([]);
const templateLoading = ref(false);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    aggregateName: null,
    sid: null,
    sKey: null,
    configParams: null,
    status: null,
  },
  rules: {
    aggregateName: [
      { required: true, message: "聚合名称不能为空", trigger: "blur" }
    ],
    sid: [
      { required: true, message: "sid不能为空", trigger: "blur" }
    ],
    sKey: [
      { required: true, message: "sKey不能为空", trigger: "blur" }
    ],
  }
});

const { queryParams, form, rules } = toRefs(data);

// 加载模板列表
function loadTemplateList() {
  return new Promise((resolve, reject) => {
    templateLoading.value = true;
    listTemplate({ status: '0' }).then(response => {
      templateList.value = response.rows;
      templateLoading.value = false;
      resolve(response.rows);
    }).catch(() => {
      templateLoading.value = false;
      reject();
    });
  });
}

// 根据模板ID获取模板名称
function getTemplateName(templateId) {
  const template = templateList.value.find(t => t.id === templateId);
  return template ? template.templateName : '';
}

/** 查询聚合配置列表 */
function getList() {
  loading.value = true;
  Promise.all([
    listAggConfig(queryParams.value),
    listTemplate({ status: '0' })
  ]).then(([aggConfigRes, templateRes]) => {
    aggConfigList.value = aggConfigRes.rows;
    total.value = aggConfigRes.total;
    templateList.value = templateRes.rows;
    loading.value = false;
  }).catch(() => {
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
    aggregateName: null,
    sid: null,
    sKey: null,
    configParams: null,
    status: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  };
  proxy.resetForm("aggConfigRef");
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
  // 加载模板列表
  loadTemplateList().then(() => {
    open.value = true;
    title.value = "添加聚合配置";
  });
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value
  // 先加载模板列表，再获取聚合配置
  loadTemplateList().then(() => {
    getAggConfig(_id).then(response => {
      form.value = response.data;
      open.value = true;
      title.value = "修改聚合配置";
    });
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["aggConfigRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateAggConfig(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addAggConfig(form.value).then(response => {
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
  proxy.$modal.confirm('是否确认删除聚合配置编号为"' + _ids + '"的数据项？').then(function () {
    return delAggConfig(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => { });
}



/** 导出按钮操作 */
function handleExport() {
  proxy.download('server/aggConfig/export', {
    ...queryParams.value
  }, `aggConfig_${new Date().getTime()}.xlsx`)
}

getList();
</script>
