---
render_macros: true
---

# OGC API - Processes

A [OGC API - Processes](https://github.com/opengeospatial/ogcapi-processes) based on the current specification draft, delivering the following functionality:

- Process listing
- Process description
- Execution via JSON POST (OGC API 1.0 core)
- Execution via KVP invocation (from OGC API 1.1 DRAFT specification)
- Asynchronous execution and dismissal
- The same wealth of input/output options as in WPS (inline, reference, simple and complex, etc.)

Missing functionality at the time of writing, and known issues:

- API definition is not fully aligned yet
- Conformance class configuration is not available
- Process chaining (not part of Core, see the Workflows DRAFT extension)

## OGC API - Processes Implementation status

| [OGC API - Maps](https://github.com/opengeospatial/ogcapi-processes) | Version | Implementation status |
|----|----|----|
| Part 1: Core | [Draft](https://docs.ogc.org/is/18-062r2/18-062r2.html) | Implementation based on current specification draft (KVP is not part of Processes 1.0, but it's part of the current draft) |

## Installing the GeoServer OGC API - Processes module

1.  Download the OGC API nightly GeoServer community module from [ogcapi-processes](https://build.geoserver.org/geoserver/main/community-latest/ogcapi-processes).

    !!! warning

        Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-{{ release }}-ogcapi-processes-plugin.zip above).

2.  Extract the contents of the archive into the `WEB-INF/lib` directory of the GeoServer installation.

3.  On restart the services are listed at <http://localhost:8080/geoserver>

## Configuration of OGC API - Processes module

The module is based on the GeoServer WPS one, follows the same configuration and exposes the same processes (so for example, it's possible to limit the processes one wants to expose via the same configuration mechanisms as WPS).
