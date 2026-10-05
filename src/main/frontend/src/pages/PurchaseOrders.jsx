import { useCallback, useEffect, useState } from 'react'
import { Card, Table, Button, Modal, Form, InputGroup, Badge } from 'react-bootstrap'
import api, { apiError } from '../services/api'
import { useToast } from '../components/Toast'
import Pagination from '../components/Pagination'

const STATUSES = ['DRAFT', 'SENT', 'PARTIALLY_RECEIVED', 'RECEIVED', 'CANCELLED']
const BADGE = {
  DRAFT: 'badge-soft-secondary',
  SENT: 'badge-soft-info',
  PARTIALLY_RECEIVED: 'badge-soft-warning',
  RECEIVED: 'badge-soft-success',
  CANCELLED: 'badge-soft-danger',
}

const EMPTY = { supplierId: '', expectedDate: '', notes: '', items: [] }

export default function PurchaseOrders() {
  const toast = useToast()
  const [data, setData] = useState(null)
  const [suppliers, setSuppliers] = useState([])
  const [medicines, setMedicines] = useState([])
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const [show, setShow] = useState(false)
  const [form, setForm] = useState(EMPTY)
  const [editId, setEditId] = useState(null)
  const [detail, setDetail] = useState(null)

  const load = useCallback(() => {
    const params = new URLSearchParams({ page, size: 10 })
    if (search) params.set('search', search)
    if (status) params.set('status', status)
    api.get('/purchase-orders?' + params.toString()).then((r) => setData(r.data)).catch((e) => toast.error(apiError(e)))
  }, [page, search, status]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    load()
    api.get('/suppliers?size=100').then((r) => setSuppliers(r.data.content || [])).catch(() => {})
    api.get('/medicines?size=200').then((r) => setMedicines(r.data.content || [])).catch(() => {})
  }, [load])

  const openCreate = () => { setForm({ ...EMPTY, items: [newItem()] }); setEditId(null); setShow(true) }

  const newItem = () => ({ medicineId: '', quantity: 1, purchasePrice: '' })

  const openEdit = async (po) => {
    if (po.status !== 'DRAFT') {
      toast.warning('Only DRAFT purchase orders can be edited')
      return
    }
    const res = await api.get('/purchase-orders/' + po.id)
    const po2 = res.data
    setForm({
      supplierId: po2.supplier?.id || '',
      expectedDate: po2.expectedDate || '',
      notes: po2.notes || '',
      items: (po2.items || []).map((i) => ({ medicineId: i.medicine?.id, quantity: i.quantity, purchasePrice: i.purchasePrice })),
    })
    setEditId(po.id)
    setShow(true)
  }

  const save = async (e) => {
    e.preventDefault()
    if (!form.items.length) { toast.warning('Add at least one medicine'); return }
    try {
      const payload = {
        supplierId: Number(form.supplierId),
        expectedDate: form.expectedDate || null,
        notes: form.notes,
        items: form.items.map((i) => ({ medicineId: Number(i.medicineId), quantity: Number(i.quantity), purchasePrice: Number(i.purchasePrice) })),
      }
      if (editId) {
        await api.put('/purchase-orders/' + editId, payload)
        toast.success('Purchase order updated')
      } else {
        await api.post('/purchase-orders', payload)
        toast.success('Purchase order created as DRAFT')
      }
      setShow(false)
      load()
    } catch (err) {
      toast.error(apiError(err))
    }
  }

  const setStatusFor = async (po, newStatus) => {
    try {
      await api.patch('/purchase-orders/' + po.id + '/status', { status: newStatus })
      toast.success('Status changed to ' + newStatus)
      load()
    } catch (err) {
      toast.error(apiError(err))
    }
  }

  const total = form.items.reduce((sum, i) => sum + (Number(i.quantity) || 0) * (Number(i.purchasePrice) || 0), 0)

  const showDetail = async (po) => {
    const res = await api.get('/purchase-orders/' + po.id)
    setDetail(res.data)
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h4 className="fw-bold mb-0">Purchase Orders</h4>
        <Button variant="primary" onClick={openCreate}>
          <i className="bi bi-cart-plus me-2"></i>New Purchase Order
        </Button>
      </div>

      <Card className="mb-3">
        <Card.Body className="py-3">
          <div className="row g-2">
            <div className="col-md-5">
              <InputGroup>
                <InputGroup.Text><i className="bi bi-search"></i></InputGroup.Text>
                <Form.Control placeholder="Search PO number or supplier..." value={search}
                  onChange={(e) => { setSearch(e.target.value); setPage(0) }} />
              </InputGroup>
            </div>
            <div className="col-md-4">
              <Form.Select value={status} onChange={(e) => { setStatus(e.target.value); setPage(0) }}>
                <option value="">All statuses</option>
                {STATUSES.map((s) => <option key={s}>{s}</option>)}
              </Form.Select>
            </div>
          </div>
        </Card.Body>
      </Card>

      <Card>
        <Card.Body className="p-0">
          <Table responsive hover className="mb-0 align-middle">
            <thead><tr><th>#</th><th>PO Number</th><th>Supplier</th><th>Order Date</th><th>Expected</th><th>Total</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {(data?.content || []).map((po) => (
                <tr key={po.id}>
                  <td>{po.id}</td>
                  <td className="fw-semibold clickable" onClick={() => showDetail(po)}>{po.poNumber}</td>
                  <td>{po.supplier?.name}</td>
                  <td className="small">{(po.orderDate || '').slice(0, 10)}</td>
                  <td className="small">{po.expectedDate || '-'}</td>
                  <td>LKR {Number(po.totalAmount).toFixed(2)}</td>
                  <td><span className={`badge ${BADGE[po.status] || 'badge-soft-secondary'}`}>{po.status.replace('_', ' ')}</span></td>
                  <td className="text-end text-nowrap">
                    {po.status === 'DRAFT' && (
                      <>
                        <Button size="sm" variant="light" title="Edit draft" className="me-1" onClick={() => openEdit(po)}>
                          <i className="bi bi-pencil"></i>
                        </Button>
                        <Button size="sm" variant="outline-primary" title="Send to supplier" onClick={() => setStatusFor(po, 'SENT')}>
                          Send
                        </Button>
                      </>
                    )}
                    {po.status === 'SENT' && (
                      <Button size="sm" variant="outline-danger" onClick={() => setStatusFor(po, 'CANCELLED')}>
                        Cancel
                      </Button>
                    )}
                  </td>
                </tr>
              ))}
              {data && data.content.length === 0 && (
                <tr><td colSpan="8"><div className="empty-state"><i className="bi bi-cart-check"></i>No purchase orders</div></td></tr>
              )}
            </tbody>
          </Table>
        </Card.Body>
      </Card>
      <Pagination page={data?.page || 0} totalPages={data?.totalPages || 0} onChange={setPage} />

      {/* Create / edit modal */}
      <Modal show={show} onHide={() => setShow(false)} centered size="lg">
        <Form onSubmit={save}>
          <Modal.Header closeButton>
            <Modal.Title className="fs-6">{editId ? 'Edit Draft Purchase Order' : 'New Purchase Order'}</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <div className="row g-3">
              <div className="col-md-6">
                <Form.Label className="small fw-semibold">Supplier *</Form.Label>
                <Form.Select required value={form.supplierId} onChange={(e) => setForm({ ...form, supplierId: e.target.value })}>
                  <option value="">Select supplier...</option>
                  {suppliers.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                </Form.Select>
              </div>
              <div className="col-md-6">
                <Form.Label className="small fw-semibold">Expected date</Form.Label>
                <Form.Control type="date" value={form.expectedDate || ''} onChange={(e) => setForm({ ...form, expectedDate: e.target.value })} />
              </div>
            </div>
            <hr />
            <div className="d-flex justify-content-between align-items-center mb-2">
              <span className="fw-semibold small">ITEMS</span>
              <Button size="sm" variant="outline-primary" onClick={() => setForm({ ...form, items: [...form.items, newItem()] })}>
                <i className="bi bi-plus-lg me-1"></i>Add Line
              </Button>
            </div>
            {form.items.map((item, idx) => (
              <div className="row g-2 mb-2" key={idx}>
                <div className="col-md-5">
                  <Form.Select required value={item.medicineId}
                    onChange={(e) => {
                      const items = [...form.items]
                      items[idx] = { ...items[idx], medicineId: e.target.value }
                      setForm({ ...form, items })
                    }}>
                    <option value="">Medicine...</option>
                    {medicines.map((m) => <option key={m.id} value={m.id}>{m.name}</option>)}
                  </Form.Select>
                </div>
                <div className="col-md-2">
                  <Form.Control type="number" min="1" required placeholder="Qty" value={item.quantity}
                    onChange={(e) => {
                      const items = [...form.items]
                      items[idx] = { ...items[idx], quantity: e.target.value }
                      setForm({ ...form, items })
                    }} />
                </div>
                <div className="col-md-3">
                  <Form.Control type="number" step="0.01" min="0" required placeholder="Unit cost" value={item.purchasePrice}
                    onChange={(e) => {
                      const items = [...form.items]
                      items[idx] = { ...items[idx], purchasePrice: e.target.value }
                      setForm({ ...form, items })
                    }} />
                </div>
                <div className="col-md-2 d-flex align-items-center justify-content-between">
                  <span className="small text-muted">
                    {(Number(item.quantity) || 0) * (Number(item.purchasePrice) || 0) ? 'LKR ' + ((Number(item.quantity) || 0) * (Number(item.purchasePrice) || 0)).toFixed(2) : '—'}
                  </span>
                  <button type="button" className="btn btn-sm btn-outline-danger"
                    onClick={() => setForm({ ...form, items: form.items.filter((_, i) => i !== idx) })}>
                    <i className="bi bi-x"></i>
                  </button>
                </div>
              </div>
            ))}
            <div className="text-end fw-bold mt-2">Total: LKR {total.toFixed(2)}</div>
            <Form.Label className="small fw-semibold mt-3">Notes</Form.Label>
            <Form.Control as="textarea" rows={2} value={form.notes || ''} onChange={(e) => setForm({ ...form, notes: e.target.value })} />
          </Modal.Body>
          <Modal.Footer>
            <Button variant="light" onClick={() => setShow(false)}>Cancel</Button>
            <Button type="submit" variant="primary">{editId ? 'Save Changes' : 'Create Draft PO'}</Button>
          </Modal.Footer>
        </Form>
      </Modal>

      {/* Detail modal */}
      <Modal show={!!detail} onHide={() => setDetail(null)} centered size="lg">
        <Modal.Header closeButton>
          <Modal.Title className="fs-6">Purchase Order {detail?.poNumber}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {detail && (
            <>
              <div className="row mb-3 small">
                <div className="col-6"><strong>Supplier:</strong> {detail.supplier?.name}</div>
                <div className="col-3"><strong>Status:</strong> {detail.status}</div>
                <div className="col-3"><strong>Expected:</strong> {detail.expectedDate || '-'}</div>
              </div>
              <Table size="sm">
                <thead><tr><th>Medicine</th><th>Qty</th><th>Received</th><th>Unit Cost</th><th>Line Total</th></tr></thead>
                <tbody>
                  {(detail.items || []).map((i) => (
                    <tr key={i.id}>
                      <td>{i.medicine?.name}</td>
                      <td>{i.quantity}</td>
                      <td>{i.receivedQuantity}</td>
                      <td>LKR {Number(i.purchasePrice).toFixed(2)}</td>
                      <td>LKR {Number(i.lineTotal).toFixed(2)}</td>
                    </tr>
                  ))}
                </tbody>
              </Table>
              <div className="text-end fw-bold">Total: LKR {Number(detail.totalAmount).toFixed(2)}</div>
            </>
          )}
        </Modal.Body>
      </Modal>
    </div>
  )
}
