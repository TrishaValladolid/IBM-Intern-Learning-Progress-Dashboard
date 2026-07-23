# Learning Progress Dashboard 

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

Start WildFly (`standalone.bat` in `wildfly\bin`) and check the console 
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
> `backend/src/main/resources/META-INF/persistence.xml`  it already does,
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

Watch the WildFly console , it should log that the WAR deployed
successfully. Test it:

```
curl http://localhost:8080/progress-dashboard/api/interns
```

Should return `[]`.

## 5. Run the frontend

```
cd frontend
npm install
npm run dev
```

Open http://localhost:5173.

