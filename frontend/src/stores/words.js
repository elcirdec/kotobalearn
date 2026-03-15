import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { wordsApi, tagsApi } from '../api'

export const useWordsStore = defineStore('words', () => {
  const words       = ref([])
  const total       = ref(0)
  const totalPages  = ref(0)
  const page        = ref(0)
  const size        = ref(20)
  const loading     = ref(false)
  const error       = ref(null)

  const search     = ref('')
  const jlpt       = ref('')
  const activeTags = ref([])

  const availableTags = ref({ field: [], pos: [], misc: [], dial: [] })
  const tagsLoaded    = ref(false)

  // Vrai uniquement quand on navigue vers /mots/:id
  // Remis à false dès qu'on revient sur WordsView
  const returnFromChild = ref(false)

  async function loadTags() {
    if (tagsLoaded.value) return
    try {
      const data = await tagsApi.list()
      availableTags.value = {
        field: data.filter(t => t.tagType === 'field').sort((a,b) => a.tagLabel.localeCompare(b.tagLabel)),
        pos:   data.filter(t => t.tagType === 'pos').sort((a,b) => a.tagLabel.localeCompare(b.tagLabel)),
        misc:  data.filter(t => t.tagType === 'misc').sort((a,b) => a.tagLabel.localeCompare(b.tagLabel)),
        dial:  data.filter(t => t.tagType === 'dial').sort((a,b) => a.tagLabel.localeCompare(b.tagLabel)),
      }
      tagsLoaded.value = true
    } catch (e) {
      console.error('Erreur chargement tags', e)
    }
  }

  async function fetchWords() {
    loading.value = true
    error.value   = null
    try {
      const params = { page: page.value, size: size.value }
      if (search.value) params.search = search.value
      if (jlpt.value)   params.jlpt   = jlpt.value
      if (activeTags.value.length > 0) params.tag = activeTags.value[0].tagCode

      const data = await wordsApi.list(params)
      words.value      = data.content ?? []
      total.value      = data.totalElements ?? 0
      totalPages.value = data.totalPages ?? 0
    } catch (e) {
      error.value = e.message
    } finally {
      loading.value = false
    }
  }

  function setPage(p)   { page.value = p; fetchWords() }
  function setSearch(s) { search.value = s; page.value = 0; fetchWords() }
  function setJlpt(j)   { jlpt.value = j; page.value = 0; fetchWords() }

  function addTag(tag) {
    if (!activeTags.value.find(t => t.tagCode === tag.tagCode)) {
      activeTags.value.push(tag)
      page.value = 0
      fetchWords()
    }
  }

  function removeTag(tagCode) {
    activeTags.value = activeTags.value.filter(t => t.tagCode !== tagCode)
    page.value = 0
    fetchWords()
  }

  // Remet tout à zéro sans déclencher de fetch
  function clear() {
    words.value = []; total.value = 0; totalPages.value = 0; page.value = 0
    search.value = ''; jlpt.value = ''; activeTags.value = []
  }

  // Reset complet avec fetch (bouton "Tout effacer")
  function reset() {
    clear()
    fetchWords()
  }

  const hasFilters = computed(() =>
    search.value || jlpt.value || activeTags.value.length > 0
  )

  return {
    words, total, totalPages, page, size, loading, error,
    search, jlpt, activeTags, availableTags, hasFilters,
    returnFromChild,
    loadTags, fetchWords, setPage, setSearch, setJlpt,
    addTag, removeTag, reset, clear
  }
})