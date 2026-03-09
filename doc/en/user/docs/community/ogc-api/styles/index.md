---
render_macros: true
---


# OGC API - Styles

A [OGC Styles API](https://github.com/opengeospatial/ogcapi-styles) based on the early draft of this specification.

This service describes, retrieves and updates the styles present on the server. Styles are cross linked with their associated collections in both the Features and Tiles API.

## OGC API - Styles Implementation status

| [OGC API - Styles](https://github.com/opengeospatial/ogcapi-styles) | Version | Implementation status |
|----|----|----|
| Part 1: Core | [Draft](http://docs.opengeospatial.org/DRAFTS/20-009.md) | Implementation based on early specification draft. |

## Installing the GeoServer OGC API - Styles module

1.  Login, and navigate to **About & Status > About GeoServer** and check **Build Information** to determine the exact version of GeoServer you are running.

2.  Visit the [website download](https://geoserver.org/download) page, change the **Development** tab, and locate the nightly release that corresponds to the GeoServer you are running.

    Follow the **Community Modules** link and download `ogcapi-styles` zip archive.

    - {{ snapshot }} example: [ogcapi-styles](https://build.geoserver.org/geoserver/main/community-latest/geoserver-{{ snapshot }}-ogcapi-styles-plugin.zip)

    The website lists active nightly builds to provide feedback to developers, you may also [browse](https://build.geoserver.org/geoserver/) for earlier branches.

3.  Extract the contents of the archive into the **`WEB-INF/lib`** directory in GeoServer.

    !!! warning

        Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-{{ snapshot }}-ogcapi-styles-plugin.zip above).

4.  Restart GeoServer.

    On restart the services are listed at <http://localhost:8080/geoserver>

## Configuration of OGC API - Styles module

At the time of writing the module has no configuration, it's simply exposing all available GeoServer styles.
