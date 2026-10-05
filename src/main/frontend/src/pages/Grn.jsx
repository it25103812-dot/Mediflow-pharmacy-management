import { useCallback, useEffect, useState } from 'react'
import { Card, Table, Button, Modal, Form, InputGroup } from 'react-bootstrap'
import api, { apiError } from '../services/api'
import { useToast } from '../components/Toast'
import ConfirmDialog from '../components/ConfirmDialog'
import Pagination from '../components/Pagination'

const BADGE = { PENDING: 'badge-soft-warning', VERIFIED: 'badge-soft-success', CANCELLED: 'badge-soft-danger' }

export default function Grn() {
  const toast = useToast()
  const [data, setData] = useState(null)
  const [pos, setPos] = useState([])
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const [show, setShow] = useState(false)
  const [poId, setPoId] = useState('')
  const [poDetail, setPoDetail] = useState(null)
  const [notes, setNotes] = useState('')
  const [items, setItems] = useState([])
  const [detail, setDetail] = useState(null)
  const [confirm, setConfirm] = useState(null)

  const load = useCallback(() => {
    const params = new URLSearchParams({ page, size: 10 })
    if (search) params.set('search', search)
    if (status) params.set('status', status)
    api.get('/grn?' + params.toString()).then((r) => setData(r.data)).catch((e) => toast.error(apiError(e)))
  }, [page, search, status]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    load()
    api.get('/purchase-orders?size=50').then((r) => {
      setPos((r.data.content || []).filter((p) => p.status === 'SENT' || p.status === 'PARTIALLY_RECEIVED'))
    }).catch(() => {})
  }, [load])

  const pickPo = async (id) => {
    setPoId(id)
    setItems([])
    if (!id) { setPoDetail(null); return }
    const res = await api.get('/purchase-orders/' + id)
    setPoDetail(res.data)
    setItems((res.data.items || []).map((i) => ({
      poItemId: i.id,
      medicineId: i.medicine?.id,
      medicineName: i.medicine?.name,
      ordered: i.quantity,
      alreadyReceived: i.receivedQuantity || 0,
      batchNumber: '',
      expiryDate: '',
      quantity: '',
      purchasePrice: i.purchasePrice,
      sellingPrice: i.medicine?.unitPrice,
    })))
  }

  const save = async (e) => {
    e.preventDefault()
    const today = new Date().toISOString().slice(0, 10)
    const touchedRows = items.filter((i) => i.batchNumber.trim() || i.expiryDate || i.quantity !== '')
    if (!touchedRows.length) { toast.warning('Fill batch details for at least one item'); return }
    for (const i of touchedRows) {
      const remaining = i.ordered - i.alreadyReceived
      if (!i.batchNumber.trim() || !i.expiryDate || !(Number(i.quantity) > 0)) {
        toast.warning(`${i.medicineName}: batch number, expiry date and quantity are all required`); return
      }
      if (i.expiryDate <= today) { toast.warning(`${i.medicineName}: expiry date must be in the future`); return }
      if (Number(i.quantity) > remaining) { toast.warning(`${i.medicineName}: quantity cannot exceed remaining ${remaining}`); return }
      if (!Number.isInteger(Number(i.quantity))) { toast.warning(`${i.medicineName}: quantity must be a whole number`); return }
      if (!(Number(i.purchasePrice) >= 0) || !(Number(i.sellingPrice) >= 0) || i.purchasePrice === '' || i.sellingPrice === '') {
        toast.warning(`${i.medicineName}: enter valid purchase and selling prices`); return
      }
    }
    const filled = touchedRows
    try {
      await api.post('/grn', {
        purchaseOrderId: Number(poId),
        notes,
        items: filled.map((i) => ({
          medicineId: Number(i.medicineId),
          poItemId: i.poItemId,
          batchNumber: i.batchNumber,
          expiryDate: i.expiryDate,
          quantity: Number(i.quantity),
          purchasePrice: Number(i.purchasePrice),
          sellingPrice: Number(i.sellingPrice),
        })),
      })
      toast.success('GRN created (PENDING). Verify it to increase stock.')
      setShow(false)
      load()
    } catch (err) {
      toast.error(apiError(err))
    }
  }

  const verify = (grn) => {
    setConfirm({
      title: 'Verify GRN',
      message: `Verify ${grn.grnNumber}? Inventory will be increased for its items.`,
      action: async () => {
        try {
          await api.patch('/grn/' + grn.id + '/verify')
          toast.success('GRN verified – stock updated')
          load()
        } catch (err) {
          toast.error(apiError(err))
        }
      },
    })
  }

  const cancel = (grn) => {
    setConfirm({
      title: 'Cancel GRN',
      message: `Cancel ${grn.grnNumber}? This cannot be undone.`,
      danger: true,
      action: async () => {
        try {
          await api.patch('/grn/' + grn.id + '/cancel')
          toast.success('GRN cancelled')
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
        <h4 className="fw-bold mb-0">Goods Received Notes</h4>
        <Button variant="primary" onClick={() => { setPoId(''); setPoDetail(null); setItems([]); setNotes(''); setShow(true) }}>
          <i className="bi bi-box-arrow-in-down me-2"></i>Receive Against PO
        </Button>
      </div>

      <Card className="mb-3">
        <Card.Body className="py-3">
          <div className="row g-2">
            <div className="col-md-5">
              <InputGroup>
                <InputGroup.Text><i className="bi bi-search"></i></InputGroup.Text>
                <Form.Control placeholder="Search GRN number or supplier..." value={search}
                  onChange={(e) => { setSearch(e.target.value); setPage(0) }} />
              </InputGroup>
            </div>
            <div className="col-md-4">
              <Form.Select value={status} onChange={(e) => { setStatus(e.target.value); setPage(0) }}>
                <option value="">All statuses</option>
                <option>PENDING</option><option>VERIFIED</option><option>CANCELLED</option>
              </Form.Select>
            </div>
          </div>
        </Card.Body>
      </Card>

      <Card>
        <Card.Body className="p-0">
          <Table responsive hover className="mb-0 align-middle">
            <thead><tr><th>#</th><th>GRN Number</th><th>PO</th><th>Supplier</th><th>Received</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {(data?.content || []).map((g) => (
                <tr key={g.id}>
                  <td>{g.id}</td>
                  <td className="fw-semibold clickable" onClick={async () => setDetail((await api.get('/grn/' + g.id)).data)}>{g.grnNumber}</td>
                  <td>{g.purchaseOrder?.poNumber}</td>
                  <td>{g.supplier?.name}</td>
                  <td className="small">{(g.receivedDate || '').replace('T', ' ').slice(0, 16)}</td>
                  <td><span className={`badge ${BADGE[g.status] || 'badge-soft-secondary'}`}>{g.status}</span></td>
                  <td className="text-end text-nowrap">
                    {g.status === 'PENDING' && (
                      <>
                        <Button size="sm" variant="outline-success" className="me-1" onClick={() => verify(g)}>
                          <i className="bi bi-check2-circle me-1"></i>Verify
                        </Button>
                        <Button size="sm" variant="outline-danger" onClick={() => cancel(g)}>Cancel</Button>
                      </>
                    )}
                  </td>
                </tr>
              ))}
              {data && data.content.length === 0 && (
                <tr><td colSpan="7"><div className="empty-state"><i className="bi bi-box-arrow-in-down"></i>No GRNs yet</div></td></tr>
              )}
            </tbody>
          </Table>
        </Card.Body>
      </Card>
      <Pagination page={data?.page || 0} totalPages={data?.totalPages || 0} onChange={setPage} />

      {/* Create GRN */}
      <Modal show={show} onHide={() => setShow(false)} centered size="xl">
        <Form onSubmit={save}>
          <Modal.Header closeButton>
            <Modal.Title className="fs-6">New Goods Received Note</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <div className="row g-3">
              <div className="col-md-6">
                <Form.Label className="small fw-semibold">Purchase order (SENT / PARTIALLY_RECEIVED) *</Form.Label>
                <Form.Select required value={poId} onChange={(e) => pickPo(e.target.value)}>
                  <option value="">Select purchase order...</option>
                  {pos.map((p) => <option key={p.id} value={p.id}>{p.poNumber} – {p.supplier?.name}</option>)}
                </Form.Select>
              </div>
              <div className="col-md-6">
                <Form.Label className="small fw-semibold">Notes</Form.Label>
                <Form.Control value={notes} onChange={(e) => setNotes(e.target.value)} />
              </div>
            </div>
            {poDetail && (
              <div className="mt-3">
                <Table size="sm" bordered>
                  <thead>
                    <tr><th>Medicine</th><th>Ordered</th><th>Already Recv</th><th>Batch No *</th><th>Expiry *</th><th>Qty Now *</th><th>Purchase</th><th>Selling</th></tr>
                  </thead>
                  <tbody>
                    {items.map((item, idx) => {
                      const remaining = item.ordered - item.alreadyReceived
                      return (
                        <tr key={idx}>
                          <td>{item.medicineName}</td>
                          <td>{item.ordered}</td>
                          <td>{item.alreadyReceived}</td>
                          <td style={{ minWidth: 130 }}>
                            <Form.Control size="sm" maxLength={50} value={item.batchNumber}
                              onChange={(e) => setItems(items.map((x, i) => i === idx ? { ...x, batchNumber: e.target.value } : x))} />
                          </td>
                          <td style={{ minWidth: 150 }}>
                            <Form.Control size="sm" type="date" min={new Date(Date.now() + 86400000).toISOString().slice(0, 10)} value={item.expiryDate}
                              onChange={(e) => setItems(items.map((x, i) => i === idx ? { ...x, expiryDate: e.target.value } : x))} />
                          </td>
                          <td style={{ minWidth: 90 }}>
                            <Form.Control size="sm" type="number" min="1" step="1" max={remaining} placeholder={`≤ ${remaining}`}
                              value={item.quantity}
                              onChange={(e) => setItems(items.map((x, i) => i === idx ? { ...x, quantity: e.target.value } : x))} />
                          </td>
                          <td style={{ minWidth: 95 }}>
                            <Form.Control size="sm" type="number" step="0.01" min="0" value={item.purchasePrice}
                              onChange={(e) => setItems(items.map((x, i) => i === idx ? { ...x, purchasePrice: e.target.value } : x))} />
                          </td>
                          <td style={{ minWidth: 95 }}>
                            <Form.Control size="sm" type="number" step="0.01" min="0" value={item.sellingPrice}
                              onChange={(e) => setItems(items.map((x, i) => i === idx ? { ...x, sellingPrice: e.target.value } : x))} />
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </Table>
                <p className="small text-muted mb-0">
                  <i className="bi bi-info-circle me-1"></i>
                  Leave a row blank to receive it later (partial receipt). Stock increases only after the GRN is verified.
                </p>
              </div>
            )}
          </Modal.Body>
          <Modal.Footer>
            <Button variant="light" onClick={() => setShow(false)}>Cancel</Button>
            <Button type="submit" variant="primary" disabled={!poDetail}>Create GRN</Button>
          </Modal.Footer>
        </Form>
      </Modal>

      {/* Detail */}
      <Modal show={!!detail} onHide={() => setDetail(null)} centered size="lg">
        <Modal.Header closeButton>
          <Modal.Title className="fs-6">GRN {detail?.grnNumber}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {detail && (
            <>
              <div className="row mb-2 small">
                <div className="col-4"><strong>PO:</strong> {detail.purchaseOrder?.poNumber}</div>
                <div className="col-4"><strong>Supplier:</strong> {detail.supplier?.name}</div>
                <div className="col-4"><strong>Status:</strong> {detail.status}</div>
              </div>
              <Table size="sm">
                <thead><tr><th>Medicine</th><th>Batch</th><th>Expiry</th><th>Qty</th><th>Purchase</th><th>Selling</th></tr></thead>
                <tbody>
                  {(detail.items || []).map((i) => (
                    <tr key={i.id}>
                      <td>{i.medicine?.name}</td>
                      <td>{i.batchNumber}</td>
                      <td>{i.expiryDate}</td>
                      <td>{i.quantity}</td>
                      <td>LKR {Number(i.purchasePrice).toFixed(2)}</td>
                      <td>LKR {Number(i.sellingPrice).toFixed(2)}</td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </>
          )}
        </Modal.Body>
      </Modal>

      <ConfirmDialog show={!!confirm} title={confirm?.title} message={confirm?.message}
        danger={confirm?.danger} confirmLabel="Yes, continue"
        onConfirm={() => { confirm?.action(); setConfirm(null) }}
        onCancel={() => setConfirm(null)} />
    </div>
  )
}
