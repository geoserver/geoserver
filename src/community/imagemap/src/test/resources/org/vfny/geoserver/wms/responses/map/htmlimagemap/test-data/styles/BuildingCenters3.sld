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
				<MinScaleDenominator>0</MinScaleDenominator>
				<MaxScaleDenominator>50000</MaxScaleDenominator>
				<PointSymbolizer>
					<Graphic>
						<Mark>
							<WellKnownName>circle</WellKnownName>
							<Stroke>
								<CssParameter name="stroke">#0000ff</CssParameter>
							</Stroke>
						</Mark>
						<Size>
							<ogc:Literal>10</ogc:Literal>
						</Size>
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
			<Rule>
				<Name>title</Name>
				<Title>all</Title>
				<MinScaleDenominator>50000</MinScaleDenominator>
				<MaxScaleDenominator>10000000</MaxScaleDenominator>
				<PointSymbolizer>
					<Graphic>
						<Mark>
							<WellKnownName>circle</WellKnownName>
							<Stroke>
								<CssParameter name="stroke">#0000ff</CssParameter>
							</Stroke>
						</Mark>
						<Size>
							<ogc:Literal>20</ogc:Literal>
						</Size>
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