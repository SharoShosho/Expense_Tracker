import api from './api'

export const aiService = {
  async getOverview() {
    const response = await api.get('/ai/tips/overview')
    return response.data
  },

  async getSpendingPattern() {
    const response = await api.get('/ai/tips/spending-pattern')
    return response.data
  },

  async getBehavioral() {
    const response = await api.get('/ai/tips/behavioral')
    return response.data
  },

  async getBenchmarking() {
    const response = await api.get('/ai/tips/benchmarking')
    return response.data
  },

  async getPredictions() {
    const response = await api.get('/ai/tips/predictions')
    return response.data
  },

  async getAnomalies() {
    const response = await api.get('/ai/tips/anomalies')
    return response.data
  },

  async getCategoryDeepDive(categoryName) {
    const response = await api.get(`/ai/tips/category/${encodeURIComponent(categoryName)}`)
    return response.data
  },

  async getWellnessScore() {
    const response = await api.get('/ai/tips/wellness-score')
    return response.data
  },

  async getHistoryTrend() {
    const response = await api.get('/ai/tips/history-trend')
    return response.data
  },
}
