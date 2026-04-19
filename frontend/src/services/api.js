import axios from 'axios'
import { resolveApiBaseUrl } from './apiBaseUrl'

const apiBaseURL = resolveApiBaseUrl()

const api = axios.create({
  baseURL: apiBaseURL,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor to attach JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    const isAuthEndpoint = config.url?.startsWith('/auth/')
    if (token && !isAuthEndpoint) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Response interceptor to handle unauthorized/forbidden responses from stale tokens
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.hash = '#/login'
    }
    return Promise.reject(error)
  }
)

export default api
