# Software Bill of Materials (SBOM)

This directory holds a [CycloneDX](https://cyclonedx.org/) SBOM for the Teiid
embedded runtime (the `teiid-embedded` module and its full dependency tree).

- `teiid-sbom.json` / `teiid-sbom.xml` — CycloneDX 1.6, aggregate BOM.

## Regenerating

The `cyclonedx-maven-plugin` is wired into the root POM and runs during
`package` (aggregator only), emitting `target/teiid-sbom.{json,xml}`:

```bash
# whole buildable reactor
mvn -Dmaven.javadoc.skip=true -Dbasepom.check.skip-all=true package

# or just the embedded runtime + its dependencies
mvn org.cyclonedx:cyclonedx-maven-plugin:makeAggregateBom \
    -pl embedded -am -DskipTests -Dbasepom.check.skip-all=true
```

Copy the generated files from `target/` into this directory to refresh the
committed snapshot.
