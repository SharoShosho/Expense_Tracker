const DEV_API_BASE_URL = '/api'
const INVALID_GITHUB_PAGES_FALLBACK = 'https://invalid-api.local/api'

const normalizeBaseUrl = (url) => {
  if (!url) return ''
  return url.trim().replace(/\/+$/, '')
}

const isGitHubPagesHost = () =>
  typeof window !== 'undefined' && window.location.hostname.endsWith('github.io')

const isRelativePath = (url) => url.startsWith('/')

export const resolveApiBaseUrl = () => {
  const configuredBaseUrl = normalizeBaseUrl(import.meta.env.VITE_API_BASE_URL)

  if (configuredBaseUrl) {
    if (import.meta.env.PROD && isGitHubPagesHost() && isRelativePath(configuredBaseUrl)) {
      console.error(
        'VITE_API_BASE_URL must be an absolute backend URL in production when deployed on GitHub Pages.'
      )
      return INVALID_GITHUB_PAGES_FALLBACK
    }
    return configuredBaseUrl
  }

  if (import.meta.env.DEV) {
    return DEV_API_BASE_URL
  }

  if (isGitHubPagesHost()) {
    console.error(
      'Missing VITE_API_BASE_URL for GitHub Pages production deployment. Set it to your backend URL, for example https://your-backend.example.com/api.'
    )
    return INVALID_GITHUB_PAGES_FALLBACK
  }

  return DEV_API_BASE_URL
}
