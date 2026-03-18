import { Link, useNavigate, useLocation } from 'react-router-dom'
import { authService } from '../services/authService'

export default function Navigation() {
  const navigate = useNavigate()
  const location = useLocation()

  const handleLogout = () => {
    authService.logout()
    navigate('/login')
  }

  const user = authService.getCurrentUser()

  const isActive = (path) =>
    location.pathname === path ? 'text-blue-600 font-semibold' : 'text-gray-600 hover:text-blue-600'

  return (
    <nav className="bg-white shadow-sm border-b border-gray-200">
      <div className="max-w-6xl mx-auto px-4 py-3 flex items-center justify-between">
        <div className="flex items-center gap-6">
          <span className="text-xl font-bold text-blue-600">💰 Expense Tracker</span>
          <Link to="/dashboard" className={isActive('/dashboard')}>
            Dashboard
          </Link>
          <Link to="/statistics" className={isActive('/statistics')}>
            Statistics
          </Link>
        </div>
        <div className="flex items-center gap-4">
          <span className="text-sm text-gray-500">{user?.email}</span>
          <button
            onClick={handleLogout}
            className="text-sm px-4 py-2 rounded-lg bg-gray-100 hover:bg-gray-200 text-gray-700 transition"
          >
            Logout
          </button>
        </div>
      </div>
    </nav>
  )
}
