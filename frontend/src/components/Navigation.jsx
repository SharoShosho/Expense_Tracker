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
      <div className="max-w-6xl mx-auto px-4 py-3 flex items-center justify-between">
        <div className="flex items-center gap-6">
          <span className="text-xl font-bold text-blue-600">💰 Expense Tracker</span>
          <Link to="/dashboard" className={isActive('/dashboard')}>
            Dashboard
          </Link>
          <Link to="/statistics" className={isActive('/statistics')}>
            Statistics
          </Link>
        </div>
        <div className="flex items-center gap-4">
          <ThemeToggle />
          <select
            value={currency}
            onChange={handleCurrencyChange}
            className="text-sm border border-gray-300 dark:border-gray-600 rounded-lg px-2 py-1.5 bg-white dark:bg-gray-800 text-gray-800 dark:text-gray-100"
            aria-label="Select currency"
          >
            {SUPPORTED_CURRENCIES.map((item) => (
              <option key={item} value={item}>
                {item}
              </option>
            ))}
          </select>
          <span className="text-sm text-gray-500 dark:text-gray-300">{user?.email}</span>
          <button
            onClick={handleLogout}
            className="text-sm px-4 py-2 rounded-lg bg-gray-100 hover:bg-gray-200 dark:bg-gray-700 dark:hover:bg-gray-600 text-gray-700 dark:text-gray-100 transition"
          >
            Logout
          </button>
        </div>
      </div>
    </nav>
  )
}
