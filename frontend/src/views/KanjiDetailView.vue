<template>
  <main class="page">
    <div class="container">

      <RouterLink to="/kanji" class="back-link">← Kanji</RouterLink>

      <div v-if="loading" class="loading">読み込み中…</div>

      <div v-else-if="!kanji" class="empty">
        <span class="jp">無</span>Kanji introuvable
      </div>

      <div v-else class="kanji-detail">

        <!-- ═══ EN-TÊTE ═══════════════════════════════════════════════════ -->
        <header class="detail-header">

          <div class="header-left">
            <!-- Tracé animé ou statique -->
            <div class="stroke-container">
              <!-- Mode vidéo -->
              <video
                v-if="kanji.kanjiVideoMp4Url && showVideo"
                class="stroke-video"
                :poster="kanji.kanjiVideoPosterUrl"
                autoplay loop muted playsinline
                :key="'video'"
              >
                <source :src="kanji.kanjiVideoWebmUrl" type="video/webm" />
                <source :src="kanji.kanjiVideoMp4Url"  type="video/mp4"  />
              </video>
              <!-- Mode statique : SVG poster ou grand caractère -->
              <img
                v-else-if="kanji.kanjiVideoPosterUrl && !showVideo"
                :src="kanji.kanjiVideoPosterUrl"
                :alt="kanji.kanjiCharacter"
                class="stroke-img"
              />
              <span v-else class="kanji-char-big jp">{{ kanji.kanjiCharacter }}</span>
            </div>

            <!-- Toggle vidéo (seulement si vidéo disponible) -->
            <div class="stroke-toggle" v-if="kanji.kanjiVideoMp4Url">
              <button
                :class="['toggle-btn', { active: !showVideo }]"
                @click="showVideo = false"
                title="Afficher le tracé statique"
              >Tracé</button>
              <button
                :class="['toggle-btn', { active: showVideo }]"
                @click="showVideo = true"
                title="Afficher l'animation"
              >Animation</button>
            </div>
          </div>

          <div class="header-right">
            <div class="header-badges">
              <span class="badge badge-jlpt"    v-if="kanji.jlptCode">{{ kanji.jlptCode }}</span>
              <span class="badge badge-grade"   v-if="kanji.kanjiGrade">Année {{ kanji.kanjiGrade }}</span>
              <span class="badge badge-strokes">{{ kanji.kanjiStrokes }} trait{{ kanji.kanjiStrokes > 1 ? 's' : '' }}</span>
            </div>

            <h1 class="kanji-meaning">{{ kanji.kanjiMeaningEnglish }}</h1>

            <!-- Radical -->
            <div class="radical" v-if="kanji.radCharacter">
              <span class="radical-char">{{ kanji.radCharacter }}</span>
              <span class="radical-info">
                Radical · {{ kanji.radMeaningEnglish }}
                <span v-if="kanji.radNameRomaji" class="radical-romaji">({{ kanji.radNameRomaji }})</span>
              </span>
            </div>
            <div class="radical radical-missing" v-else>
              <span class="radical-info muted">Radical non disponible pour ce kanji</span>
            </div>
          </div>
        </header>

        <!-- ═══ LECTURES ══════════════════════════════════════════════════ -->
        <section class="detail-section" v-if="kanji.readings?.length">
          <h2 class="section-title">Lectures</h2>
          <div class="readings">

            <div class="reading-group" v-if="onReadings.length">
              <div class="reading-type-wrap">
                <span class="reading-type">音読み · On-yomi</span>
                <!-- Tooltip ? -->
                <span class="tooltip-wrap">
                  <span class="tooltip-icon">?</span>
                  <span class="tooltip-box">
                    <strong>On-yomi (音読み)</strong><br>
                    Lecture sino-japonaise, héritée du chinois.<br>
                    Utilisée surtout dans les mots composés (ex : 学校 → がっ<em>こう</em>).
                  </span>
                </span>
              </div>
              <div class="reading-chips">
                <div v-for="r in onReadings" :key="r.readId" class="reading-chip reading-on">
                  <span class="reading-kana jp">{{ r.readKana }}</span>
                  <span class="reading-romaji">{{ r.readRomaji }}</span>
                </div>
              </div>
            </div>

            <div class="reading-group" v-if="kunReadings.length">
              <div class="reading-type-wrap">
                <span class="reading-type">訓読み · Kun-yomi</span>
                <span class="tooltip-wrap">
                  <span class="tooltip-icon">?</span>
                  <span class="tooltip-box">
                    <strong>Kun-yomi (訓読み)</strong><br>
                    Lecture japonaise native du kanji.<br>
                    Utilisée seul ou avec des hiragana (ex : 食べる → た<em>べる</em>).
                  </span>
                </span>
              </div>
              <div class="reading-chips">
                <div v-for="r in kunReadings" :key="r.readId" class="reading-chip reading-kun">
                  <span class="reading-kana jp">{{ r.readKana }}</span>
                  <span class="reading-romaji">{{ r.readRomaji }}</span>
                </div>
              </div>
            </div>

          </div>
        </section>

        <!-- ═══ MOTS COMPOSÉS ════════════════════════════════════════════ -->
        <section class="detail-section">
          <h2 class="section-title">
            Mots composés
            <span class="section-count" v-if="wordsTotal > 0">{{ wordsTotal.toLocaleString() }}</span>
          </h2>

          <div v-if="wordsLoading" class="loading-sm">Chargement des mots…</div>
          <div v-else-if="words.length === 0" class="empty-sm">Aucun mot trouvé</div>

          <div v-else>
            <div class="words-grid">
              <RouterLink
                v-for="w in words" :key="w.wordId"
                :to="`/mots/${w.wordId}`"
                class="word-card"
              >
                <span class="word-jp jp">{{ w.wordJapanese }}</span>
                <span class="word-reading">{{ w.wordReading }}</span>
                <span class="word-en">{{ truncate(w.wordTranslationEn, 50) }}</span>
              </RouterLink>
            </div>

            <nav class="pagination-simple" v-if="wordsTotalPages > 1">
              <button @click="wordsPage--; loadWords()" :disabled="wordsPage === 0">‹ Préc.</button>
              <span>{{ wordsPage + 1 }} / {{ wordsTotalPages }}</span>
              <button @click="wordsPage++; loadWords()" :disabled="wordsPage >= wordsTotalPages - 1">Suiv. ›</button>
            </nav>
          </div>
        </section>

        <!-- ═══ EXEMPLES KanjiAlive ══════════════════════════════════════ -->
        <section class="detail-section" v-if="kanji.examples?.length">
          <h2 class="section-title">
            Exemples
            <span class="section-count">{{ kanji.examples.length }}</span>
          </h2>
          <div class="examples-grid">
            <div v-for="ex in kanji.examples" :key="ex.exId" class="example-card">
              <span class="example-jp jp">{{ ex.exJapanese }}</span>
              <span class="example-en">{{ ex.exMeaningEnglish }}</span>
              <button
                v-if="ex.exAudioMp3Url"
                class="audio-btn"
                @click="playAudio(ex.exAudioMp3Url)"
                title="Écouter"
              >▶</button>
            </div>
          </div>
        </section>

      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { kanjiApi } from '../api'

const route = useRoute()

const kanji        = ref(null)
const loading      = ref(true)
const showVideo    = ref(false) // par défaut : tracé sans animation

const words           = ref([])
const wordsTotal      = ref(0)
const wordsTotalPages = ref(0)
const wordsPage       = ref(0)
const wordsLoading    = ref(false)

const onReadings  = computed(() => kanji.value?.readings?.filter(r => r.readType === 'ON')  ?? [])
const kunReadings = computed(() => kanji.value?.readings?.filter(r => r.readType === 'KUN') ?? [])

onMounted(async () => {
  try {
    kanji.value = await kanjiApi.get(route.params.id)
    await loadWords()
  } finally {
    loading.value = false
  }
})

async function loadWords() {
  wordsLoading.value = true
  try {
    const data = await kanjiApi.words(route.params.id, { page: wordsPage.value, size: 20 })
    words.value           = data.content ?? []
    wordsTotal.value      = data.totalElements ?? 0
    wordsTotalPages.value = data.totalPages ?? 0
  } catch (e) {
    console.error('Erreur mots composés', e)
  } finally {
    wordsLoading.value = false
  }
}

function playAudio(url) { new Audio(url).play() }

function truncate(str, len = 50) {
  if (!str) return ''
  return str.length > len ? str.slice(0, len) + '…' : str
}
</script>

<style scoped>
.back-link {
  display: inline-block; margin-bottom: 2rem;
  font-family: var(--font-display); font-size: 0.85rem;
  letter-spacing: 0.06em; text-transform: uppercase;
  color: var(--muted); text-decoration: none; transition: color 0.2s;
}
.back-link:hover { color: var(--ink); }

/* ─── En-tête ─────────────────────────────────────────────────────────── */
.detail-header {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 3rem;
  align-items: flex-start;
  margin-bottom: 3rem;
  padding-bottom: 2.5rem;
  border-bottom: 1px solid var(--paper-mid);
}

.stroke-container {
  width: 200px; height: 200px;
  background: white;
  border: 1px solid var(--paper-mid);
  border-radius: var(--radius);
  overflow: hidden;
  display: flex; align-items: center; justify-content: center;
}
.stroke-video, .stroke-img {
  width: 100%; height: 100%; object-fit: contain;
}
.kanji-char-big {
  font-size: 8rem; line-height: 1; color: var(--ink);
}

/* Toggle vidéo/tracé */
.stroke-toggle {
  display: flex;
  margin-top: 0.75rem;
  border: 1.5px solid var(--paper-mid);
  border-radius: 20px;
  overflow: hidden;
  width: 200px;
}
.toggle-btn {
  flex: 1;
  padding: 0.35rem 0;
  background: white;
  border: none;
  font-family: var(--font-display);
  font-size: 0.8rem;
  letter-spacing: 0.04em;
  color: var(--muted);
  cursor: pointer;
  transition: all 0.2s;
}
.toggle-btn.active {
  background: var(--ink);
  color: var(--paper);
}

/* Badges */
.header-badges {
  display: flex; gap: 0.5rem; flex-wrap: wrap; margin-bottom: 1rem;
}
.badge {
  padding: 0.25rem 0.75rem; border-radius: 3px;
  font-size: 0.75rem; letter-spacing: 0.08em;
  font-family: var(--font-display);
}
.badge-jlpt   { background: var(--vermilion); color: white; }
.badge-grade  { background: var(--ink); color: var(--paper); }
.badge-strokes { background: var(--paper-dark); color: var(--ink-light); border: 1px solid var(--paper-mid); }

.kanji-meaning {
  font-size: clamp(1.8rem, 4vw, 3rem);
  font-weight: 300; margin-bottom: 1.5rem;
  text-transform: capitalize;
}

/* Radical — force une police qui couvre les caractères Kangxi Unicode */
.radical {
  display: flex; align-items: center; gap: 1rem;
  padding: 0.75rem 1rem;
  background: var(--paper-dark); border-radius: var(--radius);
  border: 1px solid var(--paper-mid); width: fit-content;
}
.radical-char {
  font-family: 'Noto Sans JP', 'Noto Sans CJK JP', sans-serif;
  font-size: 1.8rem; line-height: 1; color: var(--vermilion);
}
.radical-info { font-size: 0.85rem; color: var(--ink-light); }
.radical-romaji { color: var(--muted); }
.radical-missing { opacity: 0.6; }
.muted { color: var(--muted); font-style: italic; }

/* ─── Sections ───────────────────────────────────────────────────────── */
.detail-section { margin-bottom: 3rem; }
.section-title {
  display: flex; align-items: center; gap: 0.75rem;
  font-size: 0.75rem; letter-spacing: 0.15em; text-transform: uppercase;
  color: var(--muted); margin-bottom: 1.25rem;
  padding-bottom: 0.5rem; border-bottom: 1px solid var(--paper-mid);
}
.section-count {
  background: var(--paper-dark); border-radius: 20px;
  padding: 0.1rem 0.5rem; font-size: 0.7rem;
}

/* ─── Lectures ───────────────────────────────────────────────────────── */
.readings { display: flex; flex-direction: column; gap: 1.5rem; }
.reading-group { display: flex; align-items: flex-start; gap: 1.5rem; flex-wrap: wrap; }

.reading-type-wrap {
  display: flex; align-items: center; gap: 0.5rem;
  width: 200px; flex-shrink: 0; padding-top: 0.3rem;
}
.reading-type {
  font-size: 0.75rem; letter-spacing: 0.06em; color: var(--muted);
  white-space: nowrap;
}

/* Tooltip ? */
.tooltip-wrap {
  position: relative; display: inline-flex;
}
.tooltip-icon {
  display: inline-flex; align-items: center; justify-content: center;
  width: 16px; height: 16px; border-radius: 50%;
  border: 1.5px solid var(--muted);
  font-size: 0.65rem; color: var(--muted);
  cursor: help; line-height: 1;
  transition: all 0.2s;
  flex-shrink: 0;
}
.tooltip-icon:hover { border-color: var(--vermilion); color: var(--vermilion); }
.tooltip-box {
  display: none;
  position: absolute;
  bottom: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%);
  background: var(--ink);
  color: var(--paper);
  font-family: var(--font-body);
  font-size: 0.78rem;
  line-height: 1.6;
  padding: 0.75rem 1rem;
  border-radius: var(--radius);
  width: 240px;
  z-index: 100;
  box-shadow: var(--shadow-lg);
  pointer-events: none;
}

.tooltip-box::after {
  content: '';
  position: absolute;
  top: 100%; left: 50%;
  transform: translateX(-50%);
  border: 6px solid transparent;
  border-top-color: var(--ink);
}
.tooltip-wrap:hover .tooltip-box { display: block; }

.reading-chips { display: flex; flex-wrap: wrap; gap: 0.6rem; }
.reading-chip {
  display: flex; flex-direction: column; align-items: center;
  padding: 0.6rem 1.2rem; border-radius: var(--radius);
  border: 1px solid var(--paper-mid); min-width: 64px; text-align: center;
}
.reading-on  { background: #fdf5f5; border-color: #f0c0b8; }
.reading-kun { background: #f5f8fd; border-color: #90aad4; }
.reading-kana  { font-size: 1.4rem; line-height: 1.2; margin-bottom: 0.25rem; }
.reading-romaji { font-size: 0.72rem; color: var(--muted); letter-spacing: 0.06em; }

/* ─── Mots composés ──────────────────────────────────────────────────── */
.words-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 0.75rem;
}
.word-card {
  display: flex; flex-direction: column; gap: 0.25rem;
  padding: 1rem 1.25rem; background: white;
  border: 1px solid var(--paper-mid); border-radius: var(--radius);
  text-decoration: none; color: var(--ink); transition: all 0.2s;
}
.word-card:hover { border-color: var(--ink); transform: translateY(-2px); box-shadow: var(--shadow); }
.word-jp     { font-size: 1.3rem; line-height: 1.2; }
.word-reading { font-size: 0.75rem; color: var(--muted); }
.word-en     { font-size: 0.8rem; color: var(--ink-light); line-height: 1.5; margin-top: 0.2rem; }

.pagination-simple {
  display: flex; align-items: center; justify-content: center;
  gap: 1.5rem; margin-top: 1.5rem;
}
.pagination-simple button {
  padding: 0.4rem 1rem; border: 1px solid var(--paper-mid);
  background: white; border-radius: var(--radius);
  font-family: var(--font-display); font-size: 0.85rem;
  cursor: pointer; transition: all 0.2s;
}
.pagination-simple button:hover:not(:disabled) {
  border-color: var(--ink); background: var(--ink); color: var(--paper);
}
.pagination-simple button:disabled { opacity: 0.35; cursor: not-allowed; }
.pagination-simple span { font-size: 0.85rem; color: var(--muted); }

/* ─── Exemples ───────────────────────────────────────────────────────── */
.examples-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 0.75rem;
}
.example-card {
  padding: 1rem 1.25rem; background: white;
  border: 1px solid var(--paper-mid);
  border-left: 3px solid var(--vermilion);
  border-radius: var(--radius); position: relative;
}
.example-jp { display: block; font-size: 1rem; margin-bottom: 0.4rem; line-height: 1.6; }
.example-en { font-size: 0.82rem; color: var(--ink-light); font-style: italic; }
.audio-btn {
  position: absolute; top: 0.75rem; right: 0.75rem;
  background: var(--paper-dark); border: 1px solid var(--paper-mid);
  border-radius: 50%; width: 28px; height: 28px;
  font-size: 0.7rem; cursor: pointer; color: var(--vermilion);
  transition: all 0.2s;
}
.audio-btn:hover { background: var(--vermilion); color: white; border-color: var(--vermilion); }

.loading-sm { padding: 2rem; text-align: center; color: var(--muted); }
.empty-sm   { padding: 1rem; color: var(--muted); font-style: italic; }

@media (max-width: 700px) {
  .detail-header { grid-template-columns: 1fr; }
  .stroke-container { width: 150px; height: 150px; margin: 0 auto; }
  .stroke-toggle { margin: 0.75rem auto 0; }
  .reading-type-wrap { width: auto; }
  .tooltip-box { left: 0; right: auto; transform: none; }
  .tooltip-box::after { left: 12px; }
}
</style>