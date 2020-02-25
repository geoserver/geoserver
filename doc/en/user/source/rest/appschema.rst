.. _rest_App-Schema:

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
   
Cleaning schemas on internal MongoDB stores
-------------------------------------------

**Clean persisted schema on an internal MongoDB Store, allowing it to generate a new one from data.**

*Request template*

.. admonition:: curl

   ::

       curl -v -X POST 
         -u admin:geoserver http://localhost:8080/geoserver/rest/workspaces/{WORKSPACE}/appschemastores/{APP_SCHEMA_STORE_NAME}/datastores/{INTERNAL_STORE_ID}/cleanSchemas
   
*Request*
   
.. admonition:: curl

   ::

       curl -v -X POST 
         -u admin:geoserver http://localhost:8080/geoserver/rest/workspaces/st/appschemastores/AppSchemaStoreName/datastores/store_id/cleanSchemas

*Response*

::

   200 OK
   
**Clean persisted schema on all internal MongoDB Stores, allowing it to generate them from data.**

*Request template*

.. admonition:: curl

   ::

       curl -v -X POST 
         -u admin:geoserver http://localhost:8080/geoserver/rest/workspaces/{WORKSPACE}/appschemastores/{APP_SCHEMA_STORE_NAME}/cleanSchemas
   
*Request*
   
.. admonition:: curl

   ::

       curl -v -X POST 
         -u admin:geoserver http://localhost:8080/geoserver/rest/workspaces/st/appschemastores/AppSchemaStoreName/cleanSchemas

*Response*

::

   200 OK
   
**Rebuild persisted schema on internal MongoDB Store, allowing it to generate them from data and query parameters.**

*Request template*

.. admonition:: curl

   ::

       curl -v -X POST 
         -u admin:geoserver http://localhost:8080/geoserver/rest/workspaces/{WORKSPACE}/appschemastores/{APP_SCHEMA_STORE_NAME}/datastores/{INTERNAL_STORE_ID}/rebuildMongoSchemas?ids={ID_1},{ID_2}&max={MAX_OBJECTS}
   
- ids: Comma separated MongoDB JSON objects ids to query for generating schemas.  Not required if the 'max' is setted.
- max: Max number of MongoDB JSON objects to get for generating schemas.  Not required if the 'ids' is setted.

*Request*
   
.. admonition:: curl

   ::

       curl -v -X POST 
         -u admin:geoserver http://localhost:8080/geoserver/rest/workspaces/st/appschemastores/AppSchemaStoreName/datastores/store_id/rebuildMongoSchemas?ids=58e5889ce4b02461ad5af081,58e5889ce4b02461ad5af080&max=5

*Response*

::

   200 OK
   
**Rebuild persisted schema on all internal MongoDB Stores, allowing it to generate them from data and query parameters.**

*Request template*

.. admonition:: curl

   ::

       curl -v -X POST 
         -u admin:geoserver http://localhost:8080/geoserver/rest/workspaces/{WORKSPACE}/appschemastores/{APP_SCHEMA_STORE_NAME}/rebuildMongoSchemas?ids={ID_1},{ID_2}&max={MAX_OBJECTS}
   
- ids: Comma separated MongoDB JSON objects ids to query for generating schemas.  Not required if the 'max' is setted.
- max: Max number of MongoDB JSON objects to get for generating schemas.  Not required if the 'ids' is setted.

*Request*
   
.. admonition:: curl

   ::

       curl -v -X POST 
         -u admin:geoserver http://localhost:8080/geoserver/rest/workspaces/st/appschemastores/AppSchemaStoreName/rebuildMongoSchemas?ids=58e5889ce4b02461ad5af081,58e5889ce4b02461ad5af080&max=5

*Response*

::

   200 OK
   
.. note:: This endpoins are only available when App-Schema and MongoDB modules are installed on Geoserver, and involved app-schema store have internal MongoDB stores in mappings definition.