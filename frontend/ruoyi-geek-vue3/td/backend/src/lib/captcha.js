import { createHash, randomUUID } from 'node:crypto'

const captchaStore = new Map()
const characters = ['1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'd', 'e', 'f', 'h', 'k', 'm', 'n', 'r', 'x', 'y', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'R', 'S', 'T', 'W', 'X', 'Y']
const colors = ['#111111', '#d32f2f', '#1565c0', '#2e7d32', '#ef6c00', '#6d4c41', '#283593']
const width = 80
const height = 30
const fontSize = 16

function md5(text = '') {
  return createHash('md5').update(String(text)).digest('hex').slice(8, 24)
}

function randomItem(list = []) {
  return list[Math.floor(Math.random() * list.length)]
}

function cleanupExpired() {
  const now = Date.now()
  for (const [key, value] of captchaStore.entries()) {
    if (value.expiresAt <= now) {
      captchaStore.delete(key)
    }
  }
}

function buildCode() {
  let code = ''
  for (let index = 0; index < 4; index += 1) {
    code += randomItem(characters)
  }
  return code
}

function buildSvg(code) {
  const lines = Array.from({ length: 3 }).map(() => {
    const x1 = Math.floor(Math.random() * width)
    const y1 = Math.floor(Math.random() * height)
    const x2 = Math.floor(Math.random() * width)
    const y2 = Math.floor(Math.random() * height)
    return `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="${randomItem(colors)}" stroke-width="1" />`
  }).join('')

  const chars = code
    .split('')
    .map((char, index) => `<text x="${index * 18 + 6}" y="20" font-family="Times New Roman" font-size="${fontSize}" fill="${randomItem(colors)}">${char}</text>`)
    .join('')

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">
  <rect width="${width}" height="${height}" fill="#ffffff" rx="4" ry="4" />
  ${lines}
  ${chars}
</svg>`
}

export function createCaptcha() {
  cleanupExpired()
  const code = buildCode()
  const captchaId = `captcha_${randomUUID()}`
  captchaStore.set(captchaId, {
    value: md5(code.toLowerCase()),
    expiresAt: Date.now() + 5 * 60 * 1000,
  })
  const svg = buildSvg(code)
  return {
    captchaId,
    imageData: `data:image/svg+xml;base64,${Buffer.from(svg, 'utf-8').toString('base64')}`,
  }
}

export function verifyCaptcha(captchaId = '', captcha = '') {
  cleanupExpired()
  const record = captchaStore.get(String(captchaId))
  if (!record) {
    return false
  }
  captchaStore.delete(String(captchaId))
  return record.value === md5(String(captcha).trim().toLowerCase())
}
