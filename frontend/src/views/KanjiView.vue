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
        <div class="search-wrap">
          <input v-model="store.searchInput" class="search-input"
            placeholder="Rechercher un kanji ou sa signification…"
            @keyup.enter="store.fetchKanji" />
          <button class="search-btn" @click="store.fetchKanji">Rechercher</button>
        </div>

        <div class="filter-group">
          <label class="filter-label">Niveau JLPT</label>
          <div class="toggles">
            <button :class="['toggle', { active: store.selectedJlpt === '' }]" @click="store.setJlpt('')">Tous</button>
            <button v-for="j in ['N5','N4','N3','N2','N1']" :key="j"
              :class="['toggle', { active: store.selectedJlpt === j }]" @click="store.setJlpt(j)">{{ j }}</button>
          </div>
        </div>

        <div class="filter-group">
          <label class="filter-label">Année scolaire</label>
          <div class="toggles">
            <button :class="['toggle', { active: store.selectedGrade === '' }]" @click="store.setGrade('')">Toutes</button>
            <button v-for="g in [1,2,3,4,5,6]" :key="g"
              :class="['toggle', { active: store.selectedGrade === String(g) }]"
              @click="store.setGrade(String(g))">{{ g }}</button>
          </div>
        </div>
      </section>

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
          <button @click="store.page--" :disabled="store.page === 0">‹</button>
          <button v-for="p in visiblePages" :key="p"
            :class="{ active: p === store.page }" @click="store.page = p">{{ p + 1 }}</button>
          <button @click="store.page++" :disabled="store.page >= store.totalPages - 1">›</button>
          <button @click="store.page = store.totalPages - 1" :disabled="store.page >= store.totalPages - 1">»</button>
        </nav>
        <p class="pagination-info">
          Page {{ store.page + 1 }} / {{ store.totalPages }} — {{ store.allKanji.length.toLocaleString() }} kanji
        </p>
      </div>

    </div>
  </main>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { useKanjiStore } from '../stores/kanji'

const store = useKanjiStore()

// ── Au départ : flag SEULEMENT si on va sur une fiche kanji ───────────────
onBeforeRouteLeave((to) => {
  store.returnFromChild = (to.name === 'kanji-detail')
})

// ── À l'arrivée ───────────────────────────────────────────────────────────
onMounted(() => {
  // Retour depuis fiche kanji → on restaure
  if (store.returnFromChild) {
    store.returnFromChild = false
    return
  }
  // Toute autre arrivée → repart à zéro
  store.clear()
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
.invitation { text-align: center; padding: 6rem 2rem; color: var(--muted); }
.invitation-jp { display: block; font-size: 5rem; opacity: 0.08; margin-bottom: 1.5rem; line-height: 1; }
.invitation p { font-family: var(--font-display); font-size: 1.1rem; letter-spacing: 0.04em; line-height: 1.8; }
.kanji-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 0.75rem; }
.kanji-card { display: flex; flex-direction: column; align-items: center; gap: 0.4rem; padding: 1.5rem 0.75rem 1rem; background: white; border: 1px solid var(--paper-mid); border-radius: var(--radius); text-decoration: none; color: var(--ink); position: relative; transition: all 0.2s ease; text-align: center; }
.kanji-card:hover { border-color: var(--ink); transform: translateY(-2px); box-shadow: var(--shadow); }
.kanji-char { font-size: 2.5rem; line-height: 1; }
.kanji-meaning { font-size: 0.72rem; color: var(--muted); line-height: 1.4; }
.kanji-jlpt { position: absolute; top: 0.4rem; right: 0.4rem; font-size: 0.6rem; letter-spacing: 0.05em; background: var(--vermilion); color: white; padding: 0.1rem 0.35rem; border-radius: 2px; }
.pagination-info { text-align: center; margin-top: 1rem; color: var(--muted); font-size: 0.85rem; }
</style>