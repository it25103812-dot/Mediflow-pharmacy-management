# MediFlow – Pharmacy Management System

A complete, modern full-stack Pharmacy Management System built for **LankaCare Pharmacy (Pvt) Ltd** as a Software Engineering project.

React frontend talks to a Spring Boot REST API over Axios, backed by Spring Data JPA and MySQL. Login, role-based access control, inventory, purchasing (PO → GRN), a transactional POS, prescriptions, reporting with PDF/CSV export and audit logs are all fully functional — no mocked core data.

```
React (Bootstrap 5)  →  Axios (JWT)  →  Spring Boot REST API  →  Spring Data JPA  →  MySQL
```

> **Running in IntelliJ IDEA?** Open the project root folder and follow [`docs/INTELLIJ_GUIDE.md`](docs/INTELLIJ_GUIDE.md).

---

## ✨ Features

| Module | Highlights |
|---|---|
| **Auth & Users** | JWT login, BCrypt hashing, protected routes, 7 roles, admin staff management, activate/deactivate, change password, audit logs |
| **Dashboard** | Live KPI cards, daily sales chart, monthly revenue chart, top-sellers chart, recent activity — all from real DB queries |
| **Medicines** | CRUD + search/filter/sort/pagination, categories, price history preserved on every change, Rx flag |
| **Batches** | Batch numbers, expiry tracking, per-batch stock, configurable reorder level, EXPIRED / NEAR_EXPIRY / LOW_STOCK statuses |
| **Inventory** | Live stock per batch, adjustments (damage/return/correction/disposal) with full history, low-stock & expiry alerts, no negative stock |
| **Suppliers & PO** | Supplier CRUD, PO lifecycle DRAFT → SENT → PARTIALLY_RECEIVED → RECEIVED (+ CANCELLED) |
| **GRN** | Receive against a PO, batch/expiry capture, **stock increases only on verification**, partial receipts |
| **Customers** | CRUD, search, purchase history |
| **POS** | Medicine search → FEFO batch pick → stock/expiry checks → cart → discount → payment method → transactional checkout with pessimistic locking (no overselling), printable receipt |
| **Prescriptions** | Record prescriptions; Rx-required medicines cannot be sold without one |
| **Reports** | Sales, revenue, medicine sales, top sellers, low stock, expiry, purchase, supplier — date-range filtered, **PDF + CSV export**, print |

### Business rules enforced server-side
- Expired batches are excluded from the POS and rejected at checkout
- Quantity is validated against live stock under a row lock (`PESSIMISTIC_WRITE`) — overselling is impossible
- Rx-required medicines require a linked prescription at sale time
- Unverified GRNs never increase stock
- Price changes always write a `price_history` row
- Every important action lands in `audit_logs`

---

## 🧰 Technologies

- **Frontend:** React 18, React Router 6, Bootstrap 5, Bootstrap Icons, Axios, Chart.js, Vite
- **Backend:** Java 17+, Spring Boot 3.3, Spring Security (JWT), Spring Data JPA, Bean Validation, iText 2.1.7 (PDF), Commons CSV, Maven
- **Database:** MySQL 8.x

---

## 🗂 Project structure

```
MediFlow/
├── database/
│   └── mediflow.sql              # Schema + sample data
├── docs/
│   ├── diagrams/                 # ER / UML diagrams go here
│   ├── SETUP_GUIDE.md
│   └── INTELLIJ_GUIDE.md
├── src/
│   └── main/
│       ├── java/com/mediflow/    # Spring Boot REST API
│       │   ├── config/           # Jackson, data bootstrap
│       │   ├── controller/       # 14 REST controllers
│       │   ├── dto/              # Request/response records
│       │   ├── entity/           # 15 JPA entities
│       │   ├── exception/        # Global exception handling
│       │   ├── repository/       # Spring Data repositories
│       │   ├── security/         # JWT, SecurityConfig (role rules)
│       │   └── service/          # Business logic (14 services)
│       ├── resources/            # application.properties, application-test.properties
│       └── frontend/             # React SPA (Vite)
│           └── src/
│               ├── components/   # Sidebar, layout, toasts, dialogs…
│               ├── context/      # AuthContext
│               ├── pages/        # 16 pages (Dashboard, POS, …)
│               └── services/     # Axios client + interceptors
├── .gitignore
├── README.md
└── pom.xml
```

---

## 🚀 Installation

### Prerequisites
- JDK 17+ (`java -version`)
- Maven 3.8+ (`mvn -v`)
- Node 18+ (`node -v`)
- MySQL 8.x running locally

### 1. MySQL setup

```bash
mysql -u root -p < database/mediflow.sql
```

This creates `mediflow_db` with all 19 tables, FKs, CHECK constraints and sample data
(10 medicines, 15 batches incl. expired/near-expiry ones, suppliers, POs, GRNs, customers, sales).

> Credentials for the app itself are in `src/main/resources/application.properties`
> (`spring.datasource.username/password` – default `root`/`root`). Change them to match your MySQL.

### 2. Backend (port 8080)

```bash
mvn spring-boot:run
```

Verify: <http://localhost:8080/api/auth/health> (no auth needed) or login below.

### 3. Frontend (port 5173)

```bash
cd src/main/frontend
npm install
npm run dev
```

Open <http://localhost:5173>. The Vite dev server proxies `/api/*` to `localhost:8080`.

---

## 🔑 Default logins (development only)

All sample accounts use the password: **`Admin@123`**

| Role | Email |
|---|---|
| Administrator | admin@mediflow.com |
| Pharmacist | pharmacist@mediflow.com |
| Store Keeper | storekeeper@mediflow.com |
| Procurement Officer | procurement@mediflow.com |
| Cashier | cashier@mediflow.com |
| Customer Relations Officer | cro@mediflow.com |
| Finance Manager | finance@mediflow.com |

> ⚠️ **Change these passwords before any real deployment.** They exist for evaluation only.

### Role → module map

| Module | ADMIN | PHARM | STORE | PROC | CASHIER | CRO | FIN |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| Users & audit logs | ✅ | | | | | | |
| Dashboard | ✅ | ✅ | ✅ | ✅ | | ✅ | ✅ |
| Medicines / categories (edit) | ✅ | ✅ | | | | | |
| Batches / inventory | ✅ | ✅ | ✅ | | read | | |
| Suppliers / PO / GRN | ✅ | | ✅ | ✅ | | | |
| POS | ✅ | | | | ✅ | | |
| Sales / bills | ✅ | ✅ | | | ✅ | | ✅ |
| Customers | ✅ | | | | ✅ | ✅ | |
| Prescriptions | ✅ | ✅ | | | ✅ | | |
| Reports | ✅ | ✅ | ✅ | ✅ | | | ✅ |

The sidebar, routes **and** the API (Spring Security `requestMatchers` + `@PreAuthorize`) all enforce this.

---

## 📚 API documentation

Base URL: `/api` — authenticated requests send `Authorization: Bearer <jwt>`.

### Auth
```
POST   /api/auth/login            {email, password} → {token, user}
GET    /api/auth/me               current user
PUT    /api/auth/password         {currentPassword, newPassword}
```

### Users (ADMIN only)
```
GET    /api/users?search&role&active&page&size
POST   /api/users                 {firstName,lastName,email,password,roleName,active}
GET    /api/users/{id}
PUT    /api/users/{id}
PATCH  /api/users/{id}/status     {active:true|false}
```

### Medicines / categories / batches
```
GET    /api/medicines?search&categoryId&active&prescriptionRequired&page&size&sortBy&sortDir
POST   /api/medicines             GET/PUT/DELETE /api/medicines/{id}
GET    /api/medicines/{id}/price-history
GET    /api/categories?all=true | ?search&page&size    POST/PUT/DELETE /api/categories…
GET    /api/batches?search&medicineId&page&size
GET    /api/batches/medicine/{medicineId}              POST/PUT /api/batches…
```

### Inventory
```
GET    /api/inventory?search&medicineId&lowStock&page&size
GET    /api/inventory/alerts
GET    /api/inventory/adjustments?batchId&type&page&size
POST   /api/inventory/adjustments {batchId, adjustmentType, quantityChange, reason}
```

### Suppliers / purchase orders / GRN
```
GET/POST /api/suppliers           GET/PUT/DELETE /api/suppliers/{id}
GET/POST /api/purchase-orders     GET/PUT /api/purchase-orders/{id}
PATCH  /api/purchase-orders/{id}/status   {status:"SENT"|"CANCELLED"}
GET/POST /api/grn
PATCH  /api/grn/{id}/verify       ← increases inventory
PATCH  /api/grn/{id}/cancel
```

### Customers / sales / prescriptions
```
GET/POST /api/customers           GET/PUT/DELETE /api/customers/{id}
GET    /api/customers/{id}/purchases
GET    /api/sales?search&status&start&end&page&size
POST   /api/sales                 {customerId|null, discount, paymentMethod, items:[{batchId,quantity}]}
GET    /api/sales/{id}
GET/POST /api/prescriptions       PUT /api/prescriptions/{id}
```

### Dashboard & reports
```
GET    /api/dashboard/summary | daily-sales?days | monthly-revenue?months | top-selling?limit | recent-activity?limit
GET    /api/reports?type=sales|revenue|medicine-sales|top-selling|low-stock|expiry|purchase|supplier&start&end
GET    /api/reports/export/pdf?type=…    (PDF download)
GET    /api/reports/export/csv?type=…    (CSV download)
```

---

## 🗄 Database design

19 normalized tables with FKs and CHECK constraints (see `database/mediflow.sql`):

`roles`, `users`, `categories`, `medicines`, `medicine_batches`, `price_history`,
`suppliers`, `purchase_orders`, `purchase_order_items`, `goods_received_notes`,
`goods_received_note_items`, `inventory`, `stock_adjustments`, `customers`,
`prescriptions`, `sales`, `sale_items`, `payments`, `audit_logs`

Key relationships:
- `medicine_batches` 1—1 `inventory` (stock is tracked **per batch**)
- `purchase_orders` 1—N `purchase_order_items`; GRN items link back to PO items
- `sales` 1—N `sale_items` (each item pins the exact batch sold) + 1—N `payments`
- `price_history` and `audit_logs` give full traceability

---

## 🧪 Quick end-to-end test

1. Login as **cashier** → POS → click *Cetirizine* → **Complete Sale** → receipt appears, stock drops by 1.
2. Login as **procurement** → GRN → *Receive Against PO* on the sample SENT order → create → **Verify** → stock increases.
3. Login as **admin** → Reports → every report renders; click **PDF**/**CSV** to download.
4. Try selling *Amoxicillin* in the POS → blocked with “requires a prescription”.

---

## 🐞 Troubleshooting

| Problem | Fix |
|---|---|
| `Access denied for user 'root'@'localhost'` | Set your MySQL password in `src/main/resources/application.properties` |
| Port 8080/5173 already in use | Change `server.port` (backend) or `server.port` in `vite.config.js` |
| CORS errors | Add your origin to `mediflow.cors.allowed-origins` in `application.properties` |
| `Public Key Retrieval is not allowed` | Already handled via `allowPublicKeyRetrieval=true` in the JDBC URL |
| Login returns 401 | Run `database/mediflow.sql` to (re)create the sample users |

---

## 👥 Team

| Member | Role | Contribution |
|---|---|---|
| *(Add your team here)* | — | — |

**Course:** Software Engineering • **Client:** LankaCare Pharmacy (Pvt) Ltd

---

## 📄 License

For academic use.

---
## Fixes included in this version
1. `src/main/java/com/mediflow/config/JacksonConfig.java` - dates are now serialized as ISO-8601 strings
   (previously arrays), which fixes the blank Sales / POS receipt / Customer history pages.
2. `src/main/frontend/src/components/Sidebar.jsx` - menu key `ADMIN` renamed to `ADMINISTRATOR`
   so the administrator sidebar menu appears.
