<template>
  <main class="page home">
    <div class="container">

      <!-- Hero -->
      <section class="hero">
        <div class="hero-kana">
          <span v-for="k in kana" :key="k" class="hero-kana-char">{{ k }}</span>
        </div>
        <div class="hero-content">
          <p class="hero-subtitle">Apprenez le japonais</p>
          <h1 class="hero-title">言葉を<br><em>学びましょう</em></h1>
          <p class="hero-desc">
            Hiragana, katakana, kanji et vocabulaire —
            une approche progressive et rigoureuse de la langue japonaise.
          </p>
          <div class="hero-actions">
            <RouterLink to="/scripts" class="btn btn-vermilion">Commencer — Hiragana</RouterLink>
            <RouterLink to="/mots" class="btn">Parcourir le vocabulaire</RouterLink>
          </div>
        </div>
      </section>

      <!-- Stats -->
      <section class="stats">
        <div class="stat-item">
          <span class="stat-num jp">215,715</span>
          <span class="stat-label">mots</span>
        </div>
        <div class="stat-sep">·</div>
        <div class="stat-item">
          <span class="stat-num jp">10,384</span>
          <span class="stat-label">kanji</span>
        </div>
        <div class="stat-sep">·</div>
        <div class="stat-item">
          <span class="stat-num jp">224</span>
          <span class="stat-label">kana</span>
        </div>
        <div class="stat-sep">·</div>
        <div class="stat-item">
          <span class="stat-num jp">31,971</span>
          <span class="stat-label">exemples</span>
        </div>
      </section>

      <div class="divider">Parcourir</div>

      <!-- Cards de navigation -->
      <section class="nav-cards">
        <RouterLink to="/scripts" class="nav-card">
          <div class="nav-card-kana">あ ア</div>
          <h2>Hiragana & Katakana</h2>
          <p>Maîtrisez les deux syllabaires fondamentaux du japonais. 46 × 2 caractères essentiels.</p>
          <span class="nav-card-link">Étudier →</span>
        </RouterLink>

        <RouterLink to="/kanji" class="nav-card">
          <div class="nav-card-kana">漢</div>
          <h2>Kanji</h2>
          <p>10 384 caractères avec lectures, exemples et radicaux. Du N5 au N1.</p>
          <span class="nav-card-link">Explorer →</span>
        </RouterLink>

        <RouterLink to="/mots" class="nav-card">
          <div class="nav-card-kana">語</div>
          <h2>Vocabulaire</h2>
          <p>215 715 mots avec traductions, parties du discours et phrases d'exemple.</p>
          <span class="nav-card-link">Chercher →</span>
        </RouterLink>
      </section>

    </div>
  </main>
</template>

<script setup>
const kana = ['日', '本', '語', 'を', '学', 'ぶ', '言', '葉', '文', '字']
</script>

<style scoped>
.home { padding-top: var(--nav-height); }

/* Hero */
.hero {
  min-height: calc(100vh - var(--nav-height));
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 4rem;
  align-items: center;
  padding: 4rem 0;
  position: relative;
}

.hero-kana {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  align-items: center;
  position: relative;
}
.hero-kana-char {
  font-family: var(--font-jp);
  font-size: clamp(2.5rem, 5vw, 4rem);
  color: var(--ink);
  opacity: 0.06;
  line-height: 1;
  animation: float 6s ease-in-out infinite;
  animation-delay: calc(var(--i, 0) * 0.4s);
}
.hero-kana-char:nth-child(odd)  { transform: translateX(-20px); }
.hero-kana-char:nth-child(even) { transform: translateX(20px); }
@keyframes float {
  0%, 100% { transform: translateY(0) translateX(var(--tx, 0)); opacity: 0.06; }
  50%       { transform: translateY(-8px) translateX(var(--tx, 0)); opacity: 0.1; }
}

.hero-subtitle {
  font-family: var(--font-display);
  font-size: 0.85rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--vermilion);
  margin-bottom: 1rem;
}
.hero-title {
  font-size: clamp(2.5rem, 5vw, 4.5rem);
  font-weight: 300;
  line-height: 1.1;
  margin-bottom: 1.5rem;
}
.hero-title em {
  font-style: italic;
  color: var(--vermilion);
}
.hero-desc {
  color: var(--ink-light);
  line-height: 1.8;
  margin-bottom: 2.5rem;
  max-width: 42ch;
}
.hero-actions {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

/* Stats */
.stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2rem;
  padding: 3rem 0;
  border-top: 1px solid var(--paper-mid);
  border-bottom: 1px solid var(--paper-mid);
  flex-wrap: wrap;
}
.stat-item { text-align: center; }
.stat-num {
  display: block;
  font-size: 2rem;
  font-weight: 600;
  color: var(--ink);
  line-height: 1;
}
.stat-label {
  font-size: 0.75rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--muted);
}
.stat-sep { font-size: 2rem; color: var(--paper-mid); }

/* Nav cards */
.nav-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 2rem;
  margin-top: 1rem;
}
.nav-card {
  display: block;
  padding: 2.5rem 2rem;
  background: white;
  border: 1px solid var(--paper-mid);
  border-radius: var(--radius);
  text-decoration: none;
  color: var(--ink);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}
.nav-card::before {
  content: '';
  position: absolute;
  bottom: 0; left: 0; right: 0;
  height: 3px;
  background: var(--vermilion);
  transform: scaleX(0);
  transition: transform 0.3s ease;
}
.nav-card:hover { transform: translateY(-4px); box-shadow: var(--shadow-lg); }
.nav-card:hover::before { transform: scaleX(1); }

.nav-card-kana {
  font-family: var(--font-jp);
  font-size: 3rem;
  color: var(--vermilion);
  opacity: 0.15;
  position: absolute;
  top: 1rem; right: 1.5rem;
  font-weight: 600;
  line-height: 1;
  transition: opacity 0.3s;
}
.nav-card:hover .nav-card-kana { opacity: 0.25; }

.nav-card h2 {
  font-size: 1.4rem;
  margin-bottom: 0.75rem;
}
.nav-card p {
  font-size: 0.9rem;
  color: var(--ink-light);
  line-height: 1.7;
  margin-bottom: 1.5rem;
}
.nav-card-link {
  font-family: var(--font-display);
  font-size: 0.85rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--vermilion);
}

@media (max-width: 900px) {
  .hero { grid-template-columns: 1fr; }
  .hero-kana { display: none; }
  .nav-cards { grid-template-columns: 1fr; }
}
</style>