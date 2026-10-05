import { useCallback, useEffect, useRef, useState } from 'react'
import { Card, Table, Button, Form, InputGroup, Modal, Badge } from 'react-bootstrap'
import api, { apiError } from '../services/api'
import { useToast } from '../components/Toast'
import { useAuth } from '../context/AuthContext'

export default function Pos() {
  const toast = useToast()
  const { user } = useAuth()
  const searchRef = useRef(null)

  const [query, setQuery] = useState('')
  const [results, setResults] = useState([])
  const [cart, setCart] = useState([])
  const [customers, setCustomers] = useState([])
  const [customerId, setCustomerId] = useState('')
  const [prescriptions, setPrescriptions] = useState([])
  const [prescriptionId, setPrescriptionId] = useState('')
  const [discount, setDiscount] = useState('0')
  const [paymentMethod, setPaymentMethod] = useState('CASH')
  const [busy, setBusy] = useState(false)
  const [receipt, setReceipt] = useState(null)

  const searchNow = useCallback((q) => {
    const params = new URLSearchParams({ size: 8, sortBy: 'name', sortDir: 'asc', active: 'true' })
    if (q) params.set('search', q)
    api.get('/medicines?' + params.toString()).then((r) => setResults(r.data.content || [])).catch(() => setResults([]))
  }, [])

  useEffect(() => {
    searchNow('')
    api.get('/customers?size=200').then((r) => setCustomers(r.data.content || [])).catch(() => {})
    api.get('/prescriptions?size=100').then((r) => setPrescriptions(r.data.content || [])).catch(() => {})
  }, [searchNow])

  useEffect(() => {
    const t = setTimeout(() => searchNow(query), 250)
    return () => clearTimeout(t)
  }, [query, searchNow])

  /** Load sellable batches (fresh stock only) for a medicine. */
  const openMedicine = async (m) => {
    let batches = []
    try {
      const res = await api.get('/batches/medicine/' + m.id)
      batches = res.data || []
    } catch (err) {
      toast.error(apiError(err))
      return
    }
    const now = new Date()
    const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
    // FEFO: earliest expiry first (API returns batches in id order)
    const sellable = batches
      .filter((b) => b.expiryDate >= today && b.quantityAvailable > 0)
      .sort((a, b) => a.expiryDate.localeCompare(b.expiryDate))
    if (sellable.length === 0) {
      toast.warning(`${m.name} has no sellable stock (out of stock or expired batches only)`)
      return
    }
    if (sellable.length === 1) {
      addToCart(m, sellable[0])
    } else {
      const pick = sellable[0]
      toast.info(`${m.name}: ${sellable.length} batches available. FEFO picked batch ${pick.batchNumber} (exp ${pick.expiryDate}).`)
      addToCart(m, pick)
    }
  }

  const addToCart = (m, batch) => {
    setCart((c) => {
      const existing = c.find((x) => x.batchId === batch.id)
      if (existing) {
        if (existing.quantity + 1 > batch.quantityAvailable) {
          toast.warning(`Only ${batch.quantityAvailable} units of ${m.name} (${batch.batchNumber}) in stock`)
          return c
        }
        return c.map((x) => x.batchId === batch.id ? { ...x, quantity: x.quantity + 1 } : x)
      }
      return [...c, {
        batchId: batch.id, medicineId: m.id, name: m.name, batchNumber: batch.batchNumber,
        expiryDate: batch.expiryDate, prescriptionRequired: m.prescriptionRequired,
        unitPrice: Number(batch.sellingPrice), quantity: 1, maxQty: batch.quantityAvailable,
      }]
    })
  }

  const setQty = (batchId, qty) => {
    setCart((c) => c.map((x) => {
      if (x.batchId !== batchId) return x
      const q = Math.max(1, Math.min(Number(qty) || 1, x.maxQty))
      if (Number(qty) > x.maxQty) toast.warning(`Only ${x.maxQty} units available for ${x.name} (${x.batchNumber})`)
      return { ...x, quantity: q }
    }))
  }

  const subtotal = cart.reduce((s, x) => s + x.unitPrice * x.quantity, 0)
  const discountNum = Math.max(0, Math.min(Number(discount) || 0, subtotal))
  const total = subtotal - discountNum
  const needsRx = cart.some((x) => x.prescriptionRequired)

  const completeSale = async () => {
    if (!cart.length) { toast.warning('Cart is empty'); return }
    if (needsRx && !prescriptionId) { toast.warning('Cart contains prescription-only medicine. Select the prescription below (record it on the Prescriptions page first if needed).'); return }
    setBusy(true)
    try {
      const res = await api.post('/sales', {
        customerId: customerId ? Number(customerId) : null,
        prescriptionId: prescriptionId ? Number(prescriptionId) : null,
        discount: discountNum,
        paymentMethod,
        items: cart.map((x) => ({ batchId: x.batchId, quantity: x.quantity, unitPrice: x.unitPrice })),
      })
      setReceipt(res.data)
      setCart([])
      setDiscount('0')
      setPrescriptionId('')
      toast.success('Sale completed – stock deducted')
      searchNow(query)
    } catch (err) {
      toast.error(apiError(err))
    } finally {
      setBusy(false)
    }
  }

  const printReceipt = () => window.print()

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h4 className="fw-bold mb-0">Point of Sale</h4>
        <span className="text-muted small">Cashier: <strong>{user?.fullName}</strong></span>
      </div>

      <div className="row g-3">
        {/* Left: search + results */}
        <div className="col-lg-6">
          <Card className="mb-3">
            <Card.Body className="py-3">
              <InputGroup>
                <InputGroup.Text><i className="bi bi-search"></i></InputGroup.Text>
                <Form.Control ref={searchRef} placeholder="Search medicine by name or generic name..." value={query}
                  onChange={(e) => setQuery(e.target.value)} autoFocus />
                {query && <Button variant="light" onClick={() => { setQuery(''); searchRef.current?.focus() }}><i className="bi bi-x"></i></Button>}
              </InputGroup>
            </Card.Body>
          </Card>
          <Card>
            <Card.Header className="py-2 small">AVAILABLE MEDICINES <span className="text-muted">(click to add – FEFO batch auto-selected)</span></Card.Header>
            <Card.Body className="p-0" style={{ maxHeight: 420, overflowY: 'auto' }}>
              {results.length === 0 && <div className="empty-state"><i className="bi bi-capsule"></i>No medicines match</div>}
              <Table hover size="sm" className="mb-0">
                <tbody>
                  {results.map((m) => (
                    <tr key={m.id} className="pos-search-result" onClick={() => openMedicine(m)}>
                      <td>
                        <div className="fw-semibold">{m.name} {m.prescriptionRequired && <Badge bg="warning" text="dark">Rx</Badge>}</div>
                        <div className="text-muted" style={{ fontSize: 12 }}>{m.genericName || ''} {m.manufacturer ? '• ' + m.manufacturer : ''}</div>
                      </td>
                      <td className="text-end fw-semibold text-nowrap">LKR {Number(m.unitPrice).toFixed(2)}</td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </Card.Body>
          </Card>
        </div>

        {/* Right: cart + totals */}
        <div className="col-lg-6">
          <Card className="mb-3">
            <Card.Header className="py-2">CART ({cart.length} items)</Card.Header>
            <Card.Body className="p-0" style={{ maxHeight: 300, overflowY: 'auto' }}>
              {cart.length === 0 && <div className="empty-state"><i className="bi bi-cart-x"></i>Cart is empty – search and click a medicine</div>}
              <Table size="sm" className="mb-0 pos-cart-row">
                <thead><tr><th>Medicine</th><th>Batch</th><th style={{ width: 92 }}>Qty</th><th className="text-end">Price</th><th className="text-end">Total</th><th></th></tr></thead>
                <tbody>
                  {cart.map((x) => (
                    <tr key={x.batchId}>
                      <td>
                        {x.name} {x.prescriptionRequired && <Badge bg="warning" text="dark">Rx</Badge>}
                        <div className="text-muted" style={{ fontSize: 11 }}>exp {x.expiryDate}</div>
                      </td>
                      <td className="small">{x.batchNumber}</td>
                      <td>
                        <Form.Control size="sm" type="number" min="1" max={x.maxQty} value={x.quantity}
                          onChange={(e) => setQty(x.batchId, e.target.value)} />
                      </td>
                      <td className="text-end small">{x.unitPrice.toFixed(2)}</td>
                      <td className="text-end fw-semibold small">{(x.unitPrice * x.quantity).toFixed(2)}</td>
                      <td>
                        <button className="btn btn-sm text-danger p-0" onClick={() => setCart((c) => c.filter((y) => y.batchId !== x.batchId))}>
                          <i className="bi bi-x-lg"></i>
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </Card.Body>
          </Card>

          <Card>
            <Card.Body>
              <div className="row g-3">
                <div className="col-md-6">
                  <Form.Label className="small fw-semibold">Customer</Form.Label>
                  <Form.Select value={customerId} onChange={(e) => setCustomerId(e.target.value)}>
                    <option value="">Walk-in customer</option>
                    {customers.map((c) => <option key={c.id} value={c.id}>{c.name} {c.phone ? '– ' + c.phone : ''}</option>)}
                  </Form.Select>
                </div>
                <div className="col-md-6">
                  <Form.Label className="small fw-semibold">Payment method</Form.Label>
                  <Form.Select value={paymentMethod} onChange={(e) => setPaymentMethod(e.target.value)}>
                    <option>CASH</option><option>CARD</option><option>ONLINE</option>
                  </Form.Select>
                </div>
                {needsRx && (
                  <div className="col-12">
                    <div className="alert alert-warning py-2 mb-2 small">
                      <i className="bi bi-clipboard2-pulse me-1"></i>
                      Prescription-only item(s) in cart. Select the prescription for this sale.
                    </div>
                    <Form.Select value={prescriptionId} onChange={(e) => setPrescriptionId(e.target.value)}>
                      <option value="">Select prescription...</option>
                      {prescriptions.map((p) => (
                        <option key={p.id} value={p.id}>
                          {p.prescriptionNumber} – Dr. {p.doctorName}{p.customer ? ' (' + p.customer.name + ')' : ''}
                        </option>
                      ))}
                    </Form.Select>
                  </div>
                )}
              </div>
              <hr />
              <div className="pos-totals">
                <div className="d-flex justify-content-between mb-1">
                  <span>Subtotal</span><span>LKR {subtotal.toFixed(2)}</span>
                </div>
                <div className="d-flex justify-content-between align-items-center mb-2">
                  <span>Discount (LKR)</span>
                  <Form.Control size="sm" type="number" min="0" step="0.01" style={{ width: 130 }} value={discount}
                    onChange={(e) => setDiscount(e.target.value)} />
                </div>
                <div className="d-flex justify-content-between grand">
                  <span>TOTAL</span><span>LKR {total.toFixed(2)}</span>
                </div>
              </div>
              <Button className="w-100 mt-3 py-2 fw-semibold" variant="success" disabled={busy || !cart.length} onClick={completeSale}>
                {busy ? <span className="spinner-border spinner-border-sm me-2"></span> : <i className="bi bi-check2-circle me-2"></i>}
                Complete Sale
              </Button>
            </Card.Body>
          </Card>
        </div>
      </div>

      {/* Receipt */}
      <Modal show={!!receipt} onHide={() => setReceipt(null)} centered size="sm">
        <Modal.Body className="p-4">
          {receipt && (
            <div className="receipt" id="receipt-area">
              <div className="receipt-header">
                <div style={{ fontWeight: 700, fontSize: 15 }}>LankaCare Pharmacy (Pvt) Ltd</div>
                <div>--------------------------------</div>
                <div>INVOICE / RECEIPT</div>
              </div>
              <div>Invoice : {receipt.invoiceNumber}</div>
              <div>Date    : {(receipt.saleDate || '').replace('T', ' ').slice(0, 16)}</div>
              <div>Cashier : {receipt.cashierName}</div>
              <div>Customer: {receipt.customerName}</div>
              <div className="r-line"></div>
              <table>
                <tbody>
                  {receipt.items.map((i) => (
                    <tr key={i.id}>
                      <td>{i.quantity} x {i.medicineName}</td>
                      <td style={{ textAlign: 'right' }}>{Number(i.lineTotal).toFixed(2)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <div className="r-line"></div>
              <div>Subtotal         : {Number(receipt.subtotal).toFixed(2)}</div>
              <div>Discount         : {Number(receipt.discount).toFixed(2)}</div>
              <div style={{ fontWeight: 700 }}>TOTAL (LKR)      : {Number(receipt.total).toFixed(2)}</div>
              <div>Payment          : {receipt.paymentMethod}</div>
              <div className="r-line"></div>
              <div className="receipt-header">
                <div>Thank you – Get well soon!</div>
                <div>Software by MediFlow</div>
              </div>
            </div>
          )}
        </Modal.Body>
        <Modal.Footer className="no-print">
          <Button variant="light" onClick={printReceipt}><i className="bi bi-printer me-1"></i>Print</Button>
          <Button variant="primary" onClick={printReceipt}><i className="bi bi-file-earmark-pdf me-1"></i>Save as PDF</Button>
          <Button variant="outline-secondary" onClick={() => setReceipt(null)}>Close</Button>
        </Modal.Footer>
      </Modal>
    </div>
  )
}
