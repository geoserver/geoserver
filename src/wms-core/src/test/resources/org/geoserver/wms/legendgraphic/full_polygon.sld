<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
	xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd"
	xmlns="http://www.opengis.net/sld"
	xmlns:ogc="http://www.opengis.net/ogc"
	xmlns:xlink="http://www.w3.org/1999/xlink"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

	<NamedLayer>
		<Name></Name>
		<UserStyle>
			<Title>A cyan polygon style</Title>
			<FeatureTypeStyle>
				<Rule>
					<Title>cyan polygon</Title>
					<PolygonSymbolizer>
						<Fill>
							<CssParameter name="fill">#0099cc
							</CssParameter>
						</Fill>
						<Stroke>
							<CssParameter name="stroke">#000000</CssParameter>
							<CssParameter name="stroke-width">0.5</CssParameter>
						</Stroke>
					</PolygonSymbolizer>
					<PolygonSymbolizer>
						<Fill>
							<GraphicFill>
								<Graphic>
									<Mark>
										<WellKnownName>circle</WellKnownName>
										<Fill>
											<CssParameter name="fill">#ffffff</CssParameter>
										</Fill>
									</Mark>
									<Size>4</Size>
									<Rotation>
										<ogc:Mul>
											<ogc:PropertyName>rotation</ogc:PropertyName>
											<ogc:Literal>-1</ogc:Literal>
										</ogc:Mul>
									</Rotation>
									<Opacity>0.4</Opacity>
								</Graphic>
							</GraphicFill>
						</Fill>
						<Stroke>
							<GraphicStroke>
								<Graphic>
									<Mark>
										<WellKnownName>square</WellKnownName>
										<Fill>
											<CssParameter name="fill">#ffff00</CssParameter>
										</Fill>
									</Mark>
									<Size>6</Size>
									<Rotation>
										<ogc:Mul>
											<ogc:PropertyName>rotation</ogc:PropertyName>
											<ogc:Literal>-1</ogc:Literal>
										</ogc:Mul>
									</Rotation>
									<Opacity>0.4</Opacity>
								</Graphic>
							</GraphicStroke>
						</Stroke>
					</PolygonSymbolizer>
				</Rule>

			</FeatureTypeStyle>
		</UserStyle>
	</NamedLayer>
</StyledLayerDescriptor>
