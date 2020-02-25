.. _csw_iso_mapping:

CSW ISO Metadata Profile Mapping File
=====================================

General
~~~~~~~

See :ref:`csw_mapping_file` for basic information on the CSW mapping file. The ISO Metadata mapping can be found in the file ``csw/MD_Metadata.properties`` inside the data directory.

Below is an example of an ISO Metadata Profile Mapping File::

  @fileIdentifier.CharacterString=id
  identificationInfo.MD_DataIdentification.citation.CI_Citation.title.CharacterString=title
  identificationInfo.MD_DataIdentification.citation.CI_Citation.alternateTitle.CharacterString=list(description,alias,strConcat('##',title)) 
  identificationInfo.MD_DataIdentification.descriptiveKeywords.MD_Keywords.keyword.CharacterString=keywords 
  identificationInfo.MD_DataIdentification.abstract.CharacterString=abstract
  $dateStamp.Date= if_then_else ( isNull("metadata.date") , 'Unknown', "metadata.date")
  hierarchyLevel.MD_ScopeCode.@codeListValue='http://purl.org/dc/dcmitype/Dataset'
  $contact.CI_ResponsibleParty.individualName.CharacterString=
  identificationInfo.MD_DataIdentification.resourceConstraints[0].MD_LegalConstraints.accessConstraints.MD_RestrictionCode=
  identificationInfo.MD_DataIdentification.resourceConstraints[1].MD_SecurityConstraints.classification.MD_ClassificationCode=
  identificationInfo.MD_DataIdentification.citation.CI_Citation.date%.CI_Date.date.Date=lapply("metadata.citation-date", if_then_else(isNull("."), "Expression/NIL", dateFormat('YYYY-MM-dd', ".")))
  identificationInfo.MD_DataIdentification.descriptiveKeywords.MD_Keywords.keyword.CharacterString=list(keywords, if_then_else(equalTo(typeOf("."), 'FeatureTypeInfo'), 'vector', 'raster'))

The full path of each field must be specified (separated with dots). XML attributes are specified with the ``@`` symbol, similar to the usual XML X-path notation. To avoid confusion with the identifier-symbol at the beginning of a mapping line, use ``\@`` (for an attribute that is not an identifier) or ``@@`` (for an attribute that is also the identifier) - see the feature catatalog mapping file for an example.

The ``%`` symbol denotes where a multi-valued mapping should be split in to multiple tags. Multiple ``%`` symbols may be used for multi-dimensional mappings - see the feature catatalog mapping file below for an example.

Indexes with square brackets can be used to avoid merging tags that shouldn't be merged, as demonstrated above for ``resourceConstraints``.

To keep the result XSD compliant, the parameters ``dateStamp.Date`` and ``contact.CI_ResponsibleParty.individualName.CharacterString`` must be preceded by a ``$`` sign to make sure that they are always included even when using property selection.

The ``lapply`` function can be used to apply expressions to items of lists, which can be handy with multidimensional fields.

The ``typeOf`` function (exclusive to CSW-ISO module) returns the type of the catalog item that is being processed (``LayerGroupInfo``, ``FeatureTypeInfo``, ``CoverageInfo``,...), which can be handy if you for example need to handle vector layers differently to raster layers.

For more information on the ISO Metadata standard, please see the `OGC Implementation Specification 07-045 <http://www.opengeospatial.org/standards/specifications/catalog>`_. 

Feature Catalogs
~~~~~~~~~~~~~~~~

Within the ISO Metadata Profile, there is also support for ``Feature Catalogues`` that contain information about vector layer type metadata. As specified by the ISO Metadata standard these are exposed in separate records. For this purpose we have a separate mapping file::
  @@uuid="metadata.custom.feature-catalog/feature-catalog-uidentifier"
  \@id="metadata.custom.feature-catalog/feature-catalog-identifier"
  $featureType.FC_FeatureType.typeName.LocalName=concatenate("name", 'Type')
  $featureType.FC_FeatureType.isAbstract.Boolean='false'
  $featureType.FC_FeatureType.featureCatalogue.@uuidref="metadata.custom.feature-catalog/feature-catalog-identifier"
  featureType.FC_FeatureType.definition.CharacterString="metadata.custom.feature-catalog/feature-type/feature-type-definition"
  featureType.FC_FeatureType.carrierOfCharacteristics%.FC_FeatureAttribute.memberName.LocalName="metadata.custom.feature-catalog/feature-type/feature-attribute/name"
  featureType.FC_FeatureType.carrierOfCharacteristics%.FC_FeatureAttribute.valueType.TypeName.aName.CharacterString="metadata.custom.feature-catalog/feature-type/feature-attribute/type"
  featureType.FC_FeatureType.carrierOfCharacteristics%.FC_FeatureAttribute.length.CharacterString="metadata.custom.feature-catalog/feature-type/feature-attribute/length"
  featureType.FC_FeatureType.carrierOfCharacteristics%.FC_FeatureAttribute.definition.CharacterString="metadata.custom.feature-catalog/feature-type/feature-attribute/definition"
  featureType.FC_FeatureType.carrierOfCharacteristics%.FC_FeatureAttribute.cardinality.Multiplicity.range.MultiplicityRange.lower.Integer="metadata.custom.feature-catalog/feature-type/feature-attribute/min-occurrence"
  featureType.FC_FeatureType.carrierOfCharacteristics%.FC_FeatureAttribute.cardinality.Multiplicity.range.MultiplicityRange.upper.UnlimitedInteger="metadata.custom.feature-catalog/feature-type/feature-attribute/max-occurrence"
  featureType.FC_FeatureType.carrierOfCharacteristics%.FC_FeatureAttribute.cardinality.Multiplicity.range.MultiplicityRange.upper.UnlimitedInteger.@isInfinite=false
  featureType.FC_FeatureType.carrierOfCharacteristics%.FC_FeatureAttribute.listedValue%.FC_ListedValue.label.CharacterString="metadata.custom.feature-catalog/feature-type/feature-attribute/domain/value"
  featureType.FC_FeatureType.carrierOfCharacteristics%.FC_FeatureAttribute.listedValue%.FC_ListedValue.definition.CharacterString="metadata.custom.feature-catalog/feature-type/feature-attribute/domain/definition"
  featureType.FC_FeatureType.carrierOfCharacteristics%.FC_FeatureAttribute.listedValue%.FC_ListedValue.code.CharacterString="metadata.custom.feature-catalog/feature-type/feature-attribute/domain/code"

Only records that have a non-null identifier in the catalog mapping file will have a feature catalogue record. There is no support in the standard Geoserver GUI for user configuration of this information.
The upcoming metadata community module makes this possible.



