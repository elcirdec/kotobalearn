<template>
  <div class="stroke-player">
    <div class="svg-row" :class="`chars-${charCount}`">
      <div v-if="isLoading" class="svg-placeholder">
        <span class="jp placeholder-char">{{ props.characters }}</span>
      </div>

      <template v-else>
        <div v-for="(cd, ci) in chars" :key="ci" class="svg-wrap" :class="{ 'svg-wrap--error': cd.missing }">
          <!-- Caractère non traceable (ponctuation, espace…) : affichage texte -->
          <div v-if="cd.missing" class="svg-text-char jp">{{ cd.char }}</div>

          <svg v-else :viewBox="cd.viewBox" xmlns="http://www.w3.org/2000/svg" class="svg-canvas">
            <g :transform="cd.groupTransform">
              <template v-for="(group, gIdx) in cd.strokeGroups" :key="`g-${ci}-${gIdx}`">
                <path
                  v-for="(d, sIdx) in group.fills"
                  :key="`f-${ci}-${gIdx}-${sIdx}`"
                  :d="d"
                  :fill="segFill(ci, gIdx, sIdx)"
                />
              </template>
            </g>
            <g v-if="anim.charIdx === ci && anim.groupIdx >= 0" :transform="cd.groupTransform">
              <path
                :d="cd.strokeGroups[anim.groupIdx]?.medians[anim.segIdx] ?? ''"
                stroke="var(--vermilion)"
                stroke-width="16"
                fill="none"
                stroke-linecap="round"
                stroke-linejoin="round"
                :style="{ strokeDasharray: animDashArray, strokeDashoffset: animDashOffset }"
              />
            </g>
          </svg>
        </div>
      </template>
    </div>

    <div class="controls" v-if="!isLoading && totalStrokes > 0">
      <button class="ctrl-btn" @click="goToFirst" :disabled="currentStroke === 0 && currentSubSeg === 0">⏮</button>
      <button class="ctrl-btn" @click="prevStroke" :disabled="currentStroke === 0 && currentSubSeg === 0">◀</button>
      <button class="ctrl-btn ctrl-play" @click="togglePlay">{{ isPlaying ? '⏸' : '▶' }}</button>
      <button class="ctrl-btn" @click="nextStroke" :disabled="currentStroke >= totalStrokes">▶</button>
      <button class="ctrl-btn" @click="goToLast"   :disabled="currentStroke >= totalStrokes">⏭</button>
    </div>

    <div class="stroke-counter" v-if="!isLoading && totalStrokes > 0">
      {{ currentStroke }} / {{ totalStrokes }} trait{{ totalStrokes > 1 ? 's' : '' }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onUnmounted } from 'vue'

const props = defineProps({
  characters: { type: String, required: true },
  // 'kana' | 'kanji' | 'word'
  // 'word' = détection automatique par caractère (kanji + kana mélangés)
  type: { type: String, default: 'kanji' },
})

const chars     = ref([])
const isLoading = ref(true)

const currentStroke  = ref(0)
const currentSubSeg  = ref(0)
const isPlaying      = ref(false)
const animDashArray  = ref(0)
const animDashOffset = ref(0)
const anim = ref({ charIdx: -1, groupIdx: -1, segIdx: -1 })

let playTimer = null
let animFrame = null

// ── Détection du type de caractère ────────────────────────────────────────

function isKanjiChar(char) {
  const cp = char.codePointAt(0)
  return (cp >= 0x4E00 && cp <= 0x9FFF)    // CJK Unified Ideographs (bloc principal)
      || (cp >= 0x3400 && cp <= 0x4DBF)    // CJK Extension A
      || (cp >= 0x20000 && cp <= 0x2A6DF)  // CJK Extension B
      || (cp >= 0xF900 && cp <= 0xFAFF)    // CJK Compatibility Ideographs
}

function isKanaChar(char) {
  const cp = char.codePointAt(0)
  return (cp >= 0x3040 && cp <= 0x309F)   // Hiragana
      || (cp >= 0x30A0 && cp <= 0x30FF)   // Katakana
}

function isAnimatable(char) {
  return isKanjiChar(char) || isKanaChar(char)
}

function charFolder(char) {
  if (isKanaChar(char)) return { folder: 'kana', subFolder: 'svgsJaKana' }
  return { folder: 'kanji', subFolder: 'svgsJa' }
}

// En mode type fixe ('kana' ou 'kanji'), on utilise le dossier déclaré
function getFolder(char) {
  if (props.type === 'word') return charFolder(char)
  const folder    = props.type === 'kana' ? 'kana'       : 'kanji'
  const subFolder = props.type === 'kana' ? 'svgsJaKana' : 'svgsJa'
  return { folder, subFolder }
}

// ── Chargement ────────────────────────────────────────────────────────────

const charCount = computed(() => [...props.characters].length || 1)

function charToFilename(char) {
  return char.codePointAt(0).toString(10) + '.svg'
}

async function loadAll(characters) {
  isLoading.value     = true
  stopPlay()
  chars.value         = []
  currentStroke.value = 0
  currentSubSeg.value = 0

  const charList = [...characters]

  // En mode 'word', on ne tente de charger que les caractères animatables
  // Les autres (ponctuation, chiffres, espace…) sont affichés en texte
  const results = await Promise.allSettled(
    charList.map(char => {
      if (props.type === 'word' && !isAnimatable(char)) {
        // Résolution immédiate avec marqueur "manquant"
        return Promise.resolve({ missing: true, char })
      }
      const { folder, subFolder } = getFolder(char)
      return fetch(`/animcjk/${folder}/${subFolder}/${charToFilename(char)}`)
        .then(r => { if (!r.ok) throw new Error(`HTTP ${r.status}`); return r.text() })
    })
  )

  chars.value = results.map((res, i) => {
    // Caractère non-animatable (résolution immédiate avec {missing,char})
    if (res.status === 'fulfilled' && res.value?.missing) {
      return { missing: true, char: res.value.char, viewBox: '', groupTransform: '', strokeGroups: [] }
    }
    if (res.status === 'rejected') {
      console.warn(`[CompoundStrokePlayer] SVG manquant : ${charList[i]}`, res.reason)
      return { missing: false, char: charList[i], viewBox: '0 0 109 109', groupTransform: '', strokeGroups: [] }
    }
    return { missing: false, char: charList[i], ...parseSvg(res.value) }
  })

  currentStroke.value = totalStrokes.value
  isLoading.value = false
}

function parseSvg(svgText) {
  const doc   = new DOMParser().parseFromString(svgText, 'image/svg+xml')
  const svgEl = doc.querySelector('svg')
  const viewBox        = svgEl?.getAttribute('viewBox') ?? '0 0 109 109'
  const groupTransform = svgEl?.querySelector('g')?.getAttribute('transform') ?? ''

  const groupMap = new Map()
  function getGroup(n) {
    if (!groupMap.has(n)) groupMap.set(n, { fills: [], medians: [] })
    return groupMap.get(n)
  }

  Array.from(doc.querySelectorAll('path')).forEach(p => {
    const d  = p.getAttribute('d')
    if (!d || d.length <= 5) return
    const cp = p.getAttribute('clip-path')
    if (cp) {
      const m = cp.match(/c(\d+)/)
      if (m) getGroup(parseInt(m[1])).medians.push(d)
    } else if (p.id) {
      const m = p.id.match(/d(\d+)/)
      if (m) getGroup(parseInt(m[1])).fills.push(d)
    }
  })

  const strokeGroups = [...groupMap.entries()]
    .sort(([a], [b]) => a - b)
    .map(([, g]) => ({ fills: g.fills, medians: g.medians }))
    .filter(g => g.fills.length > 0 && g.medians.length > 0)

  return { viewBox, groupTransform, strokeGroups }
}

// ── Comptage (ne compte que les chars non-manquants) ───────────────────────

const totalStrokes = computed(() =>
  chars.value.reduce((sum, cd) => sum + cd.strokeGroups.length, 0)
)

const cumulative = computed(() => {
  const acc = [0]
  for (const cd of chars.value) acc.push(acc.at(-1) + cd.strokeGroups.length)
  return acc
})

function resolveGroup(globalIdx) {
  for (let ci = 0; ci < chars.value.length; ci++) {
    const local = globalIdx - cumulative.value[ci]
    if (local >= 0 && local < chars.value[ci].strokeGroups.length)
      return { charIdx: ci, groupIdx: local }
  }
  return null
}

function segFill(ci, gIdx, sIdx) {
  const globalPos = cumulative.value[ci] + gIdx
  if (globalPos < currentStroke.value) return 'var(--ink)'
  if (globalPos === currentStroke.value && sIdx < currentSubSeg.value) return 'var(--ink)'
  return '#e8e1d8'
}

// ── Contrôles ─────────────────────────────────────────────────────────────

function goToFirst() { stopPlay(); currentStroke.value = 0; currentSubSeg.value = 0 }
function goToLast()  { stopPlay(); currentStroke.value = totalStrokes.value; currentSubSeg.value = 0 }
function prevStroke() { stopPlay(); if (currentStroke.value > 0) { currentStroke.value--; currentSubSeg.value = 0 } }
function nextStroke() { stopPlay(); if (currentStroke.value < totalStrokes.value) { currentStroke.value++; currentSubSeg.value = 0 } }
function togglePlay() { isPlaying.value ? stopPlay() : startPlay() }

function startPlay() {
  if (currentStroke.value >= totalStrokes.value) { currentStroke.value = 0; currentSubSeg.value = 0 }
  isPlaying.value = true
  scheduleNext()
}

function stopPlay() {
  isPlaying.value = false
  anim.value = { charIdx: -1, groupIdx: -1, segIdx: -1 }
  if (playTimer) { clearTimeout(playTimer);        playTimer = null }
  if (animFrame) { cancelAnimationFrame(animFrame); animFrame = null }
}

// ── Moteur d'animation ────────────────────────────────────────────────────

function scheduleNext() {
  if (!isPlaying.value || currentStroke.value >= totalStrokes.value) {
    isPlaying.value = false
    anim.value = { charIdx: -1, groupIdx: -1, segIdx: -1 }
    return
  }

  const pos = resolveGroup(currentStroke.value)
  if (!pos) { stopPlay(); return }

  const { charIdx, groupIdx } = pos
  const group  = chars.value[charIdx].strokeGroups[groupIdx]
  const segIdx = currentSubSeg.value

  if (segIdx >= group.medians.length) {
    currentStroke.value++
    currentSubSeg.value = 0
    anim.value = { charIdx: -1, groupIdx: -1, segIdx: -1 }
    if (isPlaying.value) playTimer = setTimeout(scheduleNext, 150)
    return
  }

  const d   = group.medians[segIdx]
  const tmp = document.createElementNS('http://www.w3.org/2000/svg', 'path')
  tmp.setAttribute('d', d)
  document.body.appendChild(tmp)
  const pathLen = Math.max(tmp.getTotalLength(), 1)
  document.body.removeChild(tmp)

  anim.value           = { charIdx, groupIdx, segIdx }
  animDashArray.value  = pathLen
  animDashOffset.value = pathLen

  const duration  = Math.min(Math.max(pathLen * 4, 300), 1200)
  const startTime = performance.now()

  function frame(now) {
    const t     = Math.min((now - startTime) / duration, 1)
    const eased = 1 - Math.pow(1 - t, 2)
    animDashOffset.value = pathLen * (1 - eased)
    if (t < 1) {
      animFrame = requestAnimationFrame(frame)
    } else {
      currentSubSeg.value++
      anim.value = { charIdx: -1, groupIdx: -1, segIdx: -1 }
      if (isPlaying.value) {
        const isLastSeg = currentSubSeg.value >= group.medians.length
        playTimer = setTimeout(scheduleNext, isLastSeg ? 150 : 0)
      }
    }
  }
  animFrame = requestAnimationFrame(frame)
}

watch(() => props.characters, val => { if (val) loadAll(val) }, { immediate: true })
onUnmounted(() => stopPlay())
</script>

<style scoped>
.stroke-player { display: flex; flex-direction: column; align-items: center; gap: 0.5rem; }

.svg-row {
  display: flex;
  background: white;
  border: 1px solid var(--paper-mid);
  border-radius: var(--radius);
  overflow: hidden;
  flex-wrap: wrap;   /* les mots longs passent à la ligne si nécessaire */
}

/* Tailles fixes pour kana/kanji simples */
.svg-row.chars-1 .svg-wrap { width: 200px; height: 200px; }
.svg-row.chars-2 .svg-wrap { width: 107px; height: 107px; }
.svg-row.chars-3 .svg-wrap { width:  80px; height:  80px; }

/* Mode word : taille uniforme quel que soit le nombre de caractères */
.svg-row.chars-4  .svg-wrap,
.svg-row.chars-5  .svg-wrap,
.svg-row.chars-6  .svg-wrap,
.svg-row.chars-7  .svg-wrap,
.svg-row.chars-8  .svg-wrap,
.svg-row.chars-9  .svg-wrap,
.svg-row.chars-10 .svg-wrap { width: 80px; height: 80px; }

.svg-wrap { position: relative; flex-shrink: 0; }
.svg-wrap + .svg-wrap { border-left: 1px solid var(--paper-mid); }
.svg-canvas { width: 100%; height: 100%; display: block; }

/* Caractère non-animatable (ponctuation, etc.) */
.svg-wrap--error {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 80px;
  background: var(--paper-dark);
}
.svg-text-char {
  font-size: 1.4rem;
  color: var(--muted);
  line-height: 1;
}

.svg-placeholder {
  display: flex; align-items: center; justify-content: center;
  min-width: 200px; height: 200px;
}
.placeholder-char { font-size: 4rem; line-height: 1; color: var(--paper-mid); }

.controls { display: flex; gap: 0.35rem; align-items: center; }
.ctrl-btn {
  width: 34px; height: 34px;
  display: flex; align-items: center; justify-content: center;
  background: white; border: 1.5px solid var(--paper-mid);
  border-radius: var(--radius); font-size: 0.8rem;
  cursor: pointer; color: var(--ink); transition: all 0.15s; line-height: 1;
}
.ctrl-btn:hover:not(:disabled) { border-color: var(--ink); background: var(--paper-dark); }
.ctrl-btn:disabled { opacity: 0.28; cursor: not-allowed; }
.ctrl-play {
  width: 42px; height: 42px; font-size: 1rem;
  background: var(--ink); color: var(--paper); border-color: var(--ink);
}
.ctrl-play:hover:not(:disabled) { background: var(--vermilion); border-color: var(--vermilion); }
.stroke-counter { font-size: 0.7rem; color: var(--muted); letter-spacing: 0.04em; }
</style>