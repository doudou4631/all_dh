# -*- coding: utf-8 -*-
"""Regenerate tencent.vue with admin dashboard layout and UTF-8 Chinese."""
from pathlib import Path

import fix_tencent_vue_encoding as base

T = base.T

path = Path(r"c:\Users\Administrator\Desktop\1500\frontend\src\views\server\mark\user\tencent.vue")

template = f"""<template>
  <div class="app-container mark-user-order-page">
    <el-card shadow="never" class="platform-card" :body-style="{{ padding: '0' }}">
      <div class="platform-layout platform-layout--single">
        <div class="platform-main">
          <div v-if="activeTab === 'submit'" class="platform-main-remain">
            <div class="submit-right-remain">
              {T['remain_label']}<span>{{{{ remainCount }}}}</span> {T['remain_suffix']}
            </div>
          </div>
          <el-tabs v-model="activeTab" class="sub-tabs">
            <el-tab-pane label="{T['tab_submit']}" name="submit">
              <div class="submit-pane">
                <div class="tencent-submit-card">
                  <h1 class="tencent-submit-card__title">{T['title']}</h1>

                  <div class="tencent-submit-box">
                    <ul class="tencent-steps">
                      <li class="tencent-step">
                        <span class="tencent-step__num">1</span>
                        <span class="tencent-step__text">
                          {T['step1_prefix']}
                          <el-link
                            type="primary"
                            href="https://yun.m.qq.com/person_apply.html"
                            target="_blank"
                          >https://yun.m.qq.com/person_apply.html</el-link>
                          {T['step1_suffix']}
                        </span>
                      </li>
                      <li class="tencent-step">
                        <span class="tencent-step__num">2</span>
                        <span class="tencent-step__text">{T['step2']}</span>
                      </li>
                    </ul>

                    <el-form
                      :model="form"
                      label-width="68px"
                      size="default"
                      class="tencent-submit-form"
                      @keyup.enter="handleSubmit"
                    >
                      <el-form-item label="{T['phone']}">
                        <el-input
                          v-model="form.phone"
                          maxlength="11"
                          clearable
                          placeholder="{T['phone_ph']}"
                          @input="handlePhoneInput"
                        />
                      </el-form-item>
                      <el-form-item label="{T['sms']}">
                        <el-input
                          v-model="form.smsCode"
                          maxlength="6"
                          clearable
                          placeholder="{T['sms_ph']}"
                          @input="handleSmsInput"
                        />
                      </el-form-item>
                      <el-form-item class="tencent-submit-form__actions">
                        <el-button
                          type="primary"
                          class="tencent-submit-form__btn"
                          :loading="submitting"
                          v-hasPermi="['server:markUser:order:add']"
                          @click="handleSubmit"
                        >
                          {T['submit_btn']}
                        </el-button>
                        <el-button
                          class="tencent-submit-form__btn tencent-submit-form__btn--reset"
                          :disabled="submitting"
                          @click="handleResetForm"
                        >
                          {T['btn_reset']}
                        </el-button>
                      </el-form-item>
                    </el-form>
                  </div>

                  <div class="tencent-submit-footer">
                    <span class="tencent-submit-footer__label">{T['result_label']}</span>
                    <span class="tencent-submit-footer__text">{{{{ resultText || '{T['footer_ph']}' }}}}</span>
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="{T['tab_record']}" name="record">
              <div class="record-search-panel">
                <div class="record-search-bar">
                  <div class="record-search-field record-search-field--keyword">
                    <span class="record-search-field__label">{T['search_keyword']}</span>
                    <el-input
                      v-model="queryParams.keyword"
                      size="small"
                      clearable
                      placeholder="{T['search_keyword_ph']}"
                      @keyup.enter="handleQuery"
                    />
                  </div>
                  <div class="record-search-field record-search-field--status">
                    <span class="record-search-field__label">{T['search_status']}</span>
                    <el-select
                      v-model="queryParams.orderStatus"
                      size="small"
                      clearable
                      placeholder="{T['search_status_ph']}"
                    >
                      <el-option label="{T['opt_pending']}" value="0" />
                      <el-option label="{T['opt_processing']}" value="1" />
                      <el-option label="{T['opt_done']}" value="2" />
                      <el-option label="{T['opt_cancelled']}" value="3" />
                    </el-select>
                  </div>
                  <div class="record-search-field record-search-field--date">
                    <span class="record-search-field__label">{T['search_date']}</span>
                    <el-date-picker
                      v-model="recordDateRange"
                      type="daterange"
                      size="small"
                      range-separator="-"
                      start-placeholder="{T['date_start']}"
                      end-placeholder="{T['date_end']}"
                      format="YYYY/MM/DD"
                      value-format="YYYY-MM-DD"
                      @change="handleRecordDateRangeChange"
                    />
                  </div>
                  <div class="record-search-field record-search-field--actions">
                    <div class="record-action-group">
                      <el-button size="small" @click="exportRecordRows">{T['btn_export']}</el-button>
                      <el-button size="small" @click="resetQuery">{T['btn_reset']}</el-button>
                      <el-button type="primary" size="small" icon="Search" @click="handleQuery">{T['btn_search']}</el-button>
                    </div>
                  </div>
                </div>
              </div>

              <div class="record-table-wrap">
                <el-table
                  v-loading="loading"
                  :data="orderList"
                  :row-key="recordRowKey"
                  @selection-change="handleRecordSelectionChange"
                >
                  <el-table-column type="selection" width="48" />
                  <el-table-column label="{T['col_user']}" prop="userName" min-width="110" show-overflow-tooltip />
                  <el-table-column label="{T['col_phone_copy']}" min-width="150" show-overflow-tooltip>
                    <template #default="scope">
                      <el-button link type="primary" @click="copyText(scope.row.phonePreview || '')">
                        {{{{ scope.row.phonePreview || '-' }}}}
                      </el-button>
                    </template>
                  </el-table-column>
                  <el-table-column label="{T['col_platform']}" prop="platformName" min-width="110" show-overflow-tooltip />
                  <el-table-column label="{T['col_status']}" width="92" align="center">
                    <template #default="scope">
                      <el-tag :type="recordStatusType(scope.row)" size="small">
                        {{{{ recordStatusLabel(scope.row) }}}}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="{T['col_order']}" prop="orderNo" min-width="180" show-overflow-tooltip />
                  <el-table-column label="{T['col_submit_time']}" min-width="160" align="center">
                    <template #default="scope">
                      {{{{ formatDateTime(scope.row.createTime) }}}}
                    </template>
                  </el-table-column>
                </el-table>
              </div>

              <pagination
                v-show="total > 0"
                :total="total"
                v-model:page="queryParams.pageNum"
                v-model:limit="queryParams.pageSize"
                @pagination="getList"
              />
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </el-card>
  </div>
</template>
"""

script_part = base.content.rsplit("</template>", 1)[1].split("<style scoped>", 1)[0]

styles = """
<style scoped>
.mark-user-order-page {
  padding: 0 !important;
  margin: 0;
  width: 100%;
}

.mark-user-order-page :deep(.platform-card) {
  border: none;
  border-radius: 0;
  box-shadow: none;
}

.mark-user-order-page :deep(.platform-card > .el-card__body) {
  padding: 0 !important;
}

.mark-user-order-page :deep(.pagination-container) {
  margin-top: 12px;
}

.platform-layout {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.platform-layout--single {
  display: block;
}

.platform-main {
  flex: 1;
  min-width: 0;
  position: relative;
}

.platform-main-remain {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  height: 40px;
  padding-right: 4px;
}

.sub-tabs {
  margin-top: 0;
}

.sub-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0;
}

.sub-tabs :deep(.el-tabs__nav-wrap) {
  padding: 0;
}

.sub-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
}

.sub-tabs :deep(.el-tabs__content) {
  padding: 0;
}

.sub-tabs :deep(.el-tabs__item) {
  height: 40px;
  line-height: 40px;
  padding: 0 16px;
}

.platform-main:has(.platform-main-remain) .sub-tabs :deep(.el-tabs__header) {
  padding-right: 150px;
}

.submit-right-remain {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  border: 1px solid #a0cfff;
  border-radius: 4px;
  background: #ecf5ff;
  color: #303133;
  font-size: 13px;
  line-height: 1;
}

.submit-right-remain span {
  color: #409eff;
  font-weight: 600;
}

.submit-pane {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 0;
  border-top: none;
  padding: 28px 24px 36px;
  background: #f5f5f5;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  box-sizing: border-box;
}

.tencent-submit-card {
  width: min(680px, 68vw);
  min-width: 520px;
  margin: 0 auto;
  padding: 32px 40px 28px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
  box-sizing: border-box;
}

.tencent-submit-card__title {
  margin: 0;
  text-align: center;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.45;
  color: #303133;
}

.tencent-submit-card__subtitle {
  margin: 10px 0 0;
  text-align: center;
  font-size: 17px;
  font-weight: 700;
  line-height: 1.45;
  color: #303133;
}

.tencent-submit-box {
  margin-top: 18px;
  padding: 24px 28px 20px;
  border: 1px solid #dcdfe6;
  border-radius: 10px;
  background: #fff;
  width: 100%;
  box-sizing: border-box;
}

.tencent-steps {
  list-style: none;
  margin: 0 0 18px;
  padding: 0;
}

.tencent-step {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 14px;
  color: #606266;
  font-size: 14px;
  line-height: 1.7;
}

.tencent-step:last-child {
  margin-bottom: 0;
}

.tencent-step__num {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  line-height: 22px;
  text-align: center;
}

.tencent-step__text {
  flex: 1;
  min-width: 0;
  word-break: break-all;
}

.tencent-submit-form {
  margin-top: 8px;
}

.tencent-submit-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.tencent-submit-form :deep(.el-input) {
  max-width: 360px;
}

.tencent-submit-form__actions {
  margin-bottom: 0;
}

.tencent-submit-form__actions :deep(.el-form-item__content) {
  margin-left: 68px !important;
  display: flex;
  align-items: center;
  gap: 10px;
}

.tencent-submit-form__btn {
  min-width: 120px;
  height: 40px;
  padding: 0 28px;
  font-size: 14px;
  font-weight: 600;
}

.tencent-submit-form__btn--reset {
  min-width: 88px;
  font-weight: 500;
}

.tencent-submit-footer {
  margin-top: 18px;
  min-height: 52px;
  padding: 12px 16px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fafafa;
  font-size: 13px;
  line-height: 1.6;
  word-break: break-all;
  box-sizing: border-box;
}

.tencent-submit-footer__label {
  margin-right: 8px;
  color: var(--el-text-color-secondary);
}

.tencent-submit-footer__text {
  color: var(--el-text-color-primary);
}

.tencent-submit-footer__text:empty {
  color: var(--el-text-color-secondary);
}

.record-search-panel {
  margin-top: 0;
  margin-bottom: 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 0;
  border-top: none;
  background: #fff;
  padding: 8px 10px;
  overflow-x: auto;
  overflow-y: visible;
}

.record-search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
  width: max-content;
  min-width: 100%;
}

.record-search-field {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex: 0 0 auto;
}

.record-search-field--keyword {
  flex: 0 1 200px;
  width: 200px;
  min-width: 140px;
}

.record-search-field--keyword :deep(.el-input) {
  width: 100%;
}

.record-search-field--status :deep(.el-select) {
  width: 96px;
}

.record-search-field--date :deep(.el-date-editor) {
  width: 210px !important;
  flex-shrink: 0;
}

.record-search-field--date :deep(.el-range-input) {
  font-size: 12px;
}

.record-search-field--actions {
  margin-left: auto;
  flex-shrink: 0;
}

.record-search-field__label {
  flex-shrink: 0;
  white-space: nowrap;
  font-size: 12px;
  color: var(--el-text-color-regular);
  line-height: 1;
}

.record-action-group {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: nowrap;
}

.record-action-group :deep(.el-button + .el-button) {
  margin-left: 0;
}

.record-table-wrap {
  width: 100%;
  overflow-x: auto;
}

.record-table-wrap :deep(.el-table) {
  min-width: 860px;
}

@media (max-width: 768px) {
  .platform-main:has(.platform-main-remain) .sub-tabs :deep(.el-tabs__header) {
    padding-right: 0;
  }

  .platform-main-remain {
    position: static;
    justify-content: flex-end;
    height: auto;
    padding: 8px 12px 0;
  }

  .submit-pane {
    padding: 16px 12px 24px;
  }

  .tencent-submit-card {
    width: 100%;
    min-width: 0;
    padding: 24px 16px 20px;
  }

  .tencent-submit-box {
    padding: 18px 14px 16px;
  }

  .tencent-submit-form :deep(.el-input) {
    max-width: none;
  }

  .tencent-submit-form__actions :deep(.el-form-item__content) {
    margin-left: 0 !important;
  }

  .tencent-submit-card__title {
    font-size: 20px;
  }

  .tencent-submit-card__subtitle {
    font-size: 16px;
  }
}
</style>
"""

path.write_text(template + script_part + styles, encoding="utf-8")
print(f"fixed {path}")
