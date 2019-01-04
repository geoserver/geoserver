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