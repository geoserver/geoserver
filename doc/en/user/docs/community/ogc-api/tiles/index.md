---
render_macros: true
---

---
render_macros: true
---

# OGC API - Tiles

A [OGC Tiles API](https://github.com/opengeospatial/OGC-API-Tiles) delivering both tiled data (vector tiles) and tiled maps (classic map tiles).

GeoServer implementation is based on an ealier specification draft (the specification is now finalized).

## OGC API - Tiles Implementation status

| [OGC API - Tiles](https://github.com/opengeospatial/ogcapi-tiles) | Version | Implementation status |
|----|----|----|
| Part 1: Core | [Draft](https://docs.ogc.org/DRAFTS/20-057.md) | Implementation based on early specification draft, not yet updated to final version |

## Installing the GeoServer OGC API - Tiles module

1.  Login, and navigate to **About & Status > About GeoServer** and check **Build Information** to determine the exact version of GeoServer you are running.

2.  Visit the [website download](https://geoserver.org/download) page, change the **Development** tab, and locate the nightly release that corresponds to the GeoServer you are running.

    Follow the **Community Modules** link and download `ogcapi-tiles` zip archive.

    - {{ version }} example: [ogcapi-tiles](https://build.geoserver.org/geoserver/main/community-latest/geoserver-{{ version }}-SNAPSHOT-ogcapi-tiles-plugin.zip)

    The website lists active nightly builds to provide feedback to developers, you may also [browse](https://build.geoserver.org/geoserver/) for earlier branches.

3.  Extract the contents of the archive into the **`WEB-INF/lib`** directory in GeoServer.

    !!! warning

        Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-{{ version }}-ogcapi-tiles-plugin.zip above).

4.  Restart GeoServer.

    On restart the services are listed at <http://localhost:8080/geoserver>

## Configuration of OGC API - Tiles module

The module exposes all Tiled layers configured for GeoWebCache using the OGC API - Tiles specification.

As such, it follows the same WMTS and Tile Caching configuration used to manage GeoWebCache.
