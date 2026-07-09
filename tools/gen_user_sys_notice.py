# -*- coding: utf-8 -*-
"""Regenerate user system notice page with UTF-8 Chinese."""
from pathlib import Path


def zh(*codes: int) -> str:
    return "".join(chr(c) for c in codes)


T = {
    "page_title": zh(0x7CFB, 0x7EDF, 0x516C, 0x544A),
    "type_notice": zh(0x901A, 0x77E5),
    "type_announce": zh(0x516C, 0x544A),
    "empty": zh(0x6682, 0x65E0, 0x516C, 0x544A),
    "load_fail": zh(0x83B7, 0x53D6, 0x516C, 0x544A, 0x5217, 0x8868, 0x5931, 0x8D25),
}

content = f"""<template>
  <div class="app-container user-sys-notice-page">
    <el-card shadow="never" v-loading="loading">
      <template #header>
        <span class="page-title">{T["page_title"]}</span>
      </template>

      <div v-if="noticeList.length" class="message-list">
        <div
          v-for="item in noticeList"
          :key="item.noticeId"
          class="message-item"
          @click="showNoticeDetail(item)"
        >
          <el-tag size="small" :type="item.noticeType === '1' ? 'danger' : 'success'">
            {{{{ item.noticeType === '1' ? '{T["type_notice"]}' : '{T["type_announce"]}' }}}}
          </el-tag>
          <div class="message-item__main">
            <div class="message-item__title">{{{{ item.noticeTitle }}}}</div>
          </div>
          <span class="message-item__time">{{{{ formatTime(item.createTime) }}}}</span>
        </div>
      </div>
      <el-empty v-else description="{T["empty"]}" :image-size="64" />

      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="currentNotice.noticeTitle" width="50%" destroy-on-close>
      <div class="notice-dialog-content" v-html="currentNotice.noticeContent"></div>
    </el-dialog>
  </div>
</template>

<script setup name="UserSysNotice" lang="ts">
import {{ ref, reactive, onMounted }} from 'vue'
import {{ listNotice }} from '@/api/system/notice'
import {{ parseTime }} from '@/utils/ruoyi'
import {{ GeekResponseForList }} from '@/types/request'

interface Notice {{
  noticeId: number
  noticeTitle: string
  noticeType: string
  noticeContent: string
  status: string
  createBy: string
  createTime: string
}}

const loading = ref(false)
const total = ref(0)
const noticeList = ref<Notice[]>([])
const dialogVisible = ref(false)
const currentNotice = ref<Notice>({{}} as Notice)
const queryParams = reactive({{
  pageNum: 1,
  pageSize: 10
}})

function formatTime(value?: string) {{
  if (!value) return '-'
  return (parseTime(value) as string) || '-'
}}

async function getList() {{
  loading.value = true
  try {{
    const res: GeekResponseForList<Notice> = await listNotice({{ ...queryParams }})
    noticeList.value = res.rows || []
    total.value = res.total || 0
  }} catch (error) {{
    console.error('{T["load_fail"]}:', error)
    noticeList.value = []
    total.value = 0
  }} finally {{
    loading.value = false
  }}
}}

function showNoticeDetail(notice: Notice) {{
  currentNotice.value = notice
  dialogVisible.value = true
}}

onMounted(() => {{
  getList()
}})
</script>

<style scoped lang="scss">
.user-sys-notice-page {{
  .page-title {{
    font-size: 15px;
    font-weight: 600;
  }}

  .message-list {{
    display: flex;
    flex-direction: column;
  }}

  .message-item {{
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 0;
    border-bottom: 1px solid var(--el-border-color-lighter);
    cursor: pointer;

    &:last-child {{
      border-bottom: none;
    }}

    &:hover {{
      opacity: 0.88;
    }}
  }}

  .message-item__main {{
    flex: 1;
    min-width: 0;
  }}

  .message-item__title {{
    font-size: 14px;
    color: var(--el-text-color-primary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }}

  .message-item__time {{
    flex-shrink: 0;
    font-size: 12px;
    color: var(--el-text-color-placeholder);
  }}

  .notice-dialog-content {{
    line-height: 1.7;
    word-break: break-word;
  }}
}}
</style>
"""

target = Path(__file__).resolve().parents[1] / "frontend/src/views/server/user/notice.vue"
target.write_text(content, encoding="utf-8")
print(f"Wrote {target}")
