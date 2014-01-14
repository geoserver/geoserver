.. _community_netcdf-out:

NetCDF Output format
====================
This plugin brings in the ability to encode WCS 2.0.1 Multidimensional output as NetCDF files using the Unidata NetCDF Java library. 

Installing the GeoServer NetCDF Output format extension
-------------------------------------------------------

 #. Download the extension from the `nightly GeoServer community module builds <http://ares.opengeo.org/geoserver/master/community-latest/>`_.

    .. warning:: Make sure to match the version of the extension to the version of the GeoServer instance!

 #. Extract the contents of the archive into the ``WEB-INF/lib`` directory of the GeoServer installation.

Getting a NetCDF output file
----------------------------
Make sure to specify NetCDF as value of the format parameter within the getCoverage request.
As an instance: 
http://localhost:8080/geoserver/wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=it.geosolutions__V&Format=application/x-netcdf...

Current limitations
-------------------

* Only WGS84 output CRS is supported
* Input coverages/slices should share the same bounding box (lon/lat coordinates are the same for the whole ND cube)
* NetCDF output will be produced only when input coverages come from a StructuredGridCoverage2D reader (This will allows to query the GranuleSource to get the list of granules in order to setup dimensions slices for each sub-coverage)