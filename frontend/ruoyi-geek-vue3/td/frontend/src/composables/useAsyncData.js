import { onMounted, ref } from 'vue'

export function useAsyncData(loader, initialValue) {
  const data = ref(initialValue)
  const loading = ref(true)
  const error = ref('')
  const lastLoadedAt = ref('')

  const refresh = async () => {
    loading.value = true
    error.value = ''
    try {
      data.value = await loader()
      lastLoadedAt.value = new Intl.DateTimeFormat('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false,
      }).format(new Date())
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Unknown error'
    } finally {
      loading.value = false
    }
  }

  onMounted(refresh)

  return { data, loading, error, refresh, lastLoadedAt }
}
