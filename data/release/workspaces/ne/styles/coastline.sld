<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor xmlns="http://www.opengis.net/sld"
    xmlns:sld="http://www.opengis.net/sld"
    xmlns:ogc="http://www.opengis.net/ogc"
    xmlns:gml="http://www.opengis.net/gml"
    version="1.0.0">
  <NamedLayer>
    <Name>coastline</Name>
    <UserStyle>
      <FeatureTypeStyle>
        <Rule>
          <Title>Coastline</Title>
          <MinScaleDenominator>7.0E7</MinScaleDenominator>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#B3CCD1</CssParameter>
              <CssParameter name="stroke-width">0.5</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
        <Rule>
          <Title>Coastline</Title>
          <MinScaleDenominator>3.5E7</MinScaleDenominator>
          <MaxScaleDenominator>7.0E7</MaxScaleDenominator>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#B3CCD1</CssParameter>
              <CssParameter name="stroke-width">0.75</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
        <Rule>
          <Title>Coastline</Title>
          <MinScaleDenominator>2.0E7</MinScaleDenominator>
          <MaxScaleDenominator>3.5E7</MaxScaleDenominator>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#B3CCD1</CssParameter>
              <CssParameter name="stroke-width">1.00</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
        <Rule>
          <Title>Coastline</Title>
          <MaxScaleDenominator>2.0E7</MaxScaleDenominator>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#B3CCD1</CssParameter>
              <CssParameter name="stroke-width">1.50</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>