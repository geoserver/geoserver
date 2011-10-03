.. _rest_config_examples_curl:

cURL
====

The examples in this section use the `cURL <http://curl.haxx.se/>`_
utility, which is a handy command line tool for executing HTTP requests and 
transferring files. Though cURL is used the examples apply to any HTTP-capable
tool or library.

Adding a new workspace
----------------------

The following creates a new workspace named "acme" with a POST request::

  curl -u admin:geoserver -v -XPOST -H 'Content-type: text/xml' \ 
     -d '<workspace><name>acme</name></workspace>' \
     http://localhost:8080/geoserver/rest/workspaces

The response should contain the following::
 
  < HTTP/1.1 201 Created
  < Date: Fri, 20 Feb 2009 01:56:28 GMT
  < Location: http://localhost:8080/geoserver/rest/workspaces/acme
  < Server: Noelios-Restlet-Engine/1.0.5
  < Transfer-Encoding: chunked

Note the ``Location`` response header which specifies the location of the 
newly created workspace. The following retrieves the new workspace as XML with a
GET request::

  curl -XGET -H 'Accept: text/xml' http://localhost:8080/geoserver/rest/workspaces/acme

The response should look like:

.. code-block:: xml

   <workspace>
     <name>acme</name>
     <dataStores>
       <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:8080/geoserver/rest/workspaces/acme/datastores.xml" type="application/xml"/>
     </dataStores>
     <coverageStores>
       <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:8080/geoserver/rest/workspaces/acme/coveragestores.xml" type="application/xml"/>
     </coverageStores>
   </workspace>

Specifying the ``Accept`` header to relay the desired representation of the 
workspace can be tedious. The following is an equivalent (yet less RESTful)
request::

  curl -XGET http://localhost:8080/geoserver/rest/workspaces/acme.xml

Uploading a Shapefile
---------------------

In this example a new datastore will be created by uploading a Shapefile. The 
following uploads the zipped shapefile ``roads.zip`` and creates a new 
datastore named ``roads``::

  curl -u admin:geoserver -XPUT -H 'Content-type: application/zip' \
     --data-binary @roads.zip \ 
     http://localhost:8080/geoserver/rest/workspaces/acme/datastores/roads/file.shp

The following retrieves the created data store as XML::

  curl -XGET http://localhost:8080/geoserver/rest/workspaces/acme/datastores/roads.xml

.. code-block:: xml

   <dataStore>
     <name>roads</name>
     <workspace>
       <name>acme</name>
       <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:8080/geoserver/rest/workspaces/acme.xml" type="application/xml"/>
     </workspace>
     <connectionParameters>
       <namespace>http://acme</namespace>
       <url>file:/Users/jdeolive/devel/geoserver/1.7.x/data/minimal/data/roads/roads.shp</url>
     </connectionParameters>
     <featureTypes>
       <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:8080/geoserver/rest/workspaces/acme/datastores/roads/featuretypes.xml" type="application/xml"/>
     </featureTypes>
   </dataStore>

By default when a shapefile is uploaded a feature type is automatically created.
See :ref:`webadmin_layers` page for details on how to control this behaviour. The following 
retrieves the created feature type as XML:: 

  curl -XGET 
    http://localhost:8080/geoserver/rest/workspaces/acme/datastores/roads/featuretypes/roads.xml

.. code-block:: xml
   
   <featureType>
     <name>roads</name>
     <nativeName>roads</nativeName>
     <namespace>
       <name>acme</name>
       <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:8080/geoserver/rest/namespaces/acme.xml" type="application/xml"/>
     </namespace>
     ...
   </featureType>

Adding an existing Shapefile
----------------------------

In the previous example a Shapefile was uploaded directly by sending a zip file
in the body of a request. This example shows how to add a Shapefile that already
exists on the server.

Consider a directory on the server ``/data/shapefiles/roads`` that contains the Shapefile ``roads.shp``. The following adds a new datastore for the 
Shapefile::

  curl -u admin:geoserver -XPUT -H 'Content-type: text/plain' \ 
     -d 'file:///data/shapefiles/roads/roads.shp' \
     http://localhost:8080/geoserver/rest/workspaces/acme/datastores/roads/external.shp

Note the ``external.shp`` part of the request uri.

Adding a directory of existing Shapefiles
-----------------------------------------

In the previous example a datastore was created for a single Shapefile that 
already existed on the server. This example shows how to add a directory of 
Shapefiles.

Consider a directory on the server ``/data/shapefiles`` that contains a number
of different Shapefiles. The following adds a new datastore for all the 
Shapefiles in the directory::

  curl -u admin:geoserver -XPUT -H 'Content-type: text/plain' \ 
     -d 'file:///data/shapefiles/roads' \
     "http://localhost:8080/geoserver/rest/workspaces/acme/datastores/roads/external.shp?configure=all"

Note the ``configure=all`` query string parameter.

Changing a feature type style
-----------------------------

In the previous example a Shapefile was uploaded, and in the process a feature 
type was created. Whenever a feature type is created an layer is implicitly 
created for it. The following retrieves the layer as XML::

  curl -XGET http://localhost:8080/geoserver/rest/layers/acme:roads.xml

.. code-block:: xml

   <layer>
     <name>roads</name>
     <path>/</path>
     <type>VECTOR</type>
     <defaultStyle>
       <name>roads_style</name>
       <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:8080/geoserver/rest/styles/roads_style.xml" type="application/xml"/>
     </defaultStyle>
     <styles>
       <style>
         <name>line</name>
         <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:8080/geoserver/rest/styles/line.xml" type="application/xml"/>
       </style>
     </styles>
     <resource class="featureType">
       <name>roads</name>
       <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:8080/geoserver/rest/workspaces/acme/datastores/roads/featuretypes/roads.xml" type="application/xml"/>
     </resource>
     <enabled>false</enabled>
   </layer>

When the layer is created a default style named ``polygon`` is assigned to 
it. This style can viewed with a WMS GetMap request (http://localhost:8080/geoserver/wms/reflect?layers=acme:roads)

In this example a new style will be created and assigned to the layer 
created in the previous example. The following creates a new style named
``roads_style``::

  curl -u admin:geoserver -XPOST -H 'Content-type: text/xml' \
    -d '<style><name>roads_style</name><filename>roads.sld</filename></style>' 
    http://localhost:8080/geoserver/rest/styles

Uploading the file ``roads.sld``::

  curl -u admin:geoserver -XPUT -H 'Content-type: application/vnd.ogc.sld+xml' \
    -d @roads.sld http://localhost:8080/geoserver/rest/styles/roads_style

The following applies the newly created style to the layer created in the 
previous example::

  curl -u admin:geoserver -XPUT -H 'Content-type: text/xml' \
    -d '<layer><defaultStyle><name>roads_style</name></defaultStyle></layer>' \
    http://localhost:8080/geoserver/rest/layers/acme:roads

The new style can be viewed with the same GetMap request (http://localhost:8080/geoserver/wms/reflect?layers=acme:roads) as above.

Adding a PostGIS database
-------------------------

.. note::

   This section assumes that a PostGIS database named ``nyc`` is present on the
   local system and is accessible by the user ``bob``.

In this example a PostGIS database named ``nyc`` will be added as a new 
data store. In preparation create the database and import the nyc.sql file::

  psql nyc < nyc.sql

The following represents the new data store:

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

Save the above xml into a file named ``nycDataStore.xml``. The following adds 
the new datastore::

  curl -u admin:geoserver -XPOST -T nycDataStore.xml -H 'Content-type: text/xml' \
    http://localhost:8080/geoserver/rest/workspaces/acme/datastores

Adding a PostGIS table
----------------------

In this example two tables from the PostGIS database created in the previous 
example will be added as feature types. The following adds the table 
``buildings`` as a new feature type::

  curl -u admin:geoserver -XPOST -H 'Content-type: text/xml' \
    -d '<featureType><name>buildings</name></featureType>' \
    http://localhost:8080/geoserver/rest/workspaces/acme/datastores/nyc/featuretypes

The following retrieves the created feature type::

  curl -XGET http://localhost:8080/geoserver/rest/workspaces/acme/datastores/nyc/featuretypes/buildings.xml

This GetMap request (http://localhost:8080/geoserver/wms/reflect?layers=acme:buildings) 
shows the rendered buildings layer.

The following adds the table ``parks`` as a new feature type::

  curl -u admin:geoserver -XPOST -H 'Content-type: text/xml' \
    -d '<featureType><name>parks</name></featureType>' \
    http://localhost:8080/geoserver/rest/workspaces/acme/datastores/nyc/featuretypes

This GetMap request (http://localhost:8080/geoserver/wms/reflect?layers=acme:parks) 
shows the rendered parks layer.

Creating a PostGIS table
------------------------

In the previous example a new feature type was added from a table that already existed in the database. The following 
creates a new feature type along with the underlying table from scratch. The following XML represents the new feature type
named 'annotations'.

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
    
Save the above xml into a file named ``annotations.xml``. The following adds 
the new datastore::

  curl -u admin:geoserver -XPOST -T annotations.xml -H 'Content-type: text/xml' \
    http://localhost:8080/geoserver/rest/workspaces/acme/datastores/nyc/featuretypes
    
The result is a new empty table named "annotations" in the "nyc" database, fully configured as a feature type. 

Creating a layer group
----------------------

In this example the layers added in previous examples will be used to create a
layer group. First a few styles need to be added. The following adds a style
for the buildings layer::

  curl -u admin:geoserver -XPUT -H 'Content-type: application/vnd.ogc.sld+xml' -d @buildings.sld \ 
   http://localhost:8080/geoserver/rest/styles/buildings_style

The following adds a style for the parks layer::

  curl -u admin:geoserver -XPUT -H 'Content-type: application/vnd.ogc.sld+xml' -d @parks.sld \ 
   http://localhost:8080/geoserver/rest/styles/parks_style

The following represents the new layer group:

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
      <style>parks</style>
      <style>buildings_style</style>
    </styles>
  </layerGroup>

Save the following in a file named ``nycLayerGroup.xml``. The following creates
the new layer group::

  curl -u admin:geoserver -XPOST -d @nycLayerGroup.xml -H 'Content-type: text/xml' \
     http://localhost:8080/geoserver/rest/layergroups

This GetMap request (http://localhost:8080/geoserver/wms/reflect?layers=nyc)
shows the rendered layer group. 

