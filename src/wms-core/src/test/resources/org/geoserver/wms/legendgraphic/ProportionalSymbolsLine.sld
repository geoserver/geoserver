<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0"
                       xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
                       xmlns="http://www.opengis.net/sld"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <UserStyle>
    <Name>SymbolSize</Name>
    <Title>Default Styler</Title>
    <Abstract></Abstract>
    <FeatureTypeStyle>
      <Rule>
        <Name>Size1</Name>
        <Abstract>Abstract</Abstract>
        <Title>title</Title>
        <LineSymbolizer>
          <Stroke>
            <GraphicStroke>
              <Graphic>
                <Mark>
                  <WellKnownName>circle</WellKnownName>
                  <Fill>
                    <CssParameter name="fill">#FF0000</CssParameter>
                  </Fill>
                  <Stroke>
                    <CssParameter name="stroke">#000000</CssParameter>
                    <CssParameter name="stroke-width">20</CssParameter>
                  </Stroke>
                </Mark>
                <Size>30</Size>
              </Graphic>
            </GraphicStroke>
          </Stroke>
        </LineSymbolizer>
      </Rule>
      <Rule>
        <Name>Size2</Name>
        <Abstract>Abstract</Abstract>
        <Title>title</Title>
        <LineSymbolizer>
          <Stroke>
            <GraphicStroke>
              <Graphic>
                <Mark>
                  <WellKnownName>circle</WellKnownName>
                  <Fill>
                    <CssParameter name="fill">#FF0000</CssParameter>
                  </Fill>
                </Mark>
                <Size>15</Size>
              </Graphic>
            </GraphicStroke>
          </Stroke>
        </LineSymbolizer>
      </Rule>
    </FeatureTypeStyle>
  </UserStyle>
</StyledLayerDescriptor>
