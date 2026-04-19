import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const repoParts = process.env.GITHUB_REPOSITORY?.split('/') ?? []
const repoName = repoParts.length === 2 ? repoParts[1] : ''
const defaultBase = process.env.GITHUB_ACTIONS === 'true' && repoName ? `/${repoName}/` : '/'

export default defineConfig({
  base: process.env.VITE_BASE_PATH || defaultBase,
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
