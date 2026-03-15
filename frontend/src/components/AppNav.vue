<template>
  <nav class="nav">
    <div class="nav-inner">
      <RouterLink to="/" class="nav-logo">
        <span class="nav-logo-jp">言葉</span>
        <span class="nav-logo-en">KotobaLearn</span>
      </RouterLink>

      <div class="nav-links">
        <RouterLink to="/scripts" class="nav-link">Hiragana · Katakana</RouterLink>
        <RouterLink to="/kanji"   class="nav-link">Kanji</RouterLink>
        <RouterLink to="/mots"    class="nav-link">Vocabulaire</RouterLink>
      </div>

      <button class="nav-burger" @click="open = !open" :class="{ active: open }">
        <span /><span /><span />
      </button>
    </div>

    <!-- Menu mobile -->
    <div class="nav-mobile" :class="{ open }">
      <RouterLink to="/scripts" @click="open = false">Hiragana · Katakana</RouterLink>
      <RouterLink to="/kanji"   @click="open = false">Kanji</RouterLink>
      <RouterLink to="/mots"    @click="open = false">Vocabulaire</RouterLink>
    </div>
  </nav>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'

const open = ref(false)
const route = useRoute()
watch(() => route.path, () => { open.value = false })
</script>

<style scoped>
.nav {
  position: fixed;
  top: 0; left: 0; right: 0;
  height: var(--nav-height);
  background: rgba(247, 243, 238, 0.92);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--paper-mid);
  z-index: 100;
}
.nav-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 2rem;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.nav-logo {
  display: flex;
  align-items: baseline;
  gap: 0.6rem;
  text-decoration: none;
}
.nav-logo-jp {
  font-family: var(--font-jp);
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--vermilion);
  line-height: 1;
}
.nav-logo-en {
  font-family: var(--font-display);
  font-size: 1.1rem;
  font-weight: 300;
  letter-spacing: 0.08em;
  color: var(--ink);
}
.nav-links {
  display: flex;
  gap: 2.5rem;
}
.nav-link {
  font-family: var(--font-display);
  font-size: 0.9rem;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--ink-light);
  text-decoration: none;
  position: relative;
  padding-bottom: 2px;
  transition: color 0.2s;
}
.nav-link::after {
  content: '';
  position: absolute;
  bottom: 0; left: 0; right: 0;
  height: 1px;
  background: var(--vermilion);
  transform: scaleX(0);
  transition: transform 0.2s ease;
}
.nav-link:hover { color: var(--ink); }
.nav-link:hover::after,
.nav-link.router-link-active::after { transform: scaleX(1); }
.nav-link.router-link-active { color: var(--ink); }

.nav-burger {
  display: none;
  flex-direction: column;
  gap: 5px;
  background: none;
  border: none;
  padding: 4px;
}
.nav-burger span {
  display: block;
  width: 22px;
  height: 1.5px;
  background: var(--ink);
  transition: all 0.3s;
}
.nav-burger.active span:nth-child(1) { transform: rotate(45deg) translate(5px, 5px); }
.nav-burger.active span:nth-child(2) { opacity: 0; }
.nav-burger.active span:nth-child(3) { transform: rotate(-45deg) translate(5px, -5px); }

.nav-mobile {
  display: none;
  flex-direction: column;
  background: var(--paper);
  border-top: 1px solid var(--paper-mid);
  padding: 1rem 2rem;
  gap: 1rem;
}
.nav-mobile a {
  font-family: var(--font-display);
  font-size: 1.1rem;
  letter-spacing: 0.06em;
  color: var(--ink);
  text-decoration: none;
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--paper-mid);
}

@media (max-width: 768px) {
  .nav-links { display: none; }
  .nav-burger { display: flex; }
  .nav-mobile.open { display: flex; }
  .nav { height: auto; }
}
</style>