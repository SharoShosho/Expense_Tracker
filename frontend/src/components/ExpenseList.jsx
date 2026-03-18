import { useState } from 'react'

const CATEGORY_COLORS = {
  Food: 'bg-green-100 text-green-800',
  Transport: 'bg-blue-100 text-blue-800',
  Entertainment: 'bg-purple-100 text-purple-800',
  Health: 'bg-red-100 text-red-800',
  Housing: 'bg-yellow-100 text-yellow-800',
  Shopping: 'bg-pink-100 text-pink-800',
  Utilities: 'bg-orange-100 text-orange-800',
  Other: 'bg-gray-100 text-gray-800',
}

export default function ExpenseList({ expenses, onEdit, onDelete }) {
  const [deletingId, setDeletingId] = useState(null)

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this expense?')) return
    setDeletingId(id)
    try {
      await onDelete(id)
    } finally {
      setDeletingId(null)
    }
  }

  if (expenses.length === 0) {
    return (
      <div className="text-center py-12 text-gray-400">
        <p className="text-4xl mb-3">📋</p>
        <p className="text-lg">No expenses yet. Add your first one!</p>
      </div>
    )
  }

  return (
    <div className="space-y-3">
      {expenses.map((expense) => (
        <div
          key={expense.id}
          className="bg-white rounded-xl border border-gray-200 p-4 flex items-center justify-between hover:shadow-sm transition"
        >
          <div className="flex items-center gap-4">
            <div className="flex flex-col">
              <span className="font-semibold text-gray-900">
                €{parseFloat(expense.amount).toFixed(2)}
              </span>
              <span className="text-xs text-gray-400">{expense.date}</span>
            </div>
            <div>
              <span
                className={`inline-block text-xs px-2 py-1 rounded-full font-medium ${
                  CATEGORY_COLORS[expense.category] || CATEGORY_COLORS.Other
                }`}
              >
                {expense.category}
              </span>
              {expense.description && (
                <p className="text-sm text-gray-600 mt-1">{expense.description}</p>
              )}
            </div>
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => onEdit(expense)}
              className="text-sm px-3 py-1.5 rounded-lg bg-blue-50 hover:bg-blue-100 text-blue-700 transition"
            >
              Edit
            </button>
            <button
              onClick={() => handleDelete(expense.id)}
              disabled={deletingId === expense.id}
              className="text-sm px-3 py-1.5 rounded-lg bg-red-50 hover:bg-red-100 text-red-700 transition disabled:opacity-50"
            >
              {deletingId === expense.id ? '...' : 'Delete'}
            </button>
          </div>
        </div>
      ))}
    </div>
  )
}
