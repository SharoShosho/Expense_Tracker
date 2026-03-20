import { useState, useEffect, useCallback, useMemo } from 'react'
import Navigation from '../components/Navigation'
import ExpenseList from '../components/ExpenseList'
import ExpenseForm from '../components/ExpenseForm'
import BudgetWidget from '../components/Budget/BudgetWidget'
import api from '../services/api'
import {
  formatCurrency,
} from '../services/currencyService'
import { useCurrencyConversion } from '../hooks/useCurrencyConversion'
import { getErrorMessage } from '../services/errorService'
import { EXPENSE_CATEGORIES } from '../constants/categories'

const CATEGORIES = ['All', ...EXPENSE_CATEGORIES]

const buildExpenseQueryParams = (activeFilters) => {
  const rawParams = {
    category: activeFilters.category && activeFilters.category !== 'All' ? activeFilters.category : '',
    search: activeFilters.search,
    startDate: activeFilters.startDate,
    endDate: activeFilters.endDate,
  }

  return Object.fromEntries(
    Object.entries(rawParams).filter(([, value]) => Boolean(value))
  )
}

export default function Dashboard() {
  const [expenses, setExpenses] = useState([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [editingExpense, setEditingExpense] = useState(null)
  const [filters, setFilters] = useState({ category: '', search: '', startDate: '', endDate: '' })
  const [error, setError] = useState('')
  const [expenseVersion, setExpenseVersion] = useState(0)
  const {
    currency,
    rateWarning,
    convertFromBaseCurrency,
    convertToBaseCurrency,
  } = useCurrencyConversion({
    warningMessage: 'Could not load live exchange rate. Amounts are shown in EUR values.',
  })

  const fetchExpenses = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const params = buildExpenseQueryParams(filters)

      const response = await api.get('/expenses', { params })
      setExpenses(response.data)
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to load expenses'))
    } finally {
      setLoading(false)
    }
  }, [filters])

  useEffect(() => {
    fetchExpenses()
  }, [fetchExpenses])

  const displayExpenses = useMemo(
    () => expenses.map((expense) => ({
      ...expense,
      amount: convertFromBaseCurrency(expense.amount),
    })),
    [expenses, convertFromBaseCurrency]
  )

  const handleCreate = async (data) => {
    try {
      const normalizedAmount = await convertToBaseCurrency(data.amount)
      await api.post('/expenses', { ...data, amount: normalizedAmount })
      setShowForm(false)
      setExpenseVersion((v) => v + 1)
      await fetchExpenses()
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to save expense'))
      throw err
    }
  }

  const handleUpdate = async (data) => {
    try {
      const normalizedAmount = await convertToBaseCurrency(data.amount)
      await api.put(`/expenses/${editingExpense.id}`, { ...data, amount: normalizedAmount })
      setEditingExpense(null)
      setExpenseVersion((v) => v + 1)
      await fetchExpenses()
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to save expense'))
      throw err
    }
  }

  const handleDelete = async (id) => {
    try {
      await api.delete(`/expenses/${id}`)
      setExpenseVersion((v) => v + 1)
      await fetchExpenses()
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to delete expense'))
      throw err
    }
  }

  const totalAmount = displayExpenses.reduce((sum, e) => sum + Number(e.amount || 0), 0)

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <Navigation />

      <main className="max-w-screen-2xl mx-auto px-3 sm:px-4 lg:px-6 py-5 sm:py-8 lg:py-10">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between mb-6">
          <div>
            <h1 className="text-xl sm:text-2xl lg:text-3xl font-bold text-gray-900 dark:text-gray-100">My Expenses</h1>
            <p className="text-gray-500 dark:text-gray-300 text-xs sm:text-sm mt-1">
              {expenses.length} expense{expenses.length !== 1 ? 's' : ''} · Total: {formatCurrency(totalAmount, currency)}
            </p>
          </div>
          <button
            onClick={() => { setShowForm(true); setEditingExpense(null) }}
            className="w-full sm:w-auto bg-blue-600 hover:bg-blue-700 text-white font-medium px-5 py-2.5 rounded-lg transition"
          >
            + Add Expense
          </button>
        </div>

        {/* Filters */}
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4 mb-6">
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-3">
            <select
              value={filters.category}
              onChange={(e) => setFilters((p) => ({ ...p, category: e.target.value }))}
              className="border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2.5 text-base sm:text-sm bg-white dark:bg-gray-900 text-gray-800 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {CATEGORIES.map((c) => (
                <option key={c} value={c === 'All' ? '' : c}>{c}</option>
              ))}
            </select>
            <input
              type="text"
              placeholder="Search description..."
              value={filters.search}
              onChange={(e) => setFilters((p) => ({ ...p, search: e.target.value }))}
              className="border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2.5 text-base sm:text-sm bg-white dark:bg-gray-900 text-gray-800 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <input
              type="date"
              value={filters.startDate}
              onChange={(e) => setFilters((p) => ({ ...p, startDate: e.target.value }))}
              className="border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2.5 text-base sm:text-sm bg-white dark:bg-gray-900 text-gray-800 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <input
              type="date"
              value={filters.endDate}
              onChange={(e) => setFilters((p) => ({ ...p, endDate: e.target.value }))}
              className="border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2.5 text-base sm:text-sm bg-white dark:bg-gray-900 text-gray-800 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
        </div>

        {/* Add/Edit Form */}
        {(showForm || editingExpense) && (
          <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6 mb-6">
            <h2 className="text-lg font-semibold text-gray-800 dark:text-gray-100 mb-4">
              {editingExpense ? 'Edit Expense' : 'New Expense'}
            </h2>
            <ExpenseForm
              currency={currency}
              initialData={editingExpense}
              onSubmit={editingExpense ? handleUpdate : handleCreate}
              onCancel={() => { setShowForm(false); setEditingExpense(null) }}
            />
          </div>
        )}

        {/* Expense list */}
        {error && (
          <div className="bg-red-50 dark:bg-red-950 border border-red-200 dark:border-red-900 text-red-700 dark:text-red-300 px-4 py-3 rounded-lg text-sm mb-4">
            {error}
          </div>
        )}

        {rateWarning && (
          <div className="bg-amber-50 dark:bg-amber-950 border border-amber-200 dark:border-amber-900 text-amber-700 dark:text-amber-300 px-4 py-3 rounded-lg text-sm mb-4">
            {rateWarning}
          </div>
        )}

        <div className="mb-6">
          <BudgetWidget refreshKey={expenseVersion} />
        </div>

        {loading ? (
          <div className="text-center py-12 text-gray-400 dark:text-gray-500">Loading...</div>
        ) : (
          <ExpenseList
            expenses={displayExpenses}
            currency={currency}
            onEdit={(expense) => { setEditingExpense(expense); setShowForm(false) }}
            onDelete={handleDelete}
          />
        )}
      </main>
    </div>
  )
}
