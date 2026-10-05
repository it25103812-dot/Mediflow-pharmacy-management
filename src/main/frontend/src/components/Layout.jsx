import { Outlet, useNavigate } from 'react-router-dom'
import Sidebar, { ROLE_TITLES } from './Sidebar'
import { useAuth } from '../context/AuthContext'

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="mf-app">
      <Sidebar />
      <div className="mf-content">
        <header className="mf-topbar">
          <div>
            <span className="fw-semibold">{ROLE_TITLES[user?.roleName] || user?.roleName}</span>
            <span className="text-muted small ms-2 d-none d-md-inline">/ LankaCare Pharmacy (Pvt) Ltd</span>
          </div>
          <div className="d-flex align-items-center gap-3">
            <div className="text-end d-none d-md-block">
              <div className="fw-semibold small">{user?.fullName}</div>
              <div className="text-muted" style={{ fontSize: 12 }}>
                {user?.email}
              </div>
            </div>
            <div
              className="rounded-circle text-white d-flex align-items-center justify-content-center fw-bold"
              style={{ width: 38, height: 38, background: 'var(--mf-accent)' }}
              title={user?.email}
            >
              {(user?.firstName || 'U')[0]}
            </div>
            <button className="btn btn-outline-danger btn-sm" onClick={handleLogout}>
              <i className="bi bi-box-arrow-right me-1"></i>
              Logout
            </button>
          </div>
        </header>
        <main className="mf-main">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
