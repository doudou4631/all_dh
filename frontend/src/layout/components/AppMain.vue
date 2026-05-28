<script setup lang="ts">
import iframeToggle from "./IframeToggle/index.vue"
import copyright from "./Copyright/index.vue"
import useTagsViewStore from '@/store/modules/tagsView'


const tagsViewStore = useTagsViewStore()
</script>

<template>
  <section class="app-main">
    <router-view v-slot="{ Component, route }">
      <transition :name="(!!route.meta && !!route.meta.transition) ? '' + route.meta.transition : 'fade-transform'"
        mode="out-in">
        <keep-alive :include="tagsViewStore.cachedViews">
          <component v-if="!route.meta.link" :is="Component" :key="route.path" />
        </keep-alive>
      </transition>
    </router-view>
    <iframe-toggle />
    <copyright />
  </section>
</template>

<style lang="scss" scoped>
@use "@/assets/styles/variables.module.scss";

.app-main:has(.copyright) {
  padding-bottom: 36px;
}

.app-main {
  min-height: calc(100vh - variables.$navbar-height - 40px); // 减去跑马灯高度40px
  width: 100%;
  position: relative;
  overflow: hidden;
}

.fixed-header+.app-main {
  padding-top: calc(variables.$navbar-height + 40px); // 加上跑马灯高度40px
}

.hasTagsView {
  .app-main {
    min-height: calc(100vh - variables.$navbar-height - variables.$tags-view-height - 40px); // 减去跑马灯高度40px
  }

  .fixed-header+.app-main {
    padding-top: calc(variables.$navbar-height + variables.$tags-view-height + 40px); // 加上跑马灯高度40px
  }
}
</style>

<style lang="scss">
// fix css style bug in open el-dialog
.el-popup-parent--hidden {
  .fixed-header {
    padding-right: 17px;
  }
}
</style>