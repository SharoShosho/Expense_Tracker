import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { budgetService } from '../../services/budgetService'
import { formatCurrency } from '../../services/currencyService'
import { useCurrencyConversion } from '../../hooks/useCurrencyConversion'
import { getErrorMessage } from '../../services/errorService'

const STATUS_ICONS = { SAFE: '🟢', NEAR_LIMIT: '🟡', EXCEEDED: '🔴' }

const STATUS_COLORS = {
  SAFE: 'text-green-700 dark:text-green-300',
  NEAR_LIMIT: 'text-amber-700 dark:text-amber-300',
  EXCEEDED: 'text-red-700 dark:text-red-300',
}

const STATUS_BAR_COLORS = {
  SAFE: 'bg-green-500',
  NEAR_LIMIT: 'bg-amber-500',
  EXCEEDED: 'bg-red-500',
}

const STATUS_ORDER = { EXCEEDED: 0, NEAR_LIMIT: 1, SAFE: 2 }

const getCurrentMonth = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

export default function BudgetWidget({ refreshKey = 0 }) {
  const [overview, setOverview] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const { currency, convertFromBaseCurrency } = useCurrencyConversion({})

  useEffect(() => {
    setLoading(true)
    setError('')
    budgetService
      .getBudgetStatus(getCurrentMonth())
      .then(setOverview)
      .catch((err) => setError(getErrorMessage(err, 'Failed to load budget overview')))
      .finally(() => setLoading(false))
  }, [refreshKey])

  if (loading) {
    return (
      <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
        <div className="text-center py-4 text-gray-400 dark:text-gray-500 text-sm">
          Loading budget overview...
        </div>
      </div>
    )
  }

  const totalBudget = convertFromBaseCurrency(overview?.totalBudget ?? 0)
  const totalSpent = convertFromBaseCurrency(overview?.totalSpent ?? 0)
  const categories = overview?.categories ?? []
  const overallPct = totalBudget > 0 ? (totalSpent / totalBudget) * 100 : 0
  const overallStatus =
    totalBudget <= 0
      ? 'SAFE'
      : overallPct >= 100
        ? 'EXCEEDED'
        : overallPct >= 80
          ? 'NEAR_LIMIT'
          : 'SAFE'
  const alertCount = (overview?.exceededCount ?? 0) + (overview?.nearLimitCount ?? 0)

  const topCategories = [...categories]
    .map((c) => ({
      ...c,
      budgetAmount: convertFromBaseCurrency(c.budgetAmount),
      spentAmount: convertFromBaseCurrency(c.spentAmount),
    }))
    .sort((a, b) => (STATUS_ORDER[a.status] ?? 3) - (STATUS_ORDER[b.status] ?? 3))
    .slice(0, 3)

  return (
    <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4 sm:p-6">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg sm:text-xl font-semibold text-gray-900 dark:text-gray-100">
          💰 Budget Overview
        </h2>
        {alertCount > 0 && (
          <span className="text-xs font-medium px-2 py-1 rounded-full bg-amber-100 dark:bg-amber-900 text-amber-800 dark:text-amber-200">
            {alertCount} alert{alertCount !== 1 ? 's' : ''}
          </span>
        )}
      </div>

      {error && (
        <div className="text-red-600 dark:text-red-400 text-sm mb-3">{error}</div>
      )}

      {categories.length === 0 ? (
        <div className="text-center py-4 text-gray-500 dark:text-gray-400 text-sm">
          No budgets set yet.{' '}
          <Link
            to="/budget/setup"
            className="text-blue-600 dark:text-blue-400 hover:underline font-medium"
          >
            Set up budgets →
          </Link>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-2 gap-3 mb-4">
            <div className="bg-gray-50 dark:bg-gray-900/50 rounded-lg p-3">
              <p className="text-xs text-gray-500 dark:text-gray-400">Total Budget</p>
              <p className="text-lg font-bold text-blue-600 dark:text-blue-400">
                {formatCurrency(totalBudget, currency)}
              </p>
            </div>
            <div className="bg-gray-50 dark:bg-gray-900/50 rounded-lg p-3">
              <p className="text-xs text-gray-500 dark:text-gray-400">Total Spent</p>
              <p
                className={`text-lg font-bold ${
                  overallStatus === 'EXCEEDED'
                    ? 'text-red-600 dark:text-red-400'
                    : 'text-gray-900 dark:text-gray-100'
                }`}
              >
                {formatCurrency(totalSpent, currency)}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 mb-4">
            <div className="flex-1 h-2.5 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
              <div
                className={`h-full transition-all ${STATUS_BAR_COLORS[overallStatus]}`}
                style={{ width: `${Math.min(100, overallPct)}%` }}
                aria-hidden="true"
              />
            </div>
            <span className={`text-xs font-medium whitespace-nowrap ${STATUS_COLORS[overallStatus]}`}>
              {overallPct.toFixed(1)}%
            </span>
          </div>

          {topCategories.length > 0 && (
            <div className="mb-4">
              <p className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-2">
                Top Categories
              </p>
              <div className="space-y-1.5">
                {topCategories.map((item) => (
                  <div key={item.category} className="flex items-center justify-between text-sm">
                    <span className="flex items-center gap-1.5">
                      <span aria-hidden="true">{STATUS_ICONS[item.status] ?? '⚪'}</span>
                      <span className="text-gray-700 dark:text-gray-300">{item.category}</span>
                    </span>
                    <span className={`text-xs ${STATUS_COLORS[item.status] ?? 'text-gray-500'}`}>
                      {formatCurrency(item.spentAmount, currency)} /{' '}
                      {formatCurrency(item.budgetAmount, currency)}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </>
      )}

      <Link
        to="/budget/setup"
        className="block w-full text-center text-sm font-medium text-blue-600 dark:text-blue-400 hover:text-blue-700 dark:hover:text-blue-300 py-2 border border-blue-200 dark:border-blue-800 rounded-lg hover:bg-blue-50 dark:hover:bg-blue-950 transition mt-2"
      >
        Manage Budgets →
      </Link>
    </div>
  )
}
