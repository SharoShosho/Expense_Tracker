import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useState } from 'react'
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

  const handleLogout = () => {
    authService.logout()
    navigate('/login')
  }

  const user = authService.getCurrentUser()

  const handleCurrencyChange = (event) => {
    const nextCurrency = setPreferredCurrency(event.target.value)
    setCurrency(nextCurrency)
  }

  const isActive = (path) =>
    location.pathname === path
      ? 'text-blue-600 dark:text-blue-400 font-semibold'
      : 'text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400'

  return (
    <nav className="bg-white dark:bg-gray-800 shadow-sm border-b border-gray-200 dark:border-gray-700">
      <div className="max-w-screen-2xl mx-auto px-3 sm:px-4 lg:px-6 py-3 flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div className="flex flex-col gap-2 sm:flex-row sm:flex-wrap sm:items-center sm:gap-4 w-full md:w-auto">
          <span className="text-lg sm:text-xl font-bold text-blue-600 whitespace-nowrap">💰 Expense Tracker</span>
          <Link to="/dashboard" className={`${isActive('/dashboard')} text-sm sm:text-base`}>
            Dashboard
          </Link>
          <Link to="/statistics" className={`${isActive('/statistics')} text-sm sm:text-base`}>
            Statistics
          </Link>
          <Link to="/budget/setup" className={`${isActive('/budget/setup')} text-sm sm:text-base`}>
            Budgets
          </Link>
          <Link to="/ai/tips" className={`${isActive('/ai/tips')} text-sm sm:text-base`}>
            AI Tips
          </Link>
        </div>
        <div className="flex flex-wrap items-center gap-2 sm:gap-3 md:justify-end">
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
          <span className="hidden lg:inline text-sm text-gray-500 dark:text-gray-300 max-w-56 truncate">{user?.email}</span>
          <button
            onClick={handleLogout}
            className="text-sm px-3 sm:px-4 py-2 rounded-lg bg-gray-100 hover:bg-gray-200 dark:bg-gray-700 dark:hover:bg-gray-600 text-gray-700 dark:text-gray-100 transition"
          >
            Logout
          </button>
        </div>
      </div>
    </nav>
  )
}
