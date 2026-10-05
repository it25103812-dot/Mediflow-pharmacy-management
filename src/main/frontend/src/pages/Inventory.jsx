import { useCallback, useEffect, useState } from 'react'
import { Card, Table, Button, Modal, Form, InputGroup, Tabs, Tab } from 'react-bootstrap'
import api, { apiError } from '../services/api'
import { useToast } from '../components/Toast'
import Pagination from '../components/Pagination'

export default function Inventory() {
  const toast = useToast()
  const [data, setData] = useState(null)
  const [alerts, setAlerts] = useState({ items: [], lowStock: 0, nearExpiry: 0, expired: 0 })
  const [adjustments, setAdjustments] = useState(null)
  const [search, setSearch] = useState('')
  const [lowStock, setLowStock] = useState(false)
  const [page, setPage] = useState(0)
  const [show, setShow] = useState(false)
  const [form, setForm] = useState({ batchId: '', adjustmentType: 'DAMAGE', quantityChange: '', reason: '' })
  const [medicines, setMedicines] = useState([])
  const [batchesForMed, setBatchesForMed] = useState([])
  const [selMed, setSelMed] = useState('')

  const load = useCallback(() => {
    const params = new URLSearchParams({ page, size: 10 })
    if (search) params.set('search', search)
    if (lowStock) params.set('lowStock', 'true')
    api.get('/inventory?' + params.toString()).then((r) => setData(r.data)).catch((e) => toast.error(apiError(e)))
    api.get('/inventory/alerts').then((r) => setAlerts(r.data)).catch(() => {})
    api.get('/inventory/adjustments?size=8').then((r) => setAdjustments(r.data)).catch(() => {})
  }, [page, search, lowStock]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { load() }, [load])

  useEffect(() => {
    api.get('/medicines?size=200').then((r) => setMedicines(r.data.content || [])).catch(() => {})
  }, [])

  const pickMedicine = (medId) => {
    setSelMed(medId)
    setForm({ ...form, batchId: '' })
    if (medId) {
      api.get('/batches/medicine/' + medId).then((r) => setBatchesForMed(r.data)).catch(() => setBatchesForMed([]))
    } else {
      setBatchesForMed([])
    }
  }

  const submitAdjust = async (e) => {
    e.preventDefault()
    try {
      await api.post('/inventory/adjustments', {
        batchId: Number(form.batchId),
        adjustmentType: form.adjustmentType,
        quantityChange: Number(form.quantityChange),
        reason: form.reason,
      })
      toast.success('Stock adjusted')
      setShow(false)
      load()
    } catch (err) {
      toast.error(apiError(err))
    }
  }

  const rowStatus = (row) => {
    if (row.batch?.expiryDate < new Date().toISOString().slice(0, 10)) return ['EXPIRED', 'badge-soft-danger']
    const soon = new Date()
    soon.setDate(soon.getDate() + 90)
    if (row.batch?.expiryDate < soon.toISOString().slice(0, 10)) return ['NEAR EXPIRY', 'badge-soft-info']
    if (row.quantityAvailable <= row.reorderLevel) return ['LOW STOCK', 'badge-soft-warning']
    return ['OK', 'badge-soft-success']
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h4 className="fw-bold mb-0">Inventory & Stock</h4>
        <Button variant="primary" onClick={() => { setForm({ batchId: '', adjustmentType: 'DAMAGE', quantityChange: '', reason: '' }); pickMedicine(''); setShow(true) }}>
          <i className="bi bi-sliders me-2"></i>Stock Adjustment
        </Button>
      </div>

      <Row_alerts alerts={alerts} />

      <Card className="mb-3">
        <Card.Body className="py-3">
          <div className="row g-2">
            <div className="col-md-5">
              <InputGroup>
                <InputGroup.Text><i className="bi bi-search"></i></InputGroup.Text>
                <Form.Control placeholder="Search medicine or batch..." value={search}
                  onChange={(e) => { setSearch(e.target.value); setPage(0) }} />
              </InputGroup>
            </div>
            <div className="col-md-4 d-flex align-items-center">
              <Form.Check type="switch" label="Low stock only" checked={lowStock}
                onChange={(e) => { setLowStock(e.target.checked); setPage(0) }} />
            </div>
          </div>
        </Card.Body>
      </Card>

      <Tabs defaultActiveKey="stock" className="mb-3">
        <Tab eventKey="stock" title="Live Stock">
          <Card>
            <Card.Body className="p-0">
              <Table responsive hover className="mb-0 align-middle">
                <thead>
                  <tr><th>#</th><th>Medicine</th><th>Batch</th><th>Expiry</th><th>Available</th><th>Reorder</th><th>Status</th></tr>
                </thead>
                <tbody>
                  {(data?.content || []).map((row) => {
                    const [label, cls] = rowStatus(row)
                    return (
                      <tr key={row.id}>
                        <td>{row.batch?.medicine?.id}</td>
                        <td className="fw-semibold">{row.batch?.medicine?.name}</td>
                        <td>{row.batch?.batchNumber}</td>
                        <td>{row.batch?.expiryDate}</td>
                        <td className="fw-bold">{row.quantityAvailable}</td>
                        <td>{row.reorderLevel}</td>
                        <td><span className={`badge ${cls}`}>{label}</span></td>
                      </tr>
                    )
                  })}
                  {data && data.content.length === 0 && (
                    <tr><td colSpan="7"><div className="empty-state"><i className="bi bi-clipboard-data"></i>No stock records</div></td></tr>
                  )}
                </tbody>
              </Table>
            </Card.Body>
          </Card>
          <Pagination page={data?.page || 0} totalPages={data?.totalPages || 0} onChange={setPage} />
        </Tab>
        <Tab eventKey="alerts" title={`Alerts (${alerts.expired + alerts.nearExpiry + alerts.lowStock})`}>
          <Card>
            <Card.Body className="p-0">
              <Table responsive hover className="mb-0 align-middle">
                <thead><tr><th>Medicine</th><th>Batch</th><th>Expiry</th><th>Qty</th><th>Status</th></tr></thead>
                <tbody>
                  {(alerts.items || []).map((a, i) => (
                    <tr key={i}>
                      <td className="fw-semibold">{a.medicineName}</td>
                      <td>{a.batchNumber}</td>
                      <td>{a.expiryDate}</td>
                      <td>{a.quantityAvailable}</td>
                      <td>
                        <span className={`badge ${a.status === 'EXPIRED' ? 'badge-soft-danger' : a.status === 'NEAR_EXPIRY' ? 'badge-soft-info' : 'badge-soft-warning'}`}>
                          {a.status.replace('_', ' ')}
                        </span>
                      </td>
                    </tr>
                  ))}
                  {(alerts.items || []).length === 0 && (
                    <tr><td colSpan="5"><div className="empty-state"><i className="bi bi-check2-circle"></i>No alerts – stock is healthy</div></td></tr>
                  )}
                </tbody>
              </Table>
            </Card.Body>
          </Card>
        </Tab>
        <Tab eventKey="history" title="Adjustment History">
          <Card>
            <Card.Body className="p-0">
              <Table responsive hover className="mb-0 align-middle">
                <thead><tr><th>Date</th><th>Batch</th><th>Type</th><th>Change</th><th>Reason</th><th>By</th></tr></thead>
                <tbody>
                  {(adjustments?.content || []).map((a) => (
                    <tr key={a.id}>
                      <td className="small">{(a.createdAt || '').replace('T', ' ').slice(0, 16)}</td>
                      <td>{a.batch?.batchNumber}</td>
                      <td><span className="badge badge-soft-secondary">{a.adjustmentType}</span></td>
                      <td className={a.quantityChange < 0 ? 'text-danger fw-semibold' : 'text-success fw-semibold'}>
                        {a.quantityChange > 0 ? '+' : ''}{a.quantityChange}
                      </td>
                      <td className="small text-muted">{a.reason || '-'}</td>
                      <td className="small">{a.performedBy?.fullName || '-'}</td>
                    </tr>
                  ))}
                  {adjustments && adjustments.content.length === 0 && (
                    <tr><td colSpan="6"><div className="empty-state">No adjustments recorded</div></td></tr>
                  )}
                </tbody>
              </Table>
            </Card.Body>
          </Card>
        </Tab>
      </Tabs>

      <Modal show={show} onHide={() => setShow(false)} centered>
        <Form onSubmit={submitAdjust}>
          <Modal.Header closeButton>
            <Modal.Title className="fs-6">Stock Adjustment</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <Form.Label className="small fw-semibold">Medicine *</Form.Label>
            <Form.Select required value={selMed} onChange={(e) => pickMedicine(e.target.value)}>
              <option value="">Select medicine...</option>
              {medicines.map((m) => <option key={m.id} value={m.id}>{m.name}</option>)}
            </Form.Select>
            <Form.Label className="small fw-semibold mt-3">Batch *</Form.Label>
            <Form.Select required value={form.batchId} onChange={(e) => setForm({ ...form, batchId: e.target.value })}>
              <option value="">Select batch...</option>
              {batchesForMed.map((b) => (
                <option key={b.id} value={b.id}>{b.batchNumber} – qty {b.quantityAvailable} – exp {b.expiryDate}</option>
              ))}
            </Form.Select>
            <div className="row g-3 mt-1">
              <div className="col-6">
                <Form.Label className="small fw-semibold">Type *</Form.Label>
                <Form.Select value={form.adjustmentType} onChange={(e) => setForm({ ...form, adjustmentType: e.target.value })}>
                  <option value="DAMAGE">Damage (-)</option>
                  <option value="RETURN">Customer return (+)</option>
                  <option value="CORRECTION">Correction (+/-)</option>
                  <option value="EXPIRED_DISPOSAL">Expired disposal (-)</option>
                </Form.Select>
              </div>
              <div className="col-6">
                <Form.Label className="small fw-semibold">Quantity change *</Form.Label>
                <Form.Control type="number" required value={form.quantityChange}
                  placeholder="e.g. -5 or +10"
                  onChange={(e) => setForm({ ...form, quantityChange: e.target.value })} />
              </div>
              <div className="col-12">
                <Form.Label className="small fw-semibold">Reason</Form.Label>
                <Form.Control maxLength={255} value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} />
              </div>
            </div>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="light" onClick={() => setShow(false)}>Cancel</Button>
            <Button type="submit" variant="primary">Apply Adjustment</Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </div>
  )
}

function Row_alerts({ alerts }) {
  return (
    <div className="row g-3 mb-4">
      <div className="col-md-4">
        <Card className="stat-card border-start border-4 border-warning">
          <Card.Body className="py-3">
            <div className="d-flex justify-content-between">
              <div>
                <p className="stat-label mb-0">Low Stock Batches</p>
                <div className="stat-value">{alerts.lowStock}</div>
              </div>
              <i className="bi bi-exclamation-triangle text-warning fs-2"></i>
            </div>
          </Card.Body>
        </Card>
      </div>
      <div className="col-md-4">
        <Card className="stat-card border-start border-4 border-info">
          <Card.Body className="py-3">
            <div className="d-flex justify-content-between">
              <div>
                <p className="stat-label mb-0">Near Expiry (90 days)</p>
                <div className="stat-value">{alerts.nearExpiry}</div>
              </div>
              <i className="bi bi-hourglass-split text-info fs-2"></i>
            </div>
          </Card.Body>
        </Card>
      </div>
      <div className="col-md-4">
        <Card className="stat-card border-start border-4 border-danger">
          <Card.Body className="py-3">
            <div className="d-flex justify-content-between">
              <div>
                <p className="stat-label mb-0">Expired</p>
                <div className="stat-value">{alerts.expired}</div>
              </div>
              <i className="bi bi-x-octagon text-danger fs-2"></i>
            </div>
          </Card.Body>
        </Card>
      </div>
    </div>
  )
}
