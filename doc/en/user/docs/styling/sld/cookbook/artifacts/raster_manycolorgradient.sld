<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0" 
    xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 
    xmlns="http://www.opengis.net/sld" 
    xmlns:ogc="http://www.opengis.net/ogc" 
    xmlns:xlink="http://www.w3.org/1999/xlink" 
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <NamedLayer>
    <Name>Many color gradient</Name>
    <UserStyle>
      <Title>SLD Cook Book: Many color gradient</Title>
      <FeatureTypeStyle>
        <Rule>
          <RasterSymbolizer>
            <ColorMap>
              <ColorMapEntry color="#000000" quantity="95" />
              <ColorMapEntry color="#0000FF" quantity="110" />
              <ColorMapEntry color="#00FF00" quantity="135" />
              <ColorMapEntry color="#FF0000" quantity="160" />
              <ColorMapEntry color="#FF00FF" quantity="185" />
              <ColorMapEntry color="#FFFF00" quantity="210" />
              <ColorMapEntry color="#00FFFF" quantity="235" />
              <ColorMapEntry color="#FFFFFF" quantity="256" />
            </ColorMap>
          </RasterSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>
