<template>
  <view></view>
</template>

<script setup>
  import { onLoad } from '@dcloudio/uni-app'

  onLoad(() => {
    uni.reLaunch({ url: '/pages/mark/index' })
  })
</script>
