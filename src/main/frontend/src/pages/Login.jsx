import { useState } from 'react'
import { useNavigate, useLocation, Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { apiError } from '../services/api'

export default function Login() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to="/" replace />

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      await login(email.trim(), password)
      navigate(location.state?.from?.pathname || '/', { replace: true })
    } catch (err) {
      setError(apiError(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="mf-login">
      <div className="login-card bg-white">
        <div className="login-head">
          <div className="login-logo">
            <i className="bi bi-heart-pulse-fill"></i>
          </div>
          <h4 className="fw-bold mb-0">MediFlow</h4>
          <p className="text-muted small mb-3">Pharmacy Management System</p>
        </div>
        <div className="p-4 p-md-5 pt-3">
          {error && (
            <div className="alert alert-danger py-2 small" role="alert">
              <i className="bi bi-exclamation-octagon-fill me-2"></i>
              {error}
            </div>
          )}
          <form onSubmit={submit}>
            <div className="mb-3">
              <label className="form-label small fw-semibold">Email</label>
              <div className="input-group">
                <span className="input-group-text bg-light">
                  <i className="bi bi-envelope"></i>
                </span>
                <input
                  type="email"
                  className="form-control"
                  placeholder="you@mediflow.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  autoFocus
                />
              </div>
            </div>
            <div className="mb-4">
              <label className="form-label small fw-semibold">Password</label>
              <div className="input-group">
                <span className="input-group-text bg-light">
                  <i className="bi bi-lock"></i>
                </span>
                <input
                  type="password"
                  className="form-control"
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
              </div>
            </div>
            <button className="btn btn-primary w-100 py-2 fw-semibold" disabled={busy}>
              {busy ? (
                <>
                  <span className="spinner-border spinner-border-sm me-2"></span>
                  Signing in...
                </>
              ) : (
                <>
                  <i className="bi bi-box-arrow-in-right me-2"></i>Sign In
                </>
              )}
            </button>
          </form>
          <hr className="my-4" />
          <p className="text-center text-muted small mb-1 fw-semibold">Demo accounts (password: Admin@123)</p>
          <div className="row g-1 small">
            {[
              ['admin@mediflow.com', 'Administrator'],
              ['pharmacist@mediflow.com', 'Pharmacist'],
              ['storekeeper@mediflow.com', 'Store Keeper'],
              ['procurement@mediflow.com', 'Procurement'],
              ['cashier@mediflow.com', 'Cashier'],
              ['finance@mediflow.com', 'Finance'],
            ].map(([mail, label]) => (
              <div className="col-6" key={mail}>
                <button
                  type="button"
                  className="btn btn-outline-secondary btn-sm w-100 text-truncate"
                  onClick={() => {
                    setEmail(mail)
                    setPassword('Admin@123')
                  }}
                >
                  {label}
                </button>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
