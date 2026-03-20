import { useState, useEffect, useCallback, useMemo } from 'react'
import Navigation from '../Navigation'
import { budgetService } from '../../services/budgetService'
import { formatCurrency } from '../../services/currencyService'
import { useCurrencyConversion } from '../../hooks/useCurrencyConversion'
import { getErrorMessage } from '../../services/errorService'
import { validateExpenseAmount } from '../../validation/validators'
import { EXPENSE_CATEGORIES } from '../../constants/categories'

const STATUS_LABELS = {
  SAFE: 'On track',
  NEAR_LIMIT: 'Near limit',
  EXCEEDED: 'Exceeded',
}

const STATUS_BAR_CLASSES = {
  SAFE: 'bg-green-500',
  NEAR_LIMIT: 'bg-amber-500',
  EXCEEDED: 'bg-red-500',
}

const STATUS_TEXT_CLASSES = {
  SAFE: 'text-green-700 dark:text-green-300',
  NEAR_LIMIT: 'text-amber-700 dark:text-amber-300',
  EXCEEDED: 'text-red-700 dark:text-red-300',
}

const getCurrentMonth = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

const clamp = (v) => Math.max(0, Math.min(100, Number(v ?? 0)))

export default function BudgetSetup() {
  const [budgetOverview, setBudgetOverview] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [formCategory, setFormCategory] = useState(EXPENSE_CATEGORIES[0])
  const [formAmount, setFormAmount] = useState('')
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)
  const [editingCategory, setEditingCategory] = useState(null)
  const [deleteConfirm, setDeleteConfirm] = useState(null)
  const [successMessage, setSuccessMessage] = useState('')

  const { currency, convertFromBaseCurrency, convertToBaseCurrency } = useCurrencyConversion({})

  const fetchOverview = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const data = await budgetService.getBudgetStatus(getCurrentMonth())
      setBudgetOverview(data)
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to load budgets'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchOverview()
  }, [fetchOverview])

  const displayOverview = useMemo(() => {
    if (!budgetOverview) return null
    return {
      ...budgetOverview,
      totalBudget: convertFromBaseCurrency(budgetOverview.totalBudget),
      totalSpent: convertFromBaseCurrency(budgetOverview.totalSpent),
      categories: (budgetOverview.categories ?? []).map((c) => ({
        ...c,
        budgetAmount: convertFromBaseCurrency(c.budgetAmount),
        spentAmount: convertFromBaseCurrency(c.spentAmount),
        remainingAmount: convertFromBaseCurrency(c.remainingAmount),
      })),
    }
  }, [budgetOverview, convertFromBaseCurrency])

  const showSuccess = (msg) => {
    setSuccessMessage(msg)
    setTimeout(() => setSuccessMessage(''), 3000)
  }

  const handleEdit = (item) => {
    setEditingCategory(item.category)
    setFormCategory(item.category)
    setFormAmount(String(item.budgetAmount))
    setFormError('')
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const handleCancelEdit = () => {
    setEditingCategory(null)
    setFormCategory(EXPENSE_CATEGORIES[0])
    setFormAmount('')
    setFormError('')
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setFormError('')
    const amountErr = validateExpenseAmount(formAmount)
    if (amountErr) {
      setFormError(amountErr)
      return
    }
    setSaving(true)
    try {
      const normalized = await convertToBaseCurrency(Number(formAmount))
      await budgetService.saveBudget(formCategory, normalized)
      const wasEditing = editingCategory
      handleCancelEdit()
      await fetchOverview()
      showSuccess(wasEditing ? 'Budget updated!' : 'Budget saved!')
    } catch (err) {
      setFormError(getErrorMessage(err, 'Failed to save budget'))
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (category) => {
    try {
      await budgetService.deleteBudget(category)
      setDeleteConfirm(null)
      await fetchOverview()
      showSuccess('Budget removed.')
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to delete budget'))
    }
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <Navigation />

      <main className="max-w-screen-lg mx-auto px-3 sm:px-4 lg:px-6 py-5 sm:py-8">
        <div className="mb-6">
          <h1 className="text-xl sm:text-2xl lg:text-3xl font-bold text-gray-900 dark:text-gray-100">
            Budget Management
          </h1>
          <p className="text-gray-500 dark:text-gray-300 text-xs sm:text-sm mt-1">
            Set monthly spending limits per category and track your progress.
          </p>
        </div>

        {successMessage && (
          <div className="mb-4 bg-green-50 dark:bg-green-950 border border-green-200 dark:border-green-900 text-green-700 dark:text-green-300 px-4 py-3 rounded-lg text-sm">
            {successMessage}
          </div>
        )}

        {/* Budget Form */}
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4 sm:p-6 mb-6">
          <h2 className="text-base sm:text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">
            {editingCategory ? `Edit Budget: ${editingCategory}` : 'Add / Update Budget'}
          </h2>
          <form onSubmit={handleSubmit} className="space-y-3">
            {formError && (
              <div className="bg-red-50 dark:bg-red-950 border border-red-200 dark:border-red-900 text-red-700 dark:text-red-300 px-3 py-2 rounded-lg text-sm">
                {formError}
              </div>
            )}

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block text-xs sm:text-sm font-medium text-gray-700 dark:text-gray-200 mb-1">
                  Category
                </label>
                <select
                  value={formCategory}
                  onChange={(e) => setFormCategory(e.target.value)}
                  disabled={!!editingCategory}
                  className="w-full border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2.5 text-base sm:text-sm bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-60"
                >
                  {EXPENSE_CATEGORIES.map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs sm:text-sm font-medium text-gray-700 dark:text-gray-200 mb-1">
                  Monthly limit ({currency})
                </label>
                <input
                  type="number"
                  inputMode="decimal"
                  min="0.01"
                  step="0.01"
                  value={formAmount}
                  onChange={(e) => setFormAmount(e.target.value)}
                  placeholder="0.00"
                  className="w-full border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2.5 text-base sm:text-sm bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>

            <div className="flex gap-2">
              <button
                type="submit"
                disabled={saving}
                className="bg-blue-600 hover:bg-blue-700 text-white font-medium px-4 py-2.5 rounded-lg transition disabled:opacity-50 text-sm"
              >
                {saving ? 'Saving...' : editingCategory ? 'Update Budget' : 'Save Budget'}
              </button>
              {editingCategory && (
                <button
                  type="button"
                  onClick={handleCancelEdit}
                  className="px-4 py-2.5 rounded-lg border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 transition text-sm"
                >
                  Cancel
                </button>
              )}
            </div>
          </form>
        </div>

        {error && (
          <div className="mb-4 bg-red-50 dark:bg-red-950 border border-red-200 dark:border-red-900 text-red-700 dark:text-red-300 px-4 py-3 rounded-lg text-sm">
            {error}
          </div>
        )}

        {/* Summary stats */}
        {!loading && (displayOverview?.categories ?? []).length > 0 && (
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
            <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl p-3 sm:p-4">
              <p className="text-xs text-gray-500 dark:text-gray-400">Total Budget</p>
              <p className="text-xl font-bold text-blue-600 mt-1">
                {formatCurrency(displayOverview.totalBudget, currency)}
              </p>
            </div>
            <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl p-3 sm:p-4">
              <p className="text-xs text-gray-500 dark:text-gray-400">Total Spent</p>
              <p className="text-xl font-bold text-gray-900 dark:text-gray-100 mt-1">
                {formatCurrency(displayOverview.totalSpent, currency)}
              </p>
            </div>
            <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl p-3 sm:p-4">
              <p className="text-xs text-gray-500 dark:text-gray-400">Near Limit</p>
              <p className="text-xl font-bold text-amber-600 mt-1">
                {displayOverview.nearLimitCount}
              </p>
            </div>
            <div className="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl p-3 sm:p-4">
              <p className="text-xs text-gray-500 dark:text-gray-400">Exceeded</p>
              <p className="text-xl font-bold text-red-600 mt-1">
                {displayOverview.exceededCount}
              </p>
            </div>
          </div>
        )}

        {/* Budget list */}
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4 sm:p-6">
          <h2 className="text-base sm:text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">
            Your Budgets
          </h2>
          {loading ? (
            <div className="text-center py-8 text-gray-400 dark:text-gray-500 text-sm">
              Loading...
            </div>
          ) : (displayOverview?.categories ?? []).length === 0 ? (
            <div className="text-center py-8 text-gray-500 dark:text-gray-400 text-sm">
              No budgets set yet. Add one above to start tracking monthly limits.
            </div>
          ) : (
            <div className="space-y-3">
              {(displayOverview.categories ?? []).map((item) => {
                const status = item.status || 'SAFE'
                const progressWidth = clamp(item.usagePercent)
                return (
                  <div
                    key={item.category}
                    className="bg-gray-50 dark:bg-gray-900/40 border border-gray-200 dark:border-gray-700 rounded-xl p-3 sm:p-4"
                  >
                    <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3">
                      <div className="min-w-0">
                        <div className="flex items-center gap-2 flex-wrap">
                          <h3 className="font-semibold text-gray-900 dark:text-gray-100">
                            {item.category}
                          </h3>
                          <span
                            className={`text-xs font-medium ${STATUS_TEXT_CLASSES[status] ?? STATUS_TEXT_CLASSES.SAFE}`}
                          >
                            {STATUS_LABELS[status] ?? STATUS_LABELS.SAFE}
                          </span>
                        </div>
                        <p className="text-xs sm:text-sm text-gray-500 dark:text-gray-400 mt-1">
                          {formatCurrency(item.spentAmount, currency)} spent of{' '}
                          {formatCurrency(item.budgetAmount, currency)}
                        </p>
                      </div>
                      <div className="flex gap-2 shrink-0">
                        <button
                          type="button"
                          onClick={() => handleEdit(item)}
                          className="text-sm px-3 py-2 rounded-lg bg-blue-50 hover:bg-blue-100 dark:bg-blue-950 dark:hover:bg-blue-900 text-blue-700 dark:text-blue-300 transition"
                        >
                          Edit
                        </button>
                        <button
                          type="button"
                          onClick={() => setDeleteConfirm(item.category)}
                          className="text-sm px-3 py-2 rounded-lg bg-red-50 hover:bg-red-100 dark:bg-red-950 dark:hover:bg-red-900 text-red-700 dark:text-red-300 transition"
                        >
                          Delete
                        </button>
                      </div>
                    </div>

                    <div className="mt-3">
                      <div
                        className="h-2.5 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden"
                        aria-hidden="true"
                      >
                        <div
                          className={`h-full transition-all duration-300 ${STATUS_BAR_CLASSES[status] ?? STATUS_BAR_CLASSES.SAFE}`}
                          style={{ width: `${progressWidth}%` }}
                        />
                      </div>
                      <div className="mt-2 flex justify-between items-center text-xs sm:text-sm">
                        <span className="text-gray-600 dark:text-gray-300">
                          {Number(item.usagePercent ?? 0).toFixed(1)}%
                        </span>
                        <span
                          className={
                            Number(item.remainingAmount ?? 0) < 0
                              ? 'text-red-700 dark:text-red-300'
                              : 'text-gray-600 dark:text-gray-300'
                          }
                        >
                          Remaining: {formatCurrency(item.remainingAmount, currency)}
                        </span>
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </div>
      </main>

      {/* Delete confirmation modal */}
      {deleteConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4">
          <div className="bg-white dark:bg-gray-800 rounded-xl shadow-xl max-w-sm w-full p-6">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-2">
              Delete Budget
            </h3>
            <p className="text-gray-600 dark:text-gray-300 text-sm mb-6">
              Are you sure you want to remove the budget for{' '}
              <strong>{deleteConfirm}</strong>? This cannot be undone.
            </p>
            <div className="flex gap-3 justify-end">
              <button
                type="button"
                onClick={() => setDeleteConfirm(null)}
                className="px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 transition text-sm"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => handleDelete(deleteConfirm)}
                className="px-4 py-2 rounded-lg bg-red-600 hover:bg-red-700 text-white transition text-sm font-medium"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
