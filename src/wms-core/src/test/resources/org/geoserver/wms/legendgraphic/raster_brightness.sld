<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0" 
    xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 
    xmlns="http://www.opengis.net/sld" 
    xmlns:ogc="http://www.opengis.net/ogc" 
    xmlns:xlink="http://www.w3.org/1999/xlink" 
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <NamedLayer>
    <Name>Brightness and contrast</Name>
    <UserStyle>
      <Title>SLD Cook Book: Brightness and contrast</Title>
      <FeatureTypeStyle>
        <Rule>
          <RasterSymbolizer>
            <ContrastEnhancement>
              <Normalize />
              <GammaValue>0.5</GammaValue>
            </ContrastEnhancement>
            <ColorMap>
              <ColorMapEntry color="#008000" quantity="70" />
              <ColorMapEntry color="#663333" quantity="256" />
            </ColorMap>
          </RasterSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>