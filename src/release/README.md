# GeoServer Release

This module stages the shared artifacts needed by the core release zips
(``bin``, ``war``, ``data``, ``javadoc``), rendered HTML licenses and README
files, the Jetty runtime libs for the standalone binary, and the filtered
``start.ini``. It is scheduled at the end of the core reactor so every piece
it references is already built.

## Assembly definition and processing

This module contains the core assembly descriptors (used by ``src/pom.xml``) to package the application and documentation into ``zip`` downloads for release:

* ``war.xml`` - GeoServer WAR file
* ``bin.xml`` - Binary distribution with Jetty
* ``javadoc.xml`` - API documentation
* ``data.xml`` - Sample data directory

``bin.xml`` is the only descriptor that consumes ``release/target/dependency/`` -
it picks up the Jetty jars, ``jakarta.servlet-api``, ``commons-el``,
``commons-logging``, ``ant`` and the slf4j + ``slf4j-reload4j`` bridge from
there. The release pom declares exactly those deps (and nothing more) so
``maven-dependency-plugin:copy-dependencies`` populates the dir with the
minimal set. The other three descriptors only pull from ``target/html`` and
the already-built ``web/app/target/``.

Each assembly descriptor packages information that has been staged into
``release/target``:

* ``target/dependency`` - Jetty runtime libs for ``bin.zip``
* ``target/html`` - rendered license + README HTML
* ``target/jetty`` - filtered ``start.ini``

## Building locally

```bash
cd src
# Step 1: Build core modules (no extensions needed for the main zips)
mvn -nsu clean install -DskipTests -pl release -am

# Step 2: Build the four core zips (war, bin, data, javadoc)
mvn -nsu -N assembly:single

# Step 3: Build extension plugin zips (NO clean - keeps step 1's reactor state)
mvn -nsu -f extension install -Prelease,assembly -DskipTests

# Step 4 (optional): Build community module plugin zips
mvn -nsu -f community install -Ppending,communityRelease,assembly -DskipTests

# Verify the resulting zips against a baseline (main branch worktree, a tagged
# release, etc.) using build/verify-extensions.sh:
../build/verify-extensions.sh /path/to/baseline/src/target/release \
                              src/target/release
```

``build/verify-extensions.sh`` diffs every ``*.zip`` entry-by-entry between
two release directories and prints ``OK`` or a unified diff per zip - useful
both as a local regression check and in CI when landing changes that touch
the assembly pipeline.

## Markdown processing

The module processes markdown files into ``html`` for inclusion in the release bundles:

- ``/src/release/src/markdown/``
- ``/src/release/extensions/`` - shared extension README/LICENSE snippets (per-module READMEs live under each ``src/extension/<mod>/src/assembly/``)
- ``/licenses/``
- ``/LICENSE.txt``

## Jetty

The standalone Jetty environment for the binary release download:

* ``jetty/``

## Installer

Windows installer NSIS environment:

* ``installer/``
