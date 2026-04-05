// URL relative → proxifiée par Vite vers http://localhost:8080
const BASE = '/api'

async function get(path, params = {}) {
  const url = new URL(BASE + path, window.location.origin)
  Object.entries(params).forEach(([k, v]) => {
    if (v !== null && v !== undefined && v !== '') url.searchParams.set(k, v)
  })
  const res = await fetch(url)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}

export const wordsApi = {
  list: (params)     => get('/words', params),
  get:  (id)         => get(`/words/${id}`),
}

export const kanjiApi = {
  list:  (params)     => get('/kanji', params),
  get:   (id)         => get(`/kanji/${id}`),
  words: (id, params) => get(`/kanji/${id}/words`, params),
  // Accepte les mêmes filtres que la liste : { jlpt, gradeGroup }
  // → retourne uniquement les traits qui existent pour ces filtres
  getStrokeCounts: (params = {}) => get('/kanji/stroke-counts', params),
}

export const scriptsApi = {
  list: (params) => get('/scripts', params),
}

export const tagsApi = {
  list: () => get('/tags'),
}

export const radicalsApi = {
  list: () => get('/radicals'),
}