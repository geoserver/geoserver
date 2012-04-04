<?xml version="1.0" encoding="UTF-8"?>
<sld:StyledLayerDescriptor xmlns:gml="http://www.opengis.net/gml" xmlns:gsml="urn:cgi:xmlns:CGI:GeoSciML:2.0" xmlns:ogc="http://www.opengis.net/ogc" xmlns:sld="http://www.opengis.net/sld" xmlns:xlink="http://www.w3.org/1999/xlink" version="1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">
	<sld:NamedLayer>
		<sld:Name>positional-accuracy</sld:Name>
		<sld:UserStyle>
			<sld:Name>positional-accuracy</sld:Name>
			<sld:Title>Positional Accuracy Theme</sld:Title>
			<sld:Abstract></sld:Abstract>
			<sld:IsDefault>1</sld:IsDefault>
			<sld:FeatureTypeStyle>
			
				<sld:Rule>
					<sld:Name>m</sld:Name>
					<sld:Title>m rule</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:observationMethod/gsml:CGI_TermValue/gsml:value/@codeSpace</ogc:PropertyName>
							<ogc:Literal>http://urn.opengis.net</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
										
					<sld:PolygonSymbolizer>
						<sld:Geometry>
							<ogc:PropertyName>gsml:shape</ogc:PropertyName>
						</sld:Geometry>
						<sld:Fill>
							<sld:CssParameter name="fill">#0000FF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>			-	
			</sld:FeatureTypeStyle>
		</sld:UserStyle>
	</sld:NamedLayer>
</sld:StyledLayerDescriptor>
