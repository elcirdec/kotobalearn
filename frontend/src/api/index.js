const BASE = 'http://localhost:8080/api'

async function get(path, params = {}) {
  const url = new URL(BASE + path)
  Object.entries(params).forEach(([k, v]) => {
    if (v !== null && v !== undefined && v !== '') url.searchParams.set(k, v)
  })
  const res = await fetch(url)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}

// ─── Words ────────────────────────────────────────────────────────────────
export const wordsApi = {
  list: (params) => get('/words', params),
  get:  (id)     => get(`/words/${id}`),
}

// ─── Kanji ────────────────────────────────────────────────────────────────
export const kanjiApi = {
  list: (params) => get('/kanji', params),
  get:  (id)     => get(`/kanji/${id}`),
  words: (id, params)    => get(`/kanji/${id}/words`, params),
}

// ─── Scripts (hiragana / katakana) ────────────────────────────────────────
export const scriptsApi = {
  list: (params) => get('/scripts', params),
}

// ─── Tags ─────────────────────────────────────────────────────────────────
export const tagsApi = {
  list: () => get('/tags'),
}

export const radicalsApi = {
  list: () => get('/radicals'),
}