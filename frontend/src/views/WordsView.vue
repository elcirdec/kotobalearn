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

        <!-- Barre de recherche -->
        <div class="search-wrap">
          <input v-model="searchInput" class="search-input"
            placeholder="Rechercher en japonais ou en anglais…"
            @keyup.enter="doSearch" />
          <button class="search-btn" @click="doSearch">Rechercher</button>
        </div>

        <!-- Ligne 1 : dropdowns thématiques -->
        <div class="filter-bar">

          <div class="filter-unit" ref="fieldRef">
            <span class="filter-bar-label">Thème</span>
            <div class="dropdown-wrap">
              <button class="dropdown-btn" :class="{ active: hasTagOfType('field') }"
                @click.stop="toggleDropdown('field')">
                {{ hasTagOfType('field') ? countTagOfType('field') + ' sélect.' : 'Choisir' }}
                <span class="dropdown-arrow">▼</span>
              </button>
              <div class="dropdown-list" v-if="openDropdown === 'field'">
                <input v-model="fieldSearch" class="dropdown-search" placeholder="Filtrer…" @click.stop />
                <button v-for="t in filteredTags('field', fieldSearch)" :key="t.tagCode"
                  @click.stop="selectTag(t, 'field')"
                  class="dropdown-item" :class="{ active: isTagActive(t.tagCode) }">
                  <span class="check">{{ isTagActive(t.tagCode) ? '✓' : '' }}</span>{{ t.tagLabel }}
                </button>
              </div>
            </div>
          </div>

          <div class="filter-bar-sep">·</div>

          <div class="filter-unit" ref="posRef">
            <span class="filter-bar-label">Nature</span>
            <div class="dropdown-wrap">
              <button class="dropdown-btn" :class="{ active: hasTagOfType('pos') }"
                @click.stop="toggleDropdown('pos')">
                {{ hasTagOfType('pos') ? countTagOfType('pos') + ' sélect.' : 'Choisir' }}
                <span class="dropdown-arrow">▼</span>
              </button>
              <div class="dropdown-list" v-if="openDropdown === 'pos'">
                <input v-model="posSearch" class="dropdown-search" placeholder="Filtrer…" @click.stop />
                <button v-for="t in filteredTags('pos', posSearch)" :key="t.tagCode"
                  @click.stop="selectTag(t, 'pos')"
                  class="dropdown-item" :class="{ active: isTagActive(t.tagCode) }">
                  <span class="check">{{ isTagActive(t.tagCode) ? '✓' : '' }}</span>{{ t.tagLabel }}
                </button>
              </div>
            </div>
          </div>

          <div class="filter-bar-sep">·</div>

          <div class="filter-unit" ref="miscRef">
            <span class="filter-bar-label">Registre</span>
            <div class="dropdown-wrap">
              <button class="dropdown-btn" :class="{ active: hasTagOfType('misc') }"
                @click.stop="toggleDropdown('misc')">
                {{ hasTagOfType('misc') ? countTagOfType('misc') + ' sélect.' : 'Choisir' }}
                <span class="dropdown-arrow">▼</span>
              </button>
              <div class="dropdown-list" v-if="openDropdown === 'misc'">
                <input v-model="miscSearch" class="dropdown-search" placeholder="Filtrer…" @click.stop />
                <button v-for="t in filteredTags('misc', miscSearch)" :key="t.tagCode"
                  @click.stop="selectTag(t, 'misc')"
                  class="dropdown-item" :class="{ active: isTagActive(t.tagCode) }">
                  <span class="check">{{ isTagActive(t.tagCode) ? '✓' : '' }}</span>{{ t.tagLabel }}
                </button>
              </div>
            </div>
          </div>

          <div class="filter-bar-sep">·</div>

          <div class="filter-unit" ref="dialRef">
            <span class="filter-bar-label">Dialecte</span>
            <div class="dropdown-wrap">
              <button class="dropdown-btn" :class="{ active: hasTagOfType('dial') }"
                @click.stop="toggleDropdown('dial')">
                {{ hasTagOfType('dial') ? countTagOfType('dial') + ' sélect.' : 'Choisir' }}
                <span class="dropdown-arrow">▼</span>
              </button>
              <div class="dropdown-list" v-if="openDropdown === 'dial'">
                <input v-model="dialSearch" class="dropdown-search" placeholder="Filtrer…" @click.stop />
                <button v-for="t in filteredTags('dial', dialSearch)" :key="t.tagCode"
                  @click.stop="selectTag(t, 'dial')"
                  class="dropdown-item" :class="{ active: isTagActive(t.tagCode) }">
                  <span class="check">{{ isTagActive(t.tagCode) ? '✓' : '' }}</span>{{ t.tagLabel }}
                </button>
              </div>
            </div>
          </div>

        </div>

        <!-- Ligne 2 : JLPT -->
        <div class="filter-bar">
          <div class="filter-unit">
            <span class="filter-bar-label">JLPT</span>
            <div class="toggles">
              <button :class="['toggle', { active: store.jlpt === '' }]" @click="setJlpt('')">Tous</button>
              <button v-for="j in ['N5','N4','N3','N2','N1']" :key="j"
                :class="['toggle', { active: store.jlpt === j }]" @click="setJlpt(j)">{{ j }}</button>
            </div>
          </div>
        </div>

        <!-- Chips -->
        <div v-if="store.hasFilters" class="chips-row">
          <div class="chips">
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
          <div class="tag-mode" v-if="store.activeTags.length >= 2">
            <span class="tag-mode-label">Combiner :</span>
            <button :class="['toggle', 'toggle-sm', { active: store.tagMode === 'or' }]"
              @click="store.setTagMode('or')">OU</button>
            <button :class="['toggle', 'toggle-sm', { active: store.tagMode === 'and' }]"
              @click="store.setTagMode('and')">ET</button>
          </div>
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
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, onBeforeRouteLeave } from 'vue-router'
import { useWordsStore } from '../stores/words'

const store = useWordsStore()
const route = useRoute()

const searchInput  = ref('')
const openDropdown = ref(null) // 'field' | 'pos' | 'misc' | 'dial' | null

const fieldSearch = ref(''); const fieldRef = ref(null)
const posSearch   = ref(''); const posRef   = ref(null)
const miscSearch  = ref(''); const miscRef  = ref(null)
const dialSearch  = ref(''); const dialRef  = ref(null)

const hasSearched = computed(() => store.words.length > 0 || store.hasFilters)

// ── Dropdown : un seul ouvert à la fois, se ferme via click dehors ────────
function toggleDropdown(type) {
  openDropdown.value = openDropdown.value === type ? null : type
}

function handleClickOutside(e) {
  const refs = { field: fieldRef, pos: posRef, misc: miscRef, dial: dialRef }
  const current = openDropdown.value
  if (current && refs[current]?.value && !refs[current].value.contains(e.target)) {
    openDropdown.value = null
  }
}

// ── Tags ──────────────────────────────────────────────────────────────────
function filteredTags(type, q) {
  const list = store.availableTags[type] ?? []
  if (!q) return list
  return list.filter(t => t.tagLabel.toLowerCase().includes(q.toLowerCase()))
}

function selectTag(tag, type) {
  store.addTag(tag)
  // Ferme le dropdown après sélection — mais reste ouvert pour permettre
  // plusieurs sélections dans le même type (on rouvre au prochain clic)
  // Si on veut fermer immédiatement, décommenter la ligne suivante :
  // openDropdown.value = null
}

function isTagActive(code)      { return store.activeTags.some(t => t.tagCode === code) }
function hasTagOfType(type)     { return store.activeTags.some(t => t.tagType === type) }
function countTagOfType(type)   { return store.activeTags.filter(t => t.tagType === type).length }
function typeLabel(type)        { return { field: 'Thème', pos: 'Nature', misc: 'Registre', dial: 'Dialecte' }[type] || type }

function setJlpt(j)    { store.setJlpt(j) }
function doSearch()    { store.setSearch(searchInput.value) }
function clearSearch() { searchInput.value = ''; store.setSearch('') }

function resetAll() {
  searchInput.value = ''
  fieldSearch.value = ''; posSearch.value = ''; miscSearch.value = ''; dialSearch.value = ''
  openDropdown.value = null
  store.clear()
}

onBeforeRouteLeave((to) => {
  store.returnFromChild = (to.name === 'word-detail')
})

onMounted(async () => {
  document.addEventListener('click', handleClickOutside)
  await store.loadTags()

  const { tag, tagLabel, tagType } = route.query
  if (tag && tagLabel && tagType) {
    store.clear(); store.returnFromChild = false; searchInput.value = ''
    store.addTag({ tagCode: tag, tagLabel: decodeURIComponent(tagLabel), tagType })
    return
  }
  if (store.returnFromChild) {
    store.returnFromChild = false
    searchInput.value = store.search
    return
  }
  store.clear(); searchInput.value = ''
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})

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

.filters { margin-bottom: 2.5rem; display: flex; flex-direction: column; gap: 0.75rem; }
.search-wrap { display: flex; gap: 0.75rem; }
.search-wrap .search-input { flex: 1; }
.search-btn { padding: 0.75rem 1.5rem; background: var(--ink); color: var(--paper); border: none; border-radius: var(--radius); font-family: var(--font-display); font-size: 0.9rem; letter-spacing: 0.06em; cursor: pointer; white-space: nowrap; transition: background 0.2s; }
.search-btn:hover { background: var(--vermilion); }

.filter-bar { display: flex; align-items: center; gap: 1rem; flex-wrap: wrap; padding: 0.75rem 1rem; background: white; border: 1px solid var(--paper-mid); border-radius: var(--radius); }
.filter-bar-sep { color: var(--paper-mid); font-size: 1.2rem; flex-shrink: 0; }
.filter-unit { display: flex; align-items: center; gap: 0.6rem; flex-shrink: 0; }
.filter-bar-label { font-size: 0.68rem; letter-spacing: 0.12em; text-transform: uppercase; color: var(--muted); white-space: nowrap; }

.toggles { display: flex; gap: 0.35rem; }
.toggle { padding: 0.3rem 0.85rem; border: 1.5px solid var(--paper-mid); border-radius: 20px; background: white; font-family: var(--font-display); font-size: 0.85rem; color: var(--ink-light); cursor: pointer; white-space: nowrap; transition: all 0.2s ease; }
.toggle:hover { border-color: var(--ink); color: var(--ink); }
.toggle.active { background: var(--ink); border-color: var(--ink); color: var(--paper); }
.toggle-sm { padding: 0.25rem 0.7rem; font-size: 0.78rem; }

.dropdown-wrap { position: relative; }
.dropdown-btn { display: flex; align-items: center; gap: 0.5rem; padding: 0.3rem 0.9rem; border: 1.5px solid var(--paper-mid); border-radius: 20px; background: white; font-family: var(--font-display); font-size: 0.85rem; color: var(--ink-light); cursor: pointer; white-space: nowrap; transition: all 0.2s; }
.dropdown-btn:hover { border-color: var(--ink); color: var(--ink); }
.dropdown-btn.active { border-color: var(--vermilion); color: var(--vermilion); background: #fdf5f5; }
.dropdown-arrow { font-size: 0.6rem; opacity: 0.6; }
.dropdown-list { position: absolute; top: calc(100% + 4px); left: 0; min-width: 220px; background: white; border: 1.5px solid var(--ink); border-radius: var(--radius); max-height: 240px; overflow-y: auto; z-index: 100; box-shadow: var(--shadow-lg); }
.dropdown-search { width: 100%; padding: 0.5rem 0.75rem; border: none; border-bottom: 1px solid var(--paper-mid); font-size: 0.85rem; outline: none; font-family: var(--font-body); }
.dropdown-item { display: flex; align-items: center; gap: 0.5rem; width: 100%; text-align: left; padding: 0.5rem 0.75rem; background: none; border: none; font-family: var(--font-body); font-size: 0.875rem; color: var(--ink); cursor: pointer; transition: background 0.15s; }
.dropdown-item:hover { background: var(--paper); }
.dropdown-item.active { color: var(--vermilion); font-weight: 500; }
.check { width: 14px; font-size: 0.75rem; color: var(--vermilion); flex-shrink: 0; }

.chips-row { display: flex; flex-direction: column; gap: 0.6rem; }
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
.tag-mode { display: flex; align-items: center; gap: 0.5rem; }
.tag-mode-label { font-size: 0.78rem; color: var(--muted); }

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

@media (max-width: 768px) {
  .filter-bar { flex-direction: column; align-items: flex-start; }
  .filter-bar-sep { display: none; }
}
</style>