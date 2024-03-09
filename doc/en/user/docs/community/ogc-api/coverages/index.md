---
render_macros: true
---

# OGC API - Coverages

A [OGC API - Coverages](https://github.com/opengeospatial/ogcapi-coverages) based on an earlier specification draft, delivering partial functionality:

-   Collection listing
-   Download in the same formats supported by WCS
-   Spatial and temporal subsetting
-   Mosaic index filtering (GeoServer extension)
-   Domain set description in JSON

Missing functionality at the time of writing, and known issues:

-   Full coverage JSON support
-   Scaling
-   CRS transformation

## OGC API - Coverages Implementation status

| [OGC API - Coverages](https://github.com/opengeospatial/ogcapi-coverages) | Version                                          | Implementation status                                                               |
|---------------------------------------------------------------------------|--------------------------------------------------|-------------------------------------------------------------------------------------|
| Part 1: Core                                                              | [Draft](https://docs.ogc.org/DRAFTS/19-087.html) | Implementation based on early specification draft, not yet updated to final version |

## Installing the GeoServer OGC API - Coverages module

1.  Download the OGC API nightly GeoServer community module from {{ download_community('ogcapi-coverages','snapshot') }}.

    !!! warning

        Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-{{ release }}-ogcapi-coverages-plugin.zip above).

2.  Extract the contents of the archive into the `WEB-INF/lib` directory of the GeoServer installation.

3.  On restart the services are listed at <http://localhost:8080/geoserver>

## Configuration of OGC API - Covearges module

The module is based on the GeoServer WCS one, follows the same configuration and exposes the same coverages.
