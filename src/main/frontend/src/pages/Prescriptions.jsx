import { useCallback, useEffect, useState } from 'react'
import { Card, Table, Button, Modal, Form, InputGroup } from 'react-bootstrap'
import api, { apiError } from '../services/api'
import { useToast } from '../components/Toast'
import Pagination from '../components/Pagination'

const EMPTY = { customerId: '', doctorName: '', prescriptionDate: '', medicineId: '', notes: '' }

export default function Prescriptions() {
  const toast = useToast()
  const [data, setData] = useState(null)
  const [customers, setCustomers] = useState([])
  const [medicines, setMedicines] = useState([])
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [show, setShow] = useState(false)
  const [form, setForm] = useState(EMPTY)
  const [editId, setEditId] = useState(null)

  const load = useCallback(() => {
    const params = new URLSearchParams({ page, size: 10 })
    if (search) params.set('search', search)
    api.get('/prescriptions?' + params.toString()).then((r) => setData(r.data)).catch((e) => toast.error(apiError(e)))
  }, [page, search]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    load()
    api.get('/customers?size=200').then((r) => setCustomers(r.data.content || [])).catch(() => {})
    api.get('/medicines?size=200&prescriptionRequired=true').then((r) => setMedicines(r.data.content || [])).catch(() => {})
  }, [load])

  const save = async (e) => {
    e.preventDefault()
    try {
      const payload = {
        ...form,
        customerId: form.customerId ? Number(form.customerId) : null,
        medicineId: form.medicineId ? Number(form.medicineId) : null,
      }
      if (editId) {
        await api.put('/prescriptions/' + editId, payload)
        toast.success('Prescription updated')
      } else {
        await api.post('/prescriptions', payload)
        toast.success('Prescription recorded')
      }
      setShow(false)
      load()
    } catch (err) {
      toast.error(apiError(err))
    }
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h4 className="fw-bold mb-0">Prescription Management</h4>
        <Button variant="primary" onClick={() => { setForm({ ...EMPTY, prescriptionDate: new Date().toISOString().slice(0, 10) }); setEditId(null); setShow(true) }}>
          <i className="bi bi-clipboard2-plus me-2"></i>New Prescription
        </Button>
      </div>

      <Card className="mb-3">
        <Card.Body className="py-3">
          <div className="row">
            <div className="col-md-5">
              <InputGroup>
                <InputGroup.Text><i className="bi bi-search"></i></InputGroup.Text>
                <Form.Control placeholder="Search prescription number or doctor..." value={search}
                  onChange={(e) => { setSearch(e.target.value); setPage(0) }} />
              </InputGroup>
            </div>
          </div>
        </Card.Body>
      </Card>

      <Card>
        <Card.Body className="p-0">
          <Table responsive hover className="mb-0 align-middle">
            <thead><tr><th>#</th><th>Number</th><th>Customer</th><th>Doctor</th><th>Date</th><th>Medicine</th><th>Notes</th><th></th></tr></thead>
            <tbody>
              {(data?.content || []).map((p) => (
                <tr key={p.id}>
                  <td>{p.id}</td>
                  <td className="fw-semibold">{p.prescriptionNumber}</td>
                  <td>{p.customer?.name || <span className="text-muted">-</span>}</td>
                  <td>{p.doctorName || '-'}</td>
                  <td className="small">{p.prescriptionDate}</td>
                  <td className="small">{p.medicine?.name || '-'}</td>
                  <td className="small text-muted" style={{ maxWidth: 220 }}>{p.notes || '-'}</td>
                  <td className="text-end">
                    <Button size="sm" variant="light" onClick={() => {
                      setForm({
                        customerId: p.customer?.id || '', doctorName: p.doctorName || '',
                        prescriptionDate: p.prescriptionDate || '', medicineId: p.medicine?.id || '',
                        notes: p.notes || '',
                      })
                      setEditId(p.id)
                      setShow(true)
                    }}>
                      <i className="bi bi-pencil"></i>
                    </Button>
                  </td>
                </tr>
              ))}
              {data && data.content.length === 0 && (
                <tr><td colSpan="8"><div className="empty-state"><i className="bi bi-clipboard2-pulse"></i>No prescriptions recorded</div></td></tr>
              )}
            </tbody>
          </Table>
        </Card.Body>
      </Card>
      <Pagination page={data?.page || 0} totalPages={data?.totalPages || 0} onChange={setPage} />

      <Modal show={show} onHide={() => setShow(false)} centered>
        <Form onSubmit={save}>
          <Modal.Header closeButton>
            <Modal.Title className="fs-6">{editId ? 'Edit Prescription' : 'New Prescription'}</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <div className="row g-3">
              <div className="col-md-6">
                <Form.Label className="small fw-semibold">Customer</Form.Label>
                <Form.Select value={form.customerId} onChange={(e) => setForm({ ...form, customerId: e.target.value })}>
                  <option value="">- select -</option>
                  {customers.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                </Form.Select>
              </div>
              <div className="col-md-6">
                <Form.Label className="small fw-semibold">Doctor name *</Form.Label>
                <Form.Control required maxLength={120} value={form.doctorName} onChange={(e) => setForm({ ...form, doctorName: e.target.value })} />
              </div>
              <div className="col-md-6">
                <Form.Label className="small fw-semibold">Prescription date</Form.Label>
                <Form.Control type="date" value={form.prescriptionDate || ''} onChange={(e) => setForm({ ...form, prescriptionDate: e.target.value })} />
              </div>
              <div className="col-md-6">
                <Form.Label className="small fw-semibold">Medicine (Rx item)</Form.Label>
                <Form.Select value={form.medicineId} onChange={(e) => setForm({ ...form, medicineId: e.target.value })}>
                  <option value="">- select -</option>
                  {medicines.map((m) => <option key={m.id} value={m.id}>{m.name}</option>)}
                </Form.Select>
              </div>
              <div className="col-12">
                <Form.Label className="small fw-semibold">Notes / dosage instructions</Form.Label>
                <Form.Control as="textarea" rows={2} value={form.notes || ''} onChange={(e) => setForm({ ...form, notes: e.target.value })} />
              </div>
            </div>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="light" onClick={() => setShow(false)}>Cancel</Button>
            <Button type="submit" variant="primary">Save</Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </div>
  )
}
