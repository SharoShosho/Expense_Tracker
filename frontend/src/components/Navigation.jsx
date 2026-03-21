import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { authService } from '../services/authService'
import ThemeToggle from './ThemeToggle'
import {
  getPreferredCurrency,
  setPreferredCurrency,
  SUPPORTED_CURRENCIES,
} from '../services/currencyService'

export default function Navigation() {
  const navigate = useNavigate()
  const location = useLocation()
  const [currency, setCurrency] = useState(getPreferredCurrency())
  const [isMenuOpen, setIsMenuOpen] = useState(false)

  const handleLogout = () => {
    authService.logout()
    setIsMenuOpen(false)
    navigate('/login')
  }

  const user = authService.getCurrentUser()

  const handleCurrencyChange = (event) => {
    const nextCurrency = setPreferredCurrency(event.target.value)
    setCurrency(nextCurrency)
  }

  useEffect(() => {
    setIsMenuOpen(false)
  }, [location.pathname])

  const navItems = [
    { to: '/dashboard', label: 'Dashboard', matcher: (pathname) => pathname === '/dashboard' },
    { to: '/expenses', label: 'Expenses', matcher: (pathname) => pathname.startsWith('/expenses') },
    { to: '/budget/setup', label: 'Budgets', matcher: (pathname) => pathname.startsWith('/budget') },
    { to: '/statistics', label: 'Statistics', matcher: (pathname) => pathname.startsWith('/statistics') },
    { to: '/ai/tips', label: 'AI Tips', matcher: (pathname) => pathname.startsWith('/ai') },
  ]

  const getLinkClassName = (isItemActive) => (
    isItemActive
      ? 'text-blue-600 dark:text-blue-400 font-semibold'
      : 'text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400'
  )

  return (
    <nav className="bg-white dark:bg-gray-800 shadow-sm border-b border-gray-200 dark:border-gray-700">
      <div className="max-w-screen-2xl mx-auto px-3 sm:px-4 lg:px-6 py-3">
        <div className="flex items-center justify-between gap-3">
          <span className="text-lg sm:text-xl font-bold text-blue-600 whitespace-nowrap">💰 Expense Tracker</span>

          <button
            type="button"
            onClick={() => setIsMenuOpen((prev) => !prev)}
            className="lg:hidden inline-flex items-center justify-center rounded-lg border border-gray-300 dark:border-gray-600 p-2 text-gray-700 dark:text-gray-100 hover:bg-gray-100 dark:hover:bg-gray-700"
            aria-label="Toggle navigation menu"
            aria-expanded={isMenuOpen}
            aria-controls="mobile-navigation"
          >
            <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              {isMenuOpen
                ? <path d="M18 6 6 18M6 6l12 12" />
                : <path d="M3 12h18M3 6h18M3 18h18" />}
            </svg>
          </button>

          <div className="hidden lg:flex lg:flex-wrap lg:items-center lg:gap-3 lg:justify-end">
            <ThemeToggle />
            <select
              value={currency}
              onChange={handleCurrencyChange}
              className="text-sm border border-gray-300 dark:border-gray-600 rounded-lg px-2 py-1.5 bg-white dark:bg-gray-800 text-gray-800 dark:text-gray-100 min-w-20"
              aria-label="Select currency"
            >
              {SUPPORTED_CURRENCIES.map((item) => (
                <option key={item} value={item}>
                  {item}
                </option>
              ))}
            </select>
            <span className="text-sm text-gray-500 dark:text-gray-300 max-w-56 truncate">{user?.email}</span>
            <button
              onClick={handleLogout}
              className="text-sm px-3 sm:px-4 py-2 rounded-lg bg-gray-100 hover:bg-gray-200 dark:bg-gray-700 dark:hover:bg-gray-600 text-gray-700 dark:text-gray-100 transition"
            >
              Logout
            </button>
          </div>
        </div>

        <div className="hidden lg:flex lg:items-center lg:gap-4 lg:mt-3">
          {navItems.map((item) => (
            <Link
              key={item.to}
              to={item.to}
              className={`${getLinkClassName(item.matcher(location.pathname))} text-sm`}
            >
              {item.label}
            </Link>
          ))}
        </div>

        {isMenuOpen && (
          <div id="mobile-navigation" className="lg:hidden mt-3 border-t border-gray-200 dark:border-gray-700 pt-3 space-y-3">
            <div className="grid grid-cols-1 gap-2">
              {navItems.map((item) => (
                <Link
                  key={item.to}
                  to={item.to}
                  className={`${getLinkClassName(item.matcher(location.pathname))} text-sm px-2 py-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700`}
                >
                  {item.label}
                </Link>
              ))}
            </div>

            <div className="flex flex-wrap items-center gap-2 sm:gap-3">
              <ThemeToggle />
              <select
                value={currency}
                onChange={handleCurrencyChange}
                className="text-sm border border-gray-300 dark:border-gray-600 rounded-lg px-2 py-1.5 bg-white dark:bg-gray-800 text-gray-800 dark:text-gray-100 min-w-20"
                aria-label="Select currency"
              >
                {SUPPORTED_CURRENCIES.map((item) => (
                  <option key={item} value={item}>
                    {item}
                  </option>
                ))}
              </select>
              <button
                onClick={handleLogout}
                className="text-sm px-3 sm:px-4 py-2 rounded-lg bg-gray-100 hover:bg-gray-200 dark:bg-gray-700 dark:hover:bg-gray-600 text-gray-700 dark:text-gray-100 transition"
              >
                Logout
              </button>
            </div>

            <span className="block text-sm text-gray-500 dark:text-gray-300 truncate">{user?.email}</span>
          </div>
        )}
      </div>
    </nav>
  )
}
