.. _app-schema.mapping-file:

Mapping File
============

An app-schema feature type is configured using a mapping file that defines the data source for the feature and the mappings from the source data to XPaths in the output XML.


Outline
-------

Here is an outline of a mapping file::

    <?xml version="1.0" encoding="UTF-8"?>
    <as:AppSchemaDataAccess xmlns:as="http://www.geotools.org/app-schema"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.geotools.org/app-schema AppSchemaDataAccess.xsd">
        <namespaces>...</namespaces>
        <includedTypes>...</includedTypes>
        <sourceDataStores>...</sourceDataStores>
        <catalog>...</catalog>
        <targetTypes...</targetTypes>
        <typeMappings>...</typeMappings>
    </as:AppSchemaDataAccess>

* ``namespaces`` defines all the namespace prefixes used in the mapping file.

* ``includedTypes`` (optional) defines all the included non-feature type mapping file locations that are referred in the mapping file.

* ``sourceDataStores`` provides the configuration information for the source data stores.

* ``catalog`` is the location of the OASIS Catalog used to resolve XML Schema locations.

* ``targetTypes`` is the location of the XML Schema that defines the feature type.

* ``typeMappings`` give the relationships between the fields of the source data store and the elements of the output complex feature.


Mapping file schema
```````````````````

* ``AppSchemaDataAccess.xsd`` is optional because it is not used by GeoServer. The presence of ``AppSchemaDataAccess.xsd`` in the same folder as the mapping file enables XML editors to observe its grammar and provide contextual help.


Settings
--------


namespaces
``````````

The ``namespaces`` section defines all the XML namespaces used in the mapping file::

    <Namespace>
        <prefix>gsml</prefix>
        <uri>urn:cgi:xmlns:CGI:GeoSciML:2.0</uri>
    </Namespace>
    <Namespace>
        <prefix>gml</prefix>
        <uri>http://www.opengis.net/gml</uri>
    </Namespace>
    <Namespace>
        <prefix>xlink</prefix>
        <uri>http://www.w3.org/1999/xlink</uri>
    </Namespace>


includedTypes (optional)
````````````````````````

Non-feature types (eg. gsml:CompositionPart is a data type that is nested in gsml:GeologicUnit) may be mapped separately for its reusability, but we don't want to configure it as a feature type as we don't want to individually access it. Related feature types don't need to be explicitly included here as it would have its own workspace configuration for GeoServer to find it. The location path in ``Include`` tag is relative to the mapping file. For an example, if gsml:CompositionPart configuration file is located in the same directory as the gsml:GeologicUnit configuration::

    <includedTypes>
        <Include>gsml_CompositionPart.xml</Include>
    </includedTypes>


sourceDataStores
````````````````

Every mapping file requires at least one data store to provide data for features. app-schema reuses GeoServer data stores, so there are many available types. See :ref:`app-schema.data-stores` for details of data store configuration. For example::

    <sourceDataStores>
        <DataStore>
            <id>datastore</id>
            <parameters>
                ...
            </parameters>
        </DataStore>
        ...
    </sourceDataStores>

If you have more than one ``DataStore`` in a mapping file, be sure to give them each a distinct ``id``.


catalog (optional)
``````````````````

The location of an OASIS XML Catalog configuration file, given as a path relative to the mapping file. See :ref:`app-schema.app-schema-resolution` for more information. For example::

    <catalog>../../../schemas/catalog.xml</catalog>


targetTypes
```````````

The ``targetTypes`` section lists all the application schemas required to define the mapping. Typically only one is required. For example::

    <targetTypes>
        <FeatureType>
            <schemaUri>http://www.geosciml.org/geosciml/2.0/xsd/geosciml.xsd</schemaUri>
        </FeatureType>
    </targetTypes>


Mappings
--------


typeMappings and FeatureTypeMapping
```````````````````````````````````

The ``typeMappings`` section is the heart of the app-schema module. It defines the mapping from simple features to the the nested structure of one or more simple features. It consists of a list of ``FeatureTypeMapping`` elements, which each define one output feature type. For example::

    <typeMappings>
        <FeatureTypeMapping>
            <mappingName>mappedfeature1</mappingName>
            <sourceDataStore>datastore</sourceDataStore>
            <sourceType>mappedfeature</sourceType>
            <targetElement>gsml:MappedFeature</targetElement>
            <isDenormalised>true</isDenormalised>
            <defaultGeometry>gsml:MappedFeature/gsml:shape/gml:Polygon</defaultGeometry>
            <attributeMappings>
                <AttributeMapping>
                    ...

* ``mappingName`` is an optional tag, to identify the mapping in :ref:`app-schema.feature-chaining` when there are multiple FeatureTypeMapping instances for the same type. This is solely for feature chaining purposes, and would not work for identifying top level features.
* ``sourceDataStore`` must be an identifier you provided when you defined a source data store the ``sourceDataStores`` section.
* ``sourceType`` is the simple feature type name. For example:

    * a table or view name, lowercase for PostGIS, uppercase for Oracle.
    * a property file name (without the .properties suffix)

* ``targetElement`` is the the element name in the target application schema. This is the same as the WFS feature type name.

* ``isDenormalised`` is an optional tag (default true) to indicate whether this type contains denormalised data or not. If data is not denormalised, then app-schema will build a more efficient query to apply the global feature limit.  When combined with a low global feature limit (via `Services --> WFS`), setting this option to false can prevent unnecessary processing and database lookups from taking place.

* ``defaultGeometry`` can be used to explicitly define the attribute of the feature type that should be used as the default geometry, this is more relevant in WMS than WFS. The default geometry XML path can reference any attribute of the feature type, exactly the same path that would be used to reference the desired property in a OGC filter. The path can reference a nested attribute belonging to a chained feature having a zero or one relationship with the root feature type.

attributeMappings and AttributeMapping
``````````````````````````````````````

``attributeMappings`` comprises a list of ``AttributeMapping`` elements::

    <AttributeMapping>
        <targetAttribute>...</targetAttribute>
        <idExpression>...</idExpression>
        <sourceExpression>...</sourceExpression>
        <targetAttributeNode>...</targetAttributeNode>
        <isMultiple>...</isMultiple>
        <ClientProperty>...</ClientProperty>
    </AttributeMapping>


targetAttribute
```````````````

``targetAttribute`` is the XPath to the output element, in the context of the target element. For example, if the containing mapping is for a feature, you should be able to map a ``gml:name`` property by setting the target attribute::

    <targetAttribute>gml:name</targetAttribute>

Multivalued attributes resulting from :ref:`app-schema.denormalised-sources` are automatically encoded. If you wish to encode multivalued attributes from different input columns as a specific instance of an attribute, you can use a (one-based) index. For example, you can set the third ``gml:name`` with::

    <targetAttribute>gml:name[3]</targetAttribute>

The reserved name ``FEATURE_LINK`` is used to map data that is not encoded in XML but is required for use in :ref:`app-schema.feature-chaining`.


idExpression (optional)
```````````````````````

A CQL expression that is used to set the custom ``gml:id`` of the output feature type. This should be the name of a database column on its own. Using functions would cause an exception because it is not supported with the default joining implementation. 
 
.. note:: 
         Every feature must have a ``gml:id``. This requirement is an implementation limitation (strictly, ``gml:id`` is optional in GML). 

         * If idExpression is unspecified, ``gml:id`` will be ``<the table name>.<primary key>``, e.g. ``MAPPEDFEATURE.1``.
      
         * In the absence of primary keys, this will be ``<the table name>.<generated gml id>``, e.g. ``MAPPEDFEATURE.fid--46fd41b8_1407138b56f_-7fe0``. 
         
         * If using property files instead of database tables, the default ``gml:id`` will be the row key found before the equals ("=") in the property file, e.g. the feature with row "mf1=Mudstone|POINT(1 2)|..." will have gml:id ``mf1``.

.. note:: ``gml:id`` must be an `NCName <http://www.w3.org/TR/1999/REC-xml-names-19990114/#NT-NCName>`_.


sourceExpression (optional)
```````````````````````````

Use a ``sourceExpression`` tag to set the element content from source data. For example, to set the element content from a column called ``DESCRIPTION``::

    <sourceExpression><OCQL>DESCRIPTION</OCQL></sourceExpression>

If ``sourceExpression`` is not present, the generated element is empty (unless set by another mapping).

You can use CQL expressions to calculate the content of the element. This example concatenated strings from two columns and a literal::

    <sourceExpression>
        <OCQL>strConCat(FIRST , strConCat(' followed by ', SECOND))</OCQL>
    </sourceExpression>

You can also use :ref:`app-schema.cql-functions` for vocabulary translations.

.. warning:: Avoid use of CQL expressions for properties that users will want to query, because the current implementation cannot reverse these expressions to generate efficient SQL, and will instead read all features to calculate the property to find the features that match the filter query. Falling back to brute force search makes queries on CQL-calculated expressions very slow. If you must concatenate strings to generate content, you may find that doing this in your database is much faster.


linkElement and linkField (optional)
````````````````````````````````````

The presence of ``linkElement`` and ``linkField`` change the meaning of ``sourceExpression`` to a :ref:`app-schema.feature-chaining` mapping, in which the source of the mapping is the feature of type ``linkElement`` with property ``linkField`` matching the expression. For example, the following ``sourceExpression`` uses as the result of the mapping the (possibly multivalued) ``gsml:MappedFeature`` for which ``gml:name[2]`` is equal to the value of ``URN`` for the source feature. This is in effect a foreign key relation::

    <sourceExpression>
        <OCQL>URN</OCQL>
        <linkElement>gsml:MappedFeature</linkElement>
        <linkField>gml:name[2]</linkField>
    </sourceExpression>

The feature type ``gsml:MappedFeature`` might be defined in another mapping file. The ``linkField`` can be ``FEATURE_LINK`` if you wish to relate the features by a property not exposed in XML.
See :ref:`app-schema.feature-chaining` for a comprehensive discussion.

For special cases, ``linkElement`` could be an OCQL function, and ``linkField`` could be omitted. 
See :ref:`app-schema.polymorphism` for further information.

..  _app-schema.mapping-file.targetAttributeNode:

targetAttributeNode (optional)
``````````````````````````````
``targetAttributeNode`` is required wherever a property type contains an abstract element and app-schema cannot determine the type of the enclosed attribute. 

In this example, ``om:result`` is of ``xs:anyType``, which is abstract. We can use ``targetAttributeNode`` to set the type of the property type to a type that encloses a non-abstract element::

    <AttributeMapping>
          <targetAttribute>om:result</targetAttribute>
          <targetAttributeNode>gml:MeasureType<targetAttributeNode>
          <sourceExpression>
              <OCQL>TOPAGE</OCQL>
          </sourceExpression>
          <ClientProperty>
              <name>xsi:type</name>
              <value>'gml:MeasureType'</value>
          </ClientProperty>
          <ClientProperty>
              <name>uom</name> 
              <value>'http://www.opengis.net/def/uom/UCUM/0/Ma'</value>
          </ClientProperty> 
    </AttributeMapping>

If the casting type is complex, the specific type is implicitly determined by the XPath in targetAttribute and targetAttributeNode is not required.
E.g., in this example ``om:result`` is automatically specialised as a MappedFeatureType::

    <AttributeMapping>
          <targetAttribute>om:result/gsml:MappedFeature/gml:name</targetAttribute>
          <sourceExpression>
              <OCQL>NAME</OCQL>
          </sourceExpression>
    </AttributeMapping>

Although it is not required, we may still specify targetAttributeNode for the root node, and map the children attributes as per normal. 
This mapping must come before the mapping for the enclosed elements. By doing this, app-schema will report an exception if a mapping is specified for any of the children attributes that violates the type in targetAttributeNode.
E.g.::

    <AttributeMapping>
          <targetAttribute>om:result</targetAttribute>
          <targetAttributeNode>gsml:MappedFeatureType<targetAttributeNode>
    </AttributeMapping> 
    <AttributeMapping>
          <targetAttribute>om:result/gsml:MappedFeature/gml:name</targetAttribute>
          <sourceExpression>
              <OCQL>NAME</OCQL>
          </sourceExpression>
    </AttributeMapping>

Note that the GML encoding rules require that complex types are never the direct property of another complex type; they are always contained in a property type to ensure that their type is encoded in a surrounding element. Encoded GML is always type/property/type/property. This is also known as the GML "striping" rule. The consequence of this for app-schema mapping files is that ``targetAttributeNode`` must be applied to the property and the type must be set to the XSD property type, not to the type of the contained attribute (``gsml:CGI_TermValuePropertyType`` not ``gsml:CGI_TermValueType``). Because the XPath refers to a property type not the encoded content, ``targetAttributeNode`` appears in a mapping with ``targetAttribute`` and no other elements when using with complex types.

The XML encoder will encode nested complex features that are mapped to a complex type that does not respect the GML striping rule. The Java configuration property ``encoder.relaxed`` can be set to ``false`` to disable this behavior.

encodeIfEmpty (optional)
````````````````````````

The ``encodeIfEmpty`` element will determine if an attribute will be encoded if it contains a null or empty value. By default ``encodeIfEmpty`` is set to false therefore any attribute that does not contain a value will be skipped::

	<encodeIfEmpty>true</encodeIfEmpty>

``encodeIfEmpty`` can be used to bring up attributes that only contain client properties such as ``xlink:title``.

isMultiple (optional)
`````````````````````

The ``isMultiple`` element states whether there might be multiple values for this attribute, coming from denormalised rows. Because the default value is ``false`` and it is omitted in this case, it is most usually seen as::

    <isMultiple>true</isMultiple>

For example, the table below is denormalised with ``NAME`` column having multiple values:

======== ======================== =================================
ID       NAME                     DESCRIPTION
======== ======================== =================================
gu.25678 Yaugher Volcanic Group 1 Olivine basalt, tuff, microgabbro
gu.25678 Yaugher Volcanic Group 2 Olivine basalt, tuff, microgabbro
======== ======================== =================================

The configuration file specifies ``isMultiple`` for ``gml:name`` attribute that is mapped to the ``NAME`` column::

    <AttributeMapping>
        <targetAttribute>gml:name</targetAttribute>                       
        <sourceExpression>
            <OCQL>NAME</OCQL>
	</sourceExpression>					
	<isMultiple>true</isMultiple>
	<ClientProperty>
	    <name>codeSpace</name>
	    <value>'urn:ietf:rfc:2141'</value>
	</ClientProperty>
    </AttributeMapping>

The output produces multiple ``gml:name`` attributes for each feature grouped by the id::

    <gsml:GeologicUnit gml:id="gu.25678">
        <gml:description>Olivine basalt, tuff, microgabbro</gml:description>
        <gml:name codeSpace="urn:ietf:rfc:2141">Yaugher Volcanic Group 1</gml:name>
        <gml:name codeSpace="urn:ietf:rfc:2141">Yaugher Volcanic Group 2</gml:name>
     ...

isList (optional)
`````````````````

The ``isList`` element states whether there might be multiple values for this attribute, concatenated as a list. The usage is similar with ``isMultiple``, except the values appear concatenated inside a single node instead of each value encoded in a separate node. Because the default value is ``false`` and it is omitted in this case, it is most usually seen as::

    <isList>true</isList>

For example, the table below has multiple ``POSITION`` for each feature:

===== ========
 ID   POSITION
===== ========
ID1.2  1948-05
ID1.2  1948-06
ID1.2  1948-07
ID1.2  1948-08
ID1.2  1948-09
===== ========

The configuration file uses ``isList`` on ``timePositionList`` attribute mapped to ``POSITION`` column::

    <AttributeMapping>
        <targetAttribute>csml:timePositionList</targetAttribute>
        <sourceExpression>
	    <OCQL>POSITION</OCQL>
        </sourceExpression>
        <isList>true</isList>
    </AttributeMapping>

The output produced::

    <csml:pointSeriesDomain>
        <csml:TimeSeries gml:id="ID1.2">
            <csml:timePositionList>1949-05 1949-06 1949-07 1949-08 1949-09</csml:timePositionList>
        </csml:TimeSeries>
    </csml:pointSeriesDomain>


ClientProperty (optional, multivalued)
``````````````````````````````````````

A mapping can have one or more ``ClientProperty`` elements which set XML attributes on the mapping target. Each ``ClientProperty`` has a ``name`` and a ``value`` that is an arbitrary CQL expression. No ``OCQL`` element is used inside ``value``.

This example of a ``ClientProperty`` element sets the ``codeSpace`` XML attribute to the literal string ``urn:ietf:rfc:2141``. Note the use of single quotes around the literal string. This could be applied to any target attribute of GML CodeType::

    <ClientProperty>
        <name>codeSpace</name>
        <value>'urn:ietf:rfc:2141'</value>
    </ClientProperty>

When the GML association pattern is used to encode a property by reference, the ``xlink:href`` attribute is set and the element is empty. This ``ClientProperty`` element sets the ``xlink:href`` XML attribute to to the value of the ``RELATED_FEATURE_URN`` field in the data source (for example, a column in an Oracle database table). This mapping could be applied to any property type, such a ``gml:FeaturePropertyType``, or other type modelled on the GML association pattern::

    <ClientProperty>
        <name>xlink:href</name>
        <value>RELATED_FEATURE_URN</value>
    </ClientProperty>

See the discussion in :ref:`app-schema.feature-chaining` for the special case in which ``xlink:href`` is created for multivalued properties by reference.


CQL
---

CQL functions enable data conversion and conditional behaviour to be specified in mapping files.

* See :ref:`app-schema.cql-functions` for information on additional functions provided by the app-schema plugin.
* The uDig manual includes a list of CQL functions:

    * http://udig.refractions.net/confluence/display/EN/Constraint+Query+Language

* CQL string literals are enclosed in single quotes, for example ``'urn:ogc:def:nil:OGC:missing'``.


Database identifiers
--------------------

When referring to database table/view names or column names, use:

* lowercase for PostGIS
* UPPERCASE for Oracle Spatial and ArcSDE


.. _app-schema.denormalised-sources:

Denormalised sources
--------------------

Multivalued properties from denormalised sources (the same source feature ID appears more than once) are automatically encoded. For example, a view might have a repeated ``id`` column with varying ``name`` so that an arbitrarily large number of ``gml:name`` properties can be encoded for the output feature.

.. warning:: Denormalised sources must grouped so that features with duplicate IDs are provided without any intervening features. This can be achieved by ensuring that denormalised source features are sorted by ID. Failure to observe this restriction will result in data corruption. This restriction is however not necessary when using :ref:`app-schema.joining` because then ordering will happen automatically.

Attributes with cardinality 1..N
--------------------------------

Consider the following two tables, the first table contains information related to meteorological stations:

======== ========== 
ID       NAME                    
======== ==========
st.1     Station 1  
st.2     Station 2  
======== ========== 

The second table contains tags that are associated with meteorological stations:

======== =========== ============ =====
ID       STATION_ID  TAG          CODE
======== =========== ============ =====
tg.1     st.1        temperature  X1Y
tg.2     st.1        wind         X2Y
tg.3     st.2        pressure     X3Y
======== =========== ============ =====

A station can have multiple tags, establishing a one to many relationship between stations and tags, the GML representation of the first station should look like this::

  (...)
  <st:Station gml:id="st.1">
    <st:name>Station 1</st:name>
    <st:tag st:code="X1Y">temperature</st:tag>
    <st:tag st:code="X2Y">wind</st:tag>
  </st:Station_gml32>
  (...)

Mappings with a one to many relationship are supported with a custom syntax in JDBC based data stores and Apache Solr data store. 

SQL based data stores
`````````````````````

When using JDBC based data stores attributes with a 1..N relationship can be mapped like this::

  (...)
  <AttributeMapping>
    <targetAttribute>st:tag</targetAttribute>
    <jdbcMultipleValue>
      <sourceColumn>ID</sourceColumn>
      <targetTable>TAGS</targetTable>
      <targetColumn>STATION_ID</targetColumn>
      <targetValue>TAG</targetValue>
    </jdbcMultipleValue>
    <ClientProperty>
      <name>st:code</name>
      <value>CODE</value>
    </ClientProperty>
  </AttributeMapping>
  (...)

The ``targetValue`` refers to the value of the ``<st:tag>`` element, the client property is mapped with the usual syntax. Behind the scenes App-Schema will take care of associating the ``st:code`` attribute value with the correct tag. 

Another variant of this feature can be used for nested elements on an unbounded anonymous sequence, Using 'anonymousAttribute' element definition
for generating child elements and values inside an anonymous umbounded sequence::

  (...)
  <AttributeMapping>
    <targetAttribute>st:tag</targetAttribute>
    <jdbcMultipleValue>
      <sourceColumn>ID</sourceColumn>
      <targetTable>TAGS</targetTable>
      <targetColumn>STATION_ID</targetColumn>
    </jdbcMultipleValue>
    <anonymousAttribute>
      <name>st:code</name>
      <value>CODE</value>
    </anonymousAttribute>
  </AttributeMapping>
  (...)
  
In this case 'st:code' element children will be generated with the computed client property value::

  (...)
  <st:Station gml:id="st.1">
    <st:name>Station 1</st:name>
    <st:tag>
      <st:code>X1Y</st:code>
      <st:code>X2Y</st:code>
    </st:tag>
  </st:Station_gml32>
  (...)

Having an schema with anonymous unbounded sequence like ::
  
  <xs:complexType name="StationType">
    <xs:complexContent>
      <xs:extension base="gml:AbstractFeatureType">
        <xs:sequence>
          <xs:element name="name" type="xs:string"/>
          <xs:element name="contact" type="st:ContactPropertyType"/>
          <xs:element name="location" type="gml:GeometryPropertyType"/>
          <xs:element maxOccurs="unbounded" minOccurs="0" name="tag" type="st:TagType"/>
          <xs:element name="measurements" type="ms:MeasurementPropertyType" minOccurs="0" maxOccurs="unbounded"/>
          <xs:element name="maintainer" minOccurs="0">
            <xs:complexType>
              <xs:sequence maxOccurs="unbounded">
                <xs:element name="name" type="xs:string"/>
                <xs:element name="level" type="xs:integer" nillable="true"/>
                <xs:element name="classType" />
              </xs:sequence>
            </xs:complexType>
          </xs:element>
        </xs:sequence>
      </xs:extension>
    </xs:complexContent>
  </xs:complexType>
  
We could define the following mapping ::

     <FeatureTypeMapping>
      <sourceDataStore>stations_gml32</sourceDataStore>
      <sourceType>stations_gml32</sourceType>
      <targetElement>st_gml32:Station_gml32</targetElement>
      <attributeMappings>
        <AttributeMapping>
          <targetAttribute>st_gml32:Station_gml32</targetAttribute>
          <idExpression>
            <OCQL>ID</OCQL>
          </idExpression>
        </AttributeMapping>
        <AttributeMapping>
          <targetAttribute>st_gml32:name</targetAttribute>
          <sourceExpression>
            <OCQL>NAME</OCQL>
          </sourceExpression>
        </AttributeMapping>
        <AttributeMapping>
          <targetAttribute>st_gml32:location</targetAttribute>
          <sourceExpression>
            <OCQL>LOCATION</OCQL>
          </sourceExpression>
        </AttributeMapping>
        <AttributeMapping>
          <targetAttribute>st_gml32:maintainer</targetAttribute>
          <jdbcMultipleValue>
            <sourceColumn>ID</sourceColumn>
            <targetTable>MAINTAINERS_GML32</targetTable>
            <targetColumn>OWNER</targetColumn>
          </jdbcMultipleValue>
          <anonymousAttribute>
            <name>st_gml32:name</name>
            <value>NAME</value>
          </anonymousAttribute>
          <anonymousAttribute>
            <name>st_gml32:level</name>
            <value>LEVEL</value>
          </anonymousAttribute>
          <anonymousAttribute>
            <name>st_gml32:classType</name>
            <value>CLASSTYPE</value>
          </anonymousAttribute>
        </AttributeMapping>
        <AttributeMapping>
          <targetAttribute>st_gml32:measurements</targetAttribute>
          <sourceExpression>
            <OCQL>ID</OCQL>
            <linkElement>ms_gml32:Measurement_gml32</linkElement>
            <linkField>FEATURE_LINK[1]</linkField>
          </sourceExpression>
        </AttributeMapping>
      </attributeMappings>
    </FeatureTypeMapping>
    
So Geoserver will produce GML like this ::

    <st_gml32:Station_gml32 gml:id="st.2">
      <st_gml32:name>station2</st_gml32:name>
      <st_gml32:location>
        <gml:Point srsDimension="2" srsName="urn:ogc:def:crs:EPSG::4326">
          <gml:pos>-3 -2</gml:pos>
        </gml:Point>
      </st_gml32:location>
      <st_gml32:measurements>
        <ms_gml32:Measurement_gml32 gml:id="ms.3">
          <ms_gml32:name>pressure</ms_gml32:name>
          <ms_gml32:tag>pressure_tag</ms_gml32:tag>
        </ms_gml32:Measurement_gml32>
      </st_gml32:measurements>
      <st_gml32:maintainer>
        <st_gml32:name>mnt_c</st_gml32:name>
        <st_gml32:level>73</st_gml32:level>
        <st_gml32:classType>st_2_mnt_c</st_gml32:classType>
        <st_gml32:name>mnt_d</st_gml32:name>
        <st_gml32:level>74</st_gml32:level>
        <st_gml32:classType>st_2_mnt_d</st_gml32:classType>
        <st_gml32:name>mnt_e</st_gml32:name>
        <st_gml32:level>75</st_gml32:level>
        <st_gml32:classType>st_2_mnt_e</st_gml32:classType>
      </st_gml32:maintainer>
    </st_gml32:Station_gml32>

Apache Solr
```````````

When using Apache Solr data stores, 1..N relationships can be handled with ``multiValued`` fields and mapped like this::

  (...)
  <AttributeMapping>
    <targetAttribute>st:tag</targetAttribute>
    <solrMultipleValue>TAG</solrMultipleValue>
    <ClientProperty>
      <name>st:code</name>
      <value>CODE</value>
    </ClientProperty>
  </AttributeMapping>
  (...)

External Apache Solr Index
--------------------------

Is possible to use an external Apache Solr index to speed up text based searches. Consider the following WFS ``GetFeatureRequest``::

  <wfs:GetFeature service="WFS" version="2.0.0" xmlns:fes="http://www.opengis.net/fes/2.0" xmlns:gml="http://www.opengis.net/gml/3.2.1" xmlns:wfs="http://www.opengis.net/wfs/2.0">
    <wfs:Query typeNames="st:Station">
      <fes:Filter>
        <fes:PropertyIsLike escapeChar="!" singleChar="." wildCard="*">
          <fes:ValueReference>st:Station/st:measurement/st:Measurement/st:description</fes:ValueReference>
          <fes:Literal>*high*</fes:Literal>
        </fes:PropertyIsLike>
      </fes:Filter>
    </wfs:Query>
  </wfs:GetFeature>

This request will return all the stations that have at least one measurement that contains the text ``high`` in its description. This type of text based queries are (usually) quite expensive to execute in relational data bases.

Apache Solr is a well known open source project that aims to solve those type of performance issues, it allow us to index several fields and efficiently query those fields. Although Apache Solr allow us to index several types of fields, e.g. numbers or even latitudes \ longitudes, where it really shines is when dealing with text fields and text based queries.  

The goal of external indexes is to allow App-Schema to take advantage of Apache Solr text searches performance and at the same time to take advantage of relational databases relational capabilities. In practice, this means that the full data will be stored in the relational database and we will only need to index in Apache Solr the fields we need to improve our text based queries.

Using the stations use case as an example, our data will be stored in a relational database, e.g. PostgreSQL, and we will index the possible descriptions measurements values in an Apache Solr index.


Our mapping file will look like this::

  <as:AppSchemaDataAccess xmlns:as="http://www.geotools.org/app-schema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.geotools.org/app-schema AppSchemaDataAccess.xsd">
    <namespaces>
      <Namespace>
        <prefix>gml</prefix>
        <uri>http://www.opengis.net/gml/3.2</uri>
      </Namespace>
      <Namespace>
        <prefix>st</prefix>
        <uri>http://www.stations.org/1.0</uri>
      </Namespace>
    </namespaces>
    <sourceDataStores>
      <SolrDataStore>
        <id>stations_solr</id>
        <url>http://localhost:8983/solr/stations_index</url>
        <index name="stations_index"/>
      </SolrDataStore>
      <DataStore>
        <id>stations_db</id>
        <parameters>
          <Parameter>
            <name>dbtype</name>
            <value>postgisng</value>
          </Parameter>
        </parameters>
      </DataStore>
    </sourceDataStores>
    <targetTypes>
      <FeatureType>
        <schemaUri>./stations.xsd</schemaUri>
      </FeatureType>
    </targetTypes>
    <typeMappings>
      <FeatureTypeMapping>
        <sourceDataStore>stations_db</sourceDataStore>
        <sourceType>stations</sourceType>
        <targetElement>st:Station</targetElement>
        <!-- configure the index data store for this feature type -->
        <indexDataStore>stations_solr</indexDataStore>
        <indexType>stations_index</indexType>
        <attributeMappings>
          <AttributeMapping>
            <targetAttribute>st:Station</targetAttribute>
            <idExpression>
              <OCQL>id</OCQL>
            </idExpression>
            <!-- the Solr index field that matches this feature type table primary key -->
            <indexField>id</indexField>
          </AttributeMapping>
          <AttributeMapping>
            <targetAttribute>st:name</targetAttribute>
            <sourceExpression>
              <OCQL>name</OCQL>
            </sourceExpression>
          </AttributeMapping>
          <AttributeMapping>
            <targetAttribute>st:measurement</targetAttribute>
            <sourceExpression>
              <OCQL>id</OCQL>
              <linkElement>st:Measurement</linkElement>
              <linkField>FEATURE_LINK[1]</linkField>
            </sourceExpression>
            <isMultiple>true</isMultiple>
          </AttributeMapping>
        </attributeMappings>
      </FeatureTypeMapping>
      <FeatureTypeMapping>
        <sourceDataStore>stations_db</sourceDataStore>
        <sourceType>measurements</sourceType>
        <targetElement>st:Measurement</targetElement>
        <attributeMappings>
          <AttributeMapping>
            <targetAttribute>st:Measurement</targetAttribute>
            <idExpression>
              <OCQL>id</OCQL>
            </idExpression>
          </AttributeMapping>
          <AttributeMapping>
            <targetAttribute>st:description</targetAttribute>
            <sourceExpression>
              <OCQL>description</OCQL>
            </sourceExpression>
            <!-- the Solr index field that indexes the description possible values -->
            <indexField>description_txt</indexField>
          </AttributeMapping>
          <AttributeMapping>
            <targetAttribute>FEATURE_LINK[1]</targetAttribute>
            <sourceExpression>
              <OCQL>station_id</OCQL>
            </sourceExpression>
          </AttributeMapping>
        </attributeMappings>
      </FeatureTypeMapping>
    </typeMappings>
  </as:AppSchemaDataAccess> 

To be able to use an external Apache Solr index, we need at least to:

* **declare the Solr data store and the index**: this is done in the root feature type mapping, e.g. *st:Station*.

* **map the Solr index field that matches the database primary key**: this is done id mapping of the root feature type, e.g.``<indexField>id</indexField>``.

* **map each attribute that is indexed in Apache Solr**: this is done using the *indexField* element, e.g ``<indexField>description_txt</indexField>``.

Is worth mentioning that if an external Solr index was defined, App-Schema will always query the external Solr index first and then query the relational database.
