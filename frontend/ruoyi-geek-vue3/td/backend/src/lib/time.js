function pad(value) {
  return String(value).padStart(2, '0')
}

export function formatChinaDateTime(value = new Date()) {
  const date = value instanceof Date ? value : new Date(value)
  const safe = Number.isNaN(date.getTime()) ? new Date() : date
  const formatter = new Intl.DateTimeFormat('zh-CN', {
    timeZone: 'Asia/Shanghai',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  })
  const parts = Object.fromEntries(
    formatter.formatToParts(safe).map((part) => [part.type, part.value]),
  )
  return `${parts.year || safe.getFullYear()}-${parts.month || pad(safe.getMonth() + 1)}-${parts.day || pad(safe.getDate())} ${parts.hour || pad(safe.getHours())}:${parts.minute || pad(safe.getMinutes())}:${parts.second || pad(safe.getSeconds())}`
}

