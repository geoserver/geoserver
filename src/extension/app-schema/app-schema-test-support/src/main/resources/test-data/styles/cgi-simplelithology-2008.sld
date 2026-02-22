<?xml version="1.0" encoding="UTF-8"?>
<?xml-stylesheet type="text/xsl" href="sld2html.xslt"?>
<sld:StyledLayerDescriptor xmlns:gml="http://www.opengis.net/gml" xmlns:gsml="urn:cgi:xmlns:CGI:GeoSciML:2.0" xmlns:ogc="http://www.opengis.net/ogc" xmlns:sld="http://www.opengis.net/sld" xmlns:xlink="http://www.w3.org/1999/xlink" version="1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">
	<sld:NamedLayer>
		<sld:Name>geology-lithology</sld:Name>
		<sld:UserStyle>
			<sld:Name>geology-lithology</sld:Name>
			<sld:Title>Geological Unit Lithology Theme</sld:Title>
			<sld:Abstract>The colour has been creatively adapted from Moyer,Hasting and Raines, 2005 (http://pubs.usgs.gov/of/2005/1314/of2005-1314.pdf) which provides xls spreadsheets for various color schemes. Most of the colors comes from lithclass 6.1 and 6.2 (see http://www.nadm-geo.org/dmdt/pdf/lithclass61.pdf for lithclass 6.1) plus some creative entries to fill missing entries.  The list of term is from the Vocab_litho_CGI.xml file from the GeoSciML SVN, May 29, 2008.
				This SLD assumes that MappedFeature is the context node</sld:Abstract>
			<sld:IsDefault>1</sld:IsDefault>
			<sld:FeatureTypeStyle>
				<sld:Rule>
					<sld:Name>acidic igneous material</sld:Name>
					<sld:Abstract>Igneous material with more than 63 percent SiO2.  (after LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:acidic_igneous_material</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFCCB3</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>acidic igneous rock</sld:Name>
					<sld:Abstract>Igneous rock with more than 63 percent SiO2.  (after LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:acidic_igneous_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FECDB2</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>amphibolite</sld:Name>
					<sld:Abstract>Metamorphic rock mainly consisting of green, brown or black amphibole and plagioclase (including albite), which combined form 75 percent or more of the rock, and both of which are present as major constituents.  The amphibole constitutes 50 percent or more of the total mafic constituents and is present in an amount of 30 percent or more; other common minerals include quartz, clinopyroxene, garnet, epidote-group minerals, biotite, titanite and scapolite.  (Coutinho et al. 2007, IUGS SCMR chapter 8 (http://www.bgs.ac.uk/SCMR/))</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:amphibolite</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#AC7F50</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>andesitic rock</sld:Name>
					<sld:Abstract>Fine grained crystalline rock, usually porphyritic, consisting of plagioclase (frequently zoned from labradorite to oligoclase), pyroxene, hornblende and/or biotite; color index M less than 35.  Includes rocks defined modally in QAPF fields 9 and 10 or chemically in TAS field O2 as andesite.  Fine grained equivalent of dioritic rock.  (after LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:andesitic_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#B14801</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>anorthositic rock</sld:Name>
					<sld:Abstract>General term for a leucocratic phaneritic crystalline rock consisting essentially of plagioclase, often with small amounts of pyroxene; colour index M less than 10.  Includes rocks defined modally in QAPF field 10 as anorthosite.  (after LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:anorthositic_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFA3B9</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>anthropogenic material</sld:Name>
					<sld:Abstract>Material known to have artificial (human-related) origin; insufficient information to classify in more detail.  (CGI concepts task group)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:anthropogenic_material</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#C0C0C0</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>anthropogenic unconsolidated material</sld:Name>
					<sld:Abstract>Unconsolidated material known to have artificial (human-related) origin.  (NGMDB 2008)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:anthropogenic_unconsolidated_material</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#C8C8C8</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>aphanite</sld:Name>
					<sld:Abstract>Rock that is too fine grained to categorize in more detail.   (NGMDB 2008)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:aphanite</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#CDCDCD</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>aplite</sld:Name>
					<sld:Abstract>Light coloured crystalline rock, characterized by a fine grained allotriomorphic-granular (aplitic, saccharoidal or xenomorphic) texture; typically granitic composition, consisting of quartz, K-feldspar and sodic plagioclase.  (Neuendorf et al. 2005)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:aplite</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFC8BF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Ash and lapilli</sld:Name>
					<sld:Abstract>Tephra in which less than 25 percent of fragments are greater than 64 mm in longest dimension  (Schmid 1981; LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:ash_and_lapilli</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFC8C3</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Ash breccia, bomb, or block tephra</sld:Name>
					<sld:Abstract>Tephra in which more than 25 percent of particles are greater than 64 mm in largest dimension. Includes ash breccia, bomb tephra and block tephra of Gillespie and Styles (1999)  (Schmid 1981; LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:ash_breccia_bomb_or_block_tephra</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFF5D9</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>Ash tuff, lapillistone, and lapilli tuff</sld:Name>
					<sld:Abstract>Pyroclastic rock in which less than 25 percent of rock by volume are more than 64 mm in longest diameter. Includes tuff, lapilli tuff, and lapillistone.  (Schmid 1981; LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:ash_tuff_lapillistone_and_lapilli_tuff</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFF5DF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>basaltic rock</sld:Name>
					<sld:Abstract>Fine-grained or porphyritic igneous rock with less than 20 percent quartz, and less than 10 percent feldspathoid minerals, in which the ratio of plagioclase to alkali feldspar is greater than 66 percent, and the color index is greater than 35. Typically composed of calcic plagioclase and clinopyroxene; phenocrysts typically include one or more of calcic plagioclase, clinopyroxene, orthopyroxene, and olivine.  Includes rocks defined modally in QAPF fields 9 and 10 or chemically in TAS field B as basalt.  (after LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:basaltic_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#DDB397</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>basic igneous material</sld:Name>
					<sld:Abstract>Igneous material with between 45 and 52 percent SiO2.  (after LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:basic_igneous_material</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#E69900</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>basic igneous rock</sld:Name>
					<sld:Abstract>Igneous rock with between 45 and 52 percent SiO2.  (after LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:basic_igneous_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#E69900</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>bauxite</sld:Name>
					<sld:Abstract>Highly aluminous material containing abundant aluminium hydroxides (gibbsite, less commonly boehmite, diaspore) and aluminium-substituted iron oxides or hydroxides and generally minor or negligible kaolin minerals; may contain up to 20 percent quartz.  Commonly has a pisolitic or nodular texture, and may be cemented.  (Eggleton, 2001)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:bauxite</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFFFB7</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>biogenic sediment</sld:Name>
					<sld:Abstract>Sediment composed of greater than 50 percent material of biogenic origin.  (NGMDB 2008)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:biogenic_sediment</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#F7F3A1</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>biogenic silica sedimentary rock</sld:Name>
					<sld:Abstract>Sedimentary rock that consists of at least 50 percent silicate mineral material, deposited directly by biological processes at the depositional surface, or in particles formed by biological processes within the basin of deposition.  (based on NADM SLTT sedimentary; Hallsworth and Knox 1999)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:biogenic_silica_sedimentary_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#F7F3A1</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>boundstone</sld:Name>
					<sld:Abstract>Sedimentary carbonate rock with preserved biogenic texture, whose original components were bound and encrusted together during deposition by the action of plants and animals during deposition, and remained substantially in the position of growth.  (Hallsworth and Knox 1999; SLTTs 2004)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:boundstone</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#E7F6F1</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>breccia</sld:Name>
					<sld:Abstract>Coarse-grained material composed of angular broken rock fragments; the fragments typically have sharp edges and unworn corners.  The fragments may be held together by a mineral cement, or in a fine-grained matrix.  Clasts may be of any composition or origin.  (Neuendorf et al. 2005)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:breccia</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#D7A7AD</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>breccia-gouge series</sld:Name>
					<sld:Abstract>Fault material with features such as void spaces (filled or unfilled), or unconsolidated matrix material between fragments, indicating loss of cohesion during deformation.  Includes fault-related breccia and gouge.  (NGMDB 2008)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:breccia_gouge_series</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#DCAAA0</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>calcareous carbonate sediment</sld:Name>
					<sld:Abstract>Carbonate sediment with a calcite (plus aragonite) to dolomite ratio greater than 1 to 1.  Includes lime-sediments.  (after Hallsworth and Knox 1999)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:calcareous_carbonate_sediment</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#DEEFFE</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>calcareous carbonate sedimentary material</sld:Name>
					<sld:Abstract>Carbonate sedimentary material of unspecified consolidation state with a calcite (plus aragonite) to dolomite ratio greater than 1 to 1.   Includes lime-sediments, limestone and dolomitic limestone.  (after Hallsworth and Knox 1999)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:calcareous_carbonate_sedimentary_material</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#C8E7FA</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>calcareous carbonate sedimentary rock</sld:Name>
					<sld:Abstract>Carbonate sedimentary rock with a calcite (plus aragonite) to dolomite ratio greater than 1 to 1.  Includes limestone and dolomitic limestone.  (NADM SLTT sedimentary; Hallsworth and Knox 1999)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:calcareous_carbonate_sedimentary_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#B2DFF5</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>carbonate gravel</sld:Name>
					<sld:Abstract>Carbonate sediment composed of more than 25 percent gravel-sized clasts (maximum diameter more than 2 mm).  (Hallsworth and Knox 1999)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:carbonate_gravel</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#9CD7F0</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>carbonate mud</sld:Name>
					<sld:Abstract>Carbonate sediment composed of more than 75 percent mud-sized clasts (maximum diameter less than 32 microns).  (Hallsworth and Knox 1999)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:carbonate_mud</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#86CFEB</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>carbonate mudstone</sld:Name>
					<sld:Abstract>Carbonate sedimentary rock with matrix-supported fabric, composed of more than 75 percent mud-sized grains (maximum diameter less than 32 microns).  The original depositional texture is preserved.  (Hallsworth and Knox 1999; NADM SLTT sedimentary)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:carbonate_mudstone</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#70C7E6</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>carbonate sand</sld:Name>
					<sld:Abstract>Carbonate sediment composed of more than 75 percent sand-sized clasts (maximum diameter between 32 microns and 2 mm).  (Hallsworth and Knox 1999)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:carbonate_sand</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#5ABFE1</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>carbonate sediment</sld:Name>
					<sld:Abstract>Sediment in which at least 50 percent of the primary and/or recrystallized constituents are composed of one (or more) of the carbonate minerals calcite, aragonite and dolomite, in particles of intrabasinal origin.  (after NADM SLTT sedimentary rocks)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:carbonate_sediment</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#44B7DC</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>carbonate sedimentary material</sld:Name>
					<sld:Abstract>Sedimentary material in which at least 50 percent of the primary and/or recrystallized constituents are composed of one (or more) of the carbonate minerals calcite, aragonite and dolomite, in particles of intrabasinal origin.  (after NADM SLTT sedimentary rocks)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:carbonate_sedimentary_material</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#2EAFD2</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>carbonate sedimentary rock</sld:Name>
					<sld:Abstract>Sedimentary rock in which at least 50 percent of the primary and/or recrystallized constituents are composed of one (or more) of the carbonate minerals calcite, aragonite and dolomite, in particles of intrabasinal origin.  (after NADM SLTT sedimentary rocks)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:carbonate_sedimentary_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#019CCD</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>carbonate wackestone</sld:Name>
					<sld:Abstract>Carbonate sedimentary rock with discernible mud supported depositional texture and containing greater than 10 percent grains.   ()</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:carbonate_wackestone</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#B7D9CC</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>carbonatite</sld:Name>
					<sld:Abstract>Igneous rock composed of more than 50 percent modal carbonate minerals.  (LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:carbonatite</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#CC3333</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>cataclasite series</sld:Name>
					<sld:Abstract>Fault-related rock that maintained primary cohesion during deformation, with matrix comprising 50 to 90 percent of rock mass; matrix is fine-grained material caused by tectonic grainsize reduction.  Includes cataclasite, protocataclasite and ultracataclasite.  (Sibson, 1977; Scholz, 1990; Snoke and Tullis, 1998;  Barker, 1998 Appendix II; NADM SLTTm, 2004)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:cataclasite_series</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#F4FFD5</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>chemical sediment</sld:Name>
					<sld:Abstract>Sediment that consists of at least 50 percent material produced by chemical (organic or inorganic) processes within the basin of deposition. Includes organic-rich, non-clastic siliceous, carbonate, evaporite, iron-rich, and phosphatic sediment classes.  (NGMDB 2008)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:chemical_sediment</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#CDDEFF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>clastic sediment</sld:Name>
					<sld:Abstract>Sediment in which at least 50 percent of the constituent particles were derived from erosion, weathering, or mass-wasting of pre-existing earth materials, and transported to the place of deposition by mechanical agents such as water, wind, ice and gravity.  (SLTTs, 2004; Neuendorf et al., 2005)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:clastic_sediment</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#D9FDD3</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>clastic sedimentary material</sld:Name>
					<sld:Abstract>Sedimentary material of unspecified consolidation state in which at least 50 percent of the constituent particles were derived from erosion, weathering, or mass-wasting of pre-existing earth materials, and transported to the place of deposition by mechanical agents such as water, wind, ice and gravity.  (SLTTs, 2004; Neuendorf et al., 2005)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:clastic_sedimentary_material</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#D9FDD3</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>clastic sedimentary rock</sld:Name>
					<sld:Abstract>Sedimentary rock in which at least 50 percent of the constituent particles were derived from erosion, weathering, or mass-wasting of pre-existing earth materials, and transported to the place of deposition by mechanical agents such as water, wind, ice and gravity.  (SLTTs, 2004; Neuendorf et al., 2005)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:clastic_sedimentary_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#D9FDD3</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>coal</sld:Name>
					<sld:Abstract>Hard, black, organic rich sedimentary rock that yields greater than 8,300 Btu on a moist, mineral-matter-free basis, or contains greater than 69 percent fixed carbon on a dry, mineral-matter-free basis; formed from the compaction or induration of variously altered plant remains similar to those of peaty deposits.  (ASTM 2002; Schopf 1956)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:coal</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#6E4900</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>composite (transformed) genesis material</sld:Name>
					<sld:Abstract>Material of unspecified consolidation state formed by geological modification of pre-existing materials outside the realm of igneous and sedimentary processes. Includes rocks formed by impact metamorphism, standard dynamothermal metamorphism, brittle deformation, weathering, metasomatism and hydrothermal alteration (diagenesis is a sedimentary process in this context).  (NADM SLTT metamorphic)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:composite_genesis_material</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#6A006A</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>composite (transformed) genesis rock</sld:Name>
					<sld:Abstract>Rock formed by geological modification of pre-existing rocks outside the realm of igneous and sedimentary processes. Includes rocks formed by impact metamorphism, standard dynamothermal metamorphism, brittle deformation, weathering, metasomatism and hydrothermal alteration (diagenesis is a sedimentary process in this context).  (NGMDB 2008)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:composite_genesis_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#5F005F</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>conglomerate</sld:Name>
					<sld:Abstract>Coarse grained sedimentary rock composed of at least 30 percent rounded to subangular fragments larger than 2 mm in diameter; typically contains finer grained material in interstices between larger fragments.  (Neuendorf et al. 2005; NADM SLTT sedimentary)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:conglomerate</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#B7D9CC</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>crystalline carbonate</sld:Name>
					<sld:Abstract>Carbonate rock of indeterminate mineralogy in which diagenetic processes have obliterated any original depositional texture.  (NADM SLTTs 2004)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:crystalline_carbonate</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#0FFFFF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>dacitic rock</sld:Name>
					<sld:Abstract>Fine grained or porphyritic crystalline rock composed of quartz and sodic plagioclase with minor amounts of biotite and/or hornblende and/or pyroxene; volcanic equivalent of granodiorite and tonalite.  Includes rocks defined modally in QAPF fields 4 and 5 or chemically in TAS Field O3.  (LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:dacitic_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FECDAC</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>diamictite</sld:Name>
					<sld:Abstract>Unsorted or poorly sorted, clastic sedimentary rock with a wide range of particle sizes such that it cannot be assigned to any other class.  (Fairbridge and Bourgeois 1978)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:diamictite</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#597D6E</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>diamicton</sld:Name>
					<sld:Abstract>Unsorted or poorly sorted, clastic sediment with a wide range of particle sizes such that it cannot be assigned to any other class.  (Fairbridge and Bourgeois 1978)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:diamicton</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#597D6E</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>diorite</sld:Name>
					<sld:Abstract>Phaneritic crystalline rock consisting of intermediate plagioclase, commonly with hornblende and often with biotite or augite; colour index M less than 90, sodic plagioclase (An0-An50).  Includes rocks defined modally in QAPF field 10 as diorite.  (LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:diorite</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FF3317</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>dioritic rock</sld:Name>
					<sld:Abstract>Phaneritic crystalline rock with M less than 90, consisting of intermediate plagioclase, commonly with hornblende and often with biotite or augite.  Includes rocks defined modally in QAPF fields 9 and 10 as diorite, monzodiorite.  (LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:dioritic_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#DFC8C8</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>doleritic rock</sld:Name>
					<sld:Abstract>Dark colored gabbroic (basaltic) or dioritic (andesitic) rock intermediate in grain size between basalt and gabbro and composed of plagioclase, pyroxene and opaque minerals; often with ophitic texture.  Typically occurs as hypabyssal intrusions.  Includes dolerite, microdiorite, diabase and microgabbro.  (Neuendorf et al 2005; LeMaitre et al 2002; Gillespie and Styles, 1999)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:doleritic_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#F4636B</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>dolomitic or magnesian sedimentary material</sld:Name>
					<sld:Abstract>Carbonate sedimentary material of unspecified consolidation degree with a ratio of magnesium carbonate to calcite (plus aragonite) greater than 1 to 1.  Includes dolomite sediment, dolostone, lime dolostone and magnesite-stone.  (after NADM SLTTs 2004, Hallsworth and Knox 1999)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:dolomitic_or_magnesian_sedimentary_material</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#BFBFFF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>dolomitic or magnesian sedimentary rock</sld:Name>
					<sld:Abstract>Carbonate sedimentary rock with a ratio of magnesium carbonate to calcite (plus aragonite) greater than 1 to 1.  Includes dolostone, lime dolostone and magnesite-stone.  (after NADM SLTTs 2004, Hallsworth and Knox 1999)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:dolomitic_or_magnesian_sedimentary_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#BFBFFF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>dolomitic sediment</sld:Name>
					<sld:Abstract>Carbonate sediment with a ratio of magnesium carbonate to calcite (plus aragonite) greater than 1 to 1.  (after NADM SLTTs 2004, Hallsworth and Knox 1999)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:dolomitic_sediment</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#BFBFFF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>duricrust</sld:Name>
					<sld:Abstract>Rock forming a hard crust or layer at or near the Earth's surface at the time of formation, e.g. in the upper horizons of a soil, characterized by structures indicative of pedogenic origin.  (CGI concepts task group)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:duricrust</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFA252</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>eclogite</sld:Name>
					<sld:Abstract>Metamorphic rock composed of 75 percent or more (by volume) omphacite and garnet, both of which are present as major constituents, the amount of neither of them being higher than 75 percent (by volume); the presence of plagioclase precludes classification as an eclogite.  (IUGS SCMR 2007 (http://www.bgs.ac.uk/SCMR/))</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:eclogite</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FF4FFF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>evaporite</sld:Name>
					<sld:Abstract>Nonclastic sedimentary rock composed of at least 50 percent non-carbonate salts, including chloride, sulfate or borate minerals; formed through precipitation of mineral salts from a saline solution (non-carbonate salt rock).  (Jackson 1997; NADM SLTT sedimentary)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:evaporite</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#9ACEFE</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>exotic alkalic igneous rock</sld:Name>
					<sld:Abstract>Igneous rock containing greater than 10 percent melilite or kalsilite. Typically undersaturated, ultrapotassic (kalsilitic rocks) or calcium-rich (melilitic rocks) mafic or ultramafic rocks.  (NGMDB 2008)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:exotic_alkalic_igneous_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FF6F91</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>exotic alkaline rock</sld:Name>
					<sld:Abstract>Kimberlite, lamproite, or lamprophyre. Generally are potassic, mafic or ultramafic rocks. Olivine (commonly serpentinized in kimberlite), and phlogopite are significant constituents.   (NGMDB 2008)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:exotic_alkaline_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFD1DC</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>exotic composition igneous rock</sld:Name>
					<sld:Abstract>Rock with 'exotic' mineralogical, textural or field setting characteristics; typically dark colored, with abundant phenocrysts.  Criteria include: presence of greater than 10 percent melilite or leucite; presence of kalsilite; greater than 50 percent carbonate minerals.  (Gillespie and Styles 1999; LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:exotic_composition_igneous_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#A6FCAA</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>fault-related material</sld:Name>
					<sld:Abstract>Material formed as a result brittle faulting, composed of greater than 10 percent matrix; matrix is fine-grained material caused by tectonic grainsize reduction.  Includes cohesive (cataclasite series) and non-cohesive (breccia-gouge series) material.  (CGI concepts task group)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:fault_related_material</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#D0CBB2</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>fine grained igneous rock</sld:Name>
					<sld:Abstract>Aphanitic or porphyritic igneous rock composed of greater than 10 percent groundmass, in which most of the crystals cannot be distinguished with the unaided eye; grainsize is typically less than 1mm.  Igneous rocks with 'exotic' composition are excluded from this concept.  (Gillespie and Styles 1999; LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:fine_grained_igneous_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FF00FF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>foid dioritic rock</sld:Name>
					<sld:Abstract>Phaneritic crystalline rock in which M is less than 90, composed of feldspathoids as 10-60 percent of the felsic minerals, sodic plagioclase (An0-An50) and large amounts of mafic minerals.  Includes rocks defined modally in QAPF field 14.  (LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:foid_dioritic_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#E88CA0</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>foid gabbroic rock</sld:Name>
					<sld:Abstract>Phaneritic crystalline rock in which M is less than 90, composed of feldspathoids as 10-60 percent of the felsic minerals, calcic plagioclase (An50-An100) and large amounts of mafic minerals.  Includes rocks defined modally in QAPF field 14.  (LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:foid_gabbroic_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#CE929F</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>foid syenitic rock</sld:Name>
					<sld:Abstract>Leucocratic, phaneritic crystalline rock composed of feldspathoids as 10-60 percent of the felsic minerals, alkali feldspar and mafic minerals.  Includes rocks defined modally in QAPF field 11.  (LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:foid_syenitic_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FF9EBE</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>foiditic rock</sld:Name>
					<sld:Abstract>Fine grained crystalline rock containing more than 60 percent feldspathoids in total light-coloured constituents.  Includes rocks defined modally in QAPF field 15 or chemically in TAS field F.  (LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:foiditic_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FF7357</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>foidolite</sld:Name>
					<sld:Abstract>Phaneritic crystalline rock containing more than 60 percent feldspathoids in total light-coloured constituents.  Includes rocks defined modally in QAPF field 15  (LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:foidolite</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FD1D68</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>foliated metamorphic rock</sld:Name>
					<sld:Abstract>Metamorphic rock in which 10 percent or more of the contained mineral grains are elements in a planar or linear fabric.  Cataclastic or glassy character precludes classification with this concept.  (based on NADM SLTT metamorphic)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:foliated_metamorphic_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#EE7CE8</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>fragmental igneous material</sld:Name>
					<sld:Abstract>Igneous material of unspecified consolidation state in which greater than 75 percent of the rock consists of fragments produced as a result of igneous rock-forming process.   (CGI concepts task group)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:fragmental_igneous_material</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#EEA0AA</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>fragmental igneous rock</sld:Name>
					<sld:Abstract>Igneous rock in which greater than 75 percent of the rock consists of fragments produced as a result of igneous rock-forming process. Includes pyroclastic rocks, autobreccia associated with lava flows and intrusive breccias.  Excludes deposits reworked by epiclastic processes.  (NGMDB 2008)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:fragmental_igneous_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#EEA0AA</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>framestone</sld:Name>
					<sld:Abstract>Carbonate reef rock consisting of a rigid framework of colonies, shells or skeletons, with internal cavities filled with fine sediment; usually created through the activities of colonial organisms.  (Hallsworth and Knox 1999; SLTTs, 2004, Table 15-3-1)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:framestone</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#A7A7FF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>gabbro</sld:Name>
					<sld:Abstract>Phaneritic crystalline rock composed essentially of calcic plagioclase (An50 to An100), pyroxene and iron oxides.  Includes rocks defined modally in QAPF Field 10 as gabbro.  (LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:gabbro</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#E9935A</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>gabbroic rock</sld:Name>
					<sld:Abstract>Phaneritic crystalline rock composed of 65 percent or more of calcic plagioclase and up to 20 percent quartz or up to 10 percent feldspathoid.  Includes rocks defined modally in QAPF fields 9 and 10 as gabbro or monzogabbro and foid-bearing varieties.  (LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:gabbroic_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FF5B5B</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>glassy igneous rock</sld:Name>
					<sld:Abstract>Igneous rock that consists of greater than 90 percent glass.  (NGMDB Glassy extrusive rock)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:glassy_igneous_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFE5F3</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>gneiss</sld:Name>
					<sld:Abstract>Foliated metamorphic rock with bands or lenticles rich in granular minerals alternating with bands or lenticles rich in minerals with a flaky or elongate prismatic habit.  Mylonitic foliation or well developed, continuous schistosity (greater than 50 percent of the rock consists of grains participate in a planar or linear fabric) precludes classification with this concept.  (Neuendorf et al. 2005)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:gneiss</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#9F00CA</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>grainstone</sld:Name>
					<sld:Abstract>Carbonate sedimentary rock with recognizable depositional fabric that is grain-supported; contains little or no mud matrix.  (Dunham 1962)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:grainstone</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFE389</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>granite</sld:Name>
					<sld:Abstract>Phaneritic crystalline rock consisting of quartz, alkali feldspar and plagioclase (typically sodic) in variable amounts, usually with biotite and/or hornblende.  Includes rocks defined modally in QAPF Field 3.  (LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:granite</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FB2338</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>granitic rock</sld:Name>
					<sld:Abstract>Phaneritic crystalline igneous rock consisting of quartz, alkali feldspar and/or plagioclase.  Includes rocks defined modally in QAPF fields 2, 3, 4 and 5 as alkali granite, granite, granodiorite or tonalite.  (LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:granitic_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#EE68A6</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>granodiorite</sld:Name>
					<sld:Abstract>Phaneritic crystalline rock consisting essentially of quartz, sodic plagioclase and lesser amounts of alkali feldspar with minor hornblende and biotite.  Includes rocks defined modally in QAPF field 4.  (LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:granodiorite</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#E979A6</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>granofels</sld:Name>
					<sld:Abstract>Phaneritic metamorphic rock with granoblastic fabric and very little or no foliation (less than 10 percent of the mineral grains in the rock are elements in a planar or linear fabric).  (NADM SLTTm 2004)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:granofels</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#A337DF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>gravel</sld:Name>
					<sld:Abstract>Sediment containing greater than 30 percent gravel-size particles (greater than 2.0 mm diameter).  (NADM SLTTs 2004)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:gravel</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#ECB400</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>hornblendite</sld:Name>
					<sld:Abstract>Ultramafic rock that consists of greater than 40 percent hornblende plus pyroxene and has a hornblende to pyroxene ratio greater than 1. Includes olivine hornblendite, olivine-pyroxene hornblendite, pyroxene hornblendite, and hornblendite.  (LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:hornblendite</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#A30109</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>hornfels</sld:Name>
					<sld:Abstract>Granofels composed of a mosaic of equidimensional grains in a characteristically granoblastic or decussate matrix; porphyroblasts or relict phenocrysts may be present.  Typically formed by contact metamorphism.  (IUGS SCMR 2007 (http://www.bgs.ac.uk/SCMR/))</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:hornfels</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#EAAFFF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>igneous material</sld:Name>
					<sld:Abstract>Earth material formed as a result of igneous processes, eg. intrusion and cooling of magma in the crust, volcanic eruption.  (CGI concepts task group)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:igneous_material</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#F84D4D</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>igneous rock</sld:Name>
					<sld:Abstract>Rock formed as a result of igneous processes, for example intrusion and cooling of magma in the crust, or volcanic eruption.  (NGMDB 2008)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:igneous_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#F84D4D</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>impact metamorphic rock</sld:Name>
					<sld:Abstract>Rock that contains features indicative of shock metamorphism, such as microscopic planar deformation features within grains or shatter cones.  Includes breccias and melt rocks.  (Stöffler and Grieve 2007; Jackson 1997)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:impact_metamorphic_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#9063FF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>intermediate composition igneous material</sld:Name>
					<sld:Abstract>Igneous material with between 52 and 63 percent SiO2.  (after LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:intermediate_composition_igneous_material</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFE699</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>intermediate composition igneous rock</sld:Name>
					<sld:Abstract>Igneous rock with between 52 and 63 percent SiO2.  (after LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:intermediate_composition_igneous_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFE699</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>iron rich sediment</sld:Name>
					<sld:Abstract>Sediment that consists of at least 50 percent iron-bearing minerals (hematite, magnetite, limonite-group, siderite, iron-sulfides), as determined by hand-lens or petrographic analysis. Corresponds to a rock typically containing 15 percent iron by weight.  (NGMDB 2008)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:iron_rich_sediment</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#B99598</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>iron rich sedimentary material</sld:Name>
					<sld:Abstract>Sedimentary material of unspecified consolidation state that consists of at least 50 percent iron-bearing minerals (hematite, magnetite, limonite-group, siderite, iron-sulfides), as determined by hand-lens or petrographic analysis. Corresponds to a rock typically containing 15 percent iron by weight.  (NGMDB 2008)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:iron_rich_sedimentary_material</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#B99598</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>iron rich sedimentary rock</sld:Name>
					<sld:Abstract>Sedimentary rock that consists of at least 50 percent iron-bearing minerals (hematite, magnetite, limonite-group, siderite, iron-sulfides), as determined by hand-lens or petrographic analysis. Corresponds to a rock typically containing 15 percent iron by weight.  (Hallsworth and Knox 1999; NADM SLTT sedimentary)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:iron_rich_sedimentary_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#B99598</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>komatiitic rock</sld:Name>
					<sld:Abstract>Ultramafic volcanic rock, typically with spinifex texture of intergrown skeletal and bladed olivine and pyroxene crystals set in abundant glass.  Includes picrite, komatiite and meimechite.  (LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:komatiitic_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#B33000</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>lapillistone, agglomerate, tuff breccia</sld:Name>
					<sld:Abstract>Pyroclastic rock in which the average grain size of particles is greater than 2 mm.  (Schmid 1981; LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:lapillistone_agglomerate_tuff_breccia</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFE6D9</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>marble</sld:Name>
					<sld:Abstract>Metamorphic rock consisting of greater than 75 percent fine- to coarse-grained recrystallized calcite and/or dolomite; usually with a granoblastic, saccharoidal texture.  (IUGS SCMR 2007 (http://www.bgs.ac.uk/SCMR/),  SLTTm1.0 2004)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:marble</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#0000FF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>metamorphic rock</sld:Name>
					<sld:Abstract>Rock formed by solid-state mineralogical, chemical and/or structural changes to a pre-existing rock, in response to marked changes in temperature, pressure, shearing stress and chemical environment; generally at depth in the crust.  (Jackson 1997)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:metamorphic_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#E6CDFF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>migmatite</sld:Name>
					<sld:Abstract>Silicate metamorphic rock that is pervasively heterogeneous on a meso- to megascopic scale that typically consists of darker and lighter parts; the darker parts usually exhibit features of metamorphic rocks whereas the lighter parts are of igneous-looking appearance.  (after IUGS SCMR 2007 (http://www.bgs.ac.uk/SCMR/))</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:migmatite</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#AC0000</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>monzodiorite</sld:Name>
					<sld:Abstract>Phaneritic crystalline igneous rock consisting of sodic plagioclase (An0 to An50), alkali feldspar, hornblende and biotite, with or without pyroxene.  Includes rocks defined modally in QAPF field 9 as monzodiorite.  (CGI concepts task group)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:monzodiorite</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFA99D</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>monzogabbro</sld:Name>
					<sld:Abstract>Phaneritic crystalline igneous rock consisting of calcic plagioclase (An50 to An100), alkali feldspar, hornblende and biotite, with or without pyroxene.  Includes rocks defined modally in QAPF field 9 as monzogabbro.  (CGI concepts task group)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:monzogabbro</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFD6D1</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>monzonite</sld:Name>
					<sld:Abstract>Phaneritic crystalline rock containing almost equal amounts of plagioclase and alkali feldspar with minor amphibole and/or pyroxene and little or no quartz.  Includes rocks defined modally in QAPF Field 8.  (LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:monzonite</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FF275A</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>mud</sld:Name>
					<sld:Abstract>Clastic sediment consisting of less than 30 percent gravel-size (2 mm) particles and with a mud to sand ratio greater than 1. Clasts may be of any composition or origin.  (based on NADM SLTT sedimentary; Neuendorf et al. 2005; particle size from Wentworth grade scale)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:mud</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#AFE6CA</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>mudstone</sld:Name>
					<sld:Abstract>Clastic sedimentary rock consisting of less than 30 percent gravel-size (2 mm) particles and with a mud to sand ratio greater than 1. Clasts may be of any composition or origin.  (Pettijohn et al. 1987 referenced in Hallsworth and Knox 1999; extrapolated from Folk, 1954, Figure 1a)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:mudstone</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#ACE4C8</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>mylonitic rock</sld:Name>
					<sld:Abstract>Metamorphic rock characterised by a foliation resulting from tectonic grain size reduction, in which more than 10 percent of the rock volume has undergone grain size reduction.  Includes protomylonite, mylonite, ultramylonite, phyllonite and blastomylonite.  (Marshak and Mitra 1988)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:mylonitic_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#D0CBB0</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>natural unconsolidated material</sld:Name>
					<sld:Abstract>Unconsolidated material known to have natural, ie. not human-made, origin.  (NGMDB vocabulary)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:natural_unconsolidated_material</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FDF43F</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>non-clastic siliceous sediment</sld:Name>
					<sld:Abstract>Sediment that consists of at least 50 percent silicate mineral material, deposited directly by chemical or biological processes at the depositional surface, or in particles formed by chemical or biological processes within the basin of deposition.  (NGMDB 2008; Hallsworth and Knox 1999)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:non_clastic_siliceous_sediment</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#6363EB</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>non-clastic siliceous sedimentary material</sld:Name>
					<sld:Abstract>Sedimentary material that consists of at least 50 percent silicate mineral material, deposited directly by chemical or biological processes at the depositional surface, or in particles formed by chemical or biological processes within the basin of deposition.  (NGMDB 2008)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:non_clastic_siliceous_sedimentary_material</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#6363EB</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>non-clastic siliceous sedimentary rock</sld:Name>
					<sld:Abstract>Sedimentary rock that consists of at least 50 percent silicate mineral material, deposited directly by chemical or biological processes at the depositional surface, or in particles formed by chemical or biological processes within the basin of deposition.  (NGMDB 2008)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:non_clastic_siliceous_sedimentary_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#6363EB</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>ooze</sld:Name>
					<sld:Abstract>Mud with a sand to mud ratio less than 1 to 9 that contains at least 30 percent skeletal remains of pelagic organisms.  Less than 1 percent gravel and less than 50 percent carbonate minerals.  (based on Bates and Jackson 1987 and Hallsworth and Knox 1999)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:ooze</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#9696B9</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>organic rich sediment</sld:Name>
					<sld:Abstract>Sediment with color, composition, texture and apparent density indicating greater than 50 percent organic content by weight on a moisture-free basis.  (NADM SLTT sedimentary)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:organic_rich_sediment</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#42413C</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>organic rich sedimentary material</sld:Name>
					<sld:Abstract>Sedimentary material in which  50 percent or more of the primary sedimentary material is organic carbon.  (NADM SLTT sedimentary)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:organic_rich_sedimentary_material</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#42413C</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>organic rich sedimentary rock</sld:Name>
					<sld:Abstract>Sedimentary rock with color, composition, texture and apparent density indicating greater than 50 percent organic content by weight on a moisture-free basis.  (NADM SLTT sedimentary)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:organic_rich_sedimentary_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#42413C</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>packstone</sld:Name>
					<sld:Abstract>Carbonate sedimentary rock with discernible grain supported depositional texture, containing greater than 10 percent grains; intergranular spaces are filled by matrix.  (Hallsworth and Knox 1999)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:packstone</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#2727E3</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>peat</sld:Name>
					<sld:Abstract>Unconsolidated organic-rich sediment composed of at least 50 percent semi-carbonised plant remains; individual remains commonly seen with unaided eye; yellowish brown to brownish black; generally fibrous texture; can be plastic or friable.  In its natural state it can be readily cut and has a very high moisture content, generally greater than 90 percent.  (Hallsworth and Knox 1999)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:peat</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFCC99</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>pegmatite</sld:Name>
					<sld:Abstract>Exceptionally coarse grained crystalline rock with interlocking crystals; most grains are 1cm or more diameter; composition is generally that of granite, but the term may refer to the coarse grained facies of any type of igneous rock;usually found as irregular dikes, lenses, or veins associated with plutons or batholiths.  (Neuendorf et al. 2005)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:pegmatite</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFD1DC</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>peridotite</sld:Name>
					<sld:Abstract>Ultramafic rock consisting of more than 40 percent (by volume) olivine with pyroxene and/or amphibole and little or no feldspar.  Commonly altered to serpentinite.  Includes rocks defined modally in the ultramafic rock classification as dunite, harzburgite, lherzolite, wehrlite, olivinite, pyroxene peridotite, pyroxene hornblende peridotite or hornblende peridotite.  (LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:peridotite</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#CE0031</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>phaneritic igneous rock</sld:Name>
					<sld:Abstract>Igneous rock in which greater than 10 percent of rock is individual crystals that can be discerned with the unaided eye. Bounding grain size is on the order of 32 to 100 microns.  Igneous rocks with 'exotic' composition are excluded from this concept.  (Neuendorf et al. 2005)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:phaneritic_igneous_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FF70B5</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>phonolitic rock</sld:Name>
					<sld:Abstract>Fine grained igneous rock containing feldspathoids and in which alkali feldspar is thought to be more abundant than plagioclase.  Includes rocks defined modally in QAPF fields 11 and 12, and TAS field Ph.  (LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:phonolitic_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#5F391F</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>phosphate rich sedimentary material</sld:Name>
					<sld:Abstract>Sedimentary material in which at least 50 percent of the primary and/or recrystallized constituents are phosphate minerals.  (NGMDB 2008)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:phosphate_rich_sedimentary_material</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#9ED7C2</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>phosphatic sediment</sld:Name>
					<sld:Abstract>Sediment in which at least 50 percent of the primary and/or recrystallized constituents are phosphate minerals.  (NGMDB 2008)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:phosphatic_sediment</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#9ED7C2</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>phosphorite</sld:Name>
					<sld:Abstract>Sedimentary rock in which at least 50 percent of the primary or recrystallized constituents are phosphate minerals.  Most commonly occurs as a bedded primary or reworked secondary marine rock, composed of microcrystalline carbonate fluorapatite in the form of lamina, pellets, oolites and nodules, and skeletal, shell and bone fragments.  (HallsworthandKnox 1999, Jackson 1997)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:phosphorite</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#BFE3DC</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>phyllite</sld:Name>
					<sld:Abstract>Rock with a well developed, continuous schistosity, an average grain size between 0.1 and 0.5 millimeters, and a silvery sheen on cleavage surfaces. Individual phyllosilicate grains are barely visible with the unaided eye.  (IUGS SCMR 2007 (http://www.bgs.ac.uk/SCMR/))</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:phyllite</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#EDEDF3</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>phyllonite</sld:Name>
					<sld:Abstract>Mylonitic rock composed largely of fine-grained mica that imparts a sheen to foliation surfaces; may have flaser lamination, isoclinal folding, and deformed veins, which indicate significant shearing.  Macroscopically resembles phyllite, but formed by mechanical degradation of initially coarser rock.  (NADM metamorphic rock vocabulary SLTTm1.0; Marshak and Mitra 1988)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:phyllonite</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#339966</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>porphyry</sld:Name>
					<sld:Abstract>Igneous rock that contains conspicuous phenocrysts in a finer grained groundmass; groundmass itself may be phaneritic or fine-grained.  (LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:porphyry</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFFFE8</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>pyroclastic material</sld:Name>
					<sld:Abstract>Fragmental igneous material that consists of more than 75 percent of particles formed by disruption as a direct result of volcanic action.   (LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:pyroclastic_material</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFEDBF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>pyroclastic rock</sld:Name>
					<sld:Abstract>Fragmental igneous rock that consists of greater than 75 percent fragments produced as a direct result of eruption or extrusion of magma from within the earth onto its surface. Includes autobreccia associated with lava flows and excludes deposits reworked by epiclastic processes.  ()</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:pyroclastic_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFEDBF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>pyroxenite</sld:Name>
					<sld:Abstract>Ultramafic phaneritic igneous rock composed almost entirely of one or more pyroxenes and occasionally biotite, hornblende and olivine. Includes rocks defined modally in the ultramafic rock classification as olivine pyroxenite, olivine-hornblende pyroxenite, pyroxenite, orthopyroxenite, clinopyroxenite and websterite.  (LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:pyroxenite</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#C1010A</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>quartz rich phaneritic igneous rock</sld:Name>
					<sld:Abstract>Phaneritic crystalline igneous rock that contains greater than 60 percent quartz.  (Gillespie and Styles 1999; LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:quartz_rich_phaneritic_igneous_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#EEA0AA</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>quartzite</sld:Name>
					<sld:Abstract>Metamorphic rock consisting of greater than or equal to 75 percent quartz; typically granoblastic texture.  (after Neuendorf et al. 2005)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:quartzite</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#9FFF9F</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>rhyolitic rock</sld:Name>
					<sld:Abstract>Fine-grained igneous rock, typically porphyritic, consisting of quartz and alkali feldspar (often as phenocrysts), with minor plagioclase and biotite, in a microcrystalline, cryptocrystalline or glassy groundmass.  Flow texture is common.  Includes rocks defined modally in QAPF fields 2 and 3 or chemically in TAS Field R as rhyolite.  (LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:rhyolitic_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FED768</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>rock</sld:Name>
					<sld:Abstract>Consolidated aggregate of one or more EarthMaterials, or a body of undifferentiated mineral matter, or of solid organic material.  Includes mineral aggregates such as granite, shale, marble; glassy matter such as obsidian; and organic material such a coal. Excludes unconsolidated materials.  (Jackson, 1997; NADM C1 2004; Neuendorf et al 2005)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FF0000</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>sand</sld:Name>
					<sld:Abstract>Clastic sediment in which less than 30 percent of particles are gravel (greater than 2 mm in diameter) and the sand to mud ratio is at least 1.   (Neuendorf et al. 2005; NGMDB)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:sand</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFCB23</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>sandstone</sld:Name>
					<sld:Abstract>Clastic sedimentary rock in which less than 30 percent of particles are greater than 2 mm in diameter (gravel) and the sand to mud ratio is at least 1.   (NGMDB; Neuendorf et al. 2005; particle size from Wentworth grade scale)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:sandstone</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#CDFFD9</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>schist</sld:Name>
					<sld:Abstract>Foliated phaneritic metamorphic rock with well developed, continuous schistosity (greater than 50 percent of mineral grains with a tabular, lamellar, or prismatic crystallographic habit that are oriented in a continuous planar or linear fabric).  (NADM metamorphic rock vocabulary SLTTm1.0; Neuendorf et al. 2005)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:schist</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#DBDBE7</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>sediment</sld:Name>
					<sld:Abstract>Unconsolidated material consisting of an aggregation of particles transported or deposited by air, water or ice, or that accumulated by other natural agents, such as chemical precipitation, and that forms in layers on the Earth's surface.  Includes epiclastic deposits.  (NADM SLTT sedimentary)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:sediment</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFFF00</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>sedimentary material</sld:Name>
					<sld:Abstract>Material formed by accumulation of solid fragmental material deposited by air, water or ice, or material that accumulated by other natural agents such as chemical precipitation from solution or secretion by organisms.  Includes both sediment and sedimentary rock.  Includes epiclastic deposits.  (SLTTs [2004])</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:sedimentary_material</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#F5F500</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>sedimentary rock</sld:Name>
					<sld:Abstract>Rock formed by accumulation and cementation of solid fragmental material deposited by air, water or ice, or as a result of other natural agents, such as precipitation from solution, the accumulation of organic material, or from biogenic processes, including secretion by organisms.  Includes epiclastic deposits.  (SLTTs [2004])</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:sedimentary_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#CFEFDF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>serpentinite</sld:Name>
					<sld:Abstract>Rock consisting of more than 75 percent serpentine-group minerals, eg. antigorite, chrysotile or lizardite; accessory chlorite, talc and magnetite may be present; derived from hydration of ferromagnesian silicate minerals such as olivine and pyroxene.  (Neuendorf et al. 2005)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:serpentinite</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#005C00</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>shale</sld:Name>
					<sld:Abstract>Laminated mudstone that will part or break along thin, closely spaced layers parallel to stratification.  (NADM SLTT sedimentary; Neuendorf et al. 2005)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:shale</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#C0D0C0</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>slate</sld:Name>
					<sld:Abstract>Compact, fine grained rock with an average grain size less than 0.032 millimeter and a well developed schistosity (slaty cleavage), and hence can be split into slabs or thin plates.  (NADM metamorphic rock vocabulary SLTTm1.0; Neuendorf et al. 2005)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:slate</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#A7A7FF</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>syenite</sld:Name>
					<sld:Abstract>Coarse grained crystalline rock consisting mainly of alkali feldspar with subordinate sodic plagioclase, biotite, pyroxene, amphibole and occasional fayalite; minor quartz or nepheline may be present.  Defined modally in QAPF Field 7.  ()</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:syenite</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#CD3278</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>syenitic rock</sld:Name>
					<sld:Abstract>Phaneritic crystalline igneous rock with M less than 90, consisting mainly of alkali feldspar and plagioclase; minor quartz or nepheline may be present, along with pyroxene, amphibole or biotite.  Includes rocks classified in QAPF fields 6, 7 and 8 as syenite or monzonite.  (LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:syenitic_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#CD3278</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>tephra</sld:Name>
					<sld:Abstract>Unconsolidated pyroclastic material in which greater than 75 percent of the fragments are deposited as a direct result of volcanic processes and the deposit has not been reworked by epiclastic processes.  Includes ash, lapilli tephra, bomb tephra,  block tephra and unconsolidated agglomerate.  (Hallsworth and Knox 1999; LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:tephra</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#C84100</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>tephritic rock</sld:Name>
					<sld:Abstract>Fine grained crystalline igneous rock containing essential feldspathoids and in which plagioclase is more abundant than alkali feldspar.  Includes rocks classified in QAPF field 14 or chemically in TAS field U1 as basanite or tephrite.  (LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:tephritic_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#C24100</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>tonalite</sld:Name>
					<sld:Abstract>Phaneritic crystalline rock consisting of quartz and intermediate plagioclase, usually with biotite and amphibole.  Includes rocks defined modally in QAPF field 5.  (LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:tonalite</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FF6F6B</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>trachytic rock</sld:Name>
					<sld:Abstract>Fine-grained volcanic rock consisting of alkali feldspar and minor mafic minerals, typically amphibole or mica; typically porphyritic.  Includes rocks defined modally in QAPF fields 6, 7 and 8 or chemically in TAS Field T as trachyte or latite.  (LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:trachytic_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FEA060</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>tuff-breccia, agglomerate, or pyroclastic breccia</sld:Name>
					<sld:Abstract>Pyroclastic rock in which greater than 25 percent of particles are greater than 64 mm in largest dimension. Includes agglomerate, pyroclastic breccia   of Gillespie and Styles (1999)  (Schmid 1981; LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:tuff_breccia_agglomerate_or_pyroclastic_breccia</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFEFD9</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>ultrabasic igneous rock</sld:Name>
					<sld:Abstract>Igneous rock with less than 45 percent SiO2.  (after LeMaitre et al. 2002)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:ultrabasic_igneous_rock</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#CC0000</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>unconsolidated material</sld:Name>
					<sld:Abstract>CompoundMaterial composed of an aggregation of particles that do not adhere to each other strongly enough that the aggregate can be considered a solid in its own right.  (NGMDB)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:unconsolidated_material</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#FFF900</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
				<sld:Rule>
					<sld:Name>wackestone</sld:Name>
					<sld:Abstract>Sandstone that contains between 15 and 75 percent matrix of unspecified origin.  (CGI concepts task group)</sld:Abstract>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
							<ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:2008:wackestone</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:CssParameter name="fill">#BDDBF1</sld:CssParameter>
						</sld:Fill>
					</sld:PolygonSymbolizer>
				</sld:Rule>
			</sld:FeatureTypeStyle>
		</sld:UserStyle>
	</sld:NamedLayer>
</sld:StyledLayerDescriptor>
