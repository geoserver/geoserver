# Installing the GeoServer FEATURES-TEMPLATING extension

1.  Download the Features Templating community module from {{ download_community('features-templating','snapshot') }}.

!!! warning

    Make sure to match the version of the extension to the version of the GeoServer instance!

2.  Extract the contents of the archive into the `WEB-INF/lib` directory of the GeoServer installation.
3.  The full package requires the OGC API - Features service to be available. If the server does not include it, the jar `gs-features-templating-ogcapi-<version>.jar` should be removed from `WEB-INF/lib`
