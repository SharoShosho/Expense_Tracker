import DetailedTips from './DetailedTips'
import { aiService } from '../../services/aiService'
import { formatCurrency } from '../../services/currencyService'
import { getPreferredCurrency } from '../../services/currencyService'
import './ai.css'

export default function AnomalyAlerts() {
  const currency = getPreferredCurrency()
  return (
    <DetailedTips
      type="ANOMALIES"
      title="Anomaly Alerts"
      icon="🚨"
      loadData={aiService.getAnomalies.bind(aiService)}
    >
      {(data) => (
        <div className="ai-card">
          <div className="flex flex-wrap gap-4 mb-4">
            <div>
              <p className="text-xs text-gray-500">Unusual Transactions</p>
              <p className={`text-2xl font-bold ${data.anomalyCount > 0 ? 'text-red-500' : 'text-green-500'}`}>
                {data.anomalyCount}
              </p>
            </div>
            {data.anomalyCount > 0 && (
              <div>
                <p className="text-xs text-gray-500">Total Anomaly Amount</p>
                <p className="text-2xl font-bold text-red-500">
                  {formatCurrency(data.totalAnomalyAmount, currency)}
                </p>
              </div>
            )}
          </div>

          {(data.anomalies || []).length === 0 ? (
            <p className="text-sm text-green-700 bg-green-50 p-3 rounded-lg">
              ✅ No unusual transactions detected this month. Great consistency!
            </p>
          ) : (
            <div>
              <h3 className="font-semibold text-gray-800 dark:text-gray-100 mb-3">
                Flagged Transactions
              </h3>
              {data.anomalies.map((anomaly, i) => (
                <div key={i} className="ai-anomaly-item">
                  <div className="ai-anomaly-item-header">
                    <span>{anomaly.description}</span>
                    <span className="ai-anomaly-item-amount">
                      {formatCurrency(anomaly.amount, currency)}
                    </span>
                  </div>
                  <div className="ai-anomaly-item-meta">
                    {anomaly.category} · {anomaly.date} · 
                    Avg for category: {formatCurrency(anomaly.averageForCategory, currency)} · 
                    {anomaly.deviationPercent.toFixed(0)}% above average
                  </div>
                  <div className="ai-anomaly-item-suggestion">{anomaly.suggestion}</div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </DetailedTips>
  )
}
