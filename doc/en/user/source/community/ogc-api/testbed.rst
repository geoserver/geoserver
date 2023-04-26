OGC Testbed Experiments
=======================

The following modules are part of the OGC Testbed experiments.

Images and Changesets
---------------------

A couple of extra API, images and changesets, based on engineering reports of `Testbed 15 <https://docs.ogc.org/per/19-018.html>`__, 
allowing to update a image mosaic and get a list of tiles affected by the change.

To install these modules:

#. Download the OGC API nightly GeoServer community module from :download_community:`ogcapi-images`.
   
   .. warning:: Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-|release|-ogcapi-images-plugin.zip above).

#. Extract the contents of the archive into the ``WEB-INF/lib`` directory of the GeoServer installation.

#. On restart the services are listed at http://localhost:8080/geoserver

DGGS
----

The DGGS functionality exposes data structured as DGGS, based on either H2 or rHealPix,
based on engineering reports of `Testbed 18 <https://docs.ogc.org/per/20-039r2.html>`__.

To make full usage of the DGGS functionality, a ClickHouse installation is also needed.

To install these modules:

#. Download the OGC API nightly GeoServer community module from :download_community:`ogcapi-dggs`.
   
   .. warning:: Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-|release|-ogcapi-dggs-plugin.zip above).

#. Extract the contents of the archive into the ``WEB-INF/lib`` directory of the GeoServer installation.

#. On restart the services are listed at http://localhost:8080/geoserver
