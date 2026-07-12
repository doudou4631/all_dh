<template>
  <view class="detail-page">
    <uni-card :is-shadow="false" class="detail-card">
      <view v-if="loading" class="empty-box">详情加载中...</view>
      <template v-else>
        <view class="detail-title">{{ detailOrder.platformName || '订单详情' }}</view>
        <view class="info-row"><text>订单号</text><text>{{ detailOrder.orderNo || '-' }}</text></view>
        <view class="info-row"><text>平台</text><text>{{ detailOrder.platformName || '-' }}</text></view>
        <view class="info-row"><text>状态</text><uni-tag :text="recordStatusLabel(detailOrder)" :type="recordStatusType(detailOrder)" size="small" /></view>
        <view class="info-row"><text>提交时间</text><text>{{ formatDateTime(detailOrder.createTime) }}</text></view>
      </template>
    </uni-card>

    <uni-section title="号码明细" type="line"></uni-section>
    <view v-if="!loading && !detailItems.length" class="empty-box">暂无明细</view>
    <uni-card v-for="item in detailItems" :key="item.id || item.phone" :is-shadow="false" class="item-card">
      <view class="item-head">
        <view class="item-phone">{{ item.phone || '-' }}</view>
        <uni-tag :text="itemStatusLabel(item.processStatus)" :type="itemStatusType(item.processStatus)" size="small" />
      </view>
      <view class="item-result">{{ item.processResult || item.processNote || item.remark || '-' }}</view>
    </uni-card>
  </view>
</template>

<script setup>
  import { ref } from 'vue'
  import { onLoad } from '@dcloudio/uni-app'
  import { getMarkUserOrderDetail } from '@/api/markUser'
  import { formatDateTime, itemStatusLabel, itemStatusType, recordStatusLabel, recordStatusType } from '@/utils/markUser'

  const loading = ref(false)
  const detailOrder = ref({})
  const detailItems = ref([])

  function loadDetail(orderId) {
    if (!orderId) return
    loading.value = true
    getMarkUserOrderDetail(orderId).then(res => {
      detailOrder.value = res.data?.order || {}
      detailItems.value = Array.isArray(res.data?.items) ? res.data.items : []
    }).finally(() => {
      loading.value = false
    })
  }

  onLoad(options => {
    loadDetail(options.orderId)
  })
</script>

<style lang="scss" scoped>
  page { background: #f5f6f7; }
  .detail-page { padding: 20rpx 20rpx 40rpx; }
  .empty-box { padding: 90rpx 20rpx; color: #8792a6; text-align: center; font-size: 28rpx; }
  .detail-title { margin-bottom: 18rpx; color: #1f2937; font-size: 34rpx; font-weight: 700; }
  .info-row { display: flex; justify-content: space-between; align-items: center; gap: 24rpx; padding: 18rpx 0; border-bottom: 1rpx solid #eef0f4; color: #344054; font-size: 26rpx; }
  .info-row text:first-child { color: #8792a6; }
  .item-card { margin-bottom: 18rpx; }
  .item-head { display: flex; justify-content: space-between; align-items: center; }
  .item-phone { color: #1f2937; font-size: 31rpx; font-weight: 700; }
  .item-result { margin-top: 14rpx; color: #667085; font-size: 25rpx; line-height: 1.6; }
</style>
