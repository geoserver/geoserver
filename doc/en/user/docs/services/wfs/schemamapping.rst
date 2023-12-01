.. _wfs_schema_mapping:

WFS schema mapping
==================

One of the functions of the GeoServer WFS is to automatically map the internal schema of a dataset to a feature type schema. This mapping is performed according to the following rules:

* The name of the feature element maps to the name of the dataset.
* The name of the feature type maps to the name of the dataset with the string "Type" appended to it.
* The name of each attribute of the dataset maps to the name of an element particle contained in the feature type.
* The type of each attribute of the dataset maps to the appropriate XML schema type (``xsd:int``, ``xsd:double``, and so on).

For example, a dataset has the following schema::

  myDataset(intProperty:Integer, stringProperty:String, floatProperty:Float, geometry:Point)

This schema would be mapped to the following XML schema, available via
a ``DescribeFeatureType`` request for the ``topp:myDataset`` type:

.. code-block:: xml

  <xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema"
   xmlns:gml="http://www.opengis.net/gml"
   xmlns:topp="http://www.openplans.org/topp" 
   targetNamespace="http://www.openplans.org/topp"
   elementFormDefault="qualified">

    <xsd:import namespace="http://www.opengis.net/gml"
     schemaLocation="http://localhost:8080/geoserver/schemas/gml/3.1.1/base/gml.xsd"/>

    <xsd:complexType name="myDatasetType">
      <xsd:complexContent>
        <xsd:extension base="gml:AbstractFeatureType">
          <xsd:sequence>
            <xsd:element maxOccurs="1" minOccurs="0" name="intProperty" nillable="true" type="xsd:int"/>
            <xsd:element maxOccurs="1" minOccurs="0" name="stringProperty" nillable="true" type="xsd:string"/>
            <xsd:element maxOccurs="1" minOccurs="0" name="floatProperty" nillable="true" type="xsd:double"/>
            <xsd:element maxOccurs="1" minOccurs="0" name="geometry" nillable="true" type="gml:PointPropertyType"/>
          </xsd:sequence>
        </xsd:extension>
      </xsd:complexContent>
    </xsd:complexType>

    <xsd:element name="myDataset" substitutionGroup="gml:_Feature" type="topp:myDatasetType"/>

  </xsd:schema>

Schema customization
--------------------

The GeoServer WFS supports a limited amount of schema output customization. A custom schema may be useful for the following:

* Limiting the attributes which are exposed in the feature type schema
* :ref:`Changing <wfs_schema_type_changing>` the types of attributes in the schema
* Changing the structure of the schema (for example, changing the base feature type)

For example, it may be useful to limit the exposed attributes in the example dataset described above. Start by retrieving the default output as a benchmark of the complete schema. With the feature type schema listed above, the ``GetFeature`` request would be as follows:

.. code-block:: xml

   <topp:myDataset gml:id="myDataset.1">
    <topp:intProperty>1</topp:intProperty>
     <topp:stringProperty>one</topp:stringProperty>
     <topp:floatProperty>1.1</topp:floatProperty>
     <topp:geometry>
       <gml:Point srsName="urn:x-ogc:def:crs:EPSG:4326">
         <gml:pos>1.0 1.0</gml:pos>
       </gml:Point>
     </topp:geometry>
   </topp:myDataset>

To remove ``floatProperty`` from the list of attributes, the following steps would be required:

#. The original schema is modified to remove the ``floatProperty``, resulting in the following type definition:

   .. code-block:: xml

      <xsd:complexType name="myDatasetType">
        <xsd:complexContent>
          <xsd:extension base="gml:AbstractFeatureType">
            <xsd:sequence>
              <xsd:element maxOccurs="1" minOccurs="0" name="intProperty" nillable="true" type="xsd:int"/>
              <xsd:element maxOccurs="1" minOccurs="0" name="stringProperty" nillable="true" type="xsd:string"/>
              <!-- remove the floatProperty element
              <xsd:element maxOccurs="1" minOccurs="0" name="floatProperty" nillable="true" type="xsd:double"/>
              -->
              <xsd:element maxOccurs="1" minOccurs="0" name="geometry" nillable="true" type="gml:PointPropertyType"/>
            </xsd:sequence>
          </xsd:extension>
          </xsd:complexContent>
      </xsd:complexType>

#. The modification is saved in a file named ``schema.xsd``.
#. The ``schema.xsd`` file is copied into the feature type directory for the
   ``topp:myDataset`` which is::

      $GEOSERVER_DATA_DIR/workspaces/<workspace>/<datastore>/myDataset/

   where ``<workspace>`` is the name of the workspace containing your data store and  ``<datastore>`` is the name of the data store which contains ``myDataset``

The modified schema will only be available to GeoServer when the configuration is reloaded or GeoServer is restarted.

A subsequent ``DescribeFeatureType`` request for ``topp:myDataset`` confirms the ``floatProperty`` element is absent:

.. code-block:: xml

      <xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema"
       xmlns:gml="http://www.opengis.net/gml"
       xmlns:topp="http://www.openplans.org/topp" 
       targetNamespace="http://www.openplans.org/topp"
       elementFormDefault="qualified">
  
        <xsd:import namespace="http://www.opengis.net/gml"
         schemaLocation="http://localhost:8080/geoserver/schemas/gml/3.1.1/base/gml.xsd"/>

        <xsd:complexType name="myDatasetType">
          <xsd:complexContent>
            <xsd:extension base="gml:AbstractFeatureType">
              <xsd:sequence>
                <xsd:element maxOccurs="1" minOccurs="0" name="intProperty" nillable="true" type="xsd:int"/>
                <xsd:element maxOccurs="1" minOccurs="0" name="stringProperty" nillable="true" type="xsd:string"/>
                <xsd:element maxOccurs="1" minOccurs="0" name="geometry" nillable="true" type="gml:PointPropertyType"/>
              </xsd:sequence>
            </xsd:extension>
          </xsd:complexContent>
        </xsd:complexType>

        <xsd:element name="myDataset" substitutionGroup="gml:_Feature" type="topp:myDatasetType"/>

      </xsd:schema>

A ``GetFeature`` request will now return features that don't include the ``floatProperty`` attribute:

.. code-block:: xml

    <topp:myDataset gml:id="myDataset.1">
      <topp:intProperty>1</topp:intProperty>
      <topp:stringProperty>one</topp:stringProperty>
      <topp:geometry>
        <gml:Point srsName="urn:x-ogc:def:crs:EPSG:4326">
          <gml:pos>1.0 1.0</gml:pos>
        </gml:Point>
      </topp:geometry>
    </topp:myDataset>

.. _wfs_schema_type_changing:

Type changing
-------------

Schema customization may be used to perform some **type changing**, although this is limited by the fact that a changed type must be in the same *domain* as the original type. For example, integer types must be changed to integer types, temporal types to temporal types, and so on.

The most common change type requirement is for geometry attributes. In many cases the underlying data set does not have the necessary metadata to report the specific geometry type of a geometry attribute. The automatic schema mapping would result in an element definition similar to the following:

.. code-block:: xml

     <xsd:element maxOccurs="1" minOccurs="0" name="geometry" nillable="true" type="gml:GeometryPropertyType"/>

However if the specific type of the geometry is known, the element definition above could be altered. For point geometry, the element definition could be altered to :

.. code-block:: xml

     <xsd:element maxOccurs="1" minOccurs="0" name="geometry" nillable="true" type="gml:PointPropertyType"/>
