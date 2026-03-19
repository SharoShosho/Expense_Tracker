import { formatCurrency } from '../services/currencyService'

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

const clampPercent = (value) => Math.max(0, Math.min(100, Number(value || 0)))

export default function BudgetProgressList({ items, currency, onDelete }) {
  if (!items || items.length === 0) {
    return (
      <div className="text-sm text-gray-500 dark:text-gray-400">
        No budgets set yet. Add one above to start tracking monthly limits.
      </div>
    )
  }

  return (
    <div className="space-y-3">
      {items.map((item) => {
        const status = item.status || 'SAFE'
        const progressWidth = clampPercent(item.usagePercent)

        return (
          <div key={item.category} className="bg-gray-50 dark:bg-gray-900/40 border border-gray-200 dark:border-gray-700 rounded-xl p-3 sm:p-4">
            <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3">
              <div className="min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <h3 className="font-semibold text-gray-900 dark:text-gray-100">{item.category}</h3>
                  <span className={`text-xs font-medium ${STATUS_TEXT_CLASSES[status] || STATUS_TEXT_CLASSES.SAFE}`}>
                    {STATUS_LABELS[status] || STATUS_LABELS.SAFE}
                  </span>
                </div>
                <p className="text-xs sm:text-sm text-gray-500 dark:text-gray-400 mt-1 break-words">
                  {formatCurrency(item.spentAmount, currency)} spent of {formatCurrency(item.budgetAmount, currency)}
                </p>
              </div>

              <button
                type="button"
                onClick={() => onDelete(item.category)}
                className="w-full sm:w-auto text-sm px-3 py-2 rounded-lg bg-red-50 hover:bg-red-100 text-red-700 transition"
              >
                Remove
              </button>
            </div>

            <div className="mt-3">
              <div className="h-2.5 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden" aria-hidden="true">
                <div
                  className={`h-full transition-all duration-300 ${STATUS_BAR_CLASSES[status] || STATUS_BAR_CLASSES.SAFE}`}
                  style={{ width: `${progressWidth}%` }}
                />
              </div>
              <div className="mt-2 flex justify-between items-center text-xs sm:text-sm">
                <span className="text-gray-600 dark:text-gray-300">{Number(item.usagePercent || 0).toFixed(1)}%</span>
                <span className={`${Number(item.remainingAmount || 0) < 0 ? 'text-red-700 dark:text-red-300' : 'text-gray-600 dark:text-gray-300'}`}>
                  Remaining: {formatCurrency(item.remainingAmount, currency)}
                </span>
              </div>
            </div>
          </div>
        )
      })}
    </div>
  )
}

