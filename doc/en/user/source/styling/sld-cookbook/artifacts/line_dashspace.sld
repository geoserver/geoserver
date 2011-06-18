<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
    xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
    xmlns="http://www.opengis.net/sld"
    xmlns:ogc="http://www.opengis.net/ogc"
    xmlns:xlink="http://www.w3.org/1999/xlink"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <NamedLayer>
    <Name>Dash - symbol and space</Name>
    <UserStyle>
      <Title>SLD Cook Book: Dash/Space line</Title>
      <FeatureTypeStyle>
        <Rule>
          <LineSymbolizer>
            <Stroke>
              <GraphicStroke>
                <Graphic>
                  <Mark>
                    <WellKnownName>circle</WellKnownName>
                    <Stroke>
                      <CssParameter name="stroke">#333333</CssParameter>
                      <CssParameter name="stroke-width">1</CssParameter>
                    </Stroke>
                    <Fill>
                      <CssParameter name="stroke">#666666</CssParameter>  
                    </Fill>
                  </Mark>
                  <Size>4</Size>
                  <CssParameter name="stroke-dasharray">4 6</CssParameter>
                </Graphic>
              </GraphicStroke>
            </Stroke>
          </LineSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>

