import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      // Proxy toutes les requêtes /api vers Spring Boot
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // Proxy les SVGs animCJK vers Spring Boot
      '/animcjk': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})