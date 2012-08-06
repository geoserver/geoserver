.. _gwc_responseheaders:

HTTP Response Headers
=====================

GeoWebCache returns both standard and custom HTTP response headers when serving a tile request, both to adhere to an HTTP 1.1 transfer control mechanism and to aid in debugging problems.

The response headers can be determined via a utility such as `cURL <http://curl.haxx.se>`_.

Example
-------

 
This is a sample request and response using cURL::

  curl -v "http://localhost:8080/geowebcache/service/wms?LAYERS=sde%3Abmworld&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&STYLES=&SRS=EPSG%3A4326&BBOX=-180,-38,-52,90&WIDTH=256&HEIGHT=256&tiled=true" > /dev/null 

::

 < HTTP/1.1 200 OK
 < geowebcache-tile-index: [0, 1, 2]
 < geowebcache-cache-result: HIT
 < geowebcache-tile-index: [0, 1, 2]
 < geowebcache-tile-bounds: -180.0,-38.0,-52.0,90.0
 < geowebcache-gridset: GlobalCRS84Pixel
 < geowebcache-crs: EPSG:4326
 < Content-Type: image/png
 < Content-Length: 102860
 < Server: Jetty(6.1.8)
 
From this, one can learn that the tile was found in the cache (``HIT``), the requested tile was from the gridset called ``GlobalCRS84Pixel`` and had a CRS of ``EPSG:4326``.


Full list of response headers
-----------------------------

The following is the full list of response headers.  Whenever GeoWebCache serves a tile request, it will write some or all of the following custom headers on the HTTP response.

.. list-table::
   :header-rows: 1

   * - Response Header
     - Description
   * - ``geowebcache-cache-result``
     - Shows whether the GeoWebCache WMS was used.  Options are:

       * ``HIT``: Tile requested was found on the cache
       * ``MISS``: Tile was not found on the cache but was acquired from the layer's data source
       * ``WMS``: Request was proxied directly to the origin WMS (for example, for GetFeatureInfo requests)
       * ``OTHER``: Response was the default white/transparent tile or an error occurred
   * - ``geowebcache-tile-index``
     - Contains the three-dimensional tile index in x,y,z order of the returned tile image in the corresponding grid space (e.g. ``[1, 0, 0]``)
   * - ``geowebcache-tile-bounds``
     - Bounds of the returned tile in the corresponding coordinate reference system (e.g. ``-180,-90,0,90``)
   * - ``geowebcache-gridset``
     - Name of the gridset the tile belongs to (see :ref:`webadmin_tilecaching_gridsets` for more information)
   * - ``geowebcache-crs``
     - Coordinate reference system code of the matching gridset (e.g. ``EPSG:900913``, ``EPSG:4326``, etc).
