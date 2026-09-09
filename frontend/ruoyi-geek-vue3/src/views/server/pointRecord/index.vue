<template>
  <div class="app-container">
    <el-card shadow="never" body-class="search-card">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item label="用户名称" prop="userId">
          <el-select v-model="queryParams.userId" placeholder="请选择用户" filterable clearable :loading="userLoading">
            <el-option v-for="user in userList" :key="user.userId" :label="user.nickName || user.userName"
              :value="user.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="变动类型" prop="pointType">
          <el-select v-model="queryParams.pointType" placeholder="请选择变动类型" clearable style="width: 160px;">
            <el-option v-for="dict in oper_score_type" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="变动原因" prop="reason">
          <el-input v-model="queryParams.reason" placeholder="请输入变动原因" clearable @keyup.enter="handleQuery" />
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
            v-hasPermi="['server:pointRecord:add']">新增</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate"
            v-hasPermi="['server:pointRecord:edit']">修改</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete"
            v-hasPermi="['server:pointRecord:remove']">删除</el-button>
        </el-col> -->
        <el-col :span="1.5">
          <el-button type="warning" plain icon="Download" @click="handleExport"
            v-hasPermi="['server:pointRecord:export']">导出</el-button>
        </el-col>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="pointRecordList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="序号" align="center" prop="id" type="index" width="50" />
        <el-table-column label="用户名称" align="center" prop="userId">
          <template #default="scope">
            <span>{{ userMap[scope.row.userId] || scope.row.userId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="积分变动金额" align="center" prop="pointAmount">
          <template #default="scope">
            <span :style="{
              color: scope.row.pointType === '1' ? '#67C23A' : '#F56C6C',
              fontWeight: 'bold'
            }">
              {{ scope.row.pointType === '1' ? '+' : '' }}{{ scope.row.pointAmount }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="变动类型" align="center" prop="pointType">
          <template #default="scope">
            <dict-tag :options="oper_score_type" :value="scope.row.pointType" />
          </template>
        </el-table-column>
        <el-table-column label="变动原因" align="center" prop="reason" />
        <!-- <el-table-column label="操作人ID" align="center" prop="operatorId" /> -->
        <el-table-column label="操作人" align="center" prop="createBy" />
        <el-table-column label="备注" align="center" prop="remark" />
        <!-- <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)"
              v-hasPermi="['server:pointRecord:edit']">修改</el-button>
            <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)"
              v-hasPermi="['server:pointRecord:remove']">删除</el-button>
          </template>
        </el-table-column> -->
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize" @pagination="getList" />
    </el-card>

    <!-- 添加或修改积分流水记录对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="pointRecordRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户ID" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入用户ID" />
        </el-form-item>
        <el-form-item label="积分变动金额" prop="pointAmount">
          <el-input v-model="form.pointAmount" placeholder="请输入积分变动金额" />
        </el-form-item>
        <el-form-item label="变动类型" prop="pointType">
          <el-select v-model="form.pointType" placeholder="请选择变动类型">
            <el-option v-for="dict in oper_score_type" :key="dict.value" :label="dict.label"
              :value="dict.value"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="变动原因" prop="reason">
          <el-input v-model="form.reason" placeholder="请输入变动原因" />
        </el-form-item>
        <el-form-item label="操作人ID" prop="operatorId">
          <el-input v-model="form.operatorId" placeholder="请输入操作人ID" />
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

<script setup name="PointRecord">
import { listPointRecord, getPointRecord, delPointRecord, addPointRecord, updatePointRecord } from "@/api/server/pointRecord";
import { listUser } from "@/api/system/user";

const { proxy } = getCurrentInstance();
const { oper_score_type } = proxy.useDict('oper_score_type');

const pointRecordList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

// 用户相关状态
const userList = ref([]);
const userLoading = ref(false);
const userMap = ref({});


const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: null,
    pointAmount: null,
    pointType: null,
    reason: null,
    operatorId: null,
  },
  rules: {
    userId: [
      { required: true, message: "用户ID不能为空", trigger: "blur" }
    ],
    pointAmount: [
      { required: true, message: "积分变动金额不能为空", trigger: "blur" }
    ],
    pointType: [
      { required: true, message: "变动类型不能为空", trigger: "change" }
    ],
    reason: [
      { required: true, message: "变动原因不能为空", trigger: "blur" }
    ],
  }
});

const { queryParams, form, rules } = toRefs(data);

// 初始化用户列表
function initUserList() {
  userLoading.value = true;
  // 构建搜索参数，加载所有用户
  const searchParams = {
    pageNum: 1,
    pageSize: 9999
  };
  listUser(searchParams).then(response => {
    // 只提取需要的字段
    userList.value = (response.rows || []).map(user => ({
      userId: user.userId,
      nickName: user.nickName || user.userName
    }));
    userLoading.value = false;
  }).catch(() => {
    userList.value = [];
    userLoading.value = false;
  });
}

// 加载用户信息并构建用户映射
function loadUserInfo() {
  return new Promise((resolve, reject) => {
    listUser({ pageSize: 100 }).then(response => {
      const users = response.rows || [];
      const map = {};
      users.forEach(user => {
        map[user.userId] = user.nickName || user.userName;
      });
      userMap.value = map;
      resolve(map);
    }).catch(() => {
      userMap.value = {};
      resolve({});
    });
  });
}

/** 查询积分流水记录列表 */
function getList() {
  loading.value = true;
  Promise.all([
    listPointRecord(queryParams.value),
    loadUserInfo(),
    initUserList()
  ]).then(([recordRes, userMap, userList]) => {
    pointRecordList.value = recordRes.rows;
    total.value = recordRes.total;
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
    userId: null,
    pointAmount: null,
    pointType: null,
    reason: null,
    operatorId: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    remark: null
  };
  proxy.resetForm("pointRecordRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  // 确保只有当userId是数字时才发送，避免类型转换错误
  if (queryParams.value.userId && typeof queryParams.value.userId !== 'number' && isNaN(Number(queryParams.value.userId))) {
    queryParams.value.userId = null;
  }
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
  title.value = "添加积分流水记录";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value
  getPointRecord(_id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改积分流水记录";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["pointRecordRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updatePointRecord(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addPointRecord(form.value).then(response => {
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
  proxy.$modal.confirm('是否确认删除积分流水记录编号为"' + _ids + '"的数据项？').then(function () {
    return delPointRecord(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => { });
}



/** 导出按钮操作 */
function handleExport() {
  proxy.download('server/pointRecord/export', {
    ...queryParams.value
  }, `pointRecord_${new Date().getTime()}.xlsx`)
}

getList();
</script>
