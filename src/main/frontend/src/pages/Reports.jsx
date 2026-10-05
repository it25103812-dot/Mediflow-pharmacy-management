import { useCallback, useEffect, useState } from 'react'
import { Card, Table, Button, Form, InputGroup } from 'react-bootstrap'
import api, { apiError } from '../services/api'
import { useToast } from '../components/Toast'

const REPORTS = [
  { value: 'sales', label: 'Daily / Sales Report' },
  { value: 'revenue', label: 'Revenue Report' },
  { value: 'medicine-sales', label: 'Medicine Sales Report' },
  { value: 'top-selling', label: 'Top Selling Medicines' },
  { value: 'low-stock', label: 'Low Stock Report' },
  { value: 'expiry', label: 'Expiry Report' },
  { value: 'purchase', label: 'Purchase Report' },
  { value: 'supplier', label: 'Supplier Report' },
]

export default function Reports() {
  const toast = useToast()
  const [type, setType] = useState('sales')
  const [start, setStart] = useState('')
  const [end, setEnd] = useState('')
  const [report, setReport] = useState(null)
  const [busy, setBusy] = useState(false)

  const load = useCallback(() => {
    setBusy(true)
    const params = new URLSearchParams({ type })
    if (start) params.set('start', start)
    if (end) params.set('end', end)
    api.get('/reports?' + params.toString())
      .then((r) => setReport(r.data))
      .catch((e) => toast.error(apiError(e)))
      .finally(() => setBusy(false))
  }, [type, start, end]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { load() }, [load])

  /** Download through axios so the Authorization header is sent (a plain link would get 401). */
  const download = async (fmt) => {
    const params = new URLSearchParams({ type })
    if (start) params.set('start', start)
    if (end) params.set('end', end)
    try {
      const res = await api.get(`/reports/export/${fmt}?` + params.toString(), { responseType: 'blob' })
      const url = window.URL.createObjectURL(new Blob([res.data]))
      const a = document.createElement('a')
      a.href = url
      a.download = `${type}-report.${fmt}`
      document.body.appendChild(a)
      a.click()
      a.remove()
      window.URL.revokeObjectURL(url)
    } catch (err) {
      toast.error('Export failed – please try again')
    }
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h4 className="fw-bold mb-0">Reports</h4>
        <div className="d-flex gap-2 no-print">
          <Button variant="outline-danger" size="sm" onClick={() => download('pdf')}>
            <i className="bi bi-file-earmark-pdf me-1"></i>PDF
          </Button>
          <Button variant="outline-success" size="sm" onClick={() => download('csv')}>
            <i className="bi bi-filetype-csv me-1"></i>CSV
          </Button>
          <Button variant="outline-secondary btn-sm" size="sm" onClick={() => window.print()}>
            <i className="bi bi-printer me-1"></i>Print
          </Button>
        </div>
      </div>

      <Card className="mb-3 no-print">
        <Card.Body className="py-3">
          <div className="row g-2">
            <div className="col-md-4">
              <Form.Label className="small fw-semibold mb-1">Report type</Form.Label>
              <Form.Select value={type} onChange={(e) => setType(e.target.value)}>
                {REPORTS.map((r) => <option key={r.value} value={r.value}>{r.label}</option>)}
              </Form.Select>
            </div>
            <div className="col-md-3">
              <Form.Label className="small fw-semibold mb-1">From</Form.Label>
              <Form.Control type="date" value={start} onChange={(e) => setStart(e.target.value)} />
            </div>
            <div className="col-md-3">
              <Form.Label className="small fw-semibold mb-1">To</Form.Label>
              <Form.Control type="date" value={end} onChange={(e) => setEnd(e.target.value)} />
            </div>
          </div>
        </Card.Body>
      </Card>

      <Card>
        <Card.Body>
          {busy && <div className="text-center py-4"><span className="spinner-border text-primary"></span></div>}
          {!busy && report && (
            <>
              <div className="text-center mb-3">
                <h5 className="fw-bold mb-0">LankaCare Pharmacy (Pvt) Ltd</h5>
                <div className="text-muted small">{report.title} • Generated {report.generatedAt}</div>
              </div>
              <div className="table-responsive">
                <Table size="sm" bordered hover className="mb-2">
                  <thead>
                    <tr>{report.columns.map((c) => <th key={c}>{c}</th>)}</tr>
                  </thead>
                  <tbody>
                    {report.rows.map((row, i) => (
                      <tr key={i}>
                        {row.map((cell, j) => <td key={j}>{cell === null || cell === undefined ? '-' : String(cell)}</td>)}
                      </tr>
                    ))}
                    {report.rows.length === 0 && (
                      <tr><td colSpan={report.columns.length}><div className="empty-state">No data for the selected filters</div></td></tr>
                    )}
                  </tbody>
                </Table>
              </div>
              {report.totals && Object.keys(report.totals).length > 0 && (
                <div className="d-flex justify-content-end gap-4 mt-2">
                  {Object.entries(report.totals).map(([k, v]) => (
                    <div key={k} className="text-end">
                      <div className="text-muted" style={{ fontSize: 11, textTransform: 'uppercase' }}>{k}</div>
                      <div className="fw-bold">{typeof v === 'number' && !Number.isInteger(v) ? Number(v).toFixed(2) : String(v)}</div>
                    </div>
                  ))}
                </div>
              )}
            </>
          )}
        </Card.Body>
      </Card>
    </div>
  )
}
