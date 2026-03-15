<template>
  <main class="page">
    <div class="container">

      <header class="page-header">
        <p class="page-eyebrow">文字</p>
        <h1>Hiragana & Katakana</h1>
        <p class="page-lead">Les deux syllabaires fondamentaux du japonais · {{ total }} kana</p>
      </header>

      <div class="tabs">
        <button
          v-for="t in tabs"
          :key="t.value"
          :class="['tab', { active: activeTab === t.value }]"
          @click="activeTab = t.value; selected = null"
        >{{ t.label }}</button>
      </div>

      <div v-if="loading" class="loading">読み込み中…</div>

      <div v-else>
        <div class="script-grid">
          <div
            v-for="s in filtered"
            :key="s.scId"
            class="script-card"
            :class="{ selected: selected?.scId === s.scId }"
            @click="selected = selected?.scId === s.scId ? null : s"
          >
            <span class="script-char jp">{{ s.scCharacter }}</span>
            <span class="script-romaji">{{ s.scRomaji }}</span>
          </div>
        </div>

        <Transition name="slide">
          <div v-if="selected" class="script-detail">
            <button class="detail-close" @click="selected = null">×</button>
            <div class="detail-char jp">{{ selected.scCharacter }}</div>
            <div class="detail-romaji">{{ selected.scRomaji }}</div>
            <div class="detail-type">{{ selected.scType === 'HIRAGANA' ? 'Hiragana' : 'Katakana' }}</div>
          </div>
        </Transition>
      </div>

    </div>
  </main>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { scriptsApi } from '../api'

const scripts   = ref([])
const loading   = ref(true)
const activeTab = ref('HIRAGANA')
const selected  = ref(null)

const tabs = [
  { value: 'HIRAGANA', label: 'Hiragana　あ' },
  { value: 'KATAKANA', label: 'Katakana　ア' },
]

// L'API retourne {content: [...]} ou un tableau direct — on gère les deux
const filtered = computed(() =>
  scripts.value.filter(s => s.scType === activeTab.value)
)

const total = computed(() => scripts.value.length)

onMounted(async () => {
  try {
    const data = await scriptsApi.list()
    // L'API /scripts retourne un objet paginé {content: [...]}
    scripts.value = Array.isArray(data) ? data : (data.content ?? [])
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.page-header { margin-bottom: 2rem; }
.page-eyebrow { font-family: var(--font-jp); font-size: 0.9rem; color: var(--vermilion); letter-spacing: 0.1em; margin-bottom: 0.5rem; }
.page-header h1 { font-size: clamp(2rem, 4vw, 3rem); }
.page-lead { color: var(--muted); margin-top: 0.5rem; }

.tabs {
  display: flex;
  margin-bottom: 2rem;
  border-bottom: 2px solid var(--paper-mid);
}
.tab {
  padding: 0.75rem 2rem;
  background: none;
  border: none;
  font-family: var(--font-display);
  font-size: 1rem;
  letter-spacing: 0.06em;
  color: var(--muted);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: all 0.2s;
}
.tab:hover { color: var(--ink); }
.tab.active { color: var(--vermilion); border-bottom-color: var(--vermilion); }

.script-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 0.75rem;
}
.script-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  padding: 1.25rem 0.5rem;
  background: white;
  border: 1px solid var(--paper-mid);
  border-radius: var(--radius);
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}
.script-card:hover { border-color: var(--vermilion); transform: translateY(-2px); box-shadow: var(--shadow); }
.script-card.selected { border-color: var(--vermilion); background: #fdf5f5; }
.script-char { font-size: 2.2rem; line-height: 1; }
.script-romaji { font-size: 0.75rem; color: var(--muted); letter-spacing: 0.06em; }

.script-detail {
  position: fixed;
  right: 2rem;
  top: 50%;
  transform: translateY(-50%);
  background: white;
  border: 1px solid var(--paper-mid);
  border-left: 3px solid var(--vermilion);
  border-radius: var(--radius);
  padding: 2.5rem 3rem;
  box-shadow: var(--shadow-lg);
  text-align: center;
  z-index: 50;
  min-width: 180px;
}
.detail-close {
  position: absolute;
  top: 0.75rem; right: 1rem;
  background: none; border: none;
  font-size: 1.2rem; color: var(--muted);
  cursor: pointer; line-height: 1;
}
.detail-char { font-size: 5rem; line-height: 1; margin-bottom: 0.75rem; }
.detail-romaji { font-family: var(--font-display); font-size: 1.5rem; color: var(--vermilion); margin-bottom: 0.5rem; }
.detail-type { font-size: 0.75rem; letter-spacing: 0.1em; text-transform: uppercase; color: var(--muted); }

.slide-enter-active, .slide-leave-active { transition: all 0.3s ease; }
.slide-enter-from, .slide-leave-to { opacity: 0; transform: translateY(-50%) translateX(20px); }

@media (max-width: 768px) {
  .script-detail { position: fixed; right: 1rem; left: 1rem; top: auto; bottom: 2rem; transform: none; }
  .slide-enter-from, .slide-leave-to { transform: translateY(20px); }
}
</style>
