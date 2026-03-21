import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { kanjiApi, radicalsApi } from '../api'

export const useKanjiStore = defineStore('kanji', () => {
  const allKanji  = ref([])
  const loading   = ref(false)
  const page      = ref(0)

  const searchInput      = ref('')
  const selectedJlpt     = ref('')
  const selectedGrade    = ref('')
  const selectedStrokes  = ref('')
  // Multi-composants AND : tableau de { radId, radCharacter, radNameRomaji, radMeaningEnglish }
  const selectedRadicals = ref([])

  const radicals       = ref([])
  const radicalsLoaded = ref(false)
  const returnFromChild = ref(false)

  const PAGE_SIZE  = 40
  const totalPages = computed(() => Math.ceil(allKanji.value.length / PAGE_SIZE))
  const pagedKanji = computed(() => allKanji.value.slice(
    page.value * PAGE_SIZE, (page.value + 1) * PAGE_SIZE
  ))
  const hasSearched = computed(() => allKanji.value.length > 0)

  // Radicaux triÃ©s par traits â€” filtrÃ©s sur ceux qui ont un nom (KanjiAlive)
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
      if (selectedRadicals.value.length > 0)
        params.radicalIds = selectedRadicals.value.map(r => r.radId).join(',')
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

  function toggleRadical(r) {
    const idx = selectedRadicals.value.findIndex(x => x.radId === r.radId)
    if (idx >= 0) {
      selectedRadicals.value.splice(idx, 1)
    } else {
      selectedRadicals.value.push(r)
    }
    fetchKanji()
  }

  function addRadicalById(radId) {
    // Ajoute un radical par ID (utilisÃ© depuis l'URL ou KanjiDetailView)
    if (selectedRadicals.value.some(r => r.radId === radId)) return
    const radical = radicals.value.find(r => r.radId === radId)
    if (radical) {
      selectedRadicals.value.push(radical)
    } else {
      // Radical pas encore chargÃ© â†’ placeholder minimal
      selectedRadicals.value.push({ radId, radCharacter: '?', radNameRomaji: null, radMeaningEnglish: null })
    }
  }

  function removeRadical(radId) {
    selectedRadicals.value = selectedRadicals.value.filter(r => r.radId !== radId)
    fetchKanji()
  }

  function clearRadicals() {
    selectedRadicals.value = []
    fetchKanji()
  }

  function clear() {
    allKanji.value = []; page.value = 0
    searchInput.value = ''; selectedJlpt.value = ''
    selectedGrade.value = ''; selectedStrokes.value = ''
    selectedRadicals.value = []
  }

  const hasActiveFilters = computed(() =>
    selectedJlpt.value || selectedGrade.value ||
    selectedStrokes.value !== '' || selectedRadicals.value.length > 0
  )

  return {
    allKanji, loading, page,
    searchInput, selectedJlpt, selectedGrade, selectedStrokes, selectedRadicals,
    radicals, radicalsByStrokes, radicalsLoaded,
    returnFromChild, totalPages, pagedKanji, hasSearched, hasActiveFilters,
    loadRadicals, fetchKanji, setJlpt, setGrade, setStrokes,
    toggleRadical, addRadicalById, removeRadical, clearRadicals, clear
  }
})

