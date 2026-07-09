<template>
  <div class="app-container home">
    <section class="home-hero">
      <div class="home-hero__profile">
        <el-avatar :size="56" :src="userInfo.avatar || profile" />
        <div class="home-hero__profile-main">
          <h2 class="home-hero__welcome">欢迎回来，{{ userInfo.name }}</h2>
          <p class="home-hero__role">{{ userInfo.roleName || '用户' }}</p>
          <p class="home-hero__login">上次登录：{{ formatLoginTime(userInfo.loginDate) }}</p>
        </div>
      </div>

      <div v-if="walletSummaryVisible" class="home-hero__stats">
        <div class="home-stat home-stat--primary" @click="goPage('/mark/userWallet')">
          <span class="home-stat__label">当前积分</span>
          <span class="home-stat__value">{{ walletSummary.pointsBalance }}</span>
        </div>
        <div class="home-stat" @click="goPage('/mark/userWallet')">
          <span class="home-stat__label">累计扣费</span>
          <span class="home-stat__value">{{ walletSummary.totalDeductAmount }}</span>
        </div>
        <div class="home-stat" @click="goPage('/mark/userWallet')">
          <span class="home-stat__label">累计退款</span>
          <span class="home-stat__value">{{ walletSummary.totalRefundAmount }}</span>
        </div>
      </div>
    </section>

    <section class="home-grid">
      <el-card class="home-panel home-panel--notice" shadow="never" v-loading="noticeLoading">
        <template #header>
          <div class="home-panel__header">
            <span class="home-panel__title">
              <el-icon><Bell /></el-icon>
              系统公告
            </span>
            <el-button v-if="noticeList.length" link type="primary" @click="viewMoreNotices">
              查看更多<el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
        </template>
        <div v-if="noticeList.length" class="message-list">
          <div
            v-for="item in noticeList"
            :key="item.noticeId"
            class="message-item message-item--notice"
            @click="showNoticeDetail(item)"
          >
            <el-tag size="small" :type="item.noticeType === '1' ? 'danger' : 'success'">
              {{ item.noticeType === '1' ? '通知' : '公告' }}
            </el-tag>
            <div class="message-item__main">
              <div class="message-item__title">{{ item.noticeTitle }}</div>
            </div>
            <span class="message-item__time">{{ formatShortTime(item.createTime) }}</span>
          </div>
        </div>
        <el-empty v-else description="暂无公告" :image-size="48" />
      </el-card>
    </section>

    <el-dialog v-model="noticeDialogVisible" :title="currentNotice.noticeTitle" width="50%" destroy-on-close>
      <div class="notice-dialog-content" v-html="currentNotice.noticeContent"></div>
    </el-dialog>
  </div>
</template>

<script setup name="Index" lang="ts">
import { ref, onMounted } from 'vue'
import { listNotice } from '@/api/system/notice'
import { parseTime } from '@/utils/ruoyi'
import { Bell, ArrowRight } from '@element-plus/icons-vue'
import profile from '@/assets/images/profile.jpg'
import useUserStore from '@/store/modules/user'
import { useRouter, type RouteLocationRaw } from 'vue-router'
import { GeekResponseForList } from '@/types/request'
import { getMarkUserWalletSummary } from '@/api/server/markUser'
import auth from '@/plugins/auth'

const router = useRouter()

interface Notice {
  noticeId: number
  noticeTitle: string
  noticeType: string
  noticeContent: string
  status: string
  createBy: string
  createTime: string
}

const noticeList = ref<Notice[]>([])
const noticeLoading = ref(false)
const noticeDialogVisible = ref(false)
const currentNotice = ref<Notice>({} as Notice)
const walletSummaryVisible = ref(false)
const walletSummary = ref({
  pointsBalance: 0,
  totalDeductAmount: 0,
  totalRefundAmount: 0
})

const userInfo = useUserStore()

function formatLoginTime(value?: string) {
  if (!value) return '暂无'
  const text = parseTime(value) as string
  return text || '暂无'
}

function formatShortTime(value?: string) {
  if (!value) return '-'
  const text = String(parseTime(value) || '')
  if (!text) return '-'
  return text.length > 16 ? text.slice(5, 16) : text
}

function goPage(path: string) {
  router.push(path)
}

const getNoticeList = async () => {
  noticeLoading.value = true
  try {
    const res: GeekResponseForList<Notice> = await listNotice({ pageNum: 1, pageSize: 5 })
    noticeList.value = res.rows || []
  } catch (error) {
    console.error('获取公告列表失败:', error)
  } finally {
    noticeLoading.value = false
  }
}

const showNoticeDetail = (notice: Notice) => {
  currentNotice.value = notice
  noticeDialogVisible.value = true
}

const viewMoreNotices = () => {
  const route: RouteLocationRaw = { path: '/userModel/notice' }
  router.push(route)
}

const getWalletSummary = async () => {
  if (!auth.hasPermi('server:markUser:wallet:list')) {
    walletSummaryVisible.value = false
    return
  }
  try {
    const res: any = await getMarkUserWalletSummary()
    const data = res?.data || {}
    walletSummary.value = {
      pointsBalance: Number(data.pointsBalance) || 0,
      totalDeductAmount: Number(data.totalDeductAmount) || 0,
      totalRefundAmount: Number(data.totalRefundAmount) || 0
    }
    walletSummaryVisible.value = true
  } catch {
    walletSummaryVisible.value = false
  }
}

onMounted(() => {
  userInfo.getInfo()
  getNoticeList()
  getWalletSummary()
})
</script>

<style scoped lang="scss">
.home {
  padding: 12px 16px 16px;
  background: var(--el-bg-color-page);
  min-height: calc(100vh - 84px);
}

.home-hero {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding: 14px 16px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid var(--el-border-color-lighter);
}

.home-hero__profile {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  flex: 1 1 280px;
}

.home-hero__profile-main {
  min-width: 0;
}

.home-hero__welcome {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  line-height: 1.35;
}

.home-hero__role {
  margin: 0 0 4px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.home-hero__login {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.home-hero__stats {
  display: flex;
  align-items: stretch;
  gap: 10px;
  flex: 0 1 420px;
}

.home-stat {
  flex: 1;
  min-width: 96px;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--el-fill-color-light);
  text-align: center;
  cursor: pointer;
  transition: background 0.2s ease, transform 0.2s ease;

  &:hover {
    background: var(--el-fill-color);
    transform: translateY(-1px);
  }
}

.home-stat--primary {
  background: #ecf5ff;
  border: 1px solid #d9ecff;

  .home-stat__value {
    color: var(--el-color-primary);
  }
}

.home-stat__label {
  display: block;
  margin-bottom: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.home-stat__value {
  display: block;
  font-size: 22px;
  line-height: 1.2;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.home-grid {
  display: block;
}

.home-panel--notice {
  min-height: auto;
}

.home-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.home-panel__title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.home-panel__badge {
  margin-left: 4px;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  flex: 1;
}

.message-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  cursor: pointer;
  min-width: 0;

  &:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }

  &:hover {
    opacity: 0.88;
  }
}

.message-item--notice {
  align-items: center;
}

.message-item__main {
  flex: 1;
  min-width: 0;
}

.message-item__title {
  font-size: 13px;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-item__time {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  line-height: 1.6;
}

.notice-dialog-content {
  line-height: 1.7;
  word-break: break-word;
}

:deep(.home-panel.el-card) {
  --el-card-padding: 14px;
  border-radius: 10px;
  display: flex;
  flex-direction: column;

  .el-card__header {
    padding: 12px 14px;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  .el-card__body {
    padding: 10px 14px 14px;
    flex: 1;
  }
}

@media (max-width: 992px) {
  .home-hero {
    flex-direction: column;
  }

  .home-hero__stats {
    width: 100%;
    flex: none;
  }
}

@media (max-width: 768px) {
  .home {
    padding: 10px 12px 14px;
  }

  .home-stat__value {
    font-size: 18px;
  }
}
</style>
