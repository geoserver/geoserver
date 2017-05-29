.. _rest_workspaces:

Workspaces
==========

The REST API allows you to create and manage workspaces in GeoServer.

.. note:: Read the :api:`API reference for /workspaces <workspaces.yaml>`.

Adding a new workspace
----------------------

**Creates a new workspace named "acme" with a POST request**

*Request*

.. admonition:: curl

   ::

       curl -v -u admin:geoserver -XPOST -H "Content-type: text/xml" 
         -d "<workspace><name>acme</name></workspace>" 
         http://localhost:8080/geoserver/rest/workspaces

.. admonition:: python

   TBD

.. admonition:: java

   TBD


*Response*

::

   201 Created

.. note:: The ``Location`` response header specifies the location (URI) of the newly created workspace.

Listing workspace details
-------------------------

**Retrieve information about a specific workspace**

*Request*

.. admonition:: curl

   ::

       curl -v -u admin:geoserver -XGET -H "Accept: text/xml" 
         http://localhost:8080/geoserver/rest/workspaces/acme

.. note:: The ``Accept`` header is optional. 

.. admonition:: python

   TBD

.. admonition:: java

   TBD

*Response*

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


.. _rest_examples_curl_imagemosaic:

Uploading a new image mosaic
--------------------------------------

**Upload a ZIP file containing a mosaic definition and granule(s)**

*Request*

.. admonition:: curl

   ::

       curl -u admin:geoserver -XPUT -H "Content-type:application/zip" --data-binary @polyphemus.zip
          http://localhost:8080/geoserver/rest/workspaces/topp/coveragestores/polyphemus/file.imagemosaic

*Response*

::

   200 OK

Updating an image mosaic contents
---------------------------------

**Harvest (or reharvest) a single file into the mosaic and update the mosaic index**

*Request*

.. admonition:: curl

   ::

       curl -v -u admin:geoserver -XPOST -H "Content-type: text/plain" -d "file:///path/to/the/file/polyphemus_20130302.nc" 
          "http://localhost:8080/geoserver/rest/workspaces/topp/coveragestores/poly-incremental/external.imagemosaic"

*Response*

::

   201 Created

**Harvest (or reharvest) a whole directory into the mosaic and update the mosaic index**

*Request*

.. admonition:: curl

   ::

        curl -v -u admin:geoserver -XPOST -H "Content-type: text/plain" -d "file:///path/to/the/mosaic/folder" 
           "http://localhost:8080/geoserver/rest/workspaces/topp/coveragestores/poly-incremental/external.imagemosaic"

*Response*

::

   201 Created

Listing image mosaic details
----------------------------

**Retrieve the image mosaic index structure**

*Request*

.. admonition:: curl

   ::

       curl -v -u admin:geoserver -XGET "http://localhost:8080/geoserver/rest/workspaces/topp/coveragestores/polyphemus-v1/coverages/NO2/index.xml"

*Response*

.. code-block:: xml

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

**Retrieve the existing granule information**

*Request*

.. admonition:: curl

   ::

       curl -v -u admin:geoserver -XGET "http://localhost:8080/geoserver/rest/workspaces/topp/coveragestores/polyphemus-v1/coverages/NO2/index/granules.xml?limit=2"

*Response*

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


Removing image mosaic granules
------------------------------

**Remove all the granules originating from a particular file**

*Request*

.. admonition:: curl

   ::

       curl -v -u admin:geoserver -XDELETE "http://localhost:8080/geoserver/rest/workspaces/topp/coveragestores/polyphemus-v1/coverages/NO2/index/granules.xml?filter=location='polyphemus_20130301.nc'"
   
*Response*

::

   200 OK


Uploading an empty mosaic
-------------------------

**Upload an archive with the definition of an mosaic, but with no granules**

Given a :download:`empty.zip <artifacts/empty.zip>` file containing:

* ``datastore.properties`` (PostGIS connection parameters)
* ``indexer.xml`` (Mosaic indexer; note the ``CanBeEmpty=true`` parameter)
* ``polyphemus-test.xml`` (Auxiliary file used by the NetCDF reader to parse schemas and tables)

.. warning:: Make sure to update the ``datastore.properties`` file with your connection parameters and refresh the ZIP before uploading it. 

*Request*

.. admonition:: curl

   ::

       curl -u admin:geoserver -XPUT -H "Content-type:application/zip" --data-binary @empty.zip
          http://localhost:8080/geoserver/rest/workspaces/topp/coveragestores/empty/file.imagemosaic?configure=none

.. note:: The ``configure=none`` parameter allows for future configuration after harvesting.

*Response*

::

  200 OK

**Configure a coverage on the mosaic**


Given a ``coverageconfig.xml``:

.. code-block:: xml

    <coverage>
      <nativeCoverageName>NO2</nativeCoverageName>
      <name>NO2</name>
    </coverage>

*Request*

.. admonition:: curl

   ::

       curl -v -u admin:geoserver -XPOST -H "Content-type: text/xml" -d @"/path/to/coverageconfig.xml" "http://localhost:8080/geoserver/rest/workspaces/topp/coveragestores/empty/coverages"

.. note:: When specifying only the coverage name, the coverage will be automatically configured.

*Response*

::

  201 Created



Uploading an app-schema mapping file
------------------------------------

**Create a new app-schema store and update the feature type mappings of an existing app-schema store by uploading a mapping configuration file**

.. _appschema_upload_create:

.. note:: The following request uploads an app-schema mapping file called ``LandCoverVector.xml`` to a data store called ``LandCoverVector``. If no ``LandCoverVector`` data store existed in workspace ``lcv`` prior to the request, it would be created.

*Request*

.. admonition:: curl

   ::

       curl -v -X PUT -d @LandCoverVector.xml -H "Content-Type: text/xml"
       -u admin:geoserver http://localhost:8080/geoserver/rest/workspaces/lcv/datastores/LandCoverVector/file.appschema?configure=all

*Response*

::

   201 Created


Listing app-schema store details
--------------------------------

*Request*

.. admonition:: curl

   ::

       curl -v -u admin:geoserver -X GET
       http://localhost:8080/geoserver/rest/workspaces/lcv/datastores/LandCoverVector.xml

*Response*

.. code-block:: xml

   <dataStore>
     <name>LandCoverVector</name>
     <type>Application Schema DataAccess</type>
     <enabled>true</enabled>
     <workspace>
       <name>lcv</name>
       <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:8080/geoserver/rest/workspaces/lcv.xml" type="application/xml"/>
     </workspace>
     <connectionParameters>
       <entry key="dbtype">app-schema</entry>
       <entry key="namespace">http://inspire.ec.europa.eu/schemas/lcv/3.0</entry>
       <entry key="url">file:/path/to/data_dir/data/lcv/LandCoverVector/LandCoverVector.appschema</entry>
     </connectionParameters>
     <__default>false</__default>
     <featureTypes>
       <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:8080/geoserver/rest/workspaces/lcv/datastores/LandCoverVector/featuretypes.xml" type="application/xml"/>
     </featureTypes>
   </dataStore>


Uploading a new app-schema mapping configuration file
-----------------------------------------------------

**Upload a new mapping configuration, stored in the mapping file "`LandCoverVector_alternative.xml", to the "LandCoverVector" data store**

*Request*

.. admonition:: curl

   ::

       curl -v -X PUT -d @LandCoverVector_alternative.xml -H "Content-Type: text/xml"
         -u admin:geoserver http://localhost:8080/geoserver/rest/workspaces/lcv/datastores/LandCoverVector/file.appschema?configure=none

*Response*

::

   200 OK

.. note:: This time the ``configure`` parameter is set to ``none``, because we don't want to configure again the feature types, just replace their mapping configuration.

.. note:: If the set of feature types mapped in the new configuration file differs from the set of feature types mapped in the old one (either some are missing, or some are new, or both), the best way to proceed is to delete the data store and create it anew issuing another PUT request, :ref:`as shown above <appschema_upload_create>`.



Uploading multiple app-schema mapping files
-------------------------------------------

**Create a new app-schema data store based on a complex mapping configuration split into multiple files, and show how to upload application schemas (i.e. XSD files) along with the mapping configuration.**

.. note:: In the previous example, we have seen how to create a new app-schema data store by uploading a mapping configuration stored in a single file; this time, things are more complicated, since the mappings have been spread over two configuration files: the main configuration file is called ``geosciml.appschema`` and contains the mappings for three feature types: ``GeologicUnit``, ``MappedFeature`` and ``GeologicEvent``; the second file is called ``cgi_termvalue.xml`` and contains the mappings for a single non-feature type, ``CGI_TermValue``.

.. note:: As explained in the :ref:`REST API reference documentation for data stores <rest_api_datastores_file_put_appschema>`, when the mapping configuration is spread over multiple files, the extension of the main configuration file must be ``.appschema``.

The main configuration file includes the second file:

.. code-block:: xml

   ...
   <includedTypes>
     <Include>cgi_termvalue.xml</Include>
   </includedTypes>
   ...

We also want to upload to GeoServer the schemas required to define the mapping, instead of having GeoServer retrieve them from the internet (which is especially useful in case our server doesn't have access to the web). The main schema is called ``geosciml.xsd`` and is referred to in ``geosciml.appschema`` as such:

.. code-block:: xml

   ...
   <targetTypes>
     <FeatureType>
       <schemaUri>geosciml.xsd</schemaUri>
     </FeatureType>
   </targetTypes>
   ...

In this case, the main schema depends on several other schemas:

.. code-block:: xml

   <include schemaLocation="geologicUnit.xsd"/>
   <include schemaLocation="borehole.xsd"/>
   <include schemaLocation="vocabulary.xsd"/>
   <include schemaLocation="geologicRelation.xsd"/>
   <include schemaLocation="fossil.xsd"/>
   <include schemaLocation="value.xsd"/>
   <include schemaLocation="geologicFeature.xsd"/>
   <include schemaLocation="geologicAge.xsd"/>
   <include schemaLocation="earthMaterial.xsd"/>
   <include schemaLocation="collection.xsd"/>
   <include schemaLocation="geologicStructure.xsd"/>

They don't need to be listed in the ``targetTypes`` section of the mapping configuration, but they must be included in the ZIP archive that will be uploaded.

.. note:: The GeoSciML schemas listed above, as pretty much any application schema out there, reference the base GML schemas (notably, ``http://schemas.opengis.net/gml/3.1.1/base/gml.xsd``) and a few other remotely hosted schemas (e.g. ``http://www.geosciml.org/cgiutilities/1.0/xsd/cgiUtilities.xsd``).
      For the example to work in a completely offline environment, one would have to either replace all remote references with local ones, or pre-populate the app-schema cache with a copy of the remote schemas. :ref:`GeoServer's user manual <app-schema-cache>` contains more information on the app-schema cache.

To summarize, we'll upload to GeoServer a ZIP archive with the following contents:

.. code-block:: console

   geosciml.appschema      # main mapping file
   cgi_termvalue.xml       # secondary mapping file
   geosciml.xsd            # main schema
   borehole.xsd
   collection.xsd
   earthMaterial.xsd
   fossil.xsd
   geologicAge.xsd
   geologicFeature.xsd
   geologicRelation.xsd
   geologicStructure.xsd
   geologicUnit.xsd
   value.xsd
   vocabulary.xsd

*Request*

.. admonition:: curl

   ::

       curl -X PUT --data-binary @geosciml.zip -H "Content-Type: application/zip"
       -u admin:geoserver http://localhost:8080/geoserver/rest/workspaces/gsml/datastores/geosciml/file.appschema?configure=all


*Response*

::

   200 OK


A new ``geosciml`` data store will be created with three feature types in it:

.. code-block:: xml

   <featureTypes>
     <featureType>
       <name>MappedFeature</name>
       <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:8080/geoserver/rest/workspaces/gsml/datastores/geosciml/featuretypes/MappedFeature.xml" type="application/xml"/>
     </featureType>
     <featureType>
       <name>GeologicEvent</name>
       <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:8080/geoserver/rest/workspaces/gsml/datastores/geosciml/featuretypes/GeologicEvent.xml" type="application/xml"/>
     </featureType>
     <featureType>
       <name>GeologicUnit</name>
       <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:8080/geoserver/rest/workspaces/gsml/datastores/geosciml/featuretypes/GeologicUnit.xml" type="application/xml"/>
     </featureType>
   </featureTypes>

