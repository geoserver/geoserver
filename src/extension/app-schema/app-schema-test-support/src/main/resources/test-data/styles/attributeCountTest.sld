<sld:StyledLayerDescriptor xmlns:gml="http://www.opengis.net/gml/3.2" xmlns:gsml="urn:cgi:xmlns:CGI:GeoSciML:2.0" xmlns:ogc="http://www.opengis.net/ogc" xmlns:sld="http://www.opengis.net/sld" xmlns:xlink="http://www.w3.org/1999/xlink" version="1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">
	<sld:NamedLayer>
		<sld:Name>occurrencecount</sld:Name>
		<sld:UserStyle>
			<sld:Name>geologic occurrence filter</sld:Name>
			<sld:Title>geologic occurrence count</sld:Title>
			<sld:Abstract></sld:Abstract>
			<sld:IsDefault>1</sld:IsDefault>
			<sld:FeatureTypeStyle>
				<sld:Rule>
					<sld:Name>1</sld:Name>
					<sld:Title>1 rule</sld:Title>
					<sld:PolygonSymbolizer>
					    <sld:Geometry>
							<ogc:PropertyName>gsml:shape</ogc:PropertyName>
						</sld:Geometry>
						<sld:Fill>
							<sld:CssParameter name="fill">
								<ogc:Function name="Recode">
									<!-- Value to transform -->
									<ogc:Function name="attributeCount">
										<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:occurrence</ogc:PropertyName>
									</ogc:Function>
									<ogc:Literal>1</ogc:Literal>
									<!-- yellow -->
									<ogc:Literal>#ffff00</ogc:Literal>
									<ogc:Literal>2</ogc:Literal>
									<!-- blue -->
									<ogc:Literal>#0000ff</ogc:Literal>
									<ogc:Literal>3</ogc:Literal>
									<!-- red -->
									<ogc:Literal>#ff0000</ogc:Literal>
								</ogc:Function>
							</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
			</sld:FeatureTypeStyle>
		</sld:UserStyle>
	</sld:NamedLayer>
</sld:StyledLayerDescriptor>
