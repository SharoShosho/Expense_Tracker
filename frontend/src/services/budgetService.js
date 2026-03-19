import api from './api'

export const budgetService = {
  async getBudgets() {
    const response = await api.get('/budgets')
    return response.data
  },

  async saveBudget(category, amount) {
    const response = await api.put('/budgets', { category, amount })
    return response.data
  },

  async deleteBudget(category) {
    await api.delete(`/budgets/${encodeURIComponent(category)}`)
  },

  async getBudgetStatus(month) {
    const response = await api.get('/budgets/status', {
      params: month ? { month } : {},
    })
    return response.data
  },
}

