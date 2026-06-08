<template>
  <div class="sub-page">
    <a class="sub-back" :href="resolveHref('/profile/')">‹ 返回个人中心</a>
    <div class="sub-card">
      <h1>号码标记清除服务用户协议</h1>
      <p class="sub-subtitle">请仔细阅读以下协议内容</p>
      <p>
        本协议是您（或称“用户”）与我们（以下简称“号码标记清除”）所订立的协议。除非您接受本协议所有条款，否则您无权注册、登录或使用本协议所涉相关服务。您的注册、登录、使用等行为将视为对本协议的接受。
      </p>
      <h2>一、服务内容</h2>
      <p>我们向用户提供号码标记查询、标记清除申诉指引及相关增值服务。具体服务内容以平台实际提供为准。</p>
      <h2>二、用户义务</h2>
      <p>用户应保证所提交号码信息真实、合法，不得利用本服务从事违法违规活动。用户应妥善保管账号信息。</p>
      <h2>三、费用说明</h2>
      <p>部分功能（如批量查询）为付费服务，收费标准以页面展示或客服告知为准。用户支付成功后，对应权益将在账户中生效。</p>
      <h2>四、免责声明</h2>
      <p>标记查询结果来源于各第三方平台，我们不对第三方平台的审核时效、处理结果作绝对保证。</p>
      <h2>五、协议变更</h2>
      <p>我们有权根据需要更新本协议，更新后的协议将在平台公布，继续使用服务即视为接受更新内容。</p>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useMobileRuntimeConfig } from '@/runtime/mobileRuntimeConfig'

const { resolveHref, loadMobileRuntimeConfig } = useMobileRuntimeConfig()

onMounted(() => {
  loadMobileRuntimeConfig()
})
</script>

<style scoped>
.sub-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 16px 15px;
  box-sizing: border-box;
}

.sub-back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #3b71fe;
  text-decoration: none;
  font-size: 14px;
  margin-bottom: 16px;
}

.sub-card {
  background: #fff;
  border-radius: 14px;
  padding: 20px 16px;
  box-shadow: 0 2px 12px rgba(15, 35, 80, 0.05);
}

.sub-card h1 {
  margin: 0 0 8px;
  font-size: 1.125rem;
  color: #2c3e50;
  text-align: center;
}

.sub-card h2 {
  margin: 20px 0 8px;
  font-size: 15px;
  color: #333;
}

.sub-card p {
  margin: 0 0 10px;
  font-size: 14px;
  line-height: 1.75;
  color: #555;
}

.sub-subtitle {
  text-align: center;
  color: #999;
  font-size: 13px;
  margin-bottom: 16px;
}
</style>
