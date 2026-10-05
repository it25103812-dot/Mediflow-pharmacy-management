import { useState } from 'react'
import { Card, Form, Button } from 'react-bootstrap'
import api, { apiError } from '../services/api'
import { useToast } from '../components/Toast'
import { useAuth } from '../context/AuthContext'
import { ROLE_TITLES } from '../components/Sidebar'

export default function Profile() {
  const toast = useToast()
  const { user } = useAuth()
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    if (newPassword !== confirmPassword) {
      toast.error('New passwords do not match')
      return
    }
    setBusy(true)
    try {
      await api.put('/auth/password', { currentPassword, newPassword })
      toast.success('Password changed successfully')
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
    } catch (err) {
      toast.error(apiError(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="row justify-content-center">
      <div className="col-lg-5">
        <Card className="mb-4">
          <Card.Header>My Profile</Card.Header>
          <Card.Body>
            <div className="d-flex align-items-center gap-3 mb-3">
              <div className="rounded-circle text-white d-flex align-items-center justify-content-center fw-bold"
                style={{ width: 64, height: 64, background: 'var(--mf-accent)', fontSize: 24 }}>
                {(user?.firstName || 'U')[0]}
              </div>
              <div>
                <h5 className="mb-0 fw-bold">{user?.fullName}</h5>
                <div className="text-muted small">{user?.email}</div>
                <span className="badge badge-soft-info mt-1">{ROLE_TITLES[user?.roleName] || user?.roleName}</span>
              </div>
            </div>
            <hr />
            <div className="small text-muted">
              <div className="d-flex justify-content-between py-1">
                <span>Status</span><span className="fw-semibold text-success">ACTIVE</span>
              </div>
              <div className="d-flex justify-content-between py-1">
                <span>Member since</span><span className="fw-semibold">{(user?.createdAt || '').slice(0, 10)}</span>
              </div>
            </div>
          </Card.Body>
        </Card>

        <Card>
          <Card.Header>Change Password</Card.Header>
          <Card.Body>
            <Form onSubmit={submit}>
              <Form.Group className="mb-3">
                <Form.Label className="small fw-semibold">Current password</Form.Label>
                <Form.Control type="password" required value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)} />
              </Form.Group>
              <Form.Group className="mb-3">
                <Form.Label className="small fw-semibold">New password (min 8 chars)</Form.Label>
                <Form.Control type="password" required minLength={8} value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)} />
              </Form.Group>
              <Form.Group className="mb-3">
                <Form.Label className="small fw-semibold">Confirm new password</Form.Label>
                <Form.Control type="password" required minLength={8} value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)} />
              </Form.Group>
              <Button type="submit" variant="primary" disabled={busy}>
                {busy ? <span className="spinner-border spinner-border-sm me-2"></span> : <i className="bi bi-key me-2"></i>}
                Update Password
              </Button>
            </Form>
          </Card.Body>
        </Card>
      </div>
    </div>
  )
}
