import { useCallback, useEffect, useState } from 'react'
import { Card, Table, Button, Modal, Form, InputGroup, Badge } from 'react-bootstrap'
import api, { apiError } from '../services/api'
import { useToast } from '../components/Toast'
import ConfirmDialog from '../components/ConfirmDialog'
import Pagination from '../components/Pagination'
import { emailError, personNameError } from '../utils/validators'

const ROLES = [
  'PHARMACIST',
  'STORE_KEEPER',
  'PROCUREMENT_OFFICER',
  'CASHIER',
  'CUSTOMER_RELATIONS_OFFICER',
  'FINANCE_MANAGER',
  'ADMINISTRATOR',
]

const EMPTY = { firstName: '', lastName: '', email: '', password: '', roleName: 'CASHIER', active: true }

export default function Users() {
  const toast = useToast()
  const [data, setData] = useState(null)
  const [search, setSearch] = useState('')
  const [role, setRole] = useState('')
  const [page, setPage] = useState(0)
  const [show, setShow] = useState(false)
  const [form, setForm] = useState(EMPTY)
  const [editId, setEditId] = useState(null)
  const [confirm, setConfirm] = useState(null)
  const [touched, setTouched] = useState({})

  const errors = {
    firstName: personNameError(form.firstName, 'First name'),
    lastName: personNameError(form.lastName, 'Last name'),
    email: (form.email || '').trim() ? emailError(form.email) : 'Email is required',
    password: editId
      ? (form.password && form.password.length < 8 ? 'Password must be at least 8 characters' : '')
      : (!form.password || form.password.length < 8 ? 'Password must be at least 8 characters' : ''),
  }
  const showErr = (k) => (touched[k] ? errors[k] : '')
  const touch = (k) => setTouched((t) => ({ ...t, [k]: true }))

  const load = useCallback(() => {
    const params = new URLSearchParams({ page, size: 10 })
    if (search) params.set('search', search)
    if (role) params.set('role', role)
    api.get('/users?' + params.toString()).then((r) => setData(r.data)).catch((e) => toast.error(apiError(e)))
  }, [page, search, role]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    load()
  }, [load])

  const openCreate = () => {
    setForm(EMPTY)
    setEditId(null)
    setTouched({})
    setShow(true)
  }

  const openEdit = (u) => {
    setForm({ firstName: u.firstName, lastName: u.lastName, email: u.email, password: '', roleName: u.roleName, active: u.active })
    setEditId(u.id)
    setTouched({})
    setShow(true)
  }

  const save = async (e) => {
    e.preventDefault()
    if (Object.values(errors).some(Boolean)) {
      setTouched({ firstName: true, lastName: true, email: true, password: true })
      return
    }
    const payload = { ...form, firstName: form.firstName.trim(), lastName: form.lastName.trim(), email: form.email.trim() }
    try {
      if (editId) {
        await api.put('/users/' + editId, payload)
        toast.success('User updated')
      } else {
        await api.post('/users', payload)
        toast.success('Staff account created')
      }
      setShow(false)
      load()
    } catch (err) {
      toast.error(apiError(err))
    }
  }

  const toggleActive = (u) => {
    setConfirm({
      title: (u.active ? 'Deactivate' : 'Activate') + ' user',
      message: `Are you sure you want to ${u.active ? 'deactivate' : 'activate'} ${u.fullName}?`,
      danger: u.active,
      action: async () => {
        try {
          await api.patch('/users/' + u.id + '/status', { active: !u.active })
          toast.success('User status updated')
          load()
        } catch (err) {
          toast.error(apiError(err))
        }
      },
    })
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h4 className="fw-bold mb-0">User Management</h4>
        <Button variant="primary" onClick={openCreate}>
          <i className="bi bi-person-plus me-2"></i>Add Staff Account
        </Button>
      </div>

      <Card className="mb-3">
        <Card.Body className="py-3">
          <div className="row g-2">
            <div className="col-md-5">
              <InputGroup>
                <InputGroup.Text><i className="bi bi-search"></i></InputGroup.Text>
                <Form.Control
                  placeholder="Search name or email..."
                  value={search}
                  onChange={(e) => { setSearch(e.target.value); setPage(0) }}
                />
              </InputGroup>
            </div>
            <div className="col-md-4">
              <Form.Select value={role} onChange={(e) => { setRole(e.target.value); setPage(0) }}>
                <option value="">All roles</option>
                {ROLES.map((r) => (
                  <option key={r}>{r}</option>
                ))}
              </Form.Select>
            </div>
          </div>
        </Card.Body>
      </Card>

      <Card>
        <Card.Body className="p-0">
          <Table responsive hover className="mb-0 align-middle">
            <thead>
              <tr>
                <th>#</th><th>Name</th><th>Email</th><th>Role</th><th>Status</th><th>Created</th><th></th>
              </tr>
            </thead>
            <tbody>
              {(data?.content || []).map((u) => (
                <tr key={u.id}>
                  <td>{u.id}</td>
                  <td className="fw-semibold">{u.fullName}</td>
                  <td>{u.email}</td>
                  <td><Badge bg="info" className="text-wrap">{u.roleName}</Badge></td>
                  <td>
                    {u.active
                      ? <span className="badge badge-soft-success">ACTIVE</span>
                      : <span className="badge badge-soft-danger">INACTIVE</span>}
                  </td>
                  <td className="text-muted small">{(u.createdAt || '').replace('T', ' ').slice(0, 10)}</td>
                  <td className="text-end">
                    <Button size="sm" variant="light" className="me-1" onClick={() => openEdit(u)}>
                      <i className="bi bi-pencil"></i>
                    </Button>
                    <Button size="sm" variant={u.active ? 'outline-danger' : 'outline-success'} onClick={() => toggleActive(u)}>
                      <i className={`bi ${u.active ? 'bi-person-slash' : 'bi-person-check'}`}></i>
                    </Button>
                  </td>
                </tr>
              ))}
              {data && data.content.length === 0 && (
                <tr><td colSpan="7"><div className="empty-state">No users found</div></td></tr>
              )}
            </tbody>
          </Table>
        </Card.Body>
      </Card>
      <Pagination page={data?.page || 0} totalPages={data?.totalPages || 0} onChange={setPage} />

      <Modal show={show} onHide={() => setShow(false)} centered>
        <Form onSubmit={save} noValidate>
          <Modal.Header closeButton>
            <Modal.Title className="fs-6">{editId ? 'Edit User' : 'New Staff Account'}</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <div className="row g-3">
              <div className="col-6">
                <Form.Label className="small fw-semibold">First name</Form.Label>
                <Form.Control required maxLength={50} value={form.firstName} isInvalid={!!showErr('firstName')}
                  onBlur={() => touch('firstName')} onChange={(e) => setForm({ ...form, firstName: e.target.value })} />
                <Form.Control.Feedback type="invalid">{errors.firstName}</Form.Control.Feedback>
              </div>
              <div className="col-6">
                <Form.Label className="small fw-semibold">Last name</Form.Label>
                <Form.Control required maxLength={50} value={form.lastName} isInvalid={!!showErr('lastName')}
                  onBlur={() => touch('lastName')} onChange={(e) => setForm({ ...form, lastName: e.target.value })} />
                <Form.Control.Feedback type="invalid">{errors.lastName}</Form.Control.Feedback>
              </div>
              <div className="col-12">
                <Form.Label className="small fw-semibold">Email</Form.Label>
                <Form.Control type="email" required maxLength={100} value={form.email} isInvalid={!!showErr('email')}
                  onBlur={() => touch('email')} onChange={(e) => setForm({ ...form, email: e.target.value })} />
                <Form.Control.Feedback type="invalid">{errors.email}</Form.Control.Feedback>
              </div>
              <div className="col-12">
                <Form.Label className="small fw-semibold">
                  {editId ? 'New password (leave blank to keep)' : 'Temporary password'}
                </Form.Label>
                <Form.Control
                  type="text"
                  required={!editId}
                  maxLength={100}
                  value={form.password}
                  placeholder="min 8 characters"
                  isInvalid={!!showErr('password')}
                  onBlur={() => touch('password')}
                  onChange={(e) => setForm({ ...form, password: e.target.value })}
                />
                <Form.Control.Feedback type="invalid">{errors.password}</Form.Control.Feedback>
              </div>
              <div className="col-8">
                <Form.Label className="small fw-semibold">Role</Form.Label>
                <Form.Select value={form.roleName} onChange={(e) => setForm({ ...form, roleName: e.target.value })}>
                  {ROLES.map((r) => (
                    <option key={r}>{r}</option>
                  ))}
                </Form.Select>
              </div>
              <div className="col-4 d-flex align-items-end">
                <Form.Check type="switch" label="Active" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} />
              </div>
            </div>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="light" onClick={() => setShow(false)}>Cancel</Button>
            <Button type="submit" variant="primary">{editId ? 'Save Changes' : 'Create Account'}</Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <ConfirmDialog
        show={!!confirm}
        title={confirm?.title}
        message={confirm?.message}
        danger={confirm?.danger}
        confirmLabel="Yes, continue"
        onConfirm={() => { confirm?.action(); setConfirm(null) }}
        onCancel={() => setConfirm(null)}
      />
    </div>
  )
}
