# CLAUDE.md – conventions for the HIS backend

Spring Boot 3.5 / Java 21 / Maven / PostgreSQL. Base package `com.kostasanagno.his`. The **Patient** module is the
reference implementation: when adding Doctor, Appointment, ... copy its structure file by file.

## Layers (package `com.kostasanagno.his.`)

| Package | Content |
|---|---|
| `config` | Security, CORS, OpenAPI, JPA auditing, properties config |
| `controller` | Thin REST controllers; depend on service **interfaces** only; `@PreAuthorize` on every endpoint |
| `service` / `service.impl` | Interfaces / `@Service` impls with class-level `@Transactional`, `@Transactional(readOnly = true)` on reads |
| `repository` | Spring Data repos (+ `*Specifications` for search) |
| `entity` | JPA entities extending `BaseEntity` |
| `dto` | Request/response DTOs – never expose entities from controllers |
| `mapper` | MapStruct mappers |
| `exception` | Custom exceptions, `ApiError`, `GlobalExceptionHandler` |
| `security` | JWT service/filter, `UserDetailsService`, 401/403 handlers, `AccessRules` |

Rules: constructor injection only (`@RequiredArgsConstructor`, no field injection); controllers never touch repositories
or `*Impl` classes; use `@Slf4j` for logging.

## Lombok

* Entities: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder` – **never `@Data`**. Extend `BaseEntity`
  (id, `createdAt`, `updatedAt`, proxy-safe id-based `equals`/`hashCode`). `toString` via
  `@ToString(onlyExplicitlyIncluded = true)` – never include lazy associations or passwords.
* DTOs: Java `record`s with Bean Validation annotations.

## DTOs & mappers

* One request record per write operation (create/update may share), one response record. List endpoints return
  `PageResponse<T>`; search filters go in a `*SearchCriteria` record and are applied through a `JpaSpecification`.
* Mappers: `@Mapper(config = CentralMapperConfig.class)` (Spring component model, **unmapped target = compile error**,
  builders disabled). Explicitly `@Mapping(ignore = true)` id/audit/server-managed fields.

## Errors

Throw `ResourceNotFoundException` (404) / `DuplicateResourceException` (409) from services. `GlobalExceptionHandler`
turns everything into `ApiError`; security 401/403 use the same shape. A new exception type = a new handler method there.

## Security

* Stateless JWT (`jjwt`), BCrypt, CSRF off, roles `ADMIN, DOCTOR, NURSE, RECEPTIONIST` (`entity.Role`).
* Everything under `/api/**` needs authentication except `POST /api/auth/login`. Use `@PreAuthorize` with the constants in
  `security/AccessRules` (add new ones there). Note: argument validation runs *before* `@PreAuthorize`; if a 403 must win
  over a 400, add a URL rule in `SecurityConfig` (done for `/api/auth/register`).
* JWT secret/expiry/issuer come from env/config (`app.jwt.*`). Never commit real secrets; the only default secrets
  live in `application-local.yml` and are marked dev-only. No `.env` files in git.
* Every new endpoint needs a case in the security matrix test (401/403/2xx).

## Database & Flyway (PRE-DEPLOYMENT POLICY)

* Flyway owns the schema; `spring.jpa.hibernate.ddl-auto=validate`. Migrations in `src/main/resources/db/migration`.
* **Not deployed yet:** keep a single baseline `V1__init_schema.sql` and **edit it in place** (add the new module's tables
  there). The local DB is reset with `docker compose down -v`. Do NOT add a migration per change.
  `V2__seed_admin_user.sql` is the dev-only admin seed.
* **After the first real deployment** (the user will say so): migrations become immutable and strictly incremental
  (`V3`, `V4`, ...). Never edit an applied migration then.
* The SQL must always match the entities exactly (types, nullability, enum columns as `VARCHAR` + CHECK).
* Soft-deleted rows: filter `active = true` in repository methods/specifications.

## Testing

`./mvnw clean verify` must pass (Docker needed for Testcontainers). Per module: service unit tests (Mockito), cases in
`ControllerSecurityTest` style (`@WebMvcTest` + imported `SecurityConfig`), and extend the Testcontainers integration test.

## Git

Small conventional commits (`feat:`, `fix:`, `chore:`, `test:`, `docs:`). Do not add `Co-Authored-By` trailers or any
mention of Claude as author. Never commit secrets, `.env` files or `target/`.
