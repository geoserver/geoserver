<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
<!-- a named layer is the basic building block of an sld document -->
<NamedLayer>
<Name>A Test Layer</Name>
<title>The title of the layer</title>
<abstract>
A styling layer used for the unit tests of sldstyler
</abstract>
<!-- with in a layer you have Named Styles -->
<UserStyle>
    <!-- again they have names, titles and abstracts -->
  <Name>population</Name>
  <Title>Population in the United States</Title>
  <Abstract>A sample filter that filters the United States into three 
            categories of population, drawn in different colors</Abstract>
    <FeatureTypeStyle>
      <Rule>
        <!-- like a linesymbolizer but with a fill too -->
        <Filter xmlns:gml="http://www.opengis.net/gml">
          <PropertyIsBetween>
            <PropertyName>PERSONS</PropertyName>
            <LowerBoundary>
              <Literal>2000000</Literal>
            </LowerBoundary>
            <UpperBoundary>
              <Literal>4000000</Literal>
            </UpperBoundary>
          </PropertyIsBetween>
        </Filter>
        <PolygonSymbolizer>
           <Fill>
              <!-- CssParameters allowed are fill (the color) and fill-opacity -->
              <CssParameter name="fill">#FF0000</CssParameter>
           </Fill>     
        </PolygonSymbolizer>
      </Rule>
      <Rule>
        <!-- like a linesymbolizer but with a fill too -->
        <Filter xmlns:gml="http://www.opengis.net/gml">
          <PropertyIsLessThan>
           <PropertyName>PERSONS</PropertyName>
           <Literal>2000000</Literal>
          </PropertyIsLessThan>
        </Filter>
        <PolygonSymbolizer>
           <Fill>
              <!-- CssParameters allowed are fill (the color) and fill-opacity -->
              <CssParameter name="fill">#00FF00</CssParameter>
           </Fill>     
        </PolygonSymbolizer>
      </Rule>
      <Rule>
        <!-- like a linesymbolizer but with a fill too -->
        <Filter xmlns:gml="http://www.opengis.net/gml">
          <PropertyIsGreaterThan>
           <PropertyName>PERSONS</PropertyName>
           <Literal>4000000</Literal>
          </PropertyIsGreaterThan>
        </Filter>
        <PolygonSymbolizer>
           <Fill>
              <!-- CssParameters allowed are fill (the color) and fill-opacity -->
              <CssParameter name="fill">#0000FF</CssParameter>
           </Fill>     
        </PolygonSymbolizer>
      </Rule>
      <Rule>
        <LineSymbolizer>
           <Stroke/>    
        </LineSymbolizer>
      </Rule>
    </FeatureTypeStyle>
</UserStyle>
</NamedLayer>
</StyledLayerDescriptor>

