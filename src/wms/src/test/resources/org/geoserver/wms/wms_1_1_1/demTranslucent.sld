<StyledLayerDescriptor version="1.0.0" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
  xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">
  <NamedLayer>
    <Name>paletteTx</Name>
    <UserStyle>
      <Name>paletteTx</Name>
      <Title>Palette with trasparency</Title>
      <Abstract>Classic elevation color progression</Abstract>
      <FeatureTypeStyle>
        <Rule>
          <RasterSymbolizer>
            <Opacity>1.0</Opacity>
            <ColorMap type="interval">
              <ColorMapEntry color="#00ff00" quantity="0" opacity="0.125"/>
              <ColorMapEntry color="#00ff00" quantity="100" label="values" opacity="0.250"/>
              <ColorMapEntry color="#00ff00" quantity="200" label="values" opacity="0.375"/>
              <ColorMapEntry color="#00ff00" quantity="300" label="values" opacity="0.5"/>
              <ColorMapEntry color="#00ff00" quantity="400" label="values" opacity="0.625"/>
              <ColorMapEntry color="#00ff00" quantity="500" label="values" opacity="0.75"/>
              <ColorMapEntry color="#00ff00" quantity="600" label="values" opacity="0.875"/>
              <ColorMapEntry color="#00ff00" quantity="700" label="values" opacity="1"/>
              <!-- The nodata value is at -9999, but there is a fair number of values above 50k that
                   do not make sense either -->
              <ColorMapEntry color="#ffffff" quantity="5000" label="values" opacity="0"/>
            </ColorMap>
          </RasterSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>