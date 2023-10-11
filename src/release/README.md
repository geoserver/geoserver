# GeoServer Release

This module depends on all the others in order to stage their artifacts for release,
as such it will be scheduled at the end of the maven multi-module build.


## Assembly definition and processing

This module contains (but does not run) the assembly descriptors (used by ``src/pom.xml``) used package up the application, extension and documentation into ``zip`` downloads for release.

Each assembly descriptor packages the information that has been staged into `release/target`:

* ``target/dependency``
* ``target/html``
* ``target/lhtml/icenses``

To build locally use use assembly:single target, with `-N` to avoid subdirectories.

```bash
cd src
mvn clean install -Prelease
mvn assembly:single -nsu -N
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
