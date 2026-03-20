import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import Dashboard from './pages/Dashboard'
import StatisticsPage from './pages/StatisticsPage'
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

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route
          path="/dashboard"
          element={
            <Protected>
              <Dashboard />
            </Protected>
          }
        />
        <Route
          path="/statistics"
          element={
            <Protected>
              <StatisticsPage />
            </Protected>
          }
        />
        <Route path="/ai/tips" element={<Protected><TipsOverview /></Protected>} />
        <Route path="/ai/tips/spending-pattern" element={<Protected><SpendingPattern /></Protected>} />
        <Route path="/ai/tips/behavioral" element={<Protected><BehavioralAnalysis /></Protected>} />
        <Route path="/ai/tips/benchmarking" element={<Protected><Benchmarking /></Protected>} />
        <Route path="/ai/tips/predictions" element={<Protected><Predictions /></Protected>} />
        <Route path="/ai/tips/anomalies" element={<Protected><AnomalyAlerts /></Protected>} />
        <Route path="/ai/tips/category/:categoryName" element={<Protected><CategoryDeepDive /></Protected>} />
        <Route path="/ai/wellness" element={<Protected><WellnessScore /></Protected>} />
        <Route path="/ai/tips/history-trend" element={<Protected><HistoryTrend /></Protected>} />
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
