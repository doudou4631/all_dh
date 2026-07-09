import { defineStore } from 'pinia'

export interface MarkUserPageSnapshot {
  submitForm: {
    phonesText: string
    requestNo: string
    remark: string
  }
  precheckDialogData: Record<string, unknown>
  precheckSourcePayload: Record<string, unknown> | null
  precheckKeyword: string
  precheckQueryStatus: string
  precheckMarkStatus: string
  precheckSelectedPhones: string[]
  activeSubTab: string
  queryParams: Record<string, unknown>
  recordDateRange: string[]
}

const useMarkUserPageStore = defineStore('markUserPage', {
  state: () => ({
    snapshots: {} as Record<string, MarkUserPageSnapshot>
  }),
  actions: {
    saveSnapshot(platformCode: string, snapshot: MarkUserPageSnapshot) {
      const key = String(platformCode || '__default__').trim() || '__default__'
      this.snapshots[key] = snapshot
    },
    getSnapshot(platformCode: string): MarkUserPageSnapshot | null {
      const key = String(platformCode || '__default__').trim() || '__default__'
      return this.snapshots[key] || null
    },
    clearSnapshot(platformCode: string) {
      const key = String(platformCode || '__default__').trim() || '__default__'
      delete this.snapshots[key]
    }
  }
})

export default useMarkUserPageStore
