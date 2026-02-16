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
							<CssParameter name="font-size">14</CssParameter>
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
								<CssParameter name="fill">#FFFFFF</CssParameter>
							</Fill>
						</Halo>
						<VendorOption name="labelAllGroup">true</VendorOption>
						<VendorOption name="spaceAround">10</VendorOption>
						<VendorOption name="followLine">true</VendorOption>
						<VendorOption name="autoWrap">50</VendorOption>
					</TextSymbolizer>
				</Rule>
			</FeatureTypeStyle>
		</UserStyle>
	</NamedLayer>
</StyledLayerDescriptor>
