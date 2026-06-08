import { reactive } from 'vue'

const state = reactive({
  wechatModalVisible: false,
  toastVisible: false,
  toastMessage: '',
  confirmVisible: false,
  confirmMessage: '请确认操作'
})

let toastTimer = null
let confirmOnConfirm = null
let confirmOnCancel = null

export function useBottomNavUiState() {
  return state
}

export function notify(message) {
  const text = String(message || '').trim()
  if (!text) return
  state.toastMessage = text
  state.toastVisible = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    state.toastVisible = false
  }, 1800)
}

export function openWechatModal() {
  state.wechatModalVisible = true
}

export function closeWechatModal() {
  state.wechatModalVisible = false
}

export function confirmDialog(message, onConfirm, onCancel) {
  state.confirmMessage = String(message || '').trim() || '请确认操作'
  state.confirmVisible = true
  confirmOnConfirm = typeof onConfirm === 'function' ? onConfirm : null
  confirmOnCancel = typeof onCancel === 'function' ? onCancel : null
}

function closeConfirmDialog(confirmed) {
  const onConfirm = confirmOnConfirm
  const onCancel = confirmOnCancel
  confirmOnConfirm = null
  confirmOnCancel = null
  state.confirmVisible = false
  if (confirmed) {
    if (onConfirm) onConfirm()
    return
  }
  if (onCancel) onCancel()
}

export function cancelConfirmDialog() {
  closeConfirmDialog(false)
}

export function approveConfirmDialog() {
  closeConfirmDialog(true)
}

export function registerWindowBottomNavBridge() {
  if (typeof window === 'undefined') return
  window.AppBottomNav = {
    openWechatModal,
    closeWechatModal,
    notify,
    confirmDialog
  }
}
