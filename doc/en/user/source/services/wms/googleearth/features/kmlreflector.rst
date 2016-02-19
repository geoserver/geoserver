.. _ge_feature_kml_reflector:

KML Reflector
=============

Standard WMS requests can be quite long and cumbersome. The following is an example of a request for KML output from GeoServer::

   http://localhost:8080/geoserver/ows?service=WMS&request=GetMap&version=1.1.1&format=application/vnd.google-earth.kml+XML&width=1024&height=1024&srs=EPSG:4326&layers=topp:states&styles=population&bbox=-180,-90,180,90

GeoServer includes an alternate way of requesting KML, and that is to use the **KML reflector**. The KML reflector is a simpler URL-encoded request that uses sensible defaults for many of the parameters in a standard WMS request. Using the KML reflector one can shorten the above request to::

   http://localhost:8080/geoserver/wms/kml?layers=topp:states

Using the KML reflector
-----------------------

The only mandatory parameter is the ``layers`` parameter. The syntax is as follows::

  http://GEOSERVER_URL/wms/kml?layers=<layer>

where ``GEOSERVER_URL`` is the URL of your GeoServer instance, and ``<layer>`` is the name of the featuretype to be served.  
  
The following table lists the default assumptions:

.. list-table::
   :widths: 20 80
   
   * - **Key**
     - **Value**
   * - ``request``
     - ``GetMap``
   * - ``service``
     - ``wms``
   * - ``version``
     - ``1.1.1``
   * - ``srs``
     - ``EPSG:4326``
   * - ``format``
     - ``applcation/vnd.google-earth.kmz+xml``
   * - ``width``
     - ``2048``
   * - ``height``
     - ``2048``
   * - ``bbox``
     - ``<layer bounds>``
   * - ``kmattr``
     - ``true``
   * - ``kmplacemark``
     - ``false``
   * - ``kmscore``
     - ``40``
   * - ``styles``
     - [default style for the featuretype]

Any of these defaults can be changed when specifying the request. For instance, to specify a particular style, one can append ``styles=population`` to the request::

   http://localhost:8080/geoserver/wms/kml?layers=topp:states&styles=population

To specify a different bounding box, append the parameter to the request::

   http://localhost:8080/geoserver/wms/kml?layers=topp:states&bbox=-124.73,24.96,-66.97,49.37

Reflector modes
---------------

The KML reflector can operate in one of three modes: **refresh**, **superoverlay**, and **download**.

The mode is set by appending the following parameter to the URL::

   mode=<mode>

where ``<mode>`` is one of the three reflector modes.  The details for each mode are as follows:   
   
.. list-table::
   :widths: 20 80

   * - **Mode**
     - **Description**
   * - ``refresh``
     - (*default for all versions except 1.7.1 through 1.7.5*) Returns dynamic KML that can be refreshed/updated by the Google Earth client. Data is refreshed and new data/imagery is downloaded when zooming/panning stops. This mode can return either vector or raster (placemark or overlay) The decision to return either vector or raster data is determined by the value of ``kmscore``.  Please see the section on :ref:`ge_feature_kml_scoring` for more information.
   * - ``superoverlay``
     - (*default for versions 1.7.1 through 1.7.5*) Returns KML as a super-overlay. A super-overlay is a form of KML in which data is broken up into regions.  Please see the section on :ref:`ge_feature_kml_super_overlays` for more information.
   * - ``download``
     - Returns KML which contains the entire data set. In the case of a vector layer, this will include a series of KML placemarks. With raster layers, this will include a single KML ground overlay. This is the only mode that doesn't dynamically request new data from the server, and thus is self-contained KML.

More about the "superoverlay" mode
----------------------------------

When requesting KML using the ``superoverlay`` mode, there are four additional submodes available regarding how and when data is requested. These options are set by appending the following parameter to the KML reflector request::

    superoverlay_mode=<submode>
	
where ``<submode>`` is one of the following options:

.. list-table::
   :widths: 20 80

   * - **Submode**
     - **Description**
   * - ``auto``
     - (*default*) Always returns vector features if the original data is in vector form, and returns raster imagery if the original data is in raster form. This can sometimes be less than optimal if the geometry of the features are very complicated, which can slow down Google Earth.
   * - ``raster``
     -  Always returns raster imagery, regardless of the original data. This is almost always faster, but all vector information is lost in this view.
   * - ``overview``
     - Displays either vector or raster data depending on the view. At higher zoom levels, raster imagery will be displayed, and at lower zoom levels, vector features will be displayed. The determination for when to switch between vector and raster is made by the regionation parameters set on the server.  See the section on :ref:`ge_feature_kml_regionation` for more information.
   * - ``hybrid``
     - Displays both raster and vector data at all times.

