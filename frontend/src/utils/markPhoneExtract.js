/**
 * Extract phone numbers from mixed text while preserving document order (no sort).
 */

function normalizePhone(raw) {
  let digits = String(raw || '').replace(/\D/g, '')
  if (digits.length === 13 && digits.startsWith('86')) {
    digits = digits.slice(2)
  }
  if (digits.length >= 7 && digits.length <= 15) {
    return digits
  }
  return ''
}

function pushUniquePhone(target, seen, raw) {
  const phone = normalizePhone(raw)
  if (!phone || seen.has(phone)) return
  seen.add(phone)
  target.push(phone)
}

function collectPhonesFromSegment(segment) {
  const trimmed = String(segment || '').trim()
  if (!trimmed) return []

  const ordered = []
  const seen = new Set()

  const digitsOnly = trimmed.replace(/\D/g, '')
  if (digitsOnly.length >= 7 && digitsOnly.length <= 15) {
    pushUniquePhone(ordered, seen, digitsOnly)
    return ordered
  }

  const mobilePattern = /(?:\+?86[-\s]?)?(?:13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])[-\s]?\d{4}[-\s]?\d{4}|1[3-9]\d{9}/gi
  let match
  while ((match = mobilePattern.exec(trimmed)) !== null) {
    pushUniquePhone(ordered, seen, match[0])
  }

  const landlinePattern = /(?:\+?86[-\s]?)?(?:\(0\d{2,3}\)|0\d{2,3})[-\s.]?\d{7,8}|0\d{2,3}[-\s.]?\d{7,8}/gi
  while ((match = landlinePattern.exec(trimmed)) !== null) {
    pushUniquePhone(ordered, seen, match[0])
  }

  const specialPattern = /[48]00[-\s.]?\d{7}/gi
  while ((match = specialPattern.exec(trimmed)) !== null) {
    pushUniquePhone(ordered, seen, match[0])
  }

  const digitRunPattern = /\d{7,15}/g
  while ((match = digitRunPattern.exec(trimmed)) !== null) {
    pushUniquePhone(ordered, seen, match[0])
  }

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
export function extractPhoneNumbersPreservingOrder(text) {
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
      allPhones.push(...collectPhonesFromSegment(segment))
    })
  })

  return dedupePhonesPreservingOrder(allPhones)
}

export function formatExtractedPhones(phones) {
  return (phones || []).join('\n')
}
