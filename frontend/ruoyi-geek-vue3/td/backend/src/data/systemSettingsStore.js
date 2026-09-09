import { access, readFile, writeFile } from 'node:fs/promises'
import { resolve } from 'node:path'

const jsonFallbackPath = resolve(process.cwd(), 'data', 'system-settings.json')

const liveConfigCandidates = [
  process.env.RIGHTCONTROL_SYSTEM_CONFIG_PATH,
  'C:\\sites\\RightControl\\site\\Configs\\system.config',
  'E:\\nixiang\\target-175178222176\\server_code\\site\\Configs\\system.config',
].filter(Boolean)

const baseDefinitions = [
  { key: 'siteName', label: '网站名称', configKey: 'SiteName', fallback: '业务系统' },
  { key: 'siteDomain', label: '网站域名', configKey: 'SiteDomain', fallback: '' },
  { key: 'cacheTime', label: '缓存时间（秒）', configKey: 'CacheTime', fallback: '0' },
  { key: 'maxUploadKb', label: '上传大小上限（KB）', configKey: 'MaxFileUpload', fallback: '2048' },
  { key: 'uploadAllowlist', label: '上传白名单', configKey: 'UploadFileType', fallback: 'png|gif|jpg|jpeg|zip|rar' },
  { key: 'homeTitle', label: '首页标题', configKey: 'HomeTitle', fallback: '' },
  { key: 'metaKey', label: 'META关键词', configKey: 'MetaKey', fallback: '' },
  { key: 'metaDescribe', label: 'META描述', configKey: 'MetaDescribe', fallback: '' },
  { key: 'copyRight', label: '版权信息', configKey: 'CopyRight', fallback: '' },
  { key: 'supportContact', label: '运维联系人', configKey: 'Contact', fallback: '' },
]

const switchDefinitions = [
  {
    key: 'robotNotify',
    label: '机器人通知',
    description: '对应旧版系统配置里的 weixinjiqiren 开关。',
    configKey: 'weixinjiqiren',
    read: (value) => String(value || '0').trim() === '1',
    write: (enabled) => (enabled ? '1' : '0'),
  },
  {
    key: 'redisCache',
    label: 'Redis 缓存',
    description: '根据旧版 RedisConnectionString 是否已配置进行显示。',
    configKey: 'RedisConnectionString',
    read: (value) => Boolean(String(value || '').trim()),
    write: null,
  },
]

function escapeRegExp(value = '') {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function encodeXml(value = '') {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

function decodeXml(value = '') {
  return String(value)
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&amp;/g, '&')
}

async function exists(filePath) {
  try {
    await access(filePath)
    return true
  } catch {
    return false
  }
}

async function resolveLiveConfigPath() {
  for (const candidate of liveConfigCandidates) {
    if (await exists(candidate)) {
      return candidate
    }
  }
  return ''
}

function parseAppSettings(xml = '') {
  const regex = /<add\s+key=(["'])(.*?)\1\s+value=(["'])(.*?)\3\s*\/>/gims
  const entries = new Map()
  let match
  while ((match = regex.exec(xml)) !== null) {
    entries.set(match[2], decodeXml(match[4]))
  }
  return entries
}

function setAppSetting(xml = '', key = '', value = '') {
  const encoded = encodeXml(value)
  const regex = new RegExp(`(<add\\s+key=(["'])${escapeRegExp(key)}\\2\\s+value=(["']))(.*?)(\\3\\s*/>)`, 'is')
  if (regex.test(xml)) {
    return xml.replace(regex, `$1${encoded}$4`)
  }
  return xml.replace(/<\/appSettings>/i, `  <add key="${key}" value="${encoded}" />\r\n</appSettings>`)
}

async function readFallbackSettings() {
  try {
    const raw = await readFile(jsonFallbackPath, 'utf-8')
    const parsed = JSON.parse(raw)
    return {
      base: Array.isArray(parsed?.base) ? parsed.base : [],
      switches: Array.isArray(parsed?.switches) ? parsed.switches : [],
    }
  } catch {
    return { base: [], switches: [] }
  }
}

async function writeFallbackSettings(payload) {
  const next = {
    base: Array.isArray(payload?.base) ? payload.base : [],
    switches: Array.isArray(payload?.switches) ? payload.switches : [],
  }
  await writeFile(jsonFallbackPath, JSON.stringify(next, null, 2), 'utf-8')
  return next
}

async function readLiveConfig() {
  const filePath = await resolveLiveConfigPath()
  if (!filePath) {
    return { filePath: '', xml: '', entries: new Map() }
  }
  const xml = await readFile(filePath, 'utf-8')
  return { filePath, xml, entries: parseAppSettings(xml) }
}

function buildLiveSettings(entries = new Map()) {
  const base = baseDefinitions.map((definition) => ({
    key: definition.key,
    label: definition.label,
    value: String(entries.get(definition.configKey) ?? definition.fallback),
  }))

  const switches = switchDefinitions
    .filter((definition) => entries.has(definition.configKey))
    .map((definition) => ({
      key: definition.key,
      label: definition.label,
      description: definition.description,
      enabled: definition.read(entries.get(definition.configKey)),
    }))

  return { base, switches }
}

async function writeLiveConfig(payload) {
  const live = await readLiveConfig()
  if (!live.filePath) {
    return writeFallbackSettings(payload)
  }

  let xml = live.xml
  const incomingBase = Array.isArray(payload?.base) ? payload.base : []
  const incomingSwitches = Array.isArray(payload?.switches) ? payload.switches : []

  for (const definition of baseDefinitions) {
    const field = incomingBase.find((item) => item.key === definition.key)
    if (field) {
      xml = setAppSetting(xml, definition.configKey, field.value ?? '')
    }
  }

  for (const definition of switchDefinitions) {
    if (!definition.write) continue
    const field = incomingSwitches.find((item) => item.key === definition.key)
    if (field) {
      xml = setAppSetting(xml, definition.configKey, definition.write(Boolean(field.enabled)))
    }
  }

  await writeFile(live.filePath, xml, 'utf-8')
  const refreshed = parseAppSettings(xml)
  return buildLiveSettings(refreshed)
}

export async function readSystemSettings() {
  const live = await readLiveConfig()
  if (live.filePath) {
    return buildLiveSettings(live.entries)
  }
  return readFallbackSettings()
}

export async function writeSystemSettings(payload) {
  const live = await readLiveConfig()
  if (live.filePath) {
    return writeLiveConfig(payload)
  }
  return writeFallbackSettings(payload)
}

export async function resetSystemSettings() {
  const live = await readLiveConfig()
  if (live.filePath) {
    return buildLiveSettings(live.entries)
  }
  return readFallbackSettings()
}

export async function isSystemSwitchEnabled(key, fallback = false) {
  const settings = await readSystemSettings()
  const target = (settings.switches || []).find((item) => item.key === key)
  return target ? Boolean(target.enabled) : fallback
}

export async function getSystemSettingValue(key, fallback = '') {
  const live = await readLiveConfig()
  if (live.filePath) {
    const entries = live.entries
    const liveMappings = {
      siteTitle: entries.get('SiteName') ?? fallback,
      siteName: entries.get('SiteName') ?? fallback,
      siteDomain: entries.get('SiteDomain') ?? fallback,
      cacheTime: entries.get('CacheTime') ?? fallback,
      maxUploadKb: entries.get('MaxFileUpload') ?? fallback,
      uploadAllowlist: entries.get('UploadFileType') ?? fallback,
      homeTitle: entries.get('HomeTitle') ?? fallback,
      metaKey: entries.get('MetaKey') ?? fallback,
      metaDescribe: entries.get('MetaDescribe') ?? fallback,
      copyRight: entries.get('CopyRight') ?? fallback,
      supportContact: entries.get('Contact') ?? fallback,
      apixiaomi: entries.get('apixiaomi') ?? fallback,
      loginTitle: '登录',
      loginNotice: '',
    }
    if (Object.prototype.hasOwnProperty.call(liveMappings, key)) {
      return String(liveMappings[key] ?? '')
    }
  }

  const settings = await readFallbackSettings()
  const target = (settings.base || []).find((item) => item.key === key)
  return target ? String(target.value ?? '') : fallback
}
