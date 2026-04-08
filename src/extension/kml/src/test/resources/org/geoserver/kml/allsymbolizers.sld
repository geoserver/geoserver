<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
    xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
    xmlns="http://www.opengis.net/sld"
    xmlns:ogc="http://www.opengis.net/ogc"
    xmlns:xlink="http://www.w3.org/1999/xlink"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <NamedLayer>
    <Name>All Symbolizers</Name>
    <UserStyle>
      <Title>Style To Exercise Symbolizer Conversion</Title>
      <Abstract>A sample style that includes all types of symbolizer for conversion in KML Transformer
      </Abstract>
      <FeatureTypeStyle>
        <Rule>
          <Name>Rule 1</Name>
          <Title>RedSquare</Title>
      <PointSymbolizer>
		    <Graphic>
			<Mark>
			    <WellKnownName>circle</WellKnownName>
			    <Fill>
				<CssParameter name="fill">#FF0000</CssParameter>
				<CssParameter name="fill-opacity">1.0</CssParameter>
			    </Fill>
			</Mark>
			<Size>11</Size>
		    </Graphic>
		</PointSymbolizer>
        <LineSymbolizer>
          <Stroke>
            <CssParameter name="stroke">
              <ogc:Literal>#003EBA</ogc:Literal>
            </CssParameter>
            <CssParameter name="stroke-width">
              <ogc:Literal>2</ogc:Literal>
            </CssParameter>
          </Stroke>
        </LineSymbolizer>
        <PolygonSymbolizer>
           <Fill>
              <!-- CssParameters allowed are fill (the color) and fill-opacity -->
              <CssParameter name="fill">#FF4D4D</CssParameter>
              <CssParameter name="fill-opacity">0.7</CssParameter>
           </Fill>     
        </PolygonSymbolizer>
        <TextSymbolizer>
		    <Label>
				<ogc:PropertyName>ID</ogc:PropertyName>
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
