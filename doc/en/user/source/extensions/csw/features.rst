.. _community_csw_features:

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

The Internal Catalog Store supports filtering on both full x-paths as well as the "Queryables" specified in GetCapabilities.

Catalog stores
--------------

The default catalog store is the Internal Catalog Store, which retrieves information from the GeoServer's internal catalog. The Simple Catalog Store (``simple-store`` module) adds an alternative simple store which reads the catalog data directly from files (mainly used for testing).

If there are multiple catalog stores present (for example, when the Simple Catalog Store module is loaded), set the Java system property ``DefaultCatalogStore`` to make sure that the correct catalog store will be used. To use the Internal Catalog Store, this property must be set to::

  DefaultCatalogStore=org.geoserver.csw.store.internal.GeoServerInternalCatalogStore
  
To use the Simple Catalog Store::

  DefaultCatalogStore=org.geoserver.csw.store.simple.GeoServerSimpleCatalogStore

Supported schemes
-----------------

The Internal Catalog Store supports two metadata schemes: 

* Dublin Core
* ISO Metadata Profile

Mapping Files
-------------

Mapping files are located in the ``csw`` directory inside the :ref:`data_directory`. Every mapping file must have the exact name of the record type name. For example:

* Dublin Core mapping can be found in the file ``csw/Record`` inside the data directory.
* ISO Metadata mapping can be found in the file ``csw/MD_Metadata`` inside the data directory.

The mapping files take the syntax from Java properties files. The left side of the equals sign specifies the target field name or path in the metadata record, paths being separated with dots. The right side of the equals sign specifies any CQL expression that denotes the value of the target property. The CQL expression is applied to each ``ResourceInfo`` object in the catalog and can retrieve all properties from this object. These expressions can make use of literals, attributes present in the ``ResourceInfo`` object, and all normal CQL operators and functions. (Example mapping files are given below.)

Placing the ``@`` symbol in front of the field will set that to use as identifier for each metadata record. This may be useful for ID filters.  Use a ``$`` sign in front of fields that are required to make sure the mapping is aware of the requirement (specifically for the purpose of property selection).

Dublin Core
~~~~~~~~~~~

Below is an example of a Dublin Core mapping file::

  @identifier.value=id
  title.value=title
  creator.value='GeoServer Catalog'
  subject.value=keywords
  subject.scheme='http://www.digest.org/2.1'
  abstract.value=abstract
  description.value=strConcat('description about ' , title)
  date.value="metadata.csw.date"
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

Examples of attributes extracted from the ResourceInfo are ``id``, ``title``, and ``keywords``, etc. The attribute ``metadata.csw.date`` uses the metadata (``java.util.``)Map from the Resource object. In this map, it searches for the keyword "csw", which in return also returns a (``java.util.``)Map from which the value for the keyword "date" is extracted.

Note that double quotes are necessary in order to preserve this meaning of the dots.

ISO Metadata Profile
~~~~~~~~~~~~~~~~~~~~

Below is an example of an ISO Metadata Profile Mapping File::

  @fileIdentifier.CharacterString=id
  identificationInfo.AbstractMD_Identification.citation.CI_Citation.title.CharacterString=title
  identificationInfo.AbstractMD_Identification.descriptiveKeywords.MD_Keywords.keyword.CharacterString=keywords 
  identificationInfo.AbstractMD_Identification.abstract.CharacterString=abstract
  $dateStamp.Date= if_then_else ( isNull("metadata.csw.date") , 'Unknown', "metadata.csw.date")
  hierarchyLevel.MD_ScopeCode.@codeListValue='http://purl.org/dc/dcmitype/Dataset'
  $contact.CI_ResponsibleParty.individualName.CharacterString='Niels Charlier'

The full path of each field must be specified (separated with dots). XML attributes are specified with the ``@`` symbol, similar to the usual XML X-path notation.

To keep the result XSD compliant, the parameters ``dateStamp.Date`` and ``contact.CI_ResponsibleParty.individualName.CharacterString`` must be preceded by a ``$`` sign to make sure that they are always included even when using property selection.

For more information on the ISO Metadata standard, please see the `OGC Implementation Specification 07-045 <http://www.opengeospatial.org/standards/specifications/catalog>`_. 

