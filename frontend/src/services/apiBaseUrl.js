const DEV_API_BASE_URL = '/api'
const ABSOLUTE_HTTP_URL_PATTERN = /^https?:\/\//i
const GITHUB_PAGES_HOST_PATTERN = /(^|\.)github\.io$/i

const normalizeBaseUrl = (url) => {
  if (!url) return ''
  return url.trim().replace(/\/+$/, '')
}

const isGitHubPagesHost = () =>
  typeof window !== 'undefined' && GITHUB_PAGES_HOST_PATTERN.test(window.location.hostname)

const isRelativePath = (url) => url.startsWith('/')
const isAbsoluteHttpUrl = (url) => ABSOLUTE_HTTP_URL_PATTERN.test(url)

export const resolveApiBaseUrl = () => {
  const configuredBaseUrl = normalizeBaseUrl(import.meta.env.VITE_API_BASE_URL)

  if (configuredBaseUrl) {
    if (
      import.meta.env.PROD &&
      isGitHubPagesHost() &&
      (isRelativePath(configuredBaseUrl) || !isAbsoluteHttpUrl(configuredBaseUrl))
    ) {
      throw new Error(
        'VITE_API_BASE_URL must be an absolute backend URL (starting with http:// or https://) in production when deployed on GitHub Pages.'
      )
    }
    return configuredBaseUrl
  }

  if (import.meta.env.DEV) {
    return DEV_API_BASE_URL
  }

  if (isGitHubPagesHost()) {
    throw new Error(
      'Missing VITE_API_BASE_URL for GitHub Pages production deployment. Set an absolute backend URL (starting with http:// or https://), for example https://your-backend.example.com/api.'
    )
  }

  return DEV_API_BASE_URL
}
