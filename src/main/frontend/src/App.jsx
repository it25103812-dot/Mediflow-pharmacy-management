import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import { ToastProvider } from './components/Toast'
import ProtectedRoute from './components/ProtectedRoute'
import Layout from './components/Layout'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Users from './pages/Users'
import Medicines from './pages/Medicines'
import Categories from './pages/Categories'
import Batches from './pages/Batches'
import Inventory from './pages/Inventory'
import Suppliers from './pages/Suppliers'
import PurchaseOrders from './pages/PurchaseOrders'
import Grn from './pages/Grn'
import Customers from './pages/Customers'
import Pos from './pages/Pos'
import Sales from './pages/Sales'
import Prescriptions from './pages/Prescriptions'
import Reports from './pages/Reports'
import Profile from './pages/Profile'
import AuditLogs from './pages/AuditLogs'

const ALL = [
  'ADMINISTRATOR',
  'PHARMACIST',
  'STORE_KEEPER',
  'PROCUREMENT_OFFICER',
  'CASHIER',
  'CUSTOMER_RELATIONS_OFFICER',
  'FINANCE_MANAGER',
]

function Router() {
  const { loading } = useAuth()
  if (loading) {
    return (
      <div className="d-flex justify-content-center align-items-center" style={{ minHeight: '100vh' }}>
        <div className="spinner-border text-primary" role="status" />
      </div>
    )
  }
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route path="/" element={<Dashboard />} />
          <Route path="/profile" element={<Profile />} />
          <Route element={<ProtectedRoute roles={['ADMINISTRATOR']} />}>
            <Route path="/users" element={<Users />} />
            <Route path="/audit-logs" element={<AuditLogs />} />
          </Route>
          <Route element={<ProtectedRoute roles={['ADMINISTRATOR', 'PHARMACIST']} />}>
            <Route path="/categories" element={<Categories />} />
          </Route>
          {/* Procurement officers browse medicines when building purchase orders (read-only) */}
          <Route element={<ProtectedRoute roles={['ADMINISTRATOR', 'PHARMACIST', 'PROCUREMENT_OFFICER']} />}>
            <Route path="/medicines" element={<Medicines />} />
          </Route>
          {/* Cashiers record/look up prescriptions for Rx-only sales (backend already allows it) */}
          <Route element={<ProtectedRoute roles={['ADMINISTRATOR', 'PHARMACIST', 'CASHIER']} />}>
            <Route path="/prescriptions" element={<Prescriptions />} />
          </Route>
          <Route element={<ProtectedRoute roles={['ADMINISTRATOR', 'PHARMACIST', 'STORE_KEEPER']} />}>
            <Route path="/batches" element={<Batches />} />
            <Route path="/inventory" element={<Inventory />} />
          </Route>
          <Route
            element={
              <ProtectedRoute roles={['ADMINISTRATOR', 'PROCUREMENT_OFFICER', 'STORE_KEEPER']} />
            }
          >
            <Route path="/suppliers" element={<Suppliers />} />
            <Route path="/purchase-orders" element={<PurchaseOrders />} />
            <Route path="/grn" element={<Grn />} />
          </Route>
          <Route element={<ProtectedRoute roles={['ADMINISTRATOR', 'CASHIER', 'CUSTOMER_RELATIONS_OFFICER']} />}>
            <Route path="/customers" element={<Customers />} />
          </Route>
          <Route element={<ProtectedRoute roles={['ADMINISTRATOR', 'CASHIER']} />}>
            <Route path="/pos" element={<Pos />} />
          </Route>
          <Route
            element={
              <ProtectedRoute roles={['ADMINISTRATOR', 'CASHIER', 'PHARMACIST', 'FINANCE_MANAGER']} />
            }
          >
            <Route path="/sales" element={<Sales />} />
          </Route>
          <Route
            element={
              <ProtectedRoute
                roles={['ADMINISTRATOR', 'FINANCE_MANAGER', 'PHARMACIST', 'STORE_KEEPER', 'PROCUREMENT_OFFICER']}
              />
            }
          >
            <Route path="/reports" element={<Reports />} />
          </Route>
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <Router />
      </ToastProvider>
    </AuthProvider>
  )
}
