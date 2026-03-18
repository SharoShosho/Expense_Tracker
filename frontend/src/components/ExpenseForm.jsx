import { useState } from 'react'
import { getErrorMessage } from '../services/errorService'
import { validateExpenseAmount } from '../validation/validators'

const CATEGORIES = ['Food', 'Transport', 'Entertainment', 'Health', 'Housing', 'Shopping', 'Utilities', 'Other']

export default function ExpenseForm({ onSubmit, onCancel, initialData, currency = 'EUR' }) {
  const [form, setForm] = useState({
    amount: initialData?.amount || '',
    category: initialData?.category || CATEGORIES[0],
    description: initialData?.description || '',
    date: initialData?.date || new Date().toISOString().split('T')[0],
  })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleChange = (e) => {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    const validationError = validateExpenseAmount(form.amount)
    if (validationError) {
      setError(validationError)
      return
    }

    setLoading(true)
    try {
      await onSubmit({
        ...form,
        amount: Number(form.amount),
      })
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to save expense'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {error && (
        <div className="bg-red-50 dark:bg-red-950 border border-red-200 dark:border-red-900 text-red-700 dark:text-red-300 px-4 py-3 rounded-lg text-sm">
          {error}
        </div>
      )}

      <div>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-200 mb-1">Amount ({currency})</label>
        <input
          type="number"
          name="amount"
          step="0.01"
          min="0.01"
          value={form.amount}
          onChange={handleChange}
          required
          className="w-full border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
          placeholder="0.00"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-200 mb-1">Category</label>
        <select
          name="category"
          value={form.category}
          onChange={handleChange}
          className="w-full border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          {CATEGORIES.map((cat) => (
            <option key={cat} value={cat}>
              {cat}
            </option>
          ))}
        </select>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-200 mb-1">Description</label>
        <input
          type="text"
          name="description"
          value={form.description}
          onChange={handleChange}
          className="w-full border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
          placeholder="What was this expense for?"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-200 mb-1">Date</label>
        <input
          type="date"
          name="date"
          value={form.date}
          onChange={handleChange}
          required
          className="w-full border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>

      <div className="flex gap-3 pt-2">
        <button
          type="submit"
          disabled={loading}
          className="flex-1 bg-blue-600 hover:bg-blue-700 text-white font-medium py-2 px-4 rounded-lg transition disabled:opacity-50"
        >
          {loading ? 'Saving...' : initialData ? 'Update Expense' : 'Add Expense'}
        </button>
        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            className="flex-1 bg-gray-100 hover:bg-gray-200 text-gray-700 font-medium py-2 px-4 rounded-lg transition"
          >
            Cancel
          </button>
        )}
      </div>
    </form>
  )
}
