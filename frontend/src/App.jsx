import { createBrowserRouter, Navigate, RouterProvider } from 'react-router-dom'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import Dashboard from './pages/Dashboard'
import StatisticsPage from './pages/StatisticsPage'
import BudgetPage from './pages/BudgetPage'
import ProtectedRoute from './components/ProtectedRoute'
import TipsOverview from './components/AI/TipsOverview'
import SpendingPattern from './components/AI/SpendingPattern'
import BehavioralAnalysis from './components/AI/BehavioralAnalysis'
import Benchmarking from './components/AI/Benchmarking'
import Predictions from './components/AI/Predictions'
import AnomalyAlerts from './components/AI/AnomalyAlerts'
import CategoryDeepDive from './components/AI/CategoryDeepDive'
import WellnessScore from './components/AI/WellnessScore'
import HistoryTrend from './components/AI/HistoryTrend'

function Protected({ children }) {
  return <ProtectedRoute>{children}</ProtectedRoute>
}

const router = createBrowserRouter(
  [
    { path: '/login', element: <LoginPage /> },
    { path: '/register', element: <RegisterPage /> },
    {
      path: '/dashboard',
      element: (
        <Protected>
          <Dashboard />
        </Protected>
      ),
    },
    {
      path: '/statistics',
      element: (
        <Protected>
          <StatisticsPage />
        </Protected>
      ),
    },
    {
      path: '/budget/setup',
      element: (
        <Protected>
          <BudgetPage />
        </Protected>
      ),
    },
    { path: '/ai/tips', element: <Protected><TipsOverview /></Protected> },
    { path: '/ai/tips/spending-pattern', element: <Protected><SpendingPattern /></Protected> },
    { path: '/ai/tips/behavioral', element: <Protected><BehavioralAnalysis /></Protected> },
    { path: '/ai/tips/benchmarking', element: <Protected><Benchmarking /></Protected> },
    { path: '/ai/tips/predictions', element: <Protected><Predictions /></Protected> },
    { path: '/ai/tips/anomalies', element: <Protected><AnomalyAlerts /></Protected> },
    { path: '/ai/tips/category/:categoryName', element: <Protected><CategoryDeepDive /></Protected> },
    { path: '/ai/wellness', element: <Protected><WellnessScore /></Protected> },
    { path: '/ai/tips/history-trend', element: <Protected><HistoryTrend /></Protected> },
    { path: '/', element: <Navigate to="/dashboard" replace /> },
  ],
  {
    future: {
      v7_startTransition: true,
      v7_relativeSplatPath: true,
    },
  },
)

export default function App() {
  return <RouterProvider router={router} />
}
