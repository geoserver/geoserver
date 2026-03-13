<?xml version="1.0" encoding="UTF-8"?>
<sld:StyledLayerDescriptor xmlns:gml="http://www.opengis.net/gml" xmlns:gsml="urn:cgi:xmlns:CGI:GeoSciML:2.0" xmlns:ogc="http://www.opengis.net/ogc" xmlns:sld="http://www.opengis.net/sld" xmlns:xlink="http://www.w3.org/1999/xlink" version="1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">
	<sld:NamedLayer>
		<sld:Name>geology-age</sld:Name>
		<sld:UserStyle>
			<sld:Name>geology-age</sld:Name>
			<sld:Title>Geological Unit Age Theme</sld:Title>
			<sld:Abstract>The color has been picked from the 2008 version of the IUGS International stratigraphic chart  (International Commission on Stratigraphy : http://www.stratigraphy.org/). The chart is located at http://www.stratigraphy.org/cheu.pdf.  The colors are from http://www.stratigraphy.org/codeu.pdf.  The chart where the CMYK colors are reported is not strickly identical  (some name changes and 1 more unit on the 2008 chart) but the colors seems to be identifcal.  The RGB color has been calculated from the CMYK by using R =  (100 - C) * 2.55 - K * 2.55, G =  (100 - M) * 2.55 - K * 2.55 and B =  (100 - Y) * 2.55 - K * 2.55.  Depending of the device  (plotter versus screen), the colors might not be exactly the same.</sld:Abstract>
			<sld:IsDefault>1</sld:IsDefault>
			<sld:FeatureTypeStyle>
				<sld:Rule>
					<sld:Name>Unknown</sld:Name>
					<sld:Title>Unknown (0 - 0 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Unknown</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFFFFF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Holocene</sld:Name>
					<sld:Title>Holocene (0 - 0.0117 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Holocene</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFF2F2</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Upper Pleistocene</sld:Name>
					<sld:Title>Upper Pleistocene (0.0117 - 0.126 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:UpperPleistocene</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFF2D9</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Ionian</sld:Name>
					<sld:Title>Ionian (0.126 - 0.781 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Ionien</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFF2CC</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Lower Pleistocene</sld:Name>
					<sld:Title>Lower Pleistocene (0.781 - 1.806 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:LowerPleistocene</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFF2BF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Pleistocene</sld:Name>
					<sld:Title>Pleistocene (0.0117 - 1.806 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Pleistocene</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFF2B2</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Gelasian</sld:Name>
					<sld:Title>Gelasian (1.806 - 2.588 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Gelasian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFFFCC</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Piacenzian</sld:Name>
					<sld:Title>Piacenzian (2.588 - 3.6 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Piacenzian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFFFBF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Zanclean</sld:Name>
					<sld:Title>Zanclean (3.6 - 5.332 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Zanclean</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFFFB2</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Pliocene</sld:Name>
					<sld:Title>Pliocene (1.806 - 5.332 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Pliocene</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFFF99</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Quaternary</sld:Name>
					<sld:Title>Quaternary (0 - 2.588 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Quaternary</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFFF80</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Messinian</sld:Name>
					<sld:Title>Messinian (5.332 - 7.256 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Messinian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFFF73</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Tortonian</sld:Name>
					<sld:Title>Tortonian (7.256 - 11.608 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Tortonian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFFF66</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Serravalian</sld:Name>
					<sld:Title>Serravalian (11.608 - 13.82 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Serravalian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFFF59</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Langhian</sld:Name>
					<sld:Title>Langhian (13.82 - 15.97 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Langhian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFFF4C</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Burdigalian</sld:Name>
					<sld:Title>Burdigalian (15.97 - 20.43 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Burdigalian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFFF40</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Aquitanian</sld:Name>
					<sld:Title>Aquitanian (20.43 - 23.03 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Aquitanian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFFF33</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Miocene</sld:Name>
					<sld:Title>Miocene (5.332 - 23.03 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Miocene</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFFF00</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Neogene</sld:Name>
					<sld:Title>Neogene (1.806 - 23.03 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Neogene</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFE600</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Chattian</sld:Name>
					<sld:Title>Chattian (23.03 - 28.4 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Chattian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFE6B2</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Rupelian</sld:Name>
					<sld:Title>Rupelian (28.4 - 33.9 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Rupelian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFD9A6</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Oligocene</sld:Name>
					<sld:Title>Oligocene (23.03 - 33.9 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Oligocene</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFBF8C</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Priabonian</sld:Name>
					<sld:Title>Priabonian (33.9 - 37.2 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Priabonian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFCCB2</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Bartonian</sld:Name>
					<sld:Title>Bartonian (37.2 - 40.4 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Bartonian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFBFA6</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Lutetian</sld:Name>
					<sld:Title>Lutetian (40.4 - 48.6 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Lutetian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFB299</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Ypresian</sld:Name>
					<sld:Title>Ypresian (48.6 - 55.8 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Ypresian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFA68C</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Eocene</sld:Name>
					<sld:Title>Eocene (33.9 - 55.8 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Eocene</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFB280</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Thanetian</sld:Name>
					<sld:Title>Thanetian (55.8 - 58.7 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Thanetian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFBF80</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Selandian</sld:Name>
					<sld:Title>Selandian (58.7 - 61.1 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Selandian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFBF73</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Danian</sld:Name>
					<sld:Title>Danian (61.1 - 65.5 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Danian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFB273</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Paleocene</sld:Name>
					<sld:Title>Paleocene (55.8 - 65.5 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Paleocene</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFA673</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Paleogene</sld:Name>
					<sld:Title>Paleogene (23.03 - 65.5 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Paleogene</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FF9966</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Cenozoic</sld:Name>
					<sld:Title>Cenozoic (0 - 65.5 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Cenozoic</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#F2FF00</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Maastrichtian</sld:Name>
					<sld:Title>Maastrichtian (65.5 - 70.6 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Maastrichtian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#F2FF8C</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Campanian</sld:Name>
					<sld:Title>Campanian (70.6 - 83.5 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Campanian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#E6FF80</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Santonian</sld:Name>
					<sld:Title>Santonian (83.5 - 85.8 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Santonian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#D9FF73</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Coniacian</sld:Name>
					<sld:Title>Coniacian (85.8 - 88.6 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Coniacian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#CCFF66</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Turonian</sld:Name>
					<sld:Title>Turonian (88.6 - 93.6 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Turonian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#BFFF59</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Cenomanian</sld:Name>
					<sld:Title>Cenomanian (93.6 - 99.6 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Cenomanian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#B2FF4C</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Upper Cretaceous</sld:Name>
					<sld:Title>Upper Cretaceous (65.5 - 99.6 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:UpperCretaceous</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#A6FF40</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Albian</sld:Name>
					<sld:Title>Albian (99.6 - 112 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Albian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#CCFF99</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Aptian</sld:Name>
					<sld:Title>Aptian (112 - 125 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Aptian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#BFFF8C</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Barremian</sld:Name>
					<sld:Title>Barremian (125 - 130 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Barremian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#B2FF80</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Hauterivian</sld:Name>
					<sld:Title>Hauterivian (130 - 133.9 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Hauterivian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#A6FF73</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Valanginian</sld:Name>
					<sld:Title>Valanginian (133.9 - 140.2 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Valanginian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#99FF66</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Berriasian</sld:Name>
					<sld:Title>Berriasian (140.2 - 145.5 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Berriasian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#8CFF59</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Lower Cretaceous</sld:Name>
					<sld:Title>Lower Cretaceous (99.6 - 145.5 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:LowerCretaceous</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#8CFF4C</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Cretaceous</sld:Name>
					<sld:Title>Cretaceous (65.5 - 145.5 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Cretaceous</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#80FF40</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Tithonian</sld:Name>
					<sld:Title>Tithonian (145.5 - 150.8 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Tithonian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#D9FFFF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Kimmeridgian</sld:Name>
					<sld:Title>Kimmeridgian (150.8 - 155.6 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Kimmeridgian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#CCFFFF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Oxfordian</sld:Name>
					<sld:Title>Oxfordian (155.6 - 161.2 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Oxfordian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#BFFFFF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Upper Jurassic</sld:Name>
					<sld:Title>Upper Jurassic (145.5 - 161.2 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:UpperJurassic</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#B2FFFF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Callovian</sld:Name>
					<sld:Title>Callovian (161.2 - 164.7 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Callovian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#BFFFF2</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Bathonian</sld:Name>
					<sld:Title>Bathonian (164.7 - 167.7 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Bathonian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#B2FFF2</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Bajocian</sld:Name>
					<sld:Title>Bajocian (167.7 - 171.6 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Bajocian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#A6FFF2</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Aelenian</sld:Name>
					<sld:Title>Aelenian (171.6 - 175.6 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Aelenian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#99FFF2</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Middle Jurassic</sld:Name>
					<sld:Title>Middle Jurassic (161.2 - 175.6 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:MiddleJurassic</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#80FFF2</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Toarcian</sld:Name>
					<sld:Title>Toarcian (175.6 - 183 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Toarcian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#99F2FF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Pliensbachian</sld:Name>
					<sld:Title>Pliensbachian (183 - 189.6 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Pliensbachian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#80F2FF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Sinemurian</sld:Name>
					<sld:Title>Sinemurian (189.6 - 196.5 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Sinemurian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#66F2FF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Hettangian</sld:Name>
					<sld:Title>Hettangian (196.5 - 199.6 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Hettangian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#4CF2FF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Lower Jurassic</sld:Name>
					<sld:Title>Lower Jurassic (175.6 - 199.6 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:LowerJurassic</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#40F2FF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Jurassic</sld:Name>
					<sld:Title>Jurassic (145.5 - 199.6 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Jurassic</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#33F2FF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Rhaetian</sld:Name>
					<sld:Title>Rhaetian (199.6 - 203.6 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Rhaetian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#E6BFFF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Norian</sld:Name>
					<sld:Title>Norian (203.6 - 216.5 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Norian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#D9B2FF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Carnian</sld:Name>
					<sld:Title>Carnian (216.5 - 228.7 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Carnian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#CCA6FF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Upper Triassic</sld:Name>
					<sld:Title>Upper Triassic (199.6 - 228.7 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:UpperTriassic</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#BF99FF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Ladinian</sld:Name>
					<sld:Title>Ladinian (228.7 - 237 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Ladinian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#CC8CFF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Anisian</sld:Name>
					<sld:Title>Anisian (237 - 245.9 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Anisian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#BF80FF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Middle Triassic</sld:Name>
					<sld:Title>Middle Triassic (228.7 - 245.9 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:MiddleTriassic</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#B273FF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Olenekian</sld:Name>
					<sld:Title>Olenekian (245.9 - 249.5 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Olenekian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#B259FF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Induan</sld:Name>
					<sld:Title>Induan (249.5 - 251 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Induan</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#A64CFF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Lower Triassic</sld:Name>
					<sld:Title>Lower Triassic (245.9 - 251 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:LowerTriassic</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#9940FF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Triassic</sld:Name>
					<sld:Title>Triassic (199.6 - 251 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Triassic</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#8033FF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Mesozoic</sld:Name>
					<sld:Title>Mesozoic (65.5 - 251 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Mesozoic</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#66FFE6</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Changhsingian</sld:Name>
					<sld:Title>Changhsingian (251 - 253.8 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Changhsingian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFBFCC</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Wuchiapingian</sld:Name>
					<sld:Title>Wuchiapingian (253.8 - 260.4 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Wuchiapingian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFB2BF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Lopingian</sld:Name>
					<sld:Title>Lopingian (251 - 260.4 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Lopingian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFA6B2</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Capitanian</sld:Name>
					<sld:Title>Capitanian (260.4 - 265.8 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Capitanian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FF99A6</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Wordian</sld:Name>
					<sld:Title>Wordian (265.8 - 268 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Wordian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FF8C99</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Roadian</sld:Name>
					<sld:Title>Roadian (268 - 270.6 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Roadian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FF808C</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Guadalupian</sld:Name>
					<sld:Title>Guadalupian (260.4 - 270.6 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Guadalupian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FF7380</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Kungurian</sld:Name>
					<sld:Title>Kungurian (270.6 - 275.6 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Kungurian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#E68C99</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Artinskian</sld:Name>
					<sld:Title>Artinskian (275.6 - 284.4 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Artinskian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#E6808C</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Sakmarian</sld:Name>
					<sld:Title>Sakmarian (284.4 - 294.6 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Sakmarian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#E67380</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Asselian</sld:Name>
					<sld:Title>Asselian (294.6 - 299 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Asselian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#E66673</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Cisuralian</sld:Name>
					<sld:Title>Cisuralian (270.6 - 299 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Cisuralian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#F25966</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Permian</sld:Name>
					<sld:Title>Permian (251 - 299 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Permian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#F24040</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Gzhelian</sld:Name>
					<sld:Title>Gzhelian (299 - 303.4 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Gzhelian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#CCE6D9</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Kasimovian</sld:Name>
					<sld:Title>Kasimovian (303.4 - 307.2 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Kasimovian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#BFE6D9</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Moscovian</sld:Name>
					<sld:Title>Moscovian (307.2 - 311.7 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Moscovian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#B2E6CC</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Bashkirian</sld:Name>
					<sld:Title>Bashkirian (311.7 - 318.1 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Bashkirian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#99E6CC</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Upper Pennsylvanian</sld:Name>
					<sld:Title>Upper Pennsylvanian (299 - 303.4 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:UpperPennsylvanian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#BFE6CC</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Middle Pennsylvanian</sld:Name>
					<sld:Title>Middle Pennsylvanian (307.2 - 311.7 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:MiddlePennsylvanian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#A6E6CC</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Lower Pennsylvanian</sld:Name>
					<sld:Title>Lower Pennsylvanian (311.7 - 318.1 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:LowerPennsylvanian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#8CE6CC</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Pennsylvanian</sld:Name>
					<sld:Title>Pennsylvanian (299 - 318.1 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Pennsylvanian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#99E6CC</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Serpukhovian</sld:Name>
					<sld:Title>Serpukhovian (318.1 - 328.3 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Serpukhovian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#BFD973</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Visean</sld:Name>
					<sld:Title>Visean (328.3 - 345.3 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Visean</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#A6D973</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Tournaisian</sld:Name>
					<sld:Title>Tournaisian (345.3 - 359.2 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Tournaisian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#8CD973</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Upper Mississippian</sld:Name>
					<sld:Title>Upper Mississippian (318.1 - 328.3 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:UpperMississippian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#B2D973</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Middle Mississippian</sld:Name>
					<sld:Title>Middle Mississippian (328.3 - 345.3 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:MiddleMississippian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#99D973</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Lower Mississippian</sld:Name>
					<sld:Title>Lower Mississippian (345.3 - 359.2 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:LowerMississippian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#80D973</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Mississippian</sld:Name>
					<sld:Title>Mississippian (318.1 - 359.2 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Mississippian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#66BF73</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Carboniferous</sld:Name>
					<sld:Title>Carboniferous (299 - 359.2 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Carboniferous</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#66D9B2</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Famennian</sld:Name>
					<sld:Title>Famennian (359.2 - 374.5 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Famennian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#F2F2CC</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Frasnian</sld:Name>
					<sld:Title>Frasnian (374.5 - 385.3 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Frasnian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#F2F2B2</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Upper Devonian</sld:Name>
					<sld:Title>Upper Devonian (359.2 - 385.3 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:UpperDevonian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#F2E6A6</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Givetian</sld:Name>
					<sld:Title>Givetian (385.3 - 391.8 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Givetian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#F2E68C</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Eifelian</sld:Name>
					<sld:Title>Eifelian (391.8 - 397.5 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Eifelian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#F2D980</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Middle Devonian</sld:Name>
					<sld:Title>Middle Devonian (385.3 - 397.5 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:MiddleDevonian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#F2CC73</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Emsian</sld:Name>
					<sld:Title>Emsian (397.5 - 407 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Emsian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#E6D980</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Pragian</sld:Name>
					<sld:Title>Pragian (407 - 411.2 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Pragian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#E6CC73</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Lochkovian</sld:Name>
					<sld:Title>Lochkovian (411.2 - 416 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Lochkovian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#E6BF66</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Lower Devonian</sld:Name>
					<sld:Title>Lower Devonian (397.5 - 416 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:LowerDevonian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#E6B259</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Devonian</sld:Name>
					<sld:Title>Devonian (359.2 - 416 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Devonian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#CC9940</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Pridoli</sld:Name>
					<sld:Title>Pridoli (416 - 418.7 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Pridoli</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#E6FFE6</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Ludfordian</sld:Name>
					<sld:Title>Ludfordian (418.7 - 421.3 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Ludfordian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#D9FFE6</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Gorstian</sld:Name>
					<sld:Title>Gorstian (421.3 - 422.9 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Gorstian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#CCFFE6</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Ludlow</sld:Name>
					<sld:Title>Ludlow (418.7 - 422.9 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Ludlow</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#BFFFD9</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Homerian</sld:Name>
					<sld:Title>Homerian (422.9 - 426.2 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Homerian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#CCFFD9</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Sheinwoodian</sld:Name>
					<sld:Title>Sheinwoodian (426.2 - 428.2 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Sheinwoodian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#BFFFCC</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Wenlock</sld:Name>
					<sld:Title>Wenlock (422.9 - 428.2 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Wenlock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#B2FFCC</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Telychian</sld:Name>
					<sld:Title>Telychian (428.2 - 436 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Telychian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#BFFFD9</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Aeronian</sld:Name>
					<sld:Title>Aeronian (436 - 439 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Aeronian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#B2FFCC</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Rhuddanian</sld:Name>
					<sld:Title>Rhuddanian (439 - 443.7 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Rhuddanian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#BFFFBF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Llandovery</sld:Name>
					<sld:Title>Llandovery (428.2 - 443.7 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Llandovery</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#99FFBF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Silurian</sld:Name>
					<sld:Title>Silurian (416 - 443.7 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Silurian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#B2FFBF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Hirnantian</sld:Name>
					<sld:Title>Hirnantian (443.7 - 445.6 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Hirnantian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#A6FFB2</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Katian</sld:Name>
					<sld:Title>Katian (445.6 - 455.8 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Katian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#99FFA6</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Sandbian</sld:Name>
					<sld:Title>Sandbian (455.8 - 460.9 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Sandbian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#8CFF99</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Upper Ordovician</sld:Name>
					<sld:Title>Upper Ordovician (443.7 - 460.9 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:UpperOrdovician</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#80FF99</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Darriwilian</sld:Name>
					<sld:Title>Darriwilian (460.9 - 468.1 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Darriwilian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#73FFA6</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Darpingian</sld:Name>
					<sld:Title>Darpingian (468.1 - 471.8 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Darpingian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#66FF99</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Middle Ordovician</sld:Name>
					<sld:Title>Middle Ordovician (460.9 - 471.8 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:MiddleOrdovician</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#4CFF80</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Floian</sld:Name>
					<sld:Title>Floian (471.8 - 478.6 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Floian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#40FF8C</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Tremadocian</sld:Name>
					<sld:Title>Tremadocian (478.6 - 488.3 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Tremadocian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#33FF80</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Lower Ordovician</sld:Name>
					<sld:Title>Lower Ordovician (471.8 - 488.3 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:LowerOrdovician</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#1AFF66</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Ordovician</sld:Name>
					<sld:Title>Ordovician (443.7 - 488.3 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Ordovician</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#00FF66</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Cambrian Stage 10</sld:Name>
					<sld:Title>Cambrian Stage 10 (488.3 - 492 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Stage10</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#E6FFCC</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Cambrian Stage 9</sld:Name>
					<sld:Title>Cambrian Stage 9 (492 - 496 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Stage9</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#D9FFBF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Paibian</sld:Name>
					<sld:Title>Paibian (496 - 499 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Paibian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#CCFFB2</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Furongian</sld:Name>
					<sld:Title>Furongian (488.3 - 499 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Furongian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#B2FF99</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Guzhangian</sld:Name>
					<sld:Title>Guzhangian (499 - 503 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Guzhangian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#CCF2B2</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Drumian</sld:Name>
					<sld:Title>Drumian (503 - 506.5 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Drumian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#BFF2A6</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Cambrian Stage 5</sld:Name>
					<sld:Title>Cambrian Stage 5 (506.5 - 510 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Stage5</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#B2F299</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Cambrian Series 3</sld:Name>
					<sld:Title>Cambrian Series 3 (499 - 510 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Series3</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#A6F28C</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Cambrian Stage 4</sld:Name>
					<sld:Title>Cambrian Stage 4 (510 - 515 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Stage4</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#B2E699</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Cambrian Stage 3</sld:Name>
					<sld:Title>Cambrian Stage 3 (515 - 521 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Stage3</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#A6E68C</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Cambrian Series 2</sld:Name>
					<sld:Title>Cambrian Series 2 (510 - 521 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Series2</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#99E680</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Cambrian Stage 2</sld:Name>
					<sld:Title>Cambrian Stage 2 (521 - 528 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Stage2</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#A6D98C</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Fortunian</sld:Name>
					<sld:Title>Fortunian (528 - 542 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Fortunian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#99D980</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Terreneuvian</sld:Name>
					<sld:Title>Terreneuvian (521 - 542 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Terreneuvian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#8CD973</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Cambrian</sld:Name>
					<sld:Title>Cambrian (488.3 - 542 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Cambrian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#80CC59</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Paleozoic</sld:Name>
					<sld:Title>Paleozoic (359.2 - 542 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Paleozoic</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#99E699</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Phanerozoic</sld:Name>
					<sld:Title>Phanerozoic (0 - 542 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Phanerozoic</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#99FFF2</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Ediacaran</sld:Name>
					<sld:Title>Ediacaran (542 - 635 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Ediacaran</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFD973</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Cryogenian</sld:Name>
					<sld:Title>Cryogenian (635 - 850 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Cryogenian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFCC66</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Tonian</sld:Name>
					<sld:Title>Tonian (850 - 1000 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Tonian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFBF59</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Neoproterozoic</sld:Name>
					<sld:Title>Neoproterozoic (542 - 1000 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Neoproterozoic</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFB24C</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Stenian</sld:Name>
					<sld:Title>Stenian (1000 - 1200 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Stenian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFD9A6</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Ectasian</sld:Name>
					<sld:Title>Ectasian (1200 - 1400 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Ectasian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFCC99</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Calymmian</sld:Name>
					<sld:Title>Calymmian (1400 - 1600 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Calymmian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFBF8C</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Mesoproterozoic</sld:Name>
					<sld:Title>Mesoproterozoic (1000 - 1600 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Mesoproterozoic</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFB273</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Statherian</sld:Name>
					<sld:Title>Statherian (1600 - 1800 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Statherian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FF73E6</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Orosirian</sld:Name>
					<sld:Title>Orosirian (1800 - 2050 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Orosirian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FF66D9</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Rhyacian</sld:Name>
					<sld:Title>Rhyacian (2050 - 2300 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Rhyacian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FF59CC</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Siderian</sld:Name>
					<sld:Title>Siderian (2300 - 2500 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Siderian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FF4CBF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Paleoproterozoic</sld:Name>
					<sld:Title>Paleoproterozoic (1600 - 2500 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Paleoproterozoic</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FF40B2</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Proterozoic</sld:Name>
					<sld:Title>Proterozoic (542 - 2500 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Proterozoic</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FF33A6</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Neoarchean</sld:Name>
					<sld:Title>Neoarchean (2500 - 2800 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Neoarchean</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FF99F2</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Mesoarchean</sld:Name>
					<sld:Title>Mesoarchean (2800 - 3200 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Mesoarchean</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FF66F2</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Paleoarchean</sld:Name>
					<sld:Title>Paleoarchean (3200 - 3600 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Paleoarchean</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FF40FF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Eoarchean</sld:Name>
					<sld:Title>Eoarchean (3600 - 4000 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Eoarchean</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#E600FF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Archean</sld:Name>
					<sld:Title>Archean (2500 - 4000 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Archean</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FF00FF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Hadean</sld:Name>
					<sld:Title>Hadean (4000 - 5600 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Hadean</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#B20073</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Precambrian</sld:Name>
					<sld:Title>Precambrian (542 - 4000 Ma)</sld:Title>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:preferredAge/gsml:GeologicEvent/gsml:eventAge/gsml:CGI_TermRange/gsml:upper/gsml:CGI_TermValue/gsml:value</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:ICS:StratChart:2008:Precambrian</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FF40B2</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
			</sld:FeatureTypeStyle>
		</sld:UserStyle>
	</sld:NamedLayer>
</sld:StyledLayerDescriptor>
