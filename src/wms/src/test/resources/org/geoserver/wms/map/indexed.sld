<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
  xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">
  <NamedLayer>
    <Name>wildareas-v2-human-influence-index-geographic</Name>
    <UserStyle>
      <Name>wildareas-v2-human-influence-index-geographic:default</Name>
      <Title>Human Influence Index v2</Title> 
      <Abstract>The Global Human Influence Index Dataset of the Last of the Wild Project, Version 2, 2005 (LWP-2) is 
      a global dataset of 1-kilometer grid cells, created from nine global data layers covering human population 
      pressure (population density), human land use and infrastructure (built-up areas, nighttime lights, land use/land cover),
      and human access (coastlines, roads, railroads, navigable rivers). The dataset is produced by the Wildlife Conservation 
      Society (WCS) and the Columbia University Center for International Earth Science Information Network (CIESIN) and is 
      available in the Geographic Coordinate system.</Abstract>
      <FeatureTypeStyle>
        <FeatureTypeName>Feature</FeatureTypeName>
        <Rule>
          <RasterSymbolizer>
            <ColorMap type="intervals">
              <ColorMapEntry color="#389E00" quantity="0.1" label="Low: 0" opacity="1" />
              <ColorMapEntry color="#FFFF00" quantity="13" label="" opacity="1" />
              <ColorMapEntry color="#FFAA00" quantity="26" label="" opacity="1" />
              <ColorMapEntry color="#FF0000" quantity="39" label="" opacity="1" />
              <ColorMapEntry color="#A80084" quantity="52" label="" opacity="1" />
              <ColorMapEntry color="#000000" quantity="65" label="High: 65" opacity="1" />
              <ColorMapEntry color="#000000" quantity="256" label="No Data" opacity="0" />   
            </ColorMap>
          </RasterSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>