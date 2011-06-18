.. _wms_configuration: 

WMS configuration
=================

Layer Groups
------------

A Layer Group is a group of layers that can be referred to by one layer name.  For example, if you put three layers (call them layer_A, layer_B, and layer_C) under the one "Layer Group" layer, then when a user makes a WMS getMap request for that group name, they will get a map of those three layers.

For information on configuring Layer Groups in the Web Administration Interface see :ref:`webadmin_layergroups`

.. _wms_configuration_limits:

Request limits
--------------

The request limit options allow the administrator to limit the resources consumed by each WMS ``GetMap`` request.

The following table shows each option name, a description, and the minimum GeoServer version at which the option is available (old versions will just ignore it if set).

.. list-table::
   :widths: 10 80 10

   * - **Option**
     - **Description**
     - **Version**
   * - **maxRequestMemory**
     - Sets the maximum amount of memory, in kilobytes, a single GetMap request is allowed to use. Each output format will make a best effort attempt to respect the maximum using the highest consuming portion of the request processing as a reference. For example, the PNG output format will take into consideration the memory used to prepare the image rendering surface in memory, usually proportional to the image size multiplied by the number of bytes per pixel
     - 1.7.5
   * - **maxRenderingTime**
     - Sets the maximum amount of time, in seconds, GeoServer will use to process the request. This time limits the "blind processing" portion of the request serving, that is, the part in which GeoServer is computing the results before writing them out to the client. The portion that     is writing results back to the client is not under the control of this parameter, since this time is also controlled by how fast the network between the server and the client is. So, for example, in the case of PNG/JPEG image generation, this option will control the pure rendering time, but not the time used to write the results back.
     - 1.7.5
   * - **maxRenderingErrors**
     - Sets the maximum amount of rendering errors tolerated by a GetMap. Usually GetMap skips over faulty features, reprojection errors and the like in an attempt to serve the results anyways. This makes for a best effort rendering, but also makes it harder to spot issues, and consumes CPU cycles as each error is handled and logged
     - 1.7.5
     
The default value of each limit is ``0``, in this case the limit won't be applied.

Once any of the set limits is exceeded, the GetMap operation will stop and a ``ServiceException`` will be returned to the client.

It is suggested that the administrator sets all of the above limits taking into consideration peak conditions. For example, while a GetMap request under normal circumstance may take less than a second, under high load it is acceptable for it to take longer, but usually, it's not sane that a request goes on for 30 minutes straight. The following table shows an example or reasonable values for the configuration options above:

.. list-table::
   :widths: 20 10 70

   * - **Option**
     - **Value**
     - **Rationale**
   * - maxRequestMemory
     - 16384
     - 16MB are sufficient to render a 2048x2048 image at 4 bytes per pixel (full color and transparency), or a 8x8 meta-tile if you are using GeoWebCache or TileCache. Mind the rendering process will use an extra in memory buffer for each subsequent FeatureTypeStyle in your SLD, so this will also limit the size of the image. For example, if the SLD contains two FeatureTypeStyle element in order to draw cased lines for an highway the maximum image size will be limited to 1448x1448 (the memory goes like the square of the image size, so halving the memory does not halve the image size)
   * - maxRenderingTime
     - 120
     - A request that processes for two minutes straight is probably drawing a lot of features independent of the current load. It might be the result of a client making a GetMap against a big layer using a custom style that does not have the proper scale dependencies
   * - maxRenderingErrors
     - 100
     - Encountering 100 errors is probably the result of a request that is trying to reproject a big data set into a projection that is not suited to area it covers, resulting in many reprojection failures.

