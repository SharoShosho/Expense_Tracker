import DetailedTips from './DetailedTips'
import { aiService } from '../../services/aiService'
import { formatCurrency } from '../../services/currencyService'
import { getPreferredCurrency } from '../../services/currencyService'
import { Link } from 'react-router-dom'
import './ai.css'

const STATUS_COLORS = {
  SAFE: 'var(--ai-green)',
  NEAR_LIMIT: 'var(--ai-yellow)',
  EXCEEDED: 'var(--ai-red)',
  NO_BUDGET: 'var(--ai-gray)',
}

export default function SpendingPattern() {
  const currency = getPreferredCurrency()
  return (
    <DetailedTips
      type="SPENDING_PATTERN"
      title="Spending Pattern Analysis"
      icon="📊"
      loadData={aiService.getSpendingPattern.bind(aiService)}
    >
      {(data) => (
        <div>
          {/* Summary row */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-4">
            <div className="ai-card">
              <p className="text-xs text-gray-500">Total Spent</p>
              <p className="text-xl font-bold text-gray-800 dark:text-gray-100">
                {formatCurrency(data.totalMonthlySpending, currency)}
              </p>
            </div>
            <div className="ai-card">
              <p className="text-xs text-gray-500">Total Budget</p>
              <p className="text-xl font-bold text-blue-600">
                {data.totalBudget > 0 ? formatCurrency(data.totalBudget, currency) : 'Not set'}
              </p>
            </div>
            <div className="ai-card">
              <p className="text-xs text-gray-500">Budget Used</p>
              <p className={`text-xl font-bold ${data.budgetUsagePercent > 100 ? 'text-red-500' : data.budgetUsagePercent > 80 ? 'text-yellow-500' : 'text-green-500'}`}>
                {data.totalBudget > 0 ? `${data.budgetUsagePercent.toFixed(1)}%` : '—'}
              </p>
            </div>
          </div>

          {/* Category breakdown */}
          {(data.categories || []).length > 0 && (
            <div className="ai-card mb-4">
              <h3 className="font-semibold text-gray-800 dark:text-gray-100 mb-3">
                Category Breakdown
              </h3>
              {data.categories.map((cat) => (
                <div key={cat.category} className="ai-category-row">
                  <div className="ai-category-row-header">
                    <div className="flex items-center gap-2">
                      <span className="ai-category-name">{cat.category}</span>
                      {cat.status !== 'NO_BUDGET' && (
                        <span className={`ai-badge ai-badge-${
                          cat.status === 'EXCEEDED' ? 'HIGH'
                            : cat.status === 'NEAR_LIMIT' ? 'MEDIUM' : 'LOW'
                        }`}>
                          {cat.status.replace('_', ' ')}
                        </span>
                      )}
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="ai-category-amount">
                        {formatCurrency(Number(cat.amount), currency)}
                      </span>
                      {cat.budget > 0 && (
                        <Link
                          to={`/ai/tips/category/${cat.category}`}
                          className="text-xs text-blue-600 hover:underline"
                        >
                          Deep Dive →
                        </Link>
                      )}
                    </div>
                  </div>
                  <div className="ai-progress-bar-track">
                    <div
                      className="ai-progress-bar-fill"
                      style={{
                        width: cat.budget > 0
                          ? `${Math.min((Number(cat.amount) / Number(cat.budget)) * 100, 100)}%`
                          : `${Math.min(cat.percentage, 100)}%`,
                        background: STATUS_COLORS[cat.status] || 'var(--ai-gray)',
                      }}
                    />
                  </div>
                  <div className="flex justify-between text-xs text-gray-500 mt-1">
                    <span>{cat.percentage.toFixed(1)}% of total</span>
                    {cat.budget > 0 && (
                      <span>Budget: {formatCurrency(Number(cat.budget), currency)}</span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}

          {data.totalPotentialSavings > 0 && (
            <div className="bg-green-50 border border-green-200 text-green-800 rounded-lg px-4 py-3 text-sm mb-4">
              💰 Potential savings by staying within budget: <strong>{formatCurrency(data.totalPotentialSavings, currency)}/month</strong>
            </div>
          )}
        </div>
      )}
    </DetailedTips>
  )
}
