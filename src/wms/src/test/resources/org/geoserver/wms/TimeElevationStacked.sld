<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
  xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" xmlns="http://www.opengis.net/sld"
  xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
>
  <NamedLayer>
    <Name>Default Polygon</Name>
    <UserStyle>
      <FeatureTypeStyle>
        <Rule>
          <PolygonSymbolizer>
            <Fill>
              <CssParameter name="fill">
                <ogc:Function name="Recode">
                  <ogc:PropertyName>id</ogc:PropertyName>
                  <ogc:Literal>0</ogc:Literal>
                  <ogc:Literal>#000000</ogc:Literal>
                  <ogc:Literal>1</ogc:Literal>
                  <ogc:Literal>#FF0000</ogc:Literal>
                  <ogc:Literal>2</ogc:Literal>
                  <ogc:Literal>#00FF00</ogc:Literal>
                  <ogc:Literal>3</ogc:Literal>
                  <ogc:Literal>#0000FF</ogc:Literal>
                </ogc:Function>
              </CssParameter>
            </Fill>
            <Stroke>
              <CssParameter name="stroke">#FFFFFF</CssParameter>
              <CssParameter name="stroke-width">1</CssParameter>
            </Stroke>
          </PolygonSymbolizer>
        </Rule>

      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>

