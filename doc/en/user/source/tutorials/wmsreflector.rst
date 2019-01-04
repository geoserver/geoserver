.. _tutorials_wmsreflector:

WMS Reflector
=============

Overview
--------
Standard WMS requests can be quite long and verbose. For instance the following, which returns an OpenLayers application with an 800x600 image set to display the feature ``topp:states``, with bounds set to the northwestern hemisphere by providing the appropriate bounding box::

   http://localhost:8080/geoserver/wms?service=WMS&request=GetMap&version=1.1.1&format=application/openlayers&width=800&height=600&srs=EPSG:4326&layers=topp:states&styles=population&bbox=-180,0,0,90
  
Typing into a browser, or HTML editor, can be quite cumbersome and error prone. The WMS Reflector solves this problem nicely by using good default values for the options that you do not specify. Using the reflector one can shorten the above request to::

    http://localhost:8080/geoserver/wms/reflect?format=application/openlayers&layers=topp:states&width=800
  
This request only specifies that  you want the reflector (wms/reflect) to return an OpenLayers application (format=application/openlayers), that you want it to display the feature "topp:states" (layers=topp:states) and that the width should be 800 pixels (width=800). However, this will not return the exact same value as above. Instead, the reflector will zoom to the bounds of the feature and return a map that is 800 pixels wide, but with the height adjusted to the aspect ratio of the feature.

Using the WMS Reflector
-----------------------
To use the WMS reflector all one must do is specify ``wms/reflect?`` as opposed to ``wms?`` in a request. The only mandatory parameter to a WMS reflector call is the :guilabel:`layers` parameter. As stated above the reflector fills in sensible defaults for the rest of the parameters. The following table lists all the defaults used:

.. list-table::
   :widths: 50 50  

   * - request
     - getmap
   * - service
     - wms
   * - version
     - 1.1.1 
   * - format
     - image/png 
   * - width
     - 512
   * - height
     - 512 if width is not specified 
   * - srs
     - EPSG:4326 
   * - bbox
     - bounds of layer(s) 
     
Any of these defaults can be overridden when specifying the request. The :guilabel:`styles` parameter is derived by using the default style as configured by GeoServer for each :guilabel:`layer` specified in the layers parameter.

Any parameter you send with a WMS request is also legitimate when requesting data from the reflector. Its strength is what it does with the parameters you do not specify, which is explored in the next section.

**layers**: This is the only mandatory parameter. It is a comma separated list of the layers you wish to include in your image or OpenLayers application.

**format**: The default output format is image/png. Alternatives include image/jpeg (good for raster backgrounds), image/png8 (8 bit colors, smaller files) and image/gif

**width**: Describes the width of the image, alternatively the size of the map in an OpenLayers. It defaults to 512 pixels and can be calculated based on the height and the aspect ratio of the bounding box.

**height**: Describes the height of the image, alternatively the map in an OpenLayers. It can be calculated based on the width and the aspect ratio of the bounding box.

**bbox**: The bounding box is automatically determined by taking the union of the bounds of the specified layers. In essence, it determines the extent of the map. By default, if you do not specify bbox, it will show you everything. If you have one layer of Los Angeles, and another of New York, it show you most of the United States. The bounding box, automatically set or specified, also determines the aspect ratio of the map. If you only specify one of width or height, the other will be determined based on the aspect ratio of the bounding box. 

.. Warning:: If you specify height, width and bounding box there are zero degrees of freedom, and if the aspect ratios do not match your image will be warped.

**styles**: You can override the default styles by providing a comma separated list with the names of styles which must be known by the server.

**srs**: The spatial reference system (SRS) parameter is somewhat difficult. If not specified the WMS Reflector will use EPSG:4326 / WGS84. It will support the native SRS of the layers as well, provided all layers share the same one.

Example 1
`````````
Request the layer topp:states , it will come back with the default style (demographic), width (512 pixels) and height (adjusted to aspect ratio)::

    http://localhost:8080/geoserver/wms/reflect?layers=topp:states

Example 2
`````````
Request the layers topp:states and sf:restricted, it will come back with the default styles, and the specified width (640 pixels) and the height automatically adjusted to the aspect ratio::

    http://localhost:8080/geoserver/wms/reflect?layers=topp:states,sf:restricted&width=640

Example 3
`````````
In the example above the sf:restricted layer is very difficult to see, because it is so small compared to the United States. To give the user a chance to get a better view, if they choose, we can return an OpenLayers application instead. Zoom in on South Dakota (SD) to see the restricted areas::

    http://localhost:8080/geoserver/wms/reflect?format=application/openlayers&layers=topp:states,sf:restricted&width=640
    
Example 4
`````````
Now, if you mainly want to show the restricted layer, but also provide the context, you can set the bounding box for the the request. The easiest way to obtain the coordinates is to use the application in example three and the coordinates at the bottom right of the map. The coordinates displayed in OpenLayers are x , y , the reflector service expects to be given bbox=minx,miny,maxx,maxy . Make sure it contains no whitespaces and users a period (".") as the decimal separator. In our case, it will be bbox=-103.929,44.375,-103.633,44.500 ::

  http://localhost:8080/geoserver/wms/reflect?format=application/openlayers&layers=topp:states,sf:restricted&width=640&bbox=-103.929,44.375,-103.633,44.500
  
Outputting to a Webpage
-----------------------
Say you have a webpage and you wish to include a picture that is 400 pixels wide and that shows the layer ``topp:states``,  on this page.  

.. code-block:: html

  <img src="http://localhost:8080/geoserver/wms/reflect?layers=topp:states&width=400" />

If you want the page to render in the browser before GeoServer is done, you should specify the height and width of the picture. You could just pick any approximate value, but it may be a good idea to look at the generated image first and then use those values. In the case of the layer above, the height becomes 169 pixels, so we can specify that as an attribute in the <img> tag:

.. code-block:: html

  <img src="http://localhost:8080/geoserver/wms/reflect?layers=topp:states&width=400" height="169" width="400"/>
  
If you are worried that the bounds of the layer may change, so that the height changes relative to the width, you may also want to specify the height in the URL to the reflector. This ensures the layer will always be centered and fit on the 400x169 canvas.

The reflector can also create a simple instance of `OpenLayers <http://www.openlayers.org/>`_ that shows the layers you specify in your request. One possible application is to turn the image above into a link that refers to the OpenLayers instance for the same feature, which is especially handy if you think a minority of your users will want to take closer look. To link to this JavaScript application, you need to specify the output format of the reflector: ``format=application/OpenLayers``::

    http://localhost:8080/geoserver/wms/reflect?format=application/openlayers&width=400
    
The image above then becomes

.. code-block:: html

    <a href="http://localhost:8080/geoserver/wms/reflect?format=application/openlayers&layers=topp:states">
    <img src="http://localhost:8080/geoserver/wms/reflect?layers=topp:states&width=400" height="169" width="400" />
    </a>
    
(The a-tags are on separate lines for clarity,  they will in fact result in a space in front and after the image).

OpenLayers in an iframe
-----------------------
Many people do not like iframes, and for good reasons, but they may be appropriate in this case. The following example will run OpenLayers in an iframe.

.. code-block:: html

  <iframe src ="http://localhost:8080/geoserver/wms/reflect?format=application/openlayers&layers=topp:states" width="100%">
  </iframe>
  
Alternatively, you can open OpenLayers in a separate webpage and choose "View Source code" in your browser. By copying the HTML you can insert the OpenLayers client in your own page without using an iframe.













  
