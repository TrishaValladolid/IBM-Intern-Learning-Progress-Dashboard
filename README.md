# Learning Progress Dashboard — Today's MVP (Jakarta EE / WildFly)

Real Jakarta EE this time: JAX-RS + JPA (Hibernate, via WildFly) + EJB, packaged
as a WAR, deployed to your existing WildFly.

Scope for today: Intern CRUD, Assignment CRUD, score entry, and the progress
tracker (completion % + average score %). Team assignments, attendance,
full analytics dashboard, reporting/CSV, and auth are cut — mention them as
"next phase" in your demo.

## 1. Create the database

In PostgreSQL:

```sql
CREATE DATABASE progress_dashboard;
```

## 2. Deploy the PostgreSQL JDBC driver into WildFly

Download the driver jar (matches the version in `pom.xml`):
https://jdbc.postgresql.org/download/ → get `postgresql-42.7.3.jar`

Copy it into WildFly's deployments folder:

```
copy postgresql-42.7.3.jar C:\path\to\wildfly\standalone\deployments\
```

Start WildFly (`standalone.bat` in `wildfly\bin`) and check the console —
you should see it deploy the jar as a JDBC driver automatically (look for a
line mentioning `postgresql-42.7.3.jar` deployed).

## 3. Create the datasource in WildFly

With WildFly running, open a **new** Command Prompt:

```
cd C:\path\to\wildfly\bin
jboss-cli.bat --connect
```

Then run this single command (adjust username/password if yours differ):

```
data-source add --name=PostgresDS --jndi-name=java:/PostgresDS --driver-name=postgresql-42.7.3.jar --connection-url=jdbc:postgresql://localhost:5432/progress_dashboard --user-name=postgres --password=postgres
```

Then enable it:

```
/subsystem=datasources/data-source=PostgresDS:enable
```

Verify it's alive:

```
/subsystem=datasources/data-source=PostgresDS:test-connection-in-pool
```

You should see `"outcome" => "success"`. Type `quit` to exit the CLI.

> This JNDI name (`java:/PostgresDS`) must match what's in
> `backend/src/main/resources/META-INF/persistence.xml` — it already does,
> don't change one without the other.

## 4. Build and deploy the backend

```
cd backend
mvn clean package
```

This produces `target/progress-dashboard.war`. Copy it into WildFly's
deployments folder:

```
copy target\progress-dashboard.war C:\path\to\wildfly\standalone\deployments\
```

Watch the WildFly console — it should log that the WAR deployed
successfully. Test it:

```
curl http://localhost:8080/progress-dashboard/api/interns
```

Should return `[]`. Note the URL includes `/progress-dashboard` — that's
the WAR's context path (from the file name), unlike the earlier Spring
Boot version which used just `/api`.

## 5. Update the frontend's API base URL

Since the context path changed, open
`frontend/src/api/client.js` and change:

```js
baseURL: 'http://localhost:8080/api',
```

to:

```js
baseURL: 'http://localhost:8080/progress-dashboard/api',
```

## 6. Run the frontend

```
cd frontend
npm install
npm run dev
```

Open http://localhost:5173.

## Demo flow

1. **Assignments** tab → add 2-3 assignments
2. **Interns** tab → add 2-3 interns
3. Click an intern's name → their progress page
4. Record a score → watch completion % and average score update

## If you're stuck close to deadline

- **`data-source add` fails / driver not found**: double-check the jar
  actually deployed (check WildFly console log on startup) and that
  `--driver-name` matches the exact jar filename.
- **WAR fails to deploy**: check WildFly's `standalone/log/server.log` for
  the actual error — paste it to me and I'll fix the code.
- **Out of time**: you can demo directly against the API with `curl` or
  Postman even if the frontend has issues — a working backend is a
  legitimate demo on its own.
