import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { kanjiApi, radicalsApi } from '../api'

export const useKanjiStore = defineStore('kanji', () => {
  const allKanji  = ref([])
  const loading   = ref(false)
  const page      = ref(0)

  const searchInput        = ref('')
  const selectedJlpt       = ref('')
  const selectedGradeGroup = ref('')   // 'primaire' | 'secondaire' | 'prenoms' | ''
  const selectedStrokes    = ref('')
  const selectedRadicals   = ref([])

  const radicals        = ref([])
  const radicalsLoaded  = ref(false)
  const returnFromChild = ref(false)

  // Liste des nombres de traits disponibles selon les filtres actifs
  const strokeCounts = ref([])

  const PAGE_SIZE  = 40
  const totalPages = computed(() => Math.ceil(allKanji.value.length / PAGE_SIZE))
  const pagedKanji = computed(() => allKanji.value.slice(
    page.value * PAGE_SIZE, (page.value + 1) * PAGE_SIZE
  ))
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

  /**
   * Charge les nombres de traits disponibles EN TENANT COMPTE des filtres actifs.
   * Si jlpt="N1" est sélectionné, seuls les traits qui existent en N1 sont proposés.
   * Si le trait actuellement sélectionné n'existe plus dans la nouvelle liste, il est réinitialisé.
   */
  async function loadStrokeCounts() {
    try {
      const params = {}
      if (selectedJlpt.value)       params.jlpt       = selectedJlpt.value
      if (selectedGradeGroup.value) params.gradeGroup  = selectedGradeGroup.value

      const counts = await kanjiApi.getStrokeCounts(params)
      strokeCounts.value = counts

      // Si le trait sélectionné n'existe plus dans la liste filtrée, on le retire
      if (selectedStrokes.value !== '' && !counts.includes(Number(selectedStrokes.value))) {
        selectedStrokes.value = ''
      }
    } catch (e) {
      console.error('Erreur chargement nombres de traits', e)
      // Fallback minimal si l'API échoue
      strokeCounts.value = [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,32,33,34]
    }
  }

  async function fetchKanji() {
    loading.value = true
    page.value    = 0
    try {
      const params = {}
      if (selectedJlpt.value)       params.jlpt       = selectedJlpt.value
      if (selectedGradeGroup.value) params.gradeGroup  = selectedGradeGroup.value
      if (selectedStrokes.value)    params.strokes     = selectedStrokes.value
      if (selectedRadicals.value.length > 0)
        params.radicalIds = selectedRadicals.value.map(r => r.radId).join(',')
      if (searchInput.value)        params.search      = searchInput.value

      const data = await kanjiApi.list(params)
      allKanji.value = Array.isArray(data) ? data : (data.content ?? [])
    } catch (e) {
      console.error('Erreur kanji', e)
      allKanji.value = []
    } finally {
      loading.value = false
    }
  }

  // Quand JLPT change → recharger les traits disponibles ET les kanji
  function setJlpt(j) {
    selectedJlpt.value = j
    selectedStrokes.value = ''   // reset le filtre traits (peut ne plus être valide)
    loadStrokeCounts()           // recalcule la liste selon le nouveau JLPT
    fetchKanji()
  }

  // Quand le groupe de grade change → idem
  function setGradeGroup(g) {
    selectedGradeGroup.value = g
    selectedStrokes.value = ''   // reset le filtre traits
    loadStrokeCounts()           // recalcule la liste selon le nouveau groupe
    fetchKanji()
  }

  function setStrokes(s)    { selectedStrokes.value     = s; fetchKanji() }

  function toggleRadical(r) {
    const idx = selectedRadicals.value.findIndex(x => x.radId === r.radId)
    if (idx >= 0) selectedRadicals.value.splice(idx, 1)
    else          selectedRadicals.value.push(r)
    fetchKanji()
  }

  function addRadicalById(radId) {
    if (selectedRadicals.value.some(r => r.radId === radId)) return
    const radical = radicals.value.find(r => r.radId === radId)
    if (radical) selectedRadicals.value.push(radical)
    else         selectedRadicals.value.push({ radId, radCharacter: '?', radNameRomaji: null })
  }

  function removeRadical(radId) {
    selectedRadicals.value = selectedRadicals.value.filter(r => r.radId !== radId)
    fetchKanji()
  }

  function clear() {
    allKanji.value = []; page.value = 0
    searchInput.value = ''; selectedJlpt.value = ''
    selectedGradeGroup.value = ''; selectedStrokes.value = ''
    selectedRadicals.value = []
    loadStrokeCounts()   // recharge la liste complète sans filtre
  }

  const hasActiveFilters = computed(() =>
    selectedJlpt.value || selectedGradeGroup.value ||
    selectedStrokes.value !== '' || selectedRadicals.value.length > 0
  )

  return {
    allKanji, loading, page,
    searchInput, selectedJlpt, selectedGradeGroup, selectedStrokes, selectedRadicals,
    radicals, radicalsByStrokes, radicalsLoaded,
    returnFromChild, totalPages, pagedKanji, hasSearched, hasActiveFilters,
    strokeCounts,
    loadRadicals, fetchKanji,
    loadStrokeCounts,
    setJlpt, setGradeGroup, setStrokes,
    toggleRadical, addRadicalById, removeRadical, clear
  }
})