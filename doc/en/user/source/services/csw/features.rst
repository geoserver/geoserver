.. _csw_features:

Catalog Services for the Web (CSW) features
===========================================

Supported operations
--------------------

The following standard CSW operations are currently supported:

* GetCapabilities
* GetRecords
* GetRecordById
* GetDomain
* DescribeRecord

(Starting with GeoServer 2.9.x, a new vendor operation has been added: :ref:`DirectDownload <csw_directdownload>`) 

The Internal Catalog Store supports filtering on both full x-paths as well as the "Queryables" specified in GetCapabilities.

Catalog stores
--------------

The default catalog store is the Internal Catalog Store, which retrieves information from the GeoServer's internal catalog. The Simple Catalog Store (``simple-store`` module) adds an alternative simple store which reads the catalog data directly from files (mainly used for testing).

If there are multiple catalog stores present (for example, when the Simple Catalog Store module is loaded), set the Java system property ``DefaultCatalogStore`` to make sure that the correct catalog store will be used. To use the Internal Catalog Store, this property must be set to::

  DefaultCatalogStore=org.geoserver.csw.store.internal.InternalCatalogStore
  
To use the Simple Catalog Store::

  DefaultCatalogStore=org.geoserver.csw.store.simple.GeoServerSimpleCatalogStore

Supported schemes
-----------------

The Internal Catalog Store currently supports two metadata schemes: 

* Dublin Core, by default.
* ISO Metadata Profile, if you install the :ref:`csw_iso` Community Module.

.. _csw_mapping_file:

Mapping Files
-------------

Mapping files are located in the ``csw`` directory inside the :ref:`datadir`. Assuming that each layer is mapped to a single record, each mapping file has the exact name of the record type name, combined with the ``.properties`` extension. For example:

* Dublin Core mapping can be found in the file ``csw/Record.properties`` inside the data directory.
* ISO Metadata mapping can be found in the file ``csw/MD_Metadata.properties`` inside the data directory (see :ref:`csw_iso` Community Module).

There is also the possibility of having mapping each layer to multiple records. In this case, one would have multiple mapping files per type. Each mapping file will be applied to each layer. For instance, if there are three ISO Metadata mapping files, each layer will generate three ISO Metadata records, one for each mapping file. Practically, the different mapping files must be given unique names, encoded in the file name after the type name, followed by a dash, and before the extension. At most one mapping file per type can be nameless (as in the above example). For instance, one could have the following files in the ``csw`` directory:

* ``csw/Record.properties``
* ``csw/Record-otherRecord.properties``
* ``csw/Record-thirdRecord.properties``
* ``csw/MD_Metadata.properties``
* ``csw/MD_Metadata-otherRecord.properties``
* ``csw/MD_Metadata-thirdRecord.properties``

In this example, each of the two types have three mapping files: one nameless, one called `otherRecord` and one called `thirdRecord`. Each layer will thus generate three records for each type. Geoserver will assume that mappings of different type but with the same name refer to the same record (in another format). (This is only relevant for GetRecords requests where outputSchema and typeNames do not match¸ which is unusual). The user is responsible for ensuring that identifiers are unique accross all mappings of the same record type.

The mapping files take the syntax from Java properties files. The left side of the equals sign specifies the target field name or path in the metadata record, paths being separated with dots. The right side of the equals sign specifies any CQL expression that denotes the value of the target property. The CQL expression is applied to each ResourceInfo_ object in the catalog and can retrieve all properties from this object. These expressions can make use of literals, properties present in the ResourceInfo_ object, and all normal CQL operators and functions. 
There is also support for complex data structures such as Maps using the dot notation and Lists using the bracket notation (Example mapping files are given below).

The properties in the ResourceInfo_ object that can be used are:: 

  name
  qualifiedName
  nativeName
  qualifiedNativeName
  alias
  title
  abstract
  description
  metadata.?
  namespace
  namespace.prefix
  namespace.name
  namespace.uri
  namespace.metadata.?
  keywords
  keywords[?]
  keywords[?].value
  keywords[?].language
  keywords[?].vocabulary
  keywordValues
  keywordValues[?]
  metadataLinks
  metadataLinks[?]
  metadataLinks[?].id
  metadataLinks[?].about
  metadataLinks[?].metadataType
  metadataLinks[?].type
  metadataLinks[?].content
  latLonBoundingBox
  latLonBoundingBox.dimension
  latLonBoundingBox.lowerCorner
  latLonBoundingBox.upperCorner
  nativeBoundingBox
  nativeBoundingBox.dimension
  nativeBoundingBox.lowerCorner
  nativeBoundingBox.upperCorner
  srs
  nativeCrs
  projectionPolicy
  enabled
  advertised
  catalog.defaultNamespace
  catalog.defaultWorkspace
  store.name
  store.description
  store.type
  store.metadata.?
  store.enabled
  store.workspace
  store.workspace.name
  store.metadata.?
  store.connectionParameters.?
  store.error

Depending on whether the resource is a FeatureTypeInfo or a CoverageInfo, additional properties may be taken from their respective object structure.
You may use :ref:`rest` to view an xml model of feature types and datastores in which the xml tags represent the available properties in the objects.

.. _ResourceInfo: http://rancor.boundlessgeo.com:8080/display/GEOS/Catalog+Design#CatalogDesign-resources

Some fields in the metadata schemes can have multiple occurrences. They may be mapped to properties in the Catalog model that are also multi-valued, such as for example ``keywords``.
It is also possible to use a filter function called ``list`` to map multiple single-valued or multi-valued catalog properties to a MetaData field with multiple occurrences (see in ISO MetaData Profile example, mapping for the ``identificationInfo.AbstractMD_Identification.citation.CI_Citation.alternateTitle`` field). 

Placing the ``@`` symbol in front of the field will set that to use as identifier for each metadata record. This may be useful for ID filters.  Use a ``$`` sign in front of fields that are required to make sure the mapping is aware of the requirement (specifically for the purpose of property selection).
  
Below is an example of a Dublin Core mapping file::

  @identifier.value=id
  title.value=title
  creator.value='GeoServer Catalog'
  subject.value=keywords
  subject.scheme='http://www.digest.org/2.1'
  abstract.value=abstract
  description.value=strConcat('description about ' , title)
  date.value="metadata.date"
  type.value='http://purl.org/dc/dcmitype/Dataset'
  publisher.value='Niels Charlier'
  #format.value=
  #language.value=
  #coverage.value=
  #source.value=
  #relation.value=
  #rights.value=
  #contributor.value=

All fields have the form of ``<fieldname>.value`` for the actual value in the field. Additionally ``<fieldname>.scheme`` can be specified for the ``@scheme`` attribute of this field.

Examples of attributes extracted from the ResourceInfo are ``id``, ``title``, and ``keywords``, etc. The attribute ``metadata.date`` uses the metadata (``java.util.``)Map from the Resource object. In this map, it searches for the keyword "date".

Note that double quotes are necessary in order to preserve this meaning of the dots.

