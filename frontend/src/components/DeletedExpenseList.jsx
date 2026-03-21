import { useMemo, useState } from 'react'
import { formatCurrency } from '../services/currencyService'

export default function DeletedExpenseList({ expenses, onRestore, onBulkRestore, currency = 'EUR' }) {
  const [selectedIds, setSelectedIds] = useState([])
  const [working, setWorking] = useState(false)

  const allSelected = useMemo(
    () => expenses.length > 0 && selectedIds.length === expenses.length,
    [expenses.length, selectedIds.length]
  )

  const toggleSelectAll = () => {
    setSelectedIds((prev) => (prev.length === expenses.length ? [] : expenses.map((expense) => expense.id)))
  }

  const toggleSelected = (id) => {
    setSelectedIds((prev) => (prev.includes(id) ? prev.filter((value) => value !== id) : [...prev, id]))
  }

  const runRestoreOne = async (id) => {
    setWorking(true)
    try {
      await onRestore(id)
      setSelectedIds((prev) => prev.filter((value) => value !== id))
    } finally {
      setWorking(false)
    }
  }

  const runRestoreBulk = async () => {
    if (!selectedIds.length) return
    setWorking(true)
    try {
      await onBulkRestore(selectedIds)
      setSelectedIds([])
    } finally {
      setWorking(false)
    }
  }

  if (!expenses.length) {
    return (
      <div className="text-center py-12 text-gray-400 dark:text-gray-500">
        <p className="text-4xl mb-3">🗑️</p>
        <p className="text-lg">No soft-deleted expenses.</p>
      </div>
    )
  }

  return (
    <>
      <div className="mb-4 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-3 sm:p-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <label className="inline-flex items-center gap-2 text-sm text-gray-700 dark:text-gray-200">
            <input
              type="checkbox"
              checked={allSelected}
              onChange={toggleSelectAll}
              className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
            />
            Select all ({selectedIds.length} selected)
          </label>
          <button
            onClick={runRestoreBulk}
            disabled={!selectedIds.length || working}
            className="px-3 py-2 rounded-lg bg-green-600 hover:bg-green-700 text-white text-sm transition disabled:opacity-50"
          >
            {working ? 'Working...' : 'Restore selected'}
          </button>
        </div>
      </div>

      <div className="space-y-3">
        {expenses.map((expense) => (
          <div
            key={expense.id}
            className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-3 sm:p-4 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between"
          >
            <div className="flex items-start sm:items-center gap-4 w-full min-w-0">
              <input
                type="checkbox"
                checked={selectedIds.includes(expense.id)}
                onChange={() => toggleSelected(expense.id)}
                className="mt-1 sm:mt-0 h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
              />
              <div className="flex flex-col">
                <span className="font-semibold text-sm sm:text-base text-gray-900 dark:text-gray-100">
                  {formatCurrency(expense.amount, currency)}
                </span>
                <span className="text-xs text-gray-400 dark:text-gray-500">
                  {expense.date} {expense.deletedAt ? `• deleted ${String(expense.deletedAt).slice(0, 10)}` : ''}
                </span>
              </div>
              <div className="min-w-0">
                <span className="inline-block text-xs px-2 py-1 rounded-full font-medium bg-gray-100 text-gray-800">
                  {expense.category}
                </span>
                {expense.description && (
                  <p className="text-sm text-gray-600 dark:text-gray-300 mt-1 break-words">{expense.description}</p>
                )}
              </div>
            </div>
            <div className="flex w-full sm:w-auto gap-2">
              <button
                onClick={() => runRestoreOne(expense.id)}
                disabled={working}
                className="flex-1 sm:flex-none text-sm px-3 py-2 rounded-lg bg-green-600 hover:bg-green-700 text-white transition disabled:opacity-50"
              >
                Restore
              </button>
            </div>
          </div>
        ))}
      </div>
    </>
  )
}

