<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0" xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
<NamedLayer>
<Name>A Test Layer</Name>
<title>The title of the layer</title>
<abstract>A styling layer used for the unit tests of sldstyler</abstract>
<UserStyle>
  <Name>population</Name>
  <Title>Population in the United States</Title>
  <Abstract>A sample filter that filters the United States into three categories of population, drawn in different colors</Abstract>
    <FeatureTypeStyle>
     <Rule>
      <Name>r1</Name>
      <PolygonSymbolizer>
        <Fill/>
        <Stroke/>
      </PolygonSymbolizer>
     </Rule>
    </FeatureTypeStyle>
</UserStyle>
</NamedLayer>
</StyledLayerDescriptor>
