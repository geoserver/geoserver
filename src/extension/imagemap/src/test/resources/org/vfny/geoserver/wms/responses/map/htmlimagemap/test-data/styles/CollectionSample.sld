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
				<Name>href</Name>
				<Title>all</Title>				
				<LineSymbolizer>
					<Stroke>						
						<CssParameter name="stroke-width">
							<ogc:Literal>4</ogc:Literal>
						</CssParameter>
					</Stroke>
				</LineSymbolizer>
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
						<PropertyName>DESCRIPTION</PropertyName>
					</Label>
				</TextSymbolizer>
			</Rule>
			
        </FeatureTypeStyle>
    </UserStyle>
</StyledLayerDescriptor>