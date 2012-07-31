.. _rest.layers:

Managing Layers through the REST API
====================================

The REST API for Layer management provides a RESTful interface through which clients can 
programatically add, modify, or remove cached Layers.

Layers list
-----------

``/gwc/rest/seed/layers.xml``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
   * - GET
     - Return the list of available layers
     - 200
     - XML
   * - POST
     - 
     - 400
     - 
   * - PUT
     - 
     - 400
     - 
   * - DELETE
     - 
     - 400
     -

Note: JSON representation is intentionally left aside as the library used for JSON marshaling has issues with multi-valued properties such as `parameterFilters`.

Sample request:

.. code-block:: xml

 curl -u admin:geoserver  "http://localhost:8080/geoserver/gwc/rest/layers"

Sample response:
 
.. code-block:: xml

 <layers>
  <layer>
    <name>img states</name>
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:8080/geoserver/gwc/rest/layers/img+states.xml" type="text/xml"/>
  </layer>
  <layer>
    <name>raster test layer</name>
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:8080/geoserver/gwc/rest/layers/raster+test+layer.xml" type="text/xml"/>
  </layer>
  <layer>
    <name>topp:states</name>
    <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:8080/geoserver/gwc/rest/layers/topp%3Astates.xml" type="text/xml"/>
  </layer>
 </layers>

Layer Operations
----------------

``/gwc/rest/seed/layers/<layer>.xml``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
   * - GET
     - Return the XML representation of the Layer
     - 200
     - XML
   * - POST
     - Modify the definition/configuration of a Layer
     - 200
     - XML
   * - PUT
     - Add a new Layer
     - 200
     - XML
   * - DELETE
     - Delete a Layer
     - 200
     -

.. note:: Beware that for the GeoServer integrated version of GeoWebcache, there are two different 
   representations for cached layers, depending on whether the tile layer is created from a GeoServer's 
   WMS layer or layer group (i.e. a ``GeoServerLayer``), or rather it doesn't match a GeoServer layer,
   but is configured in ``geowebcache.xml`` as a regular GWC layer (i.e. a ``wmsLayer``).
   The two formats are pretty similar. The main difference being that "integrated" layers are called
   GeoServerLayer and contain no image data source information, such as origin WMS URL, since they
   match a GeoServer owned layer (or layer group) and hence the image operations are routed to the GeoServer
   rendering engine internally (hence being more efficient).

*Representations*:

- Standalone :download:`XML minimal <representations/wmslayer_minimal.xml.txt>`
- Standalone :download:`XML <representations/wmslayer.xml.txt>`
- Integrated :download:`XML minimal <representations/geoserverlayer_minimal.xml.txt>`
- Integrated :download:`XML <representations/geoserverlayer.xml.txt>`

.. note:: a JSON representation is intentionally left aside as the library used for JSON marshaling has issues with multi-valued properties such as `parameterFilters`.

REST API for Layers, cURL Examples
----------------------------------

The examples in this section use the `cURL <http://curl.haxx.se/>`_
utility, which is a handy command line tool for executing HTTP requests and 
transferring files. Though cURL is used the examples apply to any HTTP-capable
tool or library.

Add Standalone Layer
++++++++++++++++++++

Sample request:

Given a `layer.xml` file as the following:

.. code-block:: xml

 <wmsLayer>
   <name>layer1</name>
   <mimeFormats>
     <string>image/png</string>
   </mimeFormats>
   <gridSubsets>
     <gridSubset>
       <gridSetName>EPSG:900913</gridSetName>
     </gridSubset>
   </gridSubsets>
   <wmsUrl>
     <string>http://localhost:8080/geoserver/wms</string>
   </wmsUrl>
   <wmsLayers>topp:states</wmsLayers>
 </wmsLayer>

.. code-block:: xml 

 curl -v -u admin:geoserver -XPUT -H "Content-type: text/xml" -d @layer.xml  "http://localhost:8080/geoserver/gwc/rest/layers/layer1.xml"

.. note:: the addressed resource ``layer1.xml``, without the ``.xml`` extension, must match the name of the layer in the xml representation.



Add Integrated Layer
++++++++++++++++++++

Given a `poi.xml` file as the following:

.. code-block:: xml

 <GeoServerLayer>
  <id>LayerInfoImpl--570ae188:124761b8d78:-7fd0</id>
  <enabled>true</enabled>
  <name>tiger:poi</name>
  <mimeFormats>
    <string>image/png8</string>
  </mimeFormats>
  <gridSubsets>
    <gridSubset>
      <gridSetName>GoogleCRS84Quad</gridSetName>
      <zoomStart>0</zoomStart>
      <zoomStop>14</zoomStop>
      <minCachedLevel>1</minCachedLevel>
      <maxCachedLevel>9</maxCachedLevel>
    </gridSubset>
  </gridSubsets>
  <metaWidthHeight>
    <int>4</int>
    <int>4</int>
  </metaWidthHeight>
  <gutter>50</gutter>
  <autoCacheStyles>true</autoCacheStyles>
 </GeoServerLayer>

.. code-block:: xml 

 curl -v -u admin:geoserver -XPUT -H "Content-type: text/xml" -d @poi.xml  "http://localhost:8080/geoserver/gwc/rest/layers/tiger:poi.xml"

.. note:: the addressed resource ``tiger:poi.xml``, without the ``poi.xml`` extension, must match the name of the layer in the xml representation,
   as well as the name of an _existing_ GeoServer layer or layer group.

Modify Layer
++++++++++++

Now, make some modifications to the layer definition on the `layer.xml` file:

.. code-block:: xml

 <GeoServerLayer>
  <enabled>true</enabled>
  <name>tiger:poi</name>
  <mimeFormats>
    <string>image/png8</string>
  </mimeFormats>
  <gridSubsets>
    <gridSubset>
      <gridSetName>GoogleCRS84Quad</gridSetName>
      <zoomStart>0</zoomStart>
      <zoomStop>14</zoomStop>
      <minCachedLevel>1</minCachedLevel>
      <maxCachedLevel>9</maxCachedLevel>
    </gridSubset>
    <gridSubset>
      <gridSetName>EPSG:900913</gridSetName>
      <extent>
        <coords>
          <double>-8238959.403861314</double>
          <double>4969300.121476209</double>
          <double>-8237812.689219721</double>
          <double>4971112.167757057</double>
        </coords>
      </extent>
    </gridSubset>
  </gridSubsets>
  <metaWidthHeight>
    <int>4</int>
    <int>4</int>
  </metaWidthHeight>
  <parameterFilters>
    <floatParameterFilter>
      <key>ELEVATION</key>
      <defaultValue>0.0</defaultValue>
      <values>
        <float>0.0</float>
        <float>1.0</float>
        <float>2.0</float>
        <float>3.0</float>
        <float>4.0</float>
      </values>
      <threshold>1.0E-3</threshold>
    </floatParameterFilter>
  </parameterFilters>
  <gutter>50</gutter>
  <autoCacheStyles>true</autoCacheStyles>
 </GeoServerLayer>

And use the HTTP POST method instead:

.. code-block:: xml 

 curl -v -u admin:geoserver -XPOST -H "Content-type: text/xml" -d @poi.xml  "http://localhost:8080/geoserver/gwc/rest/layers/tiger:poi.xml"

The above request adds a parameter filter and a grid subset to the existing ``tiger:poi`` tile layer.
Also, note that the ``<id>`` element is optional, but if present, both the id and the name attribute must match the correspoinding
id and name properties of the matching GeoServer layer or layer group.
 
Delete Layer
++++++++++++

Deleting a GeoWebCache tile layer deletes the layer configuration and **its cache on disk**. No tile images will remain on the
cache directory after deleting a tile layer.

To delete a layer, use the HTTP DELETE method against the layer resource:

.. code-block:: xml 

 curl -v -u admin:geoserver -XDELETE "http://localhost:8080/geoserver/gwc/rest/layers/layer1.xml"

.. note:: in the case that the tile layer being deleted is an integrated ``GeoServerLayer``, only the GeoWebCache layer is being
   deleted, the GeoServer layer or layer group is left untouched. You need to use GeoServer's own REST API to manipulate GeoServer
   resources. Buf if instead you're using the GeoServer REST API to delete a GeoServer layer or layer group that *has* a
   tile layer associated, this last one will get deleted as a side effect even if you didn't explicitly tried to remove the tile layer.
   
