<?xml version="1.0" encoding="UTF-8"?>
<sld:StyledLayerDescriptor xmlns:gml="http://www.opengis.net/gml" xmlns:gsml="urn:cgi:xmlns:CGI:GeoSciML:2.0" xmlns:ogc="http://www.opengis.net/ogc" xmlns:sld="http://www.opengis.net/sld" xmlns:xlink="http://www.w3.org/1999/xlink" version="1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">
	<sld:NamedLayer>
		<sld:Name>outcropcharacter</sld:Name>
		<sld:UserStyle>
			<sld:Name>outcropcharacter</sld:Name>
			<sld:Title>Outcrop Character Theme</sld:Title>
			<sld:Abstract></sld:Abstract>
			<sld:IsDefault>1</sld:IsDefault>
			<sld:FeatureTypeStyle>
				<sld:Rule>
					<sld:Name>xrule</sld:Name>
					<sld:Title>x rule</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:outcropCharacter/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>x</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#0000FF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>yrule</sld:Name>
					<sld:Title>y rule</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:outcropCharacter/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>y</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FF0000</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>zrule</sld:Name>
					<sld:Title>z rule</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:outcropCharacter/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>z</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#00FF00</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>				
			</sld:FeatureTypeStyle>
		</sld:UserStyle>
	</sld:NamedLayer>
</sld:StyledLayerDescriptor>
