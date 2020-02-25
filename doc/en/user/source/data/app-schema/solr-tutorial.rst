.. _solr_tutorial:

Apache Solr Tutorial
====================

This tutorial demonstrates how to use the App-Schema plugin with a Apache Solr data store. This tutorial will focus on the Apache Solr data store specific aspects, and the `App-Schema documentation <_app-schema>`_ should be read first.

The use case for this tutorial will be to serve through App-Schema the information about some meteorological stations index in an Apache Solr core. Note that this use case is completely fictional and only used to demonstrate the Apache Solr and App-Schema integration.

A station data is composed of some meta-information about the station, e.g. it's name and position. The only extra \ different configuration we need to provide when using Apache Solr as a data source is the configuration of the data store itself. 

Apache Solr data source configuration as a specific syntax and allow us to specify geometry attributes and to explicitly set the default geometry:

.. code-block:: xml

  <sourceDataStores>
    <SolrDataStore>
      <id>stations</id>
      <url>http://localhost:8983/solr/stations</url>
      <index name="stations">
        <geometry default="true">
          <name>location</name>
          <srid>4326</srid>
          <type>POINT</type>
        </geometry>
      </index>
    </SolrDataStore>
  </sourceDataStores>

In this particular case the the ``location`` attribute contains a point geometry and will be the default geometry.

The complete mapping file is:

.. code-block:: xml

  <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
  <as:AppSchemaDataAccess xmlns:as="http://www.geotools.org/app-schema">
      <namespaces>
          <Namespace>
              <prefix>st</prefix>
              <uri>http://www.stations.org/1.0</uri>
          </Namespace>
          <Namespace>
              <prefix>gml</prefix>
              <uri>http://www.opengis.net/gml/3.2</uri>
          </Namespace>
      </namespaces>
      <sourceDataStores>
          <SolrDataStore>
              <id>stations</id>
              <url>http://localhost:8983/solr/stations</url>
              <index name="stations">
                  <geometry default="true">
                      <name>location</name>
                      <srid>4326</srid>
                      <type>POINT</type>
                  </geometry>
              </index>
          </SolrDataStore>
      </sourceDataStores>
      <targetTypes>
          <FeatureType>
              <schemaUri>stations.xsd</schemaUri>
          </FeatureType>
      </targetTypes>
      <typeMappings>
          <FeatureTypeMapping>
              <mappingName>stations_solr</mappingName>
              <sourceDataStore>stations</sourceDataStore>
              <sourceType>stations</sourceType>
              <targetElement>st:Station</targetElement>
              <attributeMappings>
                  <AttributeMapping>
                      <targetAttribute>st:Station</targetAttribute>
                      <idExpression>
                          <OCQL>station_id</OCQL>
                      </idExpression>
                  </AttributeMapping>
                  <AttributeMapping>
                      <targetAttribute>st:stationName</targetAttribute>
                      <sourceExpression>
                          <OCQL>station_name</OCQL>
                      </sourceExpression>
                  </AttributeMapping>
                  <AttributeMapping>
                      <targetAttribute>st:position</targetAttribute>
                      <sourceExpression>
                          <OCQL>station_location</OCQL>
                      </sourceExpression>
                  </AttributeMapping>
              </attributeMappings>
          </FeatureTypeMapping>
      </typeMappings>
  </as:AppSchemaDataAccess>

The mappings for the attributes are straightforward and follow the normal App-Schema attributes mappings syntax. Currently multi valued fields are not supported.

Using Solr as App-Schema Indexes
--------------------------------

App-Schema Indexes is an extension for mapping that allows to use Apache Solr as Index for queries and retrieving data from normal App-Schema datasource (SQL DB, MongoDB, ... ).

The only requirement to use it is having Geoserver App-Schema extension and Solr extension installed.

How Index layer works
^^^^^^^^^^^^^^^^^^^^^

When App-Schema detects the index layer is activated for a FeatureType, it will use Solr configured fields for every query incoming from Geoserver OWS requests.  If the incoming query uses only indexed fields App-Schema will query only on Solr data source for retrieving matching features IDs and will connect to normal data source to get all in depth data but exclusively for matching IDs.

.. warning:: note that both Primary Keys (solr index core and data source) should match to get Index layer working. 

Linking an index only store
^^^^^^^^^^^^^^^^^^^^^^^^^^^

Begin creating the SolrDataStore definition as usual along with the Postgis store definition:

.. code-block:: xml

  (...)
  <sourceDataStores>
  (...)
      <SolrDataStore>
          <id>stations_index</id>
          <url>http://localhost:8983/solr/stations</url>
          <index name="stations">
              <geometry default="true">
                  <name>location</name>
                  <srid>4326</srid>
                  <type>POINT</type>
              </geometry>
          </index>
      </SolrDataStore>
       <DataStore>
            <id>postgis_dataStore</id>
            <parameters>
                <Parameter>
                    <name>Connection timeout</name>
                    <value>20</value>
                </Parameter>
                <Parameter>
                    <name>port</name>
                    <value>5432</value>
                </Parameter>
                <Parameter>
                    <name>passwd</name>
                    <value>postgres</value>
                </Parameter>
                <Parameter>
                    <name>dbtype</name>
                    <value>postgis</value>
                </Parameter>
  (...)
  
Link a solr index as index layer on FeatureTypeMapping setting:

* indexDataStore : The SolrDataStore id property from the store you use as index layer only.
* indexType : The solr core to use.

.. code-block:: xml
  
  <typeMappings>
  (...)
      <FeatureTypeMapping>
          <mappingName>Stations</mappingName>
          <sourceDataStore>postgis_dataStore</sourceDataStore>
          <sourceType>meteo_stations</sourceType>
          <targetElement>st:Station</targetElement>
          <defaultGeometry>st:position</defaultGeometry>
          <indexDataStore>stations_index</indexDataStore>
          <indexType>stations</indexType>
          <attributeMappings>
          (...)

Linking an index enabled attribute
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To link a solr core field as index for an AttributeMapping you only need to add an indexField definition with this format:

.. code-block:: xml

  <AttributeMapping>
  (...)
    <indexField>${SOLR_FIELD_NAME}</indexField>
  (...)
  </AttributeMapping>

* ${SOLR_FIELD_NAME} : The field name from solr core to use in index layer.

For example if you need to use solr fields: station_id and station_name; you will write on mapping:

.. code-block:: xml

  <AttributeMapping>
      <targetAttribute>st:Station</targetAttribute>
      <idExpression>
          <OCQL>id</OCQL>
      </idExpression>
      <indexField>station_id</indexField>
  </AttributeMapping>
  <AttributeMapping>
      <targetAttribute>st:stationName</targetAttribute>
      <sourceExpression>
          <OCQL>strConcat('1_', common_name)</OCQL>
      </sourceExpression>
      <indexField>station_name</indexField>
  </AttributeMapping>

