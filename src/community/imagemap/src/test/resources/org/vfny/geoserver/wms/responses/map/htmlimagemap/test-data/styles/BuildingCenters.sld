<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" 	xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 	xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" 	xmlns:xlink="http://www.w3.org/1999/xlink" 	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
	<UserStyle>
		<Name>Default Styler</Name>
		<Title>Default Styler</Title>
		<Abstract/>
		<FeatureTypeStyle>
			<FeatureTypeName>Feature</FeatureTypeName>
			<Rule>
				<Name>title</Name>
				<Title>all</Title>
				<PointSymbolizer>
					<Graphic>
						<Mark>
							<WellKnownName>circle</WellKnownName>
							<Stroke>
								<CssParameter name="stroke">#0000ff</CssParameter>
							</Stroke>
						</Mark>
						<Size>6</Size>
						<Opacity>1</Opacity>
					</Graphic>
				</PointSymbolizer>
				<TextSymbolizer>
					<Label>
						<Function name="strConcat">
							<Literal>ADDRESS: </Literal>
							<PropertyName>ADDRESS</PropertyName>							
						</Function>
					</Label>
				</TextSymbolizer>
			</Rule>
		</FeatureTypeStyle>
	</UserStyle>
</StyledLayerDescriptor>