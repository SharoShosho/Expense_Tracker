import { useState, useEffect } from 'react'
import Navigation from '../Navigation'
import { TipCard } from './DetailedTips'
import { aiService } from '../../services/aiService'
import { getErrorMessage } from '../../services/errorService'
import { Link } from 'react-router-dom'
import './ai.css'

const SCORE_COLORS = { EXCELLENT: '#27ae60', GOOD: '#3498db', FAIR: '#f39c12', POOR: '#e74c3c' }

const DIMENSIONS = [
  { key: 'spendingDisciplineScore', label: 'Spending Discipline' },
  { key: 'budgetAdherenceScore', label: 'Budget Adherence' },
  { key: 'savingRateScore', label: 'Saving Rate' },
  { key: 'financialAwarenessScore', label: 'Financial Awareness' },
  { key: 'riskManagementScore', label: 'Risk Management' },
]

function scoreColor(score) {
  if (score >= 80) return 'var(--ai-green)'
  if (score >= 60) return 'var(--ai-blue)'
  if (score >= 40) return 'var(--ai-yellow)'
  return 'var(--ai-red)'
}

export default function WellnessScore() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    aiService.getWellnessScore()
      .then(setData)
      .catch((err) => setError(getErrorMessage(err, 'Failed to load wellness score')))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="ai-page min-h-screen bg-gray-50 dark:bg-gray-900">
      <Navigation />
      <main className="max-w-screen-2xl mx-auto px-3 sm:px-4 lg:px-6 py-5 sm:py-8">
        <div className="flex items-center gap-3 mb-6">
          <Link to="/ai/tips" className="text-sm text-blue-600 hover:underline">← Back to Overview</Link>
        </div>
        <div className="ai-section-header">
          <h1>💚 Financial Wellness Score</h1>
          <p>A 5-dimension assessment of your financial health</p>
        </div>

        {loading && <div className="ai-loading">Calculating your score…</div>}
        {error && <div className="ai-error">{error}</div>}

        {data && !loading && (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Score circle */}
            <div className="ai-card flex flex-col items-center">
              <div
                className="ai-score-circle"
                style={{ borderColor: SCORE_COLORS[data.scoreLabel] || '#3498db',
                         color: SCORE_COLORS[data.scoreLabel] || '#3498db' }}
              >
                <span className="ai-score-circle-value">{data.overallScore}</span>
                <span className="ai-score-circle-label">/ 100</span>
              </div>
              <div className="text-center">
                <span
                  className="ai-badge"
                  style={{
                    background: SCORE_COLORS[data.scoreLabel] + '22',
                    color: SCORE_COLORS[data.scoreLabel],
                    border: `1px solid ${SCORE_COLORS[data.scoreLabel]}55`
                  }}
                >
                  {data.scoreLabel}
                </span>
                <p className="text-sm text-gray-500 dark:text-gray-400 mt-3">
                  Next milestone: <strong>{data.nextMilestone}</strong>
                  {' '}({data.pointsToNextMilestone} points to go)
                </p>
              </div>
            </div>

            {/* Dimensions */}
            <div className="ai-card">
              <h3 className="font-semibold text-gray-800 dark:text-gray-100 mb-4">Score Breakdown</h3>
              {DIMENSIONS.map(({ key, label }) => {
                const score = data[key] ?? 0
                return (
                  <div key={key} className="ai-dimension-row">
                    <div className="ai-dimension-label">
                      <span>{label}</span>
                      <span style={{ color: scoreColor(score) }}>{score}/100</span>
                    </div>
                    <div className="ai-progress-bar-track">
                      <div
                        className="ai-progress-bar-fill"
                        style={{
                          width: `${Math.min(score, 100)}%`,
                          background: scoreColor(score),
                        }}
                      />
                    </div>
                  </div>
                )
              })}
            </div>

            {/* Tips */}
            {data.tips?.length > 0 && (
              <div className="ai-card lg:col-span-2">
                <h3 className="font-semibold text-gray-800 dark:text-gray-100 mb-3">💡 Recommendations</h3>
                {data.tips.map((tip, i) => <TipCard key={i} tip={tip} />)}
              </div>
            )}
          </div>
        )}
      </main>
    </div>
  )
}
