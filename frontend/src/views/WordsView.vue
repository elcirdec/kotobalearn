<template>
  <main class="page">
    <div class="container">

      <header class="page-header">
        <p class="page-eyebrow">辞書</p>
        <h1>Vocabulaire</h1>
        <p class="page-lead" v-if="store.total > 0">{{ store.total.toLocaleString() }} mots trouvés</p>
        <p class="page-lead" v-else>215 715 mots · JMdict</p>
      </header>

      <section class="filters">
        <div class="search-wrap">
          <input v-model="searchInput" class="search-input"
            placeholder="Rechercher en japonais ou en anglais…"
            @keyup.enter="doSearch" />
          <button class="search-btn" @click="doSearch">Rechercher</button>
        </div>

        <div class="filter-group">
          <label class="filter-label">Niveau JLPT</label>
          <div class="toggles">
            <button :class="['toggle', { active: store.jlpt === '' }]" @click="setJlpt('')">Tous</button>
            <button v-for="j in ['N5','N4','N3','N2','N1']" :key="j"
              :class="['toggle', { active: store.jlpt === j }]" @click="setJlpt(j)">{{ j }}</button>
          </div>
        </div>

        <div class="filter-dropdowns">
          <div class="filter-group" v-for="fd in filterDefs" :key="fd.type">
            <label class="filter-label">{{ fd.label }}</label>
            <div class="searchable-select">
              <input v-model="fd.search.value" class="select-search" :placeholder="fd.placeholder"
                @focus="fd.show.value = true" @blur="delayHide(fd.type)" />
              <div class="select-dropdown" v-if="fd.show.value">
                <button v-for="t in filteredTags(fd.type, fd.search.value)" :key="t.tagCode"
                  @mousedown.prevent="selectTag(t, fd.type)"
                  class="select-option" :class="{active: isTagActive(t.tagCode)}">{{ t.tagLabel }}</button>
                <p v-if="filteredTags(fd.type, fd.search.value).length===0" class="select-empty">Aucun résultat</p>
              </div>
            </div>
          </div>
        </div>

        <div class="chips" v-if="store.hasFilters">
          <span v-if="store.search" class="chip">
            "{{ store.search }}" <button @click="clearSearch">×</button>
          </span>
          <span v-if="store.jlpt" class="chip chip-jlpt">
            JLPT {{ store.jlpt }} <button @click="setJlpt('')">×</button>
          </span>
          <span v-for="tag in store.activeTags" :key="tag.tagCode"
            :class="['chip', `chip-${tag.tagType}`]">
            <span class="chip-type">{{ typeLabel(tag.tagType) }}</span>
            {{ tag.tagLabel }}
            <button @click="store.removeTag(tag.tagCode)">×</button>
          </span>
          <button class="chip-reset" @click="resetAll">Tout effacer</button>
        </div>
      </section>

      <div v-if="!hasSearched" class="invitation">
        <span class="invitation-jp jp">探す</span>
        <p>Utilisez les filtres ou la barre de recherche<br>pour explorer le vocabulaire</p>
      </div>
      <div v-else-if="store.loading" class="loading">読み込み中…</div>
      <div v-else-if="store.words.length === 0" class="empty">
        <span class="jp">無</span>Aucun résultat
      </div>
      <div v-else>
        <div class="words-grid">
          <RouterLink v-for="word in store.words" :key="word.wordId"
            :to="`/mots/${word.wordId}`" class="word-card">
            <span class="word-jp jp">{{ word.wordJapanese }}</span>
            <span class="word-reading">{{ word.wordReading }}</span>
            <span class="word-en">{{ truncate(word.wordTranslationEn) }}</span>
          </RouterLink>
        </div>
        <nav class="pagination">
          <button @click="store.setPage(0)" :disabled="store.page===0">«</button>
          <button @click="store.setPage(store.page-1)" :disabled="store.page===0">‹</button>
          <button v-for="p in visiblePages" :key="p"
            :class="{active: p===store.page}" @click="store.setPage(p)">{{ p+1 }}</button>
          <button @click="store.setPage(store.page+1)" :disabled="store.page>=store.totalPages-1">›</button>
          <button @click="store.setPage(store.totalPages-1)" :disabled="store.page>=store.totalPages-1">»</button>
        </nav>
        <p class="pagination-info">
          Page {{ store.page+1 }} / {{ store.totalPages.toLocaleString() }} — {{ store.total.toLocaleString() }} mots
        </p>
      </div>

    </div>
  </main>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, onBeforeRouteLeave } from 'vue-router'
import { useWordsStore } from '../stores/words'

const store = useWordsStore()
const route = useRoute()

// UI locale
const searchInput = ref('')
const fieldSearch = ref(''); const showField = ref(false)
const posSearch   = ref(''); const showPos   = ref(false)
const miscSearch  = ref(''); const showMisc  = ref(false)
const dialSearch  = ref(''); const showDial  = ref(false)

const filterDefs = [
  { type: 'field', label: 'Thème',    placeholder: 'Cuisine, sport…',  search: fieldSearch, show: showField },
  { type: 'pos',   label: 'Nature',   placeholder: 'Nom, verbe…',       search: posSearch,   show: showPos   },
  { type: 'misc',  label: 'Registre', placeholder: 'Familier, formel…', search: miscSearch,  show: showMisc  },
  { type: 'dial',  label: 'Dialecte', placeholder: 'Kansai, Tokyo…',    search: dialSearch,  show: showDial  },
]

const hasSearched = computed(() => store.words.length > 0 || store.hasFilters)

// ── Au départ : on pose le flag SEULEMENT si on va sur une fiche mot ──────
onBeforeRouteLeave((to) => {
  store.returnFromChild = (to.name === 'word-detail')
})

// ── À l'arrivée : 3 cas ───────────────────────────────────────────────────
onMounted(async () => {
  await store.loadTags()

  // Cas 1 : tag cliqué depuis la fiche d'un mot
  const { tag, tagLabel, tagType } = route.query
  if (tag && tagLabel && tagType) {
    store.clear()
    store.returnFromChild = false
    searchInput.value = ''
    store.addTag({ tagCode: tag, tagLabel: decodeURIComponent(tagLabel), tagType })
    return
  }

  // Cas 2 : retour arrière depuis la fiche mot → on restaure
  if (store.returnFromChild) {
    store.returnFromChild = false
    searchInput.value = store.search // resynchronise la barre de recherche
    return
  }

  // Cas 3 : toute autre arrivée (accueil, kanji, scripts...) → on repart à zéro
  store.clear()
  searchInput.value = ''
})

function delayHide(type) {
  setTimeout(() => {
    if (type === 'field') showField.value = false
    if (type === 'pos')   showPos.value   = false
    if (type === 'misc')  showMisc.value  = false
    if (type === 'dial')  showDial.value  = false
  }, 150)
}

function filteredTags(type, q) {
  const list = store.availableTags[type] ?? []
  if (!q) return list
  return list.filter(t => t.tagLabel.toLowerCase().includes(q.toLowerCase()))
}

function selectTag(tag, type) {
  const fd = filterDefs.find(f => f.type === type)
  if (fd) { fd.show.value = false; fd.search.value = '' }
  store.addTag(tag)
}

function isTagActive(code) { return store.activeTags.some(t => t.tagCode === code) }
function typeLabel(type) {
  return { field: 'Thème', pos: 'Nature', misc: 'Registre', dial: 'Dialecte', ke_inf: 'Orthographe' }[type] || type
}

function setJlpt(j)   { store.setJlpt(j) }
function doSearch()   { store.setSearch(searchInput.value) }
function clearSearch() { searchInput.value = ''; store.setSearch('') }

function resetAll() {
  searchInput.value = ''
  filterDefs.forEach(fd => { fd.search.value = ''; fd.show.value = false })
  store.clear()
}

const visiblePages = computed(() => {
  const t = store.totalPages, cur = store.page, pages = []
  const start = Math.max(0, Math.min(cur - 2, t - 5))
  for (let i = start; i <= Math.min(t - 1, start + 4); i++) pages.push(i)
  return pages
})

function truncate(str, len = 60) {
  if (!str) return ''
  return str.length > len ? str.slice(0, len) + '…' : str
}
</script>

<style scoped>
.page-header { margin-bottom: 2.5rem; }
.page-eyebrow { font-family: var(--font-jp); font-size: 0.9rem; color: var(--vermilion); letter-spacing: 0.1em; margin-bottom: 0.5rem; }
.page-header h1 { font-size: clamp(2rem, 4vw, 3rem); }
.page-lead { color: var(--muted); margin-top: 0.5rem; }
.filters { margin-bottom: 2.5rem; display: flex; flex-direction: column; gap: 1.25rem; }
.search-wrap { display: flex; gap: 0.75rem; }
.search-wrap .search-input { flex: 1; }
.search-btn { padding: 0.75rem 1.5rem; background: var(--ink); color: var(--paper); border: none; border-radius: var(--radius); font-family: var(--font-display); font-size: 0.9rem; letter-spacing: 0.06em; cursor: pointer; white-space: nowrap; transition: background 0.2s; }
.search-btn:hover { background: var(--vermilion); }
.filter-group { display: flex; flex-direction: column; gap: 0.5rem; }
.filter-label { font-size: 0.72rem; letter-spacing: 0.1em; text-transform: uppercase; color: var(--muted); }
.toggles { display: flex; gap: 0.5rem; flex-wrap: nowrap; overflow-x: auto; }
.toggle { padding: 0.4rem 1.1rem; border: 1.5px solid var(--paper-mid); border-radius: 20px; background: white; font-family: var(--font-display); font-size: 0.9rem; color: var(--ink-light); cursor: pointer; white-space: nowrap; flex-shrink: 0; transition: all 0.2s ease; }
.toggle:hover { border-color: var(--ink); color: var(--ink); }
.toggle.active { background: var(--ink); border-color: var(--ink); color: var(--paper); }
.filter-dropdowns { display: flex; gap: 1rem; flex-wrap: wrap; }
.filter-dropdowns .filter-group { min-width: 160px; flex: 1; }
.searchable-select { position: relative; }
.select-search { width: 100%; padding: 0.65rem 1rem; border: 1.5px solid var(--paper-mid); border-radius: var(--radius); background: white; font-family: var(--font-body); font-size: 0.875rem; color: var(--ink); outline: none; transition: border-color 0.2s; }
.select-search:focus { border-color: var(--ink); }
.select-dropdown { position: absolute; top: calc(100% + 4px); left: 0; right: 0; background: white; border: 1.5px solid var(--ink); border-radius: var(--radius); max-height: 220px; overflow-y: auto; z-index: 50; box-shadow: var(--shadow-lg); }
.select-option { display: block; width: 100%; text-align: left; padding: 0.6rem 1rem; background: none; border: none; font-family: var(--font-body); font-size: 0.875rem; color: var(--ink); cursor: pointer; transition: background 0.15s; }
.select-option:hover { background: var(--paper); }
.select-option.active { background: #fdf0ee; color: var(--vermilion); font-weight: 500; }
.select-empty { padding: 0.75rem 1rem; color: var(--muted); font-size: 0.85rem; }
.chips { display: flex; flex-wrap: wrap; gap: 0.5rem; align-items: center; }
.chip { display: inline-flex; align-items: center; gap: 0.4rem; padding: 0.3rem 0.75rem; border-radius: 20px; font-size: 0.82rem; background: var(--paper-dark); border: 1px solid var(--paper-mid); color: var(--ink); }
.chip button { background: none; border: none; font-size: 1rem; line-height: 1; color: var(--muted); cursor: pointer; padding: 0; transition: color 0.2s; }
.chip button:hover { color: var(--vermilion); }
.chip-type { font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.05em; color: var(--muted); }
.chip-jlpt  { background: #fdf0ee; border-color: #f0c0b8; }
.chip-field { background: #e8eef8; border-color: #90aad4; }
.chip-pos   { background: #e8f0e8; border-color: #b8d4b8; }
.chip-misc  { background: #f0ece0; border-color: #d4c890; }
.chip-dial  { background: #ede8f0; border-color: #b890d4; }
.chip-reset { background: none; border: 1px dashed var(--paper-mid); border-radius: 20px; padding: 0.3rem 0.75rem; font-size: 0.8rem; color: var(--muted); cursor: pointer; transition: all 0.2s; }
.chip-reset:hover { border-color: var(--vermilion); color: var(--vermilion); }
.invitation { text-align: center; padding: 6rem 2rem; color: var(--muted); }
.invitation-jp { display: block; font-size: 5rem; opacity: 0.08; margin-bottom: 1.5rem; line-height: 1; }
.invitation p { font-family: var(--font-display); font-size: 1.1rem; letter-spacing: 0.04em; line-height: 1.8; }
.words-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 1rem; }
.word-card { display: flex; flex-direction: column; gap: 0.3rem; padding: 1.25rem 1.5rem; background: white; border: 1px solid var(--paper-mid); border-radius: var(--radius); text-decoration: none; color: var(--ink); transition: all 0.2s ease; }
.word-card:hover { border-color: var(--ink); transform: translateY(-2px); box-shadow: var(--shadow); }
.word-jp { font-size: 1.6rem; font-weight: 400; line-height: 1.2; }
.word-reading { font-size: 0.8rem; color: var(--muted); letter-spacing: 0.04em; }
.word-en { font-size: 0.85rem; color: var(--ink-light); line-height: 1.5; margin-top: 0.25rem; }
.pagination-info { text-align: center; margin-top: 1rem; color: var(--muted); font-size: 0.85rem; }
</style>