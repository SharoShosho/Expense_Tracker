import DetailedTips from './DetailedTips'
import { aiService } from '../../services/aiService'
import { formatCurrency } from '../../services/currencyService'
import { getPreferredCurrency } from '../../services/currencyService'
import './ai.css'

function riskClass(risk) {
  if (risk === 'HIGH') return 'ai-prediction-risk-HIGH'
  if (risk === 'MEDIUM') return 'ai-prediction-risk-MEDIUM'
  return 'ai-prediction-risk-LOW'
}

function trendArrow(trendPercent) {
  if (trendPercent > 2) return '↑'
  if (trendPercent < -2) return '↓'
  return '→'
}

export default function Predictions() {
  const currency = getPreferredCurrency()
  return (
    <DetailedTips
      type="PREDICTIONS"
      title="Spending Predictions"
      icon="🔮"
      loadData={aiService.getPredictions.bind(aiService)}
    >
      {(data) => (
        <div className="ai-card">
          <div className="flex flex-wrap gap-4 mb-4">
            <div>
              <p className="text-xs text-gray-500">Current Month</p>
              <p className="text-xl font-bold text-gray-800 dark:text-gray-100">
                {formatCurrency(data.currentMonthSpending, currency)}
              </p>
            </div>
            <div>
              <p className="text-xs text-gray-500">Monthly Trend</p>
              <p className={`text-xl font-bold ${data.trendPercent > 0 ? 'text-red-500' : data.trendPercent < 0 ? 'text-green-500' : 'text-gray-500'}`}>
                {trendArrow(data.trendPercent)} {Math.abs(data.trendPercent).toFixed(1)}%
              </p>
            </div>
          </div>

          <h3 className="font-semibold text-gray-800 dark:text-gray-100 mb-3">3-Month Forecast</h3>
          {(data.predictions || []).map((pred, i) => (
            <div key={i} className="ai-prediction-row">
              <div>
                <div className="ai-prediction-month">{pred.month}</div>
                <div className="ai-prediction-recommendation">{pred.recommendation}</div>
              </div>
              <div className="flex items-center gap-3">
                <span className="ai-prediction-amount">{formatCurrency(pred.predictedAmount, currency)}</span>
                <span className={`ai-badge ai-badge-${pred.riskLevel}`}>{pred.riskLevel}</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </DetailedTips>
  )
}
