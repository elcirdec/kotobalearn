<template>
  <main class="page">
    <div class="container">

      <RouterLink to="/scripts" class="back-link">← Kana</RouterLink>

      <div v-if="loading" class="loading">読み込み中…</div>

      <div v-else-if="!kana" class="empty">
        <span class="jp">無</span>Kana introuvable
      </div>

      <div v-else class="kana-detail">

        <header class="detail-header">

          <div class="header-left">
            <!-- Player unifié : kana simple ou composé (contrôles unifiés) -->
            <CompoundStrokePlayer
              :characters="kana.scCharacter"
              type="kana"
              :key="kana.scId"
            />
          </div>

          <div class="header-right">

            <div class="header-badges">
              <span :class="['badge', kana.scType === 'HIRAGANA' ? 'badge-hiragana' : 'badge-katakana']">
                {{ kana.scType === 'HIRAGANA' ? 'Hiragana · ひらがな' : 'Katakana · カタカナ' }}
              </span>
            </div>

            <h1 class="kana-char jp">{{ kana.scCharacter }}</h1>
            <p class="kana-romaji">{{ kana.scRomaji }}</p>

            <!-- Info pédagogique -->
            <div class="kana-info">
              <div class="tooltip-wrap" v-if="kana.scType === 'HIRAGANA'">
                <div class="info-block">
                  <span class="info-label">Système</span>
                  <span class="info-value">Hiragana (ひらがな)</span>
                </div>
                <span class="tooltip-box">
                  <strong>Hiragana (ひらがな)</strong><br>
                  Syllabaire japonais natif. Utilisé pour les mots japonais,
                  les terminaisons grammaticales (okurigana) et les furigana
                  (prononciation des kanji). Appris en premier à l'école.
                </span>
              </div>
              <div class="tooltip-wrap" v-else>
                <div class="info-block">
                  <span class="info-label">Système</span>
                  <span class="info-value">Katakana (カタカナ)</span>
                </div>
                <span class="tooltip-box">
                  <strong>Katakana (カタカナ)</strong><br>
                  Syllabaire japonais utilisé principalement pour les mots
                  d'emprunt étrangers (gairaigo), les noms étrangers,
                  les onomatopées et les termes scientifiques.
                </span>
              </div>

              <div class="info-block" v-if="equivalent">
                <span class="info-label">Équivalent</span>
                <RouterLink :to="`/scripts/${equivalent.scId}`" class="equivalent-link">
                  <span class="jp">{{ equivalent.scCharacter }}</span>
                  <span class="equivalent-label">
                    {{ kana.scType === 'HIRAGANA' ? 'Katakana' : 'Hiragana' }}
                  </span>
                </RouterLink>
              </div>
            </div>

          </div>
        </header>

        <!-- Navigation kana précédent / suivant -->
        <nav class="kana-nav" v-if="siblings.length > 0">
          <RouterLink v-if="prevKana" :to="`/scripts/${prevKana.scId}`" class="kana-nav-btn">
            ← <span class="jp">{{ prevKana.scCharacter }}</span> {{ prevKana.scRomaji }}
          </RouterLink>
          <span v-else class="kana-nav-btn disabled">←</span>

          <span class="kana-nav-pos">{{ currentIndex + 1 }} / {{ siblings.length }}</span>

          <RouterLink v-if="nextKana" :to="`/scripts/${nextKana.scId}`" class="kana-nav-btn">
            <span class="jp">{{ nextKana.scCharacter }}</span> {{ nextKana.scRomaji }} →
          </RouterLink>
          <span v-else class="kana-nav-btn disabled">→</span>
        </nav>

      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { scriptsApi } from '../api'
import CompoundStrokePlayer from '../components/CompoundStrokePlayer.vue'

const route   = useRoute()
const kana    = ref(null)
const loading = ref(true)
const allKana = ref([])



onMounted(async () => {
  // Promise.allSettled : si un appel rate, l'autre reste disponible
  const [hResult, kResult] = await Promise.allSettled([
    scriptsApi.list({ type: 'HIRAGANA' }),
    scriptsApi.list({ type: 'KATAKANA' }),
  ])
  const hiragana = hResult.status === 'fulfilled' ? (hResult.value ?? []) : []
  const katakana = kResult.status === 'fulfilled' ? (kResult.value ?? []) : []
  if (hResult.status === 'rejected') console.warn('Erreur chargement hiragana', hResult.reason)
  if (kResult.status === 'rejected') console.warn('Erreur chargement katakana', kResult.reason)
  allKana.value = [...hiragana, ...katakana]
  await loadKana()
})

watch(() => route.params.id, loadKana)

async function loadKana() {
  loading.value = true
  try {
    const id   = Number(route.params.id)
    kana.value = allKana.value.find(k => k.scId === id) ?? null
  } finally {
    loading.value = false
  }
}

// Kana du même type (pour navigation)
const siblings = computed(() =>
  kana.value ? allKana.value.filter(k => k.scType === kana.value.scType) : []
)

const currentIndex = computed(() =>
  siblings.value.findIndex(k => k.scId === kana.value?.scId)
)

const prevKana = computed(() =>
  currentIndex.value > 0 ? siblings.value[currentIndex.value - 1] : null
)

const nextKana = computed(() =>
  currentIndex.value < siblings.value.length - 1
    ? siblings.value[currentIndex.value + 1]
    : null
)

// Trouver l'équivalent (même romaji, autre type)
const equivalent = computed(() => {
  if (!kana.value) return null
  const targetType = kana.value.scType === 'HIRAGANA' ? 'KATAKANA' : 'HIRAGANA'
  return allKana.value.find(k =>
    k.scType === targetType && k.scRomaji === kana.value.scRomaji
  ) ?? null
})
</script>

<style scoped>
.back-link { display: inline-block; margin-bottom: 2rem; font-family: var(--font-display); font-size: 0.85rem; letter-spacing: 0.06em; text-transform: uppercase; color: var(--muted); text-decoration: none; transition: color 0.2s; }
.back-link:hover { color: var(--ink); }


.detail-header { display: grid; grid-template-columns: auto 1fr; gap: 3rem; align-items: flex-start; margin-bottom: 3rem; padding-bottom: 2.5rem; border-bottom: 1px solid var(--paper-mid); }

.header-badges { display: flex; gap: 0.5rem; margin-bottom: 1rem; }
.badge { padding: 0.25rem 0.75rem; border-radius: 3px; font-size: 0.75rem; letter-spacing: 0.08em; font-family: var(--font-display); }
.badge-hiragana { background: #be185d; color: white; }
.badge-katakana { background: #0369a1; color: white; }

.kana-char { font-size: clamp(4rem, 10vw, 7rem); line-height: 1; margin-bottom: 0.5rem; }
.kana-romaji { font-size: 1.5rem; color: var(--muted); font-family: var(--font-display); letter-spacing: 0.08em; margin-bottom: 1.5rem; }

.kana-info { display: flex; flex-direction: column; gap: 0.75rem; }
.info-block { display: flex; align-items: center; gap: 0.75rem; }
.info-label { font-size: 0.68rem; letter-spacing: 0.12em; text-transform: uppercase; color: var(--muted); min-width: 80px; }
.info-value { font-size: 0.9rem; color: var(--ink); }

.equivalent-link { display: flex; align-items: center; gap: 0.5rem; text-decoration: none; color: var(--ink); background: var(--paper-dark); border: 1px solid var(--paper-mid); border-radius: var(--radius); padding: 0.3rem 0.75rem; transition: all 0.2s; }
.equivalent-link:hover { border-color: var(--vermilion); color: var(--vermilion); }
.equivalent-link .jp { font-size: 1.2rem; }
.equivalent-label { font-size: 0.75rem; color: var(--muted); }

.tooltip-wrap { position: relative; display: inline-flex; }
.tooltip-box { display: none; position: absolute; bottom: calc(100% + 8px); left: 0; background: var(--ink); color: var(--paper); font-family: var(--font-body); font-size: 0.78rem; line-height: 1.6; padding: 0.75rem 1rem; border-radius: var(--radius); width: 260px; z-index: 100; box-shadow: var(--shadow-lg); pointer-events: none; text-align: left; white-space: normal; }
.tooltip-box::after { content: ''; position: absolute; top: 100%; left: 20px; border: 6px solid transparent; border-top-color: var(--ink); }
.tooltip-wrap:hover .tooltip-box { display: block; }

/* Navigation kana */
.kana-nav { display: flex; align-items: center; justify-content: space-between; padding: 1rem 0; border-top: 1px solid var(--paper-mid); margin-top: 1rem; }
.kana-nav-btn { display: flex; align-items: center; gap: 0.5rem; padding: 0.5rem 1rem; text-decoration: none; color: var(--ink); background: white; border: 1px solid var(--paper-mid); border-radius: var(--radius); font-size: 0.85rem; transition: all 0.2s; }
.kana-nav-btn:hover:not(.disabled) { border-color: var(--ink); background: var(--paper-dark); }
.kana-nav-btn.disabled { opacity: 0.3; cursor: default; }
.kana-nav-btn .jp { font-size: 1.2rem; line-height: 1; }
.kana-nav-pos { font-size: 0.8rem; color: var(--muted); }

@media (max-width: 700px) {
  .detail-header { grid-template-columns: 1fr; }
  .kana-nav { flex-wrap: wrap; gap: 0.5rem; }
}
</style>