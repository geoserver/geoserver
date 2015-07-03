<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" 
xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 
xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" 
xmlns:xlink="http://www.w3.org/1999/xlink" 
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <NamedLayer>
    <Name>test</Name>
    <UserStyle>
      <Name>Default Styler</Name>
      <Title>Default Styler</Title>
      <Abstract></Abstract>
      <FeatureTypeStyle>
        <FeatureTypeName>Feature</FeatureTypeName>
        <Rule>
          <Name>r1</Name>
          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
              <ogc:PropertyName>NAME</ogc:PropertyName>
              <ogc:Literal>Cam Bridge</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>
                  <ogc:Literal>square</ogc:Literal>
                </WellKnownName>
                <Fill/>
                <Stroke/>
              </Mark>
              <Size>6</Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>
        <Rule>
          <ElseFilter/>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>
                  <ogc:Literal>square</ogc:Literal>
                </WellKnownName>
                <Fill/>
                <Stroke/>
              </Mark>
              <Size>30</Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>
