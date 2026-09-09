<template>
   <div class="app-container">
      <h4 class="form-header h4">基本信息</h4>
      <el-form :model="form" label-width="80px">
         <el-row>
            <el-col :span="8" :offset="2">
               <el-form-item label="用户昵称" prop="nickName">
                  <el-input v-model="form.nickName" disabled />
               </el-form-item>
            </el-col>
            <el-col :span="8" :offset="2">
               <el-form-item label="登录账号" prop="userName">
                  <el-input v-model="form.userName" disabled />
               </el-form-item>
            </el-col>
         </el-row>
      </el-form>

      <h4 class="form-header h4">角色信息</h4>
      <el-table v-loading="loading" @row-click="clickRow"
         :data="roles.slice((pageNum - 1) * pageSize, pageNum * pageSize)">
         <el-table-column label="序号" width="55" type="index" align="center">
            <template #default="scope">
               <span>{{ (pageNum - 1) * pageSize + scope.$index + 1 }}</span>
            </template>
         </el-table-column>
         <el-table-column label="选择" align="center" width="55">
            <template #default="scope">
               <el-radio v-model="roleId" :value="scope.row.roleId" :disabled="!checkSelectable(scope.row)">
                  <span class="sr-only">选择角色</span>
               </el-radio>
            </template>
         </el-table-column>
         <el-table-column label="角色编号" align="center" prop="roleId" />
         <el-table-column label="角色名称" align="center" prop="roleName" />
         <el-table-column label="权限字符" align="center" prop="roleKey" />
         <el-table-column label="创建时间" align="center" prop="createTime" width="180">
            <template #default="scope">
               <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
         </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="pageNum" v-model:limit="pageSize" />

      <template v-if="hasMarkAgentRole">
         <h4 class="form-header h4">代理配置</h4>
         <el-form :model="form" label-width="100px" class="agent-form">
            <el-row>
               <el-col :span="8" :offset="2">
                  <el-form-item label="代理等级" required>
                     <el-select v-model="form.agentLevel" placeholder="请选择代理等级" style="width: 100%">
                         <el-option v-if="!isSubAgentRole" label="一级代理" :value="1" />
                         <el-option v-if="isSubAgentRole" label="二级代理" :value="2" />
                     </el-select>
                  </el-form-item>
               </el-col>
               <el-col v-if="form.agentLevel === 2" :span="8" :offset="2">
                  <el-form-item label="直属上级" required>
                     <el-select v-model="form.parentUserId" filterable placeholder="请选择一级代理" style="width: 100%">
                        <el-option
                           v-for="item in parentAgents"
                           :key="item.userId"
                           :label="formatParentLabel(item)"
                           :value="item.userId"
                        />
                     </el-select>
                  </el-form-item>
               </el-col>
            </el-row>
         </el-form>
      </template>

      <el-form label-width="100px">
         <div style="text-align: center;margin-left:-120px;margin-top:30px;">
            <el-button type="primary" @click="submitForm()">提交</el-button>
            <el-button @click="close()">返回</el-button>
         </div>
      </el-form>
   </div>
</template>

<script setup name="AuthRole">
import { getAuthRole, updateAuthRole } from "@/api/system/user";

const route = useRoute();
const { proxy } = getCurrentInstance();

const loading = ref(true);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const roleId = ref(undefined);
const roles = ref([]);
const parentAgents = ref([]);
const form = ref({
   nickName: undefined,
   userName: undefined,
   userId: undefined,
   agentLevel: undefined,
   parentUserId: undefined
});
const isAgentRoleKey = roleKey => ["mark_agent", "mark_sub_agent"].includes(roleKey);
const hasMarkAgentRole = computed(() => roles.value.some(row => isAgentRoleKey(row.roleKey) && roleId.value === row.roleId));
const isSubAgentRole = computed(() => roles.value.some(row => row.roleKey === "mark_sub_agent" && roleId.value === row.roleId));

/** 单击选中行数据 */
function clickRow(row) {
   if (checkSelectable(row)) {
      roleId.value = row.roleId;
   }
};

function checkSelectable(row) {
   return row.status === "0" ? true : false;
}

/** 角色选择数据 */
watch(roleId, selectedRoleId => {
   const selectedRole = roles.value.find(item => item.roleId === selectedRoleId);
   if (selectedRole?.roleKey === "mark_sub_agent") {
      form.value.agentLevel = 2;
   } else if (selectedRole?.roleKey === "mark_agent") {
      form.value.agentLevel = 1;
      form.value.parentUserId = undefined;
   }
});

function formatParentLabel(item) {
   return item.nickName && item.nickName !== item.userName
      ? `${item.userName}（${item.nickName}）`
      : item.userName;
}
/** 关闭按钮 */
function close() {
   const obj = { path: "/system/user" };
   proxy.$tab.closeOpenPage(obj);
};
/** 提交按钮 */
function submitForm() {
   const userId = form.value.userId;
   const rIds = roleId.value === undefined ? "" : String(roleId.value);
   if (hasMarkAgentRole.value && ![1, 2].includes(form.value.agentLevel)) {
      proxy.$modal.msgError("请选择代理等级");
      return;
   }
   if (hasMarkAgentRole.value && form.value.agentLevel === 2 && !form.value.parentUserId) {
      proxy.$modal.msgError("请选择直属一级代理");
      return;
   }
   const params = { userId: userId, roleIds: rIds };
   if (hasMarkAgentRole.value) {
      params.agentLevel = form.value.agentLevel;
      if (form.value.agentLevel === 2) {
         params.parentUserId = form.value.parentUserId;
      }
   }
   updateAuthRole(params).then(response => {
      proxy.$modal.msgSuccess("授权成功");
      close();
   });
};

(() => {
   const userId = route.params && route.params.userId;
   if (userId) {
      loading.value = true;
      getAuthRole(userId).then(response => {
         form.value = response.user;
         roles.value = response.roles;
         parentAgents.value = response.parentAgents || [];
         total.value = roles.value.length;
         roleId.value = roles.value.find(row => row.flag)?.roleId;
         loading.value = false;
      });
   }
})();
</script>

<style scoped>
.agent-form {
   padding-top: 8px;
}

.sr-only {
   position: absolute;
   width: 1px;
   height: 1px;
   padding: 0;
   margin: -1px;
   overflow: hidden;
   clip: rect(0, 0, 0, 0);
   white-space: nowrap;
   border: 0;
}
</style>
