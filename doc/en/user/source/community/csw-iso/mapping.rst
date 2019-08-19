.. _csw_iso_mapping:

CSW ISO Metadata Profile Mapping File
=====================================

See :ref:`csw_mapping_file` for basic information on the CSW mapping file. The ISO Metadata mapping can be found in the file ``csw/MD_Metadata.properties`` inside the data directory.

Below is an example of an ISO Metadata Profile Mapping File::

  @fileIdentifier.CharacterString=id
  identificationInfo.AbstractMD_Identification.citation.CI_Citation.title.CharacterString=title
  identificationInfo.AbstractMD_Identification.citation.CI_Citation.alternateTitle.CharacterString=list(description,alias,strConcat('##',title)) 
  identificationInfo.AbstractMD_Identification.descriptiveKeywords.MD_Keywords.keyword.CharacterString=keywords 
  identificationInfo.AbstractMD_Identification.abstract.CharacterString=abstract
  $dateStamp.Date= if_then_else ( isNull("metadata.date") , 'Unknown', "metadata.date")
  hierarchyLevel.MD_ScopeCode.@codeListValue='http://purl.org/dc/dcmitype/Dataset'
  $contact.CI_ResponsibleParty.individualName.CharacterString=
  identificationInfo.AbstractMD_Identification.descriptiveKeywords.MD_Keywords.keyword.CharacterString=list(keywords, if_then_else(equalTo(typeOf("."), 'FeatureTypeInfo'), 'vector', 'raster'))


The full path of each field must be specified (separated with dots). XML attributes are specified with the ``@`` symbol, similar to the usual XML X-path notation.

To keep the result XSD compliant, the parameters ``dateStamp.Date`` and ``contact.CI_ResponsibleParty.individualName.CharacterString`` must be preceded by a ``$`` sign to make sure that they are always included even when using property selection.

The ``lapply`` function can be used to apply expressions to items of lists, which can be handy with multidimensional fields.

For more information on the ISO Metadata standard, please see the `OGC Implementation Specification 07-045 <http://www.opengeospatial.org/standards/specifications/catalog>`_. 

The ``typeOf`` function (exclusive to CSW-ISO module) returns the type of the catalog item that is being processed (``LayerGroupInfo``, ``FeatureTypeInfo``, ``CoverageInfo``,...), which can be handy if you for example need to handle vector layers differently to raster layers.


