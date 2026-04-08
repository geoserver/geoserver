<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" 
	xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 
	xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" 
	xmlns:xlink="http://www.w3.org/1999/xlink" 
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <UserStyle>
        <Name>SymbolSize</Name>
        <Title>Default Styler</Title>
        <Abstract></Abstract>
        <FeatureTypeStyle>                        
            <Rule>
                <Name>Thick</Name>
                <ogc:Filter>
                  <ogc:PropertyIsLessThan>
                    <ogc:PropertyName>pop</ogc:PropertyName>
                    <ogc:Literal>200000</ogc:Literal>
                  </ogc:PropertyIsLessThan>
                </ogc:Filter>
                <PolygonSymbolizer>
                  <Fill>
                    <CssParameter name="fill">#FF0000</CssParameter>
                  </Fill>
                  <Stroke>
                    <CssParameter name="stroke">#000000</CssParameter>
                    <CssParameter name="stroke-width">4</CssParameter>
                  </Stroke>
                </PolygonSymbolizer>
            </Rule>
            <Rule>
              <Name>Thin</Name>
              <ElseFilter/>
              <PolygonSymbolizer>
                <Fill>
                  <CssParameter name="fill">#00FF00</CssParameter>
                </Fill>
                <Stroke>
                  <CssParameter name="stroke">#000000</CssParameter>
                  <CssParameter name="stroke-width">1</CssParameter>
                </Stroke>
              </PolygonSymbolizer>
            </Rule>
        </FeatureTypeStyle>
    </UserStyle>
   </StyledLayerDescriptor>
