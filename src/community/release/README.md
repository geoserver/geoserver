# Community Release

This module depends on all the community modules that are available for public testing and feedback.
Maven will order it near the end of the maven multi-module build.

## Assembly definition and processing

This module contains (but does not run) the assembly descriptors (used by ``src/community/pom.xml``)
used package up the the community modules into ``zip`` downloads for the nightly build server.

Each assembly descriptor packages the information that has been staged into `community/target`:

* ``target/dependency``
* ``target/html``
* ``target/lhtml/licenses``

To build locally use use assembly:single target, with `-N` to avoid subdirectories.

```bash
cd src/community
mvn clean install -PcommunityRelease
mvn assembly:single -nsu -N
```

## Markdown processing

The module processes markdown  files into ``html`` for including release bundles:

- ``/licenses/``
- ``/LICENSE.md``

## Jetty

The standalone jetty environment for our binary release download:

* ``jetty/``

## Installer

Windows installer NSIS environment:

* ``installer/``
