<template>
  <main class="page">
    <div class="container">

      <RouterLink to="/mots" class="back-link">← Vocabulaire</RouterLink>

      <div v-if="loading" class="loading">読み込み中…</div>

      <div v-else-if="word" class="word-detail">

        <header class="detail-header">
          <div class="detail-main">

            <!-- Titre : chaque kanji est cliquable avec tooltip enrichi -->
            <h1 class="detail-jp jp">
              <template v-for="(ch, i) in wordChars" :key="i">
                <RouterLink
                  v-if="kanjiInfos[ch]"
                  :to="`/kanji/${kanjiInfos[ch].id}`"
                  class="kanji-link"
                  :title="`Voir le kanji ${ch} : ${kanjiInfos[ch].meaning || 'signification inconnue'}`"
                >{{ ch }}</RouterLink>
                <span v-else>{{ ch }}</span>
              </template>
            </h1>

            <div class="detail-reading">{{ word.wordReading }}</div>
            <div class="detail-jlpt" v-if="word.jlptLevel">{{ word.jlptLevel }}</div>
          </div>
          <div class="detail-stamp" aria-hidden="true">語</div>
        </header>

        <!-- Player de tracé du mot entier -->
        <section class="detail-section stroke-section">
          <div class="section-title-row">
            <h2 class="section-title">Tracé du mot</h2>
            <button class="toggle-player-btn" @click="showPlayer = !showPlayer">
              {{ showPlayer ? 'Masquer' : 'Afficher' }}
            </button>
          </div>

          <Transition name="fade-player">
            <div v-if="showPlayer" class="player-wrap">
              <CompoundStrokePlayer
                :characters="word.wordJapanese"
                type="word"
                :key="word.wordJapanese"
              />
            </div>
          </Transition>
        </section>

        <section class="detail-section">
          <h2 class="section-title">Signification</h2>
          <p class="detail-translation">{{ word.wordTranslationEn }}</p>
          <p class="detail-translation-fr" v-if="word.wordTranslationFr">
            {{ word.wordTranslationFr }}
          </p>
        </section>

        <!-- Tags cliquables groupés par type -->
        <section class="detail-section" v-if="word.tags?.length">
          <h2 class="section-title">Informations</h2>
          <div class="tags-groups">
            <div v-for="(group, type) in groupedTags" :key="type" class="tag-group">
              <span class="tag-group-label">{{ typeLabel(type) }}</span>
              <div class="tags-list">
                <RouterLink
                  v-for="t in group"
                  :key="t.tagId"
                  :to="`/mots?tag=${t.tagCode}&tagLabel=${encodeURIComponent(t.tagLabel)}&tagType=${t.tagType}`"
                  :class="['tag', `tag-${t.tagType}`, 'tag-clickable']"
                  :title="`Voir tous les mots : ${t.tagLabel}`"
                >{{ t.tagLabel }}</RouterLink>
              </div>
            </div>
          </div>
        </section>

        <!-- Exemples -->
        <section class="detail-section" v-if="word.examples?.length">
          <h2 class="section-title">
            Exemples de phrases
            <span class="section-count">{{ word.examples.length }}</span>
          </h2>
          <div class="examples">
            <div v-for="(ex, i) in word.examples" :key="i" class="example">
              <p class="example-jp jp">{{ ex.japanese }}</p>
              <p class="example-en">{{ ex.english }}</p>
              <a
                v-if="ex.tatoebaId"
                :href="`https://tatoeba.org/fr/sentences/show/${ex.tatoebaId}`"
                target="_blank"
                class="example-source"
              >Tatoeba #{{ ex.tatoebaId }} ↗</a>
            </div>
          </div>
        </section>

      </div>

      <div v-else class="empty">
        <span class="jp">無</span>
        Mot introuvable
      </div>

    </div>
  </main>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { wordsApi, kanjiApi } from '../api'
import CompoundStrokePlayer from '../components/CompoundStrokePlayer.vue'

const route   = useRoute()
const word    = ref(null)
const loading = ref(true)

const showPlayer = ref(false)

// Stockage des informations des kanji (id + signification)
const kanjiInfos = ref({})

// ── Détection kanji (même logique que CompoundStrokePlayer) ───────────────
function isKanji(char) {
  const cp = char.codePointAt(0)
  return (cp >= 0x4E00 && cp <= 0x9FFF)
      || (cp >= 0x3400 && cp <= 0x4DBF)
      || (cp >= 0x20000 && cp <= 0x2A6DF)
      || (cp >= 0xF900 && cp <= 0xFAFF)
}

// ── Chargement ────────────────────────────────────────────────────────────
onMounted(async () => {
  try {
    word.value = await wordsApi.get(route.params.id)

    // Extraire les kanji uniques du mot
    const uniqueKanji = [...new Set([...word.value.wordJapanese].filter(isKanji))]

    // Charger les informations (id et signification) de chaque kanji
    const results = await Promise.allSettled(
      uniqueKanji.map(char => kanjiApi.getByCharacter(char))
    )

    results.forEach((res, i) => {
      if (res.status === 'fulfilled' && res.value?.kanjiId) {
        kanjiInfos.value[uniqueKanji[i]] = {
          id: res.value.kanjiId,
          meaning: res.value.kanjiMeaningEnglish || ''
        }
      }
    })
  } finally {
    loading.value = false
  }
})

// Découpage du mot en caractères individuels pour le template
const wordChars = computed(() =>
  word.value ? [...word.value.wordJapanese] : []
)

// ── Tags ──────────────────────────────────────────────────────────────────
const groupedTags = computed(() => {
  if (!word.value?.tags) return {}
  const order = ['pos', 'field', 'misc', 'dial', 'ke_inf']
  const groups = word.value.tags.reduce((acc, t) => {
    if (!acc[t.tagType]) acc[t.tagType] = []
    acc[t.tagType].push(t)
    return acc
  }, {})
  return Object.fromEntries(
    order.filter(k => groups[k]).map(k => [k, groups[k]])
  )
})

function typeLabel(type) {
  return {
    pos:    'Nature',
    field:  'Thème',
    misc:   'Registre',
    dial:   'Dialecte',
    ke_inf: 'Orthographe'
  }[type] || type
}
</script>

<style scoped>
/* Styles inchangés */
.back-link {
  display: inline-block;
  margin-bottom: 2rem;
  font-family: var(--font-display);
  font-size: 0.85rem;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--muted);
  text-decoration: none;
  transition: color 0.2s;
}
.back-link:hover { color: var(--ink); }

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 3rem;
  padding-bottom: 2rem;
  border-bottom: 1px solid var(--paper-mid);
}
.detail-jp {
  font-size: clamp(3rem, 8vw, 6rem);
  font-weight: 300;
  line-height: 1;
  margin-bottom: 0.5rem;
  display: flex;
  flex-wrap: wrap;
  gap: 0;
}

/* Kanji cliquables dans le titre */
.kanji-link {
  color: inherit;
  text-decoration: none;
  border-bottom: 2px solid transparent;
  transition: color 0.18s, border-color 0.18s;
  line-height: 1;
}
.kanji-link:hover {
  color: var(--vermilion);
  border-bottom-color: var(--vermilion);
}

.detail-reading { font-size: 1rem; color: var(--muted); margin-bottom: 0.75rem; letter-spacing: 0.06em; }
.detail-jlpt {
  display: inline-block;
  padding: 0.2rem 0.8rem;
  background: var(--vermilion);
  color: white;
  font-size: 0.8rem;
  letter-spacing: 0.1em;
  border-radius: 2px;
  font-family: var(--font-display);
}
.detail-stamp {
  font-family: var(--font-jp);
  font-size: 8rem;
  color: var(--vermilion);
  opacity: 0.06;
  line-height: 1;
  font-weight: 600;
  user-select: none;
}

.detail-section { margin-bottom: 2.5rem; }

.section-title {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.75rem;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  color: var(--muted);
  margin-bottom: 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--paper-mid);
}
.section-count {
  background: var(--paper-dark);
  border-radius: 20px;
  padding: 0.1rem 0.5rem;
  font-size: 0.7rem;
}

/* Section player */
.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid var(--paper-mid);
}
.section-title-row .section-title {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}
.toggle-player-btn {
  font-family: var(--font-display);
  font-size: 0.75rem;
  letter-spacing: 0.08em;
  padding: 0.25rem 0.75rem;
  background: white;
  border: 1px solid var(--paper-mid);
  border-radius: var(--radius);
  color: var(--muted);
  cursor: pointer;
  transition: all 0.2s;
}
.toggle-player-btn:hover {
  border-color: var(--vermilion);
  color: var(--vermilion);
}

.player-wrap {
  display: flex;
  justify-content: flex-start;
  padding: 1.25rem;
  background: var(--paper-dark);
  border: 1px solid var(--paper-mid);
  border-radius: var(--radius);
}

/* Transition affichage/masquage player */
.fade-player-enter-active,
.fade-player-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.fade-player-enter-from,
.fade-player-leave-to { opacity: 0; transform: translateY(-6px); }

.detail-translation { font-size: 1.1rem; line-height: 1.8; }
.detail-translation-fr { margin-top: 0.5rem; font-size: 1rem; color: var(--ink-light); font-style: italic; }

.tags-groups { display: flex; flex-direction: column; gap: 1rem; }
.tag-group { display: flex; align-items: flex-start; gap: 1rem; flex-wrap: wrap; }
.tag-group-label {
  font-size: 0.72rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--muted);
  width: 100px;
  flex-shrink: 0;
  padding-top: 0.25rem;
}
.tags-list { display: flex; flex-wrap: wrap; gap: 0.5rem; }

.tag-clickable {
  text-decoration: none;
  transition: all 0.2s ease;
  cursor: pointer;
}
.tag-clickable:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  filter: brightness(0.92);
}

.examples { display: flex; flex-direction: column; gap: 1.5rem; }
.example {
  padding: 1.5rem;
  background: white;
  border: 1px solid var(--paper-mid);
  border-left: 3px solid var(--vermilion);
  border-radius: var(--radius);
}
.example-jp { font-size: 1.1rem; margin-bottom: 0.5rem; line-height: 1.8; }
.example-en { color: var(--ink-light); font-size: 0.95rem; font-style: italic; margin-bottom: 0.5rem; }
.example-source { font-size: 0.72rem; color: var(--muted); text-decoration: none; letter-spacing: 0.05em; }
.example-source:hover { color: var(--vermilion); }
</style>