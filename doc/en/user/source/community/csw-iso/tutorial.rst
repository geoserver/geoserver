.. _csw_iso_tutorial:

Catalog Services for the Web (CSW) ISO Metadata tutorial
========================================================

This tutorial will show how to use the CSW module with the ISO Metadata Profile scheme. It assumes a fresh installation of GeoServer with the :ref:`CSW ISO Metadata Profile module installed <csw_installing>`.

Configuration
-------------

In the :file:`<data_dir>/csw` directory, create a new file named :file:`MD_Metadata` (ISO Metadata Profile mapping file) with the following contents::

  @fileIdentifier.CharacterString=prefixedName
  identificationInfo.AbstractMD_Identification.citation.CI_Citation.title.CharacterString=title
  identificationInfo.AbstractMD_Identification.descriptiveKeywords.MD_Keywords.keyword.CharacterString=keywords	
  identificationInfo.AbstractMD_Identification.abstract.CharacterString=abstract
  $dateStamp.Date= if_then_else ( isNull("metadata.date") , 'Unknown', "metadata.date")
  hierarchyLevel.MD_ScopeCode.@codeListValue='http://purl.org/dc/dcmitype/Dataset'
  $contact.CI_ResponsibleParty.individualName.CharacterString='John Smith'

Services
--------

With GeoServer running (and responding on ``http://localhost:8080``), test GeoServer CSW in a web browser by querying the CSW capabilities as follows::

  http://localhost:8080/geoserver/csw?service=csw&version=2.0.2&request=GetCapabilities

We can request a description of our Metadata record::

  http://localhost:8080/geoserver/csw?service=CSW&version=2.0.2&request=DescribeRecord&typeName=gmd:MD_Metadata
  
This yields the following result::

  <?xml version="1.0" encoding="UTF-8"?>
  <csw:DescribeRecordResponse xmlns:csw="http://www.opengis.net/cat/csw/2.0.2" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.opengis.net/cat/csw/2.0.2 http://localhost:8080/geoserver/schemas/csw/2.0.2CSW-discovery.xsd">
  <csw:SchemaComponent targetNamespace="http://www.opengis.net/cat/csw/2.0.2" schemaLanguage="http://www.w3.org/XML/Schema">
  <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:gco="http://www.isotc211.org/2005/gco" xmlns:gmd="http://www.isotc211.org/2005/gmd" targetNamespace="http://www.isotc211.org/2005/gmd" elementFormDefault="qualified" version="2012-07-13">
	<!-- ================================= Annotation ================================ -->
	<xs:annotation>
		<xs:documentation>Geographic MetaData (GMD) extensible markup language is a component of the XML Schema Implementation of Geographic Information Metadata documented in ISO/TS 19139:2007. GMD includes all the definitions of http://www.isotc211.org/2005/gmd namespace. The root document of this namespace is the file gmd.xsd. This identification.xsd schema implements the UML conceptual schema defined in A.2.2 of ISO 19115:2003. It contains the implementation of the following classes: MD_Identification, MD_BrowseGraphic, MD_DataIdentification, MD_ServiceIdentification, MD_RepresentativeFraction, MD_Usage, MD_Keywords, DS_Association, MD_AggregateInformation, MD_CharacterSetCode, MD_SpatialRepresentationTypeCode, MD_TopicCategoryCode, MD_ProgressCode, MD_KeywordTypeCode, DS_AssociationTypeCode, DS_InitiativeTypeCode, MD_ResolutionType.</xs:documentation>
	</xs:annotation>
	...
  
Query all layers as follows::

  http://localhost:8080/geoserver/csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=gmd:MD_Metadata&resultType=results&elementSetName=full&outputSchema=http://www.isotc211.org/2005/gmd
  
Request a particular layer by ID...::

  http://localhost:8080/geoserver/csw?service=CSW&version=2.0.2&request=GetRecordById&elementsetname=summary&id=CoverageInfoImpl--4a9eec43:132d48aac79:-8000&typeNames=gmd:MD_Metadata&resultType=results&elementSetName=full&outputSchema=http://www.isotc211.org/2005/gmd

...or use a filter to retrieve it by Title::
  
  http://localhost:8080/geoserver/csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=gmd:MD_Metadata&resultType=results&elementSetName=full&outputSchema=http://www.isotc211.org/2005/gmd&constraint=Title=%27mosaic%27
   
Either case should return::

    <?xml version="1.0" encoding="UTF-8"?>
    <csw:GetRecordsResponse xmlns:xml="http://www.w3.org/XML/1998/namespace" xmlns="http://www.opengis.net/cat/csw/apiso/1.0" xmlns:csw="http://www.opengis.net/cat/csw/2.0.2" xmlns:gco="http://www.isotc211.org/2005/gco" xmlns:gmd="http://www.isotc211.org/2005/gmd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" version="2.0.2" xsi:schemaLocation="http://www.opengis.net/cat/csw/2.0.2 http://localhost:8080/geoserver/schemas/csw/2.0.2/record.xsd">
      <csw:SearchStatus timestamp="2013-06-28T13:41:43.090Z"/>
      <csw:SearchResults numberOfRecordsMatched="1" numberOfRecordsReturned="1" nextRecord="0" recordSchema="http://www.isotc211.org/2005/gmd" elementSet="full">
	<gmd:MD_Metadata>
	  <gmd:fileIdentifier>
	    <gco:CharacterString>CoverageInfoImpl--4a9eec43:132d48aac79:-8000</gco:CharacterString>
	  </gmd:fileIdentifier>
	  <gmd:dateStamp>
	    <gco:Date>Unknown</gco:Date>
	  </gmd:dateStamp>
	  <gmd:identificationInfo>
	    <gmd:MD_DataIdentification>
	      <gmd:extent>
		<gmd:EX_Extent>
		  <gmd:geographicElement>
		    <gmd:EX_GeographicBoundingBox crs="urn:x-ogc:def:crs:EPSG:6.11:4326">
		      <gmd:westBoundLongitude>36.492</gmd:westBoundLongitude>
		      <gmd:southBoundLatitude>6.346</gmd:southBoundLatitude>
		      <gmd:eastBoundLongitude>46.591</gmd:eastBoundLongitude>
		      <gmd:northBoundLatitude>20.83</gmd:northBoundLatitude>
		    </gmd:EX_GeographicBoundingBox>
		  </gmd:geographicElement>
		</gmd:EX_Extent>
	      </gmd:extent>
	    </gmd:MD_DataIdentification>
	    <gmd:AbstractMD_Identification>
	      <gmd:citation>
		<gmd:CI_Citation>
		  <gmd:title>
		    <gco:CharacterString>mosaic</gco:CharacterString>
		  </gmd:title>
		</gmd:CI_Citation>
	      </gmd:citation>
	      <gmd:descriptiveKeywords>
		<gmd:MD_Keywords>
		  <gmd:keyword>
		    <gco:CharacterString>WCS</gco:CharacterString>
		  </gmd:keyword>
		  <gmd:keyword>
		    <gco:CharacterString>ImageMosaic</gco:CharacterString>
		  </gmd:keyword>
		  <gmd:keyword>
		    <gco:CharacterString>mosaic</gco:CharacterString>
		  </gmd:keyword>
		</gmd:MD_Keywords>
	      </gmd:descriptiveKeywords>
	    </gmd:AbstractMD_Identification>
	  </gmd:identificationInfo>
	  <gmd:contact>
	    <gmd:CI_ResponsibleParty>
	      <gmd:individualName>
		<gco:CharacterString>John Smith</gco:CharacterString>
	      </gmd:individualName>
	    </gmd:CI_ResponsibleParty>
	  </gmd:contact>
	  <gmd:hierarchyLevel>
	    <gmd:MD_ScopeCode codeListValue="http://purl.org/dc/dcmitype/Dataset"/>
	  </gmd:hierarchyLevel>
	</gmd:MD_Metadata>
      </csw:SearchResults>
    </csw:GetRecordsResponse>

We can request the domain of a property. For example, all values of "Title"::
  
  http://localhost:8080/geoserver/csw?service=csw&version=2.0.2&request=GetDomain&propertyName=Title
   
This should yield the following result::

    <?xml version="1.0" encoding="UTF-8"?>
    <csw:GetDomainResponse xmlns:csw="http://www.opengis.net/cat/csw/2.0.2" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dct="http://purl.org/dc/terms/" xmlns:ows="http://www.opengis.net/ows/1.1" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.opengis.net/cat/csw/2.0.2 http://localhost:8080/geoserver/schemas/csw/2.0.2/CSW-discovery.xsd">
      <csw:DomainValues type="csw:Record">
	  <csw:PropertyName>Title</csw:PropertyName>
	  <csw:ListOfValues>
	    <csw:Value>A sample ArcGrid file</csw:Value>
	    <csw:Value>Manhattan (NY) landmarks</csw:Value>
	    <csw:Value>Manhattan (NY) points of interest</csw:Value>
	    <csw:Value>Manhattan (NY) roads</csw:Value>
	    <csw:Value>North America sample imagery</csw:Value>
	    <csw:Value>Pk50095 is a A raster file accompanied by a spatial data file</csw:Value>
	    <csw:Value>Spearfish archeological sites</csw:Value>
	    <csw:Value>Spearfish bug locations</csw:Value>
	    <csw:Value>Spearfish restricted areas</csw:Value>
	    <csw:Value>Spearfish roads</csw:Value>
	    <csw:Value>Spearfish streams</csw:Value>
	    <csw:Value>Tasmania cities</csw:Value>
	    <csw:Value>Tasmania roads</csw:Value>
	    <csw:Value>Tasmania state boundaries</csw:Value>
	    <csw:Value>Tasmania water bodies</csw:Value>
	    <csw:Value>USA Population</csw:Value>
	    <csw:Value>World rectangle</csw:Value>
	    <csw:Value>mosaic</csw:Value>
	    <csw:Value>sfdem is a Tagged Image File Format with Geographic information</csw:Value>
	  </csw:ListOfValues>
      </csw:DomainValues>
    </csw:GetDomainResponse>

To request more than the first 10 records or for more complex queries you can use a HTTP POST request with an XML query as the request body. For example, using the maxRecords option in the following request it is possible to return the first 50 layers with "ImageMosaic" in a keyword::
  
  http://localhost:8080/geoserver/csw
  
Postbody::

    <?xml version="1.0" encoding="UTF-8"?>
    <GetRecords service="CSW" version="2.0.2" maxRecords="50" startPosition="1" resultType="results" outputFormat="application/xml" outputSchema="http://www.opengis.net/cat/csw/2.0.2" xmlns="http://www.opengis.net/cat/csw/2.0.2" xmlns:csw="http://www.opengis.net/cat/csw/2.0.2" xmlns:ogc="http://www.opengis.net/ogc" xmlns:ows="http://www.opengis.net/ows" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dct="http://purl.org/dc/terms/" xmlns:gml="http://www.opengis.net/gml" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.opengis.net/cat/csw/2.0.2 ../../../csw/2.0.2/CSW-discovery.xsd">
      <Query typeNames="csw:Record">
      	<ElementSetName typeNames="csw:Record">full</ElementSetName>
      	<Constraint version="1.1.0">
      	  <ogc:Filter>
      	    <ogc:PropertyIsLike wildCard="%" singleChar="_" escapeChar="\">
      	      <ogc:PropertyName>dc:subject</ogc:PropertyName>
      	      <ogc:Literal>%ImageMosaic%</ogc:Literal>
      	    </ogc:PropertyIsLike>
      	  </ogc:Filter>
      	</Constraint>
      </Query>
    </GetRecords>
