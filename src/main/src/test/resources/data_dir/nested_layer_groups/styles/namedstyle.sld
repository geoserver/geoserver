<StyledLayerDescriptor version="1.0.0" 
    xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 
    xmlns="http://www.opengis.net/sld" 
    xmlns:ogc="http://www.opengis.net/ogc" 
    xmlns:xlink="http://www.w3.org/1999/xlink" 
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <NamedLayer>
    <Name>topp:layer1</Name>
    <UserStyle>
      <Title>Toner Style</Title>
      <Abstract>A black and white cartographic style inspired by Stamen's Toner</Abstract>
      
      <!-- Background -->
      <FeatureTypeStyle>
        <Rule>
          <Name>background</Name>
          <PolygonSymbolizer>
            <Fill>
              <CssParameter name="fill">#FFFFFF</CssParameter>
            </Fill>
          </PolygonSymbolizer>
        </Rule>
      </FeatureTypeStyle>
   </UserStyle>
 </NamedLayer>
</StyledLayerDescriptor>
