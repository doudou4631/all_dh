<template>
   <div class="app-container">
      <el-row :gutter="20">
         <!--部门数据-->
         <!-- <el-col :span="4" :xs="24">
            <el-card shadow="never">
               <div class="head-container">
                  <el-input v-model="deptName" placeholder="请输入部门名称" clearable prefix-icon="Search"
                     style="margin-bottom: 20px" />
               </div>
               <div class="head-container">
                  <el-tree :data="deptOptions" :props="{ label: 'label', children: 'children' }"
                     :expand-on-click-node="false" :filter-node-method="filterNode" ref="deptTreeRef" node-key="id"
                     highlight-current default-expand-all @node-click="handleNodeClick" />
               </div>
            </el-card>
         </el-col> -->
         <!--用户数据-->
         <el-col :span="24" :xs="24">
            <el-card shadow="never" body-class="search-card">
               <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
                  <el-form-item label="用户名称" prop="userName">
                     <el-input v-model="queryParams.userName" placeholder="请输入用户名称" clearable style="width: 240px"
                        @keyup.enter="handleQuery" />
                  </el-form-item>
                  <el-form-item label="手机号码" prop="phonenumber">
                     <el-input v-model="queryParams.phonenumber" placeholder="请输入手机号码" clearable style="width: 240px"
                        @keyup.enter="handleQuery" />
                  </el-form-item>
                  <el-form-item label="状态" prop="status">
                     <el-select v-model="queryParams.status" placeholder="用户状态" clearable style="width: 240px">
                        <el-option v-for="dict in sys_normal_disable" :key="dict.value" :label="dict.label"
                           :value="dict.value" />
                     </el-select>
                  </el-form-item>
                  <el-form-item label="创建时间" style="width: 308px;">
                     <el-date-picker v-model="dateRange" value-format="YYYY-MM-DD" type="daterange" range-separator="-"
                        start-placeholder="开始日期" end-placeholder="结束日期"></el-date-picker>
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
                     <el-button type="primary" plain @click="handleAdd"
                        v-hasPermi="['system:user:add']">新增</el-button>
                  </el-col>
                  <el-col :span="1.5">
                     <el-button type="success" plain :disabled="single" @click="handleUpdate"
                        v-hasPermi="['system:user:edit']">修改</el-button>
                  </el-col>
                  <el-col :span="1.5">
                     <el-button type="danger" plain :disabled="multiple" @click="handleDelete"
                        v-hasPermi="['system:user:remove']">删除</el-button>
                  </el-col>
                  <el-col :span="1.5">
                     <el-button type="info" plain @click="handleImport"
                        v-hasPermi="['system:user:import']">导入</el-button>
                  </el-col>
                  <el-col :span="1.5">
                     <el-button type="warning" plain @click="handleExport"
                        v-hasPermi="['system:user:export']">导出</el-button>
                  </el-col>
                  <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"
                     :columns="columns"></right-toolbar>
               </el-row>

               <el-table v-loading="loading" :data="userList" size="small" class="user-table" @selection-change="handleSelectionChange">
                  <el-table-column type="selection" width="50" align="center" />
                  <el-table-column label="用户名称" align="center" key="userName" prop="userName" v-if="columns[0].visible"
                     :show-overflow-tooltip="true" width="110"/>
                  <el-table-column label="备注" align="center" prop="remark" v-if="columns[4].visible"
                     :show-overflow-tooltip="true" width="200" />
                  <el-table-column label="当前积分" align="center" key="points" prop="points" v-if="columns[1].visible"
                     :show-overflow-tooltip="true" width="100">
                     <template #default="scope">
                        <span class="points-value">{{ formatPoints(scope.row.points) }}</span>
                     </template>
                  </el-table-column>
                  <el-table-column label="状态" align="center" key="status" v-if="columns[2].visible" width="112">
                     <template #default="scope">
                        <div class="user-status">
                           <el-switch v-model="scope.row.status" active-value="0" inactive-value="1"
                              @change="handleStatusChange(scope.row)"></el-switch>
                           <span :class="scope.row.status === '0' ? 'status-enabled' : 'status-disabled'">
                              {{ scope.row.status === '0' ? '启用' : '停用' }}
                           </span>
                        </div>
                     </template>
                  </el-table-column>
                  <el-table-column label="创建时间" align="center" prop="createTime" v-if="columns[3].visible" width="155">

                     <template #default="scope">
                        <span>{{ parseTime(scope.row.createTime) }}</span>
                     </template>
                  </el-table-column>
                  <el-table-column label="操作" align="center" width="240" class-name="small-padding fixed-width"
                     fixed="right">

                     <template #default="scope">
                        <div class="user-action-row" v-if="Number(scope.row.userId) !== 1">
                           <el-tooltip content="修改" placement="top" v-if="scope.row.userId !== 1">
                              <el-button link type="primary" @click="handleUpdate(scope.row)"
                                 v-hasPermi="['system:user:edit']">修改</el-button>
                           </el-tooltip>
                           <el-button link type="primary" @click="handleAccountRecharge(scope.row)"
                              v-hasPermi="['server:pointRecord:add']">充值</el-button>
                           <el-dropdown trigger="click" @command="command => handleMoreAction(command, scope.row)">
                              <el-button link type="primary">更多<el-icon class="more-arrow"><ArrowDown /></el-icon></el-button>
                              <template #dropdown>
                                 <el-dropdown-menu>
                                    <el-dropdown-item command="queryRecharge" v-hasPermi="['server:pointRecord:add']">查询充值</el-dropdown-item>
                                    <el-dropdown-item command="bindService" v-hasPermi="['system:user:edit']">绑定服务</el-dropdown-item>
                                    <el-dropdown-item command="resetPassword" v-hasPermi="['system:user:resetPwd']">重置密码</el-dropdown-item>
                                    <el-dropdown-item command="assignRole" v-hasPermi="['system:user:edit']">分配角色</el-dropdown-item>
                                    <el-dropdown-item command="delete" divided class="danger-menu-item" v-hasPermi="['system:user:remove']">删除</el-dropdown-item>
                                 </el-dropdown-menu>
                              </template>
                           </el-dropdown>
                        </div>
                     </template>
                  </el-table-column>
               </el-table>
               <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
                  v-model:limit="queryParams.pageSize" @pagination="getList" />
            </el-card>
         </el-col>
      </el-row>

      <!-- 添加或修改用户配置对话框 -->
      <el-dialog :title="title" v-model="open" width="600px" append-to-body>
         <el-form :model="form" :rules="rules" ref="userRef" label-width="80px">
            <el-row>
               <el-col :span="12">
                  <el-form-item label="用户昵称" prop="nickName">
                     <el-input v-model="form.nickName" placeholder="请输入用户昵称" maxlength="30" />
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="归属部门" prop="deptId">
                     <el-tree-select v-model="form.deptId" :data="deptOptions"
                        :props="{ value: 'id', label: 'label', children: 'children' }" value-key="id"
                        placeholder="请选择归属部门" check-strictly />
                  </el-form-item>
               </el-col>
            </el-row>
            <el-row>
               <el-col :span="12">
                  <el-form-item label="手机号码" prop="phonenumber">
                     <el-input v-model="form.phonenumber" placeholder="请输入手机号码" maxlength="11" />
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="邮箱" prop="email">
                     <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="50" />
                  </el-form-item>
               </el-col>
            </el-row>
            <el-row>
               <el-col :span="12">
                  <el-form-item v-if="form.userId == undefined" label="用户名称" prop="userName">
                     <el-input v-model="form.userName" placeholder="请输入用户名称" maxlength="30" />
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item v-if="form.userId == undefined" label="用户密码" prop="password">
                     <el-input v-model="form.password" placeholder="请输入用户密码" type="password" maxlength="20"
                        show-password />
                  </el-form-item>
               </el-col>
            </el-row>
            <el-row>
               <el-col :span="12">
                  <el-form-item label="用户性别">
                     <el-select v-model="form.sex" placeholder="请选择">
                        <el-option v-for="dict in sys_user_sex" :key="dict.value" :label="dict.label"
                           :value="dict.value"></el-option>
                     </el-select>
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="状态">
                     <el-radio-group v-model="form.status">
                        <el-radio v-for="dict in sys_normal_disable" :key="dict.value" :value="dict.value">{{ dict.label
                        }}</el-radio>
                     </el-radio-group>
                  </el-form-item>
               </el-col>
            </el-row>
            <el-row>
               <el-col :span="12">
                  <el-form-item label="岗位">
                     <el-select v-model="form.postIds" multiple placeholder="请选择">
                        <el-option v-for="item in postOptions" :key="item.postId" :label="item.postName"
                           :value="item.postId" :disabled="item.status == 1"></el-option>
                     </el-select>
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="角色">
                     <el-select v-model="form.roleId" placeholder="请选择">
                        <el-option v-for="item in roleOptions" :key="item.roleId" :label="item.roleName"
                           :value="item.roleId" :disabled="item.status == 1"></el-option>
                     </el-select>
                  </el-form-item>
               </el-col>
            </el-row>
            <el-row>
               <el-col :span="24">
                  <el-form-item label="备注">
                     <el-input v-model="form.remark" type="textarea" placeholder="请输入内容"></el-input>
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

      <!-- 服务绑定对话框 -->
      <el-dialog :title="serviceDialog.title" v-model="serviceDialog.open" width="700px" append-to-body>
         <el-form ref="serviceRef" :model="serviceForm" :rules="serviceRules" label-width="100px">
            <el-form-item label="用户名" prop="userName">
               <el-input v-model="serviceForm.userName" disabled />
            </el-form-item>
            <el-form-item label="服务模板" prop="relTemplate">
               <el-select v-model="serviceForm.relTemplate" placeholder="请选择服务模板" clearable style="width: 100%;" @change="handleTemplateChange">
                  <el-option 
                     v-for="template in templateList" 
                     :key="template.id" 
                     :label="template.templateName" 
                     :value="template.id" />
               </el-select>
            </el-form-item>
            <el-form-item label="模板预览" v-if="selectedTemplate">
               <el-descriptions :column="1" border size="small">
                  <el-descriptions-item label="模板名称">{{ selectedTemplate.templateName }}</el-descriptions-item>
                  <el-descriptions-item label="包含API">
                     <div v-if="apiNames.length > 0">
                        <el-tag v-for="apiName in apiNames" :key="apiName" type="info" style="margin-right: 5px; margin-bottom: 3px;">
                           {{ apiName }}
                        </el-tag>
                     </div>
                     <span v-else>-</span>
                  </el-descriptions-item>
                  <el-descriptions-item label="状态">
                     <el-tag :type="selectedTemplate.status === '0' ? 'success' : 'danger'">
                        {{ selectedTemplate.status === '0' ? '启用' : '禁用' }}
                     </el-tag>
                  </el-descriptions-item>
               </el-descriptions>
            </el-form-item>
            <el-form-item label="备注" prop="remark">
               <el-input v-model="serviceForm.remark" type="textarea" placeholder="请输入备注" />
            </el-form-item>
         </el-form>
         <template #footer>
            <div class="dialog-footer">
               <el-button type="primary" @click="submitServiceForm">确 定</el-button>
               <el-button @click="serviceDialog.open = false">取 消</el-button>
            </div>
         </template>
      </el-dialog>

      <AccountRechargeDialog
         v-model="accountRechargeOpen"
         :user="accountRechargeUser"
         @changed="getList"
      />

      <QueryRechargeDialog
         v-model="queryRechargeOpen"
         :user="queryRechargeUser"
         @changed="getList"
      />

      <!-- 用户导入对话框 -->
      <el-dialog :title="upload.title" v-model="upload.open" width="400px" append-to-body>
         <el-upload ref="uploadRef" :limit="1" accept=".xlsx, .xls" :headers="upload.headers"
            :action="upload.url + '?updateSupport=' + upload.updateSupport" :disabled="upload.isUploading"
            :on-progress="handleFileUploadProgress" :on-success="handleFileSuccess" :auto-upload="false" drag>
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>

            <template #tip>
               <div class="el-upload__tip text-center">
                  <div class="el-upload__tip">
                     <el-checkbox v-model="upload.updateSupport" />是否更新已经存在的用户数据
                  </div>
                  <span>仅允许导入xls、xlsx格式文件。</span>
                  <el-button type="primary" link style="font-size:12px;vertical-align: baseline;"
                     @click="importTemplate">下载模板</el-button>
               </div>
            </template>
         </el-upload>

         <template #footer>
            <div class="dialog-footer">
               <el-button type="primary" @click="submitFileForm">确 定</el-button>
               <el-button @click="upload.open = false">取 消</el-button>
            </div>
         </template>
      </el-dialog>
   </div>
</template>

<script setup name="User">
import { ArrowDown } from '@element-plus/icons-vue';
import { getToken } from "@/utils/auth";
import { changeUserStatus, listUser, resetUserPwd, delUser, getUser, updateUser, addUser, deptTreeSelect } from "@/api/system/user";
import { listTemplate } from "@/api/server/template";
import { listPlatformConfig } from "@/api/server/platformConfig";
import AccountRechargeDialog from './components/AccountRechargeDialog.vue';
import QueryRechargeDialog from './components/QueryRechargeDialog.vue';

const router = useRouter();
const { proxy } = getCurrentInstance();
const { sys_normal_disable, sys_user_sex } = proxy.useDict("sys_normal_disable", "sys_user_sex");

const userList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const dateRange = ref([]);
const deptName = ref("");
const deptOptions = ref(undefined);
const initPassword = ref(undefined);
const postOptions = ref([]);
const roleOptions = ref([]);

// 服务绑定相关状态
const serviceDialog = reactive({
   open: false,
   title: ""
});
const serviceForm = reactive({
   userId: null,
   userName: "",
   relTemplate: "",
   remark: ""
});
const serviceRules = {
   relTemplate: [{ required: true, message: "请选择服务模板", trigger: "change" }]
};
const serviceRef = ref(null);
const templateList = ref([]);
const selectedTemplate = ref(null);
const apiNames = ref([]);
const platformNameMap = ref(new Map());
let platformNameLoadingPromise = null;

const accountRechargeOpen = ref(false);
const accountRechargeUser = ref(null);
const queryRechargeOpen = ref(false);
const queryRechargeUser = ref(null);

/*** 用户导入参数 */
const upload = reactive({
   // 是否显示弹出层（用户导入）
   open: false,
   // 弹出层标题（用户导入）
   title: "",
   // 是否禁用上传
   isUploading: false,
   // 是否更新已经存在的用户数据
   updateSupport: 0,
   // 设置上传的请求头部
   headers: { Authorization: "Bearer " + getToken() },
   // 上传的地址
   url: import.meta.env.VITE_APP_BASE_API + "/system/user/importData"
});
// 列显隐信息
const columns = ref([
   { key: 0, label: `用户名称`, visible: true },
   { key: 1, label: `积分`, visible: true },
   { key: 2, label: `状态`, visible: true },
   { key: 3, label: `创建时间`, visible: true },
   { key: 4, label: `备注`, visible: true }
]);

const data = reactive({
   form: {},
   queryParams: {
      pageNum: 1,
      pageSize: 10,
      userName: undefined,
      phonenumber: undefined,
      status: undefined,
      deptId: undefined
   },
   rules: {
      userName: [{ required: true, message: "用户名称不能为空", trigger: "blur" }, { min: 2, max: 20, message: "用户名称长度必须介于 2 和 20 之间", trigger: "blur" }],
      nickName: [{ required: true, message: "用户昵称不能为空", trigger: "blur" }],
      password: [
         { required: true, message: "用户密码不能为空", trigger: "blur" },
         { min: 5, max: 20, message: "用户密码长度必须介于 5 和 20 之间", trigger: "blur" },
         { pattern: /^[^<>"'|\\]+$/, message: "不能包含非法字符：< > \" ' \\\ |", trigger: "blur" }
      ],
      email: [{ type: "email", message: "请输入正确的邮箱地址", trigger: ["blur", "change"] }],
      phonenumber: [{ pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: "请输入正确的手机号码", trigger: "blur" }]
   }
});

const { queryParams, form, rules } = toRefs(data);

/** 通过条件过滤节点  */
const filterNode = (value, data) => {
   if (!value) return true;
   return data.label.indexOf(value) !== -1;
};
/** 根据名称筛选部门树 */
watch(deptName, val => {
   proxy.$refs["deptTreeRef"].filter(val);
});
/** 查询部门下拉树结构 */
function getDeptTree() {
   deptTreeSelect().then(response => {
      deptOptions.value = response.data;
   });
};
/** 查询用户列表 */
function getList() {
   loading.value = true;
   listUser(proxy.addDateRange(queryParams.value, dateRange.value)).then(res => {
      loading.value = false;
      userList.value = res.rows;
      total.value = res.total;
   });
};
/** 节点单击事件 */
function handleNodeClick(data) {
   queryParams.value.deptId = data.id;
   handleQuery();
};
/** 搜索按钮操作 */
function handleQuery() {
   queryParams.value.pageNum = 1;
   getList();
};
/** 重置按钮操作 */
function resetQuery() {
   dateRange.value = [];
   proxy.resetForm("queryRef");
   queryParams.value.deptId = undefined;
   proxy.$refs.tree.setCurrentKey(null);
   handleQuery();
};
/** 删除按钮操作 */
function handleDelete(row) {
   const userIds = row.userId || ids.value;
   proxy.$modal.confirm('是否确认删除用户编号为"' + userIds + '"的数据项？').then(function () {
      return delUser(userIds);
   }).then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
   }).catch(() => { });
};
/** 导出按钮操作 */
function handleExport() {
   proxy.download("system/user/export", {
      ...queryParams.value,
   }, `user_${new Date().getTime()}.xlsx`);
};
/** 用户状态修改  */
function handleStatusChange(row) {
   let text = row.status === "0" ? "启用" : "停用";
   proxy.$modal.confirm('确认要"' + text + '""' + row.userName + '"用户吗?').then(function () {
      return changeUserStatus(row.userId, row.status);
   }).then(() => {
      proxy.$modal.msgSuccess(text + "成功");
   }).catch(function () {
      row.status = row.status === "0" ? "1" : "0";
   });
};
/** 列表更多操作 */
function handleMoreAction(command, row) {
   switch (command) {
      case "queryRecharge":
         handleQueryRecharge(row);
         break;
      case "bindService":
         handleBindService(row);
         break;
      case "resetPassword":
         handleResetPwd(row);
         break;
      case "assignRole":
         handleAuthRole(row);
         break;
      case "delete":
         handleDelete(row);
         break;
      default:
         break;
   }
};

function formatPoints(value) {
   const points = Number(value);
   return Number.isFinite(points) ? points.toLocaleString('zh-CN') : (value ?? '-');
};
/** 跳转角色分配 */
function handleAuthRole(row) {
   const userId = row.userId;
   router.push("/system/user-auth/role/" + userId);
};

/** 账户充值按钮操作 */
function handleAccountRecharge(row) {
   accountRechargeUser.value = row;
   accountRechargeOpen.value = true;
}

/** 查询积分充值按钮操作 */
function handleQueryRecharge(row) {
   queryRechargeUser.value = row;
   queryRechargeOpen.value = true;
}

/** 查询服务模板列表 */
function getTemplateList() {
   return listTemplate({ status: '0' }).then(response => {
      templateList.value = response.rows;
   });
}

/** 加载平台名称缓存，避免每次切换模板都发N次详情请求 */
function ensurePlatformNameMap() {
   if (platformNameMap.value.size > 0) return Promise.resolve();
   if (platformNameLoadingPromise) return platformNameLoadingPromise;
   platformNameLoadingPromise = listPlatformConfig({ pageNum: 1, pageSize: 1000 }).then(response => {
      const map = new Map();
      const rows = response?.rows || [];
      rows.forEach(item => {
         const key = String(item.id ?? item.platformId ?? "");
         const name = item.platformName || item.name || item.apiName;
         if (key && name) map.set(key, name);
      });
      platformNameMap.value = map;
   }).finally(() => {
      platformNameLoadingPromise = null;
   });
   return platformNameLoadingPromise;
}

/** 模板选择变化处理 */
async function handleTemplateChange(templateId) {
   if (templateId) {
      selectedTemplate.value = templateList.value.find(template => String(template.id) === String(templateId));
      // 解析templateInfo并获取API名称
      if (selectedTemplate.value?.templateInfo) {
         try {
            const apiIds = JSON.parse(selectedTemplate.value.templateInfo);
            if (Array.isArray(apiIds) && apiIds.length > 0) {
               await ensurePlatformNameMap();
               apiNames.value = apiIds
                  .map(id => platformNameMap.value.get(String(id)))
                  .filter(name => !!name);
            } else {
               apiNames.value = [];
            }
         } catch (error) {
            apiNames.value = [];
         }
      } else {
         apiNames.value = [];
      }
   } else {
      selectedTemplate.value = null;
      apiNames.value = [];
   }
}

/** 服务绑定按钮操作 */
async function handleBindService(row) {
   resetServiceForm();
   serviceForm.userId = row.userId;
   serviceForm.userName = row.userName;
   serviceDialog.title = "绑定服务 - " + row.userName;
   serviceDialog.open = true;
   // 打开弹窗后回显用户已绑定模板
   try {
      const [, userResponse] = await Promise.all([
         getTemplateList(),
         getUser(row.userId),
         ensurePlatformNameMap()
      ]);
      const relTemplate = userResponse?.data?.relTemplate;
      if (relTemplate !== undefined && relTemplate !== null && String(relTemplate) !== "") {
         serviceForm.relTemplate = relTemplate;
         await handleTemplateChange(serviceForm.relTemplate);
      }
   } catch (error) {
      console.error("加载绑定服务信息失败:", error);
   }
}

/** 重置服务表单 */
function resetServiceForm() {
   serviceForm.userId = null;
   serviceForm.userName = "";
   serviceForm.relTemplate = "";
   serviceForm.remark = "";
   selectedTemplate.value = null;
   apiNames.value = [];
   if (serviceRef.value) {
      proxy.resetForm("serviceRef");
   }
}

/** 提交服务绑定表单 */
function submitServiceForm() {
   proxy.$refs["serviceRef"].validate(valid => {
      if (valid) {
         // 获取当前用户信息
         getUser(serviceForm.userId).then(userResponse => {
            const currentUser = userResponse.data;
            // 更新用户的服务绑定信息
            const updateData = {
               ...currentUser,
               relTemplate: serviceForm.relTemplate
            };
            
            updateUser(updateData).then(() => {
               proxy.$modal.msgSuccess("服务绑定成功");
               serviceDialog.open = false;
               getList();
            }).catch(() => {
               proxy.$modal.msgError("服务绑定失败");
            });
         });
      }
   });
}
/** 重置密码按钮操作 */
function handleResetPwd(row) {
   proxy.$prompt('请输入"' + row.userName + '"的新密码', "提示", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      closeOnClickModal: false,
      inputPattern: /^.{5,20}$/,
      inputErrorMessage: "用户密码长度必须介于 5 和 20 之间",
      inputValidator: (value) => {
         if (/<|>|"|'|\||\\/.test(value)) {
            return "不能包含非法字符：< > \" ' \\\ |"
         }
      },
   }).then(({ value }) => {
      resetUserPwd(row.userId,row.userName,row.nickName, value).then(response => {
         proxy.$modal.msgSuccess("修改成功，新密码是：" + value);
      });
   }).catch(() => { });
};
/** 选择条数  */
function handleSelectionChange(selection) {
   ids.value = selection.map(item => item.userId);
   single.value = selection.length != 1;
   multiple.value = !selection.length;
};
/** 导入按钮操作 */
function handleImport() {
   upload.title = "用户导入";
   upload.open = true;
};
/** 下载模板操作 */
function importTemplate() {
   proxy.download("system/user/importTemplate", {
   }, `user_template_${new Date().getTime()}.xlsx`);
};
/**文件上传中处理 */
const handleFileUploadProgress = (event, file, fileList) => {
   upload.isUploading = true;
};
/** 文件上传成功处理 */
const handleFileSuccess = (response, file, fileList) => {
   upload.open = false;
   upload.isUploading = false;
   proxy.$refs["uploadRef"].handleRemove(file);
   proxy.$alert("<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" + response.msg + "</div>", "导入结果", { dangerouslyUseHTMLString: true });
   getList();
};
/** 提交上传文件 */
function submitFileForm() {
   proxy.$refs["uploadRef"].submit();
};
/** 重置操作表单 */
function reset() {
   form.value = {
      userId: undefined,
      deptId: undefined,
      userName: undefined,
      nickName: undefined,
      password: undefined,
      phonenumber: undefined,
      email: undefined,
      sex: undefined,
      status: "0",
      points: undefined,
      remark: undefined,
      postIds: [],
      roleId: undefined
   };
   proxy.resetForm("userRef");
};
/** 取消按钮 */
function cancel() {
   open.value = false;
   reset();
};
/** 新增按钮操作 */
function handleAdd() {
   reset();
   getUser().then(response => {
      postOptions.value = response.posts;
      roleOptions.value = response.roles;
      open.value = true;
      title.value = "添加用户";
      form.value.password = initPassword.value;
   });
};
/** 修改按钮操作 */
function handleUpdate(row) {
   reset();
   const userId = row.userId || ids.value;
   getUser(userId).then(response => {
      form.value = response.data;
      postOptions.value = response.posts;
      roleOptions.value = response.roles;
      form.value.postIds = response.postIds;
      form.value.roleId = response.roleIds?.[0];
      open.value = true;
      title.value = "修改用户";
      form.password = "";
   });
};
/** 提交按钮 */
function submitForm() {
   proxy.$refs["userRef"].validate(valid => {
      if (valid) {
         const payload = {
            ...form.value,
            roleIds: form.value.roleId === undefined ? [] : [form.value.roleId]
         };
         if (form.value.userId != undefined) {
            updateUser(payload).then(response => {
               proxy.$modal.msgSuccess("修改成功");
               open.value = false;
               getList();
            });
         } else {
            addUser(payload).then(response => {
               proxy.$modal.msgSuccess("新增成功");
               open.value = false;
               getList();
            });
         }
      }
   });
};

getDeptTree();
getList();
</script>

<style scoped>
/* 缩小操作按钮高度 */
.el-table .el-button {
   padding: 4px 8px;
   font-size: 14px;
   height: 28px;
   line-height: 1;
}

.user-action-row {
   display: flex;
   align-items: center;
   justify-content: center;
   gap: 8px;
   min-height: 32px;
   white-space: nowrap;
}

.points-value { color: #67C23A; font-weight: 600; font-variant-numeric: tabular-nums; }
.user-status { display: inline-flex; align-items: center; justify-content: center; gap: 6px; }
.user-status :deep(.el-switch) { --el-switch-on-color: #409eff; }
.status-enabled { color: #409eff; font-size: 13px; }
.status-disabled { color: var(--el-text-color-secondary); font-size: 13px; }
.more-arrow { margin-left: 3px; font-size: 12px; vertical-align: -1px; }
:global(.danger-menu-item) { color: var(--el-color-danger) !important; }
</style>
