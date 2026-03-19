import {
  PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis, Tooltip,
  Legend, ResponsiveContainer,
} from 'recharts'
import { useEffect, useState } from 'react'
import { formatCurrency } from '../services/currencyService'

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#f97316', '#6b7280']

export default function StatisticsChart({ stats, currency = 'EUR' }) {
  if (!stats) return null

  const [isSmallScreen, setIsSmallScreen] = useState(false)

  useEffect(() => {
    const mediaQuery = window.matchMedia('(max-width: 640px)')
    const updateScreenSize = () => setIsSmallScreen(mediaQuery.matches)
    updateScreenSize()
    mediaQuery.addEventListener('change', updateScreenSize)

    return () => mediaQuery.removeEventListener('change', updateScreenSize)
  }, [])

  const categoryData = Object.entries(stats.byCategory || {}).map(([name, value]) => ({
    name,
    value: Number(value),
  })).filter((item) => Number.isFinite(item.value) && item.value > 0)

  const monthData = Object.entries(stats.byMonth || {})
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([name, value]) => ({
      name,
      amount: Number(value),
    }))
    .filter((item) => Number.isFinite(item.amount) && item.amount > 0)

  const pieLabel = isSmallScreen
    ? false
    : ({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`

  return (
    <div className="space-y-8">
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4 text-center">
          <p className="text-xs sm:text-sm text-gray-500 dark:text-gray-300">Total Spent</p>
          <p className="text-2xl sm:text-3xl font-bold text-blue-600">
            {formatCurrency(stats.totalAmount || 0, currency)}
          </p>
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4 text-center">
          <p className="text-xs sm:text-sm text-gray-500 dark:text-gray-300">Total Expenses</p>
          <p className="text-2xl sm:text-3xl font-bold text-green-600">{stats.totalCount || 0}</p>
        </div>
      </div>

      {categoryData.length > 0 && (
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
          <h3 className="text-lg font-semibold text-gray-800 dark:text-gray-100 mb-4">Spending by Category</h3>
          <div className="h-60 sm:h-72 lg:h-80">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={categoryData}
                  cx="50%"
                  cy="50%"
                  outerRadius={isSmallScreen ? 72 : 90}
                  dataKey="value"
                  label={pieLabel}
                  labelLine={!isSmallScreen}
                >
                  {categoryData.map((_, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip formatter={(value) => formatCurrency(value, currency)} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}

      {monthData.length > 0 && (
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
          <h3 className="text-lg font-semibold text-gray-800 dark:text-gray-100 mb-4">Monthly Spending</h3>
          <div className="h-60 sm:h-72 lg:h-80">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={monthData} margin={{ top: 8, right: 12, left: 0, bottom: isSmallScreen ? 28 : 12 }}>
                <XAxis
                  dataKey="name"
                  tick={{ fontSize: isSmallScreen ? 10 : 12 }}
                  tickMargin={8}
                  minTickGap={isSmallScreen ? 20 : 12}
                  angle={isSmallScreen ? -30 : 0}
                  textAnchor={isSmallScreen ? 'end' : 'middle'}
                  height={isSmallScreen ? 48 : 30}
                />
                <YAxis tick={{ fontSize: isSmallScreen ? 10 : 12 }} width={isSmallScreen ? 42 : 52} />
                <Tooltip formatter={(value) => formatCurrency(value, currency)} />
                {!isSmallScreen && <Legend />}
                <Bar dataKey="amount" fill="#3b82f6" name={`Amount (${currency})`} radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}
    </div>
  )
}
