# GeoServer Release

This module depends on all the others in order to stage their artifacts for release,
as such it will be scheduled at the end of the maven multi-module build.


## Assembly definition and processing

This module contains the core assembly descriptors (used by ``src/pom.xml``) to package the application and documentation into ``zip`` downloads for release:

* ``war.xml`` - GeoServer WAR file
* ``bin.xml`` - Binary distribution with Jetty
* ``javadoc.xml`` - API documentation
* ``data.xml`` - Sample data directory

Extension assemblies are now built per-module using the assembly profile defined in ``src/extension/pom.xml``. Each extension module has its own ``src/assembly/assembly.xml`` descriptor. See ``src/extension/README.md`` for details.

Each assembly descriptor packages the information that has been staged into `release/target`:

* ``target/dependency``
* ``target/html``
* ``target/licenses``

To build locally:

```bash
cd src
# Step 1: Build all modules with release profile
mvn clean install -Prelease -DskipTests

# Step 2: Build core assemblies (war, bin, javadoc, data)
mvn assembly:single -nsu -N

# Step 3: Build extension assemblies (NO clean)
cd extension
mvn install -Prelease,assembly -DskipTests
```

## Markdown processing

The module processes markdown  files into ``html`` for including release bundles:

- ``/src/release/src/markdown/``
- ``/src/release/extensions/``
- ``/licenses/``
- ``/LICENSE.txt``

## Jetty

The standalone jetty environment for our binary release download:

* ``jetty/``

## Installer

Windows installer NSIS environment:

* ``installer/``
