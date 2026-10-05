import { useCallback, useEffect, useState } from 'react'
import { Card, Table, Button, Modal, Form, InputGroup, Badge } from 'react-bootstrap'
import api, { apiError } from '../services/api'
import { useToast } from '../components/Toast'
import Pagination from '../components/Pagination'
import ConfirmDialog from '../components/ConfirmDialog'
import { useAuth } from '../context/AuthContext'

export default function Sales() {
  const toast = useToast()
  const { user } = useAuth()
  const canManage = ['ADMINISTRATOR', 'FINANCE_MANAGER'].includes(user?.roleName)
  const [data, setData] = useState(null)
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('')
  const [start, setStart] = useState('')
  const [end, setEnd] = useState('')
  const [page, setPage] = useState(0)
  const [detail, setDetail] = useState(null)
  const [edit, setEdit] = useState(null)
  const [confirm, setConfirm] = useState(null)

  const load = useCallback(() => {
    const params = new URLSearchParams({ page, size: 12 })
    if (search) params.set('search', search)
    if (status) params.set('status', status)
    if (start) params.set('start', start)
    if (end) params.set('end', end)
    api.get('/sales?' + params.toString()).then((r) => setData(r.data)).catch((e) => toast.error(apiError(e)))
  }, [page, search, status, start, end]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { load() }, [load])

  const showDetail = async (s) => {
    const res = await api.get('/sales/' + s.id)
    setDetail(res.data)
  }

  const openEdit = (s) => setEdit({
    id: s.id, invoiceNumber: s.invoiceNumber, subtotal: Number(s.subtotal),
    paymentMethod: s.paymentMethod, discount: String(s.discount ?? 0),
  })

  const saveEdit = async (e) => {
    e.preventDefault()
    try {
      await api.put('/sales/' + edit.id, { paymentMethod: edit.paymentMethod, discount: Number(edit.discount || 0) })
      toast.success('Bill updated')
      setEdit(null)
      load()
    } catch (err) {
      toast.error(apiError(err))
    }
  }

  const remove = (s) => setConfirm({
    title: 'Delete bill',
    message: `Delete invoice ${s.invoiceNumber}? ` + (s.status === 'COMPLETED'
      ? 'The sold quantities will be added back to stock. This cannot be undone.'
      : 'This cannot be undone.'),
    action: async () => {
      try {
        await api.delete('/sales/' + s.id)
        toast.success('Bill deleted')
        load()
      } catch (err) {
        toast.error(apiError(err))
      }
    },
  })

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h4 className="fw-bold mb-0">Sales & Bills</h4>
      </div>

      <Card className="mb-3">
        <Card.Body className="py-3">
          <div className="row g-2">
            <div className="col-md-3">
              <InputGroup>
                <InputGroup.Text><i className="bi bi-search"></i></InputGroup.Text>
                <Form.Control placeholder="Invoice number..." value={search}
                  onChange={(e) => { setSearch(e.target.value); setPage(0) }} />
              </InputGroup>
            </div>
            <div className="col-md-2">
              <Form.Select value={status} onChange={(e) => { setStatus(e.target.value); setPage(0) }}>
                <option value="">All statuses</option>
                <option>COMPLETED</option><option>RETURNED</option>
              </Form.Select>
            </div>
            <div className="col-md-2">
              <Form.Control type="date" value={start} onChange={(e) => { setStart(e.target.value); setPage(0) }} />
            </div>
            <div className="col-md-2">
              <Form.Control type="date" value={end} onChange={(e) => { setEnd(e.target.value); setPage(0) }} />
            </div>
          </div>
        </Card.Body>
      </Card>

      <Card>
        <Card.Body className="p-0">
          <Table responsive hover className="mb-0 align-middle">
            <thead><tr><th>#</th><th>Invoice</th><th>Date</th><th>Customer</th><th>Cashier</th><th>Items</th><th>Total</th><th>Payment</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {(data?.content || []).map((s) => (
                <tr key={s.id}>
                  <td>{s.id}</td>
                  <td className="fw-semibold clickable" onClick={() => showDetail(s)}>{s.invoiceNumber}</td>
                  <td className="small">{(s.saleDate || '').replace('T', ' ').slice(0, 16)}</td>
                  <td>{s.customer ? s.customer.name : <span className="text-muted">Walk-in</span>}</td>
                  <td className="small">{s.cashier?.fullName || '-'}</td>
                  <td>{s.items?.length}</td>
                  <td className="fw-semibold">LKR {Number(s.total).toFixed(2)}</td>
                  <td><Badge bg="light" text="dark">{s.paymentMethod}</Badge></td>
                  <td>
                    {s.status === 'COMPLETED'
                      ? <span className="badge badge-soft-success">COMPLETED</span>
                      : <span className="badge badge-soft-danger">RETURNED</span>}
                  </td>
                  <td className="text-end">
                    <Button size="sm" variant="light" title="View" onClick={() => showDetail(s)}><i className="bi bi-receipt"></i></Button>
                    {canManage && (
                      <>
                        {s.status === 'COMPLETED' && (
                          <Button size="sm" variant="light" title="Edit" className="ms-1" onClick={() => openEdit(s)}>
                            <i className="bi bi-pencil"></i>
                          </Button>
                        )}
                        <Button size="sm" variant="outline-danger" title="Delete" className="ms-1" onClick={() => remove(s)}>
                          <i className="bi bi-trash"></i>
                        </Button>
                      </>
                    )}
                  </td>
                </tr>
              ))}
              {data && data.content.length === 0 && (
                <tr><td colSpan="10"><div className="empty-state"><i className="bi bi-receipt"></i>No sales found</div></td></tr>
              )}
            </tbody>
          </Table>
        </Card.Body>
      </Card>
      <Pagination page={data?.page || 0} totalPages={data?.totalPages || 0} onChange={setPage} />

      <Modal show={!!detail} onHide={() => setDetail(null)} centered>
        <Modal.Header closeButton>
          <Modal.Title className="fs-6">Invoice {detail?.invoiceNumber}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {detail && (
            <>
              <div className="row small mb-2">
                <div className="col-6"><strong>Date:</strong> {(detail.saleDate || '').replace('T', ' ').slice(0, 16)}</div>
                <div className="col-6"><strong>Cashier:</strong> {detail.cashierName}</div>
                <div className="col-6"><strong>Customer:</strong> {detail.customerName}</div>
                <div className="col-6"><strong>Payment:</strong> {detail.paymentMethod}</div>
              </div>
              <Table size="sm">
                <thead><tr><th>Medicine</th><th>Batch</th><th>Qty</th><th>Price</th><th>Total</th></tr></thead>
                <tbody>
                  {detail.items.map((i) => (
                    <tr key={i.id}>
                      <td>{i.medicineName}</td>
                      <td className="small">{i.batchNumber}</td>
                      <td>{i.quantity}</td>
                      <td>{Number(i.unitPrice).toFixed(2)}</td>
                      <td>{Number(i.lineTotal).toFixed(2)}</td>
                    </tr>
                  ))}
                </tbody>
              </Table>
              <div className="text-end">
                <div>Subtotal: LKR {Number(detail.subtotal).toFixed(2)}</div>
                <div>Discount: LKR {Number(detail.discount).toFixed(2)}</div>
                <div className="fw-bold fs-5 text-primary">TOTAL: LKR {Number(detail.total).toFixed(2)}</div>
              </div>
            </>
          )}
        </Modal.Body>
      </Modal>

      <Modal show={!!edit} onHide={() => setEdit(null)} centered>
        <Form onSubmit={saveEdit}>
          <Modal.Header closeButton>
            <Modal.Title className="fs-6">Edit {edit?.invoiceNumber}</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            {edit && (
              <>
                <Form.Label className="small fw-semibold">Payment method *</Form.Label>
                <Form.Select required value={edit.paymentMethod} onChange={(e) => setEdit({ ...edit, paymentMethod: e.target.value })}>
                  <option>CASH</option><option>CARD</option><option>ONLINE</option>
                </Form.Select>
                <Form.Label className="small fw-semibold mt-3">Discount (LKR)</Form.Label>
                <Form.Control type="number" min="0" max={edit.subtotal} step="0.01" required value={edit.discount}
                  onChange={(e) => setEdit({ ...edit, discount: e.target.value })} />
                <div className="small text-muted mt-3">
                  Subtotal: LKR {edit.subtotal.toFixed(2)} &middot;{' '}
                  New total: <strong>LKR {Math.max(edit.subtotal - Number(edit.discount || 0), 0).toFixed(2)}</strong>
                </div>
              </>
            )}
          </Modal.Body>
          <Modal.Footer>
            <Button variant="light" onClick={() => setEdit(null)}>Cancel</Button>
            <Button type="submit" variant="primary">Save</Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <ConfirmDialog
        show={!!confirm} title={confirm?.title} message={confirm?.message} danger
        confirmLabel="Yes, delete"
        onConfirm={() => { confirm?.action(); setConfirm(null) }}
        onCancel={() => setConfirm(null)}
      />
    </div>
  )
}
