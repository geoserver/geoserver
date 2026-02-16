<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.opengis.net/sld
http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd" version="1.0.0">
  <UserLayer>
    <Name>temperature</Name>
    <UserStyle>
      <FeatureTypeStyle>
        <Rule>
          <RasterSymbolizer>
            <Opacity>1.0</Opacity>
            <ColorMap>
              <ColorMapEntry color="#FFFFFF" quantity="-1" opacity="0"/>
              <ColorMapEntry color="#FFA500" quantity="0"/>
            </ColorMap>
          </RasterSymbolizer>
        </Rule>
      </FeatureTypeStyle>
      <FeatureTypeStyle>
        <Rule>
          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
              <ogc:Function name="isCoverage"></ogc:Function>
                <ogc:Literal>false</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
          <LineSymbolizer>    
            <Stroke>      
              <CssParameter name="stroke">#000000</CssParameter>
              <CssParameter name="stroke-width">20</CssParameter>
            </Stroke>
          </LineSymbolizer>          
        </Rule>
      </FeatureTypeStyle>      
    </UserStyle>
  </UserLayer>
</StyledLayerDescriptor>