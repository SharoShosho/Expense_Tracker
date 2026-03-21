import {
  PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis, Tooltip,
  Legend, ResponsiveContainer,
} from 'recharts'
import { useEffect, useState } from 'react'
import { formatCurrency } from '../services/currencyService'

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#f97316', '#6b7280']
const STATUS_BAR_COLORS = {
  SAFE: 'bg-green-500',
  NEAR_LIMIT: 'bg-amber-500',
  EXCEEDED: 'bg-red-500',
}
const STATUS_TEXT_COLORS = {
  SAFE: 'text-green-700 dark:text-green-300',
  NEAR_LIMIT: 'text-amber-700 dark:text-amber-300',
  EXCEEDED: 'text-red-700 dark:text-red-300',
}
const STATUS_ORDER = { EXCEEDED: 0, NEAR_LIMIT: 1, SAFE: 2 }

export default function StatisticsChart({ stats, budgetOverview = null, currency = 'EUR' }) {
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

  const budgetCategories = (budgetOverview?.categories || [])
    .map((item) => ({
      ...item,
      usagePercent: Number(item.usagePercent || 0),
      statusRank: STATUS_ORDER[item.status] ?? 3,
    }))
    .sort((a, b) => (a.statusRank - b.statusRank) || (Number(b.usagePercent) - Number(a.usagePercent)))
    .slice(0, 4)

  const budgetComparisonData = (budgetOverview?.categories || [])
    .map((item) => ({
      category: item.category,
      budgetAmount: Number(item.budgetAmount || 0),
      spentAmount: Number(item.spentAmount || 0),
    }))
    .filter((item) => item.budgetAmount > 0 || item.spentAmount > 0)
    .sort((a, b) => (b.spentAmount - a.spentAmount))
    .slice(0, 8)

  const totalBudget = Number(budgetOverview?.totalBudget || 0)
  const totalSpent = Number(budgetOverview?.totalSpent || 0)
  const budgetUsagePercent = totalBudget > 0 ? (totalSpent / totalBudget) * 100 : 0
  const overallBudgetStatus =
    totalBudget <= 0
      ? 'SAFE'
      : budgetUsagePercent >= 100
        ? 'EXCEEDED'
        : budgetUsagePercent >= 80
          ? 'NEAR_LIMIT'
          : 'SAFE'
  const budgetAlertCount = (budgetOverview?.nearLimitCount || 0) + (budgetOverview?.exceededCount || 0)

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

      {budgetOverview && (
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4 sm:p-6">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between mb-4">
            <h3 className="text-lg font-semibold text-gray-800 dark:text-gray-100">Budget Statistics ({budgetOverview.month})</h3>
            {budgetAlertCount > 0 && (
              <span className="text-xs font-medium px-2 py-1 rounded-full bg-amber-100 dark:bg-amber-900 text-amber-800 dark:text-amber-200">
                {budgetAlertCount} alert{budgetAlertCount !== 1 ? 's' : ''}
              </span>
            )}
          </div>

          {!budgetCategories.length ? (
            <p className="text-sm text-gray-500 dark:text-gray-400">No budget data available for this month.</p>
          ) : (
            <>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-4">
                <div className="rounded-lg bg-gray-50 dark:bg-gray-900/40 p-3">
                  <p className="text-xs text-gray-500 dark:text-gray-400">Total Budget</p>
                  <p className="text-base sm:text-lg font-semibold text-blue-600 dark:text-blue-400">
                    {formatCurrency(totalBudget, currency)}
                  </p>
                </div>
                <div className="rounded-lg bg-gray-50 dark:bg-gray-900/40 p-3">
                  <p className="text-xs text-gray-500 dark:text-gray-400">Total Spent</p>
                  <p className="text-base sm:text-lg font-semibold text-gray-900 dark:text-gray-100">
                    {formatCurrency(totalSpent, currency)}
                  </p>
                </div>
                <div className="rounded-lg bg-gray-50 dark:bg-gray-900/40 p-3">
                  <p className="text-xs text-gray-500 dark:text-gray-400">Exceeded Categories</p>
                  <p className="text-base sm:text-lg font-semibold text-red-600 dark:text-red-400">
                    {budgetOverview.exceededCount || 0}
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-3 mb-4">
                <div className="flex-1 h-2.5 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
                  <div
                    className={`h-full transition-all ${STATUS_BAR_COLORS[overallBudgetStatus]}`}
                    style={{ width: `${Math.min(100, budgetUsagePercent)}%` }}
                    aria-hidden="true"
                  />
                </div>
                <span className={`text-xs font-semibold ${STATUS_TEXT_COLORS[overallBudgetStatus]}`}>
                  {budgetUsagePercent.toFixed(1)}%
                </span>
              </div>

              <div className="space-y-2">
                {budgetCategories.map((item) => (
                  <div
                    key={item.category}
                    className="flex items-center justify-between rounded-lg border border-gray-200 dark:border-gray-700 px-3 py-2"
                  >
                    <div>
                      <p className="text-sm font-medium text-gray-800 dark:text-gray-100">{item.category}</p>
                      <p className="text-xs text-gray-500 dark:text-gray-400">
                        {formatCurrency(item.spentAmount, currency)} / {formatCurrency(item.budgetAmount, currency)}
                      </p>
                    </div>
                    <span className={`text-xs font-semibold ${STATUS_TEXT_COLORS[item.status] || 'text-gray-500'}`}>
                      {item.status}
                    </span>
                  </div>
                ))}
              </div>

              {budgetComparisonData.length > 0 && (
                <div className="mt-5">
                  <h4 className="text-sm font-semibold text-gray-700 dark:text-gray-200 mb-3">
                    Budget vs Spent by Category
                  </h4>
                  <div className="h-64 sm:h-72">
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart
                        data={budgetComparisonData}
                        margin={{ top: 8, right: 12, left: 0, bottom: isSmallScreen ? 38 : 12 }}
                      >
                        <XAxis
                          dataKey="category"
                          tick={{ fontSize: isSmallScreen ? 10 : 12 }}
                          tickMargin={8}
                          minTickGap={isSmallScreen ? 20 : 12}
                          angle={isSmallScreen ? -30 : 0}
                          textAnchor={isSmallScreen ? 'end' : 'middle'}
                          height={isSmallScreen ? 50 : 30}
                        />
                        <YAxis tick={{ fontSize: isSmallScreen ? 10 : 12 }} width={isSmallScreen ? 42 : 52} />
                        <Tooltip formatter={(value) => formatCurrency(value, currency)} />
                        {!isSmallScreen && <Legend />}
                        <Bar dataKey="budgetAmount" fill="#10b981" name={`Budget (${currency})`} radius={[4, 4, 0, 0]} />
                        <Bar dataKey="spentAmount" fill="#3b82f6" name={`Spent (${currency})`} radius={[4, 4, 0, 0]} />
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      )}

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
