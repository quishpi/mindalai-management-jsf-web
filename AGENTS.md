# AGENTS — mindalai-management-jsf-web

Platform-management backoffice JSF frontend. Pure consumer of `mindalai-management-api`; no domain logic, no DB.

## Stack — do not guess versions
- Java 25, Spring Boot 4.1.1, JoinFaces 6.1.0, Jakarta Faces, PrimeFaces 15+, Harmony 5.1.0 — `docs/03-technology-stack.md:18`, `ADR-002`, `ADR-007`
- Theme zip: `shared-libraries/theme/harmony/harmony-layout-5.1.0.zip`
- `application.properties` only (never YAML) — `docs/03-technology-stack.md:39`
- Build: `./mvnw clean verify` ; run: `./mvnw spring-boot:run -pl management-jsf-app -am -Dspring-boot.run.arguments="--server.port=8083 --app.api.base-url=http://localhost:8082"` — `README.md:12-13`
- Port `8083` (mgmt-api `8082`, business API `8080`, business JSF `8081`) for simultaneous local run; no DB of its own.

## Hard boundaries — will break review if violated
- No `EntityManager`, `Repository` JPA, `DataSource`/`spring.datasource`, direct PostgreSQL — `docs/04-project-structure.md:82`, Regla 2/3
- No REST endpoints exposed; client only — `docs/12-api-boundaries.md:33`
- No POS/SRI logic; never contact `mindalai-api` — backends are fully separate (`docs/02-target-architecture.md:112`), no direct communication between the two JSF apps.
- Never import `mindalai-management-api` source; share only versioned DTO contract + HTTP/JSON — `docs/04-project-structure.md:52`; React migration must not touch backend — `docs/02-target-architecture.md:139`

## Project structure (when scaffolded)
```
management-jsf-app/
  presentation/jsf/ → @Named/@ViewScoped backing beans, navigation
  rest-client/      → RestClient → mindalai-management-api (DTOs, mappers, error/correlation)
  ui/               → XHTML Facelets, PrimeFaces/Harmony templates
```

## REST integration — consumes `/api/v1.0/platform/*` (`docs/12-api-boundaries.md:18`)
- `GET /api/v1.0/platform/tenants`, `POST /api/v1.0/platform/tenants`, `POST /api/v1.0/platform/licenses`, `GET /api/v1.0/platform/installations`, `GET /api/v1.0/platform/usage`, etc. — names draft, OpenAPI before Fase 1 (`docs/12-api-boundaries.md:30`)
- Uses DTOs/Responses from API — Regla 7
- `RestClient` must handle timeouts, delegate retry/idempotency to backend, map Bean Validation errors, propagate `correlationId` — `docs/03-technology-stack.md:47`, Regla 15
- Auth: login via `POST /api/v1.0/platform/auth/login` (or equivalent), store JWT in HTTP session, forward on each call — `docs/11-security.md:3`

## Navigation & domain view
- Sections aligned to `docs/10-platform-management.md:55`: `Dashboard, Tenants, Plans, Subscriptions, Licenses, Installations, Devices, Usage, Support, Configuration, Audit`
- `@ViewScoped` for lists with filters/pagination and tenant/plan/license forms
- Hierarchy `Tenant → Installation → Devices (Server + cajas)` as tree/master-detail — `docs/10-platform-management.md:22`
- Entities consumed as DTOs (no JPA): `tenant, plan, subscription, license, installation, device, usage, feature, platform_audit` — `docs/10-platform-management.md:7`; catalogs in tables, not Java enums — Regla 13
- Flow: create tenant → assign plan → create subscription → issue license → register installation → add devices → view usage.

## Security
- Session: `HttpOnly`+`Secure`+`SameSite`, CSRF, timeout `30m`, platform-role gate — `docs/11-security.md:15,26`; platform admin is separate context from tenant cashier — `docs/10-platform-management.md:73`
- Chain: `Browser → mindalai-management-jsf-web → REST credentials` — `docs/11-security.md:26`; show audit trail when API exposes it; never leak credentials in Git/logs/responses.

## Config — `application.properties`
```properties
server.port=8083
app.api.base-url=http://localhost:8082
app.api.auth.token-url=http://localhost:8082/api/v1.0/platform/auth/login
app.api.timeout=5000
server.servlet.session.timeout=30m
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.secure=true
```
No `spring.datasource` here. Full reference: `README.md:12`.

## Verification
- Start `mindalai-management-api:8082` first; then `mindalai-management-jsf-web:8083`; open `http://localhost:8083/` → login → browse Tenants → create license
- Must be stoppable without stopping `mindalai-management-api` — `docs/17-acceptance.md:5`; can run alongside `mindalai-jsf-web:8081` without conflict — `docs/02-target-architecture.md:112`
- Tests: backing-bean unit with mocked `RestClient`; integration with WireMock for `mindalai-management-api`.
