Templates With FreeMarker
-------------------------

MapML templates are written in `Freemarker <http://www.freemarker.org/>`_ , a Java-based template engine. The templates below are feature type specific and will not be applied in multi-layer WMS requests.  See :ref:`tutorial_freemarkertemplate` for general information about FreeMarker implementation in GeoServer.

MapML supports the following template types:

+----------------------------+--------------------------------------------------------------------------------------+
| Template File Name         | Purpose                                                                              |
+============================+======================================================================================+
| ``mapml-preview-head.ftl`` | Used to insert stylesheet links or elements into the MapML HTML preview viewer.      |
+----------------------------+--------------------------------------------------------------------------------------+
| ``mapml-head.ftl``         | Used to insert ``mapml-link`` elements into the MapML map-head section.              |
+----------------------------+--------------------------------------------------------------------------------------+
| ``mapml-feature-head.ftl`` | Used to insert ``map-style`` elements into a MapML feature document.                 |
+----------------------------+--------------------------------------------------------------------------------------+
| ``mapml-feature.ftl``      | Used to rewrite MapML features, with ability to change attributes, styles,           | 
|                            | geometries, and add links                                                            |
+----------------------------+--------------------------------------------------------------------------------------+

GetMap MapML HTML Preview/Layer Preview Head Stylesheet Templating
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
The preview is returned when the format includes ``subtype=mapml``. The preview is an HTML document that includes a ``head`` section with a link to the stylesheet. The default preview viewer is a simple viewer that includes a link to the default stylesheet. 
A template can be created to insert links to whole stylesheet or actual stylesheet elements.  
We can do this by creating a file called ``mapml-preview-head.ftl`` in the GeoServer data directory in the directory for the layer that we wish to append links to.  For example we could create this file under ``workspaces/topp/states_shapefile/states``.  To add stylesheet links and stylesheet elements, we enter the following text inside this new file:

.. code-block:: html

 <!-- Added from the template -->	
 <link rel="stylesheet" href="mystyle.css">
 <style>
  body {
   background-color: linen;
  }
 </style>
 <!-- End of added from the template -->

This would result in a head section that would resemble:

.. code-block:: html

    <head>
      <title>USA Population</title>
      <meta charset='utf-8'>
      <script type="module"  src="http://localhost:8080/geoserver/mapml/viewer/widget/mapml-viewer.js"></script>
      <style>
          html, body { height: 100%; }
          * { margin: 0; padding: 0; }
          mapml-viewer:defined { max-width: 100%; width: 100%; height: 100%; border: none; vertical-align: middle }
          mapml-viewer:not(:defined) > * { display: none; } map-layer { display: none; }
      </style>
      <noscript>
      <style>
          mapml-viewer:not(:defined) > :not(map-layer) { display: initial; }
      </style>
      </noscript>
      <!-- Added from the template -->
      <link rel="stylesheet" href="mystyle.css">
      <style>
          body {
              background-color: linen;
          }
      </style>
      <!-- End of added from the template -->
    </head>

GetMap MapML Head Stylesheet Templating
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
The MapML format includes a map-head element that includes map-link elements to link to other resources, including map style variants.  Additional map-link elements can be added to the map-head element by creating a ``mapml-head.ftl`` template in the GeoServer data directory in the directory for the layer we wish to append map-links to.  For example we could create the ``mapml-head.ftl`` file under ``workspaces/tiger/nyc/poly_landmarks_shapefile/poly_landmarks``: 

.. code-block:: bash

 <!-- Added from the template -->	
 <map-style>.polygon-r1-s1{stroke-opacity:3.0; stroke-dashoffset:4; stroke-width:2.0; fill:#AAAAAA; fill-opacity:3.0; stroke:#DD0000; stroke-linecap:butt}</map-style>
 <map-link href="${serviceLink("${base}","${path}","${kvp}")}" rel="\${rel}" title="templateinsertedstyle"/>
 <!-- End of added from the template -->

This would result in a map-head section that would resemble (note the inserted css styles and map-link):

.. code-block:: html

    <map-head>
      <map-title>Manhattan (NY) landmarks</map-title>
      <map-base href="http://localhost:8080/geoserver/wms"/>
      <map-meta charset="utf-8"/>
      <map-meta content="text/mapml;projection=WGS84" http-equiv="Content-Type"/>
      <map-link href="http://localhost:8080/geoserver/tiger/wms?format_options=mapml-wms-format%3Aimage%2Fpng&amp;request=GetMap&amp;crs=MapML%3AWGS84&amp;service=WMS&amp;bbox=-180.0%2C-90.0%2C180.0%2C90.0&amp;format=text%2Fmapml&amp;layers=poly_landmarks&amp;width=768&amp;styles=grass&amp;version=1.3.0&amp;height=384" rel="style" title="grass"/>
      <map-link href="http://localhost:8080/geoserver/tiger/wms?format_options=mapml-wms-format%3Aimage%2Fpng&amp;request=GetMap&amp;crs=MapML%3AWGS84&amp;service=WMS&amp;bbox=-180.0%2C-90.0%2C180.0%2C90.0&amp;format=text%2Fmapml&amp;layers=poly_landmarks&amp;width=768&amp;styles=restricted&amp;version=1.3.0&amp;height=384" rel="style" title="restricted"/>
      <map-link href="http://localhost:8080/geoserver/tiger/wms?format_options=mapml-wms-format%3Aimage%2Fpng&amp;request=GetMap&amp;crs=MapML%3AWGS84&amp;service=WMS&amp;bbox=-180.0%2C-90.0%2C180.0%2C90.0&amp;format=text%2Fmapml&amp;layers=poly_landmarks&amp;width=768&amp;styles=polygon%2C&amp;version=1.3.0&amp;height=384" rel="self style" title="polygon,"/>
      <map-link href="http://localhost:8080/geoserver/tiger/wms?format_options=mapml-wms-format%3Aimage%2Fpng&amp;request=GetMap&amp;crs=MapML%3AOSMTILE&amp;service=WMS&amp;bbox=-2.0037508342789244E7%2C-2.364438881673656E7%2C2.0037508342789244E7%2C2.364438881673657E7&amp;format=text%2Fmapml&amp;layers=poly_landmarks&amp;width=768&amp;version=1.3.0&amp;height=384" rel="alternate" projection="OSMTILE"/>
      <map-link href="http://localhost:8080/geoserver/tiger/wms?format_options=mapml-wms-format%3Aimage%2Fpng&amp;request=GetMap&amp;crs=MapML%3ACBMTILE&amp;service=WMS&amp;bbox=-8079209.971443829%2C-3626624.322362231%2C8281691.192343056%2C1.233598344760506E7&amp;format=text%2Fmapml&amp;layers=poly_landmarks&amp;width=768&amp;version=1.3.0&amp;height=384" rel="alternate" projection="CBMTILE"/>
      <map-link href="http://localhost:8080/geoserver/tiger/wms?format_options=mapml-wms-format%3Aimage%2Fpng&amp;request=GetMap&amp;crs=MapML%3AAPSTILE&amp;service=WMS&amp;bbox=-1.06373184982574E7%2C-1.06373184982574E7%2C1.46373184982574E7%2C1.46373184982574E7&amp;format=text%2Fmapml&amp;layers=poly_landmarks&amp;width=768&amp;version=1.3.0&amp;height=384" rel="alternate" projection="APSTILE"/>
      <map-link href="http://localhost:8080/geoserver/tiger/wms?format_options=mapml-wms-format%3Aimage%2Fpng&amp;request=GetMap&amp;crs=MapML%3AWGS84&amp;service=WMS&amp;bbox=-180.0%2C-90.0%2C180.0%2C90.0&amp;format=text%2Fmapml&amp;layers=poly_landmarks&amp;width=768&amp;styles=templateinsertedstyle&amp;version=1.3.0&amp;height=384" rel="style" title="templateinsertedstyle"/>
      <map-style>.bbox {display:none} .poly_landmarks-r1-s1{stroke-opacity:1.0; stroke-dashoffset:0; stroke-width:1.0; fill:#B4DFB4; fill-opacity:1.0; stroke:#88B588; stroke-linecap:butt} .poly_landmarks-r2-s1{stroke-opacity:1.0; stroke-dashoffset:0; stroke-width:1.0; fill:#8AA9D1; fill-opacity:1.0; stroke:#436C91; stroke-linecap:butt} .poly_landmarks-r3-s1{stroke-opacity:1.0; stroke-dashoffset:0; stroke-width:1.0; fill:#FDE5A5; fill-opacity:0.75; stroke:#6E6E6E; stroke-linecap:butt} .polygon-r1-s1{stroke-opacity:3.0; stroke-dashoffset:4; stroke-width:2.0; fill:#AAAAAA; fill-opacity:3.0; stroke:#DD0000; stroke-linecap:butt}</map-style>
    </map-head>

GetMap Features Inline Style Class Templating
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
MapML in feature format (when the parameter format_options=mapmlfeatures:true is set) has a map-head element that includes map-style elements where the style classes are defined.  
Within the map-body, map-feature elements include map-geometry with map-coordinates.    

The ``mapml-feature-head.ftl`` is a file that can be used to insert map-style elements with the style class definitions.
This file is placed in the GeoServer data directory in the directory for the layer we wish to append style classes to.  For example we could create the file under ``workspaces/tiger/nyc/poly_landmarks_shapefile/poly_landmarks``.  

The ``mapml-feature-head.ftl`` file would look like::

      <mapml- xmlns="http://www.w3.org/1999/xhtml">
        <map-head>
          <map-style>.desired {stroke-dashoffset:3}</map-style>
        </map-head>
      </mapml->  

This would result in a MapML feature output header that would resemble:

.. code-block:: xml

    <mapml- xmlns="http://www.w3.org/1999/xhtml">
      <map-head>
        <map-title>poi</map-title>
        <map-meta charset="UTF-8"/>
        <map-meta content="text/mapml" http-equiv="Content-Type"/>
        <map-meta name="extent" content="top-left-longitude=-74.011832,top-left-latitude=40.711946,bottom-right-longitude=-74.008573,bottom-right-latitude=40.707547"/>
        <map-meta name="cs" content="gcrs"/>
        <map-meta name="projection" content="MapML:EPSG:4326"/>
        <map-style>.bbox {display:none} .polygon-r1-s1{stroke-opacity:1.0; stroke-dashoffset:0; stroke-width:1.0; fill:#AAAAAA; fill-opacity:1.0; stroke:#000000; stroke-linecap:butt}</map-style>
        <map-style>.desired {stroke-dashoffset:3}</map-style>
      </map-head>
    </mapml->

The ``mapml-feature.ftl`` is a file can be used to insert map-style elements with the style class definitions into the map-head.  Note that this section of the template adds the styles listed but does not remove any existing styles.
It can be used to edit map-property names and values in a manner similar to :ref:`tutorials_getfeatureinfo_geojson`.  Note that this template represents a full replacement of the feature.  If there are attributes that need to be included without change, they need to be referenced in the template.  It also can be used to add style class identifiers to map-feature elements based on the feature identifier or to wrap groupings of map-coordinates with spans that specify the style class based on an index of coordinate order (zero based index that starts at the first coordinate pair of each feature).  
This file is placed in the GeoServer data directory in the directory for the layer we wish to append style classes to.  For example we could create the file under ``workspaces/tiger/poly_landmarks_shapefile/poly_landmarks``.  

An example ``mapml-feature.ftl`` file to modify a point layer would look like::
    
    <mapml- xmlns="http://www.w3.org/1999/xhtml">
      <map-head>
      </map-head>
      <map-body>
        <map-feature>
          <#list attributes as attribute>
            <#if attribute.name == "MAINPAGE">
              <map-properties name="UPDATED ${attribute.name}" value="CHANGED ${attribute.value}"/>
            <#else>
              <map-properties name="${attribute.name}" value="${attribute.value}"/>
            </#if>
          </#list>
          <#list attributes as gattribute>
            <#if gattribute.isGeometry>
              <map-geometry>
                <!-- by default (if unspecified), map-a type attribute <map-a type="text/mapml"...> -->
                <!-- is taken to mean that the link is to this or another MapML map layer, based on the -->
                <!-- value of the <map-a target="_self" ...> "_self" is the default if unspecified -->
                <!-- so, to link to another location in the current map, use href="#zoom,longitude,latitude -->
                <!-- shown below. For further information on how to create links of different behaviours, -->
                <!-- please refer to https://maps4html.org/web-map-doc/docs/other-elements/map-a/#target -->
                <#if attributes.NAME.value == "museam"><map-a href="#16,-74.01046109936,40.70758762626"></#if>
                <map-point>
                  <map-coordinates>
                    <#list gattribute.rawValue.coordinates as coord>${coord.x} ${coord.y}</#list>
                  </map-coordinates>
                </map-point>
                <!-- DO NOT FORGET to close your tags, else look for errors in your log files -->
                <#if attributes.NAME.value == "museam"></map-a></#if>
              </map-geometry>
             </#if>
           </#list>
          </map-feature>
      </map-body>
    </mapml->

This would result in a MapML feature output body that would resemble this fragment::
    
    <mapml-
      xmlns="http://www.w3.org/1999/xhtml">
      <map-head>
        <map-title>poi</map-title>
        <map-meta charset="UTF-8"/>
        <map-meta content="text/mapml" http-equiv="Content-Type"/>
        <map-meta name="cs" content="gcrs"/>
        <map-meta name="projection" content="WGS84"/>
        <map-meta name="extent" content="top-left-longitude=-74.011832,top-left-latitude=40.711946,bottom-right-longitude=-74.008573,bottom-right-latitude=40.707547"/>
        <map-style>.bbox {display:none} .poi-r1-s1{r:88.0; well-known-name:circle; opacity:1.0; fill:#FF0000; fill-opacity:1.0} .poi-r1-s2{r:56.0; well-known-name:circle; opacity:1.0; fill:#FFFFFF; fill-opacity:1.0}</map-style>
      </map-head>
      <map-body>
        <map-feature id="poi.1" class="poi-r1-s1 poi-r1-s2">
          <map-geometry>
            <map-a href="#16,-74.01046109936,40.70758762626">
              <map-point>
                <map-coordinates>-74.01046109936 40.70758762626</map-coordinates>
              </map-point>
            </map-a>
          </map-geometry>
          <map-properties>
            <table
              xmlns="http://www.w3.org/1999/xhtml">
              <thead>
                <tr>
                  <th role="columnheader" scope="col">Property name</th>
                  <th role="columnheader" scope="col">Property value</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <th scope="row">CHANGED MAINPAGE</th>
                  <td itemprop="MAINPAGE">UPDATED pics/22037827-L.jpg</td>
                </tr>
              </tbody>
            </table>
          </map-properties>
        </map-feature>

Note that in addition to tagging the coordinates with a style class, the template also changes the name of the MAINPAGE property to "UPDATED MAINPAGE" and the value to "CHANGED pics/22037827-L.jpg".  

For linestring features the template would look like::

    <mapml- xmlns="http://www.w3.org/1999/xhtml">
      <map-head>
      </map-head>
      <map-body>
        <map-feature>
          <#list attributes as attribute>
            <#if attribute.isGeometry>
              <map-geometry>
                <#if attributes.NAME.value == "Washington Sq W"><map-a href="#16,-73.999559,40.73158"></#if>
                  <map-linestring>
                    <map-coordinates>
                      <#list attribute.rawValue.coordinates as coord> ${coord.x} ${coord.y}</#list>
                    </map-coordinates></map-linestring>
                <#if attributes.NAME.value == "Washington Sq W"></map-a></#if></map-geometry>
            </#if>
          </#list>
        </map-feature>
      </map-body>
    </mapml->

For polygon features the template would look like::

    <mapml- xmlns="http://www.w3.org/1999/xhtml">
      <map-head>
      </map-head>
      <map-body>
        <map-feature>
          <#list attributes as attribute>
            <#if attribute.isGeometry>
              <map-geometry>
                <map-a href="#16,-1,0">
                  <map-polygon>
                    <#assign shell = attribute.rawValue.getExteriorRing()>
                    <map-coordinates>
                      <#list shell.coordinates as coord> ${coord.x} ${coord.y}</#list>
                    </map-coordinates>
                    <#list 0 ..< attribute.rawValue.getNumInteriorRing() as index>
                      <#assign hole = attribute.rawValue.getInteriorRingN(index)><map-coordinates><#list hole.coordinates as coord> ${coord.x} ${coord.y} </#list></map-coordinates></#list>
                  </map-polygon>
                </map-a>
              </map-geometry>
            </#if>
          </#list>
        </map-feature>
      </map-body>
    </mapml- >

For multipoint features the template would look like::

    <mapml- xmlns="http://www.w3.org/1999/xhtml">
      <map-head>
      </map-head>
      <map-body>
        <map-feature>
          <#list attributes as gattribute>
            <#if gattribute.isGeometry>
              <map-geometry>
                <map-a href="#16,-74.01046109936,40.70758762626">
                <map-multipoint>
                  <#list 0 ..< gattribute.rawValue.getNumGeometries() as index>
                    <#assign point = gattribute.rawValue.getGeometryN(index)>
                        <map-coordinates><#list point.coordinates as coord>
                          ${coord.x} ${coord.y}</#list></map-coordinates>
                  </#list>
                </map-multipoint>
                </map-a>
              </map-geometry>
             </#if>
           </#list>
          </map-feature>
        </map-body>
        </mapml->

For multiline features the template would like::

    <mapml- xmlns="http://www.w3.org/1999/xhtml">
      <map-head>
      </map-head>
      <map-body>
      <map-feature>
        <#list attributes as attribute>
          <#if attribute.isGeometry>
            <map-geometry>
              <map-a href="#16,-0.0042,-0.0006">
              <map-multilinestring>
                <#list 0 ..< attribute.rawValue.getNumGeometries() as index>
                  <#assign line = attribute.rawValue.getGeometryN(index)>
                  <map-coordinates><#list line.coordinates as coord> ${coord.x} ${coord.y}</#list></map-coordinates>
                </#list>
              </map-multilinestring>
              </map-a>                                    
            </map-geometry>
          </#if>
        </#list>
      </map-feature>
      </map-body>
      </mapml->

For multipolygon features the template would like::

    <mapml- xmlns="http://www.w3.org/1999/xhtml">
        <map-head>
        </map-head>
        <map-body>
          <map-feature>
          <#if attributes.LAND.value == "72.0">
            <#list attributes as attribute>
              <#if attribute.isGeometry>
                <map-geometry>
                  <map-a href="#16,-0.0042,-0.0006">
                  <map-multipolygon>
                <#list 0 ..< attribute.rawValue.getNumGeometries() as index>
                  <#assign polygon = attribute.rawValue.getGeometryN(index)>
                <map-polygon>
                  <#assign shell = polygon.getExteriorRing()>
                  <map-coordinates><#list shell.coordinates as coord> ${coord.x} ${coord.y}</#list></map-coordinates>
                  <#list 0 ..< polygon.getNumInteriorRing() as index>
                  <#assign hole = polygon.getInteriorRingN(index)>
                  <map-coordinates><#list hole.coordinates as coord> ${coord.x} ${coord.y}</#list></map-coordinates></#list>
                </map-polygon>
                  </#list>
                </map-multipolygon>
                </map-a>
                </map-geometry>
              </#if>
            </#list>
          <#else>
            <#list attributes as attribute>
              <#if attribute.isGeometry>
                <map-geometry>
                  <map-multipolygon>
                <#list 0 ..< attribute.rawValue.getNumGeometries() as index>
                  <#assign polygon = attribute.rawValue.getGeometryN(index)>
                <map-polygon>
                <#assign shell = polygon.getExteriorRing()>
                <map-coordinates><#list shell.coordinates as coord> ${coord.x} ${coord.y}</#list></map-coordinates>
                <#list 0 ..< polygon.getNumInteriorRing() as index>
                  <#assign hole = polygon.getInteriorRingN(index)><map-coordinates>
                  <#list hole.coordinates as coord> ${coord.x} ${coord.y}</#list></map-coordinates>
                </#list>
                </map-polygon>
                  </#list>
                </map-multipolygon>
                </map-geometry>
              </#if>
            </#list>
          </#if>
          </map-feature>
        </map-body>
        </mapml->

Templates can also be used to create MapML GeometryCollections that consist of multiple geometry types. For example, a template that creates a GeometryCollection that contains points and linestring representations of the NYC TIGER POI sample data would look like::

    <mapml- xmlns="http://www.w3.org/1999/xhtml">
      <map-head>
      </map-head>
      <map-body>
      <map-feature>
        <#list attributes as attribute>
          <#if attribute.isGeometry>
            <map-geometry>
              <map-a href="#16,-1,0">
              <map-geometrycollection>
                <map-linestring>
                  <map-coordinates><#list attribute.rawValue.coordinates as coord> ${coord.x} ${coord.y}</#list></map-coordinates>
                </map-linestring>
                <map-point>
                  <map-coordinates><#list attribute.rawValue.coordinates as coord> ${coord.x} ${coord.y}</#list></map-coordinates>
                </map-point>
              </map-geometrycollection>
              </map-a>
            </map-geometry>
          </#if>
        </#list>
      </map-feature>
      </map-body>
    </mapml->

This would result in a MapML feature output body that would resemble::

    <mapml-
      xmlns="http://www.w3.org/1999/xhtml">
      <map-head>
        <map-title>poi</map-title>
        <map-meta charset="UTF-8"/>
        <map-meta content="text/mapml" http-equiv="Content-Type"/>
        <map-meta name="cs" content="gcrs"/>
        <map-meta name="projection" content="WGS84"/>
        <map-meta name="extent" content="top-left-longitude=-74.011832,top-left-latitude=40.711946,bottom-right-longitude=-74.008573,bottom-right-latitude=40.707547"/>
        <map-style>.bbox {display:none} .poi-r1-s1{r:88.0; well-known-name:circle; opacity:1.0; fill:#FF0000; fill-opacity:1.0} .poi-r1-s2{r:56.0; well-known-name:circle; opacity:1.0; fill:#FFFFFF; fill-opacity:1.0}</map-style>
      </map-head>
      <map-body>
        <map-feature id="poi.4" class="poi-r1-s1 poi-r1-s2">
          <map-geometry>
            <map-a href="#16,-1,0">
              <map-geometrycollection>
                <map-linestring>
                  <map-coordinates> -74.00857344353 40.71194564907</map-coordinates>
                </map-linestring>
                <map-point>
                  <map-coordinates> -74.00857344353 40.71194564907</map-coordinates>
                </map-point>
              </map-geometrycollection>
            </map-a>
          </map-geometry>
          <map-properties>
            <table
              xmlns="http://www.w3.org/1999/xhtml">
              <thead>
                <tr>
                  <th role="columnheader" scope="col">Property name</th>
                  <th role="columnheader" scope="col">Property value</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <th scope="row">NAME</th>
                  <td itemprop="NAME">lox</td>
                </tr>
                <tr>
                  <th scope="row">THUMBNAIL</th>
                  <td itemprop="THUMBNAIL">pics/22037884-Ti.jpg</td>
                </tr>
                <tr>
                  <th scope="row">MAINPAGE</th>
                  <td itemprop="MAINPAGE">pics/22037884-L.jpg</td>
                </tr>
              </tbody>
            </table>
          </map-properties>
        </map-feature>
      </map-body>
    </mapml->