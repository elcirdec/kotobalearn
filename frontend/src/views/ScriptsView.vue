<template>
  <main class="page">
    <div class="container">

      <header class="page-header">
        <p class="page-eyebrow">かな</p>
        <h1>Kana</h1>
        <p class="page-lead">Les deux syllabaires fondamentaux du japonais · 224 kana</p>
      </header>

      <!-- Onglets -->
      <div class="tabs">
        <button :class="['tab', { active: activeTab === 'HIRAGANA' }]"
          @click="switchTab('HIRAGANA')">
          Hiragana <span class="tab-jp">あ</span>
        </button>
        <button :class="['tab', { active: activeTab === 'KATAKANA' }]"
          @click="switchTab('KATAKANA')">
          Katakana <span class="tab-jp">ア</span>
        </button>
      </div>

      <div v-if="loading" class="loading">読み込み中…</div>

      <div v-else class="kana-layout">

        <!-- Grille kana -->
        <div class="kana-grid">
          <button
            v-for="kana in currentKana"
            :key="kana.scId"
            :class="['kana-card', { active: selected?.scId === kana.scId }]"
            @click="select(kana)"
          >
            <span class="kana-char jp">{{ kana.scCharacter }}</span>
            <span class="kana-romaji">{{ kana.scRomaji }}</span>
          </button>
        </div>

        <!-- Panneau détail avec StrokePlayer -->
        <Transition name="slide-panel">
          <div v-if="selected" class="kana-panel">

            <button class="panel-close" @click="selected = null" title="Fermer">×</button>

            <!-- Player unifié : kana simple ou composé (みゆ → côte à côte, contrôles unifiés) -->
            <CompoundStrokePlayer
              :characters="selected.scCharacter"
              type="kana"
              :key="selected.scId"
            />

            <!-- Infos -->
            <div class="panel-info">
              <p class="panel-romaji">{{ selected.scRomaji }}</p>

              <span :class="['badge', selected.scType === 'HIRAGANA' ? 'badge-hiragana' : 'badge-katakana']">
                {{ selected.scType === 'HIRAGANA' ? 'Hiragana' : 'Katakana' }}
              </span>

              <!-- Équivalent -->
              <button v-if="equivalent" class="equivalent-btn" @click="select(equivalent)">
                <span class="jp">{{ equivalent.scCharacter }}</span>
                <span class="equivalent-label">
                  {{ selected.scType === 'HIRAGANA' ? 'Katakana' : 'Hiragana' }}
                </span>
              </button>
            </div>

            <!-- Navigation prev / next -->
            <div class="panel-nav">
              <button class="nav-btn" @click="selectPrev" :disabled="!prevKana">
                ← <span v-if="prevKana" class="jp">{{ prevKana.scCharacter }}</span>
              </button>
              <span class="nav-pos">{{ currentIndex + 1 }} / {{ currentKana.length }}</span>
              <button class="nav-btn" @click="selectNext" :disabled="!nextKana">
                <span v-if="nextKana" class="jp">{{ nextKana.scCharacter }}</span> →
              </button>
            </div>

          </div>
        </Transition>

      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { scriptsApi } from '../api'
import CompoundStrokePlayer from '../components/CompoundStrokePlayer.vue'

const activeTab = ref('HIRAGANA')
const loading   = ref(true)
const hiragana  = ref([])
const katakana  = ref([])
const selected  = ref(null)

const currentKana = computed(() =>
  activeTab.value === 'HIRAGANA' ? hiragana.value : katakana.value
)

const allKana = computed(() => [...hiragana.value, ...katakana.value])



const currentIndex = computed(() =>
  currentKana.value.findIndex(k => k.scId === selected.value?.scId)
)

const prevKana = computed(() =>
  currentIndex.value > 0 ? currentKana.value[currentIndex.value - 1] : null
)

const nextKana = computed(() =>
  currentIndex.value < currentKana.value.length - 1
    ? currentKana.value[currentIndex.value + 1]
    : null
)

const equivalent = computed(() => {
  if (!selected.value) return null
  const targetType = selected.value.scType === 'HIRAGANA' ? 'KATAKANA' : 'HIRAGANA'
  return allKana.value.find(k =>
    k.scType === targetType && k.scRomaji === selected.value.scRomaji
  ) ?? null
})

onMounted(async () => {
  // Promise.allSettled : si un appel rate (500 intermittent), l'autre reste visible
  const [hResult, kResult] = await Promise.allSettled([
    scriptsApi.list({ type: 'HIRAGANA' }),
    scriptsApi.list({ type: 'KATAKANA' }),
  ])
  hiragana.value = hResult.status === 'fulfilled' ? (hResult.value ?? []) : []
  katakana.value = kResult.status === 'fulfilled' ? (kResult.value ?? []) : []
  if (hResult.status === 'rejected') console.warn('Erreur chargement hiragana', hResult.reason)
  if (kResult.status === 'rejected') console.warn('Erreur chargement katakana', kResult.reason)
  loading.value = false
})

function select(kana) {
  selected.value = kana
}

function switchTab(tab) {
  activeTab.value = tab
  selected.value  = null
}

function selectPrev() { if (prevKana.value) selected.value = prevKana.value }
function selectNext() { if (nextKana.value) selected.value = nextKana.value }
</script>

<style scoped>
.page-header { margin-bottom: 2rem; }
.page-eyebrow { font-family: var(--font-jp); font-size: 0.9rem; color: var(--vermilion); letter-spacing: 0.1em; margin-bottom: 0.5rem; }
.page-header h1 { font-size: clamp(2rem, 4vw, 3rem); }
.page-lead { color: var(--muted); margin-top: 0.5rem; }


/* Onglets */
.tabs { display: flex; border-bottom: 2px solid var(--paper-mid); margin-bottom: 2rem; }
.tab { display: flex; align-items: center; gap: 0.5rem; padding: 0.75rem 1.5rem; background: none; border: none; border-bottom: 2px solid transparent; margin-bottom: -2px; font-family: var(--font-display); font-size: 1rem; letter-spacing: 0.04em; color: var(--muted); cursor: pointer; transition: all 0.2s; }
.tab:hover { color: var(--ink); }
.tab.active { color: var(--vermilion); border-bottom-color: var(--vermilion); }
.tab-jp { font-family: var(--font-jp); font-size: 1.1rem; }

/* Layout grille + panneau */
.kana-layout {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 1.5rem;
  align-items: start;
}

/* Grille */
.kana-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(85px, 1fr));
  gap: 0.5rem;
}

.kana-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.3rem;
  padding: 1.1rem 0.5rem 0.65rem;
  background: white;
  border: 1px solid var(--paper-mid);
  border-radius: var(--radius);
  cursor: pointer;
  transition: all 0.18s ease;
  color: var(--ink);
}

.kana-card:hover {
  border-color: var(--vermilion);
  transform: translateY(-2px);
  box-shadow: var(--shadow);
  background: #fdf5f5;
}

.kana-card.active {
  border-color: var(--vermilion);
  background: #fdf5f5;
  box-shadow: var(--shadow);
}

.kana-char  { font-size: 1.9rem; line-height: 1; }
.kana-romaji { font-size: 0.68rem; color: var(--muted); letter-spacing: 0.04em; }

/* Panneau latéral */
.kana-panel {
  width: 240px;
  background: white;
  border: 1px solid var(--paper-mid);
  border-radius: var(--radius);
  padding: 1.25rem 1rem 1rem;
  position: sticky;
  top: calc(var(--nav-height) + 1.5rem);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
}

.panel-close {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
  background: none;
  border: none;
  font-size: 1.2rem;
  color: var(--muted);
  cursor: pointer;
  line-height: 1;
  padding: 0.2rem 0.4rem;
  border-radius: var(--radius);
  transition: all 0.15s;
}
.panel-close:hover { color: var(--vermilion); background: var(--paper-dark); }

.panel-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  width: 100%;
}

.panel-romaji {
  font-family: var(--font-display);
  font-size: 1.4rem;
  letter-spacing: 0.08em;
  color: var(--ink);
}

.badge { padding: 0.2rem 0.65rem; border-radius: 3px; font-size: 0.72rem; letter-spacing: 0.08em; font-family: var(--font-display); }
.badge-hiragana { background: #be185d; color: white; }
.badge-katakana { background: #0369a1; color: white; }

.equivalent-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.35rem 0.85rem;
  background: var(--paper-dark);
  border: 1px solid var(--paper-mid);
  border-radius: var(--radius);
  cursor: pointer;
  color: var(--ink);
  transition: all 0.2s;
  font-size: 0.82rem;
}
.equivalent-btn:hover { border-color: var(--vermilion); color: var(--vermilion); }
.equivalent-btn .jp { font-size: 1.2rem; line-height: 1; }
.equivalent-label { font-size: 0.72rem; color: var(--muted); }

/* Navigation */
.panel-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-top: 0.5rem;
  border-top: 1px solid var(--paper-mid);
  gap: 0.5rem;
}

.nav-btn {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.3rem 0.6rem;
  background: white;
  border: 1px solid var(--paper-mid);
  border-radius: var(--radius);
  font-size: 0.8rem;
  cursor: pointer;
  color: var(--ink);
  transition: all 0.15s;
}
.nav-btn:hover:not(:disabled) { border-color: var(--ink); background: var(--paper-dark); }
.nav-btn:disabled { opacity: 0.3; cursor: not-allowed; }
.nav-btn .jp { font-size: 1rem; line-height: 1; }
.nav-pos { font-size: 0.7rem; color: var(--muted); white-space: nowrap; }

/* Transition panneau */
.slide-panel-enter-active, .slide-panel-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.slide-panel-enter-from, .slide-panel-leave-to {
  opacity: 0;
  transform: translateX(12px);
}

@media (max-width: 700px) {
  .kana-layout { grid-template-columns: 1fr; }
  .kana-panel { width: 100%; position: static; }
}
</style>