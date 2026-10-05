import { useEffect, useRef, useState } from 'react'
import { Card, Row, Col } from 'react-bootstrap'
import Chart from 'chart.js/auto'
import api from '../services/api'
import { useAuth } from '../context/AuthContext'

function StatCard({ icon, color, label, value }) {
  return (
    <Card className="stat-card">
      <Card.Body className="d-flex align-items-center gap-3">
        <div className="icon-box" style={{ background: color }}>
          <i className={`bi ${icon}`}></i>
        </div>
        <div>
          <div className="stat-value">{value}</div>
          <p className="stat-label mb-0">{label}</p>
        </div>
      </Card.Body>
    </Card>
  )
}

/** Tiny line/bar chart helper built on Chart.js */
function useChart(canvasRef, buildFn, data, deps) {
  const chartRef = useRef(null)
  useEffect(() => {
    if (!canvasRef.current || !window.Chart) return
    if (chartRef.current) chartRef.current.destroy()
    chartRef.current = buildFn(canvasRef.current)
    return () => {
      if (chartRef.current) chartRef.current.destroy()
      chartRef.current = null
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)
  return chartRef
}

export default function Dashboard() {
  const { user } = useAuth()
  const [summary, setSummary] = useState(null)
  const [daily, setDaily] = useState([])
  const [monthly, setMonthly] = useState([])
  const [top, setTop] = useState([])
  const [activity, setActivity] = useState([])

  const dailyCanvas = useRef(null)
  const monthlyCanvas = useRef(null)
  const topCanvas = useRef(null)

  useEffect(() => {
    document.title = 'MediFlow – Dashboard'
    api.get('/dashboard/summary').then((r) => setSummary(r.data)).catch(() => {})
    api.get('/dashboard/daily-sales?days=14').then((r) => setDaily(r.data)).catch(() => {})
    api.get('/dashboard/monthly-revenue?months=12').then((r) => setMonthly(r.data)).catch(() => {})
    api.get('/dashboard/top-selling?limit=5').then((r) => setTop(r.data)).catch(() => {})
    api.get('/dashboard/recent-activity?limit=8').then((r) => setActivity(r.data)).catch(() => {})
  }, [])

  useEffect(() => {
    if (!dailyCanvas.current || daily.length === 0) return
    const chart = new Chart(dailyCanvas.current, {
      type: 'line',
      data: {
        labels: daily.map((d) => d.label),
        datasets: [
          {
            label: 'Revenue (LKR)',
            data: daily.map((d) => d.revenue),
            borderColor: '#0d6efd',
            backgroundColor: 'rgba(13,110,253,.12)',
            fill: true,
            tension: 0.35,
          },
        ],
      },
      options: { plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } },
    })
    return () => chart.destroy()
  }, [daily])

  useEffect(() => {
    if (!monthlyCanvas.current || monthly.length === 0) return
    const chart = new Chart(monthlyCanvas.current, {
      type: 'bar',
      data: {
        labels: monthly.map((d) => d.label),
        datasets: [
          {
            label: 'Revenue (LKR)',
            data: monthly.map((d) => d.revenue),
            backgroundColor: '#16a394',
            borderRadius: 5,
          },
        ],
      },
      options: { plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } },
    })
    return () => chart.destroy()
  }, [monthly])

  useEffect(() => {
    if (!topCanvas.current || top.length === 0) return
    const chart = new Chart(topCanvas.current, {
      type: 'bar',
      data: {
        labels: top.map((d) => d.medicineName),
        datasets: [
          {
            label: 'Units sold',
            data: top.map((d) => d.totalQty),
            backgroundColor: '#f0a202',
            borderRadius: 5,
          },
        ],
      },
      options: {
        indexAxis: 'y',
        plugins: { legend: { display: false } },
        scales: { x: { beginAtZero: true } },
      },
    })
    return () => chart.destroy()
  }, [top])

  const money = (v) =>
    'LKR ' + Number(v || 0).toLocaleString('en-LK', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h4 className="fw-bold mb-0">Dashboard</h4>
          <span className="text-muted small">Welcome back, {user?.firstName} 👋</span>
        </div>
        <span className="badge bg-primary-subtle text-primary-emphasis">
          <i className="bi bi-calendar-day me-1"></i>
          {new Date().toLocaleDateString('en-GB', { day: 'numeric', month: 'long', year: 'numeric' })}
        </span>
      </div>

      <Row className="g-3 mb-4">
        <Col md={6} xl={3}>
          <StatCard icon="bi-cash-stack" color="#0d6efd" label="Today's Revenue" value={money(summary?.todayRevenue)} />
        </Col>
        <Col md={6} xl={3}>
          <StatCard icon="bi-graph-up-arrow" color="#16a394" label="Total Revenue" value={money(summary?.totalRevenue)} />
        </Col>
        <Col md={6} xl={3}>
          <StatCard icon="bi-capsule" color="#f0a202" label="Total Medicines" value={summary?.totalMedicines ?? '–'} />
        </Col>
        <Col md={6} xl={3}>
          <StatCard icon="bi-boxes" color="#7c3aed" label="Total Stock (units)" value={summary?.totalStock ?? '–'} />
        </Col>
        <Col md={6} xl={3}>
          <StatCard icon="bi-exclamation-triangle" color="#dc3545" label="Low Stock" value={summary?.lowStockCount ?? '–'} />
        </Col>
        <Col md={6} xl={3}>
          <StatCard icon="bi-hourglass-split" color="#fd7e14" label="Near Expiry" value={summary?.nearExpiryCount ?? '–'} />
        </Col>
        <Col md={6} xl={3}>
          <StatCard icon="bi-x-octagon" color="#b02a37" label="Expired" value={summary?.expiredCount ?? '–'} />
        </Col>
        <Col md={6} xl={3}>
          <StatCard icon="bi-cart-check" color="#0dcaf0" label="Pending POs" value={summary?.pendingPurchaseOrders ?? '–'} />
        </Col>
        <Col md={6} xl={3}>
          <StatCard icon="bi-people" color="#198754" label="Total Customers" value={summary?.totalCustomers ?? '–'} />
        </Col>
        <Col md={6} xl={3}>
          <StatCard icon="bi-receipt-cutoff" color="#20c997" label="Today's Orders" value={summary?.todaySales ?? '–'} />
        </Col>
        <Col md={6} xl={3}>
          <StatCard icon="bi-box-arrow-in-down" color="#6f42c1" label="Pending GRNs" value={summary?.pendingGrn ?? '–'} />
        </Col>
        <Col md={6} xl={3}>
          <StatCard icon="bi-person-badge" color="#0f2b46" label="System Users" value={summary?.totalUsers ?? '–'} />
        </Col>
      </Row>

      <Row className="g-3 mb-4">
        <Col lg={7}>
          <Card>
            <Card.Header>Daily Sales – Last 14 Days</Card.Header>
            <Card.Body>
              <canvas ref={dailyCanvas} height="110" />
            </Card.Body>
          </Card>
        </Col>
        <Col lg={5}>
          <Card>
            <Card.Header>Monthly Revenue – Last 12 Months</Card.Header>
            <Card.Body>
              <canvas ref={monthlyCanvas} height="110" />
            </Card.Body>
          </Card>
        </Col>
      </Row>

      <Row className="g-3">
        <Col lg={7}>
          <Card>
            <Card.Header>Top Selling Medicines</Card.Header>
            <Card.Body>
              <canvas ref={topCanvas} height="140" />
            </Card.Body>
          </Card>
        </Col>
        <Col lg={5}>
          <Card>
            <Card.Header>Recent Activity</Card.Header>
            <Card.Body className="p-0">
              <div style={{ maxHeight: 320, overflowY: 'auto' }}>
                {activity.length === 0 && <div className="empty-state">No activity yet</div>}
                {activity.map((a) => (
                  <div key={a.id} className="d-flex gap-2 px-3 py-2 border-bottom">
                    <i className="bi bi-activity text-primary mt-1"></i>
                    <div className="small">
                      <div>
                        <span className="fw-semibold">{a.user}</span>{' '}
                        <span className="badge bg-light text-dark border">{a.action}</span>
                      </div>
                      <div className="text-muted" style={{ fontSize: 12 }}>
                        {a.details} {a.createdAt ? '• ' + a.createdAt.replace('T', ' ').slice(0, 16) : ''}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </div>
  )
}
