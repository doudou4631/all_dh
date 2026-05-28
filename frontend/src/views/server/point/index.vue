<template>
  <div class="app-container">
    <el-card shadow="never" body-class="search-card">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item label="用户名称" prop="userId">
          <el-input v-model="queryParams.userId" placeholder="请输入用户ID" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="积分余额" prop="pointBalance">
          <el-input v-model="queryParams.pointBalance" placeholder="请输入积分余额" clearable @keyup.enter="handleQuery" />
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
            v-hasPermi="['server:point:add']">新增</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate"
            v-hasPermi="['server:point:edit']">修改</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete"
            v-hasPermi="['server:point:remove']">删除</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="warning" plain icon="Download" @click="handleExport"
            v-hasPermi="['server:point:export']">导出</el-button>
        </el-col>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="pointList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="序号" align="center" prop="id" type="index" width="50" />
        <el-table-column label="用户名称" align="center" prop="userId" />
        <el-table-column label="积分余额" align="center" prop="pointBalance" />
        <el-table-column label="备注" align="center" prop="remark" />
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)"
              v-hasPermi="['server:point:edit']">修改</el-button>
            <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)"
              v-hasPermi="['server:point:remove']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize" @pagination="getList" />
    </el-card>

    <!-- 添加或修改用户积分对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="pointRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户ID" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入用户ID" />
        </el-form-item>
        <el-form-item label="积分余额" prop="pointBalance">
          <el-input v-model="form.pointBalance" placeholder="请输入积分余额" />
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

<script setup name="Point">
import { listPoint, getPoint, delPoint, addPoint, updatePoint } from "@/api/server/point";

const { proxy } = getCurrentInstance();

const pointList = ref([]);
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
    userId: null,
    pointBalance: null,
  },
  rules: {
    userId: [
      { required: true, message: "用户ID不能为空", trigger: "blur" }
    ],
    pointBalance: [
      { required: true, message: "积分余额不能为空", trigger: "blur" }
    ],
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询用户积分列表 */
function getList() {
  loading.value = true;
  listPoint(queryParams.value).then(response => {
    pointList.value = response.rows;
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
    userId: null,
    pointBalance: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  };
  proxy.resetForm("pointRef");
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
  title.value = "添加用户积分";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value
  getPoint(_id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改用户积分";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["pointRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updatePoint(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addPoint(form.value).then(response => {
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
  proxy.$modal.confirm('是否确认删除用户积分编号为"' + _ids + '"的数据项？').then(function () {
    return delPoint(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => { });
}



/** 导出按钮操作 */
function handleExport() {
  proxy.download('server/point/export', {
    ...queryParams.value
  }, `point_${new Date().getTime()}.xlsx`)
}

getList();
</script>
