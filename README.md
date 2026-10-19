# SIGAC Backend

Information System for Minutes and Council Management (SIGAC) — REST API built with Spring Boot 3.3 and Java 21.

The naming contract shared with the frontend (entities, tables, REST paths, JSON fields, enum values)
is documented in [`docs/GLOSSARY.md`](docs/GLOSSARY.md).

## Architecture: modular monolith

```
com.university.sigac
├── core/              Shared kernel: security (JWT), exceptions, crypto/date/HTML utilities,
│                      append-only audit writer, PDF/A engine, storage, sequences, global parameters
├── config/            Spring configuration: Security, async mail, OpenAPI, typed properties, initial data
├── modules/
│   ├── users/         Users, authentication, RBAC, institutional hierarchy and council types
│   ├── templates/     Thymeleaf minutes templates (ROLE_ADMIN)
│   ├── requests/      Filing of requests with PDF attachments, and agendas (sessions + agenda items)
│   ├── session/       Live session: attendance, quorum, decisions and debate notes
│   ├── minutes/       Minutes generation, PDF/A compilation and SHA-256 signature
│   ├── repository/    Digital repository, append-only audit queries and dashboard
│   └── notifications/ Asynchronous SMTP emails (@Async)
└── SigacApplication   Main class (sigac-backend.jar)
```

Each module is split internally into `controller`, `dto`, `entity`, `enums`, `repository`, `service` (and `util`
when needed). Rules, enforced on every build by `ModularArchitectureTest` (ArchUnit):

- `core` never depends on a module; `core` and the modules only read `config.AppProperties` from `config`.
- Allowed module dependencies (no cycles): `users → notifications`, `templates → users`,
  `requests → users, notifications`, `session → requests, users`,
  `minutes → session, requests, templates, users, notifications`,
  `repository → minutes, session, requests, users, notifications`.
- A module's repositories are private: other modules use its public services
  (e.g. `UserLookupService`, `HierarchyService`, `MinutesQueryService`, `AgendaService`).
- Controllers are entry points only; no class depends on them.

## Requirements

- JDK 21 (e.g. `C:\Program Files\Java\jdk-21`)
- Maven 3.9+ (the Maven bundled with IntelliJ IDEA also works)
- MySQL 8.0 (Windows service `MySQL80`), user `root` with no password by default.
  The `sigac` database is created automatically on first start (`createDatabaseIfNotExist=true`)
  and Hibernate creates the tables (`ddl-auto: update`).

## Run locally (Windows / PowerShell)

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$mvn = "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.1.1.1\plugins\maven\lib\maven3\bin\mvn.cmd"

# Run the tests
& $mvn -q test

# Start the API on http://localhost:8080 with demo data
$env:SIGAC_SEED_DEMO = 'true'
& $mvn spring-boot:run
```

Swagger UI: `http://localhost:8080/swagger-ui.html` · Health: `http://localhost:8080/actuator/health`.

To start from an empty database:

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -e "DROP DATABASE IF EXISTS sigac;"
```

## Configuration

All settings live in `src/main/resources/application.yml` and can be overridden with environment variables
(see `.env.example`): `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (at least 32 characters),
`FRONTEND_URL`, `CORS_ORIGINS`, `MAIL_ENABLED`, `SMTP_*`, `SIGAC_STORAGE_PATH`, `SIGAC_SEED_DEMO`, etc.

## Demo users

Always created:

| Email               | Password      | Role       |
|---------------------|---------------|------------|
| `admin@sigac.local` | `Admin12345*` | ROLE_ADMIN |

Created only when `SIGAC_SEED_DEMO=true` and the database has no universities (password `Demo12345*`):

| Email                 | Role                   |
|-----------------------|------------------------|
| `sec1@demo.edu`       | ROLE_COUNCIL_SECRETARY |
| `sec2@demo.edu`       | ROLE_FACULTY_SECRETARY |
| `presidente@demo.edu` | ROLE_PRESIDENT         |
| `consejero1@demo.edu` | ROLE_COUNCIL_MEMBER    |
| `consejero2@demo.edu` | ROLE_COUNCIL_MEMBER    |
| `consejero3@demo.edu` | ROLE_COUNCIL_MEMBER    |

Change the initial passwords before using the system outside a development environment.

Existing databases are upgraded at startup by `src/main/resources/db/migrations.sql` (idempotent), because
`ddl-auto=update` never alters existing columns.

## Quick check

```powershell
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/login `
  -ContentType 'application/json' -Body '{"email":"admin@sigac.local","password":"Admin12345*"}'
Invoke-RestMethod -Uri http://localhost:8080/api/v1/hierarchy/universities `
  -Headers @{ Authorization = "Bearer $($login.token)" }
```
