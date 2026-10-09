<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" xmlns="http://www.opengis.net/sld"
                       xmlns:ogc="http://www.opengis.net/ogc"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <NamedLayer>
    <Name>bggreen</Name>
    <UserStyle>
      <Name>bggreen</Name>
      <Background>
        <CssParameter name="fill">#00FF00</CssParameter>
      </Background>
      <FeatureTypeStyle>
        <Rule>
          <PolygonSymbolizer>
            <Fill><CssParameter name="fill">#0000FF</CssParameter></Fill>
          </PolygonSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>
