import { useState, useEffect, useMemo } from 'react'
import Navigation from '../components/Navigation'
import StatisticsChart from '../components/StatisticsChart'
import api from '../services/api'
import { useCurrencyConversion } from '../hooks/useCurrencyConversion'
import { getErrorMessage } from '../services/errorService'

export default function StatisticsPage() {
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const {
    currency,
    rateWarning,
    convertFromBaseCurrency,
  } = useCurrencyConversion({
    warningMessage: 'Could not load live exchange rate. Statistics are shown in EUR values.',
  })

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const response = await api.get('/statistics')
        setStats(response.data)
      } catch (err) {
        setError(getErrorMessage(err, 'Failed to load statistics'))
      } finally {
        setLoading(false)
      }
    }
    fetchStats()
  }, [])

  const convertedStats = useMemo(() => {
    if (!stats) {
      return null
    }

    const convertMapValues = (input) => Object.fromEntries(
      Object.entries(input || {}).map(([key, value]) => [key, convertFromBaseCurrency(value)])
    )

    return {
      ...stats,
      totalAmount: convertFromBaseCurrency(stats.totalAmount),
      byCategory: convertMapValues(stats.byCategory),
      byMonth: convertMapValues(stats.byMonth),
    }
  }, [stats, convertFromBaseCurrency])

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <Navigation />

      <main className="max-w-screen-2xl mx-auto px-3 sm:px-4 lg:px-6 py-5 sm:py-8 lg:py-10">
        <div className="mb-6">
          <h1 className="text-xl sm:text-2xl lg:text-3xl font-bold text-gray-900 dark:text-gray-100">Statistics</h1>
          <p className="text-gray-500 dark:text-gray-300 text-xs sm:text-sm mt-1">Your spending overview</p>
        </div>

        {error && (
          <div className="bg-red-50 dark:bg-red-950 border border-red-200 dark:border-red-900 text-red-700 dark:text-red-300 px-4 py-3 rounded-lg text-sm mb-4">
            {error}
          </div>
        )}

        {rateWarning && (
          <div className="bg-amber-50 dark:bg-amber-950 border border-amber-200 dark:border-amber-900 text-amber-700 dark:text-amber-300 px-4 py-3 rounded-lg text-sm mb-4">
            {rateWarning}
          </div>
        )}

        {loading ? (
          <div className="text-center py-12 text-gray-400 dark:text-gray-500">Loading statistics...</div>
        ) : convertedStats ? (
          <StatisticsChart stats={convertedStats} currency={currency} />
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
