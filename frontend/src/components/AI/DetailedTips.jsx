import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import Navigation from '../Navigation'
import { aiService } from '../../services/aiService'
import { getErrorMessage } from '../../services/errorService'
import './ai.css'

function TipCard({ tip }) {
  const [expanded, setExpanded] = useState(false)
  return (
    <div className={`ai-tip-card ai-tip-card-${tip.priority}`}>
      <div className="flex items-start justify-between gap-2">
        <div>
          <div className="flex items-center gap-2 mb-1 flex-wrap">
            <h4>{tip.title}</h4>
            <span className={`ai-badge ai-badge-${tip.priority}`}>{tip.priority}</span>
            {tip.potentialSavings > 0 && (
              <span className="text-xs font-medium text-green-700">
                💰 Save {tip.potentialSavings.toFixed(2)}/mo
              </span>
            )}
          </div>
          <p>{tip.message}</p>
        </div>
        {tip.actionItems?.length > 0 && (
          <button
            className="text-xs text-blue-600 underline whitespace-nowrap"
            onClick={() => setExpanded((v) => !v)}
          >
            {expanded ? 'Less ▲' : 'Actions ▼'}
          </button>
        )}
      </div>
      {expanded && tip.actionItems?.length > 0 && (
        <ul>
          {tip.actionItems.map((item, i) => <li key={i}>• {item}</li>)}
        </ul>
      )}
    </div>
  )
}

export default function DetailedTips({ type, title, icon, loadData, children }) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    loadData()
      .then(setData)
      .catch((err) => setError(getErrorMessage(err, 'Failed to load tips')))
      .finally(() => setLoading(false))
  }, [loadData])

  return (
    <div className="ai-page min-h-screen bg-gray-50 dark:bg-gray-900">
      <Navigation />
      <main className="max-w-screen-2xl mx-auto px-3 sm:px-4 lg:px-6 py-5 sm:py-8">
        <div className="flex items-center gap-3 mb-6">
          <Link to="/ai/tips" className="text-sm text-blue-600 hover:underline">← Back to Overview</Link>
        </div>
        <div className="ai-section-header">
          <h1>{icon} {title}</h1>
        </div>

        {loading && <div className="ai-loading">Analysing your data…</div>}
        {error && <div className="ai-error">{error}</div>}

        {data && !loading && (
          <>
            {/* Custom data view rendered by parent */}
            {children && children(data)}

            {/* Tips section */}
            {data.tips?.length > 0 && (
              <div className="ai-card mt-4">
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

export { TipCard }
