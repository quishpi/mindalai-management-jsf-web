---
name: harmony-theme
description: Uso de tema Harmony 5.1.0 + PrimeFaces en JSF con templates, layouts y recursos estáticos
license: MIT
compatibility: opencode
---

## Qué hace
Guía el uso correcto del tema en `mindalai-jsf-web` / `mindalai-management-jsf-web` según `ADR-007`, `AGENTS.md:Stack`.

## Cuándo usarme
Al crear/editar XHTML Facelets, templates, CSS o integrar PrimeFaces.

## Reglas
- Tema: `shared-libraries/theme/harmony/harmony-layout-5.1.0.zip` y `harmony-theme-5.1.0-jakarta.jar` / `harmony-theme-5.1.0.jar` — `shared-libraries/theme/harmony/`.
- Layouts disponibles: `layout-*.css/.scss` (bliss, elegance, faith, etc.) + `primeflex.min.css` + `primeicons.css` — `harmony-layout-5.1.0/resources/harmony-layout/`.
- Plantillas: `layout.xhtml` base + `WEB-INF` includes; ejemplos `login.xhtml`, `invoice.xhtml`, `access.xhtml` en `harmony-layout-5.1.0/`.
- Stack JSF: JoinFaces 6.1.0 + Jakarta Faces + PrimeFaces 15+ — `docs/03-technology-stack.md:18`; `application.properties` no YAML.
- No mezclar lógica de negocio en XHTML/backing bean — Regla 4/7/8; UI consume DTOs del API.

## Verificación
- `resources/harmony-layout` accesible vía JSF `h:outputStylesheet`.
- Backing beans `@Named @ViewScoped`, navegación `faces-config.xml`.

## Antipatrón
No crear CSS ad-hoc que rompa `harmony-layout`; extender vía `layout-*.scss` o `primeflex`.
