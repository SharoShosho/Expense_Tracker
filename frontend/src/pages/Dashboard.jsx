import { useState, useEffect, useCallback, useMemo } from 'react'
import Navigation from '../components/Navigation'
import ExpenseList from '../components/ExpenseList'
import ExpenseForm from '../components/ExpenseForm'
import api from '../services/api'
import {
  DEFAULT_CURRENCY,
  formatCurrency,
  getPreferredCurrency,
  onCurrencyChange,
} from '../services/currencyService'
import { convertAmount, getExchangeRate } from '../services/exchangeRateService'

const CATEGORIES = ['All', 'Food', 'Transport', 'Entertainment', 'Health', 'Housing', 'Shopping', 'Utilities', 'Other']

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
  const [currency, setCurrency] = useState(getPreferredCurrency())
  const [exchangeRate, setExchangeRate] = useState(1)
  const [rateWarning, setRateWarning] = useState('')

  const fetchExpenses = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const params = buildExpenseQueryParams(filters)

      const response = await api.get('/expenses', { params })
      setExpenses(response.data)
    } catch {
      setError('Failed to load expenses')
    } finally {
      setLoading(false)
    }
  }, [filters])

  useEffect(() => {
    fetchExpenses()
  }, [fetchExpenses])

  useEffect(() => onCurrencyChange(setCurrency), [])

  useEffect(() => {
    let mounted = true

    const loadExchangeRate = async () => {
      try {
        const nextRate = await getExchangeRate(DEFAULT_CURRENCY, currency)
        if (mounted) {
          setExchangeRate(nextRate)
          setRateWarning('')
        }
      } catch {
        if (mounted) {
          setExchangeRate(1)
          setRateWarning('Could not load live exchange rate. Amounts are shown in EUR values.')
        }
      }
    }

    loadExchangeRate()
    return () => {
      mounted = false
    }
  }, [currency])

  const displayExpenses = useMemo(
    () => expenses.map((expense) => ({
      ...expense,
      amount: convertAmount(expense.amount, exchangeRate),
    })),
    [expenses, exchangeRate]
  )

  const convertAmountToStorageCurrency = useCallback(async (amount) => {
    if (currency === DEFAULT_CURRENCY) {
      return Number(amount)
    }

    const latestRate = await getExchangeRate(DEFAULT_CURRENCY, currency)
    return convertAmount(amount, 1 / latestRate)
  }, [currency])

  const handleCreate = async (data) => {
    const normalizedAmount = await convertAmountToStorageCurrency(data.amount)
    await api.post('/expenses', { ...data, amount: normalizedAmount })
    setShowForm(false)
    fetchExpenses()
  }

  const handleUpdate = async (data) => {
    const normalizedAmount = await convertAmountToStorageCurrency(data.amount)
    await api.put(`/expenses/${editingExpense.id}`, { ...data, amount: normalizedAmount })
    setEditingExpense(null)
    fetchExpenses()
  }

  const handleDelete = async (id) => {
    await api.delete(`/expenses/${id}`)
    fetchExpenses()
  }

  const totalAmount = displayExpenses.reduce((sum, e) => sum + Number(e.amount || 0), 0)

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <Navigation />

      <main className="max-w-4xl mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">My Expenses</h1>
            <p className="text-gray-500 dark:text-gray-300 text-sm mt-1">
              {expenses.length} expense{expenses.length !== 1 ? 's' : ''} · Total: {formatCurrency(totalAmount, currency)}
            </p>
          </div>
          <button
            onClick={() => { setShowForm(true); setEditingExpense(null) }}
            className="bg-blue-600 hover:bg-blue-700 text-white font-medium px-5 py-2.5 rounded-lg transition"
          >
            + Add Expense
          </button>
        </div>

        {/* Filters */}
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4 mb-6">
          <div className="grid grid-cols-1 sm:grid-cols-4 gap-3">
            <select
              value={filters.category}
              onChange={(e) => setFilters((p) => ({ ...p, category: e.target.value }))}
              className="border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2 text-sm bg-white dark:bg-gray-900 text-gray-800 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
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
              className="border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2 text-sm bg-white dark:bg-gray-900 text-gray-800 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <input
              type="date"
              value={filters.startDate}
              onChange={(e) => setFilters((p) => ({ ...p, startDate: e.target.value }))}
              className="border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2 text-sm bg-white dark:bg-gray-900 text-gray-800 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <input
              type="date"
              value={filters.endDate}
              onChange={(e) => setFilters((p) => ({ ...p, endDate: e.target.value }))}
              className="border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2 text-sm bg-white dark:bg-gray-900 text-gray-800 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
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
