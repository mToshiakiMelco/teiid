# Dependency Modernization

This document tracks the migration of Teiid off the legacy Java EE (`javax.*`)
platform and onto Java 17 LTS + Jakarta EE 9+/10 (`jakarta.*`). The work is
delivered as a series of small, independent pull requests, each scoped to a
single dependency axis so it can be reviewed and verified on its own. Every axis
is premised on an integration-test environment: the modernized modules are
exercised by their in-module JUnit suites (embedded Teiid + in-VM transports),
while full end-to-end runtime verification is expected to run in a dedicated
integration environment.

## Status

| # | Axis | From | To | State |
|---|------|------|----|-------|
| 1 | Java runtime | Java 9/11 build, WildFly subsystem | **Java 17 LTS**, standalone embedded MVP | Done |
| 2 | Persistence | `javax.persistence`, Hibernate 5.4, EclipseLink 2.5 | **`jakarta.persistence` 3.1**, Hibernate 6.6, EclipseLink 4.0 | Done |
| 3 | Web service | `javax.ws.rs`/`javax.xml.ws`/`javax.activation`, CXF 3.3 | **`jakarta.ws.rs` 3.1 / `xml.ws` 4.0 / `activation` 2.1**, CXF 4.0 | Done |
| 4 | Servlet / OData | `javax.servlet` (Servlet 4), Olingo 4.7, Jetty 9.4 | **`jakarta.servlet` 6.0**, Olingo 5.0, Jetty 12 (ee10) | Done |

## Approach

Each axis is a disciplined, in-place migration:

- **Namespace rewrite, not shims.** `javax.*` imports are rewritten to
  `jakarta.*`; no compatibility bridge is introduced.
- **Adapt to the new SPI where an upstream API actually changed** (e.g. the
  Hibernate 6 `Dialect` SPI, or the Jetty 12 client/servlet repackaging) rather
  than pinning old versions.
- **Keep scope tight and defer adjacent axes explicitly.** A PR that touches one
  axis calls out what it intentionally leaves for a later one.

## Axis 4 — Servlet / OData (this change)

Migrates the OData4 server layer, which the web-service PR intentionally left as
"a separate axis." The three targets are one atomic change: Olingo 4.7 is built
against `javax.servlet`, Olingo 5.0 depends on `jakarta.servlet-api` 6.0, and
Jetty 12's servlet handlers only exist in the EE-specific packages — all three
sit on Servlet 6 / Jakarta EE 10 and must move together.

- **Root `pom.xml`:** `jakarta.servlet-api` 4.0.3 → **6.0.0** (4.0.3 still ships
  the `javax` namespace); Apache Olingo 4.7.1 → **5.0.0**; Jetty 9.4.41 →
  **12.1.11**. In dependency management, `org.eclipse.jetty:jetty-servlet`
  (removed in Jetty 12) → `org.eclipse.jetty.ee10:jetty-ee10-servlet`, and the
  now-unnecessary `javax.servlet` exclusions on `jetty-server` are dropped.
- **`olingo` module:** every `javax.servlet.*` import in the servlet/filter layer
  (`web/`, `web/gzip/`, `service/OlingoBridge`) rewritten to `jakarta.servlet.*`;
  `WEB-INF/web.xml` bumped from the Servlet 2.5 descriptor to the Jakarta EE 10
  `web-app` 6.0 schema.
- **Olingo 5 API:** `TeiidODataJsonSerializer.complexCollection` now also catches
  the checked `DecoderException` that `writeComplexValue` declares in Olingo 5.
  Olingo 5 additionally tightened `Edm.Decimal` scale validation, so the dynamic
  result type of `$apply=aggregate(x with sum/average as y)` (typed
  `Edm.Decimal` with a null scale → treated as scale 0) rejected fractional
  values; `AggregateDecimalScale` restores the prior behavior by reporting a
  variable scale for those transient aggregate properties only.
- **Tests:** `TestODataIntegration` rebuilt for Jetty 12 — `ee10.servlet`
  handler packages and the reworked Jetty 12 `HttpClient` request/content API
  (`Request.body(...)` / `StringRequestContent` / `BytesRequestContent` /
  `headers(...)`).
- **`connectors/odata/translator-odata4`:** the OData *client* connector shares
  the Olingo version property and is carried to Olingo 5.0 (client API only, no
  servlet code).

### Verification

- `mvn -pl olingo -am install` — builds `olingo-common` + `olingo` and runs the
  OData4 unit + `TestODataIntegration` (embedded Teiid + embedded Jetty 12)
  suites, exercising the full migrated servlet/OData request path.
- `mvn -pl connectors/odata/translator-odata4 -am install` — confirms the OData
  client connector against Olingo 5.

> The `dev` profile (`-P dev`) or `-Dbasepom.check.skip-all=true` skips the
> legacy `basepom` javadoc/dependency-analysis checks, which predate this work
> and are unrelated to the migration.
