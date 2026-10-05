import { useCallback, useEffect, useState } from 'react'
import { Card, Table, Button, Modal, Form, InputGroup } from 'react-bootstrap'
import api, { apiError } from '../services/api'
import { useToast } from '../components/Toast'
import Pagination from '../components/Pagination'

const EMPTY = {
  medicineId: '', batchNumber: '', expiryDate: '', purchasePrice: '', sellingPrice: '',
  initialQuantity: 0, reorderLevel: 20,
}

const STATUS_BADGE = {
  OK: 'badge-soft-success',
  LOW_STOCK: 'badge-soft-warning',
  NEAR_EXPIRY: 'badge-soft-info',
  EXPIRED: 'badge-soft-danger',
}

export default function Batches() {
  const toast = useToast()
  const [data, setData] = useState(null)
  const [medicines, setMedicines] = useState([])
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [show, setShow] = useState(false)
  const [form, setForm] = useState(EMPTY)
  const [editId, setEditId] = useState(null)

  const load = useCallback(() => {
    const params = new URLSearchParams({ page, size: 10 })
    if (search) params.set('search', search)
    api.get('/batches?' + params.toString()).then((r) => setData(r.data)).catch((e) => toast.error(apiError(e)))
  }, [page, search]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    load()
    api.get('/medicines?size=200&active=true').then((r) => setMedicines(r.data.content || [])).catch(() => {})
  }, [load])

  const save = async (e) => {
    e.preventDefault()
    try {
      const payload = { ...form, medicineId: Number(form.medicineId) }
      if (editId) {
        await api.put('/batches/' + editId, payload)
        toast.success('Batch updated')
      } else {
        await api.post('/batches', payload)
        toast.success('Batch created with initial stock')
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
        <h4 className="fw-bold mb-0">Medicine Batches</h4>
        <Button variant="primary" onClick={() => { setForm({ ...EMPTY }); setEditId(null); setShow(true) }}>
          <i className="bi bi-plus-lg me-2"></i>Add Batch
        </Button>
      </div>

      <Card className="mb-3">
        <Card.Body className="py-3">
          <div className="row">
            <div className="col-md-5">
              <InputGroup>
                <InputGroup.Text><i className="bi bi-search"></i></InputGroup.Text>
                <Form.Control placeholder="Search batch number or medicine..." value={search}
                  onChange={(e) => { setSearch(e.target.value); setPage(0) }} />
              </InputGroup>
            </div>
          </div>
        </Card.Body>
      </Card>

      <Card>
        <Card.Body className="p-0">
          <Table responsive hover className="mb-0 align-middle">
            <thead>
              <tr><th>#</th><th>Medicine</th><th>Batch No</th><th>Expiry</th><th>Qty</th><th>Reorder</th><th>Purchase</th><th>Selling</th><th>Status</th><th></th></tr>
            </thead>
            <tbody>
              {(data?.content || []).map((b) => (
                <tr key={b.id}>
                  <td>{b.id}</td>
                  <td className="fw-semibold">{b.medicineName}</td>
                  <td>{b.batchNumber}</td>
                  <td>{b.expiryDate}</td>
                  <td>{b.quantityAvailable ?? '—'}</td>
                  <td>{b.reorderLevel ?? '—'}</td>
                  <td>LKR {Number(b.purchasePrice).toFixed(2)}</td>
                  <td>LKR {Number(b.sellingPrice).toFixed(2)}</td>
                  <td><span className={`badge ${STATUS_BADGE[b.stockStatus] || 'badge-soft-secondary'}`}>{(b.stockStatus || 'OK').replace('_', ' ')}</span></td>
                  <td className="text-end">
                    <Button size="sm" variant="light" onClick={() => {
                      setForm({
                        medicineId: b.medicineId, batchNumber: b.batchNumber, expiryDate: b.expiryDate,
                        purchasePrice: b.purchasePrice, sellingPrice: b.sellingPrice,
                        initialQuantity: 0, reorderLevel: b.reorderLevel || 20,
                      })
                      setEditId(b.id)
                      setShow(true)
                    }}>
                      <i className="bi bi-pencil"></i>
                    </Button>
                  </td>
                </tr>
              ))}
              {data && data.content.length === 0 && (
                <tr><td colSpan="10"><div className="empty-state"><i className="bi bi-box-seam"></i>No batches found</div></td></tr>
              )}
            </tbody>
          </Table>
        </Card.Body>
      </Card>
      <Pagination page={data?.page || 0} totalPages={data?.totalPages || 0} onChange={setPage} />

      <Modal show={show} onHide={() => setShow(false)} centered>
        <Form onSubmit={save}>
          <Modal.Header closeButton>
            <Modal.Title className="fs-6">{editId ? 'Edit Batch' : 'New Batch'}</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <div className="row g-3">
              <div className="col-12">
                <Form.Label className="small fw-semibold">Medicine *</Form.Label>
                <Form.Select required value={form.medicineId} disabled={!!editId}
                  onChange={(e) => setForm({ ...form, medicineId: e.target.value })}>
                  <option value="">Select medicine...</option>
                  {medicines.map((m) => <option key={m.id} value={m.id}>{m.name}</option>)}
                </Form.Select>
              </div>
              <div className="col-6">
                <Form.Label className="small fw-semibold">Batch number *</Form.Label>
                <Form.Control required maxLength={50} value={form.batchNumber} onChange={(e) => setForm({ ...form, batchNumber: e.target.value })} />
              </div>
              <div className="col-6">
                <Form.Label className="small fw-semibold">Expiry date *</Form.Label>
                <Form.Control type="date" required min={editId ? undefined : new Date().toISOString().slice(0, 10)}
                  value={form.expiryDate} onChange={(e) => setForm({ ...form, expiryDate: e.target.value })} />
              </div>
              <div className="col-6">
                <Form.Label className="small fw-semibold">Purchase price *</Form.Label>
                <Form.Control type="number" step="0.01" min="0" required value={form.purchasePrice}
                  onChange={(e) => setForm({ ...form, purchasePrice: e.target.value })} />
              </div>
              <div className="col-6">
                <Form.Label className="small fw-semibold">Selling price *</Form.Label>
                <Form.Control type="number" step="0.01" min="0" required value={form.sellingPrice}
                  onChange={(e) => setForm({ ...form, sellingPrice: e.target.value })} />
              </div>
              {!editId && (
                <>
                  <div className="col-6">
                    <Form.Label className="small fw-semibold">Initial quantity *</Form.Label>
                    <Form.Control type="number" min="0" required value={form.initialQuantity}
                      onChange={(e) => setForm({ ...form, initialQuantity: e.target.value })} />
                  </div>
                  <div className="col-6">
                    <Form.Label className="small fw-semibold">Reorder level *</Form.Label>
                    <Form.Control type="number" min="0" required value={form.reorderLevel}
                      onChange={(e) => setForm({ ...form, reorderLevel: e.target.value })} />
                  </div>
                </>
              )}
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
