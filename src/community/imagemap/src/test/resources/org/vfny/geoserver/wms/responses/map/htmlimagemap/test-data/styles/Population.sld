<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0" xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
<!-- a named layer is the basic building block of an sld document -->
<NamedLayer>
<Name>A Test Layer</Name>

<!-- with in a layer you have Named Styles -->
<UserStyle>
    <!-- again they have names, titles and abstracts -->
  <Name>population</Name>
  <Title>Population in the United States</Title>
  <Abstract>A sample filter that filters the United States into three 
            categories of population, drawn in different colors</Abstract>
    <FeatureTypeStyle>
      <Rule>
        <Title>2M - 4M</Title>
        <!-- like a linesymbolizer but with a fill too -->
        <ogc:Filter xmlns:gml="http://www.opengis.net/gml">
          <ogc:PropertyIsBetween>
            <ogc:PropertyName>PERSONS</ogc:PropertyName>
            <ogc:LowerBoundary>
              <ogc:Literal>2000000</ogc:Literal>
            </ogc:LowerBoundary>
            <ogc:UpperBoundary>
              <ogc:Literal>4000000</ogc:Literal>
            </ogc:UpperBoundary>
          </ogc:PropertyIsBetween>
        </ogc:Filter>
        <PolygonSymbolizer>
           <Fill>
              <!-- CssParameters allowed are fill (the color) and fill-opacity -->
              <CssParameter name="fill">#FF4D4D</CssParameter>
              <CssParameter name="fill-opacity">0.7</CssParameter>
           </Fill>     
        </PolygonSymbolizer>
      </Rule>
      <Rule>
        <!-- like a linesymbolizer but with a fill too -->
        <Title>&lt; 2M</Title>
        <ogc:Filter xmlns:gml="http://www.opengis.net/gml">
          <ogc:PropertyIsLessThan>
           <ogc:PropertyName>PERSONS</ogc:PropertyName>
           <ogc:Literal>2000000</ogc:Literal>
          </ogc:PropertyIsLessThan>
        </ogc:Filter>
        <PolygonSymbolizer>
           <Fill>
              <!-- CssParameters allowed are fill (the color) and fill-opacity -->
              <CssParameter name="fill">#4DFF4D</CssParameter>
              <CssParameter name="fill-opacity">0.7</CssParameter>
           </Fill>     
        </PolygonSymbolizer>
      </Rule>
      <Rule>
        <Title>&gt; 2M</Title>
        <!-- like a linesymbolizer but with a fill too -->
        <ogc:Filter xmlns:gml="http://www.opengis.net/gml">
          <ogc:PropertyIsGreaterThan>
           <ogc:PropertyName>PERSONS</ogc:PropertyName>
           <ogc:Literal>4000000</ogc:Literal>
          </ogc:PropertyIsGreaterThan>
        </ogc:Filter>
        <PolygonSymbolizer>
           <Fill>
              <!-- CssParameters allowed are fill (the color) and fill-opacity -->
              <CssParameter name="fill">#4D4DFF</CssParameter>
              <CssParameter name="fill-opacity">0.7</CssParameter>
           </Fill>     
        </PolygonSymbolizer>
      </Rule>
      <Rule>
        <Title>Boundary</Title>
        <LineSymbolizer>
           <Stroke/>    
        </LineSymbolizer>
        <TextSymbolizer>
        <Label>
        <ogc:PropertyName>STATE_ABBR</ogc:PropertyName>
        </Label>

        <Font>
        <CssParameter name="font-family">Times New Roman</CssParameter>
        <CssParameter name="font-style">Normal</CssParameter>
        <CssParameter name="font-size">14</CssParameter>
        </Font>
    </TextSymbolizer>
      </Rule>
    </FeatureTypeStyle>
</UserStyle>
</NamedLayer>
</StyledLayerDescriptor>

