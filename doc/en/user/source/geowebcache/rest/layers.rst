.. _gwc_rest_layers:

Managing Layers
===============

The GeoWebCache REST API provides a RESTful interface through which users can add, modify, or remove cached layers.

.. note:: JSON is not recommended for managing layers as the JSON library has a number of issues with multi-valued properties such as "parameterFilters".

Layer list
----------

URL: ``/gwc/rest/layers.xml``

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

The following example will request a full list of layers:

.. code-block:: xml

   curl -u admin:geoserver "http://localhost:8080/geoserver/gwc/rest/layers"

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

URL: ``/gwc/rest/layers/<layer>.xml``

.. note:: JSON is not recommended for managing layers as the JSON library has a number of issues with multi-valued properties such as "parameterFilters".

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
   * - GET
     - Return the XML representation of the layer
     - 200
     - XML
   * - POST
     - Modify the definition/configuration of the layer
     - 200
     - XML
   * - PUT
     - Add a new layer
     - 200
     - XML
   * - DELETE
     - Delete the layer
     - 200
     -

.. note:: There are two different representations for cached layers, depending on whether the tile layer is created from the GeoServer WMS layer or layer group (``GeoServerLayer``), or is configured in ``geowebcache.xml`` as a regular GWC layer (``wmsLayer``). A GeoServer layer is referred to as a  ``GeoServerLayer`` and contains no image data source information such as origin WMS URL. 

**Representations**:

* GeoWebCache (``wmsLayer``) :download:`XML minimal <representations/wmslayer_minimal.xml.txt>`
* GeoWebCache (``wmsLayer``) :download:`XML <representations/wmslayer.xml.txt>`
* GeoServer (``GeoServerLayer``) :download:`XML minimal <representations/geoserverlayer_minimal.xml.txt>`
* GeoServer (``GeoServerLayer``) :download:`XML <representations/geoserverlayer.xml.txt>`


The examples below use the `cURL <http://curl.haxx.se/>`_ tool, though the examples apply to any HTTP-capable tool or library.

Adding a GeoWebCache layer
~~~~~~~~~~~~~~~~~~~~~~~~~~

The following example will add a new layer to GeoWebCache:

.. code-block:: console 

   curl -v -u admin:geoserver -XPUT -H "Content-type: text/xml" -d @layer.xml  "http://localhost:8080/geoserver/gwc/rest/layers/newlayer.xml"

The :file:`layer.xml` file is defined as the following:

.. code-block:: xml

   <wmsLayer>
     <name>newlayer</name>
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

.. note:: The addressed resource (``newlayer`` in this example) must match the name of the layer in the XML representation.

Adding a GeoServer layer
~~~~~~~~~~~~~~~~~~~~~~~~

The following example will add a new layer to both GeoServer and GeoWebCache:

.. code-block:: console

   curl -v -u admin:geoserver -XPUT -H "Content-type: text/xml" -d @poi.xml  "http://localhost:8080/geoserver/gwc/rest/layers/tiger:poi.xml"

The :file:`poi.xml` file is defined as the following:

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

.. note:: The addressed resource ( ``tiger:poi`` in this example) must match the name of the layer in the XML representation, as well as the name of an *existing* GeoServer layer or layer group.

Modifying a layer
~~~~~~~~~~~~~~~~~

This example modifies the layer definition via the :file:`layer.xml` file.  The request adds a parameter filter and a grid subset to the existing ``tiger:poi`` tile layer:

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

Instead of PUT, use the HTTP POST method instead:

.. code-block:: console

   curl -v -u admin:geoserver -XPOST -H "Content-type: text/xml" -d @poi.xml  "http://localhost:8080/geoserver/gwc/rest/layers/tiger:poi.xml"


Deleting a layer
~~~~~~~~~~~~~~~~

Deleting a GeoWebCache tile layer deletes the layer configuration *as well as the layer's disk cache*. No tile images will remain in the cache directory after deleting a tile layer.

To delete a layer, use the HTTP DELETE method against the layer resource:

.. code-block:: console

   curl -v -u admin:geoserver -XDELETE "http://localhost:8080/geoserver/gwc/rest/layers/newlayer.xml"

.. note::

   If trying to delete a tile layer that is an integrated ``GeoServerLayer``, only the GeoWebCache layer definition will be deleted; the GeoServer definition is left untouched. To delete a layer in GeoServer, use the GeoServer :ref:`rest` to manipulate GeoServer resources. 

   On the other hand, deleting a GeoServer layer via the GeoServer REST API *will* automatically delete the associated tile layer.
