import { NavLink } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

/**
 * Menu visibility is derived from the user's role so each role only sees
 * the modules it is allowed to use (matching backend security rules).
 */
const MENU = {
  ADMINISTRATOR: [
    { section: 'Overview', items: [{ to: '/', icon: 'bi-speedometer2', label: 'Dashboard' }] },
    {
      section: 'Management',
      items: [
        { to: '/users', icon: 'bi-people', label: 'Users' },
        { to: '/medicines', icon: 'bi-capsule', label: 'Medicines' },
        { to: '/categories', icon: 'bi-tags', label: 'Categories' },
        { to: '/batches', icon: 'bi-box-seam', label: 'Batches' },
        { to: '/inventory', icon: 'bi-clipboard-data', label: 'Inventory' },
        { to: '/suppliers', icon: 'bi-truck', label: 'Suppliers' },
        { to: '/purchase-orders', icon: 'bi-cart-check', label: 'Purchase Orders' },
        { to: '/grn', icon: 'bi-box-arrow-in-down', label: 'GRN' },
        { to: '/customers', icon: 'bi-person-vcard', label: 'Customers' },
        { to: '/sales', icon: 'bi-receipt', label: 'Sales' },
        { to: '/prescriptions', icon: 'bi-clipboard2-pulse', label: 'Prescriptions' },
      ],
    },
    {
      section: 'Analytics',
      items: [
        { to: '/reports', icon: 'bi-graph-up', label: 'Reports' },
        { to: '/audit-logs', icon: 'bi-clock-history', label: 'Audit Logs' },
      ],
    },
  ],
  PHARMACIST: [
    { section: 'Overview', items: [{ to: '/', icon: 'bi-speedometer2', label: 'Dashboard' }] },
    {
      section: 'Pharmacy',
      items: [
        { to: '/medicines', icon: 'bi-capsule', label: 'Medicines' },
        { to: '/categories', icon: 'bi-tags', label: 'Categories' },
        { to: '/batches', icon: 'bi-box-seam', label: 'Batches' },
        { to: '/inventory', icon: 'bi-clipboard-data', label: 'Inventory' },
        { to: '/prescriptions', icon: 'bi-clipboard2-pulse', label: 'Prescriptions' },
        { to: '/sales', icon: 'bi-receipt', label: 'Sales' },
      ],
    },
  ],
  STORE_KEEPER: [
    { section: 'Overview', items: [{ to: '/', icon: 'bi-speedometer2', label: 'Dashboard' }] },
    {
      section: 'Warehouse',
      items: [
        { to: '/inventory', icon: 'bi-clipboard-data', label: 'Inventory' },
        { to: '/batches', icon: 'bi-box-seam', label: 'Batches' },
        { to: '/suppliers', icon: 'bi-truck', label: 'Suppliers' },
        { to: '/purchase-orders', icon: 'bi-cart-check', label: 'Purchase Orders' },
        { to: '/grn', icon: 'bi-box-arrow-in-down', label: 'GRN' },
      ],
    },
  ],
  PROCUREMENT_OFFICER: [
    { section: 'Overview', items: [{ to: '/', icon: 'bi-speedometer2', label: 'Dashboard' }] },
    {
      section: 'Procurement',
      items: [
        { to: '/suppliers', icon: 'bi-truck', label: 'Suppliers' },
        { to: '/purchase-orders', icon: 'bi-cart-check', label: 'Purchase Orders' },
        { to: '/grn', icon: 'bi-box-arrow-in-down', label: 'GRN' },
        { to: '/medicines', icon: 'bi-capsule', label: 'Medicines' },
      ],
    },
  ],
  CASHIER: [
    { section: 'Counter', items: [{ to: '/pos', icon: 'bi-cart3', label: 'Point of Sale' }] },
    {
      section: 'Records',
      items: [
        { to: '/sales', icon: 'bi-receipt', label: 'Sales / Bills' },
        { to: '/customers', icon: 'bi-person-vcard', label: 'Customers' },
        { to: '/prescriptions', icon: 'bi-clipboard2-pulse', label: 'Prescriptions' },
      ],
    },
  ],
  CUSTOMER_RELATIONS_OFFICER: [
    { section: 'Customers', items: [{ to: '/customers', icon: 'bi-person-vcard', label: 'Customers' }] },
    { section: 'Overview', items: [{ to: '/', icon: 'bi-speedometer2', label: 'Dashboard' }] },
  ],
  FINANCE_MANAGER: [
    { section: 'Overview', items: [{ to: '/', icon: 'bi-speedometer2', label: 'Dashboard' }] },
    {
      section: 'Finance',
      items: [
        { to: '/sales', icon: 'bi-receipt', label: 'Sales' },
        { to: '/reports', icon: 'bi-graph-up', label: 'Reports' },
      ],
    },
  ],
}

const ROLE_TITLES = {
  ADMINISTRATOR: 'Administrator',
  PHARMACIST: 'Pharmacist',
  STORE_KEEPER: 'Store Keeper',
  PROCUREMENT_OFFICER: 'Procurement Officer',
  CASHIER: 'Cashier',
  CUSTOMER_RELATIONS_OFFICER: 'Customer Relations',
  FINANCE_MANAGER: 'Finance Manager',
}

export default function Sidebar() {
  const { user, logout } = useAuth()
  const menu = MENU[user?.roleName] || []

  return (
    <aside className="mf-sidebar">
      <div className="brand">
        <div className="logo">
          <i className="bi bi-heart-pulse-fill"></i>
        </div>
        <div>
          <h5>MediFlow</h5>
          <small>LankaCare Pharmacy</small>
        </div>
      </div>
      <nav className="mf-nav">
        {menu.map((group) => (
          <div key={group.section}>
            <div className="section">{group.section}</div>
            {group.items.map((item) => (
              <NavLink key={item.to} to={item.to} end={item.to === '/'}>
                <i className={`bi ${item.icon}`}></i>
                <span>{item.label}</span>
              </NavLink>
            ))}
          </div>
        ))}
      </nav>
      <div className="p-3 border-top border-light border-opacity-10 small text-white-50">
        {ROLE_TITLES[user?.roleName] || user?.roleName}
      </div>
    </aside>
  )
}

export { ROLE_TITLES }
