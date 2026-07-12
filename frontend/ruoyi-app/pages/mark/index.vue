<template>
  <view class="mark-page">
    <view class="hero-card">
      <view>
        <view class="hero-kicker">标记用户端</view>
        <view class="hero-title">手机端提交清除</view>
        <view class="hero-subtitle">选择平台、提交号码、处理短信验证码</view>
      </view>
      <button class="hero-btn" @click="goOrders">记录</button>
    </view>

    <uni-card :is-shadow="false" class="summary-card">
      <view class="summary-row">
        <view>
          <view class="summary-label">当前账号</view>
          <view class="summary-value">{{ userStore.name || '已登录' }}</view>
        </view>
        <button class="plain-btn" @click="loadPlatforms">刷新</button>
      </view>
    </uni-card>

    <uni-section title="可用平台" type="line"></uni-section>

    <view v-if="loading" class="empty-box">平台加载中...</view>
    <view v-else-if="!platforms.length" class="empty-box">暂无可用平台或账号未配置额度</view>
    <view v-else class="platform-list">
      <uni-card v-for="item in platforms" :key="item.platformCode || item.platformName" :is-shadow="false" class="platform-card">
        <view class="platform-head">
          <view class="platform-icon">{{ platformInitial(item.platformName) }}</view>
          <view class="platform-title-wrap">
            <view class="platform-title">{{ item.platformName || item.platformCode || '平台' }}</view>
            <view class="platform-desc">{{ platformModeText(item) }}</view>
          </view>
          <uni-tag :text="isPlatformEnabled(item) ? '已开启' : '已关闭'" :type="isPlatformEnabled(item) ? 'success' : 'error'" size="small" />
        </view>
        <view class="platform-meta">
          <view>单价 <text>{{ formatUnitPrice(item.unitPrice) }}</text> 次</view>
          <view>剩余 <text>{{ formatRemain(item.remainCount) }}</text> 次</view>
        </view>
        <button class="primary-btn" :disabled="!isPlatformEnabled(item)" @click="goSubmit(item)">
          去提交
        </button>
      </uni-card>
    </view>
  </view>
</template>

<script setup>
  import { ref } from 'vue'
  import { onShow } from '@dcloudio/uni-app'
  import { useUserStore } from '@/store'
  import { listMarkUserPlatformPrice } from '@/api/markUser'
  import { formatRemain, formatUnitPrice, isPlatformEnabled, platformModeText } from '@/utils/markUser'

  const userStore = useUserStore()
  const loading = ref(false)
  const platforms = ref([])

  function platformInitial(name) {
    const text = String(name || '').trim()
    if (!text) return '标'
    if (text.includes('360')) return '360'
    return text.slice(0, 2)
  }

  function loadPlatforms() {
    loading.value = true
    listMarkUserPlatformPrice().then(res => {
      platforms.value = Array.isArray(res.data) ? res.data : []
    }).catch(() => {
      platforms.value = []
    }).finally(() => {
      loading.value = false
    })
  }

  function goSubmit(item) {
    if (!isPlatformEnabled(item)) {
      uni.showToast({ title: '当前平台未开启', icon: 'none' })
      return
    }
    const code = encodeURIComponent(String(item.platformCode || ''))
    const name = encodeURIComponent(String(item.platformName || ''))
    uni.navigateTo({ url: `/pages/mark/submit?platformCode=${code}&platformName=${name}` })
  }

  function goOrders() {
    uni.switchTab({ url: '/pages/mark/orders' })
  }

  onShow(() => {
    loadPlatforms()
  })
</script>

<style lang="scss" scoped>
  page { background: #f5f6f7; }
  .mark-page { padding: 24rpx; }
  .hero-card {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 34rpx 28rpx;
    border-radius: 22rpx;
    color: #fff;
    background: linear-gradient(135deg, #1f6bff, #35b6ff);
    box-shadow: 0 16rpx 36rpx rgba(31, 107, 255, .22);
  }
  .hero-kicker { font-size: 24rpx; opacity: .9; }
  .hero-title { margin-top: 8rpx; font-size: 40rpx; font-weight: 700; }
  .hero-subtitle { margin-top: 8rpx; font-size: 24rpx; opacity: .92; }
  .hero-btn { margin: 0; padding: 0 24rpx; height: 64rpx; line-height: 64rpx; border-radius: 32rpx; font-size: 26rpx; color: #1f6bff; background: #fff; }
  .summary-card { margin-top: 22rpx; }
  .summary-row { display: flex; justify-content: space-between; align-items: center; }
  .summary-label { color: #8792a6; font-size: 24rpx; }
  .summary-value { margin-top: 6rpx; color: #1f2937; font-size: 30rpx; font-weight: 600; }
  .plain-btn { margin: 0; padding: 0 24rpx; height: 58rpx; line-height: 58rpx; border-radius: 29rpx; font-size: 24rpx; color: #1f6bff; background: #eef5ff; }
  .empty-box { padding: 80rpx 20rpx; color: #8792a6; text-align: center; font-size: 28rpx; }
  .platform-list { padding-bottom: 24rpx; }
  .platform-card { margin-bottom: 18rpx; }
  .platform-head { display: flex; align-items: center; gap: 18rpx; }
  .platform-icon { display: flex; align-items: center; justify-content: center; width: 72rpx; height: 72rpx; border-radius: 18rpx; color: #1f6bff; background: #edf5ff; font-size: 26rpx; font-weight: 700; }
  .platform-title-wrap { flex: 1; min-width: 0; }
  .platform-title { font-size: 31rpx; font-weight: 700; color: #1f2937; }
  .platform-desc { margin-top: 6rpx; color: #8792a6; font-size: 24rpx; }
  .platform-meta { display: flex; gap: 28rpx; margin: 24rpx 0; color: #667085; font-size: 25rpx; }
  .platform-meta text { color: #1f6bff; font-weight: 700; }
  .primary-btn { height: 74rpx; line-height: 74rpx; border-radius: 37rpx; color: #fff; background: #1f6bff; font-size: 28rpx; }
  .primary-btn[disabled] { background: #b8c1d1; color: #fff; }
</style>
