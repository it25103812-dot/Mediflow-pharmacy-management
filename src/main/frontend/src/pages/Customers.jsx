import { useCallback, useEffect, useState } from 'react'
import { Card, Table, Button, Modal, Form, InputGroup } from 'react-bootstrap'
import api, { apiError } from '../services/api'
import { useToast } from '../components/Toast'
import ConfirmDialog from '../components/ConfirmDialog'
import Pagination from '../components/Pagination'
import { sanitizePhone, phoneError, emailError, requiredNameError } from '../utils/validators'

const EMPTY = { name: '', phone: '', email: '', address: '' }

export default function Customers() {
  const toast = useToast()
  const [data, setData] = useState(null)
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [show, setShow] = useState(false)
  const [form, setForm] = useState(EMPTY)
  const [editId, setEditId] = useState(null)
  const [history, setHistory] = useState(null)
  const [confirm, setConfirm] = useState(null)
  const [touched, setTouched] = useState({})

  const errors = {
    name: requiredNameError(form.name, 'Name'),
    phone: phoneError(form.phone),
    email: emailError(form.email),
  }
  const showErr = (k) => (touched[k] ? errors[k] : '')
  const touch = (k) => setTouched((t) => ({ ...t, [k]: true }))
  const openForm = (values, id) => { setForm(values); setEditId(id); setTouched({}); setShow(true) }

  const load = useCallback(() => {
    const params = new URLSearchParams({ page, size: 10 })
    if (search) params.set('search', search)
    api.get('/customers?' + params.toString()).then((r) => setData(r.data)).catch((e) => toast.error(apiError(e)))
  }, [page, search]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { load() }, [load])

  const save = async (e) => {
    e.preventDefault()
    if (Object.values(errors).some(Boolean)) {
      setTouched({ name: true, phone: true, email: true })
      return
    }
    const payload = { ...form, name: form.name.trim(), phone: (form.phone || '').trim(), email: (form.email || '').trim() }
    try {
      if (editId) {
        await api.put('/customers/' + editId, payload)
        toast.success('Customer updated')
      } else {
        await api.post('/customers', payload)
        toast.success('Customer added')
      }
      setShow(false)
      load()
    } catch (err) {
      toast.error(apiError(err))
    }
  }

  const remove = (c) => {
    setConfirm({
      title: 'Delete customer',
      message: `Permanently delete "${c.name}"? Past sales keep a reference and will block this if allowed.`,
      danger: true,
      action: async () => {
        try {
          await api.delete('/customers/' + c.id)
          toast.success('Customer deleted')
          load()
        } catch (err) {
          toast.error(apiError(err))
        }
      },
    })
  }

  const showHistory = async (c) => {
    const res = await api.get('/customers/' + c.id + '/purchases')
    setHistory({ customer: c, sales: res.data })
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h4 className="fw-bold mb-0">Customer Management</h4>
        <Button variant="primary" onClick={() => openForm(EMPTY, null)}>
          <i className="bi bi-person-plus me-2"></i>Add Customer
        </Button>
      </div>

      <Card className="mb-3">
        <Card.Body className="py-3">
          <div className="row">
            <div className="col-md-5">
              <InputGroup>
                <InputGroup.Text><i className="bi bi-search"></i></InputGroup.Text>
                <Form.Control placeholder="Search name, phone or email..." value={search}
                  onChange={(e) => { setSearch(e.target.value); setPage(0) }} />
              </InputGroup>
            </div>
          </div>
        </Card.Body>
      </Card>

      <Card>
        <Card.Body className="p-0">
          <Table responsive hover className="mb-0 align-middle">
            <thead><tr><th>#</th><th>Name</th><th>Phone</th><th>Email</th><th>Address</th><th>Created</th><th></th></tr></thead>
            <tbody>
              {(data?.content || []).map((c) => (
                <tr key={c.id}>
                  <td>{c.id}</td>
                  <td className="fw-semibold clickable" onClick={() => showHistory(c)}>{c.name}</td>
                  <td>{c.phone || '-'}</td>
                  <td>{c.email || '-'}</td>
                  <td className="small text-muted">{c.address || '-'}</td>
                  <td className="small">{(c.createdAt || '').slice(0, 10)}</td>
                  <td className="text-end text-nowrap">
                    <Button size="sm" variant="light" title="Purchase history" className="me-1" onClick={() => showHistory(c)}>
                      <i className="bi bi-receipt"></i>
                    </Button>
                    <Button size="sm" variant="light" title="Edit" className="me-1" onClick={() => openForm({ ...c }, c.id)}>
                      <i className="bi bi-pencil"></i>
                    </Button>
                    <Button size="sm" variant="outline-danger" title="Delete" onClick={() => remove(c)}>
                      <i className="bi bi-trash"></i>
                    </Button>
                  </td>
                </tr>
              ))}
              {data && data.content.length === 0 && (
                <tr><td colSpan="7"><div className="empty-state"><i className="bi bi-person-vcard"></i>No customers found</div></td></tr>
              )}
            </tbody>
          </Table>
        </Card.Body>
      </Card>
      <Pagination page={data?.page || 0} totalPages={data?.totalPages || 0} onChange={setPage} />

      <Modal show={show} onHide={() => setShow(false)} centered>
        <Form onSubmit={save} noValidate>
          <Modal.Header closeButton>
            <Modal.Title className="fs-6">{editId ? 'Edit Customer' : 'New Customer'}</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <Form.Label className="small fw-semibold">Name *</Form.Label>
            <Form.Control required maxLength={120} value={form.name} isInvalid={!!showErr('name')}
              onBlur={() => touch('name')} onChange={(e) => setForm({ ...form, name: e.target.value })} />
            <Form.Control.Feedback type="invalid">{errors.name}</Form.Control.Feedback>
            <div className="row g-3 mt-0">
              <div className="col-md-6">
                <Form.Label className="small fw-semibold">Phone</Form.Label>
                <Form.Control type="tel" inputMode="tel" maxLength={12} placeholder="0771234567" value={form.phone || ''}
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
              <div className="col-12">
                <Form.Label className="small fw-semibold">Address</Form.Label>
                <Form.Control as="textarea" rows={2} maxLength={255} value={form.address || ''} onChange={(e) => setForm({ ...form, address: e.target.value })} />
              </div>
            </div>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="light" onClick={() => setShow(false)}>Cancel</Button>
            <Button type="submit" variant="primary">{editId ? 'Save Changes' : 'Add Customer'}</Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <Modal show={!!history} onHide={() => setHistory(null)} centered size="lg">
        <Modal.Header closeButton>
          <Modal.Title className="fs-6">Purchase History – {history?.customer?.name}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {(history?.sales || []).length === 0 && <div className="empty-state">No purchases yet</div>}
          <Table size="sm">
            <thead><tr><th>Invoice</th><th>Date</th><th>Items</th><th>Total</th><th>Payment</th></tr></thead>
            <tbody>
              {(history?.sales || []).map((s) => (
                <tr key={s.id}>
                  <td className="fw-semibold">{s.invoiceNumber}</td>
                  <td className="small">{(s.saleDate || '').replace('T', ' ').slice(0, 16)}</td>
                  <td>{s.items?.length}</td>
                  <td>LKR {Number(s.total).toFixed(2)}</td>
                  <td>{s.paymentMethod}</td>
                </tr>
              ))}
            </tbody>
          </Table>
        </Modal.Body>
      </Modal>

      <ConfirmDialog show={!!confirm} title={confirm?.title} message={confirm?.message} danger
        confirmLabel="Yes, delete"
        onConfirm={() => { confirm?.action(); setConfirm(null) }}
        onCancel={() => setConfirm(null)} />
    </div>
  )
}
