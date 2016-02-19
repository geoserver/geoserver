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
  <Name>Blue</Name>
  <Title>A blue linestring style</Title>
  <Abstract>A sample style that uses a filter, printing only the
            lines with a LENGTH property of over 5000.  This will work
            with the default bc_roads layer</Abstract>
    <!-- FeatureTypeStyles describe how to render different features -->
    <!-- a feature type for polygons -->
    <FeatureTypeStyle>
      <Rule>
        <!-- like a linesymbolizer but with a fill too -->
        <Filter>
           <PropertyIsGreaterThan>
		<PropertyName>LENGTH</PropertyName>
		<Literal>5000</Literal>
	   </PropertyIsGreaterThan>
        </Filter>
        <PolygonSymbolizer> 
		<Fill>
			<CssParameter name="fill">
				<ogc:Literal>#4040C0</ogc:Literal>
			</CssParameter>
			<CssParameter name="fill-opacity">
				<ogc:Literal>1.0</ogc:Literal>
			</CssParameter>
		</Fill>
           <Stroke> 
             <CssParameter name="stroke">#0000ff</CssParameter> 
             <CssParameter name="stroke-width">2</CssParameter> 
           </Stroke> 
        </PolygonSymbolizer>
      </Rule>
    </FeatureTypeStyle>
</UserStyle>
</NamedLayer>
</StyledLayerDescriptor>

