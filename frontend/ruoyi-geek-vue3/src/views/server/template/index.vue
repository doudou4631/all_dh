<template>
  <div class="app-container">
    <el-card shadow="never" body-class="search-card">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="queryParams.templateName" placeholder="请输入模板名称" clearable @keyup.enter="handleQuery" />
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
            v-hasPermi="['server:template:add']">新增</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate"
            v-hasPermi="['server:template:edit']">修改</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete"
            v-hasPermi="['server:template:remove']">删除</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="warning" plain icon="Download" @click="handleExport"
            v-hasPermi="['server:template:export']">导出</el-button>
        </el-col>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="templateList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="序号" align="center" type="index" width="50" />
        <el-table-column label="模板名称" align="center" prop="templateName" />
        <el-table-column label="关联平台" align="center" prop="templateInfo">
          <template #default="scope">
            <div v-if="scope.row.templateInfo">
              <el-tag v-for="platform in parseTemplateInfo(scope.row.templateInfo)" :key="platform.id" size="small"
                style="margin: 2px;">
                {{ platform.platformName }}
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
              v-hasPermi="['server:template:edit']">修改</el-button>
            <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)"
              v-hasPermi="['server:template:remove']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize" @pagination="getList" />
    </el-card>

    <!-- 添加或修改查询模板定义对话框 -->
    <el-dialog :title="title" v-model="open" width="45%" append-to-body>
      <el-form ref="templateRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="form.templateName" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="关联平台" prop="templateInfo">
          <div v-loading="platformLoading" style="display: flex; flex-wrap: wrap; min-height: 50px;">
            <el-checkbox-group v-model="selectedPlatformIds">
              <el-checkbox v-for="platform in availablePlatforms" :key="platform.id" :label="platform.id"
                style="margin-right: 15px;">
                {{ platform.platformName }}
              </el-checkbox>
            </el-checkbox-group>
          </div>
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

<script setup name="Template">
import { ref, reactive, toRefs, nextTick } from 'vue';
import { listTemplate, getTemplate, delTemplate, addTemplate, updateTemplate } from "@/api/server/template";
import { listPlatformConfig } from "@/api/server/platformConfig";

const { proxy } = getCurrentInstance();
const { sys_normal_disable } = proxy.useDict('sys_normal_disable');

const templateList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

// 平台选择相关状态
const availablePlatforms = ref([]);
const selectedPlatformIds = ref([]);
const platformLoading = ref(false);

// 加载平台列表
function loadPlatformList() {
  return new Promise((resolve, reject) => {
    platformLoading.value = true;
    listPlatformConfig({ status: '0' }).then(response => {
      // 确保平台 id 为数字类型
      availablePlatforms.value = response.rows.map(p => ({
        ...p,
        id: Number(p.id)
      }));
      platformLoading.value = false;
      resolve(response.rows);
    }).catch(() => {
      platformLoading.value = false;
      reject();
    });
  });
}

function parseTemplateInfo(templateInfo) {
  if (!templateInfo) return [];
  try {
    const platformIds = typeof templateInfo === 'string'
      ? JSON.parse(templateInfo)
      : templateInfo;
    if (!Array.isArray(platformIds)) return [];
    return availablePlatforms.value.filter(p => platformIds.includes(p.id));
  } catch {
    return [];
  }
}

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    templateName: null,
    waitDelay: null,
    templateInfo: null,
    status: null,
  },
  rules: {
    templateName: [
      { required: true, message: "模板名称不能为空", trigger: "blur" }
    ],
    templateInfo: [
      { required: true, message: "模板关联信息", trigger: "blur" }
    ],
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询查询模板定义列表 */
function getList() {
  loading.value = true;
  Promise.all([
    listTemplate(queryParams.value),
    listPlatformConfig({ status: '0' })
  ]).then(([templateRes, platformRes]) => {
    templateList.value = templateRes.rows;
    total.value = templateRes.total;
    availablePlatforms.value = platformRes.rows.map(p => ({
      ...p,
      id: Number(p.id)
    }));
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
    templateName: null,
    waitDelay: null,
    templateInfo: null,
    status: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  };
  // 重置已选平台列表
  selectedPlatformIds.value = [];
  proxy.resetForm("templateRef");
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
  // 确保已选平台为空
  selectedPlatformIds.value = [];
  // 先加载平台列表，确保平台数据准备好后再打开对话框
  loadPlatformList().then(() => {
    nextTick(() => {
      open.value = true;
      title.value = "添加查询模板定义";
    });
  });
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;

  // 先加载平台列表，确保平台数据准备好后再设置选中状态
  loadPlatformList().then(() => {
    getTemplate(_id).then(response => {
      const templateData = response.data;
      form.value = templateData;

      // 解析已选平台 IDs
      if (templateData.templateInfo) {
        try {
          // 后端返回的是 JSON 字符串格式，如 "[1,2,3]"
          const parsed = typeof templateData.templateInfo === 'string'
            ? JSON.parse(templateData.templateInfo)
            : templateData.templateInfo;
          // 确保是数字类型数组 [1,2,3]
          if (Array.isArray(parsed)) {
            selectedPlatformIds.value = parsed.map(id => Number(id));
          }
          console.log('解析后的平台IDs:', selectedPlatformIds.value);
        } catch (e) {
          console.error('解析平台数据失败:', e);
          selectedPlatformIds.value = [];
        }
      }

      // 使用 nextTick 确保数据更新后再打开对话框
      nextTick(() => {
        open.value = true;
        title.value = "修改查询模板定义";
      });
    });
  });
}

/** 提交按钮 */
function submitForm() {
  // 将数组转换为 JSON 字符串格式后提交
  form.value.templateInfo = JSON.stringify(selectedPlatformIds.value);

  proxy.$refs["templateRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateTemplate(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addTemplate(form.value).then(response => {
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
  proxy.$modal.confirm('是否确认删除查询模板定义编号为"' + _ids + '"的数据项？').then(function () {
    return delTemplate(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => { });
}



/** 导出按钮操作 */
function handleExport() {
  proxy.download('server/template/export', {
    ...queryParams.value
  }, `template_${new Date().getTime()}.xlsx`)
}

getList();
</script>
