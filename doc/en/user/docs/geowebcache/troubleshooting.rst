.. _gwc_troubleshooting:

Troubleshooting
===============

This section will discuss some common issues with the integrated GeoWebCache and their solutions.

Grid misalignment
-----------------

Sometimes errors will occur when requesting data from GeoWebCache endpoints.  The error displayed might say that the "resolution is not supported" or the "bounds do not align."  This is due to the client making WMS requests that do not align with the grid of tiles that GeoWebCache has created, such as differing map bounds or layer bounds, or an unsupported resolution.  If you are using OpenLayers as a client, looking at the source code of the included demos may provide more clues to matching up the grid.

An alternative workaround is to enable direct WMS integration with the GeoServer WMS.  You can set this on the :ref:`gwc_webadmin_defaults` page.


Direct WMS integration
----------------------

Direct integration allows WMS requests served through GeoServer to be cached as if they were received and processed by GeoWebCache.  With Direct WMS Integration, a request may either be handled by the GeoServer WMS or GeoWebCache WMS.

Sometimes requests that should go to GeoWebCache will instead be passed through to GeoServer, resulting in no tiles saved.  That said, it is possible to determine why a request was not handled by GeoWebCache when intended.  This is done by using the command-line utility `cURL <http://curl.haxx.se>`_ and inspecting the response headers. 

First, obtain a sample request.  This can easily be done by going to the Layer Preview for a given layer, setting the :guilabel:`Tiled` parameter to :guilabel:`Tiled`, then right-clicking on an area of the map and copy the full path to the image location.  If done correctly, the result will be a GET request that looks something like this::

  http://localhost:8090/geoserver/nurc/wms?LAYERS=nurc%3AArc_Sample&STYLES=&FORMAT=image%2Fjpeg&TILED=true&TILESORIGIN=-180%2C-90&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG%3A4326&BBOX=-45,-45,0,0&WIDTH=256&HEIGHT=256

You can then paste this URL into a curl request:

.. code-block:: console

  curl -v "URL"

For example:

.. code-block:: console

  curl -v "http://localhost:8090/geoserver/nurc/wms?LAYERS=nurc%3AArc_Sample&STYLES=&FORMAT=image%2Fjpeg&TILED=true&TILESORIGIN=-180%2C-90&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG%3A4326&BBOX=-45,-45,0,0&WIDTH=256&HEIGHT=256"

.. note:: To omit the raw image output to the terminal, pipe the output to your system's null.  On Linux / OS X, append ``> /dev/null`` to these requests, and on Windows, append ``> nul``.

If the request doesn't go through GeoWebCache's WMS, a reason will be given in a custom response header.  Look for the following response headers:

* ``geowebcache-cache-result``:  Will say ``HIT`` if the GeoWebCache WMS processed the request, and ``MISS`` otherwise.
* ``geowebcache-miss-reason``:  If the above shows as ``MISS``, this will generated a short description of why the request wasn't handled by the GeoWebCache WMS.


The following are some example requests made along with the responses.  These responses have been truncated to show only the information relevant for troubleshooting.

Successful request
~~~~~~~~~~~~~~~~~~

This request was successfully handled by the GeoWebCache WMS.

Request:

.. code-block:: console

   curl -v "http://localhost:8080/geoserver/topp/wms?TILED=true&LAYERS=states&FORMAT=image/png&REQUEST=GetMap&STYLES=&SRS=EPSG:4326&BBOX=-135,45,-112.5,67.5&WIDTH=256&HEIGHT=256"

Response::

   < HTTP/1.1 200 OK
   < Content-Type: image/png
   < geowebcache-crs: EPSG:4326
   ...
   < geowebcache-layer: topp:states
   < geowebcache-gridset: EPSG:4326
   < geowebcache-tile-index: [2, 6, 3]
   ...
   < geowebcache-cache-result: HIT
   < geowebcache-tile-bounds: -135.0,45.0,-112.5,67.5
   ...

Wrong height parameter
~~~~~~~~~~~~~~~~~~~~~~

The following request is not handled by the GeoWebCache WMS because the image requested (256x257) does not conform to the expected 256x256 tile size.

Request:

.. code-block:: console

   curl -v "http://localhost:8080/geoserver/topp/wms?TILED=true&LAYERS=states&FORMAT=image/png&REQUEST=GetMap&STYLES=&SRS=EPSG:4326&BBOX=-135,45,-112.5,67.5&WIDTH=256&HEIGHT=257"

Response::

   < HTTP/1.1 200 OK
   < Content-Type: image/png
   < geowebcache-miss-reason: request does not align to grid(s) 'EPSG:4326' 
   ...

No tile layer associated
~~~~~~~~~~~~~~~~~~~~~~~~

The following request is not handled by the GeoWebCache WMS because the layer requested has no tile layer configured.

Request:

.. code-block:: console

   curl -v "http://localhost:8080/geoserver/topp/wms?TILED=true&LAYERS=tasmania_roads&FORMAT=image/png&REQUEST=GetMap&STYLES=&SRS=EPSG:4326&BBOX=-135,45,-112.5,67.5&WIDTH=256&HEIGHT=256"

Response::

   < HTTP/1.1 200 OK
   < Content-Type: image/png
   < geowebcache-miss-reason: not a tile layer
   ...

Missing parameter filter
~~~~~~~~~~~~~~~~~~~~~~~~

The following request is not handled by the GeoWebCache WMS because the request contains a parameter filter (BGCOLOR) that is not configured for this layer.

Request:

.. code-block:: console

   curl -v "http://localhost:8080/geoserver/topp/wms?BGCOLOR=0xAAAAAA&TILED=true&LAYERS=states&FORMAT=image/png&REQUEST=GetMap&STYLES=&SRS=EPSG:4326&BBOX=-135,45,-112.5,67.5&WIDTH=256&HEIGHT=256"

Response::

   < HTTP/1.1 200 OK
   < Content-Type: image/png
   < geowebcache-miss-reason: no parameter filter exists for BGCOLOR
   ...


CRS not defined
~~~~~~~~~~~~~~~

The following request is not handled by the GeoWebCache WMS because the request references a CRS (EPSG:26986) that does not match any of the tile layer gridsets:

Request:

.. code-block:: console

   curl -v "http://localhost:8080/geoserver/topp/wms?TILED=true&LAYERS=states&FORMAT=image/png&REQUEST=GetMap&STYLES=&SRS=EPSG:26986&BBOX=-135,45,-112.5,67.5&WIDTH=256&HEIGHT=256"

Response::

   < HTTP/1.1 200 OK
   < Content-Type: image/png
   < geowebcache-miss-reason: no cache exists for requested CRS
   ...


Workspace Styles
~~~~~~~~~~~~~~~~

If a cached layer uses a style which is tied to a workspace, the layer needs to be viewed in the context of that workspace in order for the style to be visible.  Trying to cache such a layer will result in an error. 

By default, the embeded GeoWebCache uses the global workspace.  This can be overridden using a ``WORKSPACE`` parameter. To enable this, create a List of Strings Parameter filter for the layer named ``WORKSPACE``.  Set the default to the name of the workspace containing the style.  Setting the other values will not be useful in most cases.

Moving the style to a new workspace will require updating the filter.

This parameter only applies to integrated tile layers.  If you are adding a GeoServer layer on a remote GeoServer directly to GWC, then specify the workspace as part of the path as you would normally.
