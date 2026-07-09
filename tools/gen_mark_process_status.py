# -*- coding: utf-8 -*-
from pathlib import Path


def zh(*codes: int) -> str:
    return "".join(chr(c) for c in codes)


T = {
    "pending": zh(0x5F85, 0x5904, 0x7406),
    "processing": zh(0x5904, 0x7406, 0x4E2D),
    "auto": zh(0x540E, 0x53F0, 0x5904, 0x7406, 0x4E2D),
    "success": zh(0x5904, 0x7406, 0x6210, 0x529F),
    "fail": zh(0x5904, 0x7406, 0x5931, 0x8D25),
}

content = f"""export const AUTO_PROCESSING_PLATFORMS = ['tencent_mark', 'tencent', 'tx', 'txwz', 'td_gaopin']

export const XIAOMI_PLATFORM_CODE = 'xiaomi'

export const MARK_ITEM_PROCESS_STATUS_OPTIONS = [
  {{ label: '{T["pending"]}', value: 'pending' }},
  {{ label: '{T["processing"]}', value: '3' }},
  {{ label: '{T["auto"]}', value: 'processing' }},
  {{ label: '{T["success"]}', value: '1' }},
  {{ label: '{T["fail"]}', value: '2' }}
]

export const MARK_ITEM_FEEDBACK_OPTIONS = [
  {{ label: '{T["success"]}', value: '1' }},
  {{ label: '{T["fail"]}', value: '2' }}
]

export function isAutoProcessingPlatform(row) {{
  const code = String(row?.platformCode || '').trim().toLowerCase()
  return AUTO_PROCESSING_PLATFORMS.includes(code)
}}

export function isXiaomiPlatform(row) {{
  const code = String(row?.platformCode || '').trim().toLowerCase()
  return code === XIAOMI_PLATFORM_CODE
}}

export function markItemProcessStatusLabel(status, row) {{
  const code = String(status ?? '')
  if (code === '3') return '{T["processing"]}'
  if (code === '0' && row && isXiaomiPlatform(row)) return '{T["pending"]}'
  if (code === '0' && row && isAutoProcessingPlatform(row)) return '{T["auto"]}'
  const map = {{ '0': '{T["pending"]}', '1': '{T["success"]}', '2': '{T["fail"]}', '3': '{T["processing"]}' }}
  return map[code] || '-'
}}

export function markItemProcessStatusTagType(status, row) {{
  const code = String(status ?? '')
  if (code === '1') return 'success'
  if (code === '2') return 'danger'
  if (code === '3') return 'warning'
  if (code === '0' && row && isAutoProcessingPlatform(row)) return 'warning'
  return 'info'
}}

export function buildMarkItemProcessStatusQuery(rawQuery = {{}}) {{
  const params = {{
    pageNum: rawQuery.pageNum,
    pageSize: rawQuery.pageSize,
    phone: rawQuery.phone,
    platformCode: rawQuery.platformCode,
    platformCodes: rawQuery.platformCodes,
    processStatus: null,
    params: {{ ...(rawQuery.params || {{}}) }}
  }}
  const statusFilter = String(rawQuery.processStatus ?? '').trim()
  if (statusFilter === 'processing') {{
    params.params.processingOnly = '1'
  }} else if (statusFilter === 'pending') {{
    params.processStatus = '0'
    params.params.pendingOnly = '1'
  }} else if (statusFilter === '1' || statusFilter === '2' || statusFilter === '3') {{
    params.processStatus = statusFilter
  }} else if (statusFilter === '0') {{
    params.processStatus = '0'
  }}
  return params
}}

export function canBatchProcessXiaomi(row) {{
  return isXiaomiPlatform(row) && String(row?.processStatus || '0') === '0'
}}

export function canBatchDetectXiaomi(row) {{
  return isXiaomiPlatform(row) && String(row?.processStatus || '0') === '3'
}}

export function canBatchSuccessXiaomi(row) {{
  const status = String(row?.processStatus || '0')
  return isXiaomiPlatform(row) && (status === '0' || status === '2' || status === '3')
}}

export function canSelectXiaomiBatchRow(row) {{
  return canBatchProcessXiaomi(row) || canBatchDetectXiaomi(row) || canBatchSuccessXiaomi(row)
}}

/** @deprecated use canBatchProcessXiaomi */
export function canBatchMarkXiaomiSubmitted(row) {{
  return canBatchProcessXiaomi(row)
}}
"""

target = Path(__file__).resolve().parents[1] / "frontend" / "src" / "utils" / "markProcessStatus.js"
target.write_text(content, encoding="utf-8")
print("wrote", target)
