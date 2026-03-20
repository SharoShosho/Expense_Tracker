import DetailedTips from './DetailedTips'
import { aiService } from '../../services/aiService'
import './ai.css'

export default function Benchmarking() {
  return (
    <DetailedTips
      type="BENCHMARKING"
      title="Spending Benchmarks"
      icon="📈"
      loadData={aiService.getBenchmarking.bind(aiService)}
    >
      {(data) => (
        <div className="ai-card">
          {/* Comparison table */}
          <h3 className="font-semibold text-gray-800 dark:text-gray-100 mb-3">
            Your Spending vs Average (% of total)
          </h3>
          <div className="overflow-x-auto">
            <table className="ai-compare-table">
              <thead>
                <tr>
                  <th>Category</th>
                  <th>You</th>
                  <th>Average</th>
                  <th>Difference</th>
                </tr>
              </thead>
              <tbody>
                {Object.keys(data.avgSpendingByCategory || {}).map((cat) => {
                  const userVal = (data.userSpendingByCategory || {})[cat] ?? 0
                  const avgVal = data.avgSpendingByCategory[cat] ?? 0
                  const diff = userVal - avgVal
                  return (
                    <tr key={cat}>
                      <td>{cat}</td>
                      <td>{userVal.toFixed(1)}%</td>
                      <td>{avgVal.toFixed(1)}%</td>
                      <td className={diff > 5 ? 'ai-compare-above' : diff < -5 ? 'ai-compare-below' : 'ai-compare-equal'}>
                        {diff > 0 ? '+' : ''}{diff.toFixed(1)}%
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>

          {/* Strengths */}
          {data.strengths?.length > 0 && (
            <div className="mt-4">
              <h4 className="font-semibold text-green-700 mb-2">✅ Strengths</h4>
              {data.strengths.map((s, i) => (
                <div key={i} className="ai-list-item-good">
                  <span>✓</span>
                  <span>{s}</span>
                </div>
              ))}
            </div>
          )}

          {/* Weaknesses */}
          {data.weaknesses?.length > 0 && (
            <div className="mt-4">
              <h4 className="font-semibold text-red-600 mb-2">⚠️ Areas to Improve</h4>
              {data.weaknesses.map((w, i) => (
                <div key={i} className="ai-list-item-bad">
                  <span>✗</span>
                  <span>{w}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </DetailedTips>
  )
}
