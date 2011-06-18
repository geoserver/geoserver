.. _schema_mapping:

WFS Schema Mapping
==================

One of the functions of the GeoServer WFS is to automatically map the internal 
schema of a dataset to a feature type schema. The automatic mapping is performed
with the following rules:

#. The name of the feature element maps to the name of the dataset
#. The name of the feature type maps to the name of the dataset with the string 
   "Type" appended to it
#. The name of each attribute of the dataset maps to the name of an
   element particle contained in the feature type
#. The type of each attribute of the dataset maps to the appropriate
   xml schema type (ex: xs:int, xs:double, etc...)

As an example, consider a dataset with the following schema::

  myDataset(intProperty:Integer, stringProperty:String, floatProperty:Float, geometry:Point)

The above dataset would be mapped to the following XML schema, available from
a ``DescribeFeatureType`` request for the ``topp:myDataset`` type:

.. code-block:: xml

	<xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema"
	   xmlns:gml="http://www.opengis.net/gml"
	   xmlns:topp="http://www.openplans.org/topp" 
	   targetNamespace="http://www.openplans.org/topp"
	   elementFormDefault="qualified">

	  <xsd:import namespace="http://www.opengis.net/gml" schemaLocation="http://localhost:8080/geoserver/schemas/gml/3.1.1/base/gml.xsd"/>

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

The GeoServer WFS supports a *limited* amount of customization with regard to 
schema output. A custom schema can be used to:

* Limit the attributes which are exposed in the feature type schema
* :ref:`Changing <type_changing>` the types of attributes in the schema
* Change the structure of the schema, for example changing the base feature type

A mapped schema is customized by creating a file called ``schema.xsd`` in the 
appropriate feature type directory of the GeoServer data directory. As an simple
example consider the use case of limiting the exposed attributes in the above 
dataset.

It is useful to start with the default output as a base as it is a complete
schema. With the feature type schema shown above, a ``GetFeature`` request would
result in features that look like the following:

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
	
Now consider the case of removing the ``floatProperty`` from attribute. To 
achieve this:

#. The original schema is modified and the ``floatProperty`` is removed,
   resulting in the following type definition:

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
		
#. The result is saved in a file named ``schema.xsd``.
#. The ``schema.xsd`` file is copied into the feature type directory for the
   ``topp:myDataset``::

      copy schema.xsd $GEOSERVER_DATA_DIR/workspaces/<workspace>/<datastore>/myDataset/

   Where ``<workspace>`` is the name of the workspace containing your datastore and  ``<datastore>`` is the name of the data store which contains ``myDataset``

In order for the new schema to to be picked up by GeoServer the configuration 
must be reloaded. This cab be done by logging into the admin interface and 
clicking the ``Load`` button the ``Config`` page. Or alternatively by restarting
the entire Server.

Another ``DescribeFeatureType`` request for the ``topp:myDataset`` type now
results in the ``floatProperty`` element being absent:

   .. code-block:: xml

       <xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema"
		   xmlns:gml="http://www.opengis.net/gml"
		   xmlns:topp="http://www.openplans.org/topp" 
		   targetNamespace="http://www.openplans.org/topp"
		   elementFormDefault="qualified">
  
		  <xsd:import namespace="http://www.opengis.net/gml" schemaLocation="http://localhost:8080/geoserver/schemas/gml/3.1.1/base/gml.xsd"/>

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
	
Another ``GetFeature`` request now results in features in which the
``floatProperty`` is absent:

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

.. _type_changing:

Type changing
--------------

Schema customization can be used to do a limited amount of *type changing*. 
Limited by the fact that a changed type must be in the same "domain" as the 
original type. For example integers types must be changed to integer types, 
temporal types to temporal types, etc...

The most common case is for geometry attributes. Often it is the case that the 
underlying dataset does not have the metadata necessary to report the specific
type (Point,LineString,Polygon, etc...) of a geometry attribute. In these cases
the automatic schema mapping would result in an element particle like the 
following:

.. code-block:: xml

     <xsd:element maxOccurs="1" minOccurs="0" name="geometry" nillable="true " type="gml:GeometryPropertyType"/>

However it is often the case that the user knows the specific type of the 
geometry, for example Point. The above element could be changed to:

.. code-block:: xml

     <xsd:element maxOccurs="1" minOccurs="0" name="geometry" nillable="true " type="gml:PointPropertyType"/>
