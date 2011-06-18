<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
  xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <NamedLayer>
    <Name>Simple Roads</Name>
    <UserStyle>

      <Title>Default Styler for simple road segments</Title>
      <Abstract>Light red line, 2px wide</Abstract>
      <FeatureTypeStyle>
        <Rule>
          <Title>Roads</Title>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">
                <ogc:Literal>#AA3333</ogc:Literal>
              </CssParameter>
              <CssParameter name="stroke-width">
                <ogc:Literal>2</ogc:Literal>
              </CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>