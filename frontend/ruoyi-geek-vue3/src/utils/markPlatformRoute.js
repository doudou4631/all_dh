const ROUTE_TOKEN_PLATFORM_CODES = Object.freeze({
  taidixiong: 'taidixiong',
  markusertaidixiong: 'taidixiong',
  tdgaopin: 'td_gaopin',
  markusertdgaopin: 'td_gaopin',
  tdsecond: 'td_second',
  tdsecondmark: 'td_second',
  markusertdsecond: 'td_second',
  tengxun: 'tengxun',
  tencentmark: 'tengxun',
  markusertengxun: 'tengxun',
  '360fugai': '360fugai',
  sanliulingmark: '360fugai',
  markuser360: '360fugai',
  qihusecond: 'qihu_second',
  qihusecondmark: 'qihu_second',
  markuserqihusecond: 'qihu_second',
  baidu: 'baidu',
  markuserbaidu: 'baidu',
  sogou: 'sogou',
  markusersogou: 'sogou',
  yidonggaopin: 'yidonggaopin',
  markuseryidong: 'yidonggaopin',
  dianhuabang: 'dianhuabang',
  markuserdianhuabang: 'dianhuabang',
  ltgj: 'ltgj',
  markuserltgj: 'ltgj',
  xiaomi: 'xiaomi',
  xiaomimark: 'xiaomi',
  markuserxiaomi: 'xiaomi',
  agenttaidixiong: 'taidixiong',
  agenttdgaopin: 'td_gaopin',
  agenttdsecond: 'td_second',
  agenttengxun: 'tengxun',
  agentqihusecond: 'qihu_second',
  agentbaidu: 'baidu',
  agentsogou: 'sogou',
  agentyidong: 'yidonggaopin',
  agentdianhuabang: 'dianhuabang',
  agentltgj: 'ltgj',
  agentxiaomi: 'xiaomi'
})

const PLATFORM_NAME_CODES = Object.freeze({
  '泰迪熊': 'taidixiong',
  'TdGaopin高频': 'td_gaopin',
  'Taidixiong二次': 'td_second',
  '腾讯': 'tengxun',
  '360': '360fugai',
  '360二次': 'qihu_second',
  '百度': 'baidu',
  '搜狗': 'sogou',
  '移动高频': 'yidonggaopin',
  '电话邦': 'dianhuabang',
  '联通管家': 'ltgj',
  '小米手机': 'xiaomi'
})

function normalizeCode(code) {
  const normalized = String(code || '').trim().toLowerCase()
  return normalized === '360' ? '360fugai' : normalized
}

function readQueryPlatformCode(route) {
  const query = route?.query
  if (typeof query === 'string') {
    try {
      return normalizeCode(JSON.parse(query)?.platformCode)
    } catch (error) {
      return ''
    }
  }
  return normalizeCode(query?.platformCode)
}

export function resolveMarkPlatformCodeFromRoute(route, allowedCodes = [], fallback = '') {
  const allowed = new Set((allowedCodes || []).map(normalizeCode).filter(Boolean))
  const accept = (code) => {
    const normalized = normalizeCode(code)
    return normalized && (!allowed.size || allowed.has(normalized)) ? normalized : ''
  }

  const pathSegments = String(route?.path || '').split('/').filter(Boolean).reverse()
  const routeTokens = [...pathSegments, route?.name]
  for (const token of routeTokens) {
    const mapped = ROUTE_TOKEN_PLATFORM_CODES[String(token || '').trim().toLowerCase()]
    const code = accept(mapped)
    if (code) return code
  }

  const queryCode = accept(readQueryPlatformCode(route))
  if (queryCode) return queryCode

  const titleCode = accept(PLATFORM_NAME_CODES[String(route?.meta?.title || '').trim()])
  return titleCode || accept(fallback)
}
