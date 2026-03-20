import { useParams, Link } from 'react-router-dom'
import { useState, useEffect } from 'react'
import Navigation from '../Navigation'
import { TipCard } from './DetailedTips'
import { aiService } from '../../services/aiService'
import { getErrorMessage } from '../../services/errorService'
import { formatCurrency } from '../../services/currencyService'
import { getPreferredCurrency } from '../../services/currencyService'
import { EXPENSE_CATEGORIES } from '../../constants/categories'
import './ai.css'

function BarChart({ data }) {
  if (!data || Object.keys(data).length === 0) return null
  const max = Math.max(...Object.values(data).map(Number), 1)
  return (
    <div className="ai-bar-chart">
      {Object.entries(data).map(([label, value]) => (
        <div key={label} className="ai-bar-chart-col">
          <div className="ai-bar-value">{Number(value).toFixed(0)}</div>
          <div
            className="ai-bar"
            style={{ height: `${Math.max((Number(value) / max) * 90, 4)}px` }}
          />
          <div className="ai-bar-label">{label}</div>
        </div>
      ))}
    </div>
  )
}

export default function CategoryDeepDive() {
  const { categoryName } = useParams()
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const currency = getPreferredCurrency()

  useEffect(() => {
    if (!categoryName) return
    setLoading(true)
    aiService.getCategoryDeepDive(categoryName)
      .then(setData)
      .catch((err) => setError(getErrorMessage(err, 'Failed to load category analysis')))
      .finally(() => setLoading(false))
  }, [categoryName])

  return (
    <div className="ai-page min-h-screen bg-gray-50 dark:bg-gray-900">
      <Navigation />
      <main className="max-w-screen-2xl mx-auto px-3 sm:px-4 lg:px-6 py-5 sm:py-8">
        <div className="flex items-center gap-3 mb-4">
          <Link to="/ai/tips" className="text-sm text-blue-600 hover:underline">← Back to Overview</Link>
        </div>

        {/* Category picker */}
        <div className="ai-section-header">
          <h1>🔍 Category Deep Dive</h1>
          <p>Detailed analysis for a specific spending category</p>
        </div>

        <div className="ai-card mb-4">
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-200 mb-2">
            Select Category
          </label>
          <div className="flex flex-wrap gap-2">
            {EXPENSE_CATEGORIES.map((cat) => (
              <Link
                key={cat}
                to={`/ai/tips/category/${cat}`}
                className={`px-3 py-1.5 rounded-lg text-sm font-medium transition ${
                  cat === categoryName
                    ? 'bg-blue-600 text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200 dark:bg-gray-700 dark:text-gray-200'
                }`}
              >
                {cat}
              </Link>
            ))}
          </div>
        </div>

        {loading && <div className="ai-loading">Analysing {categoryName}…</div>}
        {error && <div className="ai-error">{error}</div>}

        {data && !loading && (
          <>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 mb-4">
              {[
                { label: 'Total Spent', value: formatCurrency(data.totalSpent, currency) },
                { label: 'Budget', value: data.budget > 0 ? formatCurrency(data.budget, currency) : 'Not set' },
                { label: 'Budget Used', value: data.budget > 0 ? `${data.budgetUsagePercent.toFixed(1)}%` : '—' },
                { label: 'Transactions', value: data.transactionCount },
                { label: 'Avg Transaction', value: formatCurrency(data.avgTransactionAmount, currency) },
                { label: 'Largest Purchase', value: formatCurrency(data.maxTransactionAmount, currency) },
              ].map(({ label, value }) => (
                <div key={label} className="ai-card">
                  <p className="text-xs text-gray-500 mb-1">{label}</p>
                  <p className="text-lg font-bold text-gray-800 dark:text-gray-100">{value}</p>
                </div>
              ))}
            </div>

            {data.budget > 0 && (
              <div className="ai-card mb-4">
                <div className="flex justify-between text-sm mb-1">
                  <span>Budget Progress</span>
                  <span className={data.budgetUsagePercent > 100 ? 'text-red-600 font-semibold' : 'text-gray-600'}>
                    {data.budgetUsagePercent.toFixed(1)}%
                  </span>
                </div>
                <div className="ai-progress-bar-track">
                  <div
                    className="ai-progress-bar-fill"
                    style={{
                      width: `${Math.min(data.budgetUsagePercent, 100)}%`,
                      background: data.budgetUsagePercent > 100 ? 'var(--ai-red)'
                        : data.budgetUsagePercent > 80 ? 'var(--ai-yellow)'
                        : 'var(--ai-green)',
                    }}
                  />
                </div>
              </div>
            )}

            {data.spendingByWeek && Object.keys(data.spendingByWeek).length > 0 && (
              <div className="ai-card mb-4">
                <h3 className="font-semibold text-gray-800 dark:text-gray-100 mb-2">
                  Weekly Breakdown
                </h3>
                <BarChart data={data.spendingByWeek} />
              </div>
            )}

            {data.tips?.length > 0 && (
              <div className="ai-card">
                <h3 className="font-semibold text-gray-800 dark:text-gray-100 mb-3">💡 Recommendations</h3>
                {data.tips.map((tip, i) => <TipCard key={i} tip={tip} />)}
              </div>
            )}
          </>
        )}
      </main>
    </div>
  )
}
