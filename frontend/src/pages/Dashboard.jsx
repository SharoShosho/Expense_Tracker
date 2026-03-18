import { useState, useEffect, useCallback } from 'react'
import Navigation from '../components/Navigation'
import ExpenseList from '../components/ExpenseList'
import ExpenseForm from '../components/ExpenseForm'
import api from '../services/api'

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

  const handleCreate = async (data) => {
    await api.post('/expenses', data)
    setShowForm(false)
    fetchExpenses()
  }

  const handleUpdate = async (data) => {
    await api.put(`/expenses/${editingExpense.id}`, data)
    setEditingExpense(null)
    fetchExpenses()
  }

  const handleDelete = async (id) => {
    await api.delete(`/expenses/${id}`)
    fetchExpenses()
  }

  const totalAmount = expenses.reduce((sum, e) => sum + parseFloat(e.amount), 0)

  return (
    <div className="min-h-screen bg-gray-50">
      <Navigation />

      <main className="max-w-4xl mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">My Expenses</h1>
            <p className="text-gray-500 text-sm mt-1">
              {expenses.length} expense{expenses.length !== 1 ? 's' : ''} · Total: €{totalAmount.toFixed(2)}
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
        <div className="bg-white rounded-xl border border-gray-200 p-4 mb-6">
          <div className="grid grid-cols-1 sm:grid-cols-4 gap-3">
            <select
              value={filters.category}
              onChange={(e) => setFilters((p) => ({ ...p, category: e.target.value }))}
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
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
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <input
              type="date"
              value={filters.startDate}
              onChange={(e) => setFilters((p) => ({ ...p, startDate: e.target.value }))}
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <input
              type="date"
              value={filters.endDate}
              onChange={(e) => setFilters((p) => ({ ...p, endDate: e.target.value }))}
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
        </div>

        {/* Add/Edit Form */}
        {(showForm || editingExpense) && (
          <div className="bg-white rounded-xl border border-gray-200 p-6 mb-6">
            <h2 className="text-lg font-semibold text-gray-800 mb-4">
              {editingExpense ? 'Edit Expense' : 'New Expense'}
            </h2>
            <ExpenseForm
              initialData={editingExpense}
              onSubmit={editingExpense ? handleUpdate : handleCreate}
              onCancel={() => { setShowForm(false); setEditingExpense(null) }}
            />
          </div>
        )}

        {/* Expense list */}
        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm mb-4">
            {error}
          </div>
        )}

        {loading ? (
          <div className="text-center py-12 text-gray-400">Loading...</div>
        ) : (
          <ExpenseList
            expenses={expenses}
            onEdit={(expense) => { setEditingExpense(expense); setShowForm(false) }}
            onDelete={handleDelete}
          />
        )}
      </main>
    </div>
  )
}
