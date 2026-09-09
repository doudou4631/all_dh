import { readFile, writeFile } from 'node:fs/promises'
import { resolve } from 'node:path'

const openidPath = resolve(process.cwd(), 'data', 'openids.json')
const configPath = resolve(process.cwd(), 'data', 'config-entries.json')

const fallbackOpenids = [
  {
    id: 'openid-001',
    username: 'admin',
    openid: 'oq_demo_openid_001',
    gongzhonghao: 'appid#中国移动高频骚扰电话防护',
    okflag: 1,
    note: '系统内置测试账号',
    jisuantime: '周一',
    createdAt: '2026-06-01 09:00:00',
  },
  {
    id: 'openid-002',
    username: 'operator',
    openid: 'oq_demo_openid_002',
    gongzhonghao: 'appid#腾讯手机管家标记',
    okflag: 0,
    note: '待测试',
    jisuantime: '周三',
    createdAt: '2026-06-01 09:20:00',
  },
]

const fallbackConfigs = [
  {
    id: 'config-001',
    codekey: 'tdx_sms_login_url',
    codevalue: 'https://customer-apply-diversion.teddymobile.cn/api/v1/as/auth/login-sms',
    qiyong: 1,
    remark: '泰迪熊短信登录接口',
    createtime: '2026-06-01 08:50:00',
  },
  {
    id: 'config-002',
    codekey: 'qmf_code_url',
    codevalue: 'http://115.238.185.26:8881/code',
    qiyong: 1,
    remark: 'QMF 取码接口',
    createtime: '2026-06-01 08:55:00',
  },
  {
    id: 'config-003',
    codekey: 'proxy_source_url',
    codevalue: 'http://api3.ydaili.cn/tools/BMeasureApi.ashx',
    qiyong: 0,
    remark: '代理来源',
    createtime: '2026-06-01 08:58:00',
  },
]

async function readJson(path, fallback) {
  try {
    const raw = await readFile(path, 'utf-8')
    return JSON.parse(raw)
  } catch {
    return structuredClone(fallback)
  }
}

async function writeJson(path, data) {
  await writeFile(path, JSON.stringify(data, null, 2), 'utf-8')
  return data
}

function nowText() {
  const d = new Date()
  const pad = (v) => String(v).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

export async function listOpenids() {
  return readJson(openidPath, fallbackOpenids)
}

export async function createOpenid(payload) {
  const items = await listOpenids()
  const record = {
    id: `openid-${Date.now()}`,
    username: payload.username || '当前账号',
    openid: payload.openid || '',
    gongzhonghao: payload.gongzhonghao || '',
    okflag: Number(payload.okflag ?? 0),
    note: payload.note || '',
    jisuantime: payload.jisuantime || '',
    createdAt: nowText(),
  }
  items.unshift(record)
  await writeJson(openidPath, items)
  return record
}

export async function listConfigEntries() {
  return readJson(configPath, fallbackConfigs)
}

export async function createConfigEntry(payload) {
  const items = await listConfigEntries()
  const record = {
    id: `config-${Date.now()}`,
    codekey: payload.codekey || '',
    codevalue: payload.codevalue || '',
    qiyong: Number(payload.qiyong ?? 0),
    remark: payload.remark || '',
    createtime: nowText(),
  }
  items.unshift(record)
  await writeJson(configPath, items)
  return record
}

export async function updateConfigEntry(id, payload) {
  const items = await listConfigEntries()
  const index = items.findIndex((item) => item.id === id)
  if (index < 0) return null
  items[index] = {
    ...items[index],
    codekey: payload.codekey ?? items[index].codekey,
    codevalue: payload.codevalue ?? items[index].codevalue,
    qiyong: payload.qiyong === undefined ? items[index].qiyong : Number(payload.qiyong),
    remark: payload.remark ?? items[index].remark,
  }
  await writeJson(configPath, items)
  return items[index]
}
