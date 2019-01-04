.. _wms_configuration: 

WMS configuration
=================

Layer Groups
------------

A Layer Group is a group of layers that can be referred to by one layer name.  For example, if you put three layers (call them layer_A, layer_B, and layer_C) under the one "Layer Group" layer, then when a user makes a WMS getMap request for that group name, they will get a map of those three layers.

For information on configuring Layer Groups in the Web Administration Interface see :ref:`data_webadmin_layergroups`

.. _wms_configuration_limits:

Request limits
--------------

The request limit options allow the administrator to limit the resources consumed by each WMS ``GetMap`` request.

The following table shows the option names, a description, and the minimum GeoServer version at which the option is available (older versions will ignore it if set).

.. list-table::
   :widths: 10 80 10

   * - **Option**
     - **Description**
     - **Version**
   * - **Max rendering memory (KB)**
     - Sets the maximum amount of memory a single GetMap request is allowed to use (in kilobytes). The limit is checked before request execution by estimating how much memory would be required to produce the output in the format requested.  For example, for an image format the estimate is based on the size of the required rendering memory (which is determined by the image size, the pixel bit depth, and the number of active FeatureTypeStyles at the requested scale).  If the estimated memory size is below the limit, the request is executed; otherwise it is cancelled.
     - 1.7.5
   * - **Max rendering time (s)**
     - Sets the maximum amount of time GeoServer will spend processing a request (in seconds). This time limits the "blind processing" portion of the request, that is, the time taken to read data and compute the output result (which may occur concurrently). If the execution time reaches the limit, the request is cancelled.  The time required to write results back to the client is not limited by this parameter, since this is determined by the (unknown) network latency between the server and the client. For example, in the case of PNG/JPEG image generation, this option limits the data reading and rendering time, but not the time taken to write the image out.
     - 1.7.5
   * - **Max rendering errors (count)**
     - Sets the maximum amount of rendering errors tolerated by a GetMap request. By default GetMap makes a best-effort attempt to serve the result, ignoring invalid features, reprojection errors and the like. Setting a limit on the number of errors ignored can make it easier to notice issues, and conserves CPU cycles by reducing the errors which must be handled and logged
     - 1.7.5
   * - **Max number of dimension values**
     - Sets the maximum number of dimension (time, elevation, custom) values that a client can request in a GetMap/GetFeatureInfo request (the work to be done is usually proportional to said number of times, and the list of values is kept in memory during the processing)
     - 2.14.0


The default value of each limit is ``0``, which specifies that the limit is not applied.

If any of the request limits is exceeded, the GetMap operation is cancelled and a ``ServiceException`` is returned to the client.

When setting the above limits it is suggested that peak conditions be taken into consideration. 
For example, under normal circumstances a GetMap request may take less than a second.  Under high load it is acceptable for it to take longer, but it's usually not desirable to allow a request to go on for 30 minutes. 

The following table shows examples of reasonable values for the request limits:

.. list-table::
   :widths: 20 10 70

   * - **Option**
     - **Value**
     - **Rationale**
   * - maxRequestMemory
     - 16384
     - 16MB are sufficient to render a 2048x2048 image at 4 bytes per pixel (full color and transparency), or a 8x8 meta-tile when using GeoWebCache or TileCache. Note that the rendering process uses a separate memory buffer for each FeatureTypeStyle in an SLD, so this also affects the maximum image size. For example, if an SLD contains two FeatureTypeStyle elements in order to draw cased lines for a highway, the maximum image size will be limited to 1448x1448 (the memory requirement increases with the product of the image dimensions, so halving the memory decreases image dimensions by only about 30%)
   * - maxRenderingTime
     - 120
     - A request that processes for a full two minutes is probably rendering too many features, regardless of the current server load. This may be caused by a request against a big layer using a style that does not have suitable scale dependencies
   * - maxRenderingErrors
     - 100
     - Encountering 100 errors is probably the result of a request trying to reproject a big data set into a projection that is not appropriate for the output extent, resulting in many reprojection failures.

