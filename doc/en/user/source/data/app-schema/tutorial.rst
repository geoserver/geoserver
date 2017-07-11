.. _app-schema.tutorial:

Tutorial
========

This tutorial demonstrates how to configure two complex feature types using the app-schema plugin and data from two property files.


GeoSciML
---------

This example uses `Geoscience Markup Language (GeoSciML) 2.0 <http://geosciml.org/doc/geosciml/2.0/documentation/html/>`_, a GML 3.1 application schema:

    *"GeoSciML is an application schema that specifies a set of feature-types and supporting structures for information used in the solid-earth geosciences."*

The tutorial defines two feature types:

#. ``gsml:GeologicUnit``, which describes "a body of material in the Earth".

#. ``gsml:MappedFeature``, which describes the representation on a map of a feature, in this case ``gsml:GeologicUnit``.

Because a single ``gsml:GeologicUnit`` can be observed at several distinct locations on the Earth's surface, it can have a multivalued ``gsml:occurrence`` property, each being a ``gsml:MappedFeature``.


Installation
------------

* Install GeoServer as usual.

* Install the app-schema plugin ``geoserver-*-app-schema-plugin.zip``:

    * Place the jar files in ``WEB-INF/lib``.

    * The ``tutorial`` folder contains the GeoServer configuraration (data directory) used for this tutorial.
    
        * Either replace your existing ``data`` directory with the tutorial data directory,
        
        * Or edit ``WEB-INF/web.xml`` to set ``GEOSERVER_DATA_DIR`` to point to the tutorial data directory. (Be sure to uncomment the section that sets ``GEOSERVER_DATA_DIR``.)

* Perform any configuration required by your servlet container, and then start the servlet. For example, if you are using Tomcat, configure a new context in ``server.xml`` and then restart Tomcat.

* The first time GeoServer starts with the tutorial configuration, it will download all the schema (XSD) files it needs and store them in the ``app-schema-cache`` folder in the data directory. **You must be connected to the internet for this to work.**


datastore.xml
-------------

Each data store configuration file ``datastore.xml`` specifies the location of a mapping file and triggers its loading as an app-schema data source. This file should not be confused with the source data store, which is specified inside the mapping file.

For ``gsml_GeologicUnit`` the file is ``workspaces/gsml/gsml_GeologicUnit/datastore.xml``::

    <dataStore>
        <id>gsml_GeologicUnit_datastore</id>
        <name>gsml_GeologicUnit</name>
        <enabled>true</enabled>
        <workspace>
            <id>gsml_workspace</id>
        </workspace>
        <connectionParameters>
            <entry key="namespace">urn:cgi:xmlns:CGI:GeoSciML:2.0</entry>
            <entry key="url">file:workspaces/gsml/gsml_GeologicUnit/gsml_GeologicUnit.xml</entry>
            <entry key="dbtype">app-schema</entry>
        </connectionParameters>
    </dataStore>


For ``gsml:MappedFeature`` the file is ``workspaces/gsml/gsml_MappedFeature/datastore.xml``::

    <dataStore>
        <id>gsml_MappedFeature_datastore</id>
        <name>gsml_MappedFeature</name>
        <enabled>true</enabled>
        <workspace>
            <id>gsml_workspace</id>
        </workspace>
        <connectionParameters>
            <entry key="namespace">urn:cgi:xmlns:CGI:GeoSciML:2.0</entry>
            <entry key="url">file:workspaces/gsml/gsml_MappedFeature/gsml_MappedFeature.xml</entry>
            <entry key="dbtype">app-schema</entry>
        </connectionParameters>
    </dataStore>

.. note:: Ensure that there is no whitespace inside an ``entry`` element.


Mapping files
-------------

Configuration of app-schema feature types is performed in mapping files:

* ``workspaces/gsml/gsml_GeologicUnit/gsml_GeologicUnit.xml``

* ``workspaces/gsml/gsml_MappedFeature/gsml_MappedFeature.xml``


Namespaces
``````````

Each mapping file contains namespace prefix definitions::

    <Namespace>
        <prefix>gml</prefix>
        <uri>http://www.opengis.net/gml</uri>
    </Namespace>
    <Namespace>
        <prefix>gsml</prefix>
        <uri>urn:cgi:xmlns:CGI:GeoSciML:2.0</uri>
    </Namespace>
    <Namespace>
        <prefix>xlink</prefix>
        <uri>http://www.w3.org/1999/xlink</uri>
    </Namespace>

Only those namespace prefixes used in the mapping file need to be declared, so the mapping file for ``gsml:GeologicUnit`` has less.


Source data store
`````````````````

The data for this tutorial is contained in two property files:

* ``workspaces/gsml/gsml_GeologicUnit/gsml_GeologicUnit.properties``

* ``workspaces/gsml/gsml_MappedFeature/gsml_MappedFeature.properties``

:ref:`data_java_properties` describes the format of property files.

For this example, each feature type uses an identical source data store configuration. This ``directory`` parameter indicates that the source data is contained in property files named by their feature type, in the same directory as the corresponding mapping file::

   <sourceDataStores>
        <DataStore>
            <id>datastore</id>
            <parameters>
                <Parameter>
                    <name>directory</name>
                    <value>file:./</value>
                </Parameter>
            </parameters>
        </DataStore>
    </sourceDataStores>

See :ref:`app-schema.data-stores` for a description of how to use other types of data stores such as databases.


Target types
````````````

Both feature types are defined by the same XML Schema, the top-level schema for GeoSciML 2.0. This is specified in the ``targetTypes`` section. The type of the output feature is defined in ``targetElement`` in the ``typeMapping`` section below::

    <targetTypes>
        <FeatureType>
            <schemaUri>http://www.geosciml.org/geosciml/2.0/xsd/geosciml.xsd</schemaUri>
        </FeatureType>
    </targetTypes>

In this case the schema is published, but because the OASIS XML Catalog is used for schema resolution, a private or modified schema in the catalog can be used if desired.


Mappings
````````

The ``typeMappings`` element begins with configuration elements. From the mapping file for ``gsml:GeologicUnit``::

    <typeMappings>
        <FeatureTypeMapping>
            <sourceDataStore>datastore</sourceDataStore>
            <sourceType>gsml_GeologicUnit</sourceType>
            <targetElement>gsml:GeologicUnit</targetElement>

* The mapping starts with ``sourceDataStore``, which gives the arbitrary identifier used above to name the source of the input data in the ``sourceDataStores`` section.

* ``sourceType`` gives the name of the source simple feature type. In this case it is the simple feature type ``gsml_GeologicUnit``, sourced from the rows of the file ``gsml_GeologicUnit.properties`` in the same directory as the mapping file.

* When working with databases ``sourceType`` is the name of a table or view. Database identifiers must be lowercase for PostGIS or uppercase for Oracle Spatial.

* ``targetElement`` is the name of the output complex feature type.


gml:id mapping
``````````````

The first mapping sets the ``gml:id`` to be the feature id specified in the source property file::

    <AttributeMapping>
        <targetAttribute>
            gsml:GeologicUnit
        </targetAttribute>
        <idExpression>
            <OCQL>ID</OCQL>
        </idExpression>
    </AttributeMapping>

* ``targetAttribute`` is the XPath to the element for which the mapping applies, in this case, the top-level feature type.

* ``idExpression`` is a special form that can only be used to set the ``gml:id`` on a feature. Any field or CQL expression can be used, if it evaluates to an `NCName <http://www.w3.org/TR/1999/REC-xml-names-19990114/#NT-NCName>`_.


Ordinary mapping
````````````````

Most mappings consist of a target and source. Here is one from ``gsml:GeologicUnit``::

    <AttributeMapping>
        <targetAttribute>
            gml:description
            </targetAttribute>
        <sourceExpression>
            <OCQL>DESCRIPTION</OCQL>
        </sourceExpression>
    </AttributeMapping>

* In this case, the value of ``gml:description`` is just the value of the ``DESCRIPTION`` field in the property file.

* For a database, the field name is the name of the column (the table/view is set in ``sourceType`` above). Database identifiers must be lowercase for PostGIS or uppercase for Oracle Spatial.

* CQL expressions can be used to calculate content. Use caution because queries on CQL-calculated values prevent the construction of efficient SQL queries.

* Source expressions can be CQL literals, which are single-quoted.


Client properties
`````````````````

In addition to the element content, a mapping can set one or more "client properties" (XML attributes). Here is one from ``gsml:MappedFeature``::

    <AttributeMapping>
        <targetAttribute>
            gsml:specification
        </targetAttribute>
        <ClientProperty>
            <name>xlink:href</name>
            <value>GU_URN</value>
        </ClientProperty>
    </AttributeMapping>

* This mapping leaves the content of the ``gsml:specification`` element empty but sets an ``xlink:href`` attribute to the value of the ``GU_URN`` field.

* Multiple ``ClientProperty`` mappings can be set.

In this example from the mapping for ``gsml:GeologicUnit`` both element content and an XML attribute are provided::

    <AttributeMapping>
        <targetAttribute>
            gml:name[1]
            </targetAttribute>
        <sourceExpression>
            <OCQL>NAME</OCQL>
        </sourceExpression>
        <ClientProperty>
            <name>codeSpace</name>
            <value>'urn:x-test:classifierScheme:TestAuthority:GeologicUnitName'</value>
        </ClientProperty>
    </AttributeMapping>

* The ``codespace`` XML attribute is set to a fixed value by providing a CQL literal.

* There are multiple mappings for ``gml:name``, and the index ``[1]`` means that this mapping targets the first.


targetAttributeNode
```````````````````

If the type of a property is abstract, a ``targetAttributeNode`` mapping must be used to specify a concrete type. This mapping must occur before the mapping for the content of the property.

Here is an example from the mapping file for ``gsml:MappedFeature``::

    <AttributeMapping>
        <targetAttribute>gsml:positionalAccuracy</targetAttribute>
        <targetAttributeNode>gsml:CGI_TermValuePropertyType</targetAttributeNode>
    </AttributeMapping>
    <AttributeMapping>
        <targetAttribute>gsml:positionalAccuracy/gsml:CGI_TermValue/gsml:value</targetAttribute>
        <sourceExpression>
            <OCQL>'urn:ogc:def:nil:OGC:missing'</OCQL>
        </sourceExpression>
        <ClientProperty>
            <name>codeSpace</name>
            <value>'urn:ietf:rfc:2141'</value>
        </ClientProperty>
    </AttributeMapping>

* ``gsml:positionalAccuracy`` is of type ``gsml:CGI_TermValuePropertyType``, which is abstract, so must be mapped to its concrete subtype ``gsml:CGI_TermValuePropertyType`` with a ``targetAttributeNode`` mapping before its contents can be mapped.

* This example also demonstrates that mapping can be applied to nested properties to arbitrary depth. This becomes unmanageable for deep nesting, where feature chaining is preferred.


Feature chaining
````````````````

In feature chaining, one feature type is used as a property of an enclosing feature type, by value or by reference::

    <AttributeMapping>
        <targetAttribute>
            gsml:occurrence
        </targetAttribute>
        <sourceExpression>
            <OCQL>URN</OCQL>
            <linkElement>gsml:MappedFeature</linkElement>
            <linkField>gml:name[2]</linkField>
        </sourceExpression>
        <isMultiple>true</isMultiple>
    </AttributeMapping>


* In this case from the mapping for ``gsml:GeologicUnit``, we specify a mapping for its ``gsml:occurrence``.

* The ``URN`` field of the source ``gsml_GeologicUnit`` simple feature is use as the "foreign key", which maps to the second ``gml:name`` in each ``gsml:MappedFeature``.

* Every ``gsml:MappedFeature`` with ``gml:name[2]`` equal to the ``URN`` of the ``gsml:GeologicUnit`` under construction is included as a ``gsml:occurrence`` property of the ``gsml:GeologicUnit`` (by value).


WFS response
------------

When GeoServer is running, test app-schema WFS in a web browser. If GeoServer is listening on ``localhost:8080`` you can query the two feature types using these links:

* http://localhost:8080/geoserver/wfs?request=GetFeature&version=1.1.0&typeName=gsml:GeologicUnit

* http://localhost:8080/geoserver/wfs?request=GetFeature&version=1.1.0&typeName=gsml:MappedFeature


gsml:GeologicUnit
`````````````````

Feature chaining has been used to construct the multivalued property ``gsml:occurrence`` of ``gsml:GeologicUnit``. This property is a ``gsml:MappedFeature``. The WFS response for ``gsml:GeologicUnit`` combines the output of both feature types into a single response. The first ``gsml:GeologicUnit`` has two ``gsml:occurrence`` properties, while the second has one. The relationships between the feature instances are data driven.

Because the mapping files in the tutorial configuration do not contain attribute mappings for all mandatory properties of these feature types, the WFS response is not *schema-valid* against the GeoSciML 2.0 schemas. Schema-validity can be achieved by adding more attribute mappings to the mapping files.


.. note:: These feature types are defined in terms of GML 3.1 (the default for WFS 1.1.0); other GML versions will not work.


.. warning:: The web interface does not yet support app-schema store or layer administration.


Acknowledgements
----------------

``gsml_GeologicUnit.properties`` and ``gsml_MappedFeature.properties`` are derived from data provided by the Department of Primary Industries, Victoria, Australia. For the purposes of this tutorial, this data has been modified to the extent that it has no real-world meaning.

