<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
                       xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd"
                       xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
                       xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

  <NamedLayer>
    <Name>triangle</Name>
    <UserStyle>
      <Title>blue triangle</Title>
      <FeatureTypeStyle>
        <Rule>
          <Title>blue triangle</Title>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>qgis://diamond</WellKnownName>
                <Fill>
                  <CssParameter name="fill">#9999FF</CssParameter>
                </Fill>
                <Stroke>
                  <CssParameter name="stroke">#0000FF</CssParameter>
                </Stroke>
              </Mark>
              <Size>12</Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>

      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>