<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0" xmlns="http://www.opengis.net/sld"
  xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">

  <UserLayer>
    <Name>LayerWithRemoteOWS</Name>
    <RemoteOWS>
      <Service>WFS</Service>
      <OnlineResource xlink:href="http://demo.opengeo.org/geoserver/wfs?" />
    </RemoteOWS>
    <LayerFeatureConstraints>
      <FeatureTypeConstraint>
        <FeatureTypeName>topp:states</FeatureTypeName>
        <ogc:Filter>
          <ogc:PropertyIsLessThan>
            <ogc:PropertyName>PERSONS</ogc:PropertyName>
            <ogc:Literal>2000000</ogc:Literal>
          </ogc:PropertyIsLessThan>
        </ogc:Filter>
      </FeatureTypeConstraint>
    </LayerFeatureConstraints>
    <UserStyle>
      <Name>DefaultPolygon</Name>
      <FeatureTypeStyle>
        <Rule>
          <PolygonSymbolizer>
            <Fill/>
            <Stroke/>
          </PolygonSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </UserLayer>
</StyledLayerDescriptor>

