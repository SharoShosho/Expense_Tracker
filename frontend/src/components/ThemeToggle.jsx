import { useEffect, useState } from 'react'
import {
  getThemeState,
  onSystemThemeChange,
  onThemeChange,
  setThemePreference,
} from '../services/themeService'

export default function ThemeToggle() {
  const [themeState, setThemeState] = useState(getThemeState())

  useEffect(() => {
    const unsubscribeTheme = onThemeChange(setThemeState)
    const unsubscribeSystem = onSystemThemeChange(setThemeState)
    return () => {
      unsubscribeTheme()
      unsubscribeSystem()
    }
  }, [])

  const toggleDarkMode = () => {
    const nextPreference = themeState.activeTheme === 'dark' ? 'light' : 'dark'
    setThemeState(setThemePreference(nextPreference))
  }

  const useSystemTheme = () => {
    setThemeState(setThemePreference('system'))
  }

  return (
    <div className="flex flex-wrap items-center gap-2">
      <button
        type="button"
        onClick={toggleDarkMode}
        className="text-xs sm:text-sm px-3 py-1.5 rounded-lg bg-gray-100 hover:bg-gray-200 dark:bg-gray-700 dark:hover:bg-gray-600 text-gray-700 dark:text-gray-100 transition"
      >
        {themeState.activeTheme === 'dark' ? 'Light mode' : 'Dark mode'}
      </button>
      <button
        type="button"
        onClick={useSystemTheme}
        className="text-xs px-2 py-1 rounded-lg border border-gray-300 dark:border-gray-600 text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition whitespace-nowrap"
        title="Follow system appearance"
      >
        System ({themeState.systemTheme})
      </button>
    </div>
  )
}

