const SINGLE_QUERY_RESULT_DELAY_KEY = 'number-query-single-result-delay-seconds'

export const DEFAULT_SINGLE_QUERY_RESULT_DELAY_SECONDS = 10
export const MAX_SINGLE_QUERY_RESULT_DELAY_SECONDS = 60

function normalizeDelay(value) {
  const delay = Number(value)
  if (!Number.isFinite(delay)) return DEFAULT_SINGLE_QUERY_RESULT_DELAY_SECONDS
  return Math.min(MAX_SINGLE_QUERY_RESULT_DELAY_SECONDS, Math.max(0, Math.round(delay)))
}

export function getSingleQueryResultDelaySeconds() {
  const storedValue = localStorage.getItem(SINGLE_QUERY_RESULT_DELAY_KEY)
  return storedValue === null ? DEFAULT_SINGLE_QUERY_RESULT_DELAY_SECONDS : normalizeDelay(storedValue)
}

export function setSingleQueryResultDelaySeconds(value) {
  const delay = normalizeDelay(value)
  localStorage.setItem(SINGLE_QUERY_RESULT_DELAY_KEY, String(delay))
  return delay
}
