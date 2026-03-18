import {
  PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis, Tooltip,
  Legend, ResponsiveContainer,
} from 'recharts'
import { formatCurrency } from '../services/currencyService'

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#f97316', '#6b7280']

export default function StatisticsChart({ stats, currency = 'EUR' }) {
  if (!stats) return null

  const categoryData = Object.entries(stats.byCategory || {}).map(([name, value]) => ({
    name,
    value: parseFloat(value),
  }))

  const monthData = Object.entries(stats.byMonth || {})
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([name, value]) => ({
      name,
      amount: parseFloat(value),
    }))

  return (
    <div className="space-y-8">
      <div className="grid grid-cols-2 gap-4">
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4 text-center">
          <p className="text-sm text-gray-500 dark:text-gray-300">Total Spent</p>
          <p className="text-3xl font-bold text-blue-600">
            {formatCurrency(stats.totalAmount || 0, currency)}
          </p>
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4 text-center">
          <p className="text-sm text-gray-500 dark:text-gray-300">Total Expenses</p>
          <p className="text-3xl font-bold text-green-600">{stats.totalCount || 0}</p>
        </div>
      </div>

      {categoryData.length > 0 && (
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
          <h3 className="text-lg font-semibold text-gray-800 dark:text-gray-100 mb-4">Spending by Category</h3>
          <ResponsiveContainer width="100%" height={280}>
            <PieChart>
              <Pie
                data={categoryData}
                cx="50%"
                cy="50%"
                outerRadius={100}
                dataKey="value"
                label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
              >
                {categoryData.map((_, index) => (
                  <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip formatter={(value) => formatCurrency(value, currency)} />
            </PieChart>
          </ResponsiveContainer>
        </div>
      )}

      {monthData.length > 0 && (
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
          <h3 className="text-lg font-semibold text-gray-800 dark:text-gray-100 mb-4">Monthly Spending</h3>
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={monthData}>
              <XAxis dataKey="name" />
              <YAxis />
              <Tooltip formatter={(value) => formatCurrency(value, currency)} />
              <Legend />
              <Bar dataKey="amount" fill="#3b82f6" name={`Amount (${currency})`} radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  )
}
