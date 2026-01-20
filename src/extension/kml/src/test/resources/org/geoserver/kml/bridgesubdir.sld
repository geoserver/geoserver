<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
    xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
    xmlns="http://www.opengis.net/sld"
    xmlns:ogc="http://www.opengis.net/ogc"
    xmlns:xlink="http://www.w3.org/1999/xlink"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <NamedLayer>
    <Name>Default Point</Name>
    <UserStyle>
      <Title>simple purple square</Title>
      <Abstract>A sample style that just prints out a purple square</Abstract>
      <FeatureTypeStyle>
        <Rule>
          <Name>Rule 1</Name>
          <Title>RedSquare</Title>
          <PointSymbolizer>
            <Graphic>
              <ExternalGraphic>
                <OnlineResource xlink:type="simple" xlink:href="graphics/bridgesubdir.png"/>
                <Format>image/png</Format>
              </ExternalGraphic>
            </Graphic>
          </PointSymbolizer>
        </Rule>
       </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor> 
