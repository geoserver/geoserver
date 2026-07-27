# CAS Security Assembly

Zip packager for the CAS (Central Authentication Service) security extension.

The CAS plugin is split across two sibling modules:

* `src/extension/security/cas` -> `gs-sec-cas` (core CAS integration)
* `src/extension/security/web/web-cas` -> `gs-web-sec-cas` (CAS UI for the web
  admin)

Because `gs-web-sec-cas` depends on `gs-sec-cas`, Maven builds `cas` before
`web-cas`. Running the assembly from `cas/` would therefore miss the web jar.
This module exists solely to anchor the assembly: it depends on both sibling
jars so that by the time its `install` phase runs, both have been built and
land in its `target/dependency/` via `maven-dependency-plugin:copy-dependencies`.
