<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0"
   xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
   xmlns="http://www.opengis.net/sld"
   xmlns:ogc="http://www.opengis.net/ogc"
   xmlns:xlink="http://www.w3.org/1999/xlink"
   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
   <NamedLayer>
      <Name>Default Line</Name>
      <UserStyle>
         <Title>My Style</Title>
         <Abstract>A style</Abstract>
         <FeatureTypeStyle>
            <Rule>
              <PointSymbolizer>
                <Graphic>
                  <Mark>
                    <WellKnownName>arrow</WellKnownName>
                    <Fill>
                      <CssParameter name="fill">#FF0000</CssParameter>
                    </Fill>
                    <Stroke>
                      <CssParameter name="stroke">#000000</CssParameter>
                    </Stroke>
                  </Mark>
                  <Size>48</Size>
                </Graphic>
              </PointSymbolizer>
            </Rule>
         </FeatureTypeStyle>
      </UserStyle>
   </NamedLayer>
</StyledLayerDescriptor>