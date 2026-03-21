import api from './api'

const resolveAction = (actionMap, mode, fallbackMode) => actionMap[mode] ?? actionMap[fallbackMode]

export const expenseService = {
  listActive: (params = {}) => api.get('/expenses', { params }).then((response) => response.data),

  listDeleted: () => api.get('/data-management/expenses/deleted').then((response) => response.data),

  create: (payload) => api.post('/expenses', payload).then((response) => response.data),

  update: (id, payload) => api.put(`/expenses/${id}`, payload).then((response) => response.data),

  deleteOne: (id, mode = 'hard') => {
    const action = resolveAction(
      {
        soft: () => api.delete(`/data-management/expenses/${id}`),
        hard: () => api.delete(`/data-management/expenses/${id}/permanent`),
      },
      mode,
      'hard'
    )
    return action()
  },

  deleteMany: (ids, mode = 'soft') => {
    const action = resolveAction(
      {
        soft: () => api.post('/data-management/expenses/bulk-delete', ids),
        hard: () => api.post('/data-management/expenses/bulk-hard-delete', ids),
      },
      mode,
      'soft'
    )
    return action()
  },

  restoreOne: (id) => api.post(`/data-management/expenses/${id}/restore`),

  restoreMany: (ids) => api.post('/data-management/expenses/bulk-restore', ids),
}

