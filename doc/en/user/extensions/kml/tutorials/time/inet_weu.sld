<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0" xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
<!-- a named layer is the basic building block of an sld document -->
 <NamedLayer>
  <Name>Internet Users</Name>
  <!-- with in a layer you have Named Styles -->
  <UserStyle>
   <Name>Internet Users</Name>
   <Title>Internet Users Per 100 People</Title>
   <FeatureTypeStyle>
    <Rule>
     <Title>n/a</Title>  
     <ogc:Filter xmlns:gml="http://www.opengis.net/gml">
      <ogc:PropertyIsLessThan>
       <ogc:PropertyName>INET_P100</ogc:PropertyName>
       <ogc:Literal>0</ogc:Literal>
      </ogc:PropertyIsLessThan>
     </ogc:Filter>
     <PolygonSymbolizer>
      <Fill>
       <CssParameter name="fill">#C1C1C1</CssParameter>
       <CssParameter name="fill-opacity">0.5</CssParameter>
      </Fill>
     </PolygonSymbolizer>
    </Rule>
    <Rule>
     <Title>0 - 10</Title>  
     <ogc:Filter xmlns:gml="http://www.opengis.net/gml">
      <ogc:PropertyIsBetween>
       <ogc:PropertyName>INET_P100</ogc:PropertyName>
       <ogc:LowerBoundary>
        <ogc:Literal>0</ogc:Literal>
       </ogc:LowerBoundary>
       <ogc:UpperBoundary>
        <ogc:Literal>10</ogc:Literal>
       </ogc:UpperBoundary>
      </ogc:PropertyIsBetween>
     </ogc:Filter>
     <PolygonSymbolizer>
      <Fill>
       <CssParameter name="fill">#FF0000</CssParameter>
       <CssParameter name="fill-opacity">0.7</CssParameter>
      </Fill>
     </PolygonSymbolizer>
    </Rule>
    <Rule>
     <Title>10 - 20</Title>  
     <ogc:Filter xmlns:gml="http://www.opengis.net/gml">
      <ogc:PropertyIsBetween>
       <ogc:PropertyName>INET_P100</ogc:PropertyName>
       <ogc:LowerBoundary>
        <ogc:Literal>10</ogc:Literal>
       </ogc:LowerBoundary>
       <ogc:UpperBoundary>
        <ogc:Literal>20</ogc:Literal>
       </ogc:UpperBoundary>
      </ogc:PropertyIsBetween>
     </ogc:Filter>
     <PolygonSymbolizer>
      <Fill>
       <CssParameter name="fill">#00FF00</CssParameter>
       <CssParameter name="fill-opacity">0.7</CssParameter>
      </Fill>
     </PolygonSymbolizer>
    </Rule>
    <Rule> 
     <Title>20 - 30</Title>
     <ogc:Filter xmlns:gml="http://www.opengis.net/gml">
      <ogc:PropertyIsBetween>
       <ogc:PropertyName>INET_P100</ogc:PropertyName>
       <ogc:LowerBoundary>
        <ogc:Literal>20</ogc:Literal>
       </ogc:LowerBoundary>
       <ogc:UpperBoundary>
        <ogc:Literal>30</ogc:Literal>
       </ogc:UpperBoundary>
      </ogc:PropertyIsBetween>
     </ogc:Filter>
     <PolygonSymbolizer>
      <Fill>
       <CssParameter name="fill">#0000FF</CssParameter>
       <CssParameter name="fill-opacity">0.7</CssParameter>
      </Fill>
     </PolygonSymbolizer>
    </Rule>
    <Rule> 
     <Title>30 - 40</Title>
     <ogc:Filter xmlns:gml="http://www.opengis.net/gml">
      <ogc:PropertyIsBetween>
       <ogc:PropertyName>INET_P100</ogc:PropertyName>
       <ogc:LowerBoundary>
        <ogc:Literal>30</ogc:Literal>
       </ogc:LowerBoundary>
       <ogc:UpperBoundary>
        <ogc:Literal>40</ogc:Literal>
       </ogc:UpperBoundary>
      </ogc:PropertyIsBetween>
     </ogc:Filter>
     <PolygonSymbolizer>
      <Fill>
       <CssParameter name="fill">#FF00FF</CssParameter>
       <CssParameter name="fill-opacity">0.7</CssParameter>
      </Fill>
     </PolygonSymbolizer>
    </Rule>
    <Rule>
     <Title>40 - 50</Title>
     <ogc:Filter xmlns:gml="http://www.opengis.net/gml">
      <ogc:PropertyIsBetween>
       <ogc:PropertyName>INET_P100</ogc:PropertyName>
       <ogc:LowerBoundary>
        <ogc:Literal>40</ogc:Literal>
       </ogc:LowerBoundary>
       <ogc:UpperBoundary>
        <ogc:Literal>50</ogc:Literal>
       </ogc:UpperBoundary>
      </ogc:PropertyIsBetween>
     </ogc:Filter>
     <PolygonSymbolizer>
      <Fill>
       <CssParameter name="fill">#FFFF00</CssParameter>
       <CssParameter name="fill-opacity">0.7</CssParameter>
      </Fill>
     </PolygonSymbolizer>
    </Rule>
    <Rule>
     <Title>&gt; 50</Title>
     <ogc:Filter xmlns:gml="http://www.opengis.net/gml">
      <ogc:PropertyIsGreaterThan>
       <ogc:PropertyName>INET_P100</ogc:PropertyName>
       <ogc:Literal>50</ogc:Literal>
      </ogc:PropertyIsGreaterThan>
     </ogc:Filter>
     <PolygonSymbolizer>
      <Fill>
       <CssParameter name="fill">#00FFFF</CssParameter>
       <CssParameter name="fill-opacity">0.7</CssParameter>
      </Fill>
     </PolygonSymbolizer>
    </Rule>
    <Rule>
     <Title>Border</Title>
     <LineSymbolizer>
        <Stroke/>
     </LineSymbolizer>
     <TextSymbolizer>
      <Label>
       <ogc:PropertyName>NAME</ogc:PropertyName>
      </Label>
      <Font>
       <CssParameter name="font-family">Times New Roman</CssParameter>
       <CssParameter name="font-style">Normal</CssParameter>
       <CssParameter name="font-size">14</CssParameter>
      </Font>
      <Fill>
       <CssParameter name="fill">#000000</CssParameter>
      </Fill>
     </TextSymbolizer>
    </Rule>
   </FeatureTypeStyle>
  </UserStyle>
 </NamedLayer>
</StyledLayerDescriptor>
