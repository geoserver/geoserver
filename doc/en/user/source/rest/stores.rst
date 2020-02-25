.. _rest_stores:

Stores
======

Uploading a shapefile
---------------------

**Create a new store "roads" by uploading a shapefile "roads.zip"**

*Request*

.. admonition:: curl

   ::

       curl -v -u admin:geoserver -XPUT -H "Content-type: application/zip" 
         --data-binary @roads.zip 
         http://localhost:8080/geoserver/rest/workspaces/acme/datastores/roads/file.shp

.. admonition:: python

   TBD

.. admonition:: java

   TBD

*Response*

::

   201 Created



Listing store details
---------------------

*Retrieve information about a specific store**

*Request*

.. admonition:: curl

   ::

       curl -v -u admin:geoserver -XGET
         http://localhost:8080/geoserver/rest/workspaces/acme/datastores/roads.xml

.. admonition:: python

   TBD

.. admonition:: java

   TBD

*Response*

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

*Request*

.. note:: The XML response only provides details about the store itself, so you can use HTML to see the contents of the store.

.. code-block:: console

   curl -v -u admin:geoserver -XGET 
     http://localhost:8080/geoserver/rest/workspaces/acme/datastores/roads.html


Listing featuretype details
---------------------------

.. note:: By default when a shapefile is uploaded, a featuretype is automatically created.

*Request*

.. admonition:: curl

   ::

       curl -v -u admin:geoserver -XGET 
         http://localhost:8080/geoserver/rest/workspaces/acme/datastores/roads/featuretypes/roads.xml

.. admonition:: python

   TBD

.. admonition:: java

   TBD

*Response*

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



Adding an existing shapefile
----------------------------

**Publish a shapefile "rivers.shp" that already exists on the server without needing to be uploaded**

*Request*

.. admonition:: curl

   ::

       curl -v -u admin:geoserver -XPUT -H "Content-type: text/plain" 
         -d "file:///data/shapefiles/rivers/rivers.shp" 
         http://localhost:8080/geoserver/rest/workspaces/acme/datastores/rivers/external.shp

.. note:: The ``external.shp`` part of the request URI indicates that the file is coming from outside the catalog.

*Response*

::

   201 Created




Adding a directory of existing shapefiles
-----------------------------------------

**Create a store containing a directory of shapefiles that already exists on the server without needing to be uploaded**

*Request*

.. admonition:: curl

   ::

       curl -v -u admin:geoserver -XPUT -H "Content-type: text/plain" 
         -d "file:///data/shapefiles/" 
         "http://localhost:8080/geoserver/rest/workspaces/acme/datastores/shapefiles/external.shp?configure=all"

.. note:: The ``configure=all`` query string parameter sets each shapefile in the directory to be loaded and published.

*Response*

::

   201 Created





Adding a PostGIS database store
-------------------------------

**Add an existing PostGIS database named "nyc" as a new store**

.. note:: This example assumes that a PostGIS database named ``nyc`` is present on the local system and is accessible by the user ``bob``.

Given the following content saved as :file:`nycDataStore.xml`:

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

*Request*

.. admonition:: curl

   ::

       curl -v -u admin:geoserver -XPOST -T nycDataStore.xml -H "Content-type: text/xml" 
          http://localhost:8080/geoserver/rest/workspaces/acme/datastores

*Response*

::

   201 Created




Listing a PostGIS database store details
----------------------------------------

**Retrieve information about a PostGIS store**

*Request*

.. admonition:: curl

   ::

       curl -v -u admin:geoserver -XGET http://localhost:8080/geoserver/rest/workspaces/acme/datastores/nyc.xml

*Response*

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


Publishing a table from an existing PostGIS store
-------------------------------------------------

**Publish a new featuretype from a PostGIS store table "buildings"**

.. note:: This example assumes the table has already been created.

*Request*

.. admonition:: curl

   ::

       curl -v -u admin:geoserver -XPOST -H "Content-type: text/xml" 
         -d "<featureType><name>buildings</name></featureType>" 
         http://localhost:8080/geoserver/rest/workspaces/acme/datastores/nyc/featuretypes


.. note:: 

   This layer can viewed with a WMS GetMap request::

     http://localhost:8080/geoserver/wms/reflect?layers=acme:buildings


Creating a PostGIS table
------------------------

**Create a new featuretype in GeoServer and simultaneously create a table in PostGIS**

Given the following content saved as :file:`annotations.xml`:

.. code-block:: xml

   <featureType>
     <name>annotations</name>
     <nativeName>annotations</nativeName>
     <title>Annotations</title>
     <srs>EPSG:4326</srs>
     <attributes>
       <attribute>
         <name>the_geom</name>
         <binding>org.locationtech.jts.geom.Point</binding>
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

*Request*

.. admonition:: curl

   ::
    
       curl -v -u admin:geoserver -XPOST -T annotations.xml -H "Content-type: text/xml" 
         http://localhost:8080/geoserver/rest/workspaces/acme/datastores/nyc/featuretypes

.. note:: The NYC store must be a PostGIS store for this to succeed.

*Response*

::

   201 Created

A new and empty table named "annotations" in the "nyc" database will be created as well.