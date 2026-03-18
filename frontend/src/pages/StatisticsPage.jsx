import { useState, useEffect, useMemo } from 'react'
import Navigation from '../components/Navigation'
import StatisticsChart from '../components/StatisticsChart'
import api from '../services/api'
import { DEFAULT_CURRENCY, getPreferredCurrency, onCurrencyChange } from '../services/currencyService'
import { convertAmount, getExchangeRate } from '../services/exchangeRateService'

export default function StatisticsPage() {
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [currency, setCurrency] = useState(getPreferredCurrency())
  const [exchangeRate, setExchangeRate] = useState(1)
  const [rateWarning, setRateWarning] = useState('')

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

  useEffect(() => {
    let mounted = true

    const loadExchangeRate = async () => {
      try {
        const nextRate = await getExchangeRate(DEFAULT_CURRENCY, currency)
        if (mounted) {
          setExchangeRate(nextRate)
          setRateWarning('')
        }
      } catch {
        if (mounted) {
          setExchangeRate(1)
          setRateWarning('Could not load live exchange rate. Statistics are shown in EUR values.')
        }
      }
    }

    loadExchangeRate()
    return () => {
      mounted = false
    }
  }, [currency])

  const convertedStats = useMemo(() => {
    if (!stats) {
      return null
    }

    const convertMapValues = (input) => Object.fromEntries(
      Object.entries(input || {}).map(([key, value]) => [key, convertAmount(value, exchangeRate)])
    )

    return {
      ...stats,
      totalAmount: convertAmount(stats.totalAmount, exchangeRate),
      byCategory: convertMapValues(stats.byCategory),
      byMonth: convertMapValues(stats.byMonth),
    }
  }, [stats, exchangeRate])

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
