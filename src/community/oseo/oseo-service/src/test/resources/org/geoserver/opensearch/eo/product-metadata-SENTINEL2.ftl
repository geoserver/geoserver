<#ftl output_format="XML" strip_whitespace="true">
<#assign a = model.attributes />
<#assign c = a.collection.rawValue />
<?xml version="1.0" encoding="UTF-8"?>
<opt:EarthObservation xmlns:opt="http://www.opengis.net/opt/2.1" xmlns:gml="http://www.opengis.net/gml/3.2"
  xmlns:eop="http://www.opengis.net/eop/2.1" xmlns:gmlov="http://www.opengis.net/gml" xmlns:om="http://www.opengis.net/om/2.0"
  xmlns:ows="http://www.opengis.net/ows/2.0" xmlns:swe="http://www.opengis.net/swe/1.0" xmlns:wrs="http://www.opengis.net/cat/wrs/1.0"
  xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  gml:id="IDN10020" xsi:schemaLocation="http://www.opengis.net/opt/2.1 http://geo.spacebel.be/opensearch/xsd/opt.xsd"
>
<!-- NOTE: the various gml:id need to be generated -->
  <om:phenomenonTime>
    <gml:TimePeriod gml:id="TPN">   
      <gml:beginPosition>${a.timeStart.rawValue?iso_utc}</gml:beginPosition>
      <gml:endPosition><#if a.timeEnd.rawValue?has_content>${a.timeEnd.rawValue?iso_utc}</#if></gml:endPosition>
    </gml:TimePeriod>
  </om:phenomenonTime>
  <om:resultTime>
    <gml:TimeInstant gml:id="instantIdentifier">
      <gml:timePosition><#if a.availabilityTime.rawValue?has_content>${a.availabilityTime?iso_utc}</#if></gml:timePosition>
    </gml:TimeInstant>
  </om:resultTime>
  <om:procedure>
    <eop:EarthObservationEquipment gml:id="EOEQUIP">
      <eop:platform>
        <eop:Platform>
          <eop:shortName>${c.platform.value}</eop:shortName>
          <eop:serialIdentifier>${c.platformSerialIdentifier.value}</eop:serialIdentifier>
          <eop:orbitType>${c.orbitType.value}</eop:orbitType>
        </eop:Platform>
      </eop:platform>
      <#list c.instrument.rawValue as instrument>
      <eop:instrument>
        <eop:Instrument>
          <eop:shortName>${instrument}</eop:shortName>
          <eop:description>This is a customized property</eop:description>
        </eop:Instrument>
      </eop:instrument>
      </#list>
      <eop:sensor>
        <eop:Sensor>
          <eop:sensorType>${c.sensorType.value}</eop:sensorType>
          <#if a.resolution.value?has_content><eop:resolution uom="m">${a.resolution.value}</eop:resolution></#if>
        </eop:Sensor>
      </eop:sensor>
      <eop:acquisitionParameters>
        <eop:Acquisition>
          <eop:orbitNumber>${a.orbitNumber.value}</eop:orbitNumber>
          <eop:orbitDirection>${a.orbitDirection.value}</eop:orbitDirection>
          <#if a.startTimeFromAscendingNode.value?has_content><eop:startTimeFromAscendingNode uom="s">${a.startTimeFromAscendingNode.value}</eop:startTimeFromAscendingNode></#if>
          <#if a.completionTimeFromAscendingNode.value?has_content><eop:completionTimeFromAscendingNode uom="s">${a.completionTimeFromAscendingNode.value}</eop:completionTimeFromAscendingNode></#if>
          <#if a.illuminationAzimuthAngle.value?has_content><eop:illuminationAzimuthAngle uom="deg">${a.illuminationAzimuthAngle.value}</eop:illuminationAzimuthAngle></#if>
          <#if a.illuminationZenithAngle.value?has_content><eop:illuminationZenithAngle uom="deg">${a.illuminationZenithAngle.value}</eop:illuminationZenithAngle></#if>
          <#if a.illuminationElevationAngle.value?has_content><eop:illuminationElevationAngle uom="deg">${a.illuminationElevationAngle.value}</eop:illuminationElevationAngle></#if>
        </eop:Acquisition>
      </eop:acquisitionParameters>
    </eop:EarthObservationEquipment>
  </om:procedure>
  <om:featureOfInterest>
    <eop:Footprint gml:id="FFOI">
      <eop:multiExtentOf>
        <gml:MultiSurface gml:id="MULSFN10021" srsName="EPSG:4326">
          <gml:surfaceMembers>
            ${gml(a.footprint.rawValue)?no_esc}
          </gml:surfaceMembers>
        </gml:MultiSurface>
      </eop:multiExtentOf>
    </eop:Footprint>
  </om:featureOfInterest>
  <om:result>
    <opt:EarthObservationResult gml:id="EORN10020">
      <eop:browse>
        <eop:BrowseInformation>
          <eop:type>QUICKLOOK</eop:type>
          <eop:referenceSystemIdentifier codeSpace="EPSG">EPSG:4326</eop:referenceSystemIdentifier>
          <eop:fileName>
            <ows:ServiceReference xlink:href="${oseoLink('quicklook', 'uid', a.identifier.value, 'parentId', a.parentIdentifier.value)}">
              <ows:RequestMessage />
            </ows:ServiceReference>
          </eop:fileName>
        </eop:BrowseInformation>
      </eop:browse>
      <#if a.cloudCover.value?has_content><opt:cloudCoverPercentage uom="%">${a.cloudCover.value}</opt:cloudCoverPercentage></#if>
      <#if a.snowCover.value?has_content><opt:snowCoverPercentage uom="%">${a.snowCover.value}</opt:snowCoverPercentage></#if>
    </opt:EarthObservationResult>
  </om:result>
  <eop:metaDataProperty>
    <eop:EarthObservationMetaData>
      <eop:identifier>${a.identifier.value}</eop:identifier>
      <eop:creationDate>${a.creationDate.value}</eop:creationDate>
      <eop:modificationDate>${a.modificationDate.value}</eop:modificationDate>
      <eop:parentIdentifier>${a.parentIdentifier.value}</eop:parentIdentifier>
      <eop:acquisitionType>${a.acquisitionType.value}</eop:acquisitionType>
      <eop:acquisitionSubType codeSpace="">${a.acquisitionSubtype.value}</eop:acquisitionSubType>
      <eop:status>${a.productionStatus.value}</eop:status>
      <eop:productQualityDegradation uom="%">${a.productQualityDegradationStatus.value}</eop:productQualityDegradation>
      <eop:processing>
        <eop:ProcessingInformation>
          <eop:processingCenter>${a.processingCenter.value}</eop:processingCenter>
          <eop:processingDate><#if a.processingDate.rawValue?has_content>${a.processingDate.rawValue?iso_utc}</#if></eop:processingDate>
          <eop:processorName>${a.processorName.value}</eop:processorName>
          <eop:processingMode>${a.processorName.value}</eop:processingMode>
        </eop:ProcessingInformation>
      </eop:processing>
    </eop:EarthObservationMetaData>
  </eop:metaDataProperty>
</opt:EarthObservation>