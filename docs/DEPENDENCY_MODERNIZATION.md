# Dependency Modernization & jakarta EE Migration

This document tracks the dependency-modernization effort that accompanies the
Java 17 → 25 migration, and lays out the remaining `javax.*` → `jakarta.*`
work as concrete, executable increments.

## Done

- **SBOM**: CycloneDX 1.6 aggregate BOM generated via `cyclonedx-maven-plugin`
  (see `docs/sbom/`).
- **Core JTA → jakarta**: `javax.transaction` → `jakarta.transaction` in
  `api` / `engine` / `runtime`; `jakarta.transaction-api` 1.3.3 → 2.0.1.
  (`javax.transaction.xa` intentionally kept — it remains in the JDK.)
- **Safe security/maintenance bumps** (same major, no API break): guava 33.4.0,
  commons-lang3 3.17.0, commons-io 2.18.0, commons-codec 1.17.1, xerces 2.12.2,
  xalan 2.7.3, postgresql 42.7.4, jboss-logging 3.6.1, jna 5.16, junit 4.13.2,
  checker-qual 3.42.0.

All of the above is verified on JDK 25: the core reactor + embedded MVP build
green, unit tests pass, and the CLI queries the sample VDB end-to-end.

## Remaining jakarta EE migration (by coupled cluster)

The remaining `javax.*` packages cannot be flipped in isolation — each is
coupled to a major third-party library upgrade whose API changed well beyond
the namespace. None of these modules are on the embedded MVP / CI path.

### 1. Web services cluster — `javax.ws.rs` + `javax.xml.ws` + `javax.activation`
- Modules: `connectors/webservice/ws-cxf`, `connectors/webservice/translator-ws`,
  `connectors/odata/translator-odata`, `connectors/odata/translator-odata4`,
  `connectors/openapi/translator-openapi`, plus `javax.activation` in
  `common-core` and `engine`.
- Library bumps: **Apache CXF 3.3.6 → 4.x**, `jakarta.ws.rs-api` 3/4,
  `jakarta.xml.ws-api` 4.x (+ JAX-WS RI), `jakarta.activation-api` 2.x.
- Effort: CXF 4 moved to the jakarta namespace and reworked its client/JAX-RS
  APIs; `javax.activation` is coupled here because CXF 3 SOAP attachments use
  `javax.activation.DataHandler`. `activation` must move in lockstep with CXF.

### 2. Servlet / OData cluster — `javax.servlet`
- Module: `olingo` (18 files), with `olingo-common`.
- Library bumps: **`jakarta.servlet-api` 4 → 6**, **Apache Olingo 4.7.1 → 5.x**
  (Olingo 5 is the first jakarta release), **Jetty 9.4 → 12** (test scope).
- Effort: Olingo 5 and Jetty 12 both carry significant API changes on top of the
  servlet namespace flip.

### 3. Persistence cluster — `javax.persistence`
- Modules: `connectors/misc/translator-jpa` (11 files), `eclipselink-platform`.
- Library bumps: **`jakarta.persistence-api` 3.x**, **Hibernate 5.4 → 6.x**
  (`hibernate-dialect`), **EclipseLink 2.5 → 4.x**.
- Effort: Hibernate 6 rewrote the `Dialect` SPI that `hibernate-dialect`
  extends; this is a real reimplementation, not a rename.

## Recommended sequencing

Each cluster above is an independent, self-contained increment that keeps the
reactor green when completed and verified on its own. Suggested order by
risk/value: (3) persistence → (1) web services → (2) servlet/OData. Each should
be a separate change with its own build+test verification on JDK 25.

## Environment note

`translator-odata`, `translator-accumulo` and `translator-olap` currently fail
dependency *resolution* in the CI sandbox (unreachable third-party artifacts:
`org.jboss.oreva`, an OpenJFX snapshot, `olap4j`/mondrian) — independent of Java
version. They are excluded from the sandbox build but may resolve in a fully
provisioned CI.
