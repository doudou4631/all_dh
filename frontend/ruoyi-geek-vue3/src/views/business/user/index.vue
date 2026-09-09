<template>
  <div class="www-user-page">
    <aside class="platform-sidebar">
      <button
        v-for="platform in platforms"
        :key="platform.code"
        class="platform-item"
        :class="{ active: activeCode === platform.code }"
        type="button"
        @click="activeCode = platform.code"
      >
        {{ platform.label }}
      </button>
    </aside>

    <main class="platform-content">
      <el-card shadow="never" class="platform-card">
        <template #header>
          <div class="card-title">{{ activePlatform.label }}</div>
        </template>
        <el-empty description="暂无业务内容" />
      </el-card>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

defineOptions({ name: 'BusinessUser' })

const platforms = [
  { code: 'tdgaopin', label: 'TdGaopin高频' },
  { code: 'tengxun', label: 'Tengxun腾讯' },
  { code: 'taidixiong', label: 'Taidixiong二次' },
  { code: '360fugai', label: '360Fugai覆盖' },
  { code: 'xiaomi', label: 'Xiaomi小米' },
  { code: 'baidu', label: 'Baidu百度' },
  { code: 'yidonggaopin', label: 'YidongGaopin移动' },
  { code: 'sougou', label: 'Sougou' },
  { code: 'dianhuabang', label: 'Dianhuabang' },
  { code: 'liantongguanjia', label: 'LiantongGuanjia' }
]

const activeCode = ref(platforms[0].code)
const activePlatform = computed(() => platforms.find(item => item.code === activeCode.value) || platforms[0])
</script>

<style scoped lang="scss">
.www-user-page {
  display: flex;
  min-height: calc(100vh - 112px);
  margin: -20px;
  background: #fff;
}

.platform-sidebar {
  width: 205px;
  flex: 0 0 205px;
  padding: 8px 0;
  border-right: 1px solid #ebeef5;
  background: #fff;
}

.platform-item {
  display: block;
  width: 100%;
  min-height: 54px;
  padding: 0 16px;
  border: 0;
  background: transparent;
  color: #303133;
  font-size: 16px;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.2s, color 0.2s;
}

.platform-item:hover,
.platform-item.active {
  background: #f2f3f5;
  color: #409eff;
}

.platform-content {
  flex: 1;
  min-width: 0;
  padding: 20px;
  background: #f5f7fa;
}

.platform-card {
  min-height: 320px;
}

.card-title {
  color: #303133;
  font-size: 18px;
}
</style>
