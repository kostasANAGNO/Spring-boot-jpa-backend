# Hospital Information System (HIS) – backend

REST API for a Hospital Information System. This first stage contains the project skeleton, JWT security,
Flyway-managed schema and one complete reference module (**Patient**) that future modules (Doctor,
Appointment, Hospitalization, ...) should copy.

## Stack

Java 21 · Spring Boot 3.5 · Maven (wrapper included) · Spring Web / Data JPA (Hibernate) / Security / Validation ·
PostgreSQL 16 · Flyway · JWT (`jjwt`) · Lombok · MapStruct · springdoc-openapi (Swagger UI) ·
JUnit 5 · Mockito · Spring Security Test · Testcontainers

## Run locally

Prerequisites: JDK 21, Docker.

```bash
docker compose up -d --wait                                  # PostgreSQL on localhost:5432
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run          # API on http://localhost:8080
```

Swagger UI (local profile only): <http://localhost:8080/swagger-ui.html> – use **Authorize** with the token from login.

### Configuration (environment variables)

| Variable | Purpose | Local default |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | datasource | `jdbc:postgresql://localhost:5432/his`, `his`, `his` |
| `JWT_SECRET` | HS256 signing key, **≥ 32 bytes** | dev-only value in `application-local.yml` |
| `JWT_EXPIRATION` | token lifetime (e.g. `1h`, `30m`) | `1h` |
| `JWT_ISSUER` | token issuer | `his-backend` |
| `CORS_ALLOWED_ORIGINS` | comma separated origins | `http://localhost:3000,http://localhost:4200` |

Without the `local` profile `DB_*` and `JWT_SECRET` have **no defaults** and the app refuses to start – secrets never live in git.

## Default dev credentials (DEV ONLY)

Seeded by `V2__seed_admin_user.sql`:

| Username | Password | Role |
|---|---|---|
| `admin` | `Admin123!` | ADMIN |

> ⚠️ Local development only. Because the seed is an ordinary migration it runs on every database; change the
> password or drop the seed before the first real deployment.

## Using the API

```bash
# 1. log in
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin123!"}' | jq -r .accessToken)

# 2. create a patient
curl -s -X POST localhost:8080/api/patients -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"firstName":"Maria","lastName":"Papadopoulou","dateOfBirth":"1990-05-17","gender":"FEMALE","nationalId":"17059012345"}'

# 3. search (filters: name, nationalId, phone, email, gender; paging: page, size; sorting: sort=lastName,desc)
curl -s "localhost:8080/api/patients?name=maria&sort=lastName,asc&page=0&size=20" -H "Authorization: Bearer $TOKEN"

# 4. register another user (ADMIN only)
curl -s -X POST localhost:8080/api/auth/register -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"username":"dr.house","email":"house@his.local","password":"Sup3rSecret!","fullName":"Gregory House","roles":["DOCTOR"]}'
```

| Endpoint | Roles |
|---|---|
| `POST /api/auth/login` | public |
| `POST /api/auth/register` | ADMIN |
| `POST/GET/PUT /api/patients[/{id}]` | ADMIN, DOCTOR, NURSE, RECEPTIONIST |
| `DELETE /api/patients/{id}` (soft delete) | ADMIN |

Errors always use one JSON shape: `timestamp`, `status`, `error`, `message`, `path` and (for 400s) `validationErrors[{field, message}]`.

Patient deletion is **soft** (`active=false`, `deleted_at` set); such patients disappear from every read/search.
Their national ID stays reserved (it is unique across all rows).

## Database & Flyway policy

* Flyway is the **only** owner of the schema; Hibernate runs with `ddl-auto=validate`.
* **Pre-deployment (now):** nothing is deployed yet, so `V1__init_schema.sql` is **edited in place** to match the entities –
  no new migration per small change. Apply changes by resetting the local DB:
  ```bash
  docker compose down -v && docker compose up -d --wait
  ```
* **After the first real deployment** migrations become strictly incremental and immutable: never edit `V1`/`V2` again,
  add `V3__...sql`, `V4__...sql`, ...
* The migration must always match the entities, otherwise startup fails validation.

## Build & test

```bash
./mvnw clean verify
```

Includes unit tests (Patient service), a security matrix test (401/403/2xx per role) and a Testcontainers integration
test that boots the app on PostgreSQL – **Docker must be running**.

## Project layout

See [CLAUDE.md](CLAUDE.md) for the package structure and conventions to follow when adding modules.
