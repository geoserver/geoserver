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
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>
                  <ogc:Literal>square</ogc:Literal>
                </WellKnownName>
                <Fill/>
                <Stroke/>
              </Mark>
              <Size><ogc:Div><ogc:PropertyName>FID</ogc:PropertyName><ogc:Literal>5</ogc:Literal></ogc:Div></Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>