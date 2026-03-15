<template>
  <main class="page">
    <div class="container">

      <RouterLink to="/mots" class="back-link">← Vocabulaire</RouterLink>

      <div v-if="loading" class="loading">読み込み中…</div>

      <div v-else-if="word" class="word-detail">

        <header class="detail-header">
          <div class="detail-main">
            <h1 class="detail-jp jp">{{ word.wordJapanese }}</h1>
            <div class="detail-reading">{{ word.wordReading }}</div>
            <div class="detail-jlpt" v-if="word.jlptLevel">{{ word.jlptLevel }}</div>
          </div>
          <div class="detail-stamp" aria-hidden="true">語</div>
        </header>

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
import { wordsApi } from '../api'

const route   = useRoute()
const word    = ref(null)
const loading = ref(true)

onMounted(async () => {
  try {
    word.value = await wordsApi.get(route.params.id)
  } finally {
    loading.value = false
  }
})

const groupedTags = computed(() => {
  if (!word.value?.tags) return {}
  // Ordre d'affichage souhaité
  const order = ['pos', 'field', 'misc', 'dial', 'ke_inf']
  const groups = word.value.tags.reduce((acc, t) => {
    if (!acc[t.tagType]) acc[t.tagType] = []
    acc[t.tagType].push(t)
    return acc
  }, {})
  // Retrier selon l'ordre défini
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
.detail-jp { font-size: clamp(3rem, 8vw, 6rem); font-weight: 300; line-height: 1; margin-bottom: 0.5rem; }
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

/* Tags cliquables */
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

/* Exemples */
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
