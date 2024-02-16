---
render_macros: true
---

# OGC API - Maps {: #ogcapi-maps }

A [OGC API - Maps](https://github.com/opengeospatial/ogcapi-maps) based on the current early specification draft, delivering partial functionality:

-   Collection listing
-   Styles per collection
-   Map and info support for single collection

Missing functionality at the time of writing, and known issues:

-   API definition
-   Maps specific metadata (e.g., scale ranges)
-   Support for multi-collection map representation
-   HTML representation of a map with OpenLayers is only partially functional

## OGC API - Maps Implementation status

  ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  [OGC API - Maps](https://github.com/opengeospatial/ogcapi-maps)   Version                                                                                               Implementation status
  ----------------------------------------------------------------- ----------------------------------------------------------------------------------------------------- ----------------------------------------------------
  Part 1: Core                                                      [Draft](https://docs.ogc.org/DRAFTS/20-057.html)                                                      Implementation based on early specification draft.

  Part 2: Partitioning                                              [Draft](https://github.com/opengeospatial/ogcapi-maps/tree/master/extensions/partitioning/standard)   Implementation based on early specification draft.
  ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

## Installing the GeoServer OGC API - Maps module

1.  Download the OGC API nightly GeoServer community module from `ogcapi-maps`{.interpreted-text role="download_community"}.

    !!! warning

        Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver- {{ release }}-ogcapi-maps-plugin.zip above).

2.  Extract the contents of the archive into the `WEB-INF/lib` directory of the GeoServer installation.

3.  On restart the services are listed at <http://localhost:8080/geoserver>

## Configuration of OGC API - Maps module

The module is based on the GeoServer WMS one, follows the same configuration and exposes the same layers. As a significant difference, Maps does not have a concept of layer tree, so only individual layers and groups can be exposed.
