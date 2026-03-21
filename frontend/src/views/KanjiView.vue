<template>
  <main class="page">
    <div class="container">

      <header class="page-header">
        <p class="page-eyebrow">漢字</p>
        <h1>Kanji</h1>
        <p class="page-lead" v-if="store.allKanji.length > 0">
          {{ store.allKanji.length.toLocaleString() }} kanji trouvés
        </p>
        <p class="page-lead" v-else>10 384 kanji · Kanjidic2 & KanjiAlive</p>
      </header>

      <section class="filters">

        <!-- Recherche -->
        <div class="search-wrap">
          <input v-model="store.searchInput" class="search-input"
            placeholder="Rechercher par signification (ex: water, love…)"
            @keyup.enter="store.fetchKanji" />
          <button class="search-btn" @click="store.fetchKanji">Rechercher</button>
        </div>

        <!-- Barre de filtres -->
        <div class="filter-bar">

          <!-- JLPT -->
          <div class="filter-unit">
            <span class="filter-bar-label">JLPT</span>
            <div class="toggles">
              <button :class="['toggle', { active: store.selectedJlpt === '' }]"
                @click="store.setJlpt('')">Tous</button>
              <button v-for="j in ['N5','N4','N3','N2','N1']" :key="j"
                :class="['toggle', { active: store.selectedJlpt === j }]"
                @click="store.setJlpt(j)">{{ j }}</button>
            </div>
          </div>

          <div class="filter-bar-sep">·</div>

          <!-- Année scolaire -->
          <div class="filter-unit">
            <span class="filter-bar-label">Année scolaire</span>
            <div class="toggles">
              <button :class="['toggle', { active: store.selectedGrade === '' }]"
                @click="store.setGrade('')">Toutes</button>
              <button v-for="g in [1,2,3,4,5,6]" :key="g"
                :class="['toggle', { active: store.selectedGrade === String(g) }]"
                @click="store.setGrade(String(g))">{{ g }}</button>
            </div>
          </div>

          <div class="filter-bar-sep">·</div>

          <!-- Traits -->
          <div class="filter-unit" ref="strokesRef">
            <span class="filter-bar-label">Traits</span>
            <div class="dropdown-wrap">
              <button class="dropdown-btn" :class="{ active: store.selectedStrokes !== '' }"
                @click.stop="showStrokes = !showStrokes">
                {{ store.selectedStrokes !== '' ? store.selectedStrokes + ' trait' + (store.selectedStrokes > 1 ? 's' : '') : 'Tous' }}
                <span class="dropdown-arrow">▼</span>
              </button>
              <div class="dropdown-list" v-if="showStrokes">
                <button class="dropdown-item"
                  :class="{ active: store.selectedStrokes === '' }"
                  @click="pickStroke('')">Tous</button>
                <button v-for="s in strokeOptions" :key="s"
                  class="dropdown-item" :class="{ active: store.selectedStrokes === String(s) }"
                  @click="pickStroke(String(s))">{{ s }} trait{{ s > 1 ? 's' : '' }}</button>
              </div>
            </div>
          </div>

          <div class="filter-bar-sep">·</div>

          <!-- Composants (multi, AND) -->
          <div class="filter-unit">
            <span class="filter-bar-label">Composants</span>
            <button class="dropdown-btn" :class="{ active: store.selectedRadicals.length > 0 }"
              @click="showRadicalGrid = !showRadicalGrid">
              <span v-if="store.selectedRadicals.length > 0" class="comp-count">
                {{ store.selectedRadicals.length }}
              </span>
              {{ store.selectedRadicals.length > 0 ? 'sélectionné(s)' : 'Choisir' }}
              <span class="dropdown-arrow">{{ showRadicalGrid ? '▲' : '▼' }}</span>
            </button>
          </div>

        </div>

        <!-- Chips filtres actifs -->
        <div class="chips" v-if="store.hasActiveFilters">
          <span v-if="store.selectedJlpt" class="chip chip-jlpt">
            JLPT {{ store.selectedJlpt }}
            <button @click="store.setJlpt('')">×</button>
          </span>
          <span v-if="store.selectedGrade" class="chip chip-grade">
            Année {{ store.selectedGrade }}
            <button @click="store.setGrade('')">×</button>
          </span>
          <span v-if="store.selectedStrokes !== ''" class="chip chip-strokes">
            {{ store.selectedStrokes }} trait{{ store.selectedStrokes > 1 ? 's' : '' }}
            <button @click="store.setStrokes('')">×</button>
          </span>
          <span v-for="r in store.selectedRadicals" :key="r.radId" class="chip chip-radical">
            <span class="radical-chip-char">{{ r.radCharacter }}</span>
            {{ r.radNameRomaji }}
            <button @click="store.removeRadical(r.radId)">×</button>
          </span>
          <button class="chip-reset" @click="resetAll">Tout effacer</button>
        </div>

        <!-- Grille composants collapsible -->
        <Transition name="collapse">
          <div v-if="showRadicalGrid" class="radical-grid-wrap">
            <div v-if="!store.radicalsLoaded" class="loading-sm">Chargement…</div>
            <div v-else-if="store.radicals.length === 0" class="empty-sm">
              Aucun composant disponible
            </div>
            <div v-else>
              <p class="radical-hint">
                Sélectionnez un ou plusieurs composants —
                les kanji doivent les contenir <strong>tous</strong>
              </p>
              <div v-for="group in store.radicalsByStrokes" :key="group.strokes"
                class="radical-group">
                <span class="radical-group-label">
                  {{ group.strokes }} trait{{ group.strokes > 1 ? 's' : '' }}
                </span>
                <div class="radical-row">
                  <button v-for="r in group.list" :key="r.radId"
                    :class="['radical-cell', { active: isSelected(r.radId) }]"
                    :title="`${r.radNameRomaji} · ${r.radMeaningEnglish}`"
                    @click="store.toggleRadical(r)">
                    {{ r.radCharacter }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </Transition>

      </section>

      <!-- Contenu -->
      <div v-if="!store.hasSearched" class="invitation">
        <span class="invitation-jp jp">探す</span>
        <p>Utilisez les filtres ou la barre de recherche<br>pour explorer les kanji</p>
      </div>
      <div v-else-if="store.loading" class="loading">読み込み中…</div>
      <div v-else-if="store.allKanji.length === 0" class="empty">
        <span class="jp">無</span>Aucun résultat
      </div>
      <div v-else>
        <div class="kanji-grid">
          <RouterLink v-for="k in store.pagedKanji" :key="k.kanjiId"
            :to="`/kanji/${k.kanjiId}`" class="kanji-card">
            <span class="kanji-char jp">{{ k.kanjiCharacter }}</span>
            <span class="kanji-meaning">{{ truncate(k.kanjiMeaningEnglish, 28) }}</span>
            <span class="kanji-jlpt" v-if="k.jlptCode">{{ k.jlptCode }}</span>
          </RouterLink>
        </div>
        <nav class="pagination" v-if="store.totalPages > 1">
          <button @click="store.page = 0" :disabled="store.page === 0">«</button>
          <button @click="store.page--"   :disabled="store.page === 0">‹</button>
          <button v-for="p in visiblePages" :key="p"
            :class="{ active: p === store.page }" @click="store.page = p">{{ p + 1 }}</button>
          <button @click="store.page++"   :disabled="store.page >= store.totalPages - 1">›</button>
          <button @click="store.page = store.totalPages - 1"
            :disabled="store.page >= store.totalPages - 1">»</button>
        </nav>
        <p class="pagination-info">
          Page {{ store.page + 1 }} / {{ store.totalPages }} —
          {{ store.allKanji.length.toLocaleString() }} kanji
        </p>
      </div>

    </div>
  </main>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { useKanjiStore } from '../stores/kanji'

const store = useKanjiStore()

const strokeOptions   = Array.from({ length: 64 }, (_, i) => i + 1)
const showStrokes     = ref(false)
const showRadicalGrid = ref(false)
const strokesRef      = ref(null)

function pickStroke(s) { showStrokes.value = false; store.setStrokes(s) }
function isSelected(id) { return store.selectedRadicals.some(r => r.radId === id) }

function resetAll() {
  showStrokes.value = false
  showRadicalGrid.value = false
  store.clear()
}

function handleClickOutside(e) {
  if (strokesRef.value && !strokesRef.value.contains(e.target)) {
    showStrokes.value = false
  }
}

onBeforeRouteLeave((to) => {
  store.returnFromChild = (to.name === 'kanji-detail')
})

onMounted(async () => {
  document.addEventListener('click', handleClickOutside)
  store.loadRadicals()
  if (store.returnFromChild) {
    store.returnFromChild = false
    return
  }
  store.clear()
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

function truncate(str, len = 28) {
  if (!str) return ''
  return str.length > len ? str.slice(0, len) + '…' : str
}
</script>

<style scoped>
.page-header { margin-bottom: 2rem; }
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

.dropdown-wrap { position: relative; }
.dropdown-btn { display: flex; align-items: center; gap: 0.5rem; padding: 0.3rem 0.9rem; border: 1.5px solid var(--paper-mid); border-radius: 20px; background: white; font-family: var(--font-display); font-size: 0.85rem; color: var(--ink-light); cursor: pointer; white-space: nowrap; transition: all 0.2s; }
.dropdown-btn:hover { border-color: var(--ink); color: var(--ink); }
.dropdown-btn.active { border-color: var(--vermilion); color: var(--vermilion); background: #fdf5f5; }
.dropdown-arrow { font-size: 0.6rem; opacity: 0.6; }
.comp-count { display: inline-flex; align-items: center; justify-content: center; width: 18px; height: 18px; background: var(--vermilion); color: white; border-radius: 50%; font-size: 0.7rem; font-weight: bold; }
.dropdown-list { position: absolute; top: calc(100% + 4px); left: 0; min-width: 140px; background: white; border: 1.5px solid var(--ink); border-radius: var(--radius); max-height: 220px; overflow-y: auto; z-index: 50; box-shadow: var(--shadow-lg); }
.dropdown-item { display: block; width: 100%; text-align: left; padding: 0.5rem 1rem; background: none; border: none; font-family: var(--font-body); font-size: 0.875rem; color: var(--ink); cursor: pointer; transition: background 0.15s; }
.dropdown-item:hover { background: var(--paper); }
.dropdown-item.active { background: #fdf0ee; color: var(--vermilion); font-weight: 500; }

.chips { display: flex; flex-wrap: wrap; gap: 0.5rem; align-items: center; }
.chip { display: inline-flex; align-items: center; gap: 0.4rem; padding: 0.3rem 0.75rem; border-radius: 20px; font-size: 0.82rem; background: var(--paper-dark); border: 1px solid var(--paper-mid); color: var(--ink); }
.chip button { background: none; border: none; font-size: 1rem; line-height: 1; color: var(--muted); cursor: pointer; padding: 0; transition: color 0.2s; }
.chip button:hover { color: var(--vermilion); }
.chip-jlpt    { background: #fdf0ee; border-color: #f0c0b8; }
.chip-grade   { background: #e8eef8; border-color: #90aad4; }
.chip-strokes { background: #e8f0e8; border-color: #b8d4b8; }
.chip-radical { background: #f5f0e8; border-color: #d4c890; }
.radical-chip-char { font-family: var(--font-cjk, 'Noto Sans JP', sans-serif); font-size: 1rem; color: var(--vermilion); line-height: 1; }
.chip-reset { background: none; border: 1px dashed var(--paper-mid); border-radius: 20px; padding: 0.3rem 0.75rem; font-size: 0.8rem; color: var(--muted); cursor: pointer; transition: all 0.2s; }
.chip-reset:hover { border-color: var(--vermilion); color: var(--vermilion); }

.radical-hint { font-size: 0.78rem; color: var(--muted); font-style: italic; margin-bottom: 0.75rem; }
.radical-grid-wrap { background: white; border: 1px solid var(--paper-mid); border-radius: var(--radius); padding: 1.25rem; max-height: 360px; overflow-y: auto; }
.radical-group { margin-bottom: 0.75rem; }
.radical-group:last-child { margin-bottom: 0; }
.radical-group-label { font-size: 0.65rem; letter-spacing: 0.1em; text-transform: uppercase; color: var(--muted); display: block; margin-bottom: 0.35rem; }
.radical-row { display: flex; flex-wrap: wrap; gap: 0.3rem; }
.radical-cell { font-family: var(--font-cjk, 'Noto Sans JP', sans-serif); font-size: 1.1rem; line-height: 1; width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; background: var(--paper-dark); border: 1px solid var(--paper-mid); border-radius: var(--radius); cursor: pointer; transition: all 0.15s; color: var(--ink); }
.radical-cell:hover { border-color: var(--vermilion); color: var(--vermilion); background: #fdf5f5; }
.radical-cell.active { background: var(--vermilion); color: white; border-color: var(--vermilion); }

.collapse-enter-active, .collapse-leave-active { transition: max-height 0.3s ease, opacity 0.25s ease; overflow: hidden; max-height: 400px; }
.collapse-enter-from, .collapse-leave-to { max-height: 0; opacity: 0; }

.invitation { text-align: center; padding: 6rem 2rem; color: var(--muted); }
.invitation-jp { display: block; font-size: 5rem; opacity: 0.08; margin-bottom: 1.5rem; line-height: 1; }
.invitation p { font-family: var(--font-display); font-size: 1.1rem; letter-spacing: 0.04em; line-height: 1.8; }
.kanji-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 0.75rem; }
.kanji-card { display: flex; flex-direction: column; align-items: center; gap: 0.4rem; padding: 1.5rem 0.75rem 1rem; background: white; border: 1px solid var(--paper-mid); border-radius: var(--radius); text-decoration: none; color: var(--ink); position: relative; transition: all 0.2s ease; text-align: center; }
.kanji-card:hover { border-color: var(--ink); transform: translateY(-2px); box-shadow: var(--shadow); }
.kanji-char { font-size: 2.5rem; line-height: 1; }
.kanji-meaning { font-size: 0.72rem; color: var(--muted); line-height: 1.4; }
.kanji-jlpt { position: absolute; top: 0.4rem; right: 0.4rem; font-size: 0.6rem; letter-spacing: 0.05em; background: var(--vermilion); color: white; padding: 0.1rem 0.35rem; border-radius: 2px; }
.loading-sm { padding: 1.5rem; text-align: center; color: var(--muted); }
.empty-sm { padding: 1rem; color: var(--muted); font-style: italic; }
.pagination-info { text-align: center; margin-top: 1rem; color: var(--muted); font-size: 0.85rem; }

@media (max-width: 768px) {
  .filter-bar { flex-direction: column; align-items: flex-start; }
  .filter-bar-sep { display: none; }
}
</style>