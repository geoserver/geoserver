.. _rest_examples_curl:

cURL
====

The examples in this section use `cURL <http://curl.haxx.se/>`_, a command line tool for executing HTTP requests and transferring files, to generate requests to GeoServer's REST interface. Although the examples are based on cURL, they could be adapted for any HTTP-capable tool or library.
Please be aware, that cURL acts not entirely the same as a web-browser. In contrast to Mozilla Firefox or Google Chrome cURL will not escape special characters in your request-string automatically. To make sure, that your requests can be processed correctly, make sure, that characters like paranthesis, commas and the like are escaped before sending them via cURL.
If you use libcurl in PHP 5.5 or newer you can prepare the url-string using the function curl_escape. In older versions of PHP hmlspecialchars should do the job also.

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

Note that if you want to upload the style in a workspace (ie, not making it a global style),
and then assign this style to a layer in that workspace, you need first to create the style in the given workspace::

  curl -u admin:geoserver -XPOST -H 'Content-type: text/xml' \
    -d '<style><name>roads_style</name><filename>roads.sld</filename></style>' 
    http://localhost:8080/geoserver/rest/workspaces/acme/styles

Upload the file within the workspace::

  curl -u admin:geoserver -XPUT -H 'Content-type: application/vnd.ogc.sld+xml' \
    -d @roads.sld http://localhost:8080/geoserver/rest/workspaces/acme/styles/roads_style

And finally apply that style to the layer. Note the use of the ``<workspace>`` tag in the XML::

  curl -u admin:geoserver -XPUT -H 'Content-type: text/xml' \
    -d '<layer><defaultStyle><name>roads_style</name><workspace>acme</workspace></defaultStyle></layer>' \
    http://localhost:8080/geoserver/rest/layers/acme:roads

.. todo:: The WMS request above results in an "Internal error featureType: acme:roads does not have a properly configured datastore"  Tested on 2.2.2.


Creating a layer style (SLD package)
------------------------------------

This example will create a new style on the server and populate it the contents of a local SLD file and related images provided in a SLD package. A SLD package is a zip file containing the SLD style and related image files used in the SLD.

The following creates a new style named ``roads_style``.

.. note:: Each code block below contains a single command that may be extended over multiple lines.

.. code-block:: console

   curl -u admin:geoserver -XPOST -H "Content-type: application/zip" 
     --data-binary @roads_style.zip 
     http://localhost:8080/geoserver/rest/styles 
 
If executed correctly, the response should contain the following::

  < HTTP/1.1 201 Created

The SLD itself can be downloaded through a a GET request:

.. code-block:: console

   curl -v -u admin:geoserver -XGET
     http://localhost:8080/geoserver/rest/styles/roads_style.sld


Changing a layer style (SLD package)
------------------------------------

This example will alter a layer style using a SLD package. A SLD package is a zip file containing the SLD style and related image files used in the SLD.

In the previous example, we created a new style ``roads_style``, we can update the SLD package styling and apply the new changes to the style.

.. note:: Each code block below contains a single command that may be extended over multiple lines.

.. code-block:: console

    curl -u admin:geoserver -XPUT -H "Content-type: application/zip" 
      --data-binary @roads_style.zip 
      http://localhost:8080/geoserver/rest/workspaces/w1/styles/roads_style.zip


If executed correctly, the response should contain the following::

  < HTTP/1.1 200 OK


The SLD itself can be downloaded through a a GET request:

.. code-block:: console

   curl -v -u admin:geoserver -XGET
     http://localhost:8080/geoserver/rest/styles/roads_style.sld
     
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
       <passwd>postgres</passwd>
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


Retrieving component versions
-----------------------------

This example shows how to retrieve the versions of the main components: GeoServer, GeoTools, and GeoWebCache:

.. note:: The code block below contains a single command that is extended over multiple lines.

.. code-block:: console

   curl -v -u admin:geoserver -XGET -H "Accept: text/xml" 
     http://localhost:8080/geoserver/rest/about/version.xml

The response will look something like this:

.. code-block:: xml

  <about>
   <resource name="GeoServer">
    <Build-Timestamp>11-Dec-2012 17:55</Build-Timestamp>
    <Git-Revision>e66f8da85521f73d0fd00b71331069a5f49f7865</Git-Revision>
    <Version>2.3-SNAPSHOT</Version>
   </resource>
   <resource name="GeoTools">
    <Build-Timestamp>04-Dec-2012 02:31</Build-Timestamp>
    <Git-Revision>380a2b8545ee9221f1f2d38a4f10ef77a23bccae</Git-Revision>
    <Version>9-SNAPSHOT</Version>
   </resource>
   <resource name="GeoWebCache">
    <Git-Revision>2a534f91f6b99e5120a9eaa5db62df771dd01688</Git-Revision>
    <Version>1.3-SNAPSHOT</Version>
   </resource>
  </about>

Retrieving manifests
--------------------

This collection of examples shows how to retrieve the full manifest and subsets of the manifest as known to the ClassLoader.


.. note:: The code block below contains a single command that is extended over multiple lines.

.. code-block:: console

   curl -v -u admin:geoserver -XGET -H "Accept: text/xml"
     http://localhost:8080/geoserver/rest/about/manifest.xml

The result will be a very long list of manifest information. While this can be useful, it is often desirable to filter this list.

Filtering over resource name
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

It is possible to filter over resource names using regular expressions. This example will retrieve only resources where the ``name`` attribute matches ``gwc-.*``: 

.. note:: The code block below contains a single command that is extended over multiple lines.

.. code-block:: console

   curl -v -u admin:geoserver -XGET -H "Accept: text/xml"
     http://localhost:8080/geoserver/rest/about/manifest.xml?manifest=gwc-.*

The result will look something like this (edited for brevity):

.. code-block:: xml

  <about>
    <resource name="gwc-2.3.0">
      ...
    </resource>
    <resource name="gwc-core-1.4.0">
      ...
    </resource>
    <resource name="gwc-diskquota-core-1.4.0">
      ...
    </resource>
    <resource name="gwc-diskquota-jdbc-1.4.0">
      ...
    </resource>
    <resource name="gwc-georss-1.4.0">
      ...
    </resource>
    <resource name="gwc-gmaps-1.4.0">
      ...
    </resource>
    <resource name="gwc-kml-1.4.0">
      ...
    </resource>
    <resource name="gwc-rest-1.4.0">
      ...
    </resource>
    <resource name="gwc-tms-1.4.0">
      ...
    </resource>
    <resource name="gwc-ve-1.4.0">
      ...
    </resource>
    <resource name="gwc-wms-1.4.0">
      ...
    </resource>
    <resource name="gwc-wmts-1.4.0">
      ...
    </resource>
  </about>

Filtering over resource properties
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Filtering is also available over resulting resource properties. This example will retrieve only resources with a property equal to ``GeoServerModule``.

.. note:: The code blocks below contain a single command that is extended over multiple lines.

.. code-block:: console

  curl -u admin:geoserver -XGET -H "Accept: text/xml"
    http://localhost:8080/geoserver/rest/about/manifest.xml?key=GeoServerModule

The result will look something like this (edited for brevity):

.. code-block:: xml

  <about>
   <resource name="control-flow-2.3.0">
    <GeoServerModule>extension</GeoServerModule>
    ...
   </resource>
   ...
   <resource name="wms-2.3.0">
    <GeoServerModule>core</GeoServerModule>
    ...
   </resource>
  </about>

It is also possible to filter against both property and value. To retrieve only resources where a property named ``GeoServerModule`` has a value equal to ``extension``, append the above request with ``&value=extension``:

.. code-block:: console

   curl -u admin:geoserver -XGET -H "Accept: text/xml"
     http://localhost:8080/geoserver/rest/about/manifest.xml?key=GeoServerModule&value=extension

Uploading and modifying a image mosaic
--------------------------------------

The following command uploads a zip file containing the definition of a mosaic (along with at least one granule of the mosaic to initialize the resolutions, overviews and the like) and will configure all the coverages in it as new layers.


.. note:: The code blocks below contain a single command that is extended over multiple lines.

.. code-block:: console

   curl -u admin:geoserver -XPUT -H "Content-type:application/zip" --data-binary @polyphemus.zip
      http://localhost:8080/geoserver/rest/workspaces/topp/coveragestores/polyphemus/file.imagemosaic

The following instead instructs the mosaic to harvest (or re-harvest) a single file into the mosaic, collecting its properties and updating the mosaic index:

.. code-block:: console

   curl -v -u admin:geoserver -XPOST -H "Content-type: text/plain" -d "file:///path/to/the/file/polyphemus_20130302.nc" 
      "http://localhost:8080/geoserver/rest/workspaces/topp/coveragestores/poly-incremental/external.imagemosaic"

Harvesting can also be directed towards a whole directory, as follows:

.. code-block:: console

   curl -v -u admin:geoserver -XPOST -H "Content-type: text/plain" -d "file:///path/to/the/mosaic/folder" 
       "http://localhost:8080/geoserver/rest/workspaces/topp/coveragestores/poly-incremental/external.imagemosaic"

The image mosaic index structure can be retrieved using something like:

.. code-block:: console

   curl -v -u admin:geoserver -XGET "http://localhost:8080/geoserver/rest/workspaces/topp/coveragestores/polyphemus-v1/coverages/NO2/index.xml"

which will result in the following:

.. code-block:: json

       <Schema>
      <attributes>
        <Attribute>
          <name>the_geom</name>
          <minOccurs>0</minOccurs>
          <maxOccurs>1</maxOccurs>
          <nillable>true</nillable>
          <binding>com.vividsolutions.jts.geom.Polygon</binding>
        </Attribute>
        <Attribute>
          <name>location</name>
          <minOccurs>0</minOccurs>
          <maxOccurs>1</maxOccurs>
          <nillable>true</nillable>
          <binding>java.lang.String</binding>
        </Attribute>
        <Attribute>
          <name>imageindex</name>
          <minOccurs>0</minOccurs>
          <maxOccurs>1</maxOccurs>
          <nillable>true</nillable>
          <binding>java.lang.Integer</binding>
        </Attribute>
        <Attribute>
          <name>time</name>
          <minOccurs>0</minOccurs>
          <maxOccurs>1</maxOccurs>
          <nillable>true</nillable>
          <binding>java.sql.Timestamp</binding>
        </Attribute>
        <Attribute>
          <name>elevation</name>
          <minOccurs>0</minOccurs>
          <maxOccurs>1</maxOccurs>
          <nillable>true</nillable>
          <binding>java.lang.Double</binding>
        </Attribute>
        <Attribute>
          <name>fileDate</name>
          <minOccurs>0</minOccurs>
          <maxOccurs>1</maxOccurs>
          <nillable>true</nillable>
          <binding>java.sql.Timestamp</binding>
        </Attribute>
        <Attribute>
          <name>updated</name>
          <minOccurs>0</minOccurs>
          <maxOccurs>1</maxOccurs>
          <nillable>true</nillable>
          <binding>java.sql.Timestamp</binding>
        </Attribute>
      </attributes>
      <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:8080/geoserver/rest/workspaces/topp/coveragestores/polyphemus-v1/coverages/NO2/index/granules.xml" type="application/xml"/>
    </Schema>


Listing the existing granules can be performed as follows:

.. code-block:: console

   curl -v -u admin:geoserver -XGET "http://localhost:8080/geoserver/rest/workspaces/topp/coveragestores/polyphemus-v1/coverages/NO2/index/granules.xml?limit=2"

This will result in a GML description of the granules, as follows:

.. code-block:: xml

    <?xml version="1.0" encoding="UTF-8"?>
    <wfs:FeatureCollection xmlns:gf="http://www.geoserver.org/rest/granules" xmlns:ogc="http://www.opengis.net/ogc" xmlns:wfs="http://www.opengis.net/wfs" xmlns:gml="http://www.opengis.net/gml">
      <gml:boundedBy>
        <gml:Box srsName="http://www.opengis.net/gml/srs/epsg.xml#4326">
          <gml:coord>
            <gml:X>5.0</gml:X>
            <gml:Y>45.0</gml:Y>
          </gml:coord>
          <gml:coord>
            <gml:X>14.875</gml:X>
            <gml:Y>50.9375</gml:Y>
          </gml:coord>
        </gml:Box>
      </gml:boundedBy>
      <gml:featureMember>
        <gf:NO2 fid="NO2.1">
          <gf:the_geom>
            <gml:Polygon>
              <gml:outerBoundaryIs>
                <gml:LinearRing>
                  <gml:coordinates>5.0,45.0 5.0,50.9375 14.875,50.9375 14.875,45.0 5.0,45.0</gml:coordinates>
                </gml:LinearRing>
              </gml:outerBoundaryIs>
            </gml:Polygon>
          </gf:the_geom>
          <gf:location>polyphemus_20130301.nc</gf:location>
          <gf:imageindex>336</gf:imageindex>
          <gf:time>2013-03-01T00:00:00Z</gf:time>
          <gf:elevation>10.0</gf:elevation>
          <gf:fileDate>2013-03-01T00:00:00Z</gf:fileDate>
          <gf:updated>2013-04-11T10:54:31Z</gf:updated>
        </gf:NO2>
      </gml:featureMember>
      <gml:featureMember>
        <gf:NO2 fid="NO2.2">
          <gf:the_geom>
            <gml:Polygon>
              <gml:outerBoundaryIs>
                <gml:LinearRing>
                  <gml:coordinates>5.0,45.0 5.0,50.9375 14.875,50.9375 14.875,45.0 5.0,45.0</gml:coordinates>
                </gml:LinearRing>
              </gml:outerBoundaryIs>
            </gml:Polygon>
          </gf:the_geom>
          <gf:location>polyphemus_20130301.nc</gf:location>
          <gf:imageindex>337</gf:imageindex>
          <gf:time>2013-03-01T00:00:00Z</gf:time>
          <gf:elevation>35.0</gf:elevation>
          <gf:fileDate>2013-03-01T00:00:00Z</gf:fileDate>
          <gf:updated>2013-04-11T10:54:31Z</gf:updated>
        </gf:NO2>
      </gml:featureMember>
    </wfs:FeatureCollection>

   
Removing all the granules originating from a particular file (a NetCDF file can contain many) can be done as follows:

.. code-block:: console
   
   curl -v -u admin:geoserver -XDELETE "http://localhost:8080/geoserver/rest/workspaces/topp/coveragestores/polyphemus-v1/coverages/NO2/index/granules.xml?filter=location='polyphemus_20130301.nc'"
   
Creating an empty mosaic and harvest granules
---------------------------------------------

The next command uploads an :download:`empty.zip` file. 
This archive contains the definition of an empty mosaic (no granules in this case) through the following files::

      datastore.properties (the postgis datastore connection params)
      indexer.xml (The mosaic Indexer, note the CanBeEmpty=true parameter)
      polyphemus-test.xml (The auxiliary file used by the NetCDF reader to parse schemas and tables)

.. note:: **Make sure to update the datastore.properties file** with your connection params and refresh the zip when done, before uploading it. 
.. note:: The code blocks below contain a single command that is extended over multiple lines.
.. note:: The configure=none parameter allows for future configuration after harvesting

.. code-block:: console

   curl -u admin:geoserver -XPUT -H "Content-type:application/zip" --data-binary @empty.zip
      http://localhost:8080/geoserver/rest/workspaces/topp/coveragestores/empty/file.imagemosaic?configure=none

The following instead instructs the mosaic to harvest a single :download:`polyphemus_20120401.nc` file into the mosaic, collecting its properties and updating the mosaic index:

.. code-block:: console

   curl -v -u admin:geoserver -XPOST -H "Content-type: text/plain" -d "file:///path/to/the/file/polyphemus_20120401.nc" 
      "http://localhost:8080/geoserver/rest/workspaces/topp/coveragestores/empty/external.imagemosaic"

Once done you can get the list of coverages/granules available on that store.

.. code-block:: console

   curl -v -u admin:geoserver -XGET 
       "http://localhost:8080/geoserver/rest/workspaces/topp/coveragestores/empty/coverages.xml?list=all"

which will result in the following:

.. code-block:: json

      <list>
        <coverageName>NO2</coverageName>
        <coverageName>O3</coverageName>
      </list>

Next step is configuring ONCE for coverage (as an instance NO2), an available coverage. 

.. code-block:: console

   curl -v -u admin:geoserver -XPOST -H "Content-type: text/xm" -d @"/path/to/coverageconfig.xml" "http://localhost:8080/geoserver/rest/workspaces/topp/coveragestores/empty/coverages"

Where coverageconfig.xml may look like this

.. code-block:: json

    <coverage>
      <name>NO2</name>
    </coverage>

.. note:: When specifying only the coverage name, the coverage will be automatically configured


Master Password Change
----------------------

The master password can be fetched wit a GET request.

.. code-block:: console

   curl -v -u admin:geoserver -XGET   http://localhost:8080/geoserver/rest/security/masterpw.xml

A generated master password may be **-"}3a^Kh**. Next step is creating an XML file.

File changes.xml

.. code-block:: xml

   <masterPassword>
      <oldMasterPassword>-"}3a^Kh</oldMasterPassword>
      <newMasterPassword>geoserver1</newMasterPassword>
   </masterPassword>

Changing the master password using the file:

.. code-block:: console

   curl -v -u admin:geoserver -XPUT -H "Content-type: text/xml" -d @change.xml http://localhost:8080/geoserver/rest/security/masterpw.xml
   
Changing the catalog mode
-------------------------

Fetch the current catalog mode wit a GET request

.. code-block:: console

   curl -v -u admin:geoserver -XGET   http://localhost:8080/geoserver/rest/security/acl/catalog.xml
   
.. code-block:: xml

    <?xml version="1.0" encoding="UTF-8"?>
    <catalog>
        <mode>HIDE</mode>
    </catalog>

Create a file newMode.xml

.. code-block:: xml

    <?xml version="1.0" encoding="UTF-8"?>
    <catalog>
        <mode>MIXED</mode>
    </catalog>
   
Change the catalog mode using the file (check with the GET request after modifying)

.. code-block:: console

   curl -v -u admin:geoserver -XPUT -H "Content-type: text/xml" -d @newMode.xml http://localhost:8080/geoserver/rest/security/acl/catalog.xml

Working with access control rules
---------------------------------
   
Fetch the all current rules for layers

.. code-block:: console

   curl -v -u admin:geoserver -XGET   http://localhost:8080/geoserver/rest/security/acl/layers.xml
   
.. code-block:: xml

   <?xml version="1.0" encoding="UTF-8"?>
   <rules />
   
No rules specified. Create a file rules.xml

.. code-block:: xml

   <?xml version="1.0" encoding="UTF-8"?>
   <rules>
      <rule resource="topp.*.r">*</rule>
      <rule resource="topp.mylayer.w">*</rule>      
   </rules>
   
Add the rules (check with the GET request after adding).

.. code-block:: console

   curl -v -u admin:geoserver -XPOST -H "Content-type: text/xml" -d @rules.xml http://localhost:8080/geoserver/rest/security/acl/layers.xml 
   
Modify the files rules.xml:

.. code-block:: xml

   <?xml version="1.0" encoding="UTF-8"?>
   <rules>
      <rule resource="topp.*.r">ROLE_AUTHORIZED</rule>
      <rule resource="topp.mylayer.w">ROLE_1,ROLE_2</rule>      
   </rules>
   
Modify the rules (check with the GET request after modifying).

.. code-block:: console

   curl -v -u admin:geoserver -XPUT -H "Content-type: text/xml" -d @rules.xml http://localhost:8080/geoserver/rest/security/acl/layers.xml 

Delete the rules individually (check with the GET request after deleting)

.. code-block:: console

   curl -v -u admin:geoserver -XDELETE  http://localhost:8080/geoserver/rest/security/acl/layers/topp.*.r
   curl -v -u admin:geoserver -XDELETE  http://localhost:8080/geoserver/rest/security/acl/layers/topp.mylayer.w

   