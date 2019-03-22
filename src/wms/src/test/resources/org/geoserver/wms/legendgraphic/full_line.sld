<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
	xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
	xmlns="http://www.opengis.net/sld"
	xmlns:ogc="http://www.opengis.net/ogc"
	xmlns:xlink="http://www.w3.org/1999/xlink"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
	<!-- a Named Layer is the basic building block of an SLD document -->
	<NamedLayer>
		<Name>default_line</Name>
		<UserStyle>
			<!-- Styles can have names, titles and abstracts -->
			<Title>Default Line</Title>
			<Abstract>A sample style that draws a line</Abstract>
			<!-- FeatureTypeStyles describe how to render different features -->
			<!-- A FeatureTypeStyle for rendering lines -->
			<FeatureTypeStyle>
				<Rule>
					<Name>rule1</Name>
					<Title>Blue Line</Title>
					<Abstract>A solid blue line with a 2 pixel width</Abstract>
					<LineSymbolizer>
						<Stroke>
							<CssParameter name="stroke">#0000FF</CssParameter>
							<CssParameter name="stroke-width">2</CssParameter>
						</Stroke>
						<PerpendicularOffset>10</PerpendicularOffset>
					</LineSymbolizer>
					<LineSymbolizer>
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
						<PerpendicularOffset>10</PerpendicularOffset>
					</LineSymbolizer>
					<LineSymbolizer>
						<Stroke>
							<GraphicFill>
								<Graphic>
									<Mark>
										<WellKnownName>circle</WellKnownName>
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
							</GraphicFill>
						</Stroke>
						<PerpendicularOffset>10</PerpendicularOffset>
					</LineSymbolizer>
				</Rule>
			</FeatureTypeStyle>
		</UserStyle>
	</NamedLayer>
</StyledLayerDescriptor>

