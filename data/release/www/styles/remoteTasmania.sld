<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0" xmlns="http://www.opengis.net/sld"
  xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">

  <UserLayer>
    <Name>RemoteTasmania</Name>
    <RemoteOWS>
      <Service>WFS</Service>
      <OnlineResource xlink:href="http://sigma.openplans.org:8080/geoserver/wfs?" />
    </RemoteOWS>
    <LayerFeatureConstraints>
      <FeatureTypeConstraint>
        <FeatureTypeName>topp:tasmania_state_boundaries</FeatureTypeName>
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
  <UserLayer>
    <Name>RemoteTasmania</Name>
    <RemoteOWS>
      <Service>WFS</Service>
      <OnlineResource xlink:href="http://sigma.openplans.org:8080/geoserver/wfs?" />
    </RemoteOWS>
    <LayerFeatureConstraints>
      <FeatureTypeConstraint>
        <FeatureTypeName>topp:tasmania_roads</FeatureTypeName>
      </FeatureTypeConstraint>
    </LayerFeatureConstraints>
    <UserStyle>
      <Name>DefaultPolygon</Name>
      <FeatureTypeStyle>
        <Rule>
          <LineSymbolizer>
            <Stroke/>
          </LineSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </UserLayer>
</StyledLayerDescriptor>
