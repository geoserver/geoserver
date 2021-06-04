<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
                       xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
                       xmlns="http://www.opengis.net/sld"
                       xmlns:ogc="http://www.opengis.net/ogc"
                       xmlns:xlink="http://www.w3.org/1999/xlink"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <NamedLayer>
    <Name>animate variables</Name>
    <UserStyle>
      <Title>animate variables</Title>
      <Abstract>animate variables</Abstract>
      <FeatureTypeStyle>
        <Rule>
          <Name>animate variables</Name>
          <Title>animate variables</Title>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>square</WellKnownName>
                <Fill>
                  <CssParameter name="fill">
                    <ogc:Literal>#000000</ogc:Literal>
                  </CssParameter>
                </Fill>
              </Mark>
              <Size>
                <ogc:Function name="env">
                  <ogc:Literal>size</ogc:Literal>
                  <ogc:Literal>10</ogc:Literal>
                </ogc:Function>
              </Size>
              <Rotation>
                <ogc:Function name="env">
                  <ogc:Literal>rotation</ogc:Literal>
                  <ogc:Literal>0</ogc:Literal>
                </ogc:Function>
              </Rotation>
            </Graphic>
          </PointSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>
