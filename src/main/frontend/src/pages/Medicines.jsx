import { useCallback, useEffect, useState } from 'react'
import { Card, Table, Button, Modal, Form, InputGroup, Badge } from 'react-bootstrap'
import api, { apiError } from '../services/api'
import { useToast } from '../components/Toast'
import ConfirmDialog from '../components/ConfirmDialog'
import Pagination from '../components/Pagination'
import { useAuth } from '../context/AuthContext'

const EMPTY = {
  name: '', genericName: '', categoryId: '', manufacturer: '', unitPrice: '',
  description: '', prescriptionRequired: false, active: true,
}

export default function Medicines() {
  const toast = useToast()
  const { user } = useAuth()
  const isAdmin = user?.roleName === 'ADMINISTRATOR'
  const canEdit = isAdmin || user?.roleName === 'PHARMACIST'

  const [data, setData] = useState(null)
  const [categories, setCategories] = useState([])
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('')
  const [rxOnly, setRxOnly] = useState(false)
  const [page, setPage] = useState(0)
  const [sort, setSort] = useState({ by: 'name', dir: 'asc' })
  const [show, setShow] = useState(false)
  const [form, setForm] = useState(EMPTY)
  const [editId, setEditId] = useState(null)
  const [history, setHistory] = useState(null)
  const [confirm, setConfirm] = useState(null)

  const load = useCallback(() => {
    const params = new URLSearchParams({ page, size: 10, sortBy: sort.by, sortDir: sort.dir })
    if (search) params.set('search', search)
    if (category) params.set('categoryId', category)
    if (rxOnly) params.set('prescriptionRequired', 'true')
    api.get('/medicines?' + params.toString()).then((r) => setData(r.data)).catch((e) => toast.error(apiError(e)))
  }, [page, search, category, rxOnly, sort]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    load()
    api.get('/categories?all=true').then((r) => setCategories(Array.isArray(r.data) ? r.data : r.data.content || [])).catch(() => {})
  }, [load])

  const openCreate = () => { setForm(EMPTY); setEditId(null); setShow(true) }

  const openEdit = (m) => {
    setForm({
      name: m.name, genericName: m.genericName || '', categoryId: m.category?.id || '',
      manufacturer: m.manufacturer || '', unitPrice: m.unitPrice, description: m.description || '',
      prescriptionRequired: m.prescriptionRequired, active: m.active,
    })
    setEditId(m.id)
    setShow(true)
  }

  const save = async (e) => {
    e.preventDefault()
    const payload = { ...form, categoryId: form.categoryId || null, unitPrice: Number(form.unitPrice) }
    try {
      if (editId) {
        await api.put('/medicines/' + editId, payload)
        toast.success('Medicine updated (price changes are recorded in history)')
      } else {
        await api.post('/medicines', payload)
        toast.success('Medicine added')
      }
      setShow(false)
      load()
    } catch (err) {
      toast.error(apiError(err))
    }
  }

  const remove = (m) => {
    setConfirm({
      title: 'Delete medicine',
      message: `Delete "${m.name}"? If it already has stock, orders or sales history, it will be deactivated instead.`,
      danger: true,
      action: async () => {
        try {
          const res = await api.delete('/medicines/' + m.id)
          toast.success(res.data?.deleted ? 'Medicine deleted' : 'Medicine has history, so it was deactivated instead')
          load()
        } catch (err) {
          toast.error(apiError(err))
        }
      },
    })
  }

  const showHistory = (m) => {
    api.get('/medicines/' + m.id + '/price-history').then((r) => setHistory({ medicine: m, rows: r.data })).catch((e) => toast.error(apiError(e)))
  }

  const sortBtn = (by, label) => (
    <th className="clickable" onClick={() => setSort((s) => ({ by, dir: s.by === by && s.dir === 'asc' ? 'desc' : 'asc' }))}>
      {label} {sort.by === by && <i className={`bi ${sort.dir === 'asc' ? 'bi-caret-up-fill' : 'bi-caret-down-fill'} small`}></i>}
    </th>
  )

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h4 className="fw-bold mb-0">Medicine Management</h4>
        {canEdit && (
          <Button variant="primary" onClick={openCreate}>
            <i className="bi bi-plus-lg me-2"></i>Add Medicine
          </Button>
        )}
      </div>

      <Card className="mb-3">
        <Card.Body className="py-3">
          <div className="row g-2">
            <div className="col-md-5">
              <InputGroup>
                <InputGroup.Text><i className="bi bi-search"></i></InputGroup.Text>
                <Form.Control placeholder="Search name or generic name..." value={search}
                  onChange={(e) => { setSearch(e.target.value); setPage(0) }} />
              </InputGroup>
            </div>
            <div className="col-md-4">
              <Form.Select value={category} onChange={(e) => { setCategory(e.target.value); setPage(0) }}>
                <option value="">All categories</option>
                {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </Form.Select>
            </div>
            <div className="col-md-3 d-flex align-items-center">
              <Form.Check type="switch" label="Prescription only" checked={rxOnly}
                onChange={(e) => { setRxOnly(e.target.checked); setPage(0) }} />
            </div>
          </div>
        </Card.Body>
      </Card>

      <Card>
        <Card.Body className="p-0">
          <Table responsive hover className="mb-0 align-middle">
            <thead>
              <tr>
                <th>#</th>
                {sortBtn('name', 'Medicine')}
                <th>Generic</th><th>Category</th><th>Manufacturer</th>
                {sortBtn('unitPrice', 'Unit Price')}
                <th>Rx</th><th>Status</th><th></th>
              </tr>
            </thead>
            <tbody>
              {(data?.content || []).map((m) => (
                <tr key={m.id} className={m.active ? '' : 'table-light'}>
                  <td>{m.id}</td>
                  <td className="fw-semibold">{m.name}</td>
                  <td className="text-muted">{m.genericName || '-'}</td>
                  <td>{m.category?.name || '-'}</td>
                  <td>{m.manufacturer || '-'}</td>
                  <td>LKR {Number(m.unitPrice).toFixed(2)}</td>
                  <td>{m.prescriptionRequired ? <span className="badge badge-soft-warning">Rx</span> : <span className="badge badge-soft-secondary">OTC</span>}</td>
                  <td>{m.active ? <span className="badge badge-soft-success">ACTIVE</span> : <span className="badge badge-soft-danger">INACTIVE</span>}</td>
                  <td className="text-end text-nowrap">
                    <Button size="sm" variant="light" title="Price history" className="me-1" onClick={() => showHistory(m)}>
                      <i className="bi bi-clock-history"></i>
                    </Button>
                    {canEdit && (
                      <Button size="sm" variant="light" title="Edit" className="me-1" onClick={() => openEdit(m)}>
                        <i className="bi bi-pencil"></i>
                      </Button>
                    )}
                    {isAdmin && (
                      <Button size="sm" variant="outline-danger" title="Delete" onClick={() => remove(m)}>
                        <i className="bi bi-trash"></i>
                      </Button>
                    )}
                  </td>
                </tr>
              ))}
              {data && data.content.length === 0 && (
                <tr><td colSpan="9"><div className="empty-state"><i className="bi bi-capsule"></i>No medicines found</div></td></tr>
              )}
            </tbody>
          </Table>
        </Card.Body>
      </Card>
      <Pagination page={data?.page || 0} totalPages={data?.totalPages || 0} onChange={setPage} />

      <Modal show={show} onHide={() => setShow(false)} centered size="lg">
        <Form onSubmit={save}>
          <Modal.Header closeButton>
            <Modal.Title className="fs-6">{editId ? 'Edit Medicine' : 'New Medicine'}</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <div className="row g-3">
              <div className="col-md-8">
                <Form.Label className="small fw-semibold">Medicine name *</Form.Label>
                <Form.Control required maxLength={150} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
              </div>
              <div className="col-md-4">
                <Form.Label className="small fw-semibold">Generic name</Form.Label>
                <Form.Control maxLength={150} value={form.genericName} onChange={(e) => setForm({ ...form, genericName: e.target.value })} />
              </div>
              <div className="col-md-4">
                <Form.Label className="small fw-semibold">Category</Form.Label>
                <Form.Select value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })}>
                  <option value="">- none -</option>
                  {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                </Form.Select>
              </div>
              <div className="col-md-4">
                <Form.Label className="small fw-semibold">Manufacturer</Form.Label>
                <Form.Control maxLength={120} value={form.manufacturer} onChange={(e) => setForm({ ...form, manufacturer: e.target.value })} />
              </div>
              <div className="col-md-4">
                <Form.Label className="small fw-semibold">Unit price (LKR) *</Form.Label>
                <Form.Control type="number" step="0.01" min="0" required value={form.unitPrice}
                  onChange={(e) => setForm({ ...form, unitPrice: e.target.value })} />
              </div>
              <div className="col-12">
                <Form.Label className="small fw-semibold">Description</Form.Label>
                <Form.Control as="textarea" rows={2} maxLength={500} value={form.description}
                  onChange={(e) => setForm({ ...form, description: e.target.value })} />
              </div>
              <div className="col-md-6">
                <Form.Check type="switch" label="Prescription required" checked={form.prescriptionRequired}
                  onChange={(e) => setForm({ ...form, prescriptionRequired: e.target.checked })} />
              </div>
              <div className="col-md-6">
                <Form.Check type="switch" label="Active" checked={form.active}
                  onChange={(e) => setForm({ ...form, active: e.target.checked })} />
              </div>
            </div>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="light" onClick={() => setShow(false)}>Cancel</Button>
            <Button type="submit" variant="primary">{editId ? 'Save Changes' : 'Add Medicine'}</Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <Modal show={!!history} onHide={() => setHistory(null)} centered>
        <Modal.Header closeButton>
          <Modal.Title className="fs-6">Price History – {history?.medicine?.name}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {(history?.rows || []).length === 0 && <div className="empty-state">No price changes recorded</div>}
          <Table size="sm">
            <thead><tr><th>Changed</th><th>Old</th><th>New</th><th>By</th></tr></thead>
            <tbody>
              {(history?.rows || []).map((h) => (
                <tr key={h.id}>
                  <td className="small">{(h.changedAt || '').replace('T', ' ').slice(0, 16)}</td>
                  <td>LKR {Number(h.oldPrice).toFixed(2)}</td>
                  <td className="fw-semibold">LKR {Number(h.newPrice).toFixed(2)}</td>
                  <td className="small">{h.changedBy ? h.changedBy.fullName : 'System'}</td>
                </tr>
              ))}
            </tbody>
          </Table>
        </Modal.Body>
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
