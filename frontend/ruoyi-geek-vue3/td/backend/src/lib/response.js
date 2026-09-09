export function ok(data, message = 'ok') {
  return { success: true, message, data }
}

export function fail(message = 'error', data = null) {
  return { success: false, message, data }
}
