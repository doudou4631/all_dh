<script setup>
defineProps({
  columns: { type: Array, default: () => [] },
  rows: { type: Array, default: () => [] },
  rowKey: { type: String, default: 'id' },
  emptyText: { type: String, default: 'No records available.' },
  border: { type: Boolean, default: true },
  stripe: { type: Boolean, default: true },
  size: { type: String, default: 'small' },
  maxHeight: { type: [String, Number], default: undefined },
  tableClass: { type: String, default: '' },
})
</script>

<template>
  <div class="table-wrap element-table-wrap" :class="tableClass">
    <el-table
      class="data-table element-data-table"
      :data="rows"
      :row-key="rowKey"
      :border="border"
      :stripe="stripe"
      :size="size"
      :max-height="maxHeight"
      :empty-text="emptyText"
      table-layout="auto"
    >
      <el-table-column
        v-for="column in columns"
        :key="column.key"
        :prop="column.key"
        :label="column.label"
        :width="column.width"
        :min-width="column.minWidth || column.width || 120"
        :fixed="column.fixed"
        :align="column.align || 'left'"
        :show-overflow-tooltip="column.showOverflowTooltip !== false"
      >
        <template #default="{ row }">
          <slot :name="`cell-${column.key}`" :row="row" :value="row[column.key]" :column="column">
            {{ row[column.key] }}
          </slot>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
