export const ORDER_FAIL_REASON_DESC_MAX_LENGTH = 255

export function normalizeFailReasonDesc(value, maxLength = ORDER_FAIL_REASON_DESC_MAX_LENGTH) {
  if (value === null || value === undefined) return null
  const safeMaxLength = Number.isFinite(Number(maxLength)) && Number(maxLength) > 0
    ? Math.floor(Number(maxLength))
    : ORDER_FAIL_REASON_DESC_MAX_LENGTH
  return String(value).trim().slice(0, safeMaxLength)
}
