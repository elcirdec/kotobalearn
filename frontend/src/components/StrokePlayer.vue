<template>
  <div class="stroke-player">

    <div class="svg-container">
      <div v-if="loading" class="svg-placeholder">
        <span class="jp placeholder-char">{{ character }}</span>
      </div>
      <div v-else-if="error" class="svg-placeholder">
        <span class="jp placeholder-char faded">{{ character }}</span>
        <span class="svg-error-msg">Tracé non disponible</span>
      </div>

      <svg v-else
        :viewBox="svgViewBox"
        xmlns="http://www.w3.org/2000/svg"
        class="svg-canvas"
      >
        <!-- Couche des formes pleines (remplissages) -->
        <g :transform="groupTransform">
          <path
            v-for="(d, i) in fullStrokes"
            :key="'full-' + i"
            :d="d"
            :fill="i < currentStroke ? 'var(--ink)' : '#e8e1d8'"
            transition="fill 0.2s"
          />
        </g>

        <!-- Couche des médians animés (par-dessus) -->
        <g v-if="animatingIndex >= 0" :transform="groupTransform">
          <path
            :d="medianStrokes[animatingIndex]"
            stroke="var(--vermilion)"
            stroke-width="16"
            fill="none"
            stroke-linecap="round"
            stroke-linejoin="round"
            :style="{
              strokeDasharray: animDashArray,
              strokeDashoffset: animDashOffset
            }"
          />
        </g>
      </svg>
    </div>

    <div class="controls" v-if="!error && !loading && medianStrokes.length > 0">
      <button class="ctrl-btn" @click="goToFirst"
        :disabled="currentStroke === 0" title="Premier trait">⏮</button>
      <button class="ctrl-btn" @click="prevStroke"
        :disabled="currentStroke === 0" title="Trait précédent">◀</button>
      <button class="ctrl-btn ctrl-play" @click="togglePlay"
        :title="isPlaying ? 'Pause' : 'Lire'">{{ isPlaying ? '⏸' : '▶' }}</button>
      <button class="ctrl-btn" @click="nextStroke"
        :disabled="currentStroke >= medianStrokes.length" title="Trait suivant">▶</button>
      <button class="ctrl-btn" @click="goToLast"
        :disabled="currentStroke >= medianStrokes.length" title="Tous les traits">⏭</button>
    </div>

    <div class="stroke-counter" v-if="!error && !loading && medianStrokes.length > 0">
      {{ currentStroke }} / {{ medianStrokes.length }} trait{{ medianStrokes.length > 1 ? 's' : '' }}
    </div>

  </div>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue'

const props = defineProps({
  character: { type: String, required: true },
  type:      { type: String, default: 'kanji' }
})

const loading        = ref(true)
const error          = ref(false)
const fullStrokes    = ref([])   // formes pleines (fill)
const medianStrokes  = ref([])   // médians (stroke) pour l'animation
const groupTransform = ref('')
const svgViewBox     = ref('0 0 109 109')
const currentStroke  = ref(0)
const isPlaying      = ref(false)

const animatingIndex = ref(-1)
const animDashArray  = ref(0)
const animDashOffset = ref(0)

let playTimer = null
let animFrame = null

function charToFilename(char) {
  return char.codePointAt(0).toString(10) + '.svg'
}

async function loadSvg(char) {
  loading.value        = true
  error.value          = false
  fullStrokes.value    = []
  medianStrokes.value  = []
  groupTransform.value = ''
  currentStroke.value  = 0
  animatingIndex.value = -1
  stopPlay()

  const folder    = props.type === 'kana' ? 'kana' : 'kanji'
  const subFolder = props.type === 'kana' ? 'svgsJaKana' : 'svgsJa'
  const url       = `/animcjk/${folder}/${subFolder}/${charToFilename(char)}`

  try {
    const res = await fetch(url)
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    const text = await res.text()
    parseSvg(text)
  } catch (e) {
    console.warn(`[StrokePlayer] SVG non trouvé : ${url}`, e)
    error.value = true
  } finally {
    loading.value = false
  }
}

function parseSvg(svgText) {
  const parser = new DOMParser()
  const doc    = parser.parseFromString(svgText, 'image/svg+xml')
  const svgEl  = doc.querySelector('svg')

  const vb = svgEl?.getAttribute('viewBox')
  if (vb) svgViewBox.value = vb

  const mainGroup = svgEl?.querySelector('g')
  if (mainGroup) groupTransform.value = mainGroup.getAttribute('transform') || ''

  const allPaths = Array.from(doc.querySelectorAll('path'))

  // Séparer les chemins en deux catégories
  const fulls = []
  const medians = []

  allPaths.forEach(p => {
    const isMedian = p.getAttribute('clip-path') !== null
    const d = p.getAttribute('d')
    if (d && d.length > 5) {
      if (isMedian) medians.push(d)
      else fulls.push(d)
    }
  })

  // Normalisation : on s'assure que les deux listes ont la même taille
  // (par sécurité, on prend la taille minimale)
  const minLen = Math.min(fulls.length, medians.length)
  fullStrokes.value   = fulls.slice(0, minLen)
  medianStrokes.value = medians.slice(0, minLen)

  currentStroke.value = fullStrokes.value.length
}

function goToFirst() { stopPlay(); currentStroke.value = 0 }
function goToLast()  { stopPlay(); currentStroke.value = medianStrokes.value.length }

function prevStroke() {
  stopPlay()
  if (currentStroke.value > 0) currentStroke.value--
}

function nextStroke() {
  stopPlay()
  if (currentStroke.value < medianStrokes.value.length) currentStroke.value++
}

function togglePlay() {
  isPlaying.value ? stopPlay() : startPlay()
}

function startPlay() {
  if (currentStroke.value >= medianStrokes.value.length) currentStroke.value = 0
  isPlaying.value = true
  scheduleNext()
}

function stopPlay() {
  isPlaying.value      = false
  animatingIndex.value = -1
  if (playTimer) { clearTimeout(playTimer); playTimer = null }
  if (animFrame) { cancelAnimationFrame(animFrame); animFrame = null }
}

function scheduleNext() {
  if (!isPlaying.value || currentStroke.value >= medianStrokes.value.length) {
    isPlaying.value = false
    animatingIndex.value = -1
    return
  }

  const idx = currentStroke.value
  const d = medianStrokes.value[idx]

  const tmp = document.createElementNS('http://www.w3.org/2000/svg', 'path')
  tmp.setAttribute('d', d)
  document.body.appendChild(tmp)
  const len = Math.max(tmp.getTotalLength(), 1)
  document.body.removeChild(tmp)

  animatingIndex.value = idx
  animDashArray.value  = len
  animDashOffset.value = len

  const duration = Math.min(Math.max(len * 4, 300), 1200)
  const startTime = performance.now()

  function frame(now) {
    const t = Math.min((now - startTime) / duration, 1)
    const eased = 1 - Math.pow(1 - t, 2)
    animDashOffset.value = len * (1 - eased)

    if (t < 1) {
      animFrame = requestAnimationFrame(frame)
    } else {
      currentStroke.value++
      animatingIndex.value = -1
      if (isPlaying.value) {
        playTimer = setTimeout(scheduleNext, 150)
      }
    }
  }
  animFrame = requestAnimationFrame(frame)
}

watch(() => props.character, char => { if (char) loadSvg(char) }, { immediate: true })
onUnmounted(() => stopPlay())
</script>

<style scoped>
/* (styles inchangés) */
.stroke-player {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}
.svg-container {
  width: 200px;
  height: 200px;
  background: white;
  border: 1px solid var(--paper-mid);
  border-radius: var(--radius);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.svg-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  width: 100%;
  height: 100%;
}
.placeholder-char {
  font-size: 7rem;
  line-height: 1;
  color: var(--paper-mid);
}
.placeholder-char.faded { opacity: 0.25; }
.svg-error-msg {
  font-size: 0.68rem;
  color: var(--muted);
  letter-spacing: 0.04em;
}
.svg-canvas {
  width: 100%;
  height: 100%;
}
.controls {
  display: flex;
  gap: 0.35rem;
  align-items: center;
}
.ctrl-btn {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border: 1.5px solid var(--paper-mid);
  border-radius: var(--radius);
  font-size: 0.8rem;
  cursor: pointer;
  color: var(--ink);
  transition: all 0.15s;
  line-height: 1;
}
.ctrl-btn:hover:not(:disabled) { border-color: var(--ink); background: var(--paper-dark); }
.ctrl-btn:disabled { opacity: 0.28; cursor: not-allowed; }
.ctrl-play {
  width: 42px;
  height: 42px;
  font-size: 1rem;
  background: var(--ink);
  color: var(--paper);
  border-color: var(--ink);
}
.ctrl-play:hover:not(:disabled) { background: var(--vermilion); border-color: var(--vermilion); }
.stroke-counter {
  font-size: 0.7rem;
  color: var(--muted);
  letter-spacing: 0.04em;
}
</style>