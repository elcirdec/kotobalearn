import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { kanjiApi } from '../api'

export const useKanjiStore = defineStore('kanji', () => {
  const allKanji  = ref([])
  const loading   = ref(false)
  const page      = ref(0)

  const searchInput   = ref('')
  const selectedJlpt  = ref('')
  const selectedGrade = ref('')

  // Vrai uniquement quand on navigue vers /kanji/:id
  const returnFromChild = ref(false)

  const PAGE_SIZE  = 40
  const totalPages = computed(() => Math.ceil(allKanji.value.length / PAGE_SIZE))

  const pagedKanji = computed(() => {
    const start = page.value * PAGE_SIZE
    return allKanji.value.slice(start, start + PAGE_SIZE)
  })

  const hasSearched = computed(() => allKanji.value.length > 0)

  async function fetchKanji() {
    loading.value = true
    page.value    = 0
    try {
      const params = {}
      if (selectedJlpt.value)  params.jlpt   = selectedJlpt.value
      if (selectedGrade.value) params.grade  = selectedGrade.value
      if (searchInput.value)   params.search = searchInput.value

      const data = await kanjiApi.list(params)
      allKanji.value = Array.isArray(data) ? data : (data.content ?? [])
    } catch (e) {
      console.error('Erreur kanji', e)
      allKanji.value = []
    } finally {
      loading.value = false
    }
  }

  function setJlpt(j)  { selectedJlpt.value  = j; fetchKanji() }
  function setGrade(g) { selectedGrade.value = g; fetchKanji() }

  // Remet tout à zéro sans fetch
  function clear() {
    allKanji.value = []; page.value = 0
    searchInput.value = ''; selectedJlpt.value = ''; selectedGrade.value = ''
  }

  return {
    allKanji, loading, page, searchInput, selectedJlpt, selectedGrade,
    returnFromChild, totalPages, pagedKanji, hasSearched,
    fetchKanji, setJlpt, setGrade, clear
  }
})