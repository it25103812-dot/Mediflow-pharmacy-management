import { useCallback, useEffect, useState } from 'react'
import { Card, Table, Form, InputGroup } from 'react-bootstrap'
import api, { apiError } from '../services/api'
import { useToast } from '../components/Toast'
import Pagination from '../components/Pagination'

export default function AuditLogs() {
  const toast = useToast()
  const [data, setData] = useState(null)
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)

  const load = useCallback(() => {
    const params = new URLSearchParams({ page, size: 20 })
    if (search) params.set('search', search)
    api.get('/audit-logs?' + params.toString()).then((r) => setData(r.data)).catch((e) => toast.error(apiError(e)))
  }, [page, search]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { load() }, [load])

  return (
    <div>
      <h4 className="fw-bold mb-4">Audit Logs</h4>

      <Card className="mb-3">
        <Card.Body className="py-3">
          <div className="row">
            <div className="col-md-5">
              <InputGroup>
                <InputGroup.Text><i className="bi bi-search"></i></InputGroup.Text>
                <Form.Control placeholder="Search action or details..." value={search}
                  onChange={(e) => { setSearch(e.target.value); setPage(0) }} />
              </InputGroup>
            </div>
          </div>
        </Card.Body>
      </Card>

      <Card>
        <Card.Body className="p-0">
          <Table responsive hover className="mb-0 align-middle" size="sm">
            <thead><tr><th>#</th><th>Time</th><th>User</th><th>Action</th><th>Entity</th><th>Details</th></tr></thead>
            <tbody>
              {(data?.content || []).map((a) => (
                <tr key={a.id}>
                  <td>{a.id}</td>
                  <td className="small">{(a.createdAt || '').replace('T', ' ').slice(0, 19)}</td>
                  <td className="fw-semibold small">{a.user ? a.user.fullName : 'System'}</td>
                  <td><span className="badge badge-soft-secondary">{a.action}</span></td>
                  <td className="small">{a.entityType} {a.entityId ? '• ' + a.entityId : ''}</td>
                  <td className="small text-muted">{a.details || '-'}</td>
                </tr>
              ))}
              {data && data.content.length === 0 && (
                <tr><td colSpan="6"><div className="empty-state"><i className="bi bi-clock-history"></i>No audit entries</div></td></tr>
              )}
            </tbody>
          </Table>
        </Card.Body>
      </Card>
      <Pagination page={data?.page || 0} totalPages={data?.totalPages || 0} onChange={setPage} />
    </div>
  )
}
