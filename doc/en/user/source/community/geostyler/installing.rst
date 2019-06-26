Installing the GeoStyler extension
=============================================

 #. Download the GeoStyler extension from the `nightly GeoServer community module builds <https://build.geoserver.org/geoserver/master/community-latest/>`_.

    .. warning:: Make sure to match the version of the extension to the version of the GeoServer instance!

 #. Extract the contents of the archive into the ``WEB-INF/lib`` directory of the GeoServer installation.

Side note: the extension currently needs a fix in core in order to work. Unless those PRs are merged you'll have to manually build your GeoServer based on the contents of the PRs (see https://github.com/geoserver/geoserver/pull/3605 and https://github.com/geoserver/geoserver/pull/3604 for reference). The module might actually work in the 2.15 series, but that's highly experimental.
