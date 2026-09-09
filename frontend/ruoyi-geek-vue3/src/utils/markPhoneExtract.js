/**
 * Extract phone numbers from mixed text while preserving document order (no sort).
 */

const MOBILE_ONLY_PLATFORM_CODES = new Set(['td_gaopin', 'td_second', 'tengxun', '360fugai'])

export function isLandlineAllowedForPlatform(platformCode) {
  return !MOBILE_ONLY_PLATFORM_CODES.has(String(platformCode || '').trim().toLowerCase())
}

function normalizePhone(raw, allowLandline = true) {
  let digits = String(raw || '').replace(/\D/g, '')
  if (digits.startsWith('86') && digits.length === 13) digits = digits.slice(2)
  if (/^1[3-9]\d{9}$/.test(digits)) return digits
  if (allowLandline && digits.length >= 7 && digits.length <= 15) return digits
  return ''
}

function pushUniquePhone(target, seen, raw, allowLandline = true) {
  const phone = normalizePhone(raw, allowLandline)
  if (!phone || seen.has(phone)) return
  seen.add(phone)
  target.push(phone)
}

function collectPhonesFromSegment(segment, allowLandline = true) {
  const trimmed = String(segment || '').trim()
  if (!trimmed) return []

  const ordered = []
  const seen = new Set()

  const mobilePattern = /(?:\+?86[\s-]*)?1[3-9](?:[\s-]?\d){9}/g
  let match
  while ((match = mobilePattern.exec(trimmed)) !== null) {
    const before = trimmed[match.index - 1]
    const after = trimmed[match.index + match[0].length]
    if (/\d/.test(before || '') || /\d/.test(after || '')) continue
    pushUniquePhone(ordered, seen, match[0], false)
  }

  if (ordered.length > 0 || !allowLandline) return ordered

  pushUniquePhone(ordered, seen, trimmed, true)

  return ordered
}

function dedupePhonesPreservingOrder(phones) {
  const result = []
  const seen = new Set()
  ;(phones || []).forEach((phone) => {
    if (!phone || seen.has(phone)) return
    seen.add(phone)
    result.push(phone)
  })
  return result
}

/**
 * Walk lines top-to-bottom, segments left-to-right; keep first-seen order, no sorting.
 */
export function extractPhoneNumbersPreservingOrder(text, allowLandline = true) {
  const source = String(text || '')
  if (!source.trim()) return []

  const allPhones = []
  source.split(/\r?\n/).forEach((line) => {
    const segments = line
      .split(/[,，;；]+/)
      .map((item) => item.trim())
      .filter((item) => item.length > 0)

    if (!segments.length) return

    segments.forEach((segment) => {
      allPhones.push(...collectPhonesFromSegment(segment, allowLandline))
    })
  })

  return dedupePhonesPreservingOrder(allPhones)
}

export function formatExtractedPhones(phones) {
  return (phones || []).join('\n')
}
