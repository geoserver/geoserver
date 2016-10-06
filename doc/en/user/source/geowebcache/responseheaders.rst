.. _gwc_responseheaders:

HTTP Response Headers
=====================

The GeoWebCache integrated with GeoServer employs special information stored in the header of responses.  These headers are available either with direct calls to the :ref:`GeoWebCache endpoint <gwc_endpoint>` or with :ref:`direct WMS integration <gwc_directwms>`.

Custom response headers
-----------------------

GeoWebCache returns both standard and custom HTTP response headers when serving a tile request.  This aids in the debugging process, as well as adhering to an HTTP 1.1 transfer control mechanism.

The response headers can be determined via a utility such as `cURL <http://curl.haxx.se>`_.

Example
~~~~~~~

.. note:: For all cURL commands below, make sure to replace ``>/dev/null`` with ``>nul`` if you are running on Windows.
 
This is a sample request and response using cURL:

.. code-block:: console

  curl -v "http://localhost:8080/geoserver/gwc/service/wms?LAYERS=sde%3Abmworld&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&STYLES=&SRS=EPSG%3A4326&BBOX=-180,-38,-52,90&WIDTH=256&HEIGHT=256&tiled=true" > /dev/null 

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


List of custom response headers
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The following is the full list of custom response headers.  Whenever GeoWebCache serves a tile request, it will write some or all of the following custom headers on the HTTP response.

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
     - Name of the gridset the tile belongs to (see :ref:`gwc_webadmin_gridsets` for more information)
   * - ``geowebcache-crs``
     - Coordinate reference system code of the matching gridset (e.g. ``EPSG:900913``, ``EPSG:4326``, etc).

.. _gwc_lastmodifiedheaders:

Last-Modified and If-Modified-Since
-----------------------------------

Well behaved HTTP 1.1 clients and server applications can make use of ``Last-Modified`` and ``If-Modified-Since`` HTTP control mechanisms to know when locally cached content is up to date, eliminating the need to download the same content again. This can result in considerable bandwidth savings.  (See HTTP 1.1 `RFC 2616 <http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html>`_, sections 14.29 and 14.25, for more information on these mechanisms.)

GeoWebCache will write a ``Last-Modified`` HTTP response header when serving a tile image.  The date is written as an RFC-1123 ``HTTP-Date``::

  Last-Modified: Wed, 15 Nov 1995 04:58:08 GMT

Clients connecting to GeoWebCache can create a "conditional GET" request with the ``If-Modified-Since`` request header.  If the tile wasn't modified after the date specified in the ``Last-Modified`` response header, GeoWebCache will return a ``304`` status code indicating that the resource was available and not modified.

Example
~~~~~~~

A query for a specific tile returns the ``Last-Modified`` response header:

.. code-block:: console

 curl -v "http://localhost:8080/geoserver/gwc/service/wms?LAYERS=img%20states&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&STYLES=&EXCEPTIONS=application%2Fvnd.ogc.se_inimage&SRS=EPSG%3A4326&BBOX=-135,45,-90,90&WIDTH=256&HEIGHT=256" >/dev/null

::

 > Host: localhost:8080
 > Accept: */*
 >
 < HTTP/1.1 200 OK
 ...
 < Last-Modified: Wed, 25 Jul 2012 00:42:00 GMT
 < Content-Type: image/png
 < Content-Length: 31192

This request has the ``If-Modified-Since`` header set to one second after what was returned by ``Last-Modified``:

.. code-block:: console

 curl --header "If-Modified-Since: Wed, 25 Jul 2012 00:42:01 GMT" -v "http://localhost:8080/geoserver/gwc/service/wms?LAYERS=img%20states&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&STYLES=&EXCEPTIONS=application%2Fvnd.ogc.se_inimage&SRS=EPSG%3A4326&BBOX=-135,45,-90,90&WIDTH=256&HEIGHT=256" >/dev/null

::

 > Host: localhost:8080
 > Accept: */*
 > If-Modified-Since: Wed, 25 Jul 2012 00:42:01 GMT
 > 
 < HTTP/1.1 304 Not Modified
 < Last-Modified: Wed, 25 Jul 2012 00:42:00 GMT
 < Content-Type: image/png
 < Content-Length: 31192

The response code is ``304``. As the file hasn't been modified since the time specified in the request, no content is actually transferred.  The client is informed that its copy of the tile is up to date.

However, if you were to set the ``If-Modified-Since`` header to *before* the time stored in ``Last-Modified``, you will instead receive a ``200`` status code and the tile will be downloaded.

This example sets the ``If-Modified-Since`` header to one second before what was returned by ``Last-Modified``:

.. code-block:: console

 curl --header "If-Modified-Since: Wed, 25 Jul 2012 00:41:59 GMT" -v "http://localhost:8080/geoserver/gwc/service/wms?LAYERS=img%20states&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&STYLES=&EXCEPTIONS=application%2Fvnd.ogc.se_inimage&SRS=EPSG%3A4326&BBOX=-135,45,-90,90&WIDTH=256&HEIGHT=256" >/dev/null

::

 > Host: localhost:8080
 > Accept: */*
 > If-Modified-Since: Wed, 25 Jul 2012 00:41:59 GMT
 > 
 < HTTP/1.1 200 OK
 ...
 < Last-Modified: Wed, 25 Jul 2012 00:42:00 GMT
 < Content-Type: image/png
 < Content-Length: 31192

