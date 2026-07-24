# Learning Progress Dashboard

A Jakarta EE / WildFly web application for tracking intern progress, attendance, assignments, and grades.

**Tech stack:** Jakarta EE 10 · WildFly · JAX-RS · JPA/Hibernate · PostgreSQL · Vue 3 · Vite

---

## Prerequisites

Install all of the following before you begin.

| Tool | Version | Check |
|---|---|---|
| Java JDK | 17 or higher | `java -version` |
| Apache Maven | 3.8+ | `mvn -version` |
| Node.js | 18+ | `node -v` |
| WildFly | 27+ (Jakarta EE 10) | — |
| PostgreSQL | 14+ | `psql --version` |
| PostgreSQL JDBC driver | `postgresql-42.7.3.jar` | Download from https://jdbc.postgresql.org/download/ |

---

## Step 1 — Create the Database

Open a terminal and connect to PostgreSQL:

```bash
psql -U postgres
```

Then run:

```sql
CREATE DATABASE progress_dashboard;
```

> **Optional:** The `sql/` folder contains `schema.sql` if you prefer to create
> the tables manually before starting the application. This is not required —
> Hibernate creates all tables automatically on first deployment.
>
> ```bash
> psql -U postgres -d progress_dashboard -f sql/schema.sql
> ```

---

## Step 2 — Deploy the PostgreSQL JDBC Driver into WildFly

Copy the downloaded driver JAR into WildFly's deployments folder:

```
copy postgresql-42.7.3.jar C:\path\to\wildfly\standalone\deployments\
```

Start WildFly:

```
C:\path\to\wildfly\bin\standalone.bat
```

Watch the console — you should see a line confirming `postgresql-42.7.3.jar` deployed successfully.
Do not proceed until WildFly has fully started.

---

## Step 3 — Create the WildFly Datasource

With WildFly running, open a **new** Command Prompt:

```
cd C:\path\to\wildfly\bin
jboss-cli.bat --connect
```

Run this command (adjust `--user-name` and `--password` if your PostgreSQL credentials differ):

```
data-source add --name=PostgresDS --jndi-name=java:/PostgresDS --driver-name=postgresql-42.7.3.jar --connection-url=jdbc:postgresql://localhost:5432/progress_dashboard --user-name=postgres --password=postgres
```

Enable it:

```
/subsystem=datasources/data-source=PostgresDS:enable
```

Verify the connection:

```
/subsystem=datasources/data-source=PostgresDS:test-connection-in-pool
```

You should see `"outcome" => "success"`. Type `quit` to exit.

> The JNDI name `java:/PostgresDS` must match what is in
> `backend/src/main/resources/META-INF/persistence.xml`. Do not change one without the other.

---

## Step 4 — Build and Deploy the Backend

From the project root:

```
cd backend
mvn clean package
```

This produces `backend/target/progress-dashboard.war`. Copy it into WildFly:

```
copy target\progress-dashboard.war C:\path\to\wildfly\standalone\deployments\
```

Watch the WildFly console for a successful deployment message. Verify with:

```
curl http://localhost:8080/progress-dashboard/api/interns
```

Expected response: `[]`

> **Tables are created automatically.** Hibernate's `hbm2ddl.auto=update` creates
> all database tables on first deployment. No SQL scripts need to be run manually.

---

## Step 5 — Run the Frontend

```
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173** in your browser.

---

## Default Login Credentials

On first boot, the application automatically creates two accounts:

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | Admin (Program Coordinator) |
| `trainer` | `trainer123` | Trainer |

> **Change these passwords immediately** after first login.
> Go to **Trainers** in the left sidebar and use the **Reset Password** action.

---

## Project Structure

```
progress-dashboard-wildfly/
├── backend/                     Java / Jakarta EE backend
│   ├── src/main/java/com/dashboard/
│   │   ├── config/              Startup seeder
│   │   ├── dto/                 Request and response DTOs
│   │   ├── entity/              JPA entities (database tables)
│   │   ├── filter/              CORS filter
│   │   ├── repository/          Database access (EJB + JPA)
│   │   ├── resource/            REST API endpoints (JAX-RS)
│   │   └── security/            Auth token, password hashing, filters
│   └── src/main/resources/
│       └── META-INF/
│           └── persistence.xml  JPA / datasource configuration
├── frontend/                    Vue 3 frontend
│   └── src/
│       ├── api/                 Axios client (base URL + token injection)
│       ├── components/          Reusable UI components
│       ├── layouts/             Dashboard shell layout
│       ├── services/            Auth state management
│       ├── views/               Page components (one per route)
│       ├── router.js            Vue Router with role-based guards
│       └── style.css            Global IBM Carbon-inspired design tokens
└── sql/                         Database schema reference
    ├── schema.sql               CREATE TABLE statements for all 7 tables
    ├── seed.sql                 Notes on default accounts (see file)
    └── README.md                Instructions for using the SQL files
```

---

## API Endpoints Reference

| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/api/auth/login` | Public | Log in, receive a token |
| GET | `/api/interns` | Both | List interns |
| POST | `/api/interns` | Admin | Add intern |
| PUT | `/api/interns/{id}` | Admin | Edit intern |
| DELETE | `/api/interns/{id}` | Admin | Delete intern |
| GET | `/api/interns/{id}/progress` | Both | Intern progress summary |
| GET | `/api/interns/{id}/grades` | Both | Intern grade breakdown |
| GET | `/api/assignments` | Both | List assignments (filtered by trainer area) |
| POST | `/api/assignments` | Both | Create assignment |
| GET | `/api/submissions` | Both | All grade records |
| GET | `/api/attendance` | Both | All attendance records |
| POST | `/api/attendance/bulk` | Trainer | Save classroom roster |
| GET | `/api/attendance/summary` | Both | Attendance statistics |
| GET | `/api/trainers` | Admin | List trainer accounts |
| POST | `/api/trainers` | Admin | Create trainer account |
| PUT | `/api/trainers/{id}` | Admin | Edit trainer |
| DELETE | `/api/trainers/{id}` | Admin | Delete trainer |

---

## Troubleshooting

**`data-source add` fails / driver not found**
Double-check that `postgresql-42.7.3.jar` actually deployed (check the WildFly console on startup) and that `--driver-name` matches the exact JAR filename.

**WAR fails to deploy**
Check `wildfly/standalone/log/server.log` for the full error message.

**`curl` returns 401 Unauthorized**
The API requires a Bearer token. Log in via the frontend or use:
```bash
curl -X POST http://localhost:8080/progress-dashboard/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```
Then pass the returned token as `Authorization: Bearer <token>`.

**Frontend shows "Could not load"**
The backend is not running or WildFly stopped. Restart WildFly and redeploy the WAR if needed.

**Tables already exist error when running schema.sql**
The schema file uses `DROP TABLE IF EXISTS ... CASCADE` so it is safe to re-run.

---

## Production Notes

- **SIGNING_SECRET:** The token signing secret in `TokenService.java` should be overridden
  in production via the system property: `-Dauth.signing.secret=your-secret-here`
- **`hbm2ddl.auto`:** Change from `update` to `validate` in `persistence.xml` once the schema is stable
- **HTTPS:** Configure WildFly with an SSL certificate and update `frontend/src/api/client.js`
  to use `https://` before going live
- **Frontend base URL:** Update `baseURL` in `frontend/src/api/client.js` from `localhost:8080`
  to your server's actual hostname or IP address before running `npm run build`
