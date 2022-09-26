<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
  xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd"
  xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
  xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <NamedLayer>
    <Name>boundary_lines</Name>
    <UserStyle>
      <FeatureTypeStyle>
        <Rule>
          <Title>Boundary</Title>
          <MinScaleDenominator>7.0E7</MinScaleDenominator>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#D3C2A8</CssParameter>
              <CssParameter name="stroke-width">0.5</CssParameter>
              <CssParameter name="stroke-dasharray">2 1</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
        <Rule>
          <Title>Boundary</Title>
          <MaxScaleDenominator>7.0E7</MaxScaleDenominator>
          <MinScaleDenominator>3.5E7</MinScaleDenominator>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#D3C2A8</CssParameter>
              <CssParameter name="stroke-width">0.75</CssParameter>
              <CssParameter name="stroke-dasharray">3 1.5</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
        <Rule>
          <Title>Boundary</Title>
          <MaxScaleDenominator>3.5E7</MaxScaleDenominator>
          <MinScaleDenominator>2.0E7</MinScaleDenominator>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#D3C2A8</CssParameter>
              <CssParameter name="stroke-width">1.00</CssParameter>
              <CssParameter name="stroke-dasharray">4 2</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
        <Rule>
          <Title>Boundary</Title>
          <MaxScaleDenominator>2.0E7</MaxScaleDenominator>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#D3C2A8</CssParameter>
              <CssParameter name="stroke-width">1.50</CssParameter>
              <CssParameter name="stroke-dasharray">6 3</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>