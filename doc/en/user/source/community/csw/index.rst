.. _community_csw:

Catalogue Services for the Web
==============================

:ref:`community_csw_tutorial`

Supported Services
------------------

GeoServer supports retrieving and displaying items from the GeoServer catalog using CSW (Catalogue Services for the Web). The following standard CSW Services are currently supported:

  * GetCapabilities
  * GetRecords
  * GetRecordById
  * GetDomain
  * DescribeRecord

For more information on CSW services, we refer to OGC OpenGIS Implementation Specification 07-006r1 (see http://www.opengeospatial.org/standards/specifications/catalog) and http://www.ogcnetwork.net/node/630.

The Internal Catalog Store supports filtering on both full x-paths as well as the 'Queryables' specified in getCapabilities.

Catalog Stores
--------------

The default Catalog Store is the Internal Catalog Store, which retrieves information from the GeoServer's internal catalog. 
The module 'simple-store' adds an alternative simple store which reads the Catalog Data directly from files (this is mainly used for testing).
If there are multiple Catalog Stores present (for example, when the simple-store module is loaded), we must set the Java system property "DefaultCatalogStore" to make sure that the right
catalog store will be used. To use the Internal Catalog Store, this property must be set to::

  DefaultCatalogStore=org.geoserver.csw.store.internal.GeoServerInternalCatalogStore
  
To use the Simple Catalog Store::

  DefaultCatalogStore=org.geoserver.csw.store.simple.GeoServerSimpleCatalogStore

Supported Schemes
-----------------

Currently the Internal Catalog Store supports two MetaData schemes: 

  - Dublin Core
  - ISO MetaData Profile.

Mapping Files
-------------

The mapping files are located in the 'csw' directory inside the GeoServer Data Directory.
Every mapping file must have the exact name of the record type name. For example:

  * the Dublin Core mapping can be found in the file 'csw/Record' inside the Data Directory.
  * the ISO MetaData mapping can be found in the file 'csw/MD_Metadata' inside the Data Directory.

The mapping files take the syntax from Java properties files. On the left of the = sign, we specify the target field name or path in the MetaData record, paths beings separated with dots. On the right of the = sign, we can specify any CQL expression that specifies the value of the target property. The CQL expression is applied to each 'ResourceInfo' object in the catalog and can retrieve all properties from this object. These expressions can make use of literals, of attributes present in the 'ResourceInfo' object, and all normal CQL operators and functions. We will discuss some example mapping files below.

Furthermore, we can place a @ symbol in front of the field we also wish to use as identifier for each MetaData record (this may be useful for ID filters).  We use a $ sign in front of fields that are required to make sure the mapping is aware of the requirement (in particular for the purpose of property selection).

Dublin Core
-----------

An example of a Dublin Core Mapping File::

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

All fields have the form of <fieldname>.value (for the actual value in the field) and, additionally also <fieldname>.scheme can be specified (for the @scheme attribute) of this field.

Examples of attributes extracted from the ResourceInfo are “id”, “title”, “keywords”, etc... The attribute “metadata.csw.date” uses the MetaData (java.util.)Map from the Resource object. In this map, it searches for the keyword “csw”, which in return also returns a (java.util).Map from which the value for the keyword “date” is extracted. Note that the double quotes are necessary to preserve this meaning of the dots.

ISO Metadata Profile
--------------------

For more information on the ISO MetaData standard, we refer to OGC Implementation Specification 07-045 (see http://www.opengeospatial.org/standards/specifications/catalog). 
An example of an ISO MetaDataProfile Mapping File::

  @fileIdentifier.CharacterString=id
  identificationInfo.AbstractMD_Identification.citation.CI_Citation.title.CharacterString=title
  identificationInfo.AbstractMD_Identification.descriptiveKeywords.MD_Keywords.keyword.CharacterString=keywords	
  identificationInfo.AbstractMD_Identification.abstract.CharacterString=abstract
  $dateStamp.Date= if_then_else ( isNull("metadata.csw.date") , 'Unknown', "metadata.csw.date")
  hierarchyLevel.MD_ScopeCode.@codeListValue='http://purl.org/dc/dcmitype/Dataset'
  $contact.CI_ResponsibleParty.individualName.CharacterString='Niels Charlier'

The full path of each field must be specified (separated with dots). Xml attributes are specified with a @ symbol, similar to the usual XML X-path notation.

Note that “dateStamp.Date” and ”contact.CI_ResponsibleParty.individualName.CharacterString” must be preceded by a $ sign to make sure that it is always included, even when using property selection (to keep the result XSD compliant).

