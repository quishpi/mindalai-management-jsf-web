---
name: backoffice-ui
description: UI backoffice Platform Management — navegación por secciones, jerarquía Tenant→Installation→Device y flujo tenant-plan-license
license: MIT
compatibility: opencode
---

## Qué hace
Normaliza la experiencia de `mindalai-management-jsf-web` según `docs/10-platform-management.md:55`, `AGENTS.md:Navigation`.

## Cuándo usarme
Al crear/editar XHTML, backing beans, menús Harmony o flujos admin.

## Secciones — `docs/10-platform-management.md:55`
`Dashboard, Tenants, Plans, Subscriptions, Licenses, Installations, Devices, Usage, Support, Configuration, Audit`.

## Jerarquía — `docs/10-platform-management.md:22`
`Tenant → Installation → Devices (Server + cajas)` como árbol o master-detail.

## Flujo administrativo — `AGENTS.md:Navigation`
`create tenant → assign plan → create subscription → issue license → register installation → add devices → view usage`.

## Reglas
- `management-jsf-app/ui/*.xhtml` con Harmony template + PrimeFaces menu/breadcrumbs.
- `@ViewScoped` para listas con filtros/paginación y forms tenant/plan/license.
- Validación inmediata en vista + mensajes `Bean Validation` del API.
- Auditoría visible: quién/cuándo/resultado cuando API expone `platform_audit`.
- Sin persistencia directa; toda escritura vía `mindalai-management-api`; React futuro sin tocar backend — `docs/02-target-architecture.md:139`.

## Sin SRI
No `sri_*` aquí; catálogos platform no usan enums — Regla 13.
