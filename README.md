# mindalai-management-jsf-web — Platform Management JSF Web

> **Bilingüe / Bilingual:** Este documento está escrito en español e inglés. Cada sección contiene ambos idiomas. / This document is written in Spanish and English. Each section contains both languages.

![Java 25](https://img.shields.io/badge/Java-25-orange) ![Spring Boot 4.1.1](https://img.shields.io/badge/Spring_Boot-4.1.1-brightgreen) ![JoinFaces 6.1.0](https://img.shields.io/badge/JoinFaces-6.1.0-blue) ![PrimeFaces](https://img.shields.io/badge/PrimeFaces-15%2B-9cf) ![Harmony](https://img.shields.io/badge/Theme-Harmony-purple) ![Status](https://img.shields.io/badge/Status-Fase_0_Baseline-lightgrey)

---

## 1. Propósito / Purpose

**ES:** `mindalai-management-jsf-web` es la **aplicación web de backoffice administrativo** de la plataforma. Corresponde a `platform-management-web` definido en `shared-libraries/mindalai-platform-phase0/docs/02-target-architecture.md:99` y `shared-libraries/mindalai-platform-phase0/docs/04-project-structure.md:26`. Es un frontend JSF independiente que consume exclusivamente `mindalai-management-api` vía HTTP/JSON.

**EN:** `mindalai-management-jsf-web` is the **administrative backoffice web application** of the platform. It maps to `platform-management-web` defined in `shared-libraries/mindalai-platform-phase0/docs/02-target-architecture.md:99` and `shared-libraries/mindalai-platform-phase0/docs/04-project-structure.md:26`. It is an independent JSF frontend that consumes `mindalai-management-api` exclusively via HTTP/JSON.

> **Regla arquitectónica:** REST y JSF son deployables separados (`shared-libraries/mindalai-platform-phase0/docs/02-target-architecture.md:4`, `ADR-001`, `ADR-002`). Este proyecto no contiene lógica de dominio ni acceso a PostgreSQL.

## 2. Responsabilidades / Responsibilities

**ES:** Según `docs/02-target-architecture.md:99` y `docs/10-platform-management.md:55`:

| Sección / Area | Capacidades |
|---|---|
| Dashboard | métricas de plataforma, tenants activos, consumo, salud |
| Tenants | listado, detalle, creación, edición, estado |
| Plans | gestión de planes y características |
| Subscriptions | relación tenant-plan, vigencia, estado |
| Licenses | emisión, renovación, revocación |
| Installations | registro de instalaciones (ej. Minimarket XYZ → Installation #01 → Server + Caja 1/2/3, `docs/10-platform-management.md:22`) |
| Devices | equipos autorizados por instalación |
| Usage | visualización de consumo (comprobantes, usuarios, cajas) |
| Support | tickets/incidentes |
| Configuration | parámetros globales de plataforma |
| Audit | trazabilidad administrativa |

**EN:** Per `docs/02-target-architecture.md:99` and `docs/10-platform-management.md:55`:

| Section / Area | Capabilities |
|---|---|
| Dashboard | platform metrics, active tenants, usage, health |
| Tenants | list, detail, creation, edition, status |
| Plans | plan and feature management |
| Subscriptions | tenant-plan relationship, validity, status |
| Licenses | issuance, renewal, revocation |
| Installations | installation registry (e.g. Minimarket XYZ → Installation #01 → Server + Register 1/2/3, `docs/10-platform-management.md:22`) |
| Devices | authorized devices per installation |
| Usage | consumption view (documents, users, registers) |
| Support | tickets/incidents |
| Configuration | global platform parameters |
| Audit | administrative traceability |

**ES/EN — No responsabilidades / Non-responsibilities:** No gestión POS, no inventario, no facturación, no Authorization Service. No JPA/EntityManager/Repository, no conexión PostgreSQL, no reglas de dominio. Ver `docs/04-project-structure.md:82` y `docs/18-implementation-rules.md:1` (Reglas 2, 3, 7, 8).

## 3. Stack Tecnológico / Technology Stack

**ES/EN:** Definido en `docs/03-technology-stack.md:18`:

| Capa / Layer | Tecnología / Technology |
|---|---|
| Lenguaje / Language | Java 25 |
| Framework web / Web framework | Spring Boot 4.1.1 + JoinFaces 6.1.0 + Jakarta Faces (JSF) |
| UI | PrimeFaces, Harmony Theme (`ADR-007`) |
| Comunicación / Communication | HTTP/JSON REST client hacia `mindalai-management-api` (no JPA) |
| Infra / Infra | Linux Ubuntu Server, Docker, Docker Compose |
| Principios / Principles | `application.properties` (no YAML), UTC, logs estructurados, correlation ID |

El frontend JSF consume REST igual que lo haría un futuro frontend React (`shared-libraries/mindalai-platform-phase0/README.md:32`). La misma separación permite coexistir `mindalai-jsf-web` y `mindalai-management-jsf-web` como dos UIs independientes.

## 4. Arquitectura / Architecture

**ES:** Relación con el backend (`docs/02-target-architecture.md:40` y `docs/04-project-structure.md:89`):

```
mindalai-management-jsf-web (JoinFaces 6) ──HTTP/JSON──► mindalai-management-api (Spring Boot REST) ──► PostgreSQL Cloud
     │                                                       ▲
     │ Browser Session (HttpOnly, Secure,                    │ API credentials (JWT/OAuth2)
     │  SameSite, CSRF, timeout)                            │ OpenAPI /api/v1.0/platform
     ▼                                                       │
   Administrador de plataforma                           Audit, Tenant, License

mindalai-jsf-web (JoinFaces 6) ──HTTP/JSON──► mindalai-api ──► PostgreSQL Local + Authorization Service
```

Cada JSF Web tiene su propio REST API propietario; no hay comunicación directa entre `mindalai-management-jsf-web` y `mindalai-api` (`docs/02-target-architecture.md:112`). La separación de seguridad exige contextos distintos: un usuario de Platform Management no es automáticamente usuario del tenant (`docs/10-platform-management.md:73`).

**EN:** Relationship to backend (`docs/02-target-architecture.md:40` and `docs/04-project-structure.md:89`):

Same diagram as above.

Each JSF Web has its own proprietary REST API; there is no direct communication between `mindalai-management-jsf-web` and `mindalai-api` (`docs/02-target-architecture.md:112`). Security separation requires distinct contexts: a Platform Management user is not automatically a tenant user (`docs/10-platform-management.md:73`).

Migración JSF → React (`docs/02-target-architecture.md:139`): el backend `mindalai-management-api` permanece estable, solo se reemplaza el cliente web.

## 5. Estructura del Proyecto / Project Structure

**ES:** Estructura recomendada (`docs/04-project-structure.md:26`):

```
mindalai-management-jsf-web/
├── management-jsf-app/         # Spring Boot + JoinFaces app
│   ├── presentation/jsf/       # @Named / @ViewScoped backing beans, navigation
│   ├── rest-client/            # HTTP clients hacia mindalai-management-api
│   └── ui/                     # XHTML Facelets, templates, PrimeFaces/Harmony
├── pom.xml                     # parent POM
└── README.md
```

Regla de dependencia (`docs/04-project-structure.md:39`):

```
management-jsf-app → HTTP/JSON → management-api-app → management-application → management-domain
```

**EN:** Recommended structure (`docs/04-project-structure.md:26`):

Same tree as above.

Dependency rule (`docs/04-project-structure.md:39`):

```
management-jsf-app → HTTP/JSON → management-api-app → management-application → management-domain
```

**ES/EN — Prohibiciones verificables:** No `EntityManager`, no `Repository JPA`, no `DataSource`/`spring.datasource` hacia PostgreSQL en este proyecto (`docs/04-project-structure.md:82`). Toda operación empresarial debe pasar por un caso de uso del backend (`docs/18-implementation-rules.md:13` Regla 4).

## 6. Integración REST / REST Integration

**ES:** Este proyecto es **consumidor** puro (`docs/12-api-boundaries.md:33`):

```
JSF Backing Bean → RestClient → HTTP → mindalai-management-api
                                      → GET /api/v1.0/platform/tenants
                                      → POST /api/v1.0/platform/tenants
                                      → GET /api/v1.0/platform/licenses
                                      → GET /api/v1.0/platform/installations
                                      → GET /api/v1.0/platform/usage
```

Usa DTOs/Responses del API (`docs/18-implementation-rules.md:21` Regla 7). Los nombres son iniciales de diseño y se formalizarán como OpenAPI antes de Fase 1 (`docs/12-api-boundaries.md:30`).

**EN:** This project is a pure **consumer** (`docs/12-api-boundaries.md:33`):

```
JSF Backing Bean → RestClient → HTTP → mindalai-management-api
                                      → GET /api/v1.0/platform/tenants
                                      → POST /api/v1.0/platform/tenants
                                      → GET /api/v1.0/platform/licenses
                                      → GET /api/v1.0/platform/installations
                                      → GET /api/v1.0/platform/usage
```

It uses API DTOs/Responses (`docs/18-implementation-rules.md:21` Rule 7). Names are initial design and will be formalized as OpenAPI before Phase 1 (`docs/12-api-boundaries.md:30`).

## 7. Modelo de Datos (Visión UI) / Data Model (UI View)

**ES:** Este proyecto no posee entidades JPA. Consume vía DTOs las entidades gestionadas por `mindalai-management-api`: `tenant`, `plan`, `subscription`, `license`, `installation`, `device`, `usage`, `feature`, `support`, `platform_audit` descritas en `docs/10-platform-management.md:7` y `docs/08-erd.md:3`. La instalación se visualiza como jerarquía `Tenant → Installation → Devices (Server + cajas)` (`docs/10-platform-management.md:22`).

**EN:** This project has no JPA entities. It consumes via DTOs the entities managed by `mindalai-management-api`: `tenant`, `plan`, `subscription`, `license`, `installation`, `device`, `usage`, `feature`, `support`, `platform_audit` described in `docs/10-platform-management.md:7` and `docs/08-erd.md:3`. Installation is visualized as hierarchy `Tenant → Installation → Devices (Server + registers)` (`docs/10-platform-management.md:22`).

## 8. Seguridad / Security

**ES:** Según `docs/11-security.md:15` y `docs/10-platform-management.md:73`:
- Sesión web segura: `HttpOnly`, `Secure` bajo HTTPS, `SameSite`, CSRF, timeout, logout, control por permisos.
- Separación: `JSF Browser Session → mindalai-management-jsf-web → REST API credentials` (`docs/11-security.md:26`). Sin acceso directo a DB.
- Auditoría: usuario, tenant, instalación, acción, recurso, IP, metadata.
- Roles diferenciados: administrador de plataforma vs. usuario de tenant (contextos de seguridad distintos).

Un cajero de `mindalai-jsf-web` no accede a este backoffice y viceversa.

**EN:** Per `docs/11-security.md:15` and `docs/10-platform-management.md:73`:
- Secure web session: `HttpOnly`, `Secure` over HTTPS, `SameSite`, CSRF, timeout, logout, permission control.
- Separation: `JSF Browser Session → mindalai-management-jsf-web → REST API credentials` (`docs/11-security.md:26`). No direct DB access.
- Audit: user, tenant, installation, action, resource, IP, metadata.
- Differentiated roles: platform admin vs. tenant user (distinct security contexts).

A cashier of `mindalai-jsf-web` does not access this backoffice and vice versa.

## 9. Configuración / Configuration

**ES:** `application.properties` (no YAML) (`docs/03-technology-stack.md:39`). Variables clave:
- `server.port` (distinto de `mindalai-management-api` y de `mindalai-jsf-web`/`mindalai-api`)
- `app.api.base-url` (URL de `mindalai-management-api`, ej. `http://localhost:8082`)
- `app.api.auth.*` (credenciales/token para invocar la API)
- `spring.security.*`, `server.servlet.session.*` (timeout, cookies)
- `logging.level.*` (logs estructurados + correlation ID)

No se configura `spring.datasource` en este proyecto (Reglas 2-3).

**EN:** `application.properties` (not YAML) (`docs/03-technology-stack.md:39`). Key vars:
- `server.port` (different from `mindalai-management-api` and from `mindalai-jsf-web`/`mindalai-api`)
- `app.api.base-url` (URL of `mindalai-management-api`, e.g. `http://localhost:8082`)
- `app.api.auth.*` (credentials/token to call the API)
- `spring.security.*`, `server.servlet.session.*` (timeout, cookies)
- `logging.level.*` (structured logs + correlation ID)

No `spring.datasource` is configured in this project (Rules 2-3).

## 10. Desarrollo Local / Local Development

**ES:**

```
Requisitos: Java 25, Maven Wrapper, mindalai-management-api en ejecución
Pasos:
  ./mvnw clean verify
  ./mvnw spring-boot:run -pl management-jsf-app -am -Dspring-boot.run.arguments="--server.port=8083 --app.api.base-url=http://localhost:8082"
Verificar: http://localhost:8083/  (login), navegar Tenants, crear licencia de prueba
```

Debe ser posible detener este proyecto sin detener `mindalai-management-api` (`docs/17-acceptance.md:5`). En el futuro, reemplazarlo por React sin modificar el backend (`docs/02-target-architecture.md:139`).

**EN:**

```
Prerequisites: Java 25, Maven Wrapper, running mindalai-management-api
Steps:
  ./mvnw clean verify
  ./mvnw spring-boot:run -pl management-jsf-app -am -Dspring-boot.run.arguments="--server.port=8083 --app.api.base-url=http://localhost:8082"
Verify: http://localhost:8083/  (login), browse Tenants, create test license
```

It must be possible to stop this project without stopping `mindalai-management-api` (`docs/17-acceptance.md:5`). In the future, replace it with React without modifying backend (`docs/02-target-architecture.md:139`).

## 11. Roadmap / Roadmap

**ES:** Este proyecto inicia en **Fase 1 — Foundation** (`docs/15-roadmap.md:7`): junto a `mindalai-management-api` implementa `Platform Management Web` — login, dashboard, tenant list/detail, license/installation management (`docs/16-phase1-backlog.md:40` EPIC 5). Luego **Fase 8 — Commercial** (`docs/15-roadmap.md:81`): Installer, Licensing, Updates, Backup, Support. Finalmente **Fase 9 — SaaS** (`docs/15-roadmap.md:94`): provisioning, subscriptions, billing, usage, feature flags, `Local ↔ Cloud`.

**EN:** This project starts in **Phase 1 — Foundation** (`docs/15-roadmap.md:7`): alongside `mindalai-management-api` it implements `Platform Management Web` — login, dashboard, tenant list/detail, license/installation management (`docs/16-phase1-backlog.md:40` EPIC 5). Then **Phase 8 — Commercial** (`docs/15-roadmap.md:81`): Installer, Licensing, Updates, Backup, Support. Finally **Phase 9 — SaaS** (`docs/15-roadmap.md:94`): provisioning, subscriptions, billing, usage, feature flags, `Local ↔ Cloud`.

## 12. Reglas de Implementación Aplicables / Applicable Implementation Rules

**ES/EN:** De `docs/18-implementation-rules.md:1` aplican: Reglas 1, 2, 3, 4, 7, 8, 14. Énfasis: no JSF en REST (Regla 1), no JPA/PostgreSQL en JSF (Reglas 2-3), frontend usa DTOs del API (Regla 7), dominio sin conocimiento de JSF (Regla 8), no microservicios prematuros (Regla 14).

## 13. Referencias / References

- `shared-libraries/mindalai-platform-phase0/README.md:1` — Decisiones y separación REST/JSF
- `shared-libraries/mindalai-platform-phase0/docs/02-target-architecture.md:99` — Rol platform-management-web
- `shared-libraries/mindalai-platform-phase0/docs/03-technology-stack.md:18` — Stack JSF (JoinFaces 6.1.0, PrimeFaces, Harmony)
- `shared-libraries/mindalai-platform-phase0/docs/04-project-structure.md:26` y `89` — Estructura `management-jsf-app → management-api-app`
- `shared-libraries/mindalai-platform-phase0/docs/10-platform-management.md:1` — Entidades y dashboard administrativo
- `shared-libraries/mindalai-platform-phase0/docs/12-api-boundaries.md:33` — JSF como consumidor REST
- `shared-libraries/mindalai-platform-phase0/docs/11-security.md:15` — Sesión JSF y separación de contextos
- `shared-libraries/mindalai-platform-phase0/docs/14-adr/ADR-001-separation-rest-jsf.md:1`, `ADR-002-platform-management-web.md`, `ADR-007-harmony.md` — ADRs
- `shared-libraries/mindalai-platform-phase0/docs/16-phase1-backlog.md:40` — EPIC 5 (Platform Management Web)
- `shared-libraries/mindalai-platform-phase0/docs/17-acceptance.md:1` — Criterios de aceptación
- `shared-libraries/mindalai-platform-phase0/diagrams/architecture.mmd` — Diagrama

## 14. Criterios de Aceptación / Acceptance Criteria

**ES:** De `docs/17-acceptance.md:1` y `docs/16-phase1-backlog.md:40`: login administrativo, dashboard operativo, gestión completa de tenants/licencias/instalaciones con auditoría, instalacion modelada como `Tenant → Installation → Devices`.

**EN:** From `docs/17-acceptance.md:1` and `docs/16-phase1-backlog.md:40`: admin login, operational dashboard, full tenant/license/installation management with audit, installation modeled as `Tenant → Installation → Devices`.

---

*Este proyecto es el backoffice de `mindalai-management-api`. Su evolución a React no afecta al backend. Ver `shared-libraries/mindalai-platform-phase0/docs/02-target-architecture.md:139` y `shared-libraries/mindalai-platform-phase0/README.md:34`.*
