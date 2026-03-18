const THEME_STORAGE_KEY = 'themePreference'
const THEME_CHANGED_EVENT = 'expense-tracker:theme-changed'

export const THEME_PREFERENCES = ['system', 'light', 'dark']

export const getThemePreference = () => {
  const storedPreference = localStorage.getItem(THEME_STORAGE_KEY)
  return THEME_PREFERENCES.includes(storedPreference) ? storedPreference : 'system'
}

export const getSystemTheme = () =>
  window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'

export const resolveActiveTheme = (preference = getThemePreference()) =>
  preference === 'system' ? getSystemTheme() : preference

export const applyTheme = (preference = getThemePreference()) => {
  const activeTheme = resolveActiveTheme(preference)
  document.documentElement.classList.toggle('dark', activeTheme === 'dark')
  return activeTheme
}

export const setThemePreference = (preference) => {
  const safePreference = THEME_PREFERENCES.includes(preference) ? preference : 'system'
  localStorage.setItem(THEME_STORAGE_KEY, safePreference)
  const activeTheme = applyTheme(safePreference)
  const nextState = {
    preference: safePreference,
    activeTheme,
    systemTheme: getSystemTheme(),
  }
  window.dispatchEvent(new CustomEvent(THEME_CHANGED_EVENT, {
    detail: nextState,
  }))
  return nextState
}

export const getThemeState = () => {
  const preference = getThemePreference()
  return {
    preference,
    systemTheme: getSystemTheme(),
    activeTheme: resolveActiveTheme(preference),
  }
}

export const onThemeChange = (listener) => {
  const handler = (event) => {
    listener({
      preference: event.detail.preference,
      activeTheme: event.detail.activeTheme,
      systemTheme: getSystemTheme(),
    })
  }

  window.addEventListener(THEME_CHANGED_EVENT, handler)
  return () => window.removeEventListener(THEME_CHANGED_EVENT, handler)
}

export const onSystemThemeChange = (listener) => {
  const media = window.matchMedia('(prefers-color-scheme: dark)')
  const handler = () => {
    const preference = getThemePreference()
    const activeTheme = applyTheme(preference)
    listener({
      preference,
      activeTheme,
      systemTheme: getSystemTheme(),
    })
  }

  media.addEventListener('change', handler)
  return () => media.removeEventListener('change', handler)
}


