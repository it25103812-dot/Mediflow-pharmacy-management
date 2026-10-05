# MediFlow – Setup Guide (step by step)

## 0. Prerequisites check

```bash
java -version     # need 17+
mvn -v            # need 3.8+
node -v           # need 18+
mysql --version   # need 8.x
```

## 1. Database

```bash
mysql -u root -p < database/mediflow.sql
```

Creates database `mediflow_db`, 19 tables, sample data and 7 login accounts.
Password for all sample accounts: `Admin@123`.

If your MySQL credentials differ, edit:

```
src/main/resources/application.properties
  spring.datasource.username=root
  spring.datasource.password=root
```

## 2. Backend

```bash
mvn spring-boot:run
```

- Starts on http://localhost:8080
- Logs: console output (or `server.log` when run from the packaged jar)
- Test the API:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@mediflow.com","password":"Admin@123"}'
```

You should receive a JWT token.

### Build a production jar

```bash
mvn clean package
java -jar target/mediflow-backend-1.0.0.jar
```

### Run tests / smoke check without MySQL (H2)

```bash
mvn test -Dspring.profiles.active=test
# or run the app with: java -jar target/mediflow-backend-1.0.0.jar --spring.profiles.active=test
```

The `test` profile creates an in-memory schema and seeds demo data — useful for CI
or quick evaluation without a MySQL server. **Real deployments must use MySQL.**

## 3. Frontend

```bash
cd src/main/frontend
npm install
npm run dev
```

- Opens on http://localhost:5173
- Vite proxies `/api/*` → `http://localhost:8080` (see `vite.config.js`)
- Production build: `npm run build` → static files in `src/main/frontend/dist`

## 4. Login and explore

| Try this | Expected result |
|---|---|
| Admin → Dashboard | Live KPIs + 3 charts from DB |
| Admin → Medicines → edit a price | Old price saved in price history (clock icon) |
| Store Keeper → Inventory → Adjustment | Stock changes, history recorded |
| Procurement → GRN → create + verify | Inventory increases ONLY after verify |
| Cashier → POS → sell item | Stock deducted transactionally, receipt shown |
| Cashier → POS → add Amoxicillin | Blocked: prescription required |
| Any role → try opening an admin URL | “Access denied” / API returns 403 |

## 5. Common errors

| Error | Cause | Fix |
|---|---|---|
| `Table 'mediflow_db.users' doesn't exist` | SQL script not run | Run step 1 |
| `Access denied for user` | Wrong DB password | Edit `application.properties` |
| `Communications link failure` | MySQL not running | Start the MySQL service |
| Port 8080 in use | Another app | `server.port=8081` in properties (update vite proxy target too) |
| `npm install` fails | Old Node | Use Node 18+ |
| 401 after restarting backend | Old token in localStorage | Log out and back in (JWT secret is stable) |

## 6. Git quick start

```bash
git init
git add .
git commit -m "MediFlow – Pharmacy Management System"
git branch -M main
git remote add origin https://github.com/<your-team>/MediFlow-Pharmacy-Management-System.git
git push -u origin main
```
