import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import Navigation from '../Navigation'
import { aiService } from '../../services/aiService'
import { getErrorMessage } from '../../services/errorService'
import { formatCurrency } from '../../services/currencyService'
import { getPreferredCurrency } from '../../services/currencyService'
import './ai.css'

const TYPE_ROUTES = {
  SPENDING_PATTERN: '/ai/tips/spending-pattern',
  BEHAVIORAL: '/ai/tips/behavioral',
  BENCHMARKING: '/ai/tips/benchmarking',
  PREDICTIONS: '/ai/tips/predictions',
  ANOMALIES: '/ai/tips/anomalies',
  WELLNESS_SCORE: '/ai/wellness',
  HISTORY_TREND: '/ai/tips/history-trend',
}

const TYPE_ICONS = {
  SPENDING_PATTERN: '📊',
  BEHAVIORAL: '🧠',
  BENCHMARKING: '📈',
  PREDICTIONS: '🔮',
  ANOMALIES: '🚨',
  WELLNESS_SCORE: '💚',
  HISTORY_TREND: '📅',
}

export default function TipsOverview() {
  const [overview, setOverview] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const currency = getPreferredCurrency()

  useEffect(() => {
    aiService.getOverview()
      .then(setOverview)
      .catch((err) => setError(getErrorMessage(err, 'Failed to load tips overview')))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="ai-page min-h-screen bg-gray-50 dark:bg-gray-900">
      <Navigation />
      <main className="max-w-screen-2xl mx-auto px-3 sm:px-4 lg:px-6 py-5 sm:py-8">
        <div className="ai-section-header">
          <h1>🤖 AI Savings Tips</h1>
          <p>Personalised insights based on your actual spending and budgets</p>
        </div>

        {loading && <div className="ai-loading">Analysing your spending data…</div>}
        {error && <div className="ai-error">{error}</div>}

        {overview && !loading && (
          <>
            {/* Stats bar */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
              <div className="ai-card text-center">
                <p className="text-sm text-gray-500 dark:text-gray-400 mb-1">Wellness Score</p>
                <p className="text-3xl font-bold text-blue-600">{overview.wellnessScore}<span className="text-lg text-gray-400">/100</span></p>
              </div>
              <div className="ai-card text-center">
                <p className="text-sm text-gray-500 dark:text-gray-400 mb-1">Active Tips</p>
                <p className="text-3xl font-bold text-gray-800 dark:text-gray-100">{overview.totalTipsCount}</p>
              </div>
              <div className="ai-card text-center">
                <p className="text-sm text-gray-500 dark:text-gray-400 mb-1">Potential Monthly Savings</p>
                <p className="text-3xl font-bold text-green-600">{formatCurrency(overview.totalPotentialSavings, currency)}</p>
              </div>
            </div>

            {/* Summary cards */}
            <div className="ai-overview-grid">
              {(overview.summaries || []).map((summary) => (
                <Link
                  key={summary.type}
                  to={TYPE_ROUTES[summary.type] || '/ai/tips'}
                  className="ai-summary-card"
                >
                  <div className="ai-summary-card-type">
                    {TYPE_ICONS[summary.type] || '💡'} {summary.type.replace(/_/g, ' ')}
                  </div>
                  <div className="ai-summary-card-title">{summary.title}</div>
                  <div className="ai-summary-card-text">{summary.summary}</div>
                  <div className="ai-summary-card-footer">
                    <span className={`ai-badge ai-badge-${summary.priority}`}>{summary.priority}</span>
                    {summary.potentialSavings > 0 && (
                      <span className="ai-summary-card-savings">
                        Save {formatCurrency(summary.potentialSavings, currency)}/mo
                      </span>
                    )}
                  </div>
                </Link>
              ))}
            </div>
          </>
        )}
      </main>
    </div>
  )
}
