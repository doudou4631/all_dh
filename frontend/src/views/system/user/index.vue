<template>
   <div class="app-container" :class="{ 'user-table-page': isAgentAccountPage }">
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
            <el-alert
               v-if="isAgentAccountPage && isAgent"
               class="user-template-banner"
               :type="myMarkTemplateBound ? 'success' : 'warning'"
               :closable="false"
               show-icon
            >
               <div class="user-template-banner__content">
                  <span>
                     我的标记模板：
                     <strong>{{ myMarkTemplateDisplay }}</strong>
                     <span class="user-template-banner__hint">
                        {{ myMarkTemplateBound ? '（与用户端模板独立，互不同步）' : '（代理端展示用，请先绑定）' }}
                     </span>
                  </span>
                  <div class="user-template-banner__actions">
                     <el-button
                        v-if="canManageMarkTemplate"
                        type="primary"
                        size="small"
                        @click="handleGoMarkTemplatePage"
                     >管理我的模板</el-button>
                     <el-button type="warning" size="small" @click="handleBindMyMarkTemplate">绑定我的模板</el-button>
                  </div>
               </div>
            </el-alert>
            <el-card shadow="never" :body-class="isAgentAccountPage ? 'user-table-search-card' : 'search-card'">
               <el-form
                  :model="queryParams"
                  ref="queryRef"
                  :inline="true"
                  v-show="showSearch"
                  :label-width="isAgentAccountPage ? 'auto' : '68px'"
                  :class="{ 'user-table-search-form': isAgentAccountPage }"
               >
                  <el-form-item label="用户名称" prop="userName">
                     <el-input
                        v-model="queryParams.userName"
                        placeholder="请输入用户名称"
                        clearable
                        :style="{ width: isAgentAccountPage ? '140px' : '240px' }"
                        :size="isAgentAccountPage ? 'small' : 'default'"
                        @keyup.enter="handleQuery"
                     />
                  </el-form-item>
                  <el-form-item label="手机号码" prop="phonenumber">
                     <el-input
                        v-model="queryParams.phonenumber"
                        placeholder="请输入手机号码"
                        clearable
                        :style="{ width: isAgentAccountPage ? '140px' : '240px' }"
                        :size="isAgentAccountPage ? 'small' : 'default'"
                        @keyup.enter="handleQuery"
                     />
                  </el-form-item>
                  <el-form-item label="状态" prop="status">
                     <el-select
                        v-model="queryParams.status"
                        placeholder="用户状态"
                        clearable
                        :style="{ width: isAgentAccountPage ? '100px' : '240px' }"
                        :size="isAgentAccountPage ? 'small' : 'default'"
                     >
                        <el-option v-for="dict in sys_normal_disable" :key="dict.value" :label="dict.label"
                           :value="dict.value" />
                     </el-select>
                  </el-form-item>
                  <el-form-item label="创建时间" :style="{ width: isAgentAccountPage ? 'auto' : '308px' }">
                     <el-date-picker
                        v-model="dateRange"
                        value-format="YYYY-MM-DD"
                        type="daterange"
                        range-separator="-"
                        start-placeholder="开始日期"
                        end-placeholder="结束日期"
                        :size="isAgentAccountPage ? 'small' : 'default'"
                        :style="{ width: isAgentAccountPage ? '220px' : '100%' }"
                     />
                  </el-form-item>
                  <el-form-item class="user-table-search-actions">
                     <el-button type="primary" icon="Search" :size="isAgentAccountPage ? 'small' : 'default'" @click="handleQuery">搜索</el-button>
                     <el-button icon="Refresh" :size="isAgentAccountPage ? 'small' : 'default'" @click="resetQuery">重置</el-button>
                  </el-form-item>
               </el-form>
            </el-card>

            <el-card shadow="never" :class="isAgentAccountPage ? 'user-table-card' : 'mt10'">
               <el-row :gutter="10" :class="isAgentAccountPage ? 'user-table-toolbar' : 'mb8'">
                  <el-col :span="1.5">
                     <el-button type="primary" plain icon="Plus" :size="isAgentAccountPage ? 'small' : 'default'" @click="handleAdd"
                        v-hasPermi="['system:user:add']">新增</el-button>
                  </el-col>
                  <el-col :span="1.5" v-if="!isAgentAccountPage">
                     <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate"
                        v-hasPermi="['system:user:edit']">修改</el-button>
                  </el-col>
                  <el-col :span="1.5">
                     <el-button type="danger" plain icon="Delete" :disabled="multiple" :size="isAgentAccountPage ? 'small' : 'default'" @click="handleDelete"
                        v-hasPermi="['system:user:remove']">删除</el-button>
                  </el-col>
                  <el-col :span="1.5" v-if="!isAgentAccountPage">
                     <el-button type="info" plain icon="Upload" @click="handleImport"
                        v-hasPermi="['system:user:import']">导入</el-button>
                  </el-col>
                  <el-col :span="1.5" v-if="!isAgentAccountPage">
                     <el-button type="warning" plain icon="Download" @click="handleExport"
                        v-hasPermi="['system:user:export']">导出</el-button>
                  </el-col>
                  <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"
                     :columns="columns"></right-toolbar>
               </el-row>

               <el-table
                  v-loading="loading"
                  :data="userList"
                  :size="isAgentAccountPage ? 'small' : 'default'"
                  :border="isAgentAccountPage"
                  :class="{ 'user-table-main': isAgentAccountPage }"
                  @selection-change="handleSelectionChange"
               >
                  <el-table-column type="selection" :width="isAgentAccountPage ? 42 : 50" align="center" />
                  <el-table-column
                     label="用户编号"
                     align="center"
                     key="userId"
                     prop="userId"
                     v-if="columns[0].visible && !isAgentAccountPage"
                     width="120"
                  />
                  <el-table-column
                     label="用户编号"
                     align="left"
                     key="userIdCompact"
                     prop="userId"
                     v-if="columns[0].visible && isAgentAccountPage"
                     min-width="150"
                     show-overflow-tooltip
                     class-name="user-id-cell"
                  />
                  <el-table-column label="用户名称" align="center" key="userName" prop="userName" v-if="columns[1].visible"
                     :show-overflow-tooltip="true" :min-width="isAgentAccountPage ? 90 : 120"/>
                  <el-table-column label="用户昵称" align="center" key="nickName" prop="nickName" v-if="columns[2].visible"
                     :show-overflow-tooltip="true" :min-width="isAgentAccountPage ? 90 : 120"/>
                  <!-- <el-table-column label="部门" align="center" key="deptName" prop="dept.deptName"
                     v-if="columns[3].visible" :show-overflow-tooltip="true" /> -->
                  <el-table-column label="当前模板" align="center" key="points" prop="relMarkTemplate"
                     v-if="columns[3].visible && shouldShowMarkTemplateColumn" :show-overflow-tooltip="true" :min-width="isAgentAccountPage ? 100 : 140">
                     <template #default="scope">
                        <el-tag v-if="!hasBoundMarkTemplate(scope.row)" type="danger" size="small">未绑定</el-tag>
                        <span v-else class="user-table-template">{{ resolveMarkTemplateName(scope.row) }}</span>
                     </template>
                  </el-table-column>
                  <el-table-column label="当前积分" align="center" key="points" prop="points"
                     v-if="columns[3].visible && !shouldShowMarkTemplateColumn" :show-overflow-tooltip="true" width="120">
                     <template #default="scope">
                        <span style="color: #67C23A; font-weight: bold;">{{ scope.row.points }}</span>
                     </template>
                  </el-table-column>
                  <el-table-column label="手机号码" align="center" key="phonenumber" prop="phonenumber"
                     v-if="columns[4].visible && !(isAgentAccountPage && isAgent)" :min-width="isAgentAccountPage ? 110 : 120" />
                  <el-table-column label="状态" align="center" key="status" v-if="columns[5].visible" width="72">
                     <template #default="scope">
                        <el-switch v-model="scope.row.status" active-value="0" inactive-value="1"
                           :disabled="!canEditUser"
                           @change="handleStatusChange(scope.row)"></el-switch>
                     </template>
                  </el-table-column>
                  <el-table-column label="创建时间" align="center" prop="createTime" v-if="columns[6].visible" :min-width="isAgentAccountPage ? 140 : 160">

                     <template #default="scope">
                        <span>{{ parseTime(scope.row.createTime) }}</span>
                     </template>
                  </el-table-column>
                  <el-table-column
                     label="操作"
                     align="center"
                     :width="isAgentAccountPage ? 340 : 620"
                     class-name="small-padding fixed-width"
                     fixed="right"
                  >

                     <template #default="scope">
                        <div class="operation-actions operation-actions--table" v-if="isAgentAccountPage">
                           <el-button
                              v-if="canBindMarkTemplate && Number(scope.row.userId) !== 1"
                              type="warning"
                              plain
                              size="small"
                              @click="handleBindMarkTemplate(scope.row)"
                           >
                              {{ resolveBindTemplateActionLabel(scope.row) }}
                           </el-button>
                           <el-button link type="success" size="small" @click="handleAddPoints(scope.row)"
                              v-hasPermi="['server:pointRecord:add']"
                              v-if="Number(scope.row.userId) !== 1">充值</el-button>
                           <el-button link type="danger" size="small" @click="handleDeductPoints(scope.row)"
                              v-hasPermi="['server:pointRecord:add']"
                              v-if="Number(scope.row.userId) !== 1">扣减</el-button>
                           <el-button link type="primary" size="small" @click="handleViewAgentPlatformQuota(scope.row)"
                              v-hasPermi="['server:pointRecord:add']"
                              v-if="isAgent && Number(scope.row.userId) !== 1">平台次数</el-button>
                           <el-button link type="info" size="small" @click="handleToggleOwnedAccounts(scope.row)"
                              v-if="isSuperAdminAgentAccountPage && Number(scope.row.userId) !== 1">
                              {{ isCurrentOwner(scope.row) ? '收起' : '名下账户' }}
                           </el-button>
                           <el-dropdown
                              v-if="Number(scope.row.userId) !== 1"
                              @command="(command) => handleCommand(command, scope.row)"
                           >
                              <el-button link type="primary" size="small">更多</el-button>
                              <template #dropdown>
                                 <el-dropdown-menu>
                                    <el-dropdown-item
                                       v-if="canBindMarkTemplate && isAgent"
                                       command="handleBindMarkTemplate"
                                    >绑定模板</el-dropdown-item>
                                    <el-dropdown-item command="handleUpdate">编辑账号</el-dropdown-item>
                                    <el-dropdown-item command="handleDelete" v-hasPermi="['system:user:remove']">删除账号</el-dropdown-item>
                                    <el-dropdown-item command="handleResetPwd" v-hasPermi="['system:user:resetPwd']">重置密码</el-dropdown-item>
                                    <el-dropdown-item command="handleAuthRole" v-hasPermi="['system:user:edit']">分配角色</el-dropdown-item>
                                 </el-dropdown-menu>
                              </template>
                           </el-dropdown>
                        </div>
                        <div class="operation-actions" v-else>
                           <el-tooltip content="修改" placement="top" v-if="scope.row.userId !== 1">
                              <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)"
                                 v-hasPermi="['system:user:edit']" />
                           </el-tooltip>
                           <el-tooltip content="删除" placement="top" v-if="scope.row.userId !== 1">
                              <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)"
                                 v-hasPermi="['system:user:remove']" />
                           </el-tooltip>
                           <el-tooltip content="重置密码" placement="top" v-if="scope.row.userId !== 1">
                              <el-button link type="primary" icon="Key" @click="handleResetPwd(scope.row)"
                                 v-hasPermi="['system:user:resetPwd']" />
                           </el-tooltip>
                           <el-tooltip content="分配角色" placement="top" v-if="scope.row.userId !== 1">
                              <el-button link type="primary" icon="CircleCheck" @click="handleAuthRole(scope.row)"
                                 v-hasPermi="['system:user:edit']" />
                           </el-tooltip>
                           <el-button type="success" @click="handleAddPoints(scope.row)"
                              v-hasPermi="['server:pointRecord:add']"
                              v-if="Number(scope.row.userId) !== 1" round>充值</el-button>
                           <el-button type="danger" @click="handleDeductPoints(scope.row)"
                              v-hasPermi="['server:pointRecord:add']"
                              v-if="Number(scope.row.userId) !== 1" round>扣减</el-button>
                           <el-button type="primary" @click="handleBindService(scope.row)"
                              v-hasPermi="['system:user:edit']"
                              v-if="Number(scope.row.userId) !== 1 && !isAgent" round>绑定服务</el-button>
                        </div>
                     </template>
                  </el-table-column>
               </el-table>
               <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
                  v-model:limit="queryParams.pageSize" @pagination="getList" />
            </el-card>

            <el-card shadow="never" class="mt10" v-if="isSuperAdminAgentAccountPage && ownedAccountPanelVisible">
               <template #header>
                  <div class="owned-account-header">
                     <span>{{ ownedAccountPanelTitle }}</span>
                     <el-button link type="primary" @click="closeOwnedAccountPanel">收起</el-button>
                  </div>
               </template>
               <el-table v-loading="ownedAccountLoading" :data="ownedAccountList">
                  <el-table-column label="用户编号" align="center" prop="userId" width="120" />
                  <el-table-column label="用户名称" align="center" prop="userName" :show-overflow-tooltip="true" width="120" />
                  <el-table-column label="用户昵称" align="center" prop="nickName" :show-overflow-tooltip="true" width="120" />
                  <el-table-column label="角色" align="center" width="100">
                     <template #default>
                        <el-tag type="success">标记下单</el-tag>
                     </template>
                  </el-table-column>
                  <el-table-column label="手机号码" align="center" prop="phonenumber" width="120" />
                  <el-table-column label="状态" align="center" width="100">
                     <template #default="scope">
                        <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'">
                           {{ scope.row.status === '0' ? '启用' : '停用' }}
                        </el-tag>
                     </template>
                  </el-table-column>
                  <el-table-column label="创建时间" align="center" prop="createTime">
                     <template #default="scope">
                        <span>{{ parseTime(scope.row.createTime) }}</span>
                     </template>
                  </el-table-column>
               </el-table>
               <pagination v-show="ownedAccountTotal > 0" :total="ownedAccountTotal" v-model:page="ownedAccountQuery.pageNum"
                  v-model:limit="ownedAccountQuery.pageSize" @pagination="loadOwnedAccounts" />
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
                     <el-select v-model="form.roleIds" multiple placeholder="请选择">
                        <el-option v-for="item in roleOptions" :key="item.roleId" :label="item.roleName"
                           :value="item.roleId" :disabled="item.status == 1"></el-option>
                     </el-select>
                  </el-form-item>
               </el-col>
            </el-row>
            <el-row v-if="isAgentAccountPage && canEditMarkTemplateInForm">
               <el-col :span="12">
                  <el-form-item label="标记模板" prop="relMarkTemplate">
                     <el-select v-model="form.relMarkTemplate" clearable filterable placeholder="请选择用户标记模板">
                        <el-option v-for="item in markTemplateList" :key="item.id" :label="item.templateName"
                           :value="item.id" />
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
      <!-- 标记模板绑定对话框 -->
      <el-dialog :title="markTemplateDialog.title" v-model="markTemplateDialog.open" width="520px" append-to-body>
         <el-form ref="markTemplateRef" :model="markTemplateForm" :rules="markTemplateRules" label-width="90px">
            <el-form-item label="用户名">
               <el-input v-model="markTemplateForm.userName" disabled />
            </el-form-item>
            <el-form-item label="标记模板" prop="relMarkTemplate">
               <el-select
                  v-model="markTemplateForm.relMarkTemplate"
                  placeholder="请选择标记模板"
                  clearable
                  style="width: 100%;"
               >
                  <el-option v-for="item in markTemplateList" :key="item.id" :label="item.templateName" :value="item.id" />
               </el-select>
            </el-form-item>
         </el-form>
         <template #footer>
            <div class="dialog-footer">
               <el-button type="primary" @click="submitMarkTemplateForm">确 定</el-button>
               <el-button @click="markTemplateDialog.open = false">取 消</el-button>
            </div>
         </template>
      </el-dialog>

      <!-- 积分操作对话框 -->
      <el-dialog :title="pointDialog.title" v-model="pointDialog.open" width="400px" append-to-body>
         <el-form ref="pointRef" :model="pointForm" :rules="pointRules" label-width="80px">
            <el-form-item label="用户名" prop="userName">
               <el-input v-model="pointForm.userName" disabled />
            </el-form-item>
            <el-form-item v-if="useAgentQuotaAdjust" label="平台" prop="platformCode">
               <el-select
                  v-model="pointForm.platformCode"
                  placeholder="请选择平台"
                  :loading="pointPlatformLoading"
                  style="width: 100%;"
                  @change="handlePointPlatformChange"
               >
                  <el-option
                     v-for="item in pointPlatformOptions"
                     :key="item.platformCode"
                     :label="`${item.platformName}（剩余 ${item.remainCount || 0} 次）`"
                     :value="item.platformCode"
                  />
               </el-select>
            </el-form-item>
            <el-form-item v-if="useAgentQuotaAdjust" label="剩余次数">
               <span style="color: #67C23A; font-weight: 600;">{{ selectedPointPlatformRemain }}</span>
            </el-form-item>
            <el-form-item :label="useAgentQuotaAdjust ? '次数' : '积分值'" prop="points">
               <el-input-number v-model="pointForm.points" :min="1" :precision="0" :placeholder="useAgentQuotaAdjust ? '请输入次数' : '请输入积分值'"
                  style="width: 100%;" />
            </el-form-item>
            <el-form-item label="备注" prop="remark">
               <el-input v-model="pointForm.remark" type="textarea" placeholder="请输入备注" />
            </el-form-item>
         </el-form>
         <template #footer>
            <div class="dialog-footer">
               <el-button type="primary" @click="submitPointForm">确 定</el-button>
               <el-button @click="pointDialog.open = false">取 消</el-button>
            </div>
         </template>
      </el-dialog>
      <el-dialog :title="platformQuotaDialog.title" v-model="platformQuotaDialog.open" width="520px" append-to-body>
         <el-table v-loading="platformQuotaLoading" :data="platformQuotaList" empty-text="暂无平台次数数据">
            <el-table-column label="平台名称" align="center" prop="platformName" :show-overflow-tooltip="true" min-width="180">
               <template #default="scope">
                  <span>{{ scope.row.platformName || '-' }}</span>
               </template>
            </el-table-column>
            <el-table-column label="平台编码" align="center" prop="platformCode" min-width="140" />
            <el-table-column label="剩余次数" align="center" prop="remainCount" width="110">
               <template #default="scope">
                  <span style="color: #67C23A; font-weight: 600;">{{ scope.row.remainCount }}</span>
               </template>
            </el-table-column>
         </el-table>
         <template #footer>
            <div class="dialog-footer">
               <el-button @click="platformQuotaDialog.open = false">关 闭</el-button>
            </div>
         </template>
      </el-dialog>

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
import { getToken } from "@/utils/auth";
import { getInfo } from "@/api/login";
import { changeUserStatus, listUser, resetUserPwd, delUser, getUser, updateUser, addUser, deptTreeSelect } from "@/api/system/user";
import { getConfigKey } from "@/api/system/config";
import { adjustPointRecord } from "@/api/server/pointRecord";
import { adjustMarkAgentQuota, listMarkAgentQuotaPlatformOptions } from "@/api/server/markAgent";
import { listTemplate } from "@/api/server/template";
import { listMarkTemplateOptions } from "@/api/server/markTemplate";
import { listPlatformConfig } from "@/api/server/platformConfig";
import useUserStore from "@/store/modules/user";

const router = useRouter();
const route = useRoute();
const { proxy } = getCurrentInstance();
const userStore = useUserStore();
const { sys_normal_disable, sys_user_sex } = proxy.useDict("sys_normal_disable", "sys_user_sex");
const canEditUser = computed(() => proxy.$auth.hasPermi('system:user:edit'));
const roleSet = computed(() => new Set(Array.isArray(userStore.roles) ? userStore.roles : []));
const isAgent = computed(() => roleSet.value.has('agent') || roleSet.value.has('mark_agent'));
const isAccountAdmin = computed(() => roleSet.value.has('admin') || roleSet.value.has('mark_admin'));
const canBindMarkTemplate = computed(() => isAgentAccountPage.value && (isAccountAdmin.value || isAgent.value));
const canManageMarkTemplate = computed(() => isAgentAccountPage.value && isAgent.value && proxy.$auth.hasPermi('server:markTemplate:list'));
const canEditMarkTemplateInForm = computed(() => isAgentAccountPage.value && (isAccountAdmin.value || isAgent.value));
const isAgentAccountPage = computed(() => route.path.includes("agentAccount"));
const shouldShowMarkTemplateColumn = computed(() => isAgentAccountPage.value && (isAccountAdmin.value || isAgent.value));
const isSuperAdminAgentAccountPage = computed(() => isAgentAccountPage.value && isAccountAdmin.value);
const useAgentQuotaAdjust = computed(() => isAgentAccountPage.value);
const AGENT_ROLE_KEYS = ["agent", "mark_agent"];
const DOWNSTREAM_ROLE_KEYS = ["user", "mark_user"];

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
const ownedAccountPanelVisible = ref(false);
const ownedAccountLoading = ref(false);
const ownedAccountList = ref([]);
const ownedAccountTotal = ref(0);
const selectedAgentOwner = ref(null);
const ownedAccountQuery = reactive({
   pageNum: 1,
   pageSize: 10
});
const ownedAccountPanelTitle = computed(() => {
   if (!selectedAgentOwner.value) {
      return "名下标记下单账户";
   }
   const ownerName = selectedAgentOwner.value.nickName || selectedAgentOwner.value.userName;
   return `${ownerName}（${selectedAgentOwner.value.userName}）名下标记下单账户（${ownedAccountTotal.value}）`;
});

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
const markTemplateList = ref([]);
const selectedTemplate = ref(null);
const apiNames = ref([]);
const markTemplateDialog = reactive({
   open: false,
   title: ""
});
const loginUserIdCache = ref(null);
const markTemplateForm = reactive({
   userId: null,
   userName: "",
   relMarkTemplate: ""
});
const markTemplateRules = {
   relMarkTemplate: [{ required: true, message: "请选择标记模板", trigger: "change" }]
};
const markTemplateRef = ref(null);
const platformNameMap = ref(new Map());
let platformNameLoadingPromise = null;

function loadMarkTemplateList() {
   return listMarkTemplateOptions().then(response => {
      const list = Array.isArray(response?.data) ? response.data : [];
      const normalizedList = list.map(item => {
         const normalizedId = normalizeMarkTemplateId(item?.id);
         const normalizedName = String(item?.templateName || item?.template_name || "").trim();
         return {
            ...item,
            id: normalizedId,
            templateName: normalizedName || (normalizedId ? `模板#${normalizedId}` : "未命名模板")
         };
      }).filter(item => item.id !== null);
      markTemplateList.value = normalizedList.sort((a, b) => {
         const aDefault = isMarkTemplateDefault(a) ? 1 : 0;
         const bDefault = isMarkTemplateDefault(b) ? 1 : 0;
         if (aDefault !== bDefault) return bDefault - aDefault;
         const aNum = Number(a?.id);
         const bNum = Number(b?.id);
         if (!Number.isNaN(aNum) && !Number.isNaN(bNum)) {
            return bNum - aNum;
         }
         return String(b?.id || "").localeCompare(String(a?.id || ""));
      });
   }).catch(() => {
      markTemplateList.value = [];
   });
}

// 积分操作相关状态
const pointDialog = reactive({
   open: false,
   title: "",
   type: "add" // add-充值, deduct-扣减
});
const pointForm = reactive({
   userId: null,
   userName: "",
   points: 1,
   platformCode: "",
   platformName: "",
   remark: "",
   type: "add"
});
const pointRules = {
   points: [{ required: true, message: "积分值不能为空", trigger: "blur" }],
   platformCode: [{
      validator: (_rule, value, callback) => {
         if (!useAgentQuotaAdjust.value) {
            callback();
            return;
         }
         if (!value) {
            callback(new Error("请选择平台"));
            return;
         }
         callback();
      },
      trigger: "change"
   }]
};
const pointRef = ref(null);
const pointPlatformOptions = ref([]);
const pointPlatformLoading = ref(false);
const platformQuotaDialog = reactive({
   open: false,
   title: ""
});
const platformQuotaList = ref([]);
const platformQuotaLoading = ref(false);
const selectedPointPlatform = computed(() => {
   return pointPlatformOptions.value.find(item => item.platformCode === pointForm.platformCode) || null;
});
const selectedPointPlatformRemain = computed(() => {
   const remain = Number(selectedPointPlatform.value?.remainCount ?? 0);
   return Number.isFinite(remain) ? Math.max(0, remain) : 0;
});

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
   { key: 0, label: `用户编号`, visible: true },
   { key: 1, label: `用户名称`, visible: true },
   { key: 2, label: `用户昵称`, visible: true },
   // { key: 3, label: `部门`, visible: true },
   { key: 3, label: `积分`, visible: true },
   { key: 4, label: `手机号码`, visible: true },
   { key: 5, label: `状态`, visible: true },
   { key: 6, label: `创建时间`, visible: true }
]);

const data = reactive({
   form: {},
   queryParams: {
      pageNum: 1,
      pageSize: 10,
      userName: undefined,
      phonenumber: undefined,
      status: undefined,
      deptId: undefined,
      roleKey: undefined,
      excludeRoleKey: undefined
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
      phonenumber: [{ pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: "请输入正确的手机号码", trigger: "blur" }],
      relMarkTemplate: [{
         validator: (_rule, value, callback) => {
            if (!canEditMarkTemplateInForm.value || isAgent.value) {
               callback();
               return;
            }
            if (value === undefined || value === null || String(value).trim() === "") {
               callback(new Error("请选择标记模板"));
               return;
            }
            callback();
         },
         trigger: "change"
      }]
   }
});

const { queryParams, form, rules } = toRefs(data);
function normalizeMarkTemplateId(value) {
   if (value === undefined || value === null || String(value).trim() === "") {
      return null;
   }
   return String(value).trim();
}

const markTemplateNameMap = computed(() => {
   const map = new Map();
   const options = Array.isArray(markTemplateList.value) ? markTemplateList.value : [];
   options.forEach(item => {
      const id = normalizeMarkTemplateId(item?.id);
      if (id === null) {
         return;
      }
      const name = String(item?.templateName || "").trim();
      map.set(id, name || `模板#${id}`);
   });
   return map;
});

function resolveMarkTemplateName(row) {
   const templateId = normalizeMarkTemplateId(row?.relMarkTemplate);
   if (templateId === null) {
      return "未绑定";
   }
   return markTemplateNameMap.value.get(templateId) || `模板#${templateId}`;
}

function hasBoundMarkTemplate(row) {
   return normalizeMarkTemplateId(row?.relMarkTemplate) !== null;
}

const myMarkTemplateDisplay = computed(() => {
   const templateId = normalizeMarkTemplateId(userStore.relMarkTemplate);
   if (templateId === null) {
      return "未绑定";
   }
   return markTemplateNameMap.value.get(templateId) || `模板#${templateId}`;
});

const myMarkTemplateBound = computed(() => normalizeMarkTemplateId(userStore.relMarkTemplate) !== null);

async function resolveLoginUserId(forceRefresh = false) {
   if (!forceRefresh && loginUserIdCache.value) {
      return loginUserIdCache.value;
   }
   const response = await getInfo();
   loginUserIdCache.value = response?.user?.userId ?? null;
   return loginUserIdCache.value;
}

function resolveBindTemplateActionLabel(row) {
   return hasBoundMarkTemplate(row) ? "改绑模板" : "绑定模板";
}

async function handleBindMyMarkTemplate() {
   const userId = await resolveLoginUserId();
   if (!userId) {
      proxy.$modal.msgError("无法识别当前登录用户");
      return;
   }
   await handleBindMarkTemplate({
      userId,
      userName: userStore.name || userStore.nickName || String(userId)
   });
}

function handleGoMarkTemplatePage() {
   router.push("/mark/markTemplate");
}

function pickAgentDefaultMarkTemplateId() {
   const options = Array.isArray(markTemplateList.value) ? markTemplateList.value : [];
   if (options.length === 0) {
      return null;
   }
   const defaultItem = options.find(item => isMarkTemplateDefault(item));
   if (defaultItem && defaultItem.id !== undefined && defaultItem.id !== null) {
      return normalizeMarkTemplateId(defaultItem.id);
   }
   if (options.length === 1) {
      return normalizeMarkTemplateId(options[0].id);
   }
   return null;
}

function isMarkTemplateDefault(item) {
   const flag = String(item?.isDefault ?? '').trim();
   return flag === '1' || flag.toLowerCase() === 'true';
}

function ensureAgentDefaultMarkTemplateSelected() {
   if (!canEditMarkTemplateInForm.value || isAgent.value) {
      return;
   }
   const current = normalizeMarkTemplateId(form.value.relMarkTemplate);
   if (current !== null) {
      form.value.relMarkTemplate = current;
      return;
   }
   const fallback = pickAgentDefaultMarkTemplateId();
   if (fallback !== null) {
      form.value.relMarkTemplate = fallback;
   }
}

function applyAccountScope() {
   if (isAgentAccountPage.value) {
      if (isAgent.value) {
         queryParams.value.roleKey = undefined;
         queryParams.value.excludeRoleKey = "agent";
      } else {
         queryParams.value.roleKey = "agent";
         queryParams.value.excludeRoleKey = undefined;
      }
      return;
   }
   queryParams.value.roleKey = undefined;
   queryParams.value.excludeRoleKey = "agent";
}
function buildScopedListParams(extra = {}) {
   return proxy.addDateRange({
      ...extra,
      pageNum: 1,
      pageSize: 5000,
      roleKey: undefined,
      excludeRoleKey: undefined
   }, dateRange.value);
}

async function loadUsersByRoleKeys(roleKeys, baseParams = {}) {
   const scopeParams = buildScopedListParams(baseParams);
   const responses = await Promise.all(roleKeys.map(roleKey => listUser({ ...scopeParams, roleKey })));
   const mergedMap = new Map();
   responses.forEach(res => {
      (res?.rows || []).forEach(item => {
         if (item && item.userId !== undefined && item.userId !== null) {
            mergedMap.set(item.userId, item);
         }
      });
   });
   return Array.from(mergedMap.values()).sort((a, b) => Number(a.userId) - Number(b.userId));
}

async function loadAgentAccountUsers() {
   const mergedRows = await loadUsersByRoleKeys(AGENT_ROLE_KEYS, { ...queryParams.value });
   total.value = mergedRows.length;
   const start = (queryParams.value.pageNum - 1) * queryParams.value.pageSize;
   const end = start + queryParams.value.pageSize;
   userList.value = mergedRows.slice(start, end);
}

function closeOwnedAccountPanel() {
   ownedAccountPanelVisible.value = false;
   selectedAgentOwner.value = null;
   ownedAccountList.value = [];
   ownedAccountTotal.value = 0;
   ownedAccountQuery.pageNum = 1;
}

function isCurrentOwner(row) {
   return Boolean(
      ownedAccountPanelVisible.value &&
      selectedAgentOwner.value &&
      row &&
      Number(selectedAgentOwner.value.userId) === Number(row.userId)
   );
}

async function loadOwnedAccounts() {
   if (!selectedAgentOwner.value?.userName) {
      ownedAccountList.value = [];
      ownedAccountTotal.value = 0;
      return;
   }
   ownedAccountLoading.value = true;
   try {
      const mergedRows = await loadUsersByRoleKeys(DOWNSTREAM_ROLE_KEYS, {
         createBy: selectedAgentOwner.value.userName
      });
      ownedAccountTotal.value = mergedRows.length;
      const start = (ownedAccountQuery.pageNum - 1) * ownedAccountQuery.pageSize;
      const end = start + ownedAccountQuery.pageSize;
      ownedAccountList.value = mergedRows.slice(start, end);
   } catch (error) {
      ownedAccountList.value = [];
      ownedAccountTotal.value = 0;
      proxy.$modal.msgError("加载名下账户失败");
   } finally {
      ownedAccountLoading.value = false;
   }
}

function handleToggleOwnedAccounts(row) {
   if (!row || row.userId === undefined || row.userId === null) {
      return;
   }
   if (isCurrentOwner(row)) {
      closeOwnedAccountPanel();
      return;
   }
   selectedAgentOwner.value = {
      userId: row.userId,
      userName: row.userName,
      nickName: row.nickName
   };
   ownedAccountQuery.pageNum = 1;
   ownedAccountPanelVisible.value = true;
   loadOwnedAccounts();
}

function getDefaultAgentDownstreamRoleIds() {
   if (!(isAgentAccountPage.value && isAgent.value)) {
      return [];
   }
   const userRole = roleOptions.value.find(item => ["user", "mark_user"].includes(item.roleKey) && item.status === "0");
   if (userRole && userRole.roleId !== undefined && userRole.roleId !== null) {
      return [userRole.roleId];
   }
   return [];
}

function filterAgentRoleOptions(roles, selectedRoleIds = []) {
   const roleList = Array.isArray(roles) ? roles : [];
   if (!(isAgentAccountPage.value && isAgent.value)) {
      return roleList;
   }
   const selectedSet = new Set((selectedRoleIds || []).map(item => String(item)));
   const hasAgentRoleSelected = roleList.some(item =>
      selectedSet.has(String(item?.roleId)) && ["agent", "mark_agent"].includes(String(item?.roleKey || "").trim())
   );
   const allowedRoleKeys = hasAgentRoleSelected ? ["agent", "mark_agent"] : ["user", "mark_user"];
   return roleList.filter(item =>
      allowedRoleKeys.includes(String(item?.roleKey || "").trim()) && String(item?.status) === "0"
   );
}

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
   if (isSuperAdminAgentAccountPage.value) {
      loadAgentAccountUsers()
         .catch(() => {
            userList.value = [];
            total.value = 0;
            proxy.$modal.msgError("加载代理账户列表失败");
         })
         .finally(() => {
            loading.value = false;
         });
      return;
   }
   applyAccountScope();
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
   if (isSuperAdminAgentAccountPage.value) {
      closeOwnedAccountPanel();
   }
   getList();
};
/** 重置按钮操作 */
function resetQuery() {
   dateRange.value = [];
   proxy.resetForm("queryRef");
   queryParams.value.deptId = undefined;
   proxy.$refs.tree.setCurrentKey(null);
   if (isSuperAdminAgentAccountPage.value) {
      closeOwnedAccountPanel();
   }
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
   let exportParams = { ...queryParams.value };
   if (isSuperAdminAgentAccountPage.value) {
      exportParams = {
         ...exportParams,
         roleKey: AGENT_ROLE_KEYS.join(","),
         excludeRoleKey: undefined
      };
   } else {
      applyAccountScope();
      exportParams = { ...queryParams.value };
   }
   proxy.download("system/user/export", {
      ...exportParams,
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
/** 更多操作 */
function handleCommand(command, row) {
   switch (command) {
      case "handleBindMarkTemplate":
         handleBindMarkTemplate(row);
         break;
      case "handleUpdate":
         handleUpdate(row);
         break;
      case "handleDelete":
         handleDelete(row);
         break;
      case "handleResetPwd":
         handleResetPwd(row);
         break;
      case "handleAuthRole":
         handleAuthRole(row);
         break;
      default:
         break;
   }
};
/** 跳转角色分配 */
function handleAuthRole(row) {
   const userId = row.userId;
   router.push("/system/user-auth/role/" + userId);
};
function handlePointPlatformChange(platformCode) {
   const platform = pointPlatformOptions.value.find(item => item.platformCode === platformCode);
   pointForm.platformName = platform?.platformName || "";
}
async function fetchMarkAgentPlatformQuotaOptions(userId) {
   if (!userId) {
      return [];
   }
   const resp = await listMarkAgentQuotaPlatformOptions(userId);
   const list = Array.isArray(resp?.data) ? resp.data : [];
   return list.map(item => ({
      ...item,
      remainCount: Number.isFinite(Number(item?.remainCount)) ? Number(item.remainCount) : 0
   }));
}

async function loadPointPlatformOptions(userId) {
   if (!useAgentQuotaAdjust.value || !userId) {
      pointPlatformOptions.value = [];
      pointForm.platformCode = "";
      pointForm.platformName = "";
      return;
   }
   pointPlatformLoading.value = true;
   try {
      pointPlatformOptions.value = await fetchMarkAgentPlatformQuotaOptions(userId);
      if (pointPlatformOptions.value.length > 0) {
         pointForm.platformCode = pointPlatformOptions.value[0].platformCode;
         pointForm.platformName = pointPlatformOptions.value[0].platformName || "";
      } else {
         pointForm.platformCode = "";
         pointForm.platformName = "";
      }
   } catch (error) {
      pointPlatformOptions.value = [];
      pointForm.platformCode = "";
      pointForm.platformName = "";
      proxy.$modal.msgError(error?.message || "加载平台次数失败");
   } finally {
      pointPlatformLoading.value = false;
   }
}

async function handleViewAgentPlatformQuota(row) {
   if (!row?.userId) {
      return;
   }
   platformQuotaDialog.title = `平台次数 - ${row.userName || row.userId}`;
   platformQuotaDialog.open = true;
   platformQuotaLoading.value = true;
   platformQuotaList.value = [];
   try {
      platformQuotaList.value = await fetchMarkAgentPlatformQuotaOptions(row.userId);
   } catch (error) {
      platformQuotaList.value = [];
      proxy.$modal.msgError(error?.message || "加载平台次数失败");
   } finally {
      platformQuotaLoading.value = false;
   }
}
/** 积分充值按钮操作 */
async function handleAddPoints(row) {
   resetPointForm();
   pointForm.userId = row.userId;
   pointForm.userName = row.userName;
   pointForm.type = "add";
   pointDialog.type = "add";
   pointDialog.title = (useAgentQuotaAdjust.value ? "平台次数充值 - " : "积分充值 - ") + row.userName;
   if (useAgentQuotaAdjust.value) {
      await loadPointPlatformOptions(row.userId);
      if (pointPlatformOptions.value.length === 0) {
         proxy.$modal.msgWarning("该用户暂无可调整的平台");
      }
   }
   pointDialog.open = true;
}

/** 积分扣减按钮操作 */
async function handleDeductPoints(row) {
   resetPointForm();
   pointForm.userId = row.userId;
   pointForm.userName = row.userName;
   pointForm.type = "deduct";
   pointDialog.type = "deduct";
   pointDialog.title = (useAgentQuotaAdjust.value ? "平台次数扣减 - " : "积分扣减 - ") + row.userName;
   if (useAgentQuotaAdjust.value) {
      await loadPointPlatformOptions(row.userId);
      if (pointPlatformOptions.value.length === 0) {
         proxy.$modal.msgWarning("该用户暂无可调整的平台");
      }
   }
   pointDialog.open = true;
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
   if (isAgentAccountPage.value) {
      proxy.$modal.msgWarning("标记业务账户请在“编辑账号”中绑定标记模板");
      return;
   }
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

/** 重置积分表单 */
function resetPointForm() {
   pointForm.userId = null;
   pointForm.userName = "";
   pointForm.points = 1;
   pointForm.platformCode = "";
   pointForm.platformName = "";
   pointForm.remark = "";
   pointForm.type = "add";
   pointPlatformOptions.value = [];
   pointPlatformLoading.value = false;
   if (pointRef.value) {
      pointRef.value.resetFields();
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

/** 重置标记模板绑定表单 */
function resetMarkTemplateForm() {
   markTemplateForm.userId = null;
   markTemplateForm.userName = "";
   markTemplateForm.relMarkTemplate = "";
   if (markTemplateRef.value) {
      proxy.resetForm("markTemplateRef");
   }
}

function buildUserUpdatePayload(userResponse, patch = {}) {
   const currentUser = userResponse?.data || {};
   const roleIds = Array.isArray(userResponse?.roleIds) && userResponse.roleIds.length
      ? userResponse.roleIds
      : (Array.isArray(currentUser.roleIds) && currentUser.roleIds.length
         ? currentUser.roleIds
         : (Array.isArray(currentUser.roles) ? currentUser.roles.map(role => role.roleId) : []));
   return {
      ...currentUser,
      roleIds,
      ...patch
   };
}

function resolveRequestErrorMessage(error, fallback) {
   const message = String(error?.message || error?.msg || "").trim();
   return message || fallback;
}

/** 标记模板绑定按钮操作 */
async function handleBindMarkTemplate(row) {
   if (!canBindMarkTemplate.value) {
      return;
   }
   resetMarkTemplateForm();
   markTemplateForm.userId = row.userId;
   markTemplateForm.userName = row.userName;
   markTemplateDialog.title = `${hasBoundMarkTemplate(row) ? "改绑" : "绑定"}模板 - ${row.userName}`;
   markTemplateDialog.open = true;
   try {
      const [, userResponse] = await Promise.all([
         loadMarkTemplateList(),
         getUser(row.userId)
      ]);
      markTemplateForm.relMarkTemplate = normalizeMarkTemplateId(userResponse?.data?.relMarkTemplate) || "";
   } catch (error) {
      markTemplateForm.relMarkTemplate = "";
      proxy.$modal.msgError(resolveRequestErrorMessage(error, "加载标记模板信息失败"));
   }
}

/** 提交标记模板绑定表单 */
function submitMarkTemplateForm() {
   proxy.$refs["markTemplateRef"].validate(valid => {
      if (!valid) {
         return;
      }
      const templateId = normalizeMarkTemplateId(markTemplateForm.relMarkTemplate);
      getUser(markTemplateForm.userId).then(userResponse => {
         const updateData = buildUserUpdatePayload(userResponse, {
            relMarkTemplate: templateId
         });
         updateUser(updateData).then(async () => {
            proxy.$modal.msgSuccess("标记模板绑定成功");
            markTemplateDialog.open = false;
            const loginUserId = await resolveLoginUserId();
            if (loginUserId && Number(markTemplateForm.userId) === Number(loginUserId)) {
               userStore.relMarkTemplate = templateId;
            }
            getList();
         }).catch((error) => {
            proxy.$modal.msgError(resolveRequestErrorMessage(error, "标记模板绑定失败"));
         });
      });
   });
}

/** 提交积分表单 */
function submitPointForm() {
   proxy.$refs["pointRef"].validate(valid => {
      if (valid) {
         if (useAgentQuotaAdjust.value) {
            if (!pointForm.platformCode) {
               proxy.$modal.msgError("请选择平台");
               return;
            }
            if (pointDialog.type === "deduct" && Number(pointForm.points || 0) > selectedPointPlatformRemain.value) {
               proxy.$modal.msgError("当前平台剩余次数不足");
               return;
            }
            const request = {
               userId: pointForm.userId,
               platformCode: pointForm.platformCode,
               platformName: pointForm.platformName || selectedPointPlatform.value?.platformName || "",
               adjustType: pointForm.type === "add" ? "ADD" : "SUBTRACT",
               changeCount: Number(pointForm.points || 0),
               remark: pointForm.remark
            };
            adjustMarkAgentQuota(request).then(() => {
               proxy.$modal.msgSuccess(pointForm.type === "add" ? "充值成功" : "扣减成功");
               pointDialog.open = false;
               getList();
            });
            return;
         }

         const pointData = {
            id: null,
            userId: pointForm.userId,
            pointAmount: pointForm.points,
            pointType: pointForm.type === "add" ? "1" : "2",
            reason: pointForm.type === "add" ? "充值" : "扣减",
            remark: pointForm.remark
         };
         adjustPointRecord(pointData).then(() => {
            proxy.$modal.msgSuccess(pointForm.type === "add" ? "充值成功" : "扣减成功");
            pointDialog.open = false;
            getList();
         });
      }
   });
}

/** 提交服务绑定表单 */
function submitServiceForm() {
   proxy.$refs["serviceRef"].validate(valid => {
      if (valid) {
         // 获取当前用户信息
         getUser(serviceForm.userId).then(userResponse => {
            const updateData = buildUserUpdatePayload(userResponse, {
               relTemplate: serviceForm.relTemplate
            });
            
            updateUser(updateData).then(() => {
               proxy.$modal.msgSuccess("服务绑定成功");
               serviceDialog.open = false;
               getList();
            }).catch((error) => {
               proxy.$modal.msgError(resolveRequestErrorMessage(error, "服务绑定失败"));
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
      roleIds: [],
      relMarkTemplate: undefined
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
   const markTemplatePromise = isAgentAccountPage.value ? loadMarkTemplateList() : Promise.resolve();
   Promise.all([getUser(), markTemplatePromise]).then(([response]) => {
      postOptions.value = response.posts;
      roleOptions.value = filterAgentRoleOptions(response.roles, form.value.roleIds);
      const defaultRoleIds = getDefaultAgentDownstreamRoleIds();
      if (defaultRoleIds.length > 0) {
         form.value.roleIds = defaultRoleIds;
      }
      ensureAgentDefaultMarkTemplateSelected();
      open.value = true;
      title.value = "添加用户";
      form.value.password = initPassword.value;
   });
};
/** 修改按钮操作 */
function handleUpdate(row) {
   reset();
   const userId = row.userId || ids.value;
   const markTemplatePromise = isAgentAccountPage.value ? loadMarkTemplateList() : Promise.resolve();
   Promise.all([getUser(userId), markTemplatePromise]).then(([response]) => {
      form.value = response.data;
      postOptions.value = response.posts;
      form.value.postIds = response.postIds;
      form.value.roleIds = response.roleIds;
      roleOptions.value = filterAgentRoleOptions(response.roles, form.value.roleIds);
      form.value.relMarkTemplate = normalizeMarkTemplateId(form.value.relMarkTemplate);
      open.value = true;
      title.value = "修改用户";
      form.value.password = "";
   });
};
/** 提交按钮 */
function submitForm() {
   proxy.$refs["userRef"].validate(valid => {
      if (valid) {
         const normalizedRelMarkTemplate = normalizeMarkTemplateId(form.value.relMarkTemplate);
         const payload = {
            ...form.value,
            relMarkTemplate: normalizedRelMarkTemplate
         };
         if (form.value.userId == undefined && isAgentAccountPage.value && isAgent.value && (!Array.isArray(form.value.roleIds) || form.value.roleIds.length === 0)) {
            const defaultRoleIds = getDefaultAgentDownstreamRoleIds();
            if (defaultRoleIds.length > 0) {
               payload.roleIds = defaultRoleIds;
            }
         }
         if (form.value.userId != undefined) {
            delete payload.password;
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
getConfigKey("sys.user.initPassword").then(res => {
   initPassword.value = res.msg;
});
if (isAgentAccountPage.value) {
   loadMarkTemplateList();
   if (isAgent.value) {
      resolveLoginUserId().catch(() => {});
   }
}
getList();
</script>

<style scoped>
.user-table-page {
   padding-top: 8px;
}

.user-table-page :deep(.user-table-search-card) {
   padding: 8px 10px 2px;
}

.user-table-page :deep(.user-table-search-form) {
   display: flex;
   flex-wrap: wrap;
   align-items: center;
   gap: 4px 8px;
}

.user-table-page :deep(.user-table-search-form .el-form-item) {
   margin-right: 0;
   margin-bottom: 8px;
}

.user-table-page :deep(.user-table-search-form .el-form-item__label) {
   padding-right: 6px;
   font-size: 12px;
}

.user-table-page :deep(.user-table-search-actions) {
   margin-left: auto;
}

.user-table-page .user-table-card {
   margin-top: 8px;
}

.user-table-page .user-table-card :deep(.el-card__body) {
   padding: 10px 12px 12px;
}

.user-table-page .user-table-toolbar {
   margin-bottom: 8px;
   align-items: center;
}

.user-table-page :deep(.user-table-main .el-table__header .cell),
.user-table-page :deep(.user-table-main .el-table__body .cell) {
   font-size: 12px;
   padding: 0 6px;
}

.user-table-page :deep(.user-table-main .el-table__cell) {
   padding: 6px 0;
}

.user-table-page :deep(.user-id-cell .cell) {
   white-space: nowrap;
   font-family: Consolas, Monaco, monospace;
   font-size: 12px;
}

.user-table-template {
   color: #409eff;
   font-weight: 600;
   font-size: 12px;
}

/* 缩小操作按钮高度 */
.el-table .el-button {
   padding: 4px 8px;
   font-size: 14px;
   height: 28px;
   line-height: 1;
}

.user-table-page .operation-actions--table .el-button {
   padding: 0 4px;
   height: auto;
   font-size: 12px;
}

.owned-account-header {
   display: flex;
   align-items: center;
   justify-content: space-between;
}

.operation-actions {
   display: flex;
   align-items: center;
   justify-content: center;
   flex-wrap: wrap;
   gap: 6px;
}

.operation-actions--table {
   flex-wrap: nowrap;
   gap: 2px 6px;
   justify-content: flex-start;
}

.user-template-banner {
   margin-bottom: 8px;
}

.user-template-banner__content {
   display: flex;
   align-items: center;
   justify-content: space-between;
   gap: 12px;
   width: 100%;
}

.user-template-banner__hint {
   margin-left: 6px;
   color: var(--el-color-warning);
   font-size: 12px;
}

.user-template-banner__actions {
   display: flex;
   align-items: center;
   gap: 8px;
   flex-shrink: 0;
}

.mark-template-dialog-hint {
   margin-bottom: 12px;
}
</style>
