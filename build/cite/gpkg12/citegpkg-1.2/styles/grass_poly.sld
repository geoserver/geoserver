<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
  xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">
  <NamedLayer>

    <Name>Grass</Name>

    <UserStyle>
      <Name>grass</Name>
      <Title>Grass fill</Title>
      <Abstract>A style filling polygons with a grass theme coming from a PNG file</Abstract>
      <FeatureTypeStyle>
        <Rule>
          <Name>Grass</Name>
          <Abstract>Grass style that uses a texture</Abstract>
          <PolygonSymbolizer>
            <Fill>
              <GraphicFill>
                <Graphic>
                  <ExternalGraphic>
                    <OnlineResource xlink:type="simple" xlink:href="grass_fill.png" />
                    <Format>image/png</Format>
                  </ExternalGraphic>
                  <Opacity>
                    <ogc:Literal>1.0</ogc:Literal>
                  </Opacity>
                </Graphic>
              </GraphicFill>
            </Fill>

            <Stroke>
              <CssParameter name="stroke">#FF0000</CssParameter>
              <CssParameter name="stroke-width">1</CssParameter>
            </Stroke>
          </PolygonSymbolizer>

        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>