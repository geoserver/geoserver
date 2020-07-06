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
				<Name>dirt_road</Name>
				<Title>Dirt Road by Green Forest</Title>
				<ogc:Filter>
					<ogc:PropertyIsEqualTo>
						<ogc:PropertyName>NAME</ogc:PropertyName>
						<ogc:Literal>Dirt Road by Green Forest</ogc:Literal>
					</ogc:PropertyIsEqualTo>
				</ogc:Filter>
				<LineSymbolizer>
					<Stroke>
						<CssParameter name="stroke">
							<ogc:Literal>#C0A000</ogc:Literal>
						</CssParameter>
						<CssParameter name="stroke-width">
							<ogc:Literal>4</ogc:Literal>
						</CssParameter>
					</Stroke>
				</LineSymbolizer>
			</Rule>
			<Rule>
				<Name>route_5</Name>
				<Title>Route 5</Title>
				<ogc:Filter>
					<ogc:PropertyIsEqualTo>
						<ogc:PropertyName>NAME</ogc:PropertyName>
						<ogc:Literal>Route 5</ogc:Literal>
					</ogc:PropertyIsEqualTo>
				</ogc:Filter>
				<LineSymbolizer>
					<Stroke>
						<CssParameter name="stroke">
							<ogc:Literal>#000000</ogc:Literal>
						</CssParameter>
						<CssParameter name="stroke-width">
							<ogc:Literal>4</ogc:Literal>
						</CssParameter>
					</Stroke>
				</LineSymbolizer>
			</Rule>
			<Rule>
				<Name>main_street</Name>
				<Title>Main Street</Title>
				<ogc:Filter>
					<ogc:PropertyIsEqualTo>
						<ogc:PropertyName>NAME</ogc:PropertyName>
						<ogc:Literal>Main Street</ogc:Literal>
					</ogc:PropertyIsEqualTo>
				</ogc:Filter>
				<LineSymbolizer>
					<Stroke>
						<CssParameter name="stroke">
							<ogc:Literal>#E04000</ogc:Literal>
						</CssParameter>
						<CssParameter name="stroke-width">
							<ogc:Literal>4</ogc:Literal>
						</CssParameter>
					</Stroke>
				</LineSymbolizer>
			</Rule>
        </FeatureTypeStyle>
    </UserStyle>
</StyledLayerDescriptor>
