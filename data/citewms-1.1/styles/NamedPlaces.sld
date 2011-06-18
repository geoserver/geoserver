<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" 
	xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 
	xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" 
	xmlns:xlink="http://www.w3.org/1999/xlink" 
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
	<UserStyle>
		<Name>Default Styler</Name>
		<Title>Default Styler</Title>
		<Abstract></Abstract>
		<FeatureTypeStyle>
			<FeatureTypeName>Feature</FeatureTypeName>
			<Rule>
				<Name>ashton</Name>
				<Title>Ashton</Title>
				<ogc:Filter>
					<ogc:PropertyIsEqualTo>
						<ogc:PropertyName>NAME</ogc:PropertyName>
						<ogc:Literal>Ashton</ogc:Literal>
					</ogc:PropertyIsEqualTo>
				</ogc:Filter>
				<PolygonSymbolizer>
					<Fill>
						<CssParameter name="fill">
							<ogc:Literal>#AAAAAA</ogc:Literal>
						</CssParameter>
					</Fill>
					<Stroke>
						<CssParameter name="stroke">
							<ogc:Literal>#000000</ogc:Literal>
						</CssParameter>
					</Stroke>
				</PolygonSymbolizer>
			</Rule>
			<Rule>
				<Name>goose_island</Name>
				<Title>Goose Island</Title>
				<ogc:Filter>
					<ogc:PropertyIsEqualTo>
						<ogc:PropertyName>NAME</ogc:PropertyName>
						<ogc:Literal>Goose Island</ogc:Literal>
					</ogc:PropertyIsEqualTo>
				</ogc:Filter>
				<PolygonSymbolizer>
					<Fill>
						<CssParameter name="fill">
							<ogc:Literal>#FFFFFF</ogc:Literal>
						</CssParameter>
					</Fill>
					<Stroke>
						<CssParameter name="stroke">
							<ogc:Literal>#000000</ogc:Literal>
						</CssParameter>
					</Stroke>
				</PolygonSymbolizer>
			</Rule>
		</FeatureTypeStyle>
	</UserStyle>
</StyledLayerDescriptor>
