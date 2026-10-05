import { useCallback, useEffect, useState } from 'react'
import { Card, Table, Button, Modal, Form, InputGroup } from 'react-bootstrap'
import api, { apiError } from '../services/api'
import { useToast } from '../components/Toast'
import ConfirmDialog from '../components/ConfirmDialog'
import Pagination from '../components/Pagination'
import { useAuth } from '../context/AuthContext'
import { sanitizePhone, phoneError, emailError, requiredNameError, optionalNameError } from '../utils/validators'

const EMPTY = { name: '', contactPerson: '', phone: '', email: '', address: '', active: true }

export default function Suppliers() {
  const toast = useToast()
  const { user } = useAuth()
  const [data, setData] = useState(null)
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [show, setShow] = useState(false)
  const [form, setForm] = useState(EMPTY)
  const [editId, setEditId] = useState(null)
  const [confirm, setConfirm] = useState(null)
  const [touched, setTouched] = useState({})

  const errors = {
    name: requiredNameError(form.name, 'Supplier name'),
    contactPerson: optionalNameError(form.contactPerson, 'Contact person'),
    phone: phoneError(form.phone),
    email: emailError(form.email),
  }
  const showErr = (k) => (touched[k] ? errors[k] : '')
  const touch = (k) => setTouched((t) => ({ ...t, [k]: true }))

  const load = useCallback(() => {
    const params = new URLSearchParams({ page, size: 10 })
    if (search) params.set('search', search)
    api.get('/suppliers?' + params.toString()).then((r) => setData(r.data)).catch((e) => toast.error(apiError(e)))
  }, [page, search]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { load() }, [load])

  const save = async (e) => {
    e.preventDefault()
    if (Object.values(errors).some(Boolean)) {
      setTouched({ name: true, contactPerson: true, phone: true, email: true })
      return
    }
    const payload = {
      ...form,
      name: form.name.trim(),
      contactPerson: (form.contactPerson || '').trim(),
      phone: (form.phone || '').trim(),
      email: (form.email || '').trim(),
    }
    try {
      if (editId) {
        await api.put('/suppliers/' + editId, payload)
        toast.success('Supplier updated')
      } else {
        await api.post('/suppliers', payload)
        toast.success('Supplier added')
      }
      setShow(false)
      load()
    } catch (err) {
      toast.error(apiError(err))
    }
  }

  const remove = (s) => {
    setConfirm({
      title: 'Delete supplier',
      message: `Delete "${s.name}"? If it already has purchase orders or GRNs, it will be deactivated instead.`,
      danger: true,
      action: async () => {
        try {
          const res = await api.delete('/suppliers/' + s.id)
          toast.success(res.data?.deleted ? 'Supplier deleted' : 'Supplier has history, so it was deactivated instead')
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
        <h4 className="fw-bold mb-0">Supplier Management</h4>
        <Button variant="primary" onClick={() => { setForm(EMPTY); setEditId(null); setTouched({}); setShow(true) }}>
          <i className="bi bi-truck me-2"></i>Add Supplier
        </Button>
      </div>

      <Card className="mb-3">
        <Card.Body className="py-3">
          <div className="row">
            <div className="col-md-5">
              <InputGroup>
                <InputGroup.Text><i className="bi bi-search"></i></InputGroup.Text>
                <Form.Control placeholder="Search supplier or contact person..." value={search}
                  onChange={(e) => { setSearch(e.target.value); setPage(0) }} />
              </InputGroup>
            </div>
          </div>
        </Card.Body>
      </Card>

      <Card>
        <Card.Body className="p-0">
          <Table responsive hover className="mb-0 align-middle">
            <thead><tr><th>#</th><th>Supplier</th><th>Contact Person</th><th>Phone</th><th>Email</th><th>Address</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {(data?.content || []).map((s) => (
                <tr key={s.id}>
                  <td>{s.id}</td>
                  <td className="fw-semibold">{s.name}</td>
                  <td>{s.contactPerson || '-'}</td>
                  <td>{s.phone || '-'}</td>
                  <td>{s.email || '-'}</td>
                  <td className="small text-muted">{s.address || '-'}</td>
                  <td>{s.active ? <span className="badge badge-soft-success">ACTIVE</span> : <span className="badge badge-soft-danger">INACTIVE</span>}</td>
                  <td className="text-end text-nowrap">
                    <Button size="sm" variant="light" className="me-1" onClick={() => { setForm({ ...s, active: !!s.active }); setEditId(s.id); setTouched({}); setShow(true) }}>
                      <i className="bi bi-pencil"></i>
                    </Button>
                    {user?.roleName === 'ADMINISTRATOR' && (
                      <Button size="sm" variant="outline-danger" onClick={() => remove(s)}>
                        <i className="bi bi-trash"></i>
                      </Button>
                    )}
                  </td>
                </tr>
              ))}
              {data && data.content.length === 0 && (
                <tr><td colSpan="8"><div className="empty-state"><i className="bi bi-truck"></i>No suppliers found</div></td></tr>
              )}
            </tbody>
          </Table>
        </Card.Body>
      </Card>
      <Pagination page={data?.page || 0} totalPages={data?.totalPages || 0} onChange={setPage} />

      <Modal show={show} onHide={() => setShow(false)} centered>
        <Form onSubmit={save} noValidate>
          <Modal.Header closeButton>
            <Modal.Title className="fs-6">{editId ? 'Edit Supplier' : 'New Supplier'}</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <div className="row g-3">
              <div className="col-12">
                <Form.Label className="small fw-semibold">Supplier name *</Form.Label>
                <Form.Control required maxLength={150} value={form.name} isInvalid={!!showErr('name')}
                  onBlur={() => touch('name')} onChange={(e) => setForm({ ...form, name: e.target.value })} />
                <Form.Control.Feedback type="invalid">{errors.name}</Form.Control.Feedback>
              </div>
              <div className="col-md-6">
                <Form.Label className="small fw-semibold">Contact person</Form.Label>
                <Form.Control maxLength={100} value={form.contactPerson || ''} isInvalid={!!showErr('contactPerson')}
                  onBlur={() => touch('contactPerson')} onChange={(e) => setForm({ ...form, contactPerson: e.target.value })} />
                <Form.Control.Feedback type="invalid">{errors.contactPerson}</Form.Control.Feedback>
              </div>
              <div className="col-md-6">
                <Form.Label className="small fw-semibold">Phone</Form.Label>
                <Form.Control type="tel" inputMode="tel" maxLength={12} placeholder="0112345678" value={form.phone || ''}
                  isInvalid={!!showErr('phone')} onBlur={() => touch('phone')}
                  onChange={(e) => setForm({ ...form, phone: sanitizePhone(e.target.value) })} />
                <Form.Control.Feedback type="invalid">{errors.phone}</Form.Control.Feedback>
              </div>
              <div className="col-md-6">
                <Form.Label className="small fw-semibold">Email</Form.Label>
                <Form.Control type="email" maxLength={100} value={form.email || ''} isInvalid={!!showErr('email')}
                  onBlur={() => touch('email')} onChange={(e) => setForm({ ...form, email: e.target.value })} />
                <Form.Control.Feedback type="invalid">{errors.email}</Form.Control.Feedback>
              </div>
              <div className="col-md-6 d-flex align-items-end">
                <Form.Check type="switch" label="Active" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} />
              </div>
              <div className="col-12">
                <Form.Label className="small fw-semibold">Address</Form.Label>
                <Form.Control as="textarea" rows={2} maxLength={255} value={form.address || ''} onChange={(e) => setForm({ ...form, address: e.target.value })} />
              </div>
            </div>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="light" onClick={() => setShow(false)}>Cancel</Button>
            <Button type="submit" variant="primary">{editId ? 'Save Changes' : 'Add Supplier'}</Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <ConfirmDialog show={!!confirm} title={confirm?.title} message={confirm?.message} danger
        confirmLabel="Yes, delete"
        onConfirm={() => { confirm?.action(); setConfirm(null) }}
        onCancel={() => setConfirm(null)} />
    </div>
  )
}
