# Running MediFlow in IntelliJ IDEA

## Folder structure
```
mediflow/
├── pom.xml                 <- Spring Boot 3 / Maven (open THIS folder in IntelliJ)
├── src/
│   └── main/
│       ├── java, resources <- backend (Java 17+)
│       └── frontend/       <- React + Vite
├── database/mediflow.sql   <- MySQL schema + sample data
├── docs/diagrams/          <- diagrams
└── .run/                   <- ready-made IntelliJ run configurations
```

## One-time setup
1. **Install**: JDK 17 or newer, Node.js 18+, MySQL 8.
2. **Database** - run once (MySQL Workbench, or terminal):
   `mysql -u root -p < database/mediflow.sql`
   (If `mediflow_db` already exists from older tests: `DROP DATABASE mediflow_db;` first.)
3. **IntelliJ**: `File > Open...` and select the **mediflow** folder (the one with `pom.xml`).
   Choose *Trust Project*. IntelliJ imports the Maven project `mediflow-backend` automatically.
4. **Project SDK**: `File > Project Structure > Project > SDK` = JDK 17+.
5. **Frontend packages** - open IntelliJ's Terminal tab:
   ```
   cd src/main/frontend
   npm install
   ```
6. **MySQL password** is `root` by default. If yours is different, edit the run configuration
   *Backend (MySQL)* > *Environment variables*: `DB_USERNAME=root;DB_PASSWORD=yourpassword`
   (or edit `src/main/resources/application.properties`).

## Running (top-right run dropdown)
| Run configuration | What it does |
|---|---|
| **MediFlow (Backend + Frontend)** | Starts both with MySQL. Press the green Run button. |
| **MediFlow DEMO (H2 + Frontend)** | Starts both with a built-in in-memory H2 database. No MySQL needed. Data resets on restart. |
| Backend (MySQL) | Backend only, http://localhost:8080 |
| Backend (H2 demo - no MySQL) | Backend only, demo data |
| Frontend (Vite dev) | Frontend only, http://localhost:5173 |

> The *Frontend (Vite dev)* and compound configurations need **IntelliJ IDEA Ultimate** (npm run
> configuration). On **Community Edition** run the backend config from the dropdown, then in the
> Terminal tab: `cd src/main/frontend && npm run dev`.

Open **http://localhost:5173** and log in.

## Logins (password for all: `Admin@123`)
admin@mediflow.com, pharmacist@mediflow.com, cashier@mediflow.com, storekeeper@mediflow.com,
procurement@mediflow.com, cro@mediflow.com, finance@mediflow.com

## Troubleshooting
- **Run config shows "Module not specified / not found"**: open the Maven tool window and click
  *Reload All Maven Projects*; the module is called `mediflow-backend`.
- **`Access denied for user 'root'`**: wrong DB password (see step 6).
- **`Communications link failure`**: MySQL service is not running.
- **`Schema-validation: missing table`**: import `database/mediflow.sql` (do not use other test SQL files).
- **Port 8080 or 5173 busy**: stop the other process, or change `server.port` / `vite.config.js`.
- **Java version error**: set the Project SDK and Maven *Runner JRE* to 17+
  (`Settings > Build > Build Tools > Maven > Runner`).
