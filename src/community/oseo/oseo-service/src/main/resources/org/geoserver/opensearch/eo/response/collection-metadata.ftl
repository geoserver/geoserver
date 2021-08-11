<#ftl output_format="XML">
<#assign a = model.attributes />
<?xml version="1.0" encoding="UTF-8"?>
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
    <gco:CharacterString>${a.identifier.value}</gco:CharacterString>
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
    <!-- TODO: add information in the collections table? -->
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
    <gco:Date>${a.timeStart.rawValue?iso_utc}</gco:Date>
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
            <gco:CharacterString>${a.identifier.value}</gco:CharacterString>
          </gmd:title>
          <gmd:date>
            <gmd:CI_Date>
              <gmd:date>
                <gco:Date>${a.timeStart.rawValue?iso_utc}</gco:Date>
              </gmd:date>
              <gmd:dateType>
                <gmd:CI_DateTypeCode
                  codeList="http://www.isotc211.org/2005/resources/codeList.xml#CI_DateTypeCode"
                  codeListValue="creation" />s
              </gmd:dateType>
            </gmd:CI_Date>
          </gmd:date>
          <gmd:identifier>
            <gmd:RS_Identifier>
              <gmd:code>
                <gco:CharacterString>${a.identifier.value}</gco:CharacterString>
              </gmd:code>
              <gmd:codeSpace>
                 <!-- TODO: move this codespace to the attribute table? -->
                <gco:CharacterString>http://peps.cnes.fr</gco:CharacterString>
              </gmd:codeSpace>
            </gmd:RS_Identifier>
          </gmd:identifier>
        </gmd:CI_Citation>
      </gmd:citation>
      <gmd:abstract>
        <gco:CharacterString>${a.description.name}</gco:CharacterString>
      </gmd:abstract>
      <gmd:pointOfContact>
        <!-- TODO: add information in the collections table? -->
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
                <gco:Decimal>${minx(a.footprint.rawValue)}</gco:Decimal>
              </gmd:westBoundLongitude>
              <gmd:eastBoundLongitude>
                <gco:Decimal>${maxx(a.footprint.rawValue)}</gco:Decimal>
              </gmd:eastBoundLongitude>
              <gmd:southBoundLatitude>
                <gco:Decimal>${miny(a.footprint.rawValue)}</gco:Decimal>
              </gmd:southBoundLatitude>
              <gmd:northBoundLatitude>
                <gco:Decimal>${maxy(a.footprint.rawValue)}</gco:Decimal>
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
                  <gml:beginPosition>${a.timeStart.rawValue?iso_utc}</gml:beginPosition>
                  <gml:endPosition><#if a.timeEnd.rawValue?has_content>${a.timeEnd.rawValue?iso_utc}</#if></gml:endPosition>
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
                <gmd:URL>${oseoLink('search', 'uid', a.identifier, 'httpAccept', 'application/atom+xml')}
                </gmd:URL>
              </gmd:linkage>
            </gmd:CI_OnlineResource>
          </gmd:onLine>
        </gmd:MD_DigitalTransferOptions>
      </gmd:transferOptions>
    </gmd:MD_Distribution>
  </gmd:distributionInfo>
  <gmi:acquisitionInformation>
    <gmi:MI_AcquisitionInformation>
      <gmi:platform>
        <gmi:MI_Platform>
          <gmi:identifier>
            <gmd:MD_Identifier>
              <gmd:code>
                <gmx:Anchor
                  xlink:href="http://gcmdservices.gsfc.nasa.gov/kms/concept/2ce20983-98b2-40b9-bb0e-a08074fb93b3"
                >${a.platform.value}</gmx:Anchor>
              </gmd:code>
            </gmd:MD_Identifier>
          </gmi:identifier>
          <gmi:description>
            <gco:CharacterString>${a.platform.value}</gco:CharacterString>
          </gmi:description>
          <#list a.instrument.rawValue as instrument>
          <gmi:instrument>
            <gmi:MI_Instrument>
              <gmi:identifier>
                  <gmd:MD_Identifier>
                    <gmd:code>
                      <!-- Not sure what concept code to use here -->
                      <gmx:Anchor
                        xlink:href="http://gcmdservices.gsfc.nasa.gov/kms/concept/2ce20983-98b2-40b9-bb0e-a08074fb93b3"
                      >${instrument}</gmx:Anchor>
                    </gmd:code>
                  </gmd:MD_Identifier>
              </gmi:identifier>
              <gmi:type>
                <gmi:MI_SensorTypeCode codeListValue="${a.sensorType.value}">${a.sensorType.value}</gmi:MI_SensorTypeCode>
              </gmi:type>
            </gmi:MI_Instrument>
          </gmi:instrument>
          </#list>
        </gmi:MI_Platform>
      </gmi:platform>
    </gmi:MI_AcquisitionInformation>
  </gmi:acquisitionInformation>
</gmi:MI_Metadata>