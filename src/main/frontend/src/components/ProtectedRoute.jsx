import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

/** Blocks unauthenticated users; optionally restricts to a list of roles. */
export default function ProtectedRoute({ roles }) {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return (
      <div className="d-flex justify-content-center align-items-center" style={{ minHeight: '60vh' }}>
        <div className="spinner-border text-primary" role="status" />
      </div>
    )
  }
  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }
  if (roles && !roles.includes(user.roleName)) {
    return (
      <div className="empty-state py-5">
        <i className="bi bi-shield-lock"></i>
        <h5>Access denied</h5>
        <p className="text-muted">Your role ({user.roleName}) cannot open this page.</p>
      </div>
    )
  }
  return <Outlet />
}
