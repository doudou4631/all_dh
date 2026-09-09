<script setup lang="ts">
import { ElMessageBox, ElDialog, ElIcon } from 'element-plus'
import { Warning } from '@element-plus/icons-vue'
import { ref } from 'vue'
import Breadcrumb from './Breadcrumb.vue'
import TopNav from './TopNav.vue'
import RuoYiGitee from './RuoYi/Git/gitee.vue'
import RuoYiGithub from './RuoYi/Git/github.vue'
import RuoYiDoc from './RuoYi/Doc/index.vue'
import Hamburger from '@/components/Hamburger/index.vue'
import Screenfull from '@/components/Screenfull/index.vue'
import SizeSelect from '@/components/SizeSelect/index.vue'
import HeaderSearch from '@/components/HeaderSearch/index.vue'
import useAppStore from '@/store/modules/app'
import useUserStore from '@/store/modules/user'
import useSettingsStore from '@/store/modules/settings'
import { useRouter } from 'vue-router'
import { RoutesAlias } from '@/router/routesAlias'

const appStore = useAppStore()
const userStore = useUserStore()
const settingsStore = useSettingsStore()
const router = useRouter()

// 免责声明弹窗状态
const disclaimerVisible = ref(false)

// 打开免责声明
function openDisclaimer() {
  disclaimerVisible.value = true
}

// 关闭免责声明
function closeDisclaimer() {
  disclaimerVisible.value = false
}

function handleCommand(command: string) {
  switch (command) {
    case "setLayout":
      setLayout();
      break;
    case "logout":
      logout();
      break;
    default:
      break;
  }
}

function logout() {
  ElMessageBox.confirm('确定注销并退出系统吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.logOut().then(() => {
      location.href = router.resolve(RoutesAlias.Home).href;
    })
  }).catch(() => { });
}

const emits = defineEmits(['setLayout'])
function setLayout() {
  emits('setLayout');
}
</script>
<template>
  <div class="navbar">
    <!-- 侧边栏切换按钮 -->
    <hamburger id="hamburger-container" :is-active="appStore.sidebar.opened"
      @toggleClick="appStore.toggleSideBar(false)" />

    <!-- 顶部导航栏 -->
    <top-nav id="topmenu-container" v-if="settingsStore.topNav" />
    <!-- 面包屑导航栏 -->
    <breadcrumb id="breadcrumb-container" v-else />

    <!-- 右侧菜单 -->
    <div class="right-menu">
      <template v-if="appStore.device !== 'mobile'">
        <el-tooltip content="免责声明" effect="dark" placement="bottom">
          <div id="disclaimer" class="right-menu-item hover-effect" @click="openDisclaimer">
            <el-icon>
              <Warning />
            </el-icon>
            <span style="margin-left: 5px; font-size: 14px;">免责声明</span>
          </div>
        </el-tooltip>

        <header-search id="header-search" class="right-menu-item" />

        <el-tooltip content="专注模式" effect="dark" placement="bottom">
          <screenfull id="screenfull" class="right-menu-item hover-effect svg-menu-item" />
        </el-tooltip>

        <el-tooltip content="布局大小" effect="dark" placement="bottom">
          <size-select id="size-select" class="right-menu-item hover-effect svg-menu-item" />
        </el-tooltip>
      </template>
      <el-dropdown @command="handleCommand" class="right-menu-item hover-effect" trigger="click">
        <div class="avatar-wrapper">
          <el-avatar :size="30" :src="userStore.avatar" />
          <span>{{ userStore.name }}</span>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <router-link to="/user/profile">
              <el-dropdown-item>个人中心</el-dropdown-item>
            </router-link>
            <el-dropdown-item command="setLayout">
              <span>布局设置</span>
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <span>退出登录</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <div class="right-menu-item hover-effect" @click="setLayout">
        <svg-icon icon-class="more-up" />
      </div>
    </div>

    <!-- 免责声明弹窗 -->
    <el-dialog v-model="disclaimerVisible" title="免责声明" width="800px" append-to-body destroy-on-close>
      <div class="disclaimer-content">
        <p>请在使用本系统前仔细阅读以下内容</p>

        <h4>法律声明</h4>
        <p>本站（"号码标记查询系统"）严格遵守《中华人民共和国网络安全法》、《中华人民共和国个人信息保护法》等相关法律法规，仅提供号码标记信息查询技术服务，不涉及任何非法数据采集、存储或处理行为。</p>

        <h4>数据来源说明</h4>
        <p>数据获取方式：本系统通过模拟常规浏览器访问行为，从公开网络渠道获取号码标记信息，所有数据获取均在法律允许范围内进行，不存在任何技术攻击、数据窃取或隐私侵犯行为。</p>
        <p>数据性质：查询结果来源于网络公开的实时数据反馈，通过合法、透明的方式获取，不涉及公民个人敏感信息的收集。</p>

        <h4>查询结果声明</h4>
        <p>准确性声明：因网络环境、数据源稳定性、技术限制等因素影响，查询结果可能存在延迟、偏差或不完整情况。用户应当对查询结果的准确性进行独立核实。</p>
        <p>法律效力：本系统提供的所有查询结果仅供用户参考，不具有任何法律效力，不作为法律诉讼、商业决策或其他正式用途的依据。</p>

        <h4>服务性质说明</h4>
        <p>技术服务费：用户支付的费用为代查询技术服务费，该费用基于查询复杂度、资源消耗等成本因素由系统自动计算，收费标准可能根据实际情况动态调整。</p>
        <p>服务范围：因技术壁垒、网络稳定性及第三方平台限制等因素，本系统无法保证覆盖所有号码标记来源，查询结果可能存在局限性。</p>

        <h4>用户责任声明</h4>
        <p>合理使用：用户应保证使用本系统符合国家法律法规要求，不得将查询结果用于任何非法用途。</p>
        <p>自行判断：用户应对查询结果进行审慎判断，因依赖查询结果而产生的任何直接或间接损失，本系统不承担相应责任。</p>

        <h4>权利保留</h4>
        <p>服务变更：本站保留在不另行通知的情况下，调整服务内容、收费标准及技术实现方式的权利。</p>
        <p>免责范围：对于因不可抗力、第三方服务中断、网络攻击或其他不可归责于本站的原因导致的服务中断或数据错误，本站不承担责任。</p>

        <p>请在使用本系统前仔细阅读并理解本免责声明的全部内容。如继续使用本系统，即表示您已阅读、理解并同意接受本免责声明的所有条款。</p>
      </div>
    </el-dialog>
  </div>
</template>



<style lang='scss' scoped>
@use "@/assets/styles/variables.module.scss";

.navbar {
  height: variables.$navbar-height;
  overflow: hidden;
  position: relative;
  background: variables.$navbar-color;

  @if variables.$navbar-color !=variables.$page-background-color {
    box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  }

  #hamburger-container {
    line-height: 46px;
    height: 100%;
    float: left;
    cursor: pointer;
    transition: background 0.3s;
    -webkit-tap-highlight-color: transparent;

    &:hover {
      background: rgba(0, 0, 0, 0.025);
    }
  }

  #breadcrumb-container {
    float: left;
  }

  #topmenu-container {
    position: absolute;
    left: 50px;
  }

  .errLog-container {
    display: inline-block;
    vertical-align: top;
  }

  .right-menu {
    float: right;
    height: 100%;
    line-height: 50px;
    display: flex;
    align-items: center;
    justify-content: center;

    &:focus {
      outline: none;
    }

    .right-menu-item {
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 0 8px;
      font-size: 18px;
      color: #5a5e66;

      &.svg-menu-item {
        height: 38px;
        width: 38px;
        border-radius: 8px;
        margin-left: 8px;
      }

      &:not(.svg-menu-item) {
        height: 100%;
      }

      &.hover-effect {
        cursor: pointer;
        transition: background 0.3s;

        &:hover {
          background: #f4f4f6;
        }
      }
    }


    .avatar-wrapper {
      display: flex;
      align-items: center;
      justify-content: center;

      span {
        margin-left: 8px;
        color: #5a5e66;
        font-size: 14px;
        font-weight: bold;
      }
    }
  }

  :deep(.disclaimer-content) {
    max-height: 500px;
    overflow-y: auto;
    line-height: 1.8;
    font-size: 14px;
    color: #333;

    h3 {
      text-align: center;
      margin-bottom: 20px;
      font-size: 18px;
      color: #1f2937;
      font-weight: bold !important;
    }

    h4 {
      margin: 20px 0 10px 0;
      font-size: 16px;
      color: #111827;
      border-bottom: 1px solid #e5e7eb;
      padding-bottom: 5px;
      font-weight: bold !important;
    }

    p {
      margin: 10px 0;
      text-align: justify;
    }

    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-track {
      background: #f1f1f1;
      border-radius: 3px;
    }

    &::-webkit-scrollbar-thumb {
      background: #c1c1c1;
      border-radius: 3px;
    }

    &::-webkit-scrollbar-thumb:hover {
      background: #a1a1a1;
    }
  }
}
</style>
