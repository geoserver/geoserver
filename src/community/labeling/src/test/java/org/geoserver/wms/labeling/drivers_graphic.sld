<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
  xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd"
  xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
  xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

  <NamedLayer>
    <Name>label</Name>
    <UserStyle>
      <Title>yellow square point style</Title>
      <FeatureTypeStyle>
        <Rule>
          <Title>yellow point</Title>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>square</WellKnownName>
                <Fill>
                  <CssParameter name="fill">#ffff00</CssParameter>
                </Fill>
              </Mark>
              <Size>6</Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>
        <Rule>
          <PointSymbolizer>
            <Graphic>
                <ExternalGraphic>
                <OnlineResource xlink:type="simple" xlink:href="http://geoserver.org?fontSize=18;titleColorCode=967826;valueColorCode=651b96;titleFontName=SansSerif;valueFontName=Monospaced;roundCornerRadius=25;margin=14" />
                <Format>geoserver/label</Format>
              </ExternalGraphic>
              <Displacement>
                <DisplacementX>0</DisplacementX>
                <DisplacementY>-7</DisplacementY>
              </Displacement>
            </Graphic>
          </PointSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>