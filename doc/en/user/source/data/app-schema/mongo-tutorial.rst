.. _mongo_tutorial:

MongoDB Tutorial
================

This tutorial demonstrates how to use app-schema plugin with a MongoDB data store. This tutorial will focus on the MongoDB data store specificities is highly recommended to read the app-schema documentation before.
 

Use Case
--------

The use case for this tutorial will be to serve through app-schema the information about some meteorological stations stored in a MongoDB database. Note that this use case is completely fictional and only used to demonstrate the MongoDB and app-schema integration.

First of all let's insert some test data in a MongoDB data store:

.. code-block:: javascript

    db.stations.insert({
        "id": "1",
        "name": "station 1",
        "contact": {
            "mail": "station1@mail.com"
        },
        "geometry": {
            "coordinates": [
                50,
                60
            ],
            "type": "Point"
        },
        "measurements": [
            {
                "name": "temp",
                "unit": "c",
                "values": [
                    {
                        "time": 1482146800,
                        "value": 20
                    }
                ]
            },
            {
                "name": "wind",
                "unit": "km/h",
                "values": [
                    {
                        "time": 1482146833,
                        "value": 155
                    }
                ]
            }
        ]
    })

    db.stations.insert({
        "id": "2",
        "name": "station 2",
        "contact": {
            "mail": "station2@mail.com"
        },
        "geometry": {
            "coordinates": [
                100,
                -50
            ],
            "type": "Point"
        },
        "measurements": [
            {
                "name": "temp",
                "unit": "c",
                "values": [
                    {
                        "time": 1482146911,
                        "value": 35
                    },
                    {
                        "time": 1482146935,
                        "value": 25
                    }
                ]
            },
            {
                "name": "wind",
                "unit": "km/h",
                "values": [
                    {
                        "time": 1482146964,
                        "value": 80
                    }
                ]
            },
            {
                "name": "pression",
                "unit": "pa",
                "values": [
                    {
                        "time": 1482147026,
                        "value": 1019
                    },
                    {
                        "time": 1482147051,
                        "value": 1015
                    }
                ]
            }
        ]
    })

    db.stations.createIndex({
        "geometry": "2dsphere"
    })

This is the schema that will be used to do the mappings in app-schema:

.. code-block:: xml

    <xs:schema version="1.0" xmlns:xs="http://www.w3.org/2001/XMLSchema"
               xmlns:gml="http://www.opengis.net/gml"
               xmlns:st="http://www.stations.org/1.0"
               targetNamespace="http://www.stations.org/1.0"
               elementFormDefault="qualified" attributeFormDefault="unqualified">

      <xs:import namespace="http://www.opengis.net/gml"
                 schemaLocation="http://schemas.opengis.net/gml/3.2.1/gml.xsd"/>

      <xs:complexType name="ContactType">
        <xs:sequence>
          <xs:element name="mail" minOccurs="0" maxOccurs="1" type="xs:string"/>
        </xs:sequence>
      </xs:complexType>

      <xs:complexType name="MeasurementPropertyType">
        <xs:sequence minOccurs="0">
          <xs:element ref="st:Measurement"/>
        </xs:sequence>
        <xs:attributeGroup ref="gml:AssociationAttributeGroup"/>
      </xs:complexType>

      <xs:complexType name="MeasurementType" abstract="true">
        <xs:sequence>
          <xs:element name="name" minOccurs="1" maxOccurs="1" type="xs:string"/>
          <xs:element name="unit" minOccurs="1" maxOccurs="1" type="xs:string"/>
          <xs:element name="values" minOccurs="1" maxOccurs="unbounded" type="st:ValuePropertyType"/>
        </xs:sequence>
      </xs:complexType>

      <xs:complexType name="ValuePropertyType">
        <xs:sequence minOccurs="0">
          <xs:element ref="st:Value"/>
        </xs:sequence>
        <xs:attributeGroup ref="gml:AssociationAttributeGroup"/>
      </xs:complexType>

      <xs:complexType name="ValueType">
        <xs:sequence>
          <xs:element name="timestamp" minOccurs="1" maxOccurs="1" type="xs:long"/>
          <xs:element name="value" minOccurs="1" maxOccurs="1" type="xs:double"/>
        </xs:sequence>
      </xs:complexType>

      <xs:complexType name="StationFeatureType">
        <xs:complexContent>
          <xs:extension base="gml:AbstractFeatureType">
            <xs:sequence>
              <xs:element name="name" minOccurs="1" maxOccurs="1" type="xs:string"/>
              <xs:element name="contact" minOccurs="0" maxOccurs="1" type="st:ContactType"/>
              <xs:element name="measurement" minOccurs="0" maxOccurs="unbounded" type="st:MeasurementPropertyType"/>
              <xs:element name="geometry" type="gml:GeometryPropertyType" minOccurs="0" maxOccurs="1"/>
            </xs:sequence>
          </xs:extension>
        </xs:complexContent>
      </xs:complexType>

      <xs:element name="StationFeature" type="st:StationFeatureType"  substitutionGroup="gml:_Feature"/>
      <xs:element name="Measurement" type="st:MeasurementType"  substitutionGroup="gml:_Feature"/>
      <xs:element name="Value" type="st:ValueType"  substitutionGroup="gml:_Feature"/>

    </xs:schema>

Mappings
--------

MongoDB Store
^^^^^^^^^^^^^

When configuring app-schema mappings for a MongoDB source some connection parameters tweaks might be needed, in order to ensure that the full set of recognized and made available to the mapping.
The setup of a MongoDB Store implies the creation of a Mongo schema, inferred from the db collection. 
This process by default will use a random Mongo object from the collection. If that object doesn't contain all attributes of interest, the result will be an incomplete schema.  
This behaviour can thus be controlled by means of the following two parameters, which should be provided inside the ``<parameters>`` element under the ``<DataStore>`` node:

* ``objs_id_schema``, which specifies a comma separeted list of MongoDB JSON object to be used to build the schema (not needed if ``max_objs_schema`` is present).

.. code-block:: xml

  <Parameter>
    <name>objs_id_schema</name>
    <value>6eb85d889396eb0475f815ef,6eb85d889396eb0475f815eg</value>
  </Parameter>


* ``max_objs_schema``, which specifies the max number of MongoDB JSON object to be used to build the schema and where a value of ``-1`` means all the objects present in the collection (not needed if ``objs_id_schema`` is present).

.. code-block:: xml

  <Parameter>
    <name>max_objs_schema</name>
    <value>-1</value>
  </Parameter>


Both parameters can also be specified via the REST API, see :ref:`Cleaning schemas on internal MongoDB stores <rest_App-Schema>` for more details.

Nested elements
^^^^^^^^^^^^^^^

MongoDB objects may contain nested elements and nested collections. The following three functions make possible to select nested elements and link nested collections using a JSON path:

.. list-table::
   :widths: 20 30 50

   * - **Function**
     - **Example**
     - **Description**
   * - jsonSelect
     - jsonSelect('contact.mail')
     - Used to retrieve the value for the mapping from a MongoDB object.  
   * - collectionLink
     - collectionLink('measurements.values')
     - Used when chaining entities with a nested collection.
   * - collectionId
     - collectionId()
     - Instructs the mapper to generate a ID for the nested collection.
   * - nestedCollectionLink
     - nestedCollectionLink()
     - Used on the nested collection to create a link with the parent feature.



Mappings file example
^^^^^^^^^^^^^^^^^^^^^

A station data is composed of some meta-information about the station and a list of measurements. Each measurement as some meta-information and contains a list of values. The mappings will contain three top entities: the station, the measurements and the values.

Follows a the complete mappings file:

.. code-block:: xml

  <?xml version="1.0" encoding="UTF-8"?>
  <as:AppSchemaDataAccess xmlns:as="http://www.geotools.org/app-schema"
                        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://www.geotools.org/app-schema AppSchemaDataAccess.xsd">
  <namespaces>
    <Namespace>
      <prefix>st</prefix>
      <uri>http://www.stations.org/1.0</uri>
    </Namespace>
    <Namespace>
      <prefix>gml</prefix>
      <uri>http://www.opengis.net/gml</uri>
    </Namespace>
  </namespaces>

  <sourceDataStores>
    <DataStore>
      <id>data_source</id>
      <parameters>
        <Parameter>
          <name>data_store</name>
          <value>mongodb://{mongoHost}:{mongoPort}/{dataBaseName}</value>
        </Parameter>
        <Parameter>
          <name>namespace</name>
          <value>http://www.stations.org/1.0</value>
        </Parameter>
        <Parameter>
          <name>schema_store</name>
          <value>file:{schemaStore}</value>
        </Parameter>
        <Parameter>
          <name>data_store_type</name>
          <value>complex</value>
        </Parameter>
        <Parameter>
            <name>max_objs_schema</name>
            <value>-1</value>
        </Parameter>
      </parameters>
    </DataStore>
  </sourceDataStores>

  <targetTypes>
    <FeatureType>
      <schemaUri>stations.xsd</schemaUri>
    </FeatureType>
  </targetTypes>

  <typeMappings>
    <FeatureTypeMapping>
      <sourceDataStore>data_source</sourceDataStore>
      <sourceType>{collectionName}</sourceType>
      <targetElement>st:StationFeature</targetElement>
      <attributeMappings>
        <AttributeMapping>
          <targetAttribute>st:StationFeature</targetAttribute>
          <idExpression>
            <OCQL>jsonSelect('id')</OCQL>
          </idExpression>
        </AttributeMapping>
        <AttributeMapping>
          <targetAttribute>st:name</targetAttribute>
          <sourceExpression>
            <OCQL>jsonSelect('name')</OCQL>
          </sourceExpression>
        </AttributeMapping>
        <AttributeMapping>
          <targetAttribute>st:contact/st:mail</targetAttribute>
          <sourceExpression>
            <OCQL>jsonSelect('contact.mail')</OCQL>
          </sourceExpression>
        </AttributeMapping>
        <AttributeMapping>
          <targetAttribute>st:measurement</targetAttribute>
          <sourceExpression>
            <OCQL>collectionLink('measurements')</OCQL>
            <linkElement>aaa</linkElement>
            <linkField>FEATURE_LINK[1]</linkField>
          </sourceExpression>
          <isMultiple>true</isMultiple>
        </AttributeMapping>
        <AttributeMapping>
          <targetAttribute>st:geometry</targetAttribute>
          <sourceExpression>
            <OCQL>jsonSelect('geometry')</OCQL>
          </sourceExpression>
        </AttributeMapping>
      </attributeMappings>
    </FeatureTypeMapping>
    <FeatureTypeMapping>
      <sourceDataStore>data_source</sourceDataStore>
      <sourceType>{collectionName}</sourceType>
      <mappingName>aaa</mappingName>
      <targetElement>st:Measurement</targetElement>
      <attributeMappings>
        <AttributeMapping>
          <targetAttribute>st:Measurement</targetAttribute>
          <idExpression>
            <OCQL>collectionId()</OCQL>
          </idExpression>
        </AttributeMapping>
        <AttributeMapping>
          <targetAttribute>st:name</targetAttribute>
          <sourceExpression>
            <OCQL>jsonSelect('name')</OCQL>
          </sourceExpression>
        </AttributeMapping>
        <AttributeMapping>
          <targetAttribute>st:unit</targetAttribute>
          <sourceExpression>
            <OCQL>jsonSelect('unit')</OCQL>
          </sourceExpression>
        </AttributeMapping>
        <AttributeMapping>
          <targetAttribute>st:values</targetAttribute>
          <sourceExpression>
            <OCQL>collectionLink('values')</OCQL>
            <linkElement>st:Value</linkElement>
            <linkField>FEATURE_LINK[2]</linkField>
          </sourceExpression>
          <isMultiple>true</isMultiple>
        </AttributeMapping>
        <AttributeMapping>
          <targetAttribute>FEATURE_LINK[1]</targetAttribute>
          <sourceExpression>
            <OCQL>nestedCollectionLink()</OCQL>
          </sourceExpression>
        </AttributeMapping>
      </attributeMappings>
    </FeatureTypeMapping>
    <FeatureTypeMapping>
      <sourceDataStore>data_source</sourceDataStore>
      <sourceType>{collectionName}</sourceType>
      <targetElement>st:Value</targetElement>
      <attributeMappings>
        <AttributeMapping>
          <targetAttribute>st:Value</targetAttribute>
          <idExpression>
            <OCQL>collectionId()</OCQL>
          </idExpression>
        </AttributeMapping>
        <AttributeMapping>
          <targetAttribute>st:timestamp</targetAttribute>
          <sourceExpression>
            <OCQL>jsonSelect('time')</OCQL>
          </sourceExpression>
        </AttributeMapping>
        <AttributeMapping>
          <targetAttribute>st:value</targetAttribute>
          <sourceExpression>
            <OCQL>jsonSelect('value')</OCQL>
          </sourceExpression>
        </AttributeMapping>
        <AttributeMapping>
          <targetAttribute>FEATURE_LINK[2]</targetAttribute>
          <sourceExpression>
            <OCQL>nestedCollectionLink()</OCQL>
          </sourceExpression>
        </AttributeMapping>
      </attributeMappings>
    </FeatureTypeMapping>
  </typeMappings>

  </as:AppSchemaDataAccess>

The mappings for the attributes are straightforward, for example the following mapping:

.. code-block:: xml

    <AttributeMapping>
        <targetAttribute>st:contact/st:mail</targetAttribute>
        <sourceExpression>
            <OCQL>jsonSelect('contact.mail')</OCQL>
        </sourceExpression>
    </AttributeMapping>

The mapping above defines that the contact mail for a station will be available at the JSON path ``contact.mail`` and that the correspondent XML schema element is the XPATH ``st:contact/st:mail``.

The feature chaining is a little bit more complex. Let's take as an example the chaining between ``StationFeature`` and ``Measurement`` features. In the ``StationFeature`` feature type the link to the Measurement entity is defined with the following mapping:

.. code-block:: xml

    <AttributeMapping>
        <targetAttribute>st:measurement</targetAttribute>
        <sourceExpression>
            <OCQL>collectionLink('measurements')</OCQL>
            <linkElement>st:Measurement</linkElement>
            <linkField>FEATURE_LINK[1]</linkField>
        </sourceExpression>
        <isMultiple>true</isMultiple>
    </AttributeMapping>

and in the ``Measurement`` feature type the link to the parent feature is defined with the following mapping:

.. code-block:: xml

    <AttributeMapping>
        <targetAttribute>FEATURE_LINK[1]</targetAttribute>
        <sourceExpression>
            <OCQL>nestedCollectionLink()</OCQL>
        </sourceExpression>
    </AttributeMapping>

With the two mapping above we tie the two features types together. When working with a MongoDB data store this mappings will always be petty much the same, only the nested collection path and the feature link index need to be updated. Note that the JSON path of the nested collections attributes are relative to the parent.

Querying
--------

To create an MongoDB app-schema layer in GeoServer, the app-schema extension and the mongo-complex extension needs to be installed.

A workspace for each name space declared in the mappings file needs to be created, in this case the workspace ``st`` with URI ``http://www.stations.org/1.0`` needs to be created. No need to create a ``gml`` workspace.  

Creating a MongoDB app-schema layer is similar to any other app-schema layer, just create an app-schema store pointing to the correct mappings file and select the layer correspondent to the top entity, in this case ``st:StationFeature``.

Is possible to query with WFS complex features encoded in GML and GeoJson  using complex features filtering capabilities.
For example, querying all the stations that have a measurement value with a time stamp superior to ``1482146964``:

.. code-block:: xml

    <wfs:Query typeName="st:StationFeature">
        <ogc:Filter>
            <ogc:Filter>
                <ogc:PropertyIsGreaterThan>
                        <ogc:PropertyName>  
                            st:StationFeature/st:measurement/st:values/st:timestamp
                        </ogc:PropertyName>
                        <ogc:Literal>
                            1482146964
                        </ogc:Literal>
                    </ogc:PropertyIsGreaterThan>
            </ogc:Filter>
        </ogc:Filter>
    </wfs:Query>
