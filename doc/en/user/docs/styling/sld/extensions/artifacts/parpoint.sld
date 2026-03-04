<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
  xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">
  <NamedLayer>
    <UserStyle>
      <Title>Default point</Title>
      <Abstract>A sample style that just prints out a 6px wide red square</Abstract>

      <FeatureTypeStyle>
        <Rule>
          <Title>Red square</Title>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName><ogc:Function name="env">
                      <ogc:Literal>name</ogc:Literal>
                      <ogc:Literal>square</ogc:Literal>
                   </ogc:Function>
                </WellKnownName>
                <Fill>
                  <CssParameter name="fill">
                    #<ogc:Function name="env">
                      <ogc:Literal>color</ogc:Literal>
                      <ogc:Literal>FF0000</ogc:Literal>
                   </ogc:Function>
                  </CssParameter>
                </Fill>
              </Mark>
              <Size>
                 <ogc:Function name="env">
                    <ogc:Literal>size</ogc:Literal>
                    <ogc:Literal>6</ogc:Literal>
                 </ogc:Function>
              </Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>

      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>