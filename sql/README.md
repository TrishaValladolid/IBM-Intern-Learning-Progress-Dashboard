# SQL Files

This folder contains the database schema for the **Learning Progress Dashboard**.

## Files

| File | Purpose |
|---|---|
| `schema.sql` | Creates all 7 tables from scratch. Safe to re-run — drops existing tables first. |
| `seed.sql` | Documents the default accounts. Read the notes inside before running. |

---

## Do You Need to Run These?

**Probably not.** The application creates all tables automatically via Hibernate
(`hbm2ddl.auto=update`) on the first deployment, and default accounts are seeded
automatically by `StartupDataSeeder.java` when the `app_user` table is empty.

Use these files if you:
- Want to inspect the schema before running the app
- Need to reset the database to a clean state during development
- Are setting up on a server where you prefer to create tables manually

---

## How to Run

### Step 1 — Create the database (if you haven't already)

```sql
CREATE DATABASE progress_dashboard;
```

### Step 2 — Run the schema

```bash
psql -U postgres -d progress_dashboard -f schema.sql
```

This drops and recreates all 7 tables:

| Table | Description |
|---|---|
| `app_user` | Admin and trainer login accounts |
| `user_training_assignment` | Which training areas each trainer is restricted to |
| `intern` | Intern records (name, batch, track, status, profile) |
| `assignment` | Assignments (title, max score, batch, training area) |
| `training` | Trainings assigned to each intern (e.g. Java, Ionic) |
| `submission` | Grades — one row per intern per assignment |
| `attendance` | Daily attendance records per intern |
| `attendance_session` | Groups attendance records by training + date |

### Step 3 — Deploy the application

After running `schema.sql`, deploy the WAR to WildFly. On first startup,
`StartupDataSeeder.java` will insert the two default accounts automatically:

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | ADMIN |
| `trainer` | `trainer123` | TRAINER |

> **Change these passwords immediately** after first login via the Trainers page.

---

## Table Relationships

```
app_user
    └── user_training_assignment  (trainer -> training area names)

intern
    ├── training        (intern -> trainings completed)
    ├── submission      (intern -> assignment grades)
    └── attendance      (intern -> daily attendance records)
            └── attendance_session  (groups records by training + date)

assignment
    └── submission      (assignment -> intern grades)
```
