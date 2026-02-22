<?xml version="1.0" encoding="UTF-8"?>
<sld:StyledLayerDescriptor xmlns:gml="http://www.opengis.net/gml/3.2" xmlns:gsml="urn:cgi:xmlns:CGI:GeoSciML-Core:3.0.0" xmlns:ogc="http://www.opengis.net/ogc" xmlns:sld="http://www.opengis.net/sld" xmlns:xlink="http://www.w3.org/1999/xlink" version="1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">
	<sld:NamedLayer>
		<sld:Name>mappedfeaturebyname</sld:Name>
		<sld:UserStyle>
			<sld:Name>name filter</sld:Name>
			<sld:Title>Name Filter Theme</sld:Title>
			<sld:Abstract></sld:Abstract>
			<sld:IsDefault>1</sld:IsDefault>
			<sld:FeatureTypeStyle>
			
				<sld:Rule>
					<sld:Name>m</sld:Name>
					<sld:Title>m rule</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsLike wildCard="*" singleChar="#" escapeChar="!">
							<ogc:PropertyName>gml:name</ogc:PropertyName>
							<ogc:Literal>M*</ogc:Literal>
						</ogc:PropertyIsLike>
					</ogc:Filter>
										
					<sld:PolygonSymbolizer>
						<sld:Geometry>
							<ogc:PropertyName>gsml:shape</ogc:PropertyName>
						</sld:Geometry>
						<sld:Fill>
							<sld:CssParameter name="fill">#0000FF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>				
			</sld:FeatureTypeStyle>
		</sld:UserStyle>
	</sld:NamedLayer>
</sld:StyledLayerDescriptor>
