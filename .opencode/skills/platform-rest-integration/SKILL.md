---
name: platform-rest-integration
description: Integración JSF backoffice como cliente de mindalai-management-api — RestClient y DTOs platform, sin DB
license: MIT
compatibility: opencode
---

## Qué hace
Define cómo `mindalai-management-jsf-web` consume `mindalai-management-api` — `AGENTS.md:REST integration`, `docs/04-project-structure.md:52`, `docs/12-api-boundaries.md:33`.

## Cuándo usarme
Al tocar `rest-client/`, backing beans platform o `app.api.*` en este repo.

## Reglas
- Solo `mindalai-management-api`; nunca `mindalai-api`/`mindalai-jsf-web` — backends separados — `docs/02-target-architecture.md:112`.
- HTTP/JSON `/api/v1.0/platform/*` versionado — `docs/12-api-boundaries.md:18`; nombres draft, OpenAPI antes Fase 1 — `docs/12-api-boundaries.md:30`; React futuro igual.
- DTOs del API (Regla 7), 0 JPA/DataSource en JSF (Regla 2/3), dominio sin HTTP (Regla 8).
- `RestClient` con `app.api.base-url=http://localhost:8082`, `timeout=5000`, `auth.token-url=/platform/auth/login`, `X-Correlation-Id` — `docs/03-technology-stack.md:47`, Regla 15, `AGENTS.md:Config`.
- Estructura: `management-jsf-app/rest-client/*` + `presentation/jsf/@Named/@ViewScoped` + `ui/*` — `AGENTS.md`.
- `server.port=8083`, `application.properties` no YAML.

## Auth
`POST /platform/auth/login` → JWT en `HttpSession` → interceptor `Authorization: Bearer`.

## Check
`grep -R "spring.datasource\|EntityManager" management-jsf-app` debe ser vacío.
