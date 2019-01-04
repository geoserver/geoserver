.. _app-schema.complex-features:

Complex Features
================

To understand complex features, and why you would want use them, you first need
to know a little about simple features.


Simple features
---------------

A common use of GeoServer WFS is to connect to a data source such as a database
and access one or more tables, where each table is treated as a WFS simple feature type.
Simple features contain a list of properties that each have one piece of simple information such as a string or number.
(Special provision is made for geometry objects, which are treated like single items of simple data.)
The Open Geospatial Consortium (OGC) defines three Simple Feature profiles; SF-0, SF-1, and SF-2.
GeoServer simple features are close to OGC SF-0, the simplest OGC profile.

GeoServer WFS simple features provide a straightforward mapping from a database table or 
similar structure to a "flat" XML representation, where every column of the table maps to 
an XML element that usually contains no further structure.
One reason why GeoServer WFS is so easy to use with simple features is that the conversion
from columns in a database table to XML elements is automatic. The name of each element is the
name of the column, in the namespace of the data store. The name of the feature type defaults to
the name of the table. GeoServer WFS can manufacture an XSD type definition for every simple feature type it serves.
Submit a DescribeFeatureType request to see it.

Benefits of simple features
```````````````````````````

* Easy to implement
* Fast
* Support queries on properties, including spatial queries on geometries

Drawbacks of simple features
````````````````````````````

* When GeoServer automatically generates an XSD, the XML format is tied to the database schema.
* To share data with GeoServer simple features, participants must either use the same database schema or translate between different schemas.
* Even if a community could agree on a single database schema, as more data owners with different data are added to a community, the number of columns in the table becomes unmanageable.
* Interoperability is difficult because simple features do not allow modification of only part of the schema.

Simple feature example
``````````````````````

For example, if we had a database table ``stations`` containing information about GPS stations::

    | id | code |      name      |         location         |
    +----+------+----------------+--------------------------+
    | 27 | ALIC | Alice Springs  | POINT(133.8855 -23.6701) |
    | 4  | NORF | Norfolk Island | POINT(167.9388 -29.0434) |
    | 12 | COCO | Cocos          | POINT(96.8339 -12.1883)  |
    | 31 | ALBY | Albany         | POINT(117.8102 -34.9502) |

GeoServer would then be able to create the following simple feature WFS response fragment::

    <gps:stations gml:id="stations.27">
        <gps:code>ALIC</gps:code>
        <gps:name>Alice Springs</gps:name>
        <gps:location>
            <gml:Point srsName="urn:x-ogc:def:crs:EPSG:4326">
                <gml:pos>-23.6701 133.8855</gml:pos>
            </gml:Point>
        </gps:location>
    </gps:stations>

* Every row in the table is converted into a feature.
* Every column in the table is converted into an element, which contains the value for that row.
* Every element is in the namespace of the data store.
* Automatic conversions are applied to some special types like geometries, which have internal structure, and include elements defined in GML.


Complex features
----------------

Complex features contain properties that can contain further nested properties to arbitrary depth. In particular,
complex features can contain properties that are other complex features. Complex features can be used to represent
information not as an XML view of a single table, but as a collection of related objects of different types.

============================================================    =====================================================
Simple feature                                                  Complex feature
============================================================    =====================================================
Properties are single data item, e.g. text, number, geometry    Properties can be complex, including complex features
XML view of a single table                                      Collection of related identifiable objects
Schema automatically generated based on database                Schema agreed by community
One large type                                                  Multiple different types
Straightforward                                                 Richly featured data standards
Interoperability relies on simplicity and customisation         Interoperability through standardization
============================================================    =====================================================

Benefits of complex features
````````````````````````````

* Can define information model as an object-oriented structure, an *application schema*.
* Information is modelled not as a single table but as a collection of related objects whose associations and types may vary from feature to feature (polymorphism), permitting rich expression of content.
* By breaking the schema into a collection of independent types, communities need only extend those types they need to modify. This simplifies governance and permits interoperability between related communities who can agree on common base types but need not agree on application-specific subtypes..

Drawbacks of complex features
`````````````````````````````

* More complex to implement
* Complex responses might slower if more database queries are required for each feature.
* Information modelling is required to standardize an application schema. While this is beneficial, it requires effort from the user community.


Complex feature example
```````````````````````

Let us return to our ``stations`` table and supplement it with a foreign key ``gu_id`` that describes 
the relationship between the GPS station and the geologic unit to which it is
physically attached::

    | id | code |      name      |         location         | gu_id |
    +----+------+----------------+--------------------------+-------+
    | 27 | ALIC | Alice Springs  | POINT(133.8855 -23.6701) | 32785 |
    | 4  | NORF | Norfolk Island | POINT(167.9388 -29.0434) | 10237 | 
    | 12 | COCO | Cocos          | POINT(96.8339 -12.1883)  | 19286 |
    | 31 | ALBY | Albany         | POINT(117.8102 -34.9502) | 92774 |


The geologic unit is is stored in the table ``geologicunit``::

    | gu_id |                       urn             |         text        |
    +-------+---------------------------------------+---------------------+
    | 32785 | urn:x-demo:feature:GeologicUnit:32785 | Metamorphic bedrock |
    ...

The simple features approach would be to join the ``stations`` table with the ``geologicunit`` 
table into one view and then deliver "flat" XML that contained all the properties of both. 
The complex feature approach is to deliver the two tables as separate feature types.
This allows the relationship between the entities to be represented while preserving their individual identity.

For example, we could map the GPS station to a ``sa:SamplingPoint`` with a ``gsml:GeologicUnit``.
The these types are defined in the following application schemas respectively:

* http://schemas.opengis.net/sampling/1.0.0/sampling.xsd

    * Documentation: OGC 07-002r3: http://portal.opengeospatial.org/files/?artifact_id=22467

* http://www.geosciml.org/geosciml/2.0/xsd/geosciml.xsd

    * Documentation: http://www.geosciml.org/geosciml/2.0/doc/

The complex feature WFS response fragment could then be encoded as::

    <sa:SamplingPoint gml:id="stations.27>
      <gml:name codeSpace="urn:x-demo:SimpleName">Alice Springs</gml:name>
      <gml:name codeSpace="urn:x-demo:IGS:ID">ALIC</gml:name>
      <sa:sampledFeature>
         <gsml:GeologicUnit gml:id="geologicunit.32785">
             <gml:description>Metamorphic bedrock</gml:description>
             <gml:name codeSpace="urn:x-demo:Feature">urn:x-demo:feature:GeologicUnit:32785</gml:name>
         </gsml:GeologicUnit>
      </sa:sampledFeature>
      <sa:relatedObservation xlink:href="urn:x-demo:feature:GeologicUnit:32785" />
      <sa:position>
          <gml:Point srsName="urn:x-ogc:def:crs:EPSG:4326">
              <gml:pos>-23.6701 133.8855</gml:pos>
          </gml:Point>
      </sa:position>
    </sa:SamplingPoint>

* The property ``sa:sampledFeature`` can reference any other feature type, inline (included in the response) or by reference (an ``xlink:href`` URL or URN). This is an example of the use of polymorphism.
* The property ``sa:relatedObservation`` refers to the same GeologicUnit as ``sa:sampledFeature``, but by reference.
* Derivation of new types provides an extension point, allowing information models to be reused and extended in a way that supports backwards compatibility.
* Multiple sampling points can share a single GeologicUnit. Application schemas can also define multivalued properties to support many-to-one or many-to-many associations.
* Each GeologicUnit could have further properties describing in detail the properties of the rock, such as colour, weathering, lithology, or relevant geologic events.
* The GeologicUnit feature type can be served separately, and could be uniquely identified through its properties as the same instance seen in the SamplingPoint.

Portrayal complex features (SF0)
````````````````````````````````
Portrayal schemas are standardized schemas with flat attributes, also known as simple feature level 0 (SF0). Because a community schema is still required (e.g. GeoSciML-Portrayal), app-schema plugin is still used to map the database columns to the attributes.

* :doc:`WFS CSV output format <../../services/wfs/outputformats>` is supported for complex features with portrayal schemas. At the moment, propertyName selection is not yet supported with csv outputFormat, so it always returns the full set of attributes. 
* Complex features with nesting and multi-valued properties are not supported with :doc:`WFS CSV output format <../../services/wfs/outputformats>`.