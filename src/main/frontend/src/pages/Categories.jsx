import { useCallback, useEffect, useState } from 'react'
import { Card, Table, Button, Modal, Form, InputGroup } from 'react-bootstrap'
import api, { apiError } from '../services/api'
import { useToast } from '../components/Toast'
import Pagination from '../components/Pagination'
import ConfirmDialog from '../components/ConfirmDialog'
import { useAuth } from '../context/AuthContext'

export default function Categories() {
  const toast = useToast()
  const { user } = useAuth()
  const isAdmin = user?.roleName === 'ADMINISTRATOR'
  const canEdit = isAdmin || user?.roleName === 'PHARMACIST'

  const [data, setData] = useState(null)
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [show, setShow] = useState(false)
  const [form, setForm] = useState({ name: '', description: '', active: true })
  const [confirm, setConfirm] = useState(null)
  const [editId, setEditId] = useState(null)

  const load = useCallback(() => {
    const params = new URLSearchParams({ page, size: 10 })
    if (search) params.set('search', search)
    api.get('/categories?' + params.toString()).then((r) => setData(r.data)).catch((e) => toast.error(apiError(e)))
  }, [page, search]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { load() }, [load])

  const save = async (e) => {
    e.preventDefault()
    try {
      if (editId) {
        await api.put('/categories/' + editId, form)
        toast.success('Category updated')
      } else {
        await api.post('/categories', form)
        toast.success('Category created')
      }
      setShow(false)
      load()
    } catch (err) {
      toast.error(apiError(err))
    }
  }

  const remove = (c) => {
    setConfirm({
      title: 'Delete category',
      message: `Delete "${c.name}"? If medicines already use it, it will be deactivated instead.`,
      action: async () => {
        try {
          const res = await api.delete('/categories/' + c.id)
          toast.success(res.data?.deleted ? 'Category deleted' : 'Category is in use, so it was deactivated instead')
          load()
        } catch (err) {
          toast.error(apiError(err))
        }
      },
    })
  }

  const reactivate = async (c) => {
    try {
      await api.put('/categories/' + c.id, { name: c.name, description: c.description || '', active: true })
      toast.success('Category activated')
      load()
    } catch (err) {
      toast.error(apiError(err))
    }
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h4 className="fw-bold mb-0">Categories</h4>
        {canEdit && (
          <Button variant="primary" onClick={() => { setForm({ name: '', description: '', active: true }); setEditId(null); setShow(true) }}>
            <i className="bi bi-plus-lg me-2"></i>Add Category
          </Button>
        )}
      </div>

      <Card className="mb-3">
        <Card.Body className="py-3">
          <div className="row">
            <div className="col-md-5">
              <InputGroup>
                <InputGroup.Text><i className="bi bi-search"></i></InputGroup.Text>
                <Form.Control placeholder="Search categories..." value={search}
                  onChange={(e) => { setSearch(e.target.value); setPage(0) }} />
              </InputGroup>
            </div>
          </div>
        </Card.Body>
      </Card>

      <Card>
        <Card.Body className="p-0">
          <Table responsive hover className="mb-0 align-middle">
            <thead><tr><th>#</th><th>Category Name</th><th>Description</th><th>Status</th>{canEdit && <th></th>}</tr></thead>
            <tbody>
              {(data?.content || []).map((c) => (
                <tr key={c.id}>
                  <td>{c.id}</td>
                  <td className="fw-semibold">{c.name}</td>
                  <td className="text-muted">{c.description || '-'}</td>
                  <td>{c.active ? <span className="badge badge-soft-success">ACTIVE</span> : <span className="badge badge-soft-danger">INACTIVE</span>}</td>
                  {canEdit && (
                    <td className="text-end">
                      <Button size="sm" variant="light" title="Edit" className="me-1"
                        onClick={() => { setForm({ name: c.name, description: c.description || '', active: !!c.active }); setEditId(c.id); setShow(true) }}>
                        <i className="bi bi-pencil"></i>
                      </Button>
                      {isAdmin && (
                        <Button size="sm" variant="outline-danger" title="Delete" onClick={() => remove(c)}>
                          <i className="bi bi-trash"></i>
                        </Button>
                      )}
                      {!c.active && (
                        <Button size="sm" variant="outline-success" title="Activate" onClick={() => reactivate(c)}>
                          <i className="bi bi-arrow-counterclockwise"></i>
                        </Button>
                      )}
                    </td>
                  )}
                </tr>
              ))}
              {data && data.content.length === 0 && (
                <tr><td colSpan="5"><div className="empty-state"><i className="bi bi-tags"></i>No categories found</div></td></tr>
              )}
            </tbody>
          </Table>
        </Card.Body>
      </Card>
      <Pagination page={data?.page || 0} totalPages={data?.totalPages || 0} onChange={setPage} />

      <Modal show={show} onHide={() => setShow(false)} centered>
        <Form onSubmit={save}>
          <Modal.Header closeButton>
            <Modal.Title className="fs-6">{editId ? 'Edit Category' : 'New Category'}</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <Form.Label className="small fw-semibold">Category name *</Form.Label>
            <Form.Control required maxLength={80} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
            <Form.Label className="small fw-semibold mt-3">Description</Form.Label>
            <Form.Control as="textarea" rows={2} maxLength={255} value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })} />
            <Form.Check className="mt-3" type="switch" label="Active" checked={form.active}
              onChange={(e) => setForm({ ...form, active: e.target.checked })} />
          </Modal.Body>
          <Modal.Footer>
            <Button variant="light" onClick={() => setShow(false)}>Cancel</Button>
            <Button type="submit" variant="primary">Save</Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <ConfirmDialog
        show={!!confirm} title={confirm?.title} message={confirm?.message} danger
        confirmLabel="Yes, deactivate"
        onConfirm={() => { confirm?.action(); setConfirm(null) }}
        onCancel={() => setConfirm(null)}
      />
    </div>
  )
}
