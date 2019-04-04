<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" 
 xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 
 xmlns="http://www.opengis.net/sld" 
 xmlns:ogc="http://www.opengis.net/ogc" 
 xmlns:xlink="http://www.w3.org/1999/xlink" 
 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <!-- The only significant update in this style is that it now has TWO FeatureTypeStyles -->
  <NamedLayer>
    <Name>test_raster_style</Name>
    <UserStyle>
      <Title>Default Raster</Title>
      <Abstract>A sample style that draws a raster, good for displaying imagery</Abstract>
      <FeatureTypeStyle>
        <Rule>
          <Name>rule1</Name>
          <Title>Opaque Raster</Title>
          <Abstract>A raster with 100% opacity</Abstract>
          <RasterSymbolizer>
            <Opacity>1.0</Opacity>
          </RasterSymbolizer>
        </Rule>
      </FeatureTypeStyle>
      <FeatureTypeStyle>
        <Rule>
          <Name>rule2</Name>
          <Title>Opaque Raster</Title>
          <Abstract>A raster with 100% opacity</Abstract>
          <RasterSymbolizer>
            <Opacity>1.0</Opacity>
          </RasterSymbolizer>
        </Rule>
      </FeatureTypeStyle>      
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>