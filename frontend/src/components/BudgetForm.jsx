import { useMemo, useState } from 'react'
import { getErrorMessage } from '../services/errorService'
import { validateExpenseAmount } from '../validation/validators'

export default function BudgetForm({ categories, currency, onSave }) {
  const [form, setForm] = useState({
    category: categories[0] || 'Other',
    amount: '',
  })
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const safeCategories = useMemo(
    () => (categories.length > 0 ? categories : ['Other']),
    [categories]
  )

  const handleChange = (event) => {
    const { name, value } = event.target
    setForm((previous) => ({ ...previous, [name]: value }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')

    const validationError = validateExpenseAmount(form.amount)
    if (validationError) {
      setError(validationError)
      return
    }

    setSaving(true)
    try {
      await onSave({
        category: form.category,
        amount: Number(form.amount),
      })
      setForm((previous) => ({ ...previous, amount: '' }))
    } catch (err) {
      setError(getErrorMessage(err, 'Failed to save budget'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      {error && (
        <div className="bg-red-50 dark:bg-red-950 border border-red-200 dark:border-red-900 text-red-700 dark:text-red-300 px-3 py-2 rounded-lg text-sm">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <div>
          <label className="block text-xs sm:text-sm font-medium text-gray-700 dark:text-gray-200 mb-1">Category</label>
          <select
            name="category"
            value={form.category}
            onChange={handleChange}
            className="w-full border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2.5 text-base sm:text-sm bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            {safeCategories.map((category) => (
              <option key={category} value={category}>{category}</option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-xs sm:text-sm font-medium text-gray-700 dark:text-gray-200 mb-1">Monthly budget ({currency})</label>
          <input
            type="number"
            inputMode="decimal"
            name="amount"
            min="0.01"
            step="0.01"
            value={form.amount}
            onChange={handleChange}
            placeholder="0.00"
            className="w-full border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2.5 text-base sm:text-sm bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
      </div>

      <button
        type="submit"
        disabled={saving}
        className="w-full sm:w-auto bg-blue-600 hover:bg-blue-700 text-white font-medium px-4 py-2.5 rounded-lg transition disabled:opacity-50"
      >
        {saving ? 'Saving budget...' : 'Save budget'}
      </button>
    </form>
  )
}

