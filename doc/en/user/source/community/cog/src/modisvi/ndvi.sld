<StyledLayerDescriptor xmlns="http://www.opengis.net/sld" xmlns:sld="http://www.opengis.net/sld" xmlns:gml="http://www.opengis.net/gml" xmlns:ogc="http://www.opengis.net/ogc" version="1.0.0">
  <NamedLayer>
    <Name>Default Styler</Name>
    <UserStyle>
      <Name>ndvi</Name>
      <Title>ndvi</Title>
      <FeatureTypeStyle>
        <Name>name</Name>
        <Rule>
          <RasterSymbolizer>
            <ChannelSelection>
              <GrayChannel>
                <SourceChannelName>1</SourceChannelName>
              </GrayChannel>
            </ChannelSelection>
            <ColorMap>
              <ColorMapEntry color="#000000" quantity="-1"/>
              <ColorMapEntry color="#0000ff" quantity="-0.75"/>
              <ColorMapEntry color="#ff00ff" quantity="-0.25"/>
              <ColorMapEntry color="#ff0000" quantity="0"/>
              <ColorMapEntry color="#ffff00" quantity="0.5"/>
              <ColorMapEntry color="#00ff00" quantity="1"/>
            </ColorMap>
            <ContrastEnhancement/>
          </RasterSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>
