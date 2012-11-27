.. _rest_examples_curl:

cURL
====

The examples in this section use `cURL <http://curl.haxx.se/>`_, a command line tool for executing HTTP requests and transferring files, to generate requests to GeoServer’s REST interface. Although the examples are based on cURL, they could be adapted for any HTTP-capable tool or library.

.. todo::

   The following extra sections could be added for completeness:

   * Deleting a workspace/store/featuretype/style/layergroup
   * Renaming a workspace/store/featuretype/style/layergroup


Adding a new workspace
----------------------

The following creates a new workspace named "acme" with a POST request:

.. note:: Each code block below contains a single command that may be extended over multiple lines.

.. code-block:: console

   curl -v -u admin:geoserver -XPOST -H "Content-type: text/xml" 
     -d "<workspace><name>acme</name></workspace>" 
     http://localhost:8080/geoserver/rest/workspaces

If executed correctly, the response should contain the following::
 
  < HTTP/1.1 201 Created
  ...
  < Location: http://localhost:8080/geoserver/rest/workspaces/acme

Note the ``Location`` response header, which specifies the location (URI) of the newly created workspace.

The workspace information can be retrieved as XML with a GET request:

.. code-block:: console

   curl -v -u admin:geoserver -XGET -H "Accept: text/xml" 
     http://localhost:8080/geoserver/rest/workspaces/acme

The response should look like this:

.. code-block:: xml

   <workspace>
     <name>acme</name>
     <dataStores>
       <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" 
        href="http://localhost:8080/geoserver/rest/workspaces/acme/datastores.xml" 
        type="application/xml"/>
     </dataStores>
     <coverageStores>
       <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" 
        href="http://localhost:8080/geoserver/rest/workspaces/acme/coveragestores.xml" 
        type="application/xml"/>
     </coverageStores>
     <wmsStores>
       <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" 
        href="http://localhost:8080/geoserver/rest/workspaces/acme/wmsstores.xml" 
        type="application/xml"/>
     </wmsStores>
   </workspace>

This shows that the workspace can contain "``dataStores``" (for :ref:`vector data <data_vector>`), "``coverageStores``" (for :ref:`raster data <data_raster>`), and "``wmsStores``" (for :ref:`cascaded WMS servers <data_external_wms>`).

.. note:: 

   The ``Accept`` header is optional. The following request omits the ``Accept`` header, but will return the same response as above.

   .. code-block:: console

      curl -v -u admin:geoserver -XGET http://localhost:8080/geoserver/rest/workspaces/acme.xml


Uploading a shapefile
---------------------

In this example a new store will be created by uploading a shapefile.

The following request uploads a zipped shapefile named ``roads.zip`` and creates a new store named ``roads``.

.. note:: Each code block below contains a single command that may be extended over multiple lines.

.. code-block:: console

   curl -v -u admin:geoserver -XPUT -H "Content-type: application/zip" 
     --data-binary @roads.zip 
     http://localhost:8080/geoserver/rest/workspaces/acme/datastores/roads/file.shp

The ``roads`` identifier in the URI refers to the name of the store to be created. To create a store named ``somethingelse``, the URI would be  ``http://localhost:8080/geoserver/rest/workspaces/acme/datastores/somethingelse/file.shp``

If executed correctly, the response should contain the following::
 
  < HTTP/1.1 201 Created

The store information can be retrieved as XML with a GET request:

.. code-block:: console

   curl -v -u admin:geoserver -XGET
     http://localhost:8080/geoserver/rest/workspaces/acme/datastores/roads.xml

The response should look like this:

.. code-block:: xml

   <dataStore>
     <name>roads</name>
     <type>Shapefile</type>
     <enabled>true</enabled>
     <workspace>
       <name>acme</name>
       <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" 
        href="http://localhost:8080/geoserver/rest/workspaces/acme.xml" type="application/xml"/>
     </workspace>
     <connectionParameters>
       <entry key="url">file:/C:/path/to/data_dir/data/acme/roads/</entry>
       <entry key="namespace">http://acme</entry>
     </connectionParameters>
     <__default>false</__default>
     <featureTypes>
       <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" 
        href="http://localhost:8080/geoserver/rest/workspaces/acme/datastores/roads/featuretypes.xml" 
        type="application/xml"/>
     </featureTypes>
   </dataStore>

By default when a shapefile is uploaded, a feature type is automatically created. The feature type information can be retrieved as XML with a GET request:

.. code-block:: console

   curl -v -u admin:geoserver -XGET 
     http://localhost:8080/geoserver/rest/workspaces/acme/datastores/roads/featuretypes/roads.xml

If executed correctly, the response will be:

.. code-block:: xml

   <featureType>
     <name>roads</name>
     <nativeName>roads</nativeName>
     <namespace>
       <name>acme</name>
       <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" 
        href="http://localhost:8080/geoserver/rest/namespaces/acme.xml" type="application/xml"/>
     </namespace>
     ...
   </featureType>

The remainder of the response consists of layer metadata and configuration information.


Adding an existing shapefile
----------------------------

In the previous example a shapefile was uploaded directly to GeoServer by sending a zip file in the body of a PUT request. This example shows how to publish a shapefile that already exists on the server.

Consider a directory on the server ``/data/shapefiles/rivers`` that contains the shapefile ``rivers.shp``. The following adds a new store for the shapefile:

.. note:: Each code block below contains a single command that may be extended over multiple lines.

.. code-block:: console

   curl -v -u admin:geoserver -XPUT -H "Content-type: text/plain" 
     -d "file:///data/shapefiles/rivers/rivers.shp" 
     http://localhost:8080/geoserver/rest/workspaces/acme/datastores/rivers/external.shp

The ``external.shp`` part of the request URI indicates that the file is coming from outside the catalog.

If executed correctly, the response should contain the following::
 
  < HTTP/1.1 201 Created

The shapefile will be added to the existing store and published as a layer.

To verify the contents of the store, execute a GET request. Since the XML response only provides details about the store itself without showing its contents, execute a GET request for HTML:

.. code-block:: console

   curl -v -u admin:geoserver -XGET 
     http://localhost:8080/geoserver/rest/workspaces/acme/datastores/rivers.html


Adding a directory of existing shapefiles
-----------------------------------------

This example shows how to load and create a store that contains a number of shapefiles, all with a single operation. This example is very similar to the example above of adding a single shapefile.

Consider a directory on the server ``/data/shapefiles`` that contains multiple shapefiles. The following adds a new store for the directory.

.. note:: Each code block below contains a single command that may be extended over multiple lines.

.. code-block:: console

   curl -v -u admin:geoserver -XPUT -H "Content-type: text/plain" 
     -d "file:///data/shapefiles/" 
     "http://localhost:8080/geoserver/rest/workspaces/acme/datastores/shapefiles/external.shp?configure=all"

Note the ``configure=all`` query string parameter, which sets each shapefile in the directory to be loaded and published.

If executed correctly, the response should contain the following::
 
  < HTTP/1.1 201 Created

To verify the contents of the store, execute a GET request. Since the XML response only provides details about the store itself without showing its contents, execute a GET request for HTML:

.. code-block:: console

   curl -v -u admin:geoserver -XGET 
   http://localhost:8080/geoserver/rest/workspaces/acme/datastores/shapefiles.html


Creating a layer style
----------------------

This example will create a new style on the server and populate it the contents of a local SLD file.

The following creates a new style named ``roads_style``:

.. note:: Each code block below contains a single command that may be extended over multiple lines.

.. code-block:: console

   curl -v -u admin:geoserver -XPOST -H "Content-type: text/xml" 
     -d "<style><name>roads_style</name><filename>roads.sld</filename></style>" 
     http://localhost:8080/geoserver/rest/styles

If executed correctly, the response should contain the following::

  < HTTP/1.1 201 Created

This request uploads a file called :file:`roads.sld` file and populates the ``roads_style`` with its contents:

.. code-block:: console

   curl -v -u admin:geoserver -XPUT -H "Content-type: application/vnd.ogc.sld+xml" 
     -d @roads.sld http://localhost:8080/geoserver/rest/styles/roads_style

If executed correctly, the response should contain the following::

  < HTTP/1.1 200 OK

The SLD itself can be downloaded through a a GET request:

.. code-block:: console

   curl -v -u admin:geoserver -XGET
     http://localhost:8080/geoserver/rest/styles/roads_style.sld


Changing a layer style
----------------------

This example will alter a layer style. Prior to making any changes, it is helpful to view the existing configuration for a given layer. 

.. note:: Each code block below contains a single command that may be extended over multiple lines.

The following retrieves the "acme:roads" layer information as XML:

.. code-block:: console

   curl -v -u admin:geoserver -XGET "http://localhost:8080/geoserver/rest/layers/acme:roads.xml"

The response in this case would be: 

.. code-block:: xml

   <layer>
     <name>roads</name>
     <type>VECTOR</type>
     <defaultStyle>
       <name>line</name>
       <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" 
        href="http://localhost:8080/geoserver/rest/styles/line.xml" type="application/xml"/>
     </defaultStyle>
     <resource class="featureType">
       <name>roads</name>
       <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" 
        href="http://localhost:8080/geoserver/rest/workpaces/acme/datastores/roads/featuretypes/roads.xml" 
        type="application/xml"/>
     </resource>
     <enabled>true</enabled>
     <attribution>
       <logoWidth>0</logoWidth>
       <logoHeight>0</logoHeight>
     </attribution>
   </layer>

When the layer is created, GeoServer assigns a default style to the layer that matches the geometry of the layer. In this case a style named ``line`` is assigned to the layer. This style can viewed with a WMS request::

  http://localhost:8080/geoserver/wms/reflect?layers=acme:roads

In this next example a new style will be created called ``roads_style`` and assigned to the "acme:roads" layer:

.. code-block:: console

   curl -v -u admin:geoserver -XPUT -H "Content-type: text/xml" 
     -d "<layer><defaultStyle><name>roads_style</name></defaultStyle></layer>" 
     http://localhost:8080/geoserver/rest/layers/acme:roads

If executed correctly, the response should contain the following::

  < HTTP/1.1 200 OK

The new style can be viewed with the same WMS request as above::

  http://localhost:8080/geoserver/wms/reflect?layers=acme:roads

.. todo:: The WMS request above results in an "Internal error featureType: acme:roads does not have a properly configured datastore"  Tested on 2.2.2.


Adding a PostGIS database
-------------------------

In this example a PostGIS database named ``nyc`` will be added as a new store. This section assumes that a PostGIS database named ``nyc`` is present on the local system and is accessible by the user ``bob``.

Create a new text file and add the following content to it. This will represent the new store. Save the file as :file:`nycDataStore.xml`.

.. code-block:: xml

   <dataStore> 
     <name>nyc</name>
     <connectionParameters>
       <host>localhost</host>
       <port>5432</port>
       <database>nyc</database> 
       <user>bob</user>
       <dbtype>postgis</dbtype>
     </connectionParameters>
   </dataStore> 

The following will add the new PostGIS store to the GeoServer catalog:

.. note:: Each code block below contains a single command that may be extended over multiple lines.

.. code-block:: console

   curl -v -u admin:geoserver -XPOST -T nycDataStore.xml -H "Content-type: text/xml" 
     http://localhost:8080/geoserver/rest/workspaces/acme/datastores

If executed correctly, the response should contain the following::

  < HTTP/1.1 200 OK

The store information can be retrieved as XML with a GET request:

.. code-block:: console

   curl -v -u admin:geoserver -XGET http://localhost:8080/geoserver/rest/workspaces/acme/datastores/nyc.xml

The response should look like the following:

.. code-block:: xml

   <dataStore>
     <name>nyc</name>
     <type>PostGIS</type>
     <enabled>true</enabled>
     <workspace>
       <name>acme</name>
       <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" 
        href="http://localhost:8080/geoserver/rest/workspaces/acme.xml" type="application/xml"/>
     </workspace>
     <connectionParameters>
       <entry key="port">5432</entry>
       <entry key="dbtype">postgis</entry>
       <entry key="host">localhost</entry>
       <entry key="user">bob</entry>
       <entry key="database">nyc</entry>
       <entry key="namespace">http://acme</entry>
     </connectionParameters>
     <__default>false</__default>
     <featureTypes>
       <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" 
        href="http://localhost:8080/geoserver/rest/workspaces/acme/datastores/nyc/featuretypes.xml" 
        type="application/xml"/>
     </featureTypes>
   </dataStore>

Adding a PostGIS table
----------------------

In this example a table from the PostGIS database created in the previous example will be added as a featuretypes. This example assumes the table has already been created.

The following adds the table ``buildings`` as a new feature type:

.. note:: Each code block below contains a single command that may be extended over multiple lines.

.. todo:: This didn't work. (500)

.. code-block:: console

   curl -v -u admin:geoserver -XPOST -H "Content-type: text/xml" 
     -d "<featureType><name>buildings</name></featureType>" 
     http://localhost:8080/geoserver/rest/workspaces/acme/datastores/nyc/featuretypes

The featuretype information can be retrieved as XML with a GET request:

.. code-block:: console

   curl -v -u admin:geoserver -XGET 
     http://localhost:8080/geoserver/rest/workspaces/acme/datastores/nyc/featuretypes/buildings.xml

This layer can viewed with a WMS GetMap request::

  http://localhost:8080/geoserver/wms/reflect?layers=acme:buildings


Creating a PostGIS table
------------------------

In the previous example, a new feature type was added based on a PostGIS table that already existed in the database. The following example will not only create a new feature type in GeoServer, but will also create the PostGIS table itself.

Create a new text file and add the following content to it. This will represent the definition of the new feature type and table. Save the file as :file:`annotations.xml`.

.. code-block:: xml

   <featureType>
     <name>annotations</name>
     <nativeName>annotations</nativeName>
     <title>Annotations</title>
     <srs>EPSG:4326</srs>
     <attributes>
       <attribute>
         <name>the_geom</name>
         <binding>com.vividsolutions.jts.geom.Point</binding>
       </attribute>
       <attribute>
         <name>description</name>
         <binding>java.lang.String</binding>
       </attribute>
       <attribute>
         <name>timestamp</name>
         <binding>java.util.Date</binding>
       </attribute>
     </attributes>
   </featureType>
    
This request will perform the feature type creation and add the new table:

.. note:: Each code block below contains a single command that may be extended over multiple lines.

.. code-block:: console

   curl -v -u admin:geoserver -XPOST -T annotations.xml -H "Content-type: text/xml" 
     http://localhost:8080/geoserver/rest/workspaces/acme/datastores/nyc/featuretypes
    
The result is a new, empty table named "annotations" in the "nyc" database, fully configured as a feature type.

The featuretype information can be retrieved as XML with a GET request:

.. code-block:: console

   curl -v -u admin:geoserver -XGET 
     http://localhost:8080/geoserver/rest/workspaces/acme/datastores/nyc/featuretypes/annotations.xml


Creating a layer group
----------------------

In this example a layer group will be created, based on layers that already exist on the server.

Create a new text file and add the following content to it. This file will represent the definition of the new layer group. Save the file as :file:`nycLayerGroup.xml`.

.. code-block:: xml

   <layerGroup>
     <name>nyc</name>
     <layers>
       <layer>roads</layer>
       <layer>parks</layer>
       <layer>buildings</layer>
     </layers>
     <styles>
       <style>roads_style</style>
       <style>polygon</style>
       <style>polygon</style>
     </styles>
   </layerGroup>


The following request creates the new layer group:

.. note:: Each code block below contains a single command that may be extended over multiple lines.

.. code-block:: console

   curl -v -u admin:geoserver -XPOST -d @nycLayerGroup.xml -H "Content-type: text/xml" 
     http://localhost:8080/geoserver/rest/layergroups

.. note:: The argument ``-d@filename.xml`` in this example is used to send a file as the body of an HTTP request with a POST method. The argument ``-T filename.xml`` used in the previous example was used to send a file as the body of an HTTP request with a PUT method.

This layer group can be viewed with a WMS GetMap request::

  http://localhost:8080/geoserver/wms/reflect?layers=nyc


