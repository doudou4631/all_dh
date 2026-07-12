<template>
  <view class="orders-page">
    <uni-card :is-shadow="false" class="filter-card">
      <uni-search-bar v-model="keyword" placeholder="订单号/手机号" cancelButton="none" @confirm="refresh" @clear="refresh" />
      <button class="primary-btn" :disabled="loading" @click="refresh">{{ loading ? '查询中...' : '查询' }}</button>
    </uni-card>

    <view v-if="loading && !orders.length" class="empty-box">记录加载中...</view>
    <view v-else-if="!orders.length" class="empty-box">暂无任务记录</view>

    <view v-else class="order-list">
      <uni-card v-for="item in orders" :key="`${item.id}-${item.itemId || ''}`" :is-shadow="false" class="order-card">
        <view class="order-head">
          <view>
            <view class="order-platform">{{ item.platformName || '平台' }}</view>
            <view class="order-no">{{ item.orderNo || '-' }}</view>
          </view>
          <uni-tag :text="recordStatusLabel(item)" :type="recordStatusType(item)" size="small" />
        </view>
        <view class="order-grid">
          <view><text>号码</text><text>{{ item.phonePreview || '-' }}</text></view>
          <view><text>数量</text><text>{{ item.totalCount || 1 }}</text></view>
          <view><text>扣次</text><text>{{ item.totalAmount ?? '-' }}</text></view>
          <view><text>时间</text><text>{{ formatDateTime(item.createTime) }}</text></view>
        </view>
        <view class="order-actions">
          <button class="light-btn" @click="copyOrderNo(item.orderNo)">复制订单号</button>
          <button class="primary-mini-btn" @click="openDetail(item)">查看详情</button>
        </view>
      </uni-card>
    </view>

    <uni-load-more v-if="orders.length" :status="loadMoreStatus" @clickLoadMore="loadMore" />
  </view>
</template>

<script setup>
  import { computed, ref } from 'vue'
  import { onShow } from '@dcloudio/uni-app'
  import { listMarkUserOrder } from '@/api/markUser'
  import { formatDateTime, recordStatusLabel, recordStatusType } from '@/utils/markUser'

  const keyword = ref('')
  const loading = ref(false)
  const orders = ref([])
  const pageNum = ref(1)
  const pageSize = 10
  const total = ref(0)

  const hasMore = computed(() => orders.value.length < total.value)
  const loadMoreStatus = computed(() => loading.value ? 'loading' : (hasMore.value ? 'more' : 'noMore'))

  function buildQuery() {
    const text = String(keyword.value || '').trim()
    return {
      pageNum: pageNum.value,
      pageSize,
      keyword: text || null,
      phone: /^\d{7,15}$/.test(text) ? text : null
    }
  }

  function fetchOrders(append = false) {
    loading.value = true
    listMarkUserOrder(buildQuery()).then(res => {
      const rows = Array.isArray(res.rows) ? res.rows : []
      orders.value = append ? orders.value.concat(rows) : rows
      total.value = Number(res.total || rows.length || 0)
    }).catch(() => {
      if (!append) orders.value = []
    }).finally(() => {
      loading.value = false
    })
  }

  function refresh() {
    pageNum.value = 1
    fetchOrders(false)
  }

  function loadMore() {
    if (!hasMore.value || loading.value) return
    pageNum.value += 1
    fetchOrders(true)
  }

  function openDetail(item) {
    if (!item.id) {
      uni.showToast({ title: '订单不存在', icon: 'none' })
      return
    }
    uni.navigateTo({ url: `/pages/mark/detail?orderId=${item.id}&itemId=${item.itemId || ''}` })
  }

  function copyOrderNo(orderNo) {
    const text = String(orderNo || '').trim()
    if (!text) {
      uni.showToast({ title: '没有可复制内容', icon: 'none' })
      return
    }
    uni.setClipboardData({ data: text })
  }

  onShow(() => {
    refresh()
  })
</script>

<style lang="scss" scoped>
  page { background: #f5f6f7; }
  .orders-page { padding: 20rpx 20rpx 40rpx; }
  .primary-btn { height: 72rpx; line-height: 72rpx; border-radius: 36rpx; color: #fff; background: #1f6bff; font-size: 28rpx; }
  .empty-box { padding: 100rpx 20rpx; color: #8792a6; text-align: center; font-size: 28rpx; }
  .order-card { margin-bottom: 18rpx; }
  .order-head { display: flex; justify-content: space-between; gap: 20rpx; align-items: flex-start; }
  .order-platform { color: #1f2937; font-size: 31rpx; font-weight: 700; }
  .order-no { margin-top: 6rpx; color: #8792a6; font-size: 23rpx; }
  .order-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 18rpx 26rpx; margin-top: 24rpx; }
  .order-grid view { display: flex; flex-direction: column; gap: 6rpx; }
  .order-grid text:first-child { color: #8792a6; font-size: 23rpx; }
  .order-grid text:last-child { color: #344054; font-size: 25rpx; }
  .order-actions { display: grid; grid-template-columns: 1fr 1fr; gap: 18rpx; margin-top: 24rpx; }
  .light-btn, .primary-mini-btn { height: 64rpx; line-height: 64rpx; border-radius: 32rpx; font-size: 25rpx; }
  .light-btn { color: #1f6bff; background: #eef5ff; }
  .primary-mini-btn { color: #fff; background: #1f6bff; }
</style>
