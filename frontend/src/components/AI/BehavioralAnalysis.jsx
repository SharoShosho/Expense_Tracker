import DetailedTips from './DetailedTips'
import { aiService } from '../../services/aiService'
import { formatCurrency } from '../../services/currencyService'
import { getPreferredCurrency } from '../../services/currencyService'
import './ai.css'

export default function BehavioralAnalysis() {
  const currency = getPreferredCurrency()
  return (
    <DetailedTips
      type="BEHAVIORAL"
      title="Behavioral Pattern Recognition"
      icon="🧠"
      loadData={aiService.getBehavioral.bind(aiService)}
    >
      {(data) => (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="ai-card">
            <h3 className="font-semibold text-gray-800 dark:text-gray-100 mb-4">Spending Behaviour</h3>
            {[
              { label: 'Peak Spending Day', value: data.peakSpendingDay },
              { label: 'Avg Weekday Spending', value: formatCurrency(data.weekdayAvgSpending, currency) },
              { label: 'Avg Weekend Spending', value: formatCurrency(data.weekendAvgSpending, currency) },
              { label: 'Impulse Transactions', value: data.impulseTransactionCount },
              { label: 'Avg Daily Transactions', value: data.dailyTransactionCount },
            ].map(({ label, value }) => (
              <div key={label} className="flex justify-between py-2 border-b border-gray-100 dark:border-gray-700 last:border-0">
                <span className="text-sm text-gray-600 dark:text-gray-300">{label}</span>
                <span className="text-sm font-semibold text-gray-800 dark:text-gray-100">{value}</span>
              </div>
            ))}
          </div>

          <div className="ai-card">
            <h3 className="font-semibold text-gray-800 dark:text-gray-100 mb-3">Weekend vs Weekday</h3>
            <div className="ai-dimension-row">
              <div className="ai-dimension-label">
                <span>Weekday avg</span>
                <span>{formatCurrency(data.weekdayAvgSpending, currency)}</span>
              </div>
              <div className="ai-progress-bar-track">
                <div
                  className="ai-progress-bar-fill"
                  style={{
                    width: `${Math.min((data.weekdayAvgSpending /
                      Math.max(data.weekdayAvgSpending, data.weekendAvgSpending, 1)) * 100, 100)}%`,
                    background: 'var(--ai-blue)',
                  }}
                />
              </div>
            </div>
            <div className="ai-dimension-row">
              <div className="ai-dimension-label">
                <span>Weekend avg</span>
                <span>{formatCurrency(data.weekendAvgSpending, currency)}</span>
              </div>
              <div className="ai-progress-bar-track">
                <div
                  className="ai-progress-bar-fill"
                  style={{
                    width: `${Math.min((data.weekendAvgSpending /
                      Math.max(data.weekdayAvgSpending, data.weekendAvgSpending, 1)) * 100, 100)}%`,
                    background: data.weekendAvgSpending > data.weekdayAvgSpending * 1.5
                      ? 'var(--ai-red)' : 'var(--ai-yellow)',
                  }}
                />
              </div>
            </div>

            {data.impulseTransactionCount > 0 && (
              <div className="mt-4 bg-yellow-50 border border-yellow-200 text-yellow-800 rounded-lg px-3 py-2 text-sm">
                ⚠️ {data.impulseTransactionCount} potential impulse purchase{data.impulseTransactionCount !== 1 ? 's' : ''} detected
              </div>
            )}
          </div>
        </div>
      )}
    </DetailedTips>
  )
}
