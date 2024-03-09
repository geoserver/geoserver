---
render_macros: true
---

# OGC API - Styles

A [OGC Styles API](https://github.com/opengeospatial/ogcapi-styles) based on the early draft of this specification.

This service describes, retrieves and updates the styles present on the server. Styles are cross linked with their associated collections in both the Features and Tiles API.

## OGC API - Styles Implementation status

| [OGC API - Styles](https://github.com/opengeospatial/ogcapi-styles) | Version                                                    | Implementation status                              |
|---------------------------------------------------------------------|------------------------------------------------------------|----------------------------------------------------|
| Part 1: Core                                                        | [Draft](http://docs.opengeospatial.org/DRAFTS/20-009.html) | Implementation based on early specification draft. |

## Installing the GeoServer OGC API - Styles module

1.  Download the OGC API nightly GeoServer community module from {{ download_community('ogcapi-styles','snapshot') }}.

    !!! warning

        Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-{{ release }}-ogcapi-styles-features-plugin.zip above).

2.  Extract the contents of the archive into the `WEB-INF/lib` directory of the GeoServer installation.

3.  On restart the services are listed at <http://localhost:8080/geoserver>

## Configuration of OGC API - Styles module

At the time of writing the module has no configuration, it's simply exposing all available GeoServer styles.
