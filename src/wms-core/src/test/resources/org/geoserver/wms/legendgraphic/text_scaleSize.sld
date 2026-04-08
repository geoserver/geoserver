<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.1.0"
	xmlns="http://www.opengis.net/se"
	xmlns:ogc="http://www.opengis.net/ogc"
	xmlns:xlink="http://www.w3.org/1999/xlink"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xmlns:gml="http://www.opengis.net/gml"
	xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">
	<NamedLayer>
		<Name>USA states population</Name>
		<UserStyle>
			<Name>population</Name>
			<Title>Population in the United States</Title>
			<Abstract>A sample filter that filters the United States into three
				categories of population, drawn in different colors
			</Abstract>
			<FeatureTypeStyle>
				<Rule>
					<Title>Boundary</Title>
					<LineSymbolizer>
						<Stroke>
							<CssParameter name="stroke-width">0.2</CssParameter>
						</Stroke>
					</LineSymbolizer>
					<TextSymbolizer>
						<Label>
							<ogc:PropertyName>STATE_ABBR</ogc:PropertyName>
						</Label>
						<Font>
							<CssParameter name="font-family">
								<ogc:PropertyName>STATE_FONT</ogc:PropertyName>
							</CssParameter>
							<CssParameter name="font-family">Lobster</CssParameter>
							<CssParameter name="font-family">Times New Roman</CssParameter>
							<CssParameter name="font-style">Normal</CssParameter>
							<CssParameter name="font-size">
								<ogc:Function name="Categorize">
									<!-- Value to transform -->
									<ogc:Function name="env">
										<ogc:Literal>wms_scale_denominator</ogc:Literal>
									</ogc:Function>
									<!-- Output values and thresholds -->
									<!-- Ranges: -->
									<!-- [scale <= 300, font 12] -->
									<!-- [scale 300 - 2500, font 10] -->
									<!-- [scale > 2500, font 8] -->
									<ogc:Literal>12</ogc:Literal>
									<ogc:Literal>300</ogc:Literal>
									<ogc:Literal>10</ogc:Literal>
									<ogc:Literal>2500</ogc:Literal>
									<ogc:Literal>8</ogc:Literal>
								</ogc:Function>
							</CssParameter>
						</Font>
						<Font>
							<CssParameter name="font-family">Times New Roman</CssParameter>
							<CssParameter name="font-style">Italic</CssParameter>
							<CssParameter name="font-size">9</CssParameter>
						</Font>
						<LabelPlacement>
							<PointPlacement>
								<AnchorPoint>
									<AnchorPointX>0.5</AnchorPointX>
									<AnchorPointY>0.5</AnchorPointY>
								</AnchorPoint>
							</PointPlacement>
						</LabelPlacement>
						<Halo>
							<Radius>1</Radius>
							<Fill>
								<CssParameter name="stroke-width">0.2</CssParameter>
							</Fill>
						</Halo>
					</TextSymbolizer>
				</Rule>
			</FeatureTypeStyle>
		</UserStyle>
	</NamedLayer>
</StyledLayerDescriptor>