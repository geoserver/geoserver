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
				</sld:Rule>		
				
				<sld:Rule>
					<sld:Name>m</sld:Name>
					<sld:Title>m rule 2</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:observationMethod/gsml:CGI_TermValue/gsml:value/@codeSpace</ogc:PropertyName>
							<ogc:Literal>a</ogc:Literal>
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
				</sld:Rule>	
				
				<sld:Rule>
					<sld:Name>m</sld:Name>
					<sld:Title>m rule 3</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:observationMethod/gsml:CGI_TermValue/gsml:value/@codeSpace</ogc:PropertyName>
							<ogc:Literal>b</ogc:Literal>
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
				</sld:Rule>	
				
				<sld:Rule>
					<sld:Name>m</sld:Name>
					<sld:Title>m rule 4</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:observationMethod/gsml:CGI_TermValue/gsml:value/@codeSpace</ogc:PropertyName>
							<ogc:Literal>c</ogc:Literal>
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
				</sld:Rule>	
				
				<sld:Rule>
					<sld:Name>m</sld:Name>
					<sld:Title>m rule 5</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:observationMethod/gsml:CGI_TermValue/gsml:value/@codeSpace</ogc:PropertyName>
							<ogc:Literal>d</ogc:Literal>
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
				</sld:Rule>	
				
				
				<sld:Rule>
					<sld:Name>m</sld:Name>
					<sld:Title>m rule 6</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:observationMethod/gsml:CGI_TermValue/gsml:value/@codeSpace</ogc:PropertyName>
							<ogc:Literal>e</ogc:Literal>
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
				</sld:Rule>	
				
				<sld:Rule>
					<sld:Name>m</sld:Name>
					<sld:Title>m rule 7</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:observationMethod/gsml:CGI_TermValue/gsml:value/@codeSpace</ogc:PropertyName>
							<ogc:Literal>f</ogc:Literal>
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
				</sld:Rule>	
				
				<sld:Rule>
					<sld:Name>m</sld:Name>
					<sld:Title>m rule 8</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:observationMethod/gsml:CGI_TermValue/gsml:value/@codeSpace</ogc:PropertyName>
							<ogc:Literal>g</ogc:Literal>
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
				</sld:Rule>	
				
				<sld:Rule>
					<sld:Name>m</sld:Name>
					<sld:Title>m rule 9</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:observationMethod/gsml:CGI_TermValue/gsml:value/@codeSpace</ogc:PropertyName>
							<ogc:Literal>h</ogc:Literal>
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
				</sld:Rule>	
				
				<sld:Rule>
					<sld:Name>m</sld:Name>
					<sld:Title>m rule 10</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:observationMethod/gsml:CGI_TermValue/gsml:value/@codeSpace</ogc:PropertyName>
							<ogc:Literal>i</ogc:Literal>
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
				</sld:Rule>	
				
				<sld:Rule>
					<sld:Name>m</sld:Name>
					<sld:Title>m rule 11</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:observationMethod/gsml:CGI_TermValue/gsml:value/@codeSpace</ogc:PropertyName>
							<ogc:Literal>j</ogc:Literal>
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
				</sld:Rule>		
				
				<sld:Rule>
					<sld:Name>m</sld:Name>
					<sld:Title>m rule 12</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:observationMethod/gsml:CGI_TermValue/gsml:value/@codeSpace</ogc:PropertyName>
							<ogc:Literal>k</ogc:Literal>
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
				</sld:Rule>	
				
				<sld:Rule>
					<sld:Name>m</sld:Name>
					<sld:Title>m rule 13</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:observationMethod/gsml:CGI_TermValue/gsml:value/@codeSpace</ogc:PropertyName>
							<ogc:Literal>l</ogc:Literal>
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
				</sld:Rule>	
				
				<sld:Rule>
					<sld:Name>m</sld:Name>
					<sld:Title>m rule 14</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:observationMethod/gsml:CGI_TermValue/gsml:value/@codeSpace</ogc:PropertyName>
							<ogc:Literal>m</ogc:Literal>
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
				</sld:Rule>	
				
				<sld:Rule>
					<sld:Name>m</sld:Name>
					<sld:Title>m rule 15</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:observationMethod/gsml:CGI_TermValue/gsml:value/@codeSpace</ogc:PropertyName>
							<ogc:Literal>n</ogc:Literal>
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
				</sld:Rule>	
				
				
				<sld:Rule>
					<sld:Name>m</sld:Name>
					<sld:Title>m rule 16</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:observationMethod/gsml:CGI_TermValue/gsml:value/@codeSpace</ogc:PropertyName>
							<ogc:Literal>o</ogc:Literal>
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
				</sld:Rule>	
				
				<sld:Rule>
					<sld:Name>m</sld:Name>
					<sld:Title>m rule 17</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:observationMethod/gsml:CGI_TermValue/gsml:value/@codeSpace</ogc:PropertyName>
							<ogc:Literal>p</ogc:Literal>
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
				</sld:Rule>	
				
				<sld:Rule>
					<sld:Name>m</sld:Name>
					<sld:Title>m rule 18</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:observationMethod/gsml:CGI_TermValue/gsml:value/@codeSpace</ogc:PropertyName>
							<ogc:Literal>q</ogc:Literal>
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
				</sld:Rule>	
				
				<sld:Rule>
					<sld:Name>m</sld:Name>
					<sld:Title>m rule 19</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:observationMethod/gsml:CGI_TermValue/gsml:value/@codeSpace</ogc:PropertyName>
							<ogc:Literal>r</ogc:Literal>
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
				</sld:Rule>	
								
				<sld:Rule>
					<sld:Name>m</sld:Name>
					<sld:Title>m rule 20</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:observationMethod/gsml:CGI_TermValue/gsml:value/@codeSpace</ogc:PropertyName>
							<ogc:Literal>S</ogc:Literal>
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
				</sld:Rule>	
				
				<sld:Rule>
					<sld:Name>m</sld:Name>
					<sld:Title>m rule 21</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:observationMethod/gsml:CGI_TermValue/gsml:value/@codeSpace</ogc:PropertyName>
							<ogc:Literal>t</ogc:Literal>
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
				</sld:Rule>	
				
			</sld:FeatureTypeStyle>
		</sld:UserStyle>
	</sld:NamedLayer>
</sld:StyledLayerDescriptor>
