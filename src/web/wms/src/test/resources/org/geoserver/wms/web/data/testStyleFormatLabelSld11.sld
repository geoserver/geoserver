<StyledLayerDescriptor xmlns="http://www.opengis.net/sld"
                       xmlns:ogc="http://www.opengis.net/ogc"
                       xmlns:se="http://www.opengis.net/se"
                       xmlns:xlink="http://www.w3.org/1999/xlink"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       version="1.1.0"
                       xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd">
	<NamedLayer>
		<se:Name>OCEANSEA_1M:Foundation</se:Name>
		<UserStyle>
			<se:Name>GEOSYM</se:Name>
			<IsDefault>1</IsDefault>
			<se:FeatureTypeStyle>
				<se:FeatureTypeName>Foundation</se:FeatureTypeName>
				<se:Rule>
					<se:Name>main</se:Name>
					<se:PolygonSymbolizer uom="http://www.opengis.net/sld/units/pixel">
						<se:Name>MySymbol</se:Name>
						<se:Description>
							<se:Title>Example Symbol</se:Title>
							<se:Abstract>This is just a simple example.</se:Abstract>
						</se:Description>
						<se:Geometry>
							<ogc:PropertyName>GEOMETRY</ogc:PropertyName>
						</se:Geometry>
						<se:Fill>
							<se:SvgParameter name="fill">#96C3F5</se:SvgParameter>
						</se:Fill>
					</se:PolygonSymbolizer>
				</se:Rule>
			</se:FeatureTypeStyle>
		</UserStyle>
	</NamedLayer>
</StyledLayerDescriptor>