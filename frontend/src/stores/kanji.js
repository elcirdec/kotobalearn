import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { kanjiApi, radicalsApi } from '../api'

export const useKanjiStore = defineStore('kanji', () => {
  const allKanji  = ref([])
  const loading   = ref(false)
  const page      = ref(0)

  const searchInput     = ref('')
  const selectedJlpt    = ref('')
  const selectedGrade   = ref('')
  const selectedStrokes = ref('')
  const selectedRadical = ref(null) // { radId, radCharacter, radNameRomaji, radMeaningEnglish }

  const radicals       = ref([])
  const radicalsLoaded = ref(false)
  const returnFromChild = ref(false)

  const PAGE_SIZE  = 40
  const totalPages = computed(() => Math.ceil(allKanji.value.length / PAGE_SIZE))
  const pagedKanji = computed(() => allKanji.value.slice(page.value * PAGE_SIZE, (page.value + 1) * PAGE_SIZE))
  const hasSearched = computed(() => allKanji.value.length > 0)

  const radicalsByStrokes = computed(() => {
    const groups = {}
    for (const r of radicals.value) {
      const s = r.radStrokes ?? 0
      if (!groups[s]) groups[s] = []
      groups[s].push(r)
    }
    return Object.entries(groups)
      .sort(([a], [b]) => Number(a) - Number(b))
      .map(([strokes, list]) => ({ strokes: Number(strokes), list }))
  })

  async function loadRadicals() {
    if (radicalsLoaded.value) return
    try {
      radicals.value = await radicalsApi.list()
      radicalsLoaded.value = true
    } catch (e) {
      console.error('Erreur chargement radicaux', e)
    }
  }

  async function fetchKanji() {
    loading.value = true
    page.value    = 0
    try {
      const params = {}
      if (selectedJlpt.value)    params.jlpt      = selectedJlpt.value
      if (selectedGrade.value)   params.grade     = selectedGrade.value
      if (selectedStrokes.value) params.strokes   = selectedStrokes.value
      if (selectedRadical.value) params.radicalIds = selectedRadical.value.radId
      if (searchInput.value)     params.search    = searchInput.value

      const data = await kanjiApi.list(params)
      allKanji.value = Array.isArray(data) ? data : (data.content ?? [])
    } catch (e) {
      console.error('Erreur kanji', e)
      allKanji.value = []
    } finally {
      loading.value = false
    }
  }

  function setJlpt(j)    { selectedJlpt.value   = j; fetchKanji() }
  function setGrade(g)   { selectedGrade.value  = g; fetchKanji() }
  function setStrokes(s) { selectedStrokes.value = s; fetchKanji() }

  // Toggle : même radical → désélectionne
  function setRadical(r) {
    if (selectedRadical.value?.radId === r.radId) {
      selectedRadical.value = null
    } else {
      selectedRadical.value = r
    }
    fetchKanji()
  }

  function clearRadical() {
    selectedRadical.value = null
    fetchKanji()
  }

  function clear() {
    allKanji.value = []; page.value = 0
    searchInput.value = ''; selectedJlpt.value = ''
    selectedGrade.value = ''; selectedStrokes.value = ''
    selectedRadical.value = null
  }

  const hasActiveFilters = computed(() =>
    selectedJlpt.value || selectedGrade.value ||
    selectedStrokes.value !== '' || selectedRadical.value !== null
  )

  return {
    allKanji, loading, page,
    searchInput, selectedJlpt, selectedGrade, selectedStrokes, selectedRadical,
    radicals, radicalsByStrokes, radicalsLoaded,
    returnFromChild, totalPages, pagedKanji, hasSearched, hasActiveFilters,
    loadRadicals, fetchKanji, setJlpt, setGrade, setStrokes,
    setRadical, clearRadical, clear
  }
})