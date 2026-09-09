import request from '@/utils/request'

export function listMarkFundTotal(query) {
  return request({ url: '/server/mark/fund/total/list', method: 'get', params: query })
}

export function getMarkFundTotalSummary(query) {
  return request({ url: '/server/mark/fund/total/summary', method: 'get', params: query })
}

export function listMarkFundPlatformOptions() {
  return request({ url: '/server/mark/fund/total/platform/options', method: 'get' })
}
