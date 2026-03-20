import DetailedTips from './DetailedTips'
import { aiService } from '../../services/aiService'
import { formatCurrency } from '../../services/currencyService'
import { getPreferredCurrency } from '../../services/currencyService'
import './ai.css'

function BarChart({ data }) {
  if (!data || Object.keys(data).length === 0) return null
  const values = Object.values(data).map(Number)
  const max = Math.max(...values, 1)
  return (
    <div className="ai-bar-chart">
      {Object.entries(data).map(([month, amount]) => (
        <div key={month} className="ai-bar-chart-col">
          <div className="ai-bar-value">{Number(amount).toFixed(0)}</div>
          <div
            className="ai-bar"
            style={{ height: `${Math.max((Number(amount) / max) * 90, 4)}px` }}
          />
          <div className="ai-bar-label">{month.slice(5)}</div>
        </div>
      ))}
    </div>
  )
}

const TREND_COLORS = {
  INCREASING: '#e74c3c',
  DECREASING: '#27ae60',
  STABLE: '#3498db',
}

export default function HistoryTrend() {
  const currency = getPreferredCurrency()
  return (
    <DetailedTips
      type="HISTORY_TREND"
      title="6-Month Spending History"
      icon="📅"
      loadData={aiService.getHistoryTrend.bind(aiService)}
    >
      {(data) => (
        <div className="ai-card">
          <div className="flex flex-wrap gap-4 mb-4">
            <div>
              <p className="text-xs text-gray-500">6-Month Average</p>
              <p className="text-xl font-bold text-gray-800 dark:text-gray-100">
                {formatCurrency(data.avgMonthlySpending, currency)}
              </p>
            </div>
            <div>
              <p className="text-xs text-gray-500">Trend Direction</p>
              <p
                className="text-xl font-bold"
                style={{ color: TREND_COLORS[data.trendDirection] || '#64748b' }}
              >
                {data.trendDirection === 'INCREASING' ? '↑' : data.trendDirection === 'DECREASING' ? '↓' : '→'}{' '}
                {data.trendDirection}
              </p>
            </div>
            <div>
              <p className="text-xs text-gray-500">Monthly Change</p>
              <p className={`text-xl font-bold ${data.trendPercent > 0 ? 'text-red-500' : data.trendPercent < 0 ? 'text-green-500' : 'text-gray-500'}`}>
                {data.trendPercent > 0 ? '+' : ''}{data.trendPercent.toFixed(1)}%/mo
              </p>
            </div>
          </div>

          {data.isUnsustainable && (
            <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg px-4 py-3 text-sm mb-4 font-medium">
              🚨 Warning: Current spending trend is unsustainable. Immediate action required.
            </div>
          )}

          <BarChart data={data.monthlySpending} />

          <div className="mt-2 text-xs text-gray-500 text-center">Monthly spending over the last 6 months</div>
        </div>
      )}
    </DetailedTips>
  )
}
