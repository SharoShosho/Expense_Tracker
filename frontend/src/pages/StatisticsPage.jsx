import { useState, useEffect } from 'react'
import Navigation from '../components/Navigation'
import StatisticsChart from '../components/StatisticsChart'
import api from '../services/api'
import { getPreferredCurrency, onCurrencyChange } from '../services/currencyService'

export default function StatisticsPage() {
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [currency, setCurrency] = useState(getPreferredCurrency())

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const response = await api.get('/statistics')
        setStats(response.data)
      } catch {
        setError('Failed to load statistics')
      } finally {
        setLoading(false)
      }
    }
    fetchStats()
  }, [])

  useEffect(() => onCurrencyChange(setCurrency), [])

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <Navigation />

      <main className="max-w-4xl mx-auto px-4 py-8">
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Statistics</h1>
          <p className="text-gray-500 dark:text-gray-300 text-sm mt-1">Your spending overview</p>
        </div>

        {error && (
          <div className="bg-red-50 dark:bg-red-950 border border-red-200 dark:border-red-900 text-red-700 dark:text-red-300 px-4 py-3 rounded-lg text-sm mb-4">
            {error}
          </div>
        )}

        {loading ? (
          <div className="text-center py-12 text-gray-400 dark:text-gray-500">Loading statistics...</div>
        ) : stats ? (
          <StatisticsChart stats={stats} currency={currency} />
        ) : (
          <div className="text-center py-12 text-gray-400 dark:text-gray-500">
            <p className="text-4xl mb-3">📊</p>
            <p className="text-lg">No data available yet. Add some expenses first!</p>
          </div>
        )}
      </main>
    </div>
  )
}
