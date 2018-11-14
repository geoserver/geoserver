-- data
INSERT INTO collection
("id", "name", "primary", "htmlDescription", "footprint", "timeStart", "timeEnd", "productCqlFilter", "masked", "eoIdentifier", "eoProductType", "eoPlatform", "eoPlatformSerialIdentifier", "eoInstrument", "eoSensorType", "eoCompositeType", "eoProcessingLevel", "eoOrbitType", "eoSpectralRange", "eoWavelength", "eoSecurityConstraints", "eoDissemination", "eoAcquisitionStation")
VALUES(17, 'SENTINEL2', NULL, '<table>
  <tr valign="top">
    <td>
      <b>Title</b>
    </td>
    <td>Sentinel-2</td>
  </tr>
  <tr valign="top">
    <td>
      <b>Description</b>
    </td>
    <td>The SENTINEL-2 mission is a land monitoring constellation of two satellites each equipped with a MSI (Multispectral Imager) instrument covering 13 spectral bands providing high resolution optical imagery (i.e., 10m, 20m, 60 m) every 10 days with one satellite and 5 days with two satellites.</td>
  </tr>
  <tr valign="top">
    <td>
      <b>Type</b>
    </td>
    <td>Collection</td>
  </tr>
  <tr valign="top">
    <td>
      <b>Date</b>
    </td>
    <td>2015-06-23T00:00:00Z/</td>
  </tr>
  <tr valign="top">
    <td>
      <b>Media Type</b>
    </td>
    <td>
      <a href="${ATOM_URL}" title="ATOM format">ATOM</a>
    </td>
  </tr>
  <tr valign="top">
    <td>
      <b>Metadata</b>
    </td>
    <td>
      <a href="${ISO_METADATA_LINK}" title="ISO format">ISO</a>
    </td>
  </tr>
</table>', ST_GeomFromText('POLYGON((-179 89,179 89,179 -89,-179 -89,-179 89))', 4326), '2015-07-01 10:20:21.000', '2016-02-26 10:20:21.000', NULL, NULL, 'SENTINEL2', 'S2MSI1C', 'Sentinel-2', 'A', 'MSI', 'OPTICAL', NULL, 'Level-1C', 'LEO', NULL, NULL, NULL, NULL, NULL);
INSERT INTO collection
("id", "name", "primary", "htmlDescription", "footprint", "timeStart", "timeEnd", "productCqlFilter", "masked", "eoIdentifier", "eoProductType", "eoPlatform", "eoPlatformSerialIdentifier", "eoInstrument", "eoSensorType", "eoCompositeType", "eoProcessingLevel", "eoOrbitType", "eoSpectralRange", "eoWavelength", "eoSecurityConstraints", "eoDissemination", "eoAcquisitionStation")
VALUES(32, 'SENTINEL1', NULL, '<table>
  <tr valign="top">
    <td>
      <b>Title</b>
    </td>
    <td>Landsat-8</td>
  </tr>
  <tr valign="top">
    <td>
      <b>Description</b>
    </td>
    <td>Landsat 8 is an American Earth observation satellite launched on February 11, 2013. It is the eighth satellite in the Landsat program; the seventh to reach orbit successfully.</p><p>Originally called the Landsat Data Continuity Mission (LDCM), it is a collaboration between NASA and the United States Geological Survey (USGS).</p><p>the Landsat 8 Operational Land Imager (OLI) collects data from nine spectral bands. Seven of the nine bands are consistent with the Thematic Mapper (TM) and Enhanced Thematic Mapper Plus (ETM+) sensors found on earlier Landsat satellites, providing for compatibility with the historical Landsat data, while also improving measurement capabilities.</td>
  </tr>
  <tr valign="top">
    <td>
      <b>Type</b>
    </td>
    <td>Collection</td>
  </tr>
  <tr valign="top">
    <td>
      <b>Date</b>
    </td>
    <td>2013-02-12T00:00:00Z/</td>
  </tr>
  <tr valign="top">
    <td>
      <b>Media Type</b>
    </td>
    <td>
      <a href="${ATOM_URL}" title="ATOM format">ATOM</a>
    </td>
  </tr>
  <tr valign="top">
    <td>
      <b>Metadata</b>
    </td>
    <td>
      <a href="${ISO_METADATA_LINK}" title="ISO format">ISO</a>
    </td>
  </tr>
</table>', ST_GeomFromText('POLYGON((-179 89,179 89,179 -89,-179 -89,-179 89))', 4326), '2015-02-26 10:20:21.000', NULL , NULL, NULL, 'SENTINEL1', NULL, 'Sentinel-1', NULL, 'SAR', 'RADAR', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO collection
("id", "name", "primary", "htmlDescription", "footprint", "timeStart", "timeEnd", "productCqlFilter", "masked", "eoIdentifier", "eoProductType", "eoPlatform", "eoPlatformSerialIdentifier", "eoInstrument", "eoSensorType", "eoCompositeType", "eoProcessingLevel", "eoOrbitType", "eoSpectralRange", "eoWavelength", "eoSecurityConstraints", "eoDissemination", "eoAcquisitionStation")
VALUES(31, 'LANDSAT8', NULL, '<table>
  <tr valign="top">
    <td>
      <b>Title</b>
    </td>
    <td>Sentinel-1 (PEPS)</td>
  </tr>
  <tr valign="top">
    <td>
      <b>Description</b>
    </td>
    <td>The SENTINEL-1 mission comprises a constellation of two polar-orbiting satellites, operating day and night performing C-band Synthetic Aperture Radar (SAR) imaging, enabling them to acquire imagery regardless of the weather. Sentinel-1 is operated in four imaging modes with different resolutions (down to 10 m) and coverage (up to 400 km swath), offering reliable wide area monitoring every 12 days with one satellite and 6 days with two satellites.</td>
  </tr>
  <tr valign="top">
    <td>
      <b>Type</b>
    </td>
    <td>Collection</td>
  </tr>
  <tr valign="top">
    <td>
      <b>Date</b>
    </td>
    <td>2014-04-03T00:00:00Z/</td>
  </tr>
  <tr valign="top">
    <td>
      <b>Media Type</b>
    </td>
    <td>
      <a href="${ATOM_URL}" title="ATOM format">ATOM</a>
    </td>
  </tr>
  <tr valign="top">
    <td>
      <b>Metadata</b>
    </td>
    <td>
      <a href="${ISO_METADATA_LINK}" title="ISO format">ISO</a>
    </td>
  </tr>
</table>', ST_GeomFromText('POLYGON((-179 89,179 89,179 -89,-179 -89,-179 89))', 4326), '1988-02-26 10:20:21.000', '2013-03-01 10:20:21.000', NULL, NULL, 'LANDSAT8', NULL, '', NULL, 'OLI', 'OPTICAL', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO collection
("id", "name", "primary", "htmlDescription", "footprint", "timeStart", "timeEnd", "productCqlFilter", "masked", "eoIdentifier", "eoProductType", "eoPlatform", "eoPlatformSerialIdentifier", "eoInstrument", "eoSensorType", "eoCompositeType", "eoProcessingLevel", "eoOrbitType", "eoSpectralRange", "eoWavelength", "eoSecurityConstraints", "eoDissemination", "eoAcquisitionStation")
VALUES(18, 'ATMTEST', NULL, '<table>
  <tr valign="top">
    <td>
      <b>Title</b>
    </td>
    <td>The sample atmospheric sensor (SAS)</td>
  </tr>
</table>', ST_GeomFromText('POLYGON((-179 89,179 89,179 -89,-179 -89,-179 89))', 4326), '2018-02-26 10:20:21.000', '2018-03-01 10:20:21.000', NULL, NULL, 'SAS1', NULL, '', NULL, 'OLI', 'ATMOSPHERIC', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
-- setup sequence to allow new inserts
select setval('collection_id_seq'::regclass, 33);
-- metadata
INSERT INTO collection_metadata ("mid","metadata") VALUES (
17,'<?xml version="1.0" encoding="UTF-8"?>
<gmi:MI_Metadata xmlns:gmi="http://www.isotc211.org/2005/gmi" xmlns:atom="http://www.w3.org/2005/Atom"
  xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:eo="http://a9.com/-/opensearch/extensions/eo/1.0/"
  xmlns:gco="http://www.isotc211.org/2005/gco" xmlns:geo="http://a9.com/-/opensearch/extensions/geo/1.0/"
  xmlns:georss="http://www.georss.org/georss" xmlns:gmd="http://www.isotc211.org/2005/gmd"
  xmlns:gml="http://www.opengis.net/gml/3.2" xmlns:gmx="http://www.isotc211.org/2005/gmx" xmlns:os="http://a9.com/-/spec/opensearch/1.1/"
  xmlns:semantic="http://a9.com/-/opensearch/extensions/semantic/1.0/" xmlns:sru="http://a9.com/-/opensearch/extensions/sru/2.0/"
  xmlns:time="http://a9.com/-/opensearch/extensions/time/1.0/" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.isotc211.org/2005/gmi ..\..\schema/ISO/gmi/gmi.xsd "
>
  <gmd:fileIdentifier>
    <gco:CharacterString>EOP:CNES:PEPS:S2</gco:CharacterString>
  </gmd:fileIdentifier>
  <gmd:language>
    <gmd:LanguageCode
      codeList="http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/resources/codelist/ML_gmxCodelists.xml#LanguageCode"
      codeListValue="eng"
    >eng</gmd:LanguageCode>
  </gmd:language>
  <gmd:hierarchyLevel>
    <gmd:MD_ScopeCode
      codeList="http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/resources/Codelist/ML_gmx Codelists.xml#MD_ScopeCode"
      codeListValue="series"
    >series</gmd:MD_ScopeCode>
  </gmd:hierarchyLevel>
  <gmd:contact>
    <gmd:CI_ResponsibleParty>
      <gmd:individualName>
        <gco:CharacterString>CustomerTechnicalSupport</gco:CharacterString>
      </gmd:individualName>
      <gmd:organisationName>
        <gco:CharacterString>CNES</gco:CharacterString>
      </gmd:organisationName>
      <gmd:contactInfo>
        <gmd:CI_Contact>
          <gmd:address>
            <gmd:CI_Address>
              <gmd:deliveryPoint>
                <gco:CharacterString>18 avenue Edouard Belin</gco:CharacterString>
              </gmd:deliveryPoint>
              <gmd:city>
                <gco:CharacterString>Toulouse</gco:CharacterString>
              </gmd:city>
              <gmd:postalCode>
                <gco:CharacterString>31401</gco:CharacterString>
              </gmd:postalCode>
              <gmd:country>
                <gco:CharacterString>France</gco:CharacterString>
              </gmd:country>
              <gmd:electronicMailAddress>
                <gco:CharacterString>jerome.gasperi@cnes.fr</gco:CharacterString>
              </gmd:electronicMailAddress>
            </gmd:CI_Address>
          </gmd:address>
        </gmd:CI_Contact>
      </gmd:contactInfo>
      <gmd:role>
        <gmd:CI_RoleCode codeList="http://www.isotc211.org/2005/resources/codeList.xml#CI_RoleCode"
          codeListValue="publisher" />
      </gmd:role>
    </gmd:CI_ResponsibleParty>
  </gmd:contact>
  <gmd:dateStamp>
    <gco:Date>2016-08-31</gco:Date>
  </gmd:dateStamp>
  <gmd:metadataStandardName>
    <gco:CharacterString>ISO19115</gco:CharacterString>
  </gmd:metadataStandardName>
  <gmd:metadataStandardVersion>
    <gco:CharacterString>2003/Cor.1:2006</gco:CharacterString>
  </gmd:metadataStandardVersion>
  <gmd:identificationInfo>
    <gmd:MD_DataIdentification>
      <gmd:citation>
        <gmd:CI_Citation>
          <gmd:title>
            <gco:CharacterString>Sentinel-2 (PEPS)</gco:CharacterString>
          </gmd:title>
          <gmd:date>
            <gmd:CI_Date>
              <gmd:date>
                <gco:Date>2015-06-23</gco:Date>
              </gmd:date>
              <gmd:dateType>
                <gmd:CI_DateTypeCode
                  codeList="http://www.isotc211.org/2005/resources/codeList.xml#CI_DateTypeCode"
                  codeListValue="creation" />
              </gmd:dateType>
            </gmd:CI_Date>
          </gmd:date>
          <gmd:identifier>
            <gmd:RS_Identifier>
              <gmd:code>
                <gco:CharacterString>EOP:CNES:PEPS:S2</gco:CharacterString>
              </gmd:code>
              <gmd:codeSpace>
                <gco:CharacterString>http://peps.cnes.fr</gco:CharacterString>
              </gmd:codeSpace>
            </gmd:RS_Identifier>
          </gmd:identifier>
        </gmd:CI_Citation>
      </gmd:citation>
      <gmd:abstract>
        <gco:CharacterString>The SENTINEL-2 mission is a land monitoring constellation of two
          satellites each equipped with a MSI (Multispectral Imager) instrument covering 13 spectral
          bands providing high resolution optical imagery (i.e., 10m, 20m, 60 m) every 10 days with
          one satellite and 5 days with two satellites.</gco:CharacterString>
      </gmd:abstract>
      <gmd:pointOfContact>
        <gmd:CI_ResponsibleParty>
          <gmd:organisationName>
            <gco:CharacterString>CNES</gco:CharacterString>
          </gmd:organisationName>
          <gmd:positionName>
            <gco:CharacterString>CustomerTechnicalSupport</gco:CharacterString>
          </gmd:positionName>
          <gmd:contactInfo>
            <gmd:CI_Contact>
              <gmd:address>
                <gmd:CI_Address>
                  <gmd:deliveryPoint>
                    <gco:CharacterString>18 avenue Edouard Belin</gco:CharacterString>
                  </gmd:deliveryPoint>
                  <gmd:city>
                    <gco:CharacterString>Toulouse</gco:CharacterString>
                  </gmd:city>
                  <gmd:postalCode>
                    <gco:CharacterString>31401</gco:CharacterString>
                  </gmd:postalCode>
                  <gmd:country>
                    <gco:CharacterString>France</gco:CharacterString>
                  </gmd:country>
                  <gmd:electronicMailAddress>
                    <gco:CharacterString>jerome.gasperi@cnes.fr</gco:CharacterString>
                  </gmd:electronicMailAddress>
                </gmd:CI_Address>
              </gmd:address>
              <gmd:onlineResource>
                <gmd:CI_OnlineResource>
                  <gmd:linkage>
                    <gmd:URL>http://peps.cnes.fr/</gmd:URL>
                  </gmd:linkage>
                </gmd:CI_OnlineResource>
              </gmd:onlineResource>
            </gmd:CI_Contact>
          </gmd:contactInfo>
          <gmd:role>
            <gmd:CI_RoleCode
              codeList="http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/resources/codelist/ML_gmxCodelists.xml#CI_RoleCode"
              codeListValue="originator"
            > originator            </gmd:CI_RoleCode>
          </gmd:role>
        </gmd:CI_ResponsibleParty>
      </gmd:pointOfContact>
      <gmd:descriptiveKeywords>
        <gmd:MD_Keywords>
          <gmd:keyword>
            <gco:CharacterString>FedEO</gco:CharacterString>
          </gmd:keyword>
        </gmd:MD_Keywords>
      </gmd:descriptiveKeywords>
      <gmd:descriptiveKeywords>
        <gmd:MD_Keywords>
          <gmd:keyword>
            <gco:CharacterString>PEPS</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>CNES</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>optical</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>sentinel2</gco:CharacterString>
          </gmd:keyword>
        </gmd:MD_Keywords>
      </gmd:descriptiveKeywords>
      <gmd:descriptiveKeywords>
        <gmd:MD_Keywords>
          <gmd:keyword>
            <gmx:Anchor xlink:href="http://www.eionet.europa.eu/gemet/concept/4599">land</gmx:Anchor>
          </gmd:keyword>
          <gmd:keyword>
            <gmx:Anchor xlink:href="http://www.eionet.europa.eu/gemet/concept/4612">land cover</gmx:Anchor>
          </gmd:keyword>
          <gmd:keyword>
            <gmx:Anchor xlink:href="http://www.eionet.europa.eu/gemet/concept/1391">chlorophyll</gmx:Anchor>
          </gmd:keyword>
          <gmd:keyword>
            <gmx:Anchor xlink:href="http://www.eionet.europa.eu/gemet/concept/5496">natural disaster</gmx:Anchor>
          </gmd:keyword>
          <gmd:type>
            <gmd:MD_KeywordTypeCode
              codeList="http://www.isotc211.org/2005/resources/codeList.xml#MD_KeywordTypeCode"
              codeListValue="theme" />
          </gmd:type>
          <gmd:thesaurusName>
            <gmd:CI_Citation>
              <gmd:title>
                <gmx:Anchor xlink:href="http://www.eionet.europa.eu/gemet">GEMET - INSPIRE Themes, Version 1.0</gmx:Anchor>
              </gmd:title>
              <gmd:date>
                <gmd:CI_Date>
                  <gmd:date>
                    <gco:Date>2008-06-01</gco:Date>
                  </gmd:date>
                  <gmd:dateType>
                    <gmd:CI_DateTypeCode
                      codeList="http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/resources/codelist/ML_gmxCodelists.xml#CI_DateTypeCode"
                      codeListValue="publication"
                    >publication</gmd:CI_DateTypeCode>
                  </gmd:dateType>
                </gmd:CI_Date>
              </gmd:date>
            </gmd:CI_Citation>
          </gmd:thesaurusName>
        </gmd:MD_Keywords>
      </gmd:descriptiveKeywords>
      <gmd:resourceConstraints>
        <gmd:MD_Constraints>
          <gmd:useLimitation>
            <gco:CharacterString>http://earth.esa.int/dataproducts/accessingeodata/
            </gco:CharacterString>
          </gmd:useLimitation>
        </gmd:MD_Constraints>
      </gmd:resourceConstraints>
      <gmd:resourceConstraints>
        <gmd:MD_LegalConstraints>
          <gmd:accessConstraints>
            <gmd:MD_RestrictionCode
              codeList="http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/resources/codelist/ML_gmxCodelists.xml#MD_RestrictionCode"
              codeListValue="otherRestrictions"
            >otherRestrictions</gmd:MD_RestrictionCode>
          </gmd:accessConstraints>
        </gmd:MD_LegalConstraints>
      </gmd:resourceConstraints>
      <gmd:language>
        <gmd:LanguageCode
          codeList="http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/resources/codelist/ML_gmxCodelists.xml#LanguageCode"
          codeListValue="eng"
        >eng</gmd:LanguageCode>
      </gmd:language>
      <gmd:extent>
        <gmd:EX_Extent>
          <gmd:geographicElement>
            <gmd:EX_GeographicBoundingBox>
              <gmd:westBoundLongitude>
                <gco:Decimal>-180</gco:Decimal>
              </gmd:westBoundLongitude>
              <gmd:eastBoundLongitude>
                <gco:Decimal>180</gco:Decimal>
              </gmd:eastBoundLongitude>
              <gmd:southBoundLatitude>
                <gco:Decimal>-90</gco:Decimal>
              </gmd:southBoundLatitude>
              <gmd:northBoundLatitude>
                <gco:Decimal>90</gco:Decimal>
              </gmd:northBoundLatitude>
            </gmd:EX_GeographicBoundingBox>
          </gmd:geographicElement>
        </gmd:EX_Extent>
      </gmd:extent>
      <gmd:extent>
        <gmd:EX_Extent>
          <gmd:temporalElement>
            <gmd:EX_TemporalExtent>
              <gmd:extent>
                <gml:TimePeriod gml:id="timeperiod1">
                  <gml:beginPosition>2015-06-23</gml:beginPosition>
                  <gml:endPosition />
                </gml:TimePeriod>
              </gmd:extent>
            </gmd:EX_TemporalExtent>
          </gmd:temporalElement>
        </gmd:EX_Extent>
      </gmd:extent>
    </gmd:MD_DataIdentification>
  </gmd:identificationInfo>
  <gmd:distributionInfo>
    <gmd:MD_Distribution>
      <gmd:distributionFormat>
        <gmd:MD_Format>
          <gmd:name gco:nilReason="inapplicable" />
          <gmd:version gco:nilReason="inapplicable" />
        </gmd:MD_Format>
      </gmd:distributionFormat>
      <gmd:transferOptions>
        <gmd:MD_DigitalTransferOptions>
          <gmd:onLine>
            <gmd:CI_OnlineResource>
              <gmd:linkage>
                <gmd:URL>http://fedeo.esa.int/opensearch/description.xml?parentIdentifier=EOP:CNES:PEPS:S2&amp;sensorType=OPTICAL&amp;platform=S2A
                </gmd:URL>
              </gmd:linkage>
              <gmd:name>
                <gco:CharacterString>FedEO Clearinghouse</gco:CharacterString>
              </gmd:name>
              <gmd:description>
                <gco:CharacterString>FedEO Clearinghouse</gco:CharacterString>
              </gmd:description>
              <gmd:function>
                <gmd:CI_OnLineFunctionCode
                  codeList="http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/resources/codelist/ML_gmxCodelists.xml#CI_OnLineFunctionCode"
                  codeListValue="search" />
              </gmd:function>
            </gmd:CI_OnlineResource>
          </gmd:onLine>
        </gmd:MD_DigitalTransferOptions>
      </gmd:transferOptions>
    </gmd:MD_Distribution>
  </gmd:distributionInfo>
  <gmd:dataQualityInfo>
    <gmd:DQ_DataQuality>
      <gmd:scope>
        <gmd:DQ_Scope>
          <gmd:level>
            <gmd:MD_ScopeCode
              codeList="http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/resources/codelist/ML_gmxCodelists.xml#MD_ScopeCode"
              codeListValue="series"
            >series</gmd:MD_ScopeCode>
          </gmd:level>
        </gmd:DQ_Scope>
      </gmd:scope>
      <gmd:report>
        <gmd:DQ_DomainConsistency>
          <gmd:result>
            <gmd:DQ_ConformanceResult>
              <gmd:specification>
                <gmd:CI_Citation>
                  <gmd:title>
                    <gco:CharacterString>COMMISSION REGULATION (EU) No 1089/2010 of 23 November 2010
                      implementing Directive 2007/2/EC of the European Parliament and of the Council
                      as regards interoperability of spatial data sets and services
                    </gco:CharacterString>
                  </gmd:title>
                  <gmd:date>
                    <gmd:CI_Date>
                      <gmd:date>
                        <gco:Date>2010-12-08</gco:Date>
                      </gmd:date>
                      <gmd:dateType>
                        <gmd:CI_DateTypeCode
                          codeList="http://www.isotc211.org/2005/resources/codeList.xml#CI_DateTypeCode"
                          codeListValue="publication" />
                      </gmd:dateType>
                    </gmd:CI_Date>
                  </gmd:date>
                </gmd:CI_Citation>
              </gmd:specification>
              <gmd:explanation>
                <gco:CharacterString>non testé</gco:CharacterString>
              </gmd:explanation>
              <gmd:pass>
                <gco:Boolean>false</gco:Boolean>
              </gmd:pass>
            </gmd:DQ_ConformanceResult>
          </gmd:result>
        </gmd:DQ_DomainConsistency>
      </gmd:report>
      <gmd:lineage>
        <gmd:LI_Lineage>
          <gmd:statement>
            <gco:CharacterString>SENTINEL 2 MSI</gco:CharacterString>
          </gmd:statement>
        </gmd:LI_Lineage>
      </gmd:lineage>
    </gmd:DQ_DataQuality>
  </gmd:dataQualityInfo>
  <gmi:acquisitionInformation>
    <gmi:MI_AcquisitionInformation>
      <gmi:platform>
        <gmi:MI_Platform>
          <gmi:identifier>
            <gmd:MD_Identifier>
              <gmd:code>
                <gmx:Anchor
                  xlink:href="http://gcmdservices.gsfc.nasa.gov/kms/concept/2ce20983-98b2-40b9-bb0e-a08074fb93b3"
                >SENTINEL-2</gmx:Anchor>
              </gmd:code>
            </gmd:MD_Identifier>
          </gmi:identifier>
          <gmi:description>
            <gco:CharacterString>SENTINEL-2</gco:CharacterString>
          </gmi:description>
          <gmi:instrument>
            <gmi:MI_Instrument>
              <gmi:citation>
                <gmd:CI_Citation>
                  <gmd:title>
                    <gco:CharacterString>MultiSpectral Instrument (MSI)</gco:CharacterString>
                  </gmd:title>
                  <gmd:date>
                    <gmd:CI_Date>
                      <gmd:date>
                        <gco:Date>2015-06-23</gco:Date>
                      </gmd:date>
                      <gmd:dateType>
                        <gmd:CI_DateTypeCode
                          codeList="http://www.isotc211.org/2005/resources/codeList.xml#CI_DateTypeCode"
                          codeListValue="creation"
                        >creation</gmd:CI_DateTypeCode>
                      </gmd:dateType>
                    </gmd:CI_Date>
                  </gmd:date>
                  <gmd:identifier>
                    <gmd:RS_Identifier>
                      <gmd:code>
                        <gco:CharacterString>MSI</gco:CharacterString>
                      </gmd:code>
                    </gmd:RS_Identifier>
                  </gmd:identifier>
                </gmd:CI_Citation>
              </gmi:citation>
              <gmi:type>
                <gmi:MI_SensorTypeCode />
              </gmi:type>
            </gmi:MI_Instrument>
          </gmi:instrument>
        </gmi:MI_Platform>
      </gmi:platform>
    </gmi:MI_AcquisitionInformation>
  </gmi:acquisitionInformation>
</gmi:MI_Metadata>');
INSERT INTO collection_metadata ("mid","metadata") VALUES (
32,'<?xml version="1.0" encoding="UTF-8"?>
<gmi:MI_Metadata xmlns:gmi="http://www.isotc211.org/2005/gmi" xmlns:atom="http://www.w3.org/2005/Atom"
  xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:eo="http://a9.com/-/opensearch/extensions/eo/1.0/"
  xmlns:gco="http://www.isotc211.org/2005/gco" xmlns:geo="http://a9.com/-/opensearch/extensions/geo/1.0/"
  xmlns:georss="http://www.georss.org/georss" xmlns:gmd="http://www.isotc211.org/2005/gmd"
  xmlns:gml="http://www.opengis.net/gml/3.2" xmlns:gmx="http://www.isotc211.org/2005/gmx" xmlns:os="http://a9.com/-/spec/opensearch/1.1/"
  xmlns:semantic="http://a9.com/-/opensearch/extensions/semantic/1.0/" xmlns:sru="http://a9.com/-/opensearch/extensions/sru/2.0/"
  xmlns:time="http://a9.com/-/opensearch/extensions/time/1.0/" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.isotc211.org/2005/gmi ..\..\schema/ISO/gmi/gmi.xsd "
>
  <gmd:fileIdentifier>
    <gco:CharacterString>EOP:CNES:PEPS:S1</gco:CharacterString>
  </gmd:fileIdentifier>
  <gmd:language>
    <gmd:LanguageCode
      codeList="http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/resources/codelist/ML_gmxCodelists.xml#LanguageCode"
      codeListValue="eng"
    >eng</gmd:LanguageCode>
  </gmd:language>
  <gmd:hierarchyLevel>
    <gmd:MD_ScopeCode
      codeList="http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/resources/Codelist/ML_gmx Codelists.xml#MD_ScopeCode"
      codeListValue="series"
    >series</gmd:MD_ScopeCode>
  </gmd:hierarchyLevel>
  <gmd:contact>
    <gmd:CI_ResponsibleParty>
      <gmd:individualName>
        <gco:CharacterString>CustomerTechnicalSupport</gco:CharacterString>
      </gmd:individualName>
      <gmd:organisationName>
        <gco:CharacterString>CNES</gco:CharacterString>
      </gmd:organisationName>
      <gmd:contactInfo>
        <gmd:CI_Contact>
          <gmd:address>
            <gmd:CI_Address>
              <gmd:deliveryPoint>
                <gco:CharacterString>18 avenue Edouard Belin</gco:CharacterString>
              </gmd:deliveryPoint>
              <gmd:city>
                <gco:CharacterString>Toulouse</gco:CharacterString>
              </gmd:city>
              <gmd:postalCode>
                <gco:CharacterString>31401</gco:CharacterString>
              </gmd:postalCode>
              <gmd:country>
                <gco:CharacterString>France</gco:CharacterString>
              </gmd:country>
              <gmd:electronicMailAddress>
                <gco:CharacterString>jerome.gasperi@cnes.fr</gco:CharacterString>
              </gmd:electronicMailAddress>
            </gmd:CI_Address>
          </gmd:address>
        </gmd:CI_Contact>
      </gmd:contactInfo>
      <gmd:role>
        <gmd:CI_RoleCode codeList="http://www.isotc211.org/2005/resources/codeList.xml#CI_RoleCode"
          codeListValue="publisher" />
      </gmd:role>
    </gmd:CI_ResponsibleParty>
  </gmd:contact>
  <gmd:dateStamp>
    <gco:Date>2016-08-31</gco:Date>
  </gmd:dateStamp>
  <gmd:metadataStandardName>
    <gco:CharacterString>ISO19115</gco:CharacterString>
  </gmd:metadataStandardName>
  <gmd:metadataStandardVersion>
    <gco:CharacterString>2003/Cor.1:2006</gco:CharacterString>
  </gmd:metadataStandardVersion>
  <gmd:identificationInfo>
    <gmd:MD_DataIdentification>
      <gmd:citation>
        <gmd:CI_Citation>
          <gmd:title>
            <gco:CharacterString>Sentinel-1 (PEPS)</gco:CharacterString>
          </gmd:title>
          <gmd:date>
            <gmd:CI_Date>
              <gmd:date>
                <gco:Date>2014-04-03</gco:Date>
              </gmd:date>
              <gmd:dateType>
                <gmd:CI_DateTypeCode
                  codeList="http://www.isotc211.org/2005/resources/codeList.xml#CI_DateTypeCode"
                  codeListValue="creation" />
              </gmd:dateType>
            </gmd:CI_Date>
          </gmd:date>
          <gmd:identifier>
            <gmd:RS_Identifier>
              <gmd:code>
                <gco:CharacterString>EOP:CNES:PEPS:S1</gco:CharacterString>
              </gmd:code>
              <gmd:codeSpace>
                <gco:CharacterString>http://peps.cnes.fr</gco:CharacterString>
              </gmd:codeSpace>
            </gmd:RS_Identifier>
          </gmd:identifier>
        </gmd:CI_Citation>
      </gmd:citation>
      <gmd:abstract>
        <gco:CharacterString>The SENTINEL-1 mission comprises a constellation of two polar-orbiting
          satellites, operating day and night performing C-band Synthetic Aperture Radar (SAR)
          imaging, enabling them to acquire imagery regardless of the weather. Sentinel-1 is
          operated in four imaging modes with different resolutions (down to 10 m) and coverage (up
          to 400 km swath), offering reliable wide area monitoring every 12 days with one satellite
          and 6 days with two satellites.</gco:CharacterString>
      </gmd:abstract>
      <gmd:pointOfContact>
        <gmd:CI_ResponsibleParty>
          <gmd:organisationName>
            <gco:CharacterString>CNES</gco:CharacterString>
          </gmd:organisationName>
          <gmd:positionName>
            <gco:CharacterString>CustomerTechnicalSupport</gco:CharacterString>
          </gmd:positionName>
          <gmd:contactInfo>
            <gmd:CI_Contact>
              <gmd:address>
                <gmd:CI_Address>
                  <gmd:deliveryPoint>
                    <gco:CharacterString>18 avenue Edouard Belin</gco:CharacterString>
                  </gmd:deliveryPoint>
                  <gmd:city>
                    <gco:CharacterString>Toulouse</gco:CharacterString>
                  </gmd:city>
                  <gmd:postalCode>
                    <gco:CharacterString>31401</gco:CharacterString>
                  </gmd:postalCode>
                  <gmd:country>
                    <gco:CharacterString>France</gco:CharacterString>
                  </gmd:country>
                  <gmd:electronicMailAddress>
                    <gco:CharacterString>jerome.gasperi@cnes.fr</gco:CharacterString>
                  </gmd:electronicMailAddress>
                </gmd:CI_Address>
              </gmd:address>
              <gmd:onlineResource>
                <gmd:CI_OnlineResource>
                  <gmd:linkage>
                    <gmd:URL>http://peps.cnes.fr/</gmd:URL>
                  </gmd:linkage>
                </gmd:CI_OnlineResource>
              </gmd:onlineResource>
            </gmd:CI_Contact>
          </gmd:contactInfo>
          <gmd:role>
            <gmd:CI_RoleCode
              codeList="http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/resources/codelist/ML_gmxCodelists.xml#CI_RoleCode"
              codeListValue="originator"
            > originator            </gmd:CI_RoleCode>
          </gmd:role>
        </gmd:CI_ResponsibleParty>
      </gmd:pointOfContact>
      <gmd:descriptiveKeywords>
        <gmd:MD_Keywords>
          <gmd:keyword>
            <gco:CharacterString>FedEO</gco:CharacterString>
          </gmd:keyword>
        </gmd:MD_Keywords>
      </gmd:descriptiveKeywords>
      <gmd:descriptiveKeywords>
        <gmd:MD_Keywords>
          <gmd:keyword>
            <gco:CharacterString>PEPS</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>CNES</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>level1</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>radar</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>sentinel1</gco:CharacterString>
          </gmd:keyword>
        </gmd:MD_Keywords>
      </gmd:descriptiveKeywords>
      <gmd:descriptiveKeywords>
        <gmd:MD_Keywords>
          <gmd:keyword>
            <gmx:Anchor xlink:href="http://www.eionet.europa.eu/gemet/concept/4599">land</gmx:Anchor>
          </gmd:keyword>
          <gmd:keyword>
            <gmx:Anchor xlink:href="http://www.eionet.europa.eu/gemet/concept/4612">land cover</gmx:Anchor>
          </gmd:keyword>
          <gmd:keyword>
            <gmx:Anchor xlink:href="http://www.eionet.europa.eu/gemet/concept/7495">sea</gmx:Anchor>
          </gmd:keyword>
          <gmd:keyword>
            <gmx:Anchor xlink:href="http://www.eionet.europa.eu/gemet/concept/4131">ice</gmx:Anchor>
          </gmd:keyword>
          <gmd:keyword>
            <gmx:Anchor xlink:href="http://www.eionet.europa.eu/gemet/concept/5496">natural disaster</gmx:Anchor>
          </gmd:keyword>
          <gmd:type>
            <gmd:MD_KeywordTypeCode
              codeList="http://www.isotc211.org/2005/resources/codeList.xml#MD_KeywordTypeCode"
              codeListValue="theme" />
          </gmd:type>
          <gmd:thesaurusName>
            <gmd:CI_Citation>
              <gmd:title>
                <gmx:Anchor xlink:href="http://www.eionet.europa.eu/gemet">GEMET - INSPIRE Themes, Version 1.0</gmx:Anchor>
              </gmd:title>
              <gmd:date>
                <gmd:CI_Date>
                  <gmd:date>
                    <gco:Date>2008-06-01</gco:Date>
                  </gmd:date>
                  <gmd:dateType>
                    <gmd:CI_DateTypeCode
                      codeList="http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/resources/codelist/ML_gmxCodelists.xml#CI_DateTypeCode"
                      codeListValue="publication"
                    >publication</gmd:CI_DateTypeCode>
                  </gmd:dateType>
                </gmd:CI_Date>
              </gmd:date>
            </gmd:CI_Citation>
          </gmd:thesaurusName>
        </gmd:MD_Keywords>
      </gmd:descriptiveKeywords>
      <gmd:resourceConstraints>
        <gmd:MD_Constraints>
          <gmd:useLimitation>
            <gco:CharacterString>http://earth.esa.int/dataproducts/accessingeodata/
            </gco:CharacterString>
          </gmd:useLimitation>
        </gmd:MD_Constraints>
      </gmd:resourceConstraints>
      <gmd:resourceConstraints>
        <gmd:MD_LegalConstraints>
          <gmd:accessConstraints>
            <gmd:MD_RestrictionCode
              codeList="http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/resources/codelist/ML_gmxCodelists.xml#MD_RestrictionCode"
              codeListValue="otherRestrictions"
            >otherRestrictions</gmd:MD_RestrictionCode>
          </gmd:accessConstraints>
        </gmd:MD_LegalConstraints>
      </gmd:resourceConstraints>
      <gmd:language>
        <gmd:LanguageCode
          codeList="http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/resources/codelist/ML_gmxCodelists.xml#LanguageCode"
          codeListValue="eng"
        >eng</gmd:LanguageCode>
      </gmd:language>
      <gmd:extent>
        <gmd:EX_Extent>
          <gmd:geographicElement>
            <gmd:EX_GeographicBoundingBox>
              <gmd:westBoundLongitude>
                <gco:Decimal>-180</gco:Decimal>
              </gmd:westBoundLongitude>
              <gmd:eastBoundLongitude>
                <gco:Decimal>180</gco:Decimal>
              </gmd:eastBoundLongitude>
              <gmd:southBoundLatitude>
                <gco:Decimal>-90</gco:Decimal>
              </gmd:southBoundLatitude>
              <gmd:northBoundLatitude>
                <gco:Decimal>90</gco:Decimal>
              </gmd:northBoundLatitude>
            </gmd:EX_GeographicBoundingBox>
          </gmd:geographicElement>
        </gmd:EX_Extent>
      </gmd:extent>
      <gmd:extent>
        <gmd:EX_Extent>
          <gmd:temporalElement>
            <gmd:EX_TemporalExtent>
              <gmd:extent>
                <gml:TimePeriod gml:id="timeperiod1">
                  <gml:beginPosition>2014-04-03</gml:beginPosition>
                  <gml:endPosition />
                </gml:TimePeriod>
              </gmd:extent>
            </gmd:EX_TemporalExtent>
          </gmd:temporalElement>
        </gmd:EX_Extent>
      </gmd:extent>
    </gmd:MD_DataIdentification>
  </gmd:identificationInfo>
  <gmd:distributionInfo>
    <gmd:MD_Distribution>
      <gmd:distributionFormat>
        <gmd:MD_Format>
          <gmd:name gco:nilReason="inapplicable" />
          <gmd:version gco:nilReason="inapplicable" />
        </gmd:MD_Format>
      </gmd:distributionFormat>
      <gmd:transferOptions>
        <gmd:MD_DigitalTransferOptions>
          <gmd:onLine>
            <gmd:CI_OnlineResource>
              <gmd:linkage>
                <gmd:URL>http://fedeo.esa.int/opensearch/description.xml?parentIdentifier=EOP:CNES:PEPS:S1&amp;sensorType=RADAR&amp;platform=S1A
                </gmd:URL>
              </gmd:linkage>
              <gmd:name>
                <gco:CharacterString>FedEO Clearinghouse</gco:CharacterString>
              </gmd:name>
              <gmd:description>
                <gco:CharacterString>FedEO Clearinghouse</gco:CharacterString>
              </gmd:description>
              <gmd:function>
                <gmd:CI_OnLineFunctionCode
                  codeList="http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/resources/codelist/ML_gmxCodelists.xml#CI_OnLineFunctionCode"
                  codeListValue="search" />
              </gmd:function>
            </gmd:CI_OnlineResource>
          </gmd:onLine>
        </gmd:MD_DigitalTransferOptions>
      </gmd:transferOptions>
    </gmd:MD_Distribution>
  </gmd:distributionInfo>
  <gmd:dataQualityInfo>
    <gmd:DQ_DataQuality>
      <gmd:scope>
        <gmd:DQ_Scope>
          <gmd:level>
            <gmd:MD_ScopeCode
              codeList="http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/resources/codelist/ML_gmxCodelists.xml#MD_ScopeCode"
              codeListValue="series"
            >series</gmd:MD_ScopeCode>
          </gmd:level>
        </gmd:DQ_Scope>
      </gmd:scope>
      <gmd:report>
        <gmd:DQ_DomainConsistency>
          <gmd:result>
            <gmd:DQ_ConformanceResult>
              <gmd:specification>
                <gmd:CI_Citation>
                  <gmd:title>
                    <gco:CharacterString>COMMISSION REGULATION (EU) No 1089/2010 of 23 November 2010
                      implementing Directive 2007/2/EC of the European Parliament and of the Council
                      as regards interoperability of spatial data sets and services
                    </gco:CharacterString>
                  </gmd:title>
                  <gmd:date>
                    <gmd:CI_Date>
                      <gmd:date>
                        <gco:Date>2010-12-08</gco:Date>
                      </gmd:date>
                      <gmd:dateType>
                        <gmd:CI_DateTypeCode
                          codeList="http://www.isotc211.org/2005/resources/codeList.xml#CI_DateTypeCode"
                          codeListValue="publication" />
                      </gmd:dateType>
                    </gmd:CI_Date>
                  </gmd:date>
                </gmd:CI_Citation>
              </gmd:specification>
              <gmd:explanation>
                <gco:CharacterString>non testé</gco:CharacterString>
              </gmd:explanation>
              <gmd:pass>
                <gco:Boolean>false</gco:Boolean>
              </gmd:pass>
            </gmd:DQ_ConformanceResult>
          </gmd:result>
        </gmd:DQ_DomainConsistency>
      </gmd:report>
      <gmd:lineage>
        <gmd:LI_Lineage>
          <gmd:statement>
            <gco:CharacterString>SENTINEL 1 C-SAR  </gco:CharacterString>
          </gmd:statement>
        </gmd:LI_Lineage>
      </gmd:lineage>
    </gmd:DQ_DataQuality>
  </gmd:dataQualityInfo>
  <gmi:acquisitionInformation>
    <gmi:MI_AcquisitionInformation>
      <gmi:platform>
        <gmi:MI_Platform>
          <gmi:identifier>
            <gmd:MD_Identifier>
              <gmd:code>
                <gmx:Anchor
                  xlink:href="http://gcmdservices.gsfc.nasa.gov/kms/concept/c7279e54-f7c1-4ee7-a957-719d6021a3f6"
                >SENTINEL-1A</gmx:Anchor>
              </gmd:code>
            </gmd:MD_Identifier>
          </gmi:identifier>
          <gmi:description>
            <gco:CharacterString>SENTINEL-1A</gco:CharacterString>
          </gmi:description>
          <gmi:instrument>
            <gmi:MI_Instrument>
              <gmi:citation>
                <gmd:CI_Citation>
                  <gmd:title>
                    <gco:CharacterString>C-SAR</gco:CharacterString>
                  </gmd:title>
                  <gmd:date>
                    <gmd:CI_Date>
                      <gmd:date>
                        <gco:Date>2009-06-11</gco:Date>
                      </gmd:date>
                      <gmd:dateType>
                        <gmd:CI_DateTypeCode
                          codeList="http://www.isotc211.org/2005/resources/codeList.xml#CI_DateTypeCode"
                          codeListValue="creation"
                        >creation</gmd:CI_DateTypeCode>
                      </gmd:dateType>
                    </gmd:CI_Date>
                  </gmd:date>
                  <gmd:identifier>
                    <gmd:RS_Identifier>
                      <gmd:code>
                        <gmx:Anchor
                          xlink:href="http://gcmdservices.gsfc.nasa.gov/kms/concept/1c53d85e-3792-4081-9748-192fd3140aa6"
                        >C-SAR</gmx:Anchor>
                      </gmd:code>
                    </gmd:RS_Identifier>
                  </gmd:identifier>
                </gmd:CI_Citation>
              </gmi:citation>
              <gmi:type>
                <gmi:MI_SensorTypeCode />
              </gmi:type>
            </gmi:MI_Instrument>
          </gmi:instrument>
        </gmi:MI_Platform>
      </gmi:platform>
    </gmi:MI_AcquisitionInformation>
  </gmi:acquisitionInformation>
</gmi:MI_Metadata>');
INSERT INTO collection_metadata ("mid","metadata") VALUES (
31,'<?xml version="1.0" encoding="UTF-8"?>
<gmd:MD_Metadata xmlns:gmd="http://www.isotc211.org/2005/gmd" xmlns:atom="http://www.w3.org/2005/Atom" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dif="http://gcmd.gsfc.nasa.gov/Aboutus/xml/dif/" xmlns:eo="http://a9.com/-/opensearch/extensions/eo/1.0/" xmlns:fn="http://www.w3.org/2005/02/xpath-functions" xmlns:gco="http://www.isotc211.org/2005/gco" xmlns:geo="http://a9.com/-/opensearch/extensions/geo/1.0/" xmlns:geonet="http://www.fao.org/geonetwork" xmlns:georss="http://www.georss.org/georss" xmlns:gml="http://www.opengis.net/gml/3.2" xmlns:os="http://a9.com/-/spec/opensearch/1.1/" xmlns:semantic="http://a9.com/-/opensearch/extensions/semantic/1.0/" xmlns:sru="http://a9.com/-/opensearch/extensions/sru/2.0/" xmlns:time="http://a9.com/-/opensearch/extensions/time/1.0/" xmlns:util="java:java.util.UUID">
  <gmd:fileIdentifier>
    <gco:CharacterString>Landsat_8</gco:CharacterString>
  </gmd:fileIdentifier>
  <gmd:language>
    <gco:CharacterString>eng</gco:CharacterString>
  </gmd:language>
  <gmd:characterSet>
    <gmd:MD_CharacterSetCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#MD_CharacterSetCode" codeListValue="utf8">utf8</gmd:MD_CharacterSetCode>
  </gmd:characterSet>
  <gmd:hierarchyLevel>
    <gmd:MD_ScopeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#MD_ScopeCode" codeListValue="series">series</gmd:MD_ScopeCode>
  </gmd:hierarchyLevel>
  <gmd:contact>
    <gmd:CI_ResponsibleParty>
      <gmd:individualName>
        <gco:CharacterString>EROS CENTER,</gco:CharacterString>
      </gmd:individualName>
      <gmd:contactInfo>
        <gmd:CI_Contact>
          <gmd:phone>
            <gmd:CI_Telephone>
              <gmd:voice>
                <gco:CharacterString>605-594-6151, 800-252-4547</gco:CharacterString>
              </gmd:voice>
              <gmd:facsimile>
                <gco:CharacterString>605-594-6589</gco:CharacterString>
              </gmd:facsimile>
            </gmd:CI_Telephone>
          </gmd:phone>
          <gmd:address>
            <gmd:CI_Address>
              <gmd:deliveryPoint>
                <gco:CharacterString>LTA Customer Services</gco:CharacterString>
              </gmd:deliveryPoint>
              <gmd:deliveryPoint>
                <gco:CharacterString>U.S. Geological Survey</gco:CharacterString>
              </gmd:deliveryPoint>
              <gmd:deliveryPoint>
                <gco:CharacterString>EROS Center</gco:CharacterString>
              </gmd:deliveryPoint>
              <gmd:deliveryPoint>
                <gco:CharacterString>47914 252nd Street</gco:CharacterString>
              </gmd:deliveryPoint>
              <gmd:city>
                <gco:CharacterString>Sioux Falls</gco:CharacterString>
              </gmd:city>
              <gmd:administrativeArea>
                <gco:CharacterString>SD</gco:CharacterString>
              </gmd:administrativeArea>
              <gmd:postalCode>
                <gco:CharacterString>57198-0001</gco:CharacterString>
              </gmd:postalCode>
              <gmd:country>
                <gco:CharacterString>USA</gco:CharacterString>
              </gmd:country>
              <gmd:electronicMailAddress>
                <gco:CharacterString>lta@usgs.gov</gco:CharacterString>
              </gmd:electronicMailAddress>
            </gmd:CI_Address>
          </gmd:address>
        </gmd:CI_Contact>
      </gmd:contactInfo>
      <gmd:role>
        <gmd:CI_RoleCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_RoleCode" codeListValue="author">author</gmd:CI_RoleCode>
      </gmd:role>
    </gmd:CI_ResponsibleParty>
  </gmd:contact>
  <gmd:contact>
    <gmd:CI_ResponsibleParty>
      <gmd:organisationName>
        <gco:CharacterString>GCMD</gco:CharacterString>
      </gmd:organisationName>
      <gmd:role>
        <gmd:CI_RoleCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_RoleCode" codeListValue="originator">originator</gmd:CI_RoleCode>
      </gmd:role>
    </gmd:CI_ResponsibleParty>
  </gmd:contact>
  <gmd:dateStamp>
    <gco:Date>2013-03-13</gco:Date>
  </gmd:dateStamp>
  <gmd:metadataStandardName>
    <gco:CharacterString>CEOS IDN DIF</gco:CharacterString>
  </gmd:metadataStandardName>
  <gmd:metadataStandardVersion>
    <gco:CharacterString>VERSION 9.9.3</gco:CharacterString>
  </gmd:metadataStandardVersion>
  <gmd:identificationInfo>
    <gmd:MD_DataIdentification>
      <gmd:citation>
        <gmd:CI_Citation>
          <gmd:title>
            <gco:CharacterString>Landsat 8</gco:CharacterString>
          </gmd:title>
          <gmd:alternateTitle>
            <gco:CharacterString>Landsat 8</gco:CharacterString>
          </gmd:alternateTitle>
          <gmd:date>
            <gmd:CI_Date>
              <gmd:date>
                <gco:Date>2013-03-13</gco:Date>
              </gmd:date>
              <gmd:dateType>
                <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="publication">publication</gmd:CI_DateTypeCode>
              </gmd:dateType>
            </gmd:CI_Date>
          </gmd:date>
          <gmd:citedResponsibleParty>
            <gmd:CI_ResponsibleParty>
              <gmd:individualName>
                <gco:CharacterString>U.S. Geological Survey</gco:CharacterString>
              </gmd:individualName>
              <gmd:role>
                <gmd:CI_RoleCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_RoleCode" codeListValue="originator">originator</gmd:CI_RoleCode>
              </gmd:role>
            </gmd:CI_ResponsibleParty>
          </gmd:citedResponsibleParty>
          <gmd:citedResponsibleParty>                              </gmd:citedResponsibleParty>
          <gmd:citedResponsibleParty>                              </gmd:citedResponsibleParty>
        </gmd:CI_Citation>
      </gmd:citation>
      <gmd:abstract>
        <gco:CharacterString>This data set is a raster file containing global information for bands 1 through 11 for Landsat 8 Operational Land Imager (OLI) and Thermal Infrared Sensor (TIRS).    [ This document was provided by NASA Global Change Master Directory.       For more information on the source of this metadata please visit      http://gcmd.nasa.gov/r/geoss/[GCMD]Landsat_8 ]</gco:CharacterString>
      </gmd:abstract>
      <gmd:purpose>
        <gco:CharacterString>The mission of the Landsat 8 satellite is to provide a vehicle for continuing the flow of global change information to users worldwide. The Landsat 8 satellite fulfills its mission by providing repetitive, synoptic coverage continental surfaces and by collecting data in spectral bands that include the visible, near-infrared, shortwave, and thermal infrared portions of the electromagnetic spectrum. Landsat 8 mission objectives include:      1)  Maintaining Landsat data continuity by providing data that are consistent in terms of data acquisition, geometry, spatial resolution, calibration, coverage characteristics, and spectral characteristics with previous Landsat data.      2)  Generating and periodically refreshing a global archive of substantially cloud-free, Sun-lit, land-mass imagery.      3)  Continuing to make remote sensing satellite data available to domestic and international users and expanding the use of such data for global change research in both the Government and private commercial sectors.      4)  Promoting interdisciplinary research via synergism with other EOS observations, specifically, orbiting in tandem with the EOS Terra satellite for near coincident observations.      Supplemental_Information:      The Landsat 8 satellite is another step in the development and application of remotely sensed satellite data for use in managing the Earth land resources. Improving upon earlier Landsat systems, the OLI and TIRS sensors aboard Landsat 8, provide for new capabilities in the remote sensing of Earth land surface. Landsat 8 data are collected from a nominal altitude of 705 kilometers in a near-polar, near-circular, Sun-synchronous orbit at an inclination of 98.2 degrees, imaging the same 183-km swath of Earth surface every 16 days. Additional information can be found at the following sites: http://landsat.gsfc.nasa.gov/ and http://landsat.usgs.gov/ Landsat 8 Information    http://landsat.usgs.gov/landsat8.php      Data Set Credit:      The Landsat Program, as defined by Cong</gco:CharacterString>
      </gmd:purpose>
      <gmd:status>
        <gmd:MD_ProgressCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#MD_ProgressCode" codeListValue="onGoing">onGoing</gmd:MD_ProgressCode>
      </gmd:status>
      <gmd:pointOfContact>
        <gmd:CI_ResponsibleParty>
          <gmd:individualName>
            <gco:CharacterString>EROS CENTER,</gco:CharacterString>
          </gmd:individualName>
          <gmd:organisationName>
            <gco:CharacterString>DOI/USGS/EROS &gt; Earth Resources Observation and Science Center, U.S. Geological Survey, U.S. Department of the Interior</gco:CharacterString>
          </gmd:organisationName>
          <gmd:positionName>
            <gco:CharacterString>DATA CENTER CONTACT</gco:CharacterString>
          </gmd:positionName>
          <gmd:contactInfo>
            <gmd:CI_Contact>
              <gmd:phone>
                <gmd:CI_Telephone>
                  <gmd:voice>
                    <gco:CharacterString>605-594-6151, 800-252-4547</gco:CharacterString>
                  </gmd:voice>
                  <gmd:facsimile>
                    <gco:CharacterString>605-594-6589</gco:CharacterString>
                  </gmd:facsimile>
                </gmd:CI_Telephone>
              </gmd:phone>
              <gmd:address>
                <gmd:CI_Address>
                  <gmd:deliveryPoint>
                    <gco:CharacterString>LTA Customer Services</gco:CharacterString>
                  </gmd:deliveryPoint>
                  <gmd:deliveryPoint>
                    <gco:CharacterString>U.S. Geological Survey</gco:CharacterString>
                  </gmd:deliveryPoint>
                  <gmd:deliveryPoint>
                    <gco:CharacterString>EROS Center</gco:CharacterString>
                  </gmd:deliveryPoint>
                  <gmd:deliveryPoint>
                    <gco:CharacterString>47914 252nd Street</gco:CharacterString>
                  </gmd:deliveryPoint>
                  <gmd:city>
                    <gco:CharacterString>Sioux Falls</gco:CharacterString>
                  </gmd:city>
                  <gmd:administrativeArea>
                    <gco:CharacterString>SD</gco:CharacterString>
                  </gmd:administrativeArea>
                  <gmd:postalCode>
                    <gco:CharacterString>57198-0001</gco:CharacterString>
                  </gmd:postalCode>
                  <gmd:country>
                    <gco:CharacterString>USA</gco:CharacterString>
                  </gmd:country>
                  <gmd:electronicMailAddress>
                    <gco:CharacterString>lta@usgs.gov</gco:CharacterString>
                  </gmd:electronicMailAddress>
                </gmd:CI_Address>
              </gmd:address>
              <gmd:onlineResource>
                <gmd:CI_OnlineResource>
                  <gmd:linkage>
                    <gmd:URL>http://eros.usgs.gov/</gmd:URL>
                  </gmd:linkage>
                </gmd:CI_OnlineResource>
              </gmd:onlineResource>
            </gmd:CI_Contact>
          </gmd:contactInfo>
          <gmd:role>
            <gmd:CI_RoleCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_RoleCode" codeListValue="resourceProvider">resourceProvider</gmd:CI_RoleCode>
          </gmd:role>
        </gmd:CI_ResponsibleParty>
      </gmd:pointOfContact>
      <gmd:descriptiveKeywords>
        <gmd:MD_Keywords>
          <gmd:keyword>
            <gco:CharacterString>EARTH SCIENCE &gt; LAND SURFACE &gt; LAND USE/LAND COVER &gt; LAND USE CLASSES</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>EARTH SCIENCE &gt; LAND SURFACE &gt; LANDSCAPE &gt; LANDSCAPE PATTERNS</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>EARTH SCIENCE &gt; LAND SURFACE &gt; SURFACE RADIATIVE PROPERTIES &gt; REFLECTANCE</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>EARTH SCIENCE &gt; LAND SURFACE &gt; GEOMORPHIC LANDFORMS/PROCESSES</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>EARTH SCIENCE &gt; SPECTRAL/ENGINEERING &gt; INFRARED WAVELENGTHS &gt; BRIGHTNESS TEMPERATURE</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>EARTH SCIENCE &gt; SPECTRAL/ENGINEERING &gt; INFRARED WAVELENGTHS &gt; INFRARED IMAGERY</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>EARTH SCIENCE &gt; SPECTRAL/ENGINEERING &gt; SENSOR CHARACTERISTICS &gt; ULTRAVIOLET SENSOR TEMPERATURE</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>EARTH SCIENCE &gt; SPECTRAL/ENGINEERING &gt; VISIBLE WAVELENGTHS &gt; VISIBLE IMAGERY</gco:CharacterString>
          </gmd:keyword>
          <gmd:thesaurusName>
            <gmd:CI_Citation>
              <gmd:title>
                <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
              </gmd:title>
              <gmd:alternateTitle>
                <gco:CharacterString>Science Keywords</gco:CharacterString>
              </gmd:alternateTitle>
              <gmd:date>
                <gmd:CI_Date>
                  <gmd:date>
                    <gco:Date>2008-02-05</gco:Date>
                  </gmd:date>
                  <gmd:dateType>
                    <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision">revision</gmd:CI_DateTypeCode>
                  </gmd:dateType>
                </gmd:CI_Date>
              </gmd:date>
              <gmd:collectiveTitle>
                <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
              </gmd:collectiveTitle>
            </gmd:CI_Citation>
          </gmd:thesaurusName>
        </gmd:MD_Keywords>
      </gmd:descriptiveKeywords>
      <gmd:descriptiveKeywords>
        <gmd:MD_Keywords>
          <gmd:keyword>
            <gco:CharacterString>EROS</gco:CharacterString>
          </gmd:keyword>
        </gmd:MD_Keywords>
      </gmd:descriptiveKeywords>
      <gmd:descriptiveKeywords>
        <gmd:MD_Keywords>
          <gmd:keyword>
            <gco:CharacterString>OLI &gt; Operational Land Imager</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>TIRS &gt; Thermal Infrared Sensor</gco:CharacterString>
          </gmd:keyword>
          <gmd:thesaurusName>
            <gmd:CI_Citation>
              <gmd:title>
                <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
              </gmd:title>
              <gmd:alternateTitle>
                <gco:CharacterString>Instruments</gco:CharacterString>
              </gmd:alternateTitle>
              <gmd:date>
                <gmd:CI_Date>
                  <gmd:date>
                    <gco:Date>2009-06-10</gco:Date>
                  </gmd:date>
                  <gmd:dateType>
                    <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision">revision</gmd:CI_DateTypeCode>
                  </gmd:dateType>
                </gmd:CI_Date>
              </gmd:date>
              <gmd:collectiveTitle>
                <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
              </gmd:collectiveTitle>
            </gmd:CI_Citation>
          </gmd:thesaurusName>
        </gmd:MD_Keywords>
      </gmd:descriptiveKeywords>
      <gmd:descriptiveKeywords>
        <gmd:MD_Keywords>
          <gmd:keyword>
            <gco:CharacterString>LANDSAT-8</gco:CharacterString>
          </gmd:keyword>
          <gmd:thesaurusName>
            <gmd:CI_Citation>
              <gmd:title>
                <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
              </gmd:title>
              <gmd:alternateTitle>
                <gco:CharacterString>Platforms</gco:CharacterString>
              </gmd:alternateTitle>
              <gmd:date>
                <gmd:CI_Date>
                  <gmd:date>
                    <gco:Date>2009-06-10</gco:Date>
                  </gmd:date>
                  <gmd:dateType>
                    <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision">revision</gmd:CI_DateTypeCode>
                  </gmd:dateType>
                </gmd:CI_Date>
              </gmd:date>
              <gmd:collectiveTitle>
                <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
              </gmd:collectiveTitle>
            </gmd:CI_Citation>
          </gmd:thesaurusName>
        </gmd:MD_Keywords>
      </gmd:descriptiveKeywords>
      <gmd:descriptiveKeywords>
        <gmd:MD_Keywords>
          <gmd:keyword>
            <gco:CharacterString>CWIC &gt; CEOS WGISS Integrated Catalog</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>ESIP &gt; Earth Science Information Partners Program</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>LDCM &gt; Landsat Data Continuity Mission</gco:CharacterString>
          </gmd:keyword>
          <gmd:thesaurusName>
            <gmd:CI_Citation>
              <gmd:title>
                <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
              </gmd:title>
              <gmd:alternateTitle>
                <gco:CharacterString>Projects</gco:CharacterString>
              </gmd:alternateTitle>
              <gmd:date>
                <gmd:CI_Date>
                  <gmd:date>
                    <gco:Date>2009-06-10</gco:Date>
                  </gmd:date>
                  <gmd:dateType>
                    <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision">revision</gmd:CI_DateTypeCode>
                  </gmd:dateType>
                </gmd:CI_Date>
              </gmd:date>
              <gmd:collectiveTitle>
                <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
              </gmd:collectiveTitle>
            </gmd:CI_Citation>
          </gmd:thesaurusName>
        </gmd:MD_Keywords>
      </gmd:descriptiveKeywords>
      <gmd:descriptiveKeywords>
        <gmd:MD_Keywords>
          <gmd:keyword>
            <gco:CharacterString>SOOS</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>USA/USGS</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>AMD</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>ECHO</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>ARCTIC</gco:CharacterString>
          </gmd:keyword>
          <gmd:thesaurusName>
            <gmd:CI_Citation>
              <gmd:title>
                <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
              </gmd:title>
              <gmd:alternateTitle>
                <gco:CharacterString>IDN Nodes</gco:CharacterString>
              </gmd:alternateTitle>
              <gmd:date>
                <gmd:CI_Date>
                  <gmd:date>
                    <gco:Date>2007-04-01</gco:Date>
                  </gmd:date>
                  <gmd:dateType>
                    <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision">revision</gmd:CI_DateTypeCode>
                  </gmd:dateType>
                </gmd:CI_Date>
              </gmd:date>
              <gmd:collectiveTitle>
                <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
              </gmd:collectiveTitle>
            </gmd:CI_Citation>
          </gmd:thesaurusName>
        </gmd:MD_Keywords>
      </gmd:descriptiveKeywords>
      <gmd:descriptiveKeywords>
        <gmd:MD_Keywords>
          <gmd:keyword>
            <gco:CharacterString>CONTINENT &gt; AUSTRALIA/NEW ZEALAND &gt; AUSTRALIA</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>CONTINENT &gt; NORTH AMERICA &gt; GREENLAND</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>CONTINENT &gt; AFRICA</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>GEOGRAPHIC REGION &gt; ARCTIC</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>CONTINENT &gt; ASIA</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>CONTINENT &gt; EUROPE</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>CONTINENT &gt; NORTH AMERICA</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>GEOGRAPHIC REGION &gt; GLOBAL</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>GEOGRAPHIC REGION &gt; POLAR</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>CONTINENT &gt; SOUTH AMERICA</gco:CharacterString>
          </gmd:keyword>
          <gmd:thesaurusName>
            <gmd:CI_Citation>
              <gmd:title>
                <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
              </gmd:title>
              <gmd:alternateTitle>
                <gco:CharacterString>Locations</gco:CharacterString>
              </gmd:alternateTitle>
              <gmd:date>
                <gmd:CI_Date>
                  <gmd:date>
                    <gco:Date>2009-12-22</gco:Date>
                  </gmd:date>
                  <gmd:dateType>
                    <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision">revision</gmd:CI_DateTypeCode>
                  </gmd:dateType>
                </gmd:CI_Date>
              </gmd:date>
              <gmd:collectiveTitle>
                <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
              </gmd:collectiveTitle>
            </gmd:CI_Citation>
          </gmd:thesaurusName>
        </gmd:MD_Keywords>
      </gmd:descriptiveKeywords>
      <gmd:descriptiveKeywords>
        <gmd:MD_Keywords>
          <gmd:keyword>
            <gco:CharacterString>geossDataCore</gco:CharacterString>
          </gmd:keyword>
        </gmd:MD_Keywords>
      </gmd:descriptiveKeywords>
      <gmd:descriptiveKeywords>
        <gmd:MD_Keywords>
          <gmd:keyword>
            <gco:CharacterString>geossNoMonetaryCharge</gco:CharacterString>
          </gmd:keyword>
        </gmd:MD_Keywords>
      </gmd:descriptiveKeywords>
      <gmd:resourceConstraints>
        <gmd:MD_LegalConstraints>
          <gmd:accessConstraints>
            <gmd:MD_RestrictionCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#MD_RestrictionCode" codeListValue="otherRestrictions">otherRestrictions</gmd:MD_RestrictionCode>
          </gmd:accessConstraints>
          <gmd:otherConstraints>
            <gco:CharacterString>There are no restrictions to this data set.</gco:CharacterString>
          </gmd:otherConstraints>
        </gmd:MD_LegalConstraints>
      </gmd:resourceConstraints>
      <gmd:resourceConstraints>
        <gmd:MD_LegalConstraints>
          <gmd:useConstraints>
            <gmd:MD_RestrictionCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#MD_RestrictionCode" codeListValue="otherRestrictions">otherRestrictions</gmd:MD_RestrictionCode>
          </gmd:useConstraints>
          <gmd:otherConstraints>
            <gco:CharacterString>There is no guarantee of warranty concerning the accuracy of these data.  Users should be aware that temporal changes may have occurred since the data was collected and that some parts of these data may no longer represent actual surface conditions.  Users should not use these data for critical applications without a full awareness of their limitations.  Acknowledgement of the originating agencies would be appreciated in products derived from these data.  Any user who modifies the data set is obligated to describe the types of modifications they perform.  User specifically agrees not to misrepresent the data set, nor to imply that changes made were approved or endorsed by the U.S. Geological Survey.  Please refer to http://www.usgs.gov/privacy.html for the USGS disclaimer.</gco:CharacterString>
          </gmd:otherConstraints>
        </gmd:MD_LegalConstraints>
      </gmd:resourceConstraints>
      <gmd:language>
        <gco:CharacterString>eng</gco:CharacterString>
      </gmd:language>
      <gmd:characterSet>
        <gmd:MD_CharacterSetCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#MD_CharacterSetCode" codeListValue="utf8">utf8</gmd:MD_CharacterSetCode>
      </gmd:characterSet>
      <gmd:topicCategory>
        <gmd:MD_TopicCategoryCode>climatologyMeteorologyAtmosphere</gmd:MD_TopicCategoryCode>
      </gmd:topicCategory>
      <gmd:topicCategory>
        <gmd:MD_TopicCategoryCode>elevation</gmd:MD_TopicCategoryCode>
      </gmd:topicCategory>
      <gmd:topicCategory>
        <gmd:MD_TopicCategoryCode>geoscientificInformation</gmd:MD_TopicCategoryCode>
      </gmd:topicCategory>
      <gmd:topicCategory>
        <gmd:MD_TopicCategoryCode>imageryBaseMapsEarthCover</gmd:MD_TopicCategoryCode>
      </gmd:topicCategory>
      <gmd:topicCategory>
        <gmd:MD_TopicCategoryCode>planningCadastre</gmd:MD_TopicCategoryCode>
      </gmd:topicCategory>
      <gmd:extent>
        <gmd:EX_Extent>
          <gmd:geographicElement>
            <gmd:EX_GeographicBoundingBox>
              <gmd:westBoundLongitude>
                <gco:Decimal>-180.0</gco:Decimal>
              </gmd:westBoundLongitude>
              <gmd:eastBoundLongitude>
                <gco:Decimal>180.0</gco:Decimal>
              </gmd:eastBoundLongitude>
              <gmd:southBoundLatitude>
                <gco:Decimal>-82.71</gco:Decimal>
              </gmd:southBoundLatitude>
              <gmd:northBoundLatitude>
                <gco:Decimal>82.74</gco:Decimal>
              </gmd:northBoundLatitude>
            </gmd:EX_GeographicBoundingBox>
          </gmd:geographicElement>
          <gmd:geographicElement>
            <gmd:EX_GeographicDescription>
              <gmd:geographicIdentifier>
                <gmd:MD_Identifier>
                  <gmd:authority>
                    <gmd:CI_Citation>
                      <gmd:title>
                        <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
                      </gmd:title>
                      <gmd:alternateTitle>
                        <gco:CharacterString>Locations</gco:CharacterString>
                      </gmd:alternateTitle>
                      <gmd:date>
                        <gmd:CI_Date>
                          <gmd:date>
                            <gco:Date>2009-12-22</gco:Date>
                          </gmd:date>
                          <gmd:dateType>
                            <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision"/>
                          </gmd:dateType>
                        </gmd:CI_Date>
                      </gmd:date>
                      <gmd:collectiveTitle>
                        <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
                      </gmd:collectiveTitle>
                    </gmd:CI_Citation>
                  </gmd:authority>
                  <gmd:code>
                    <gco:CharacterString>CONTINENT &gt; AUSTRALIA/NEW ZEALAND &gt; AUSTRALIA</gco:CharacterString>
                  </gmd:code>
                </gmd:MD_Identifier>
              </gmd:geographicIdentifier>
            </gmd:EX_GeographicDescription>
          </gmd:geographicElement>
          <gmd:geographicElement>
            <gmd:EX_GeographicDescription>
              <gmd:geographicIdentifier>
                <gmd:MD_Identifier>
                  <gmd:authority>
                    <gmd:CI_Citation>
                      <gmd:title>
                        <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
                      </gmd:title>
                      <gmd:alternateTitle>
                        <gco:CharacterString>Locations</gco:CharacterString>
                      </gmd:alternateTitle>
                      <gmd:date>
                        <gmd:CI_Date>
                          <gmd:date>
                            <gco:Date>2009-12-22</gco:Date>
                          </gmd:date>
                          <gmd:dateType>
                            <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision"/>
                          </gmd:dateType>
                        </gmd:CI_Date>
                      </gmd:date>
                      <gmd:collectiveTitle>
                        <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
                      </gmd:collectiveTitle>
                    </gmd:CI_Citation>
                  </gmd:authority>
                  <gmd:code>
                    <gco:CharacterString>CONTINENT &gt; NORTH AMERICA &gt; GREENLAND</gco:CharacterString>
                  </gmd:code>
                </gmd:MD_Identifier>
              </gmd:geographicIdentifier>
            </gmd:EX_GeographicDescription>
          </gmd:geographicElement>
          <gmd:geographicElement>
            <gmd:EX_GeographicDescription>
              <gmd:geographicIdentifier>
                <gmd:MD_Identifier>
                  <gmd:authority>
                    <gmd:CI_Citation>
                      <gmd:title>
                        <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
                      </gmd:title>
                      <gmd:alternateTitle>
                        <gco:CharacterString>Locations</gco:CharacterString>
                      </gmd:alternateTitle>
                      <gmd:date>
                        <gmd:CI_Date>
                          <gmd:date>
                            <gco:Date>2009-12-22</gco:Date>
                          </gmd:date>
                          <gmd:dateType>
                            <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision"/>
                          </gmd:dateType>
                        </gmd:CI_Date>
                      </gmd:date>
                      <gmd:collectiveTitle>
                        <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
                      </gmd:collectiveTitle>
                    </gmd:CI_Citation>
                  </gmd:authority>
                  <gmd:code>
                    <gco:CharacterString>CONTINENT &gt; AFRICA</gco:CharacterString>
                  </gmd:code>
                </gmd:MD_Identifier>
              </gmd:geographicIdentifier>
            </gmd:EX_GeographicDescription>
          </gmd:geographicElement>
          <gmd:geographicElement>
            <gmd:EX_GeographicDescription>
              <gmd:geographicIdentifier>
                <gmd:MD_Identifier>
                  <gmd:authority>
                    <gmd:CI_Citation>
                      <gmd:title>
                        <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
                      </gmd:title>
                      <gmd:alternateTitle>
                        <gco:CharacterString>Locations</gco:CharacterString>
                      </gmd:alternateTitle>
                      <gmd:date>
                        <gmd:CI_Date>
                          <gmd:date>
                            <gco:Date>2009-12-22</gco:Date>
                          </gmd:date>
                          <gmd:dateType>
                            <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision"/>
                          </gmd:dateType>
                        </gmd:CI_Date>
                      </gmd:date>
                      <gmd:collectiveTitle>
                        <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
                      </gmd:collectiveTitle>
                    </gmd:CI_Citation>
                  </gmd:authority>
                  <gmd:code>
                    <gco:CharacterString>GEOGRAPHIC REGION &gt; ARCTIC</gco:CharacterString>
                  </gmd:code>
                </gmd:MD_Identifier>
              </gmd:geographicIdentifier>
            </gmd:EX_GeographicDescription>
          </gmd:geographicElement>
          <gmd:geographicElement>
            <gmd:EX_GeographicDescription>
              <gmd:geographicIdentifier>
                <gmd:MD_Identifier>
                  <gmd:authority>
                    <gmd:CI_Citation>
                      <gmd:title>
                        <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
                      </gmd:title>
                      <gmd:alternateTitle>
                        <gco:CharacterString>Locations</gco:CharacterString>
                      </gmd:alternateTitle>
                      <gmd:date>
                        <gmd:CI_Date>
                          <gmd:date>
                            <gco:Date>2009-12-22</gco:Date>
                          </gmd:date>
                          <gmd:dateType>
                            <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision"/>
                          </gmd:dateType>
                        </gmd:CI_Date>
                      </gmd:date>
                      <gmd:collectiveTitle>
                        <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
                      </gmd:collectiveTitle>
                    </gmd:CI_Citation>
                  </gmd:authority>
                  <gmd:code>
                    <gco:CharacterString>CONTINENT &gt; ASIA</gco:CharacterString>
                  </gmd:code>
                </gmd:MD_Identifier>
              </gmd:geographicIdentifier>
            </gmd:EX_GeographicDescription>
          </gmd:geographicElement>
          <gmd:geographicElement>
            <gmd:EX_GeographicDescription>
              <gmd:geographicIdentifier>
                <gmd:MD_Identifier>
                  <gmd:authority>
                    <gmd:CI_Citation>
                      <gmd:title>
                        <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
                      </gmd:title>
                      <gmd:alternateTitle>
                        <gco:CharacterString>Locations</gco:CharacterString>
                      </gmd:alternateTitle>
                      <gmd:date>
                        <gmd:CI_Date>
                          <gmd:date>
                            <gco:Date>2009-12-22</gco:Date>
                          </gmd:date>
                          <gmd:dateType>
                            <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision"/>
                          </gmd:dateType>
                        </gmd:CI_Date>
                      </gmd:date>
                      <gmd:collectiveTitle>
                        <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
                      </gmd:collectiveTitle>
                    </gmd:CI_Citation>
                  </gmd:authority>
                  <gmd:code>
                    <gco:CharacterString>CONTINENT &gt; EUROPE</gco:CharacterString>
                  </gmd:code>
                </gmd:MD_Identifier>
              </gmd:geographicIdentifier>
            </gmd:EX_GeographicDescription>
          </gmd:geographicElement>
          <gmd:geographicElement>
            <gmd:EX_GeographicDescription>
              <gmd:geographicIdentifier>
                <gmd:MD_Identifier>
                  <gmd:authority>
                    <gmd:CI_Citation>
                      <gmd:title>
                        <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
                      </gmd:title>
                      <gmd:alternateTitle>
                        <gco:CharacterString>Locations</gco:CharacterString>
                      </gmd:alternateTitle>
                      <gmd:date>
                        <gmd:CI_Date>
                          <gmd:date>
                            <gco:Date>2009-12-22</gco:Date>
                          </gmd:date>
                          <gmd:dateType>
                            <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision"/>
                          </gmd:dateType>
                        </gmd:CI_Date>
                      </gmd:date>
                      <gmd:collectiveTitle>
                        <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
                      </gmd:collectiveTitle>
                    </gmd:CI_Citation>
                  </gmd:authority>
                  <gmd:code>
                    <gco:CharacterString>CONTINENT &gt; NORTH AMERICA</gco:CharacterString>
                  </gmd:code>
                </gmd:MD_Identifier>
              </gmd:geographicIdentifier>
            </gmd:EX_GeographicDescription>
          </gmd:geographicElement>
          <gmd:geographicElement>
            <gmd:EX_GeographicDescription>
              <gmd:geographicIdentifier>
                <gmd:MD_Identifier>
                  <gmd:authority>
                    <gmd:CI_Citation>
                      <gmd:title>
                        <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
                      </gmd:title>
                      <gmd:alternateTitle>
                        <gco:CharacterString>Locations</gco:CharacterString>
                      </gmd:alternateTitle>
                      <gmd:date>
                        <gmd:CI_Date>
                          <gmd:date>
                            <gco:Date>2009-12-22</gco:Date>
                          </gmd:date>
                          <gmd:dateType>
                            <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision"/>
                          </gmd:dateType>
                        </gmd:CI_Date>
                      </gmd:date>
                      <gmd:collectiveTitle>
                        <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
                      </gmd:collectiveTitle>
                    </gmd:CI_Citation>
                  </gmd:authority>
                  <gmd:code>
                    <gco:CharacterString>GEOGRAPHIC REGION &gt; GLOBAL</gco:CharacterString>
                  </gmd:code>
                </gmd:MD_Identifier>
              </gmd:geographicIdentifier>
            </gmd:EX_GeographicDescription>
          </gmd:geographicElement>
          <gmd:geographicElement>
            <gmd:EX_GeographicDescription>
              <gmd:geographicIdentifier>
                <gmd:MD_Identifier>
                  <gmd:authority>
                    <gmd:CI_Citation>
                      <gmd:title>
                        <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
                      </gmd:title>
                      <gmd:alternateTitle>
                        <gco:CharacterString>Locations</gco:CharacterString>
                      </gmd:alternateTitle>
                      <gmd:date>
                        <gmd:CI_Date>
                          <gmd:date>
                            <gco:Date>2009-12-22</gco:Date>
                          </gmd:date>
                          <gmd:dateType>
                            <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision"/>
                          </gmd:dateType>
                        </gmd:CI_Date>
                      </gmd:date>
                      <gmd:collectiveTitle>
                        <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
                      </gmd:collectiveTitle>
                    </gmd:CI_Citation>
                  </gmd:authority>
                  <gmd:code>
                    <gco:CharacterString>GEOGRAPHIC REGION &gt; POLAR</gco:CharacterString>
                  </gmd:code>
                </gmd:MD_Identifier>
              </gmd:geographicIdentifier>
            </gmd:EX_GeographicDescription>
          </gmd:geographicElement>
          <gmd:geographicElement>
            <gmd:EX_GeographicDescription>
              <gmd:geographicIdentifier>
                <gmd:MD_Identifier>
                  <gmd:authority>
                    <gmd:CI_Citation>
                      <gmd:title>
                        <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
                      </gmd:title>
                      <gmd:alternateTitle>
                        <gco:CharacterString>Locations</gco:CharacterString>
                      </gmd:alternateTitle>
                      <gmd:date>
                        <gmd:CI_Date>
                          <gmd:date>
                            <gco:Date>2009-12-22</gco:Date>
                          </gmd:date>
                          <gmd:dateType>
                            <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision"/>
                          </gmd:dateType>
                        </gmd:CI_Date>
                      </gmd:date>
                      <gmd:collectiveTitle>
                        <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
                      </gmd:collectiveTitle>
                    </gmd:CI_Citation>
                  </gmd:authority>
                  <gmd:code>
                    <gco:CharacterString>CONTINENT &gt; SOUTH AMERICA</gco:CharacterString>
                  </gmd:code>
                </gmd:MD_Identifier>
              </gmd:geographicIdentifier>
            </gmd:EX_GeographicDescription>
          </gmd:geographicElement>
          <gmd:temporalElement>
            <gmd:EX_TemporalExtent>
              <gmd:extent>
                <gml:TimePeriod gml:id="d7387e246">
                  <gml:begin>
                    <gml:TimeInstant gml:id="d7387e248">
                      <gml:timePosition>2013-02-11</gml:timePosition>
                    </gml:TimeInstant>
                  </gml:begin>
                  <gml:end>
                    <gml:TimeInstant gml:id="aadfe4406-50fc-4abc-bb8f-26f8eda08a1d">
                      <gml:timePosition indeterminatePosition="unknown"/>
                    </gml:TimeInstant>
                  </gml:end>
                </gml:TimePeriod>
              </gmd:extent>
            </gmd:EX_TemporalExtent>
          </gmd:temporalElement>
        </gmd:EX_Extent>
      </gmd:extent>
    </gmd:MD_DataIdentification>
  </gmd:identificationInfo>
  <gmd:distributionInfo>
    <gmd:MD_Distribution>
      <gmd:distributionFormat>
        <gmd:MD_Format>
          <gmd:name>
            <gco:CharacterString>GeoTIFF</gco:CharacterString>
          </gmd:name>
          <gmd:version gco:nilReason="missing">
            <gco:CharacterString/>
          </gmd:version>
        </gmd:MD_Format>
      </gmd:distributionFormat>
      <gmd:distributor>
        <gmd:MD_Distributor>
          <gmd:distributorContact>
            <gmd:CI_ResponsibleParty>
              <gmd:individualName>
                <gco:CharacterString>EROS CENTER,</gco:CharacterString>
              </gmd:individualName>
              <gmd:organisationName>
                <gco:CharacterString>DOI/USGS/EROS &gt; Earth Resources Observation and Science Center, U.S. Geological Survey, U.S. Department of the Interior</gco:CharacterString>
              </gmd:organisationName>
              <gmd:positionName>
                <gco:CharacterString>DATA CENTER CONTACT</gco:CharacterString>
              </gmd:positionName>
              <gmd:contactInfo>
                <gmd:CI_Contact>
                  <gmd:phone>
                    <gmd:CI_Telephone>
                      <gmd:voice>
                        <gco:CharacterString>605-594-6151, 800-252-4547</gco:CharacterString>
                      </gmd:voice>
                      <gmd:facsimile>
                        <gco:CharacterString>605-594-6589</gco:CharacterString>
                      </gmd:facsimile>
                    </gmd:CI_Telephone>
                  </gmd:phone>
                  <gmd:address>
                    <gmd:CI_Address>
                      <gmd:deliveryPoint>
                        <gco:CharacterString>LTA Customer Services</gco:CharacterString>
                      </gmd:deliveryPoint>
                      <gmd:deliveryPoint>
                        <gco:CharacterString>U.S. Geological Survey</gco:CharacterString>
                      </gmd:deliveryPoint>
                      <gmd:deliveryPoint>
                        <gco:CharacterString>EROS Center</gco:CharacterString>
                      </gmd:deliveryPoint>
                      <gmd:deliveryPoint>
                        <gco:CharacterString>47914 252nd Street</gco:CharacterString>
                      </gmd:deliveryPoint>
                      <gmd:city>
                        <gco:CharacterString>Sioux Falls</gco:CharacterString>
                      </gmd:city>
                      <gmd:administrativeArea>
                        <gco:CharacterString>SD</gco:CharacterString>
                      </gmd:administrativeArea>
                      <gmd:postalCode>
                        <gco:CharacterString>57198-0001</gco:CharacterString>
                      </gmd:postalCode>
                      <gmd:country>
                        <gco:CharacterString>USA</gco:CharacterString>
                      </gmd:country>
                      <gmd:electronicMailAddress>
                        <gco:CharacterString>lta@usgs.gov</gco:CharacterString>
                      </gmd:electronicMailAddress>
                    </gmd:CI_Address>
                  </gmd:address>
                  <gmd:onlineResource>
                    <gmd:CI_OnlineResource>
                      <gmd:linkage>
                        <gmd:URL>http://eros.usgs.gov/</gmd:URL>
                      </gmd:linkage>
                    </gmd:CI_OnlineResource>
                  </gmd:onlineResource>
                </gmd:CI_Contact>
              </gmd:contactInfo>
              <gmd:role>
                <gmd:CI_RoleCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_RoleCode" codeListValue="distributor">distributor</gmd:CI_RoleCode>
              </gmd:role>
            </gmd:CI_ResponsibleParty>
          </gmd:distributorContact>
          <gmd:distributionOrderProcess>
            <gmd:MD_StandardOrderProcess>
              <gmd:fees>
                <gco:CharacterString>$0 U.S. Dollars for Level 1 Terrain correction</gco:CharacterString>
              </gmd:fees>
            </gmd:MD_StandardOrderProcess>
          </gmd:distributionOrderProcess>
        </gmd:MD_Distributor>
      </gmd:distributor>
      <gmd:transferOptions>
        <gmd:MD_DigitalTransferOptions>
          <gmd:onLine>
            <gmd:CI_OnlineResource>
              <gmd:linkage>
                <gmd:URL>http://glovis.usgs.gov http://earthexplorer.usgs.gov</gmd:URL>
              </gmd:linkage>
              <gmd:protocol>
                <gco:CharacterString>WWW:LINK-1.0-http--link</gco:CharacterString>
              </gmd:protocol>
              <gmd:name>
                <gco:CharacterString>GET DATA</gco:CharacterString>
              </gmd:name>
              <gmd:description>
                <gco:CharacterString>Dataset searching and requesting capabilities are available through the Global Visualization viewer and EarthExplorer at the above URLs.</gco:CharacterString>
              </gmd:description>
            </gmd:CI_OnlineResource>
          </gmd:onLine>
          <gmd:onLine>
            <gmd:CI_OnlineResource>
              <gmd:linkage>
                <gmd:URL>http://landsat.usgs.gov/landsat8.php</gmd:URL>
              </gmd:linkage>
              <gmd:protocol>
                <gco:CharacterString>WWW:LINK-1.0-http--link</gco:CharacterString>
              </gmd:protocol>
              <gmd:name>
                <gco:CharacterString>VIEW RELATED INFORMATION</gco:CharacterString>
              </gmd:name>
              <gmd:description>
                <gco:CharacterString>Landsat 8 OLI and TIRS information.</gco:CharacterString>
              </gmd:description>
            </gmd:CI_OnlineResource>
          </gmd:onLine>
          <gmd:onLine>
            <gmd:CI_OnlineResource>
              <gmd:linkage>
                <gmd:URL>http://eros.usgs.gov</gmd:URL>
              </gmd:linkage>
              <gmd:protocol>
                <gco:CharacterString>WWW:LINK-1.0-http--link</gco:CharacterString>
              </gmd:protocol>
              <gmd:name>
                <gco:CharacterString>Data Set Citation</gco:CharacterString>
              </gmd:name>
              <gmd:description>
                <gco:CharacterString>Landsat 8</gco:CharacterString>
              </gmd:description>
            </gmd:CI_OnlineResource>
          </gmd:onLine>
        </gmd:MD_DigitalTransferOptions>
      </gmd:transferOptions>
    </gmd:MD_Distribution>
  </gmd:distributionInfo>
  <gmd:dataQualityInfo>
    <gmd:DQ_DataQuality>
      <gmd:scope>
        <gmd:DQ_Scope>
          <gmd:level>
            <gmd:MD_ScopeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#MD_ScopeCode" codeListValue="dataset">dataset</gmd:MD_ScopeCode>
          </gmd:level>
        </gmd:DQ_Scope>
      </gmd:scope>
      <gmd:report>
        <gmd:DQ_AbsoluteExternalPositionalAccuracy>
          <gmd:nameOfMeasure>
            <gco:CharacterString>Latitude Resolution</gco:CharacterString>
          </gmd:nameOfMeasure>
          <gmd:result>
            <gmd:DQ_QuantitativeResult>
              <gmd:valueUnit>
                <gml:DerivedUnit gml:id="d7387e371">
                  <gml:identifier codeSpace=""/>
                  <gml:derivationUnitTerm uom="meters (bands 10 and 11)"/>
                </gml:DerivedUnit>
              </gmd:valueUnit>
              <gmd:value>
                <gco:Record>100</gco:Record>
              </gmd:value>
            </gmd:DQ_QuantitativeResult>
          </gmd:result>
        </gmd:DQ_AbsoluteExternalPositionalAccuracy>
      </gmd:report>
      <gmd:report>
        <gmd:DQ_AbsoluteExternalPositionalAccuracy>
          <gmd:nameOfMeasure>
            <gco:CharacterString>Longitude Resolution</gco:CharacterString>
          </gmd:nameOfMeasure>
          <gmd:result>
            <gmd:DQ_QuantitativeResult>
              <gmd:valueUnit>
                <gml:DerivedUnit gml:id="d7387e374">
                  <gml:identifier codeSpace=""/>
                  <gml:derivationUnitTerm uom="meters (bands 10 and 11)"/>
                </gml:DerivedUnit>
              </gmd:valueUnit>
              <gmd:value>
                <gco:Record>100</gco:Record>
              </gmd:value>
            </gmd:DQ_QuantitativeResult>
          </gmd:result>
        </gmd:DQ_AbsoluteExternalPositionalAccuracy>
      </gmd:report>
      <gmd:lineage>
        <gmd:LI_Lineage>
          <gmd:statement>
            <?xml version="1.0" encoding="UTF-8"?>
<gmd:MD_Metadata xmlns:gmd="http://www.isotc211.org/2005/gmd" xmlns:atom="http://www.w3.org/2005/Atom" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dif="http://gcmd.gsfc.nasa.gov/Aboutus/xml/dif/" xmlns:eo="http://a9.com/-/opensearch/extensions/eo/1.0/" xmlns:fn="http://www.w3.org/2005/02/xpath-functions" xmlns:gco="http://www.isotc211.org/2005/gco" xmlns:geo="http://a9.com/-/opensearch/extensions/geo/1.0/" xmlns:geonet="http://www.fao.org/geonetwork" xmlns:georss="http://www.georss.org/georss" xmlns:gml="http://www.opengis.net/gml/3.2" xmlns:os="http://a9.com/-/spec/opensearch/1.1/" xmlns:semantic="http://a9.com/-/opensearch/extensions/semantic/1.0/" xmlns:sru="http://a9.com/-/opensearch/extensions/sru/2.0/" xmlns:time="http://a9.com/-/opensearch/extensions/time/1.0/" xmlns:util="java:java.util.UUID">
  <gmd:fileIdentifier>
    <gco:CharacterString>Landsat_8</gco:CharacterString>
  </gmd:fileIdentifier>
  <gmd:language>
    <gco:CharacterString>eng</gco:CharacterString>
  </gmd:language>
  <gmd:characterSet>
    <gmd:MD_CharacterSetCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#MD_CharacterSetCode" codeListValue="utf8">utf8</gmd:MD_CharacterSetCode>
  </gmd:characterSet>
  <gmd:hierarchyLevel>
    <gmd:MD_ScopeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#MD_ScopeCode" codeListValue="series">series</gmd:MD_ScopeCode>
  </gmd:hierarchyLevel>
  <gmd:contact>
    <gmd:CI_ResponsibleParty>
      <gmd:individualName>
        <gco:CharacterString>EROS CENTER,</gco:CharacterString>
      </gmd:individualName>
      <gmd:contactInfo>
        <gmd:CI_Contact>
          <gmd:phone>
            <gmd:CI_Telephone>
              <gmd:voice>
                <gco:CharacterString>605-594-6151, 800-252-4547</gco:CharacterString>
              </gmd:voice>
              <gmd:facsimile>
                <gco:CharacterString>605-594-6589</gco:CharacterString>
              </gmd:facsimile>
            </gmd:CI_Telephone>
          </gmd:phone>
          <gmd:address>
            <gmd:CI_Address>
              <gmd:deliveryPoint>
                <gco:CharacterString>LTA Customer Services</gco:CharacterString>
              </gmd:deliveryPoint>
              <gmd:deliveryPoint>
                <gco:CharacterString>U.S. Geological Survey</gco:CharacterString>
              </gmd:deliveryPoint>
              <gmd:deliveryPoint>
                <gco:CharacterString>EROS Center</gco:CharacterString>
              </gmd:deliveryPoint>
              <gmd:deliveryPoint>
                <gco:CharacterString>47914 252nd Street</gco:CharacterString>
              </gmd:deliveryPoint>
              <gmd:city>
                <gco:CharacterString>Sioux Falls</gco:CharacterString>
              </gmd:city>
              <gmd:administrativeArea>
                <gco:CharacterString>SD</gco:CharacterString>
              </gmd:administrativeArea>
              <gmd:postalCode>
                <gco:CharacterString>57198-0001</gco:CharacterString>
              </gmd:postalCode>
              <gmd:country>
                <gco:CharacterString>USA</gco:CharacterString>
              </gmd:country>
              <gmd:electronicMailAddress>
                <gco:CharacterString>lta@usgs.gov</gco:CharacterString>
              </gmd:electronicMailAddress>
            </gmd:CI_Address>
          </gmd:address>
        </gmd:CI_Contact>
      </gmd:contactInfo>
      <gmd:role>
        <gmd:CI_RoleCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_RoleCode" codeListValue="author">author</gmd:CI_RoleCode>
      </gmd:role>
    </gmd:CI_ResponsibleParty>
  </gmd:contact>
  <gmd:contact>
    <gmd:CI_ResponsibleParty>
      <gmd:organisationName>
        <gco:CharacterString>GCMD</gco:CharacterString>
      </gmd:organisationName>
      <gmd:role>
        <gmd:CI_RoleCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_RoleCode" codeListValue="originator">originator</gmd:CI_RoleCode>
      </gmd:role>
    </gmd:CI_ResponsibleParty>
  </gmd:contact>
  <gmd:dateStamp>
    <gco:Date>2013-03-13</gco:Date>
  </gmd:dateStamp>
  <gmd:metadataStandardName>
    <gco:CharacterString>CEOS IDN DIF</gco:CharacterString>
  </gmd:metadataStandardName>
  <gmd:metadataStandardVersion>
    <gco:CharacterString>VERSION 9.9.3</gco:CharacterString>
  </gmd:metadataStandardVersion>
  <gmd:identificationInfo>
    <gmd:MD_DataIdentification>
      <gmd:citation>
        <gmd:CI_Citation>
          <gmd:title>
            <gco:CharacterString>Landsat 8</gco:CharacterString>
          </gmd:title>
          <gmd:alternateTitle>
            <gco:CharacterString>Landsat 8</gco:CharacterString>
          </gmd:alternateTitle>
          <gmd:date>
            <gmd:CI_Date>
              <gmd:date>
                <gco:Date>2013-03-13</gco:Date>
              </gmd:date>
              <gmd:dateType>
                <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="publication">publication</gmd:CI_DateTypeCode>
              </gmd:dateType>
            </gmd:CI_Date>
          </gmd:date>
          <gmd:citedResponsibleParty>
            <gmd:CI_ResponsibleParty>
              <gmd:individualName>
                <gco:CharacterString>U.S. Geological Survey</gco:CharacterString>
              </gmd:individualName>
              <gmd:role>
                <gmd:CI_RoleCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_RoleCode" codeListValue="originator">originator</gmd:CI_RoleCode>
              </gmd:role>
            </gmd:CI_ResponsibleParty>
          </gmd:citedResponsibleParty>
          <gmd:citedResponsibleParty>                              </gmd:citedResponsibleParty>
          <gmd:citedResponsibleParty>                              </gmd:citedResponsibleParty>
        </gmd:CI_Citation>
      </gmd:citation>
      <gmd:abstract>
        <gco:CharacterString>This data set is a raster file containing global information for bands 1 through 11 for Landsat 8 Operational Land Imager (OLI) and Thermal Infrared Sensor (TIRS).    [ This document was provided by NASA Global Change Master Directory.       For more information on the source of this metadata please visit      http://gcmd.nasa.gov/r/geoss/[GCMD]Landsat_8 ]</gco:CharacterString>
      </gmd:abstract>
      <gmd:purpose>
        <gco:CharacterString>The mission of the Landsat 8 satellite is to provide a vehicle for continuing the flow of global change information to users worldwide. The Landsat 8 satellite fulfills its mission by providing repetitive, synoptic coverage continental surfaces and by collecting data in spectral bands that include the visible, near-infrared, shortwave, and thermal infrared portions of the electromagnetic spectrum. Landsat 8 mission objectives include:      1)  Maintaining Landsat data continuity by providing data that are consistent in terms of data acquisition, geometry, spatial resolution, calibration, coverage characteristics, and spectral characteristics with previous Landsat data.      2)  Generating and periodically refreshing a global archive of substantially cloud-free, Sun-lit, land-mass imagery.      3)  Continuing to make remote sensing satellite data available to domestic and international users and expanding the use of such data for global change research in both the Government and private commercial sectors.      4)  Promoting interdisciplinary research via synergism with other EOS observations, specifically, orbiting in tandem with the EOS Terra satellite for near coincident observations.      Supplemental_Information:      The Landsat 8 satellite is another step in the development and application of remotely sensed satellite data for use in managing the Earth land resources. Improving upon earlier Landsat systems, the OLI and TIRS sensors aboard Landsat 8, provide for new capabilities in the remote sensing of Earth land surface. Landsat 8 data are collected from a nominal altitude of 705 kilometers in a near-polar, near-circular, Sun-synchronous orbit at an inclination of 98.2 degrees, imaging the same 183-km swath of Earth surface every 16 days. Additional information can be found at the following sites: http://landsat.gsfc.nasa.gov/ and http://landsat.usgs.gov/ Landsat 8 Information    http://landsat.usgs.gov/landsat8.php      Data Set Credit:      The Landsat Program, as defined by Cong</gco:CharacterString>
      </gmd:purpose>
      <gmd:status>
        <gmd:MD_ProgressCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#MD_ProgressCode" codeListValue="onGoing">onGoing</gmd:MD_ProgressCode>
      </gmd:status>
      <gmd:pointOfContact>
        <gmd:CI_ResponsibleParty>
          <gmd:individualName>
            <gco:CharacterString>EROS CENTER,</gco:CharacterString>
          </gmd:individualName>
          <gmd:organisationName>
            <gco:CharacterString>DOI/USGS/EROS &gt; Earth Resources Observation and Science Center, U.S. Geological Survey, U.S. Department of the Interior</gco:CharacterString>
          </gmd:organisationName>
          <gmd:positionName>
            <gco:CharacterString>DATA CENTER CONTACT</gco:CharacterString>
          </gmd:positionName>
          <gmd:contactInfo>
            <gmd:CI_Contact>
              <gmd:phone>
                <gmd:CI_Telephone>
                  <gmd:voice>
                    <gco:CharacterString>605-594-6151, 800-252-4547</gco:CharacterString>
                  </gmd:voice>
                  <gmd:facsimile>
                    <gco:CharacterString>605-594-6589</gco:CharacterString>
                  </gmd:facsimile>
                </gmd:CI_Telephone>
              </gmd:phone>
              <gmd:address>
                <gmd:CI_Address>
                  <gmd:deliveryPoint>
                    <gco:CharacterString>LTA Customer Services</gco:CharacterString>
                  </gmd:deliveryPoint>
                  <gmd:deliveryPoint>
                    <gco:CharacterString>U.S. Geological Survey</gco:CharacterString>
                  </gmd:deliveryPoint>
                  <gmd:deliveryPoint>
                    <gco:CharacterString>EROS Center</gco:CharacterString>
                  </gmd:deliveryPoint>
                  <gmd:deliveryPoint>
                    <gco:CharacterString>47914 252nd Street</gco:CharacterString>
                  </gmd:deliveryPoint>
                  <gmd:city>
                    <gco:CharacterString>Sioux Falls</gco:CharacterString>
                  </gmd:city>
                  <gmd:administrativeArea>
                    <gco:CharacterString>SD</gco:CharacterString>
                  </gmd:administrativeArea>
                  <gmd:postalCode>
                    <gco:CharacterString>57198-0001</gco:CharacterString>
                  </gmd:postalCode>
                  <gmd:country>
                    <gco:CharacterString>USA</gco:CharacterString>
                  </gmd:country>
                  <gmd:electronicMailAddress>
                    <gco:CharacterString>lta@usgs.gov</gco:CharacterString>
                  </gmd:electronicMailAddress>
                </gmd:CI_Address>
              </gmd:address>
              <gmd:onlineResource>
                <gmd:CI_OnlineResource>
                  <gmd:linkage>
                    <gmd:URL>http://eros.usgs.gov/</gmd:URL>
                  </gmd:linkage>
                </gmd:CI_OnlineResource>
              </gmd:onlineResource>
            </gmd:CI_Contact>
          </gmd:contactInfo>
          <gmd:role>
            <gmd:CI_RoleCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_RoleCode" codeListValue="resourceProvider">resourceProvider</gmd:CI_RoleCode>
          </gmd:role>
        </gmd:CI_ResponsibleParty>
      </gmd:pointOfContact>
      <gmd:descriptiveKeywords>
        <gmd:MD_Keywords>
          <gmd:keyword>
            <gco:CharacterString>EARTH SCIENCE &gt; LAND SURFACE &gt; LAND USE/LAND COVER &gt; LAND USE CLASSES</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>EARTH SCIENCE &gt; LAND SURFACE &gt; LANDSCAPE &gt; LANDSCAPE PATTERNS</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>EARTH SCIENCE &gt; LAND SURFACE &gt; SURFACE RADIATIVE PROPERTIES &gt; REFLECTANCE</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>EARTH SCIENCE &gt; LAND SURFACE &gt; GEOMORPHIC LANDFORMS/PROCESSES</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>EARTH SCIENCE &gt; SPECTRAL/ENGINEERING &gt; INFRARED WAVELENGTHS &gt; BRIGHTNESS TEMPERATURE</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>EARTH SCIENCE &gt; SPECTRAL/ENGINEERING &gt; INFRARED WAVELENGTHS &gt; INFRARED IMAGERY</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>EARTH SCIENCE &gt; SPECTRAL/ENGINEERING &gt; SENSOR CHARACTERISTICS &gt; ULTRAVIOLET SENSOR TEMPERATURE</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>EARTH SCIENCE &gt; SPECTRAL/ENGINEERING &gt; VISIBLE WAVELENGTHS &gt; VISIBLE IMAGERY</gco:CharacterString>
          </gmd:keyword>
          <gmd:thesaurusName>
            <gmd:CI_Citation>
              <gmd:title>
                <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
              </gmd:title>
              <gmd:alternateTitle>
                <gco:CharacterString>Science Keywords</gco:CharacterString>
              </gmd:alternateTitle>
              <gmd:date>
                <gmd:CI_Date>
                  <gmd:date>
                    <gco:Date>2008-02-05</gco:Date>
                  </gmd:date>
                  <gmd:dateType>
                    <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision">revision</gmd:CI_DateTypeCode>
                  </gmd:dateType>
                </gmd:CI_Date>
              </gmd:date>
              <gmd:collectiveTitle>
                <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
              </gmd:collectiveTitle>
            </gmd:CI_Citation>
          </gmd:thesaurusName>
        </gmd:MD_Keywords>
      </gmd:descriptiveKeywords>
      <gmd:descriptiveKeywords>
        <gmd:MD_Keywords>
          <gmd:keyword>
            <gco:CharacterString>EROS</gco:CharacterString>
          </gmd:keyword>
        </gmd:MD_Keywords>
      </gmd:descriptiveKeywords>
      <gmd:descriptiveKeywords>
        <gmd:MD_Keywords>
          <gmd:keyword>
            <gco:CharacterString>OLI &gt; Operational Land Imager</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>TIRS &gt; Thermal Infrared Sensor</gco:CharacterString>
          </gmd:keyword>
          <gmd:thesaurusName>
            <gmd:CI_Citation>
              <gmd:title>
                <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
              </gmd:title>
              <gmd:alternateTitle>
                <gco:CharacterString>Instruments</gco:CharacterString>
              </gmd:alternateTitle>
              <gmd:date>
                <gmd:CI_Date>
                  <gmd:date>
                    <gco:Date>2009-06-10</gco:Date>
                  </gmd:date>
                  <gmd:dateType>
                    <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision">revision</gmd:CI_DateTypeCode>
                  </gmd:dateType>
                </gmd:CI_Date>
              </gmd:date>
              <gmd:collectiveTitle>
                <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
              </gmd:collectiveTitle>
            </gmd:CI_Citation>
          </gmd:thesaurusName>
        </gmd:MD_Keywords>
      </gmd:descriptiveKeywords>
      <gmd:descriptiveKeywords>
        <gmd:MD_Keywords>
          <gmd:keyword>
            <gco:CharacterString>LANDSAT-8</gco:CharacterString>
          </gmd:keyword>
          <gmd:thesaurusName>
            <gmd:CI_Citation>
              <gmd:title>
                <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
              </gmd:title>
              <gmd:alternateTitle>
                <gco:CharacterString>Platforms</gco:CharacterString>
              </gmd:alternateTitle>
              <gmd:date>
                <gmd:CI_Date>
                  <gmd:date>
                    <gco:Date>2009-06-10</gco:Date>
                  </gmd:date>
                  <gmd:dateType>
                    <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision">revision</gmd:CI_DateTypeCode>
                  </gmd:dateType>
                </gmd:CI_Date>
              </gmd:date>
              <gmd:collectiveTitle>
                <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
              </gmd:collectiveTitle>
            </gmd:CI_Citation>
          </gmd:thesaurusName>
        </gmd:MD_Keywords>
      </gmd:descriptiveKeywords>
      <gmd:descriptiveKeywords>
        <gmd:MD_Keywords>
          <gmd:keyword>
            <gco:CharacterString>CWIC &gt; CEOS WGISS Integrated Catalog</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>ESIP &gt; Earth Science Information Partners Program</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>LDCM &gt; Landsat Data Continuity Mission</gco:CharacterString>
          </gmd:keyword>
          <gmd:thesaurusName>
            <gmd:CI_Citation>
              <gmd:title>
                <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
              </gmd:title>
              <gmd:alternateTitle>
                <gco:CharacterString>Projects</gco:CharacterString>
              </gmd:alternateTitle>
              <gmd:date>
                <gmd:CI_Date>
                  <gmd:date>
                    <gco:Date>2009-06-10</gco:Date>
                  </gmd:date>
                  <gmd:dateType>
                    <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision">revision</gmd:CI_DateTypeCode>
                  </gmd:dateType>
                </gmd:CI_Date>
              </gmd:date>
              <gmd:collectiveTitle>
                <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
              </gmd:collectiveTitle>
            </gmd:CI_Citation>
          </gmd:thesaurusName>
        </gmd:MD_Keywords>
      </gmd:descriptiveKeywords>
      <gmd:descriptiveKeywords>
        <gmd:MD_Keywords>
          <gmd:keyword>
            <gco:CharacterString>SOOS</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>USA/USGS</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>AMD</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>ECHO</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>ARCTIC</gco:CharacterString>
          </gmd:keyword>
          <gmd:thesaurusName>
            <gmd:CI_Citation>
              <gmd:title>
                <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
              </gmd:title>
              <gmd:alternateTitle>
                <gco:CharacterString>IDN Nodes</gco:CharacterString>
              </gmd:alternateTitle>
              <gmd:date>
                <gmd:CI_Date>
                  <gmd:date>
                    <gco:Date>2007-04-01</gco:Date>
                  </gmd:date>
                  <gmd:dateType>
                    <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision">revision</gmd:CI_DateTypeCode>
                  </gmd:dateType>
                </gmd:CI_Date>
              </gmd:date>
              <gmd:collectiveTitle>
                <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
              </gmd:collectiveTitle>
            </gmd:CI_Citation>
          </gmd:thesaurusName>
        </gmd:MD_Keywords>
      </gmd:descriptiveKeywords>
      <gmd:descriptiveKeywords>
        <gmd:MD_Keywords>
          <gmd:keyword>
            <gco:CharacterString>CONTINENT &gt; AUSTRALIA/NEW ZEALAND &gt; AUSTRALIA</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>CONTINENT &gt; NORTH AMERICA &gt; GREENLAND</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>CONTINENT &gt; AFRICA</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>GEOGRAPHIC REGION &gt; ARCTIC</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>CONTINENT &gt; ASIA</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>CONTINENT &gt; EUROPE</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>CONTINENT &gt; NORTH AMERICA</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>GEOGRAPHIC REGION &gt; GLOBAL</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>GEOGRAPHIC REGION &gt; POLAR</gco:CharacterString>
          </gmd:keyword>
          <gmd:keyword>
            <gco:CharacterString>CONTINENT &gt; SOUTH AMERICA</gco:CharacterString>
          </gmd:keyword>
          <gmd:thesaurusName>
            <gmd:CI_Citation>
              <gmd:title>
                <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
              </gmd:title>
              <gmd:alternateTitle>
                <gco:CharacterString>Locations</gco:CharacterString>
              </gmd:alternateTitle>
              <gmd:date>
                <gmd:CI_Date>
                  <gmd:date>
                    <gco:Date>2009-12-22</gco:Date>
                  </gmd:date>
                  <gmd:dateType>
                    <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision">revision</gmd:CI_DateTypeCode>
                  </gmd:dateType>
                </gmd:CI_Date>
              </gmd:date>
              <gmd:collectiveTitle>
                <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
              </gmd:collectiveTitle>
            </gmd:CI_Citation>
          </gmd:thesaurusName>
        </gmd:MD_Keywords>
      </gmd:descriptiveKeywords>
      <gmd:descriptiveKeywords>
        <gmd:MD_Keywords>
          <gmd:keyword>
            <gco:CharacterString>geossDataCore</gco:CharacterString>
          </gmd:keyword>
        </gmd:MD_Keywords>
      </gmd:descriptiveKeywords>
      <gmd:descriptiveKeywords>
        <gmd:MD_Keywords>
          <gmd:keyword>
            <gco:CharacterString>geossNoMonetaryCharge</gco:CharacterString>
          </gmd:keyword>
        </gmd:MD_Keywords>
      </gmd:descriptiveKeywords>
      <gmd:resourceConstraints>
        <gmd:MD_LegalConstraints>
          <gmd:accessConstraints>
            <gmd:MD_RestrictionCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#MD_RestrictionCode" codeListValue="otherRestrictions">otherRestrictions</gmd:MD_RestrictionCode>
          </gmd:accessConstraints>
          <gmd:otherConstraints>
            <gco:CharacterString>There are no restrictions to this data set.</gco:CharacterString>
          </gmd:otherConstraints>
        </gmd:MD_LegalConstraints>
      </gmd:resourceConstraints>
      <gmd:resourceConstraints>
        <gmd:MD_LegalConstraints>
          <gmd:useConstraints>
            <gmd:MD_RestrictionCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#MD_RestrictionCode" codeListValue="otherRestrictions">otherRestrictions</gmd:MD_RestrictionCode>
          </gmd:useConstraints>
          <gmd:otherConstraints>
            <gco:CharacterString>There is no guarantee of warranty concerning the accuracy of these data.  Users should be aware that temporal changes may have occurred since the data was collected and that some parts of these data may no longer represent actual surface conditions.  Users should not use these data for critical applications without a full awareness of their limitations.  Acknowledgement of the originating agencies would be appreciated in products derived from these data.  Any user who modifies the data set is obligated to describe the types of modifications they perform.  User specifically agrees not to misrepresent the data set, nor to imply that changes made were approved or endorsed by the U.S. Geological Survey.  Please refer to http://www.usgs.gov/privacy.html for the USGS disclaimer.</gco:CharacterString>
          </gmd:otherConstraints>
        </gmd:MD_LegalConstraints>
      </gmd:resourceConstraints>
      <gmd:language>
        <gco:CharacterString>eng</gco:CharacterString>
      </gmd:language>
      <gmd:characterSet>
        <gmd:MD_CharacterSetCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#MD_CharacterSetCode" codeListValue="utf8">utf8</gmd:MD_CharacterSetCode>
      </gmd:characterSet>
      <gmd:topicCategory>
        <gmd:MD_TopicCategoryCode>climatologyMeteorologyAtmosphere</gmd:MD_TopicCategoryCode>
      </gmd:topicCategory>
      <gmd:topicCategory>
        <gmd:MD_TopicCategoryCode>elevation</gmd:MD_TopicCategoryCode>
      </gmd:topicCategory>
      <gmd:topicCategory>
        <gmd:MD_TopicCategoryCode>geoscientificInformation</gmd:MD_TopicCategoryCode>
      </gmd:topicCategory>
      <gmd:topicCategory>
        <gmd:MD_TopicCategoryCode>imageryBaseMapsEarthCover</gmd:MD_TopicCategoryCode>
      </gmd:topicCategory>
      <gmd:topicCategory>
        <gmd:MD_TopicCategoryCode>planningCadastre</gmd:MD_TopicCategoryCode>
      </gmd:topicCategory>
      <gmd:extent>
        <gmd:EX_Extent>
          <gmd:geographicElement>
            <gmd:EX_GeographicBoundingBox>
              <gmd:westBoundLongitude>
                <gco:Decimal>-180.0</gco:Decimal>
              </gmd:westBoundLongitude>
              <gmd:eastBoundLongitude>
                <gco:Decimal>180.0</gco:Decimal>
              </gmd:eastBoundLongitude>
              <gmd:southBoundLatitude>
                <gco:Decimal>-82.71</gco:Decimal>
              </gmd:southBoundLatitude>
              <gmd:northBoundLatitude>
                <gco:Decimal>82.74</gco:Decimal>
              </gmd:northBoundLatitude>
            </gmd:EX_GeographicBoundingBox>
          </gmd:geographicElement>
          <gmd:geographicElement>
            <gmd:EX_GeographicDescription>
              <gmd:geographicIdentifier>
                <gmd:MD_Identifier>
                  <gmd:authority>
                    <gmd:CI_Citation>
                      <gmd:title>
                        <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
                      </gmd:title>
                      <gmd:alternateTitle>
                        <gco:CharacterString>Locations</gco:CharacterString>
                      </gmd:alternateTitle>
                      <gmd:date>
                        <gmd:CI_Date>
                          <gmd:date>
                            <gco:Date>2009-12-22</gco:Date>
                          </gmd:date>
                          <gmd:dateType>
                            <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision"/>
                          </gmd:dateType>
                        </gmd:CI_Date>
                      </gmd:date>
                      <gmd:collectiveTitle>
                        <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
                      </gmd:collectiveTitle>
                    </gmd:CI_Citation>
                  </gmd:authority>
                  <gmd:code>
                    <gco:CharacterString>CONTINENT &gt; AUSTRALIA/NEW ZEALAND &gt; AUSTRALIA</gco:CharacterString>
                  </gmd:code>
                </gmd:MD_Identifier>
              </gmd:geographicIdentifier>
            </gmd:EX_GeographicDescription>
          </gmd:geographicElement>
          <gmd:geographicElement>
            <gmd:EX_GeographicDescription>
              <gmd:geographicIdentifier>
                <gmd:MD_Identifier>
                  <gmd:authority>
                    <gmd:CI_Citation>
                      <gmd:title>
                        <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
                      </gmd:title>
                      <gmd:alternateTitle>
                        <gco:CharacterString>Locations</gco:CharacterString>
                      </gmd:alternateTitle>
                      <gmd:date>
                        <gmd:CI_Date>
                          <gmd:date>
                            <gco:Date>2009-12-22</gco:Date>
                          </gmd:date>
                          <gmd:dateType>
                            <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision"/>
                          </gmd:dateType>
                        </gmd:CI_Date>
                      </gmd:date>
                      <gmd:collectiveTitle>
                        <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
                      </gmd:collectiveTitle>
                    </gmd:CI_Citation>
                  </gmd:authority>
                  <gmd:code>
                    <gco:CharacterString>CONTINENT &gt; NORTH AMERICA &gt; GREENLAND</gco:CharacterString>
                  </gmd:code>
                </gmd:MD_Identifier>
              </gmd:geographicIdentifier>
            </gmd:EX_GeographicDescription>
          </gmd:geographicElement>
          <gmd:geographicElement>
            <gmd:EX_GeographicDescription>
              <gmd:geographicIdentifier>
                <gmd:MD_Identifier>
                  <gmd:authority>
                    <gmd:CI_Citation>
                      <gmd:title>
                        <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
                      </gmd:title>
                      <gmd:alternateTitle>
                        <gco:CharacterString>Locations</gco:CharacterString>
                      </gmd:alternateTitle>
                      <gmd:date>
                        <gmd:CI_Date>
                          <gmd:date>
                            <gco:Date>2009-12-22</gco:Date>
                          </gmd:date>
                          <gmd:dateType>
                            <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision"/>
                          </gmd:dateType>
                        </gmd:CI_Date>
                      </gmd:date>
                      <gmd:collectiveTitle>
                        <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
                      </gmd:collectiveTitle>
                    </gmd:CI_Citation>
                  </gmd:authority>
                  <gmd:code>
                    <gco:CharacterString>CONTINENT &gt; AFRICA</gco:CharacterString>
                  </gmd:code>
                </gmd:MD_Identifier>
              </gmd:geographicIdentifier>
            </gmd:EX_GeographicDescription>
          </gmd:geographicElement>
          <gmd:geographicElement>
            <gmd:EX_GeographicDescription>
              <gmd:geographicIdentifier>
                <gmd:MD_Identifier>
                  <gmd:authority>
                    <gmd:CI_Citation>
                      <gmd:title>
                        <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
                      </gmd:title>
                      <gmd:alternateTitle>
                        <gco:CharacterString>Locations</gco:CharacterString>
                      </gmd:alternateTitle>
                      <gmd:date>
                        <gmd:CI_Date>
                          <gmd:date>
                            <gco:Date>2009-12-22</gco:Date>
                          </gmd:date>
                          <gmd:dateType>
                            <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision"/>
                          </gmd:dateType>
                        </gmd:CI_Date>
                      </gmd:date>
                      <gmd:collectiveTitle>
                        <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
                      </gmd:collectiveTitle>
                    </gmd:CI_Citation>
                  </gmd:authority>
                  <gmd:code>
                    <gco:CharacterString>GEOGRAPHIC REGION &gt; ARCTIC</gco:CharacterString>
                  </gmd:code>
                </gmd:MD_Identifier>
              </gmd:geographicIdentifier>
            </gmd:EX_GeographicDescription>
          </gmd:geographicElement>
          <gmd:geographicElement>
            <gmd:EX_GeographicDescription>
              <gmd:geographicIdentifier>
                <gmd:MD_Identifier>
                  <gmd:authority>
                    <gmd:CI_Citation>
                      <gmd:title>
                        <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
                      </gmd:title>
                      <gmd:alternateTitle>
                        <gco:CharacterString>Locations</gco:CharacterString>
                      </gmd:alternateTitle>
                      <gmd:date>
                        <gmd:CI_Date>
                          <gmd:date>
                            <gco:Date>2009-12-22</gco:Date>
                          </gmd:date>
                          <gmd:dateType>
                            <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision"/>
                          </gmd:dateType>
                        </gmd:CI_Date>
                      </gmd:date>
                      <gmd:collectiveTitle>
                        <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
                      </gmd:collectiveTitle>
                    </gmd:CI_Citation>
                  </gmd:authority>
                  <gmd:code>
                    <gco:CharacterString>CONTINENT &gt; ASIA</gco:CharacterString>
                  </gmd:code>
                </gmd:MD_Identifier>
              </gmd:geographicIdentifier>
            </gmd:EX_GeographicDescription>
          </gmd:geographicElement>
          <gmd:geographicElement>
            <gmd:EX_GeographicDescription>
              <gmd:geographicIdentifier>
                <gmd:MD_Identifier>
                  <gmd:authority>
                    <gmd:CI_Citation>
                      <gmd:title>
                        <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
                      </gmd:title>
                      <gmd:alternateTitle>
                        <gco:CharacterString>Locations</gco:CharacterString>
                      </gmd:alternateTitle>
                      <gmd:date>
                        <gmd:CI_Date>
                          <gmd:date>
                            <gco:Date>2009-12-22</gco:Date>
                          </gmd:date>
                          <gmd:dateType>
                            <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision"/>
                          </gmd:dateType>
                        </gmd:CI_Date>
                      </gmd:date>
                      <gmd:collectiveTitle>
                        <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
                      </gmd:collectiveTitle>
                    </gmd:CI_Citation>
                  </gmd:authority>
                  <gmd:code>
                    <gco:CharacterString>CONTINENT &gt; EUROPE</gco:CharacterString>
                  </gmd:code>
                </gmd:MD_Identifier>
              </gmd:geographicIdentifier>
            </gmd:EX_GeographicDescription>
          </gmd:geographicElement>
          <gmd:geographicElement>
            <gmd:EX_GeographicDescription>
              <gmd:geographicIdentifier>
                <gmd:MD_Identifier>
                  <gmd:authority>
                    <gmd:CI_Citation>
                      <gmd:title>
                        <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
                      </gmd:title>
                      <gmd:alternateTitle>
                        <gco:CharacterString>Locations</gco:CharacterString>
                      </gmd:alternateTitle>
                      <gmd:date>
                        <gmd:CI_Date>
                          <gmd:date>
                            <gco:Date>2009-12-22</gco:Date>
                          </gmd:date>
                          <gmd:dateType>
                            <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision"/>
                          </gmd:dateType>
                        </gmd:CI_Date>
                      </gmd:date>
                      <gmd:collectiveTitle>
                        <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
                      </gmd:collectiveTitle>
                    </gmd:CI_Citation>
                  </gmd:authority>
                  <gmd:code>
                    <gco:CharacterString>CONTINENT &gt; NORTH AMERICA</gco:CharacterString>
                  </gmd:code>
                </gmd:MD_Identifier>
              </gmd:geographicIdentifier>
            </gmd:EX_GeographicDescription>
          </gmd:geographicElement>
          <gmd:geographicElement>
            <gmd:EX_GeographicDescription>
              <gmd:geographicIdentifier>
                <gmd:MD_Identifier>
                  <gmd:authority>
                    <gmd:CI_Citation>
                      <gmd:title>
                        <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
                      </gmd:title>
                      <gmd:alternateTitle>
                        <gco:CharacterString>Locations</gco:CharacterString>
                      </gmd:alternateTitle>
                      <gmd:date>
                        <gmd:CI_Date>
                          <gmd:date>
                            <gco:Date>2009-12-22</gco:Date>
                          </gmd:date>
                          <gmd:dateType>
                            <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision"/>
                          </gmd:dateType>
                        </gmd:CI_Date>
                      </gmd:date>
                      <gmd:collectiveTitle>
                        <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
                      </gmd:collectiveTitle>
                    </gmd:CI_Citation>
                  </gmd:authority>
                  <gmd:code>
                    <gco:CharacterString>GEOGRAPHIC REGION &gt; GLOBAL</gco:CharacterString>
                  </gmd:code>
                </gmd:MD_Identifier>
              </gmd:geographicIdentifier>
            </gmd:EX_GeographicDescription>
          </gmd:geographicElement>
          <gmd:geographicElement>
            <gmd:EX_GeographicDescription>
              <gmd:geographicIdentifier>
                <gmd:MD_Identifier>
                  <gmd:authority>
                    <gmd:CI_Citation>
                      <gmd:title>
                        <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
                      </gmd:title>
                      <gmd:alternateTitle>
                        <gco:CharacterString>Locations</gco:CharacterString>
                      </gmd:alternateTitle>
                      <gmd:date>
                        <gmd:CI_Date>
                          <gmd:date>
                            <gco:Date>2009-12-22</gco:Date>
                          </gmd:date>
                          <gmd:dateType>
                            <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision"/>
                          </gmd:dateType>
                        </gmd:CI_Date>
                      </gmd:date>
                      <gmd:collectiveTitle>
                        <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
                      </gmd:collectiveTitle>
                    </gmd:CI_Citation>
                  </gmd:authority>
                  <gmd:code>
                    <gco:CharacterString>GEOGRAPHIC REGION &gt; POLAR</gco:CharacterString>
                  </gmd:code>
                </gmd:MD_Identifier>
              </gmd:geographicIdentifier>
            </gmd:EX_GeographicDescription>
          </gmd:geographicElement>
          <gmd:geographicElement>
            <gmd:EX_GeographicDescription>
              <gmd:geographicIdentifier>
                <gmd:MD_Identifier>
                  <gmd:authority>
                    <gmd:CI_Citation>
                      <gmd:title>
                        <gco:CharacterString>NASA/GCMD Earth Science Keywords</gco:CharacterString>
                      </gmd:title>
                      <gmd:alternateTitle>
                        <gco:CharacterString>Locations</gco:CharacterString>
                      </gmd:alternateTitle>
                      <gmd:date>
                        <gmd:CI_Date>
                          <gmd:date>
                            <gco:Date>2009-12-22</gco:Date>
                          </gmd:date>
                          <gmd:dateType>
                            <gmd:CI_DateTypeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_DateTypeCode" codeListValue="revision"/>
                          </gmd:dateType>
                        </gmd:CI_Date>
                      </gmd:date>
                      <gmd:collectiveTitle>
                        <gco:CharacterString>Olsen, L.M., G. Major, K. Shein, J. Scialdone, R. Vogel, S. Leicester, H. Weir, S. Ritz, T. Stevens, M. Meaux, C.Solomon, R. Bilodeau, M. Holland, T. Northcutt, R. A. Restrepo, 2007 .   NASA/Global Change Master Directory (GCMD) Earth Science Keywords. Version  6.0.0.0.0</gco:CharacterString>
                      </gmd:collectiveTitle>
                    </gmd:CI_Citation>
                  </gmd:authority>
                  <gmd:code>
                    <gco:CharacterString>CONTINENT &gt; SOUTH AMERICA</gco:CharacterString>
                  </gmd:code>
                </gmd:MD_Identifier>
              </gmd:geographicIdentifier>
            </gmd:EX_GeographicDescription>
          </gmd:geographicElement>
          <gmd:temporalElement>
            <gmd:EX_TemporalExtent>
              <gmd:extent>
                <gml:TimePeriod gml:id="d7387e246">
                  <gml:begin>
                    <gml:TimeInstant gml:id="d7387e248">
                      <gml:timePosition>2013-02-11</gml:timePosition>
                    </gml:TimeInstant>
                  </gml:begin>
                  <gml:end>
                    <gml:TimeInstant gml:id="aadfe4406-50fc-4abc-bb8f-26f8eda08a1d">
                      <gml:timePosition indeterminatePosition="unknown"/>
                    </gml:TimeInstant>
                  </gml:end>
                </gml:TimePeriod>
              </gmd:extent>
            </gmd:EX_TemporalExtent>
          </gmd:temporalElement>
        </gmd:EX_Extent>
      </gmd:extent>
    </gmd:MD_DataIdentification>
  </gmd:identificationInfo>
  <gmd:distributionInfo>
    <gmd:MD_Distribution>
      <gmd:distributionFormat>
        <gmd:MD_Format>
          <gmd:name>
            <gco:CharacterString>GeoTIFF</gco:CharacterString>
          </gmd:name>
          <gmd:version gco:nilReason="missing">
            <gco:CharacterString/>
          </gmd:version>
        </gmd:MD_Format>
      </gmd:distributionFormat>
      <gmd:distributor>
        <gmd:MD_Distributor>
          <gmd:distributorContact>
            <gmd:CI_ResponsibleParty>
              <gmd:individualName>
                <gco:CharacterString>EROS CENTER,</gco:CharacterString>
              </gmd:individualName>
              <gmd:organisationName>
                <gco:CharacterString>DOI/USGS/EROS &gt; Earth Resources Observation and Science Center, U.S. Geological Survey, U.S. Department of the Interior</gco:CharacterString>
              </gmd:organisationName>
              <gmd:positionName>
                <gco:CharacterString>DATA CENTER CONTACT</gco:CharacterString>
              </gmd:positionName>
              <gmd:contactInfo>
                <gmd:CI_Contact>
                  <gmd:phone>
                    <gmd:CI_Telephone>
                      <gmd:voice>
                        <gco:CharacterString>605-594-6151, 800-252-4547</gco:CharacterString>
                      </gmd:voice>
                      <gmd:facsimile>
                        <gco:CharacterString>605-594-6589</gco:CharacterString>
                      </gmd:facsimile>
                    </gmd:CI_Telephone>
                  </gmd:phone>
                  <gmd:address>
                    <gmd:CI_Address>
                      <gmd:deliveryPoint>
                        <gco:CharacterString>LTA Customer Services</gco:CharacterString>
                      </gmd:deliveryPoint>
                      <gmd:deliveryPoint>
                        <gco:CharacterString>U.S. Geological Survey</gco:CharacterString>
                      </gmd:deliveryPoint>
                      <gmd:deliveryPoint>
                        <gco:CharacterString>EROS Center</gco:CharacterString>
                      </gmd:deliveryPoint>
                      <gmd:deliveryPoint>
                        <gco:CharacterString>47914 252nd Street</gco:CharacterString>
                      </gmd:deliveryPoint>
                      <gmd:city>
                        <gco:CharacterString>Sioux Falls</gco:CharacterString>
                      </gmd:city>
                      <gmd:administrativeArea>
                        <gco:CharacterString>SD</gco:CharacterString>
                      </gmd:administrativeArea>
                      <gmd:postalCode>
                        <gco:CharacterString>57198-0001</gco:CharacterString>
                      </gmd:postalCode>
                      <gmd:country>
                        <gco:CharacterString>USA</gco:CharacterString>
                      </gmd:country>
                      <gmd:electronicMailAddress>
                        <gco:CharacterString>lta@usgs.gov</gco:CharacterString>
                      </gmd:electronicMailAddress>
                    </gmd:CI_Address>
                  </gmd:address>
                  <gmd:onlineResource>
                    <gmd:CI_OnlineResource>
                      <gmd:linkage>
                        <gmd:URL>http://eros.usgs.gov/</gmd:URL>
                      </gmd:linkage>
                    </gmd:CI_OnlineResource>
                  </gmd:onlineResource>
                </gmd:CI_Contact>
              </gmd:contactInfo>
              <gmd:role>
                <gmd:CI_RoleCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_RoleCode" codeListValue="distributor">distributor</gmd:CI_RoleCode>
              </gmd:role>
            </gmd:CI_ResponsibleParty>
          </gmd:distributorContact>
          <gmd:distributionOrderProcess>
            <gmd:MD_StandardOrderProcess>
              <gmd:fees>
                <gco:CharacterString>$0 U.S. Dollars for Level 1 Terrain correction</gco:CharacterString>
              </gmd:fees>
            </gmd:MD_StandardOrderProcess>
          </gmd:distributionOrderProcess>
        </gmd:MD_Distributor>
      </gmd:distributor>
      <gmd:transferOptions>
        <gmd:MD_DigitalTransferOptions>
          <gmd:onLine>
            <gmd:CI_OnlineResource>
              <gmd:linkage>
                <gmd:URL>http://glovis.usgs.gov http://earthexplorer.usgs.gov</gmd:URL>
              </gmd:linkage>
              <gmd:protocol>
                <gco:CharacterString>WWW:LINK-1.0-http--link</gco:CharacterString>
              </gmd:protocol>
              <gmd:name>
                <gco:CharacterString>GET DATA</gco:CharacterString>
              </gmd:name>
              <gmd:description>
                <gco:CharacterString>Dataset searching and requesting capabilities are available through the Global Visualization viewer and EarthExplorer at the above URLs.</gco:CharacterString>
              </gmd:description>
            </gmd:CI_OnlineResource>
          </gmd:onLine>
          <gmd:onLine>
            <gmd:CI_OnlineResource>
              <gmd:linkage>
                <gmd:URL>http://landsat.usgs.gov/landsat8.php</gmd:URL>
              </gmd:linkage>
              <gmd:protocol>
                <gco:CharacterString>WWW:LINK-1.0-http--link</gco:CharacterString>
              </gmd:protocol>
              <gmd:name>
                <gco:CharacterString>VIEW RELATED INFORMATION</gco:CharacterString>
              </gmd:name>
              <gmd:description>
                <gco:CharacterString>Landsat 8 OLI and TIRS information.</gco:CharacterString>
              </gmd:description>
            </gmd:CI_OnlineResource>
          </gmd:onLine>
          <gmd:onLine>
            <gmd:CI_OnlineResource>
              <gmd:linkage>
                <gmd:URL>http://eros.usgs.gov</gmd:URL>
              </gmd:linkage>
              <gmd:protocol>
                <gco:CharacterString>WWW:LINK-1.0-http--link</gco:CharacterString>
              </gmd:protocol>
              <gmd:name>
                <gco:CharacterString>Data Set Citation</gco:CharacterString>
              </gmd:name>
              <gmd:description>
                <gco:CharacterString>Landsat 8</gco:CharacterString>
              </gmd:description>
            </gmd:CI_OnlineResource>
          </gmd:onLine>
        </gmd:MD_DigitalTransferOptions>
      </gmd:transferOptions>
    </gmd:MD_Distribution>
  </gmd:distributionInfo>
  <gmd:dataQualityInfo>
    <gmd:DQ_DataQuality>
      <gmd:scope>
        <gmd:DQ_Scope>
          <gmd:level>
            <gmd:MD_ScopeCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#MD_ScopeCode" codeListValue="dataset">dataset</gmd:MD_ScopeCode>
          </gmd:level>
        </gmd:DQ_Scope>
      </gmd:scope>
      <gmd:report>
        <gmd:DQ_AbsoluteExternalPositionalAccuracy>
          <gmd:nameOfMeasure>
            <gco:CharacterString>Latitude Resolution</gco:CharacterString>
          </gmd:nameOfMeasure>
          <gmd:result>
            <gmd:DQ_QuantitativeResult>
              <gmd:valueUnit>
                <gml:DerivedUnit gml:id="d7387e371">
                  <gml:identifier codeSpace=""/>
                  <gml:derivationUnitTerm uom="meters (bands 10 and 11)"/>
                </gml:DerivedUnit>
              </gmd:valueUnit>
              <gmd:value>
                <gco:Record>100</gco:Record>
              </gmd:value>
            </gmd:DQ_QuantitativeResult>
          </gmd:result>
        </gmd:DQ_AbsoluteExternalPositionalAccuracy>
      </gmd:report>
      <gmd:report>
        <gmd:DQ_AbsoluteExternalPositionalAccuracy>
          <gmd:nameOfMeasure>
            <gco:CharacterString>Longitude Resolution</gco:CharacterString>
          </gmd:nameOfMeasure>
          <gmd:result>
            <gmd:DQ_QuantitativeResult>
              <gmd:valueUnit>
                <gml:DerivedUnit gml:id="d7387e374">
                  <gml:identifier codeSpace=""/>
                  <gml:derivationUnitTerm uom="meters (bands 10 and 11)"/>
                </gml:DerivedUnit>
              </gmd:valueUnit>
              <gmd:value>
                <gco:Record>100</gco:Record>
              </gmd:value>
            </gmd:DQ_QuantitativeResult>
          </gmd:result>
        </gmd:DQ_AbsoluteExternalPositionalAccuracy>
      </gmd:report>
      <gmd:lineage>
        <gmd:LI_Lineage>
          <gmd:statement>
            <gco:CharacterString>Attribute_Accuracy: Attribute_Accuracy_Report: Panchromatic band 8 has a resolution of 15 meters. Logical_Consistency_Report: Landsat 8 data are collected from a nominal altitude of 705 kilometers in a near-polar, near-circular, Sun-synchronous orbit at an inclination of 98.2 degrees, imaging the same 183-km swath of the Earth surface every 16 days. The pixels representing the bands for  the image are in the data set only once. Positional_Accuracy:</gco:CharacterString>
          </gmd:statement>
        </gmd:LI_Lineage>
      </gmd:lineage>
    </gmd:DQ_DataQuality>
  </gmd:dataQualityInfo>
  <gmd:metadataConstraints>
    <gmd:MD_LegalConstraints>
      <gmd:useLimitation>
        <gco:CharacterString>This metadata record is publicly available.</gco:CharacterString>
      </gmd:useLimitation>
    </gmd:MD_LegalConstraints>
  </gmd:metadataConstraints>
  <gmd:metadataMaintenance>
    <gmd:MD_MaintenanceInformation>
      <gmd:maintenanceAndUpdateFrequency>
        <gmd:MD_MaintenanceFrequencyCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#MD_MaintenanceFrequencyCode" codeListValue="asNeeded">asNeeded</gmd:MD_MaintenanceFrequencyCode>
      </gmd:maintenanceAndUpdateFrequency>
      <gmd:dateOfNextUpdate>
        <gco:Date>2014-03-01</gco:Date>
      </gmd:dateOfNextUpdate>
      <gmd:contact>
        <gmd:CI_ResponsibleParty>
          <gmd:individualName>
            <gco:CharacterString>EROS CENTER,</gco:CharacterString>
          </gmd:individualName>
          <gmd:contactInfo>
            <gmd:CI_Contact>
              <gmd:phone>
                <gmd:CI_Telephone>
                  <gmd:voice>
                    <gco:CharacterString>605-594-6151, 800-252-4547</gco:CharacterString>
                  </gmd:voice>
                  <gmd:facsimile>
                    <gco:CharacterString>605-594-6589</gco:CharacterString>
                  </gmd:facsimile>
                </gmd:CI_Telephone>
              </gmd:phone>
              <gmd:address>
                <gmd:CI_Address>
                  <gmd:deliveryPoint>
                    <gco:CharacterString>LTA Customer Services</gco:CharacterString>
                  </gmd:deliveryPoint>
                  <gmd:deliveryPoint>
                    <gco:CharacterString>U.S. Geological Survey</gco:CharacterString>
                  </gmd:deliveryPoint>
                  <gmd:deliveryPoint>
                    <gco:CharacterString>EROS Center</gco:CharacterString>
                  </gmd:deliveryPoint>
                  <gmd:deliveryPoint>
                    <gco:CharacterString>47914 252nd Street</gco:CharacterString>
                  </gmd:deliveryPoint>
                  <gmd:city>
                    <gco:CharacterString>Sioux Falls</gco:CharacterString>
                  </gmd:city>
                  <gmd:administrativeArea>
                    <gco:CharacterString>SD</gco:CharacterString>
                  </gmd:administrativeArea>
                  <gmd:postalCode>
                    <gco:CharacterString>57198-0001</gco:CharacterString>
                  </gmd:postalCode>
                  <gmd:country>
                    <gco:CharacterString>USA</gco:CharacterString>
                  </gmd:country>
                  <gmd:electronicMailAddress>
                    <gco:CharacterString>lta@usgs.gov</gco:CharacterString>
                  </gmd:electronicMailAddress>
                </gmd:CI_Address>
              </gmd:address>
            </gmd:CI_Contact>
          </gmd:contactInfo>
          <gmd:role>
            <gmd:CI_RoleCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_RoleCode" codeListValue="author">author</gmd:CI_RoleCode>
          </gmd:role>
        </gmd:CI_ResponsibleParty>
      </gmd:contact>
    </gmd:MD_MaintenanceInformation>
  </gmd:metadataMaintenance>
</gmd:MD_Metadata>
          </gmd:statement>
        </gmd:LI_Lineage>
      </gmd:lineage>
    </gmd:DQ_DataQuality>
  </gmd:dataQualityInfo>
  <gmd:metadataConstraints>
    <gmd:MD_LegalConstraints>
      <gmd:useLimitation>
        <gco:CharacterString>This metadata record is publicly available.</gco:CharacterString>
      </gmd:useLimitation>
    </gmd:MD_LegalConstraints>
  </gmd:metadataConstraints>
  <gmd:metadataMaintenance>
    <gmd:MD_MaintenanceInformation>
      <gmd:maintenanceAndUpdateFrequency>
        <gmd:MD_MaintenanceFrequencyCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#MD_MaintenanceFrequencyCode" codeListValue="asNeeded">asNeeded</gmd:MD_MaintenanceFrequencyCode>
      </gmd:maintenanceAndUpdateFrequency>
      <gmd:dateOfNextUpdate>
        <gco:Date>2014-03-01</gco:Date>
      </gmd:dateOfNextUpdate>
      <gmd:contact>
        <gmd:CI_ResponsibleParty>
          <gmd:individualName>
            <gco:CharacterString>EROS CENTER,</gco:CharacterString>
          </gmd:individualName>
          <gmd:contactInfo>
            <gmd:CI_Contact>
              <gmd:phone>
                <gmd:CI_Telephone>
                  <gmd:voice>
                    <gco:CharacterString>605-594-6151, 800-252-4547</gco:CharacterString>
                  </gmd:voice>
                  <gmd:facsimile>
                    <gco:CharacterString>605-594-6589</gco:CharacterString>
                  </gmd:facsimile>
                </gmd:CI_Telephone>
              </gmd:phone>
              <gmd:address>
                <gmd:CI_Address>
                  <gmd:deliveryPoint>
                    <gco:CharacterString>LTA Customer Services</gco:CharacterString>
                  </gmd:deliveryPoint>
                  <gmd:deliveryPoint>
                    <gco:CharacterString>U.S. Geological Survey</gco:CharacterString>
                  </gmd:deliveryPoint>
                  <gmd:deliveryPoint>
                    <gco:CharacterString>EROS Center</gco:CharacterString>
                  </gmd:deliveryPoint>
                  <gmd:deliveryPoint>
                    <gco:CharacterString>47914 252nd Street</gco:CharacterString>
                  </gmd:deliveryPoint>
                  <gmd:city>
                    <gco:CharacterString>Sioux Falls</gco:CharacterString>
                  </gmd:city>
                  <gmd:administrativeArea>
                    <gco:CharacterString>SD</gco:CharacterString>
                  </gmd:administrativeArea>
                  <gmd:postalCode>
                    <gco:CharacterString>57198-0001</gco:CharacterString>
                  </gmd:postalCode>
                  <gmd:country>
                    <gco:CharacterString>USA</gco:CharacterString>
                  </gmd:country>
                  <gmd:electronicMailAddress>
                    <gco:CharacterString>lta@usgs.gov</gco:CharacterString>
                  </gmd:electronicMailAddress>
                </gmd:CI_Address>
              </gmd:address>
            </gmd:CI_Contact>
          </gmd:contactInfo>
          <gmd:role>
            <gmd:CI_RoleCode codeList="http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_RoleCode" codeListValue="author">author</gmd:CI_RoleCode>
          </gmd:role>
        </gmd:CI_ResponsibleParty>
      </gmd:contact>
    </gmd:MD_MaintenanceInformation>
  </gmd:metadataMaintenance>
</gmd:MD_Metadata>');
-- collection ogc links
INSERT INTO public.collection_ogclink
("collection_id", "offering", "method", "code", "type", "href")
VALUES(17, 'http://www.opengis.net/spec/owc/1.0/req/atom/wms', 'GET', 'GetCapabilities', 'application/xml', '${BASE_URL}/sentinel2/ows?service=wms&version=1.3.0&request=GetCapabilities');
INSERT INTO public.collection_ogclink
("collection_id", "offering", "method", "code", "type", "href")
VALUES(31, 'http://www.opengis.net/spec/owc/1.0/req/atom/wms', 'GET', 'GetCapabilities', 'application/xml', '${BASE_URL}/landsat8/ows?service=wms&version=1.3.0&request=GetCapabilities');
INSERT INTO public.collection_ogclink
("collection_id", "offering", "method", "code", "type", "href")
VALUES(32, 'http://www.opengis.net/spec/owc/1.0/req/atom/wms', 'GET', 'GetCapabilities', 'application/xml', '${BASE_URL}/sentinel1/ows?service=wms&version=1.3.0&request=GetCapabilities');
-- collection publishing metadata
INSERT into public.collection_layer
("cid", "workspace", "layer", "separateBands", "bands", "browseBands", "heterogeneousCRS", "mosaicCRS", "defaultLayer")
VALUES(17, 'gs', 'sentinel2', true, 'B01,B02,B03,B04,B05,B06,B07,B08,B09,B10,B11,B12', 'B04,B03,B02', true, 'EPSG:4326', true);
INSERT into public.collection_layer
("cid", "workspace", "layer", "separateBands", "bands", "browseBands", "heterogeneousCRS", "mosaicCRS", "defaultLayer")
VALUES(31, 'gs', 'landsat8-SINGLE', false, null, null, true, 'EPSG:4326', true);
INSERT into public.collection_layer
("cid", "workspace", "layer", "separateBands", "bands", "browseBands", "heterogeneousCRS", "mosaicCRS", "defaultLayer")
VALUES(31, 'gs', 'landsat8-SEPARATE', true, 'B01,B02,B03,B04,B05,B06,B07,B08,B09', 'B04,B03,B02', true, 'EPSG:4326', false);