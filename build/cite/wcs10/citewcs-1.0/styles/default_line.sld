<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
		xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
		xmlns="http://www.opengis.net/sld"
		xmlns:ogc="http://www.opengis.net/ogc"
		xmlns:xlink="http://www.w3.org/1999/xlink"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
		<!-- a named layer is the basic building block of an sld document -->

	<NamedLayer>
		<Name>Default Line</Name>
		<UserStyle>
		    <!-- they have names, titles and abstracts -->

			<Title>Blue Line</Title>
			<Abstract>A sample style that just prints out a blue line</Abstract>
			<!-- FeatureTypeStyles describe how to render different features -->
			<!-- a feature type for lines -->

			<FeatureTypeStyle>
				<!--FeatureTypeName>Feature</FeatureTypeName-->
				<Rule>
					<Name>Rule 1</Name>
					<Title>Blue Line</Title>
					<Abstract>A blue line with a 1 pixel width</Abstract>

					<!-- like a polygonsymbolizer -->
					<LineSymbolizer>
						<Stroke>
							<CssParameter name="stroke">#0000FF</CssParameter>
						</Stroke>
					</LineSymbolizer>
				</Rule>

		    </FeatureTypeStyle>
		</UserStyle>
	</NamedLayer>
</StyledLayerDescriptor>
