import { createRouter, createWebHistory } from 'vue-router'
import HomeView        from '../views/HomeView.vue'
import WordsView       from '../views/WordsView.vue'
import WordDetailView  from '../views/WordDetailView.vue'
import KanjiView       from '../views/KanjiView.vue'
import KanjiDetailView from '../views/KanjiDetailView.vue'
import ScriptsView     from '../views/ScriptsView.vue'

const routes = [
  { path: '/',            component: HomeView,       name: 'home'        },
  { path: '/mots',        component: WordsView,       name: 'words'       },
  { path: '/mots/:id',    component: WordDetailView,  name: 'word-detail' },
  { path: '/kanji',       component: KanjiView,       name: 'kanji'       },
  { path: '/kanji/:id',   component: KanjiDetailView, name: 'kanji-detail'},
  { path: '/scripts',     component: ScriptsView,     name: 'scripts'     },
]

export default createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})