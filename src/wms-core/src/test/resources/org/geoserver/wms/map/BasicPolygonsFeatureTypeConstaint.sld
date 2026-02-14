<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
	xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd"
	xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
	xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
	<NamedLayer>
		<Name>cite:BasicPolygons</Name>
		<LayerFeatureConstraints>
			<FeatureTypeConstraint>
			    <FeatureTypeName>cite:BasicPolygons</FeatureTypeName>
				<ogc:Filter>	
					<ogc:PropertyIsEqualTo>
						<ogc:PropertyName>ID</ogc:PropertyName>
						<ogc:Literal>xyz</ogc:Literal>
					</ogc:PropertyIsEqualTo>
				</ogc:Filter>
			</FeatureTypeConstraint>
		</LayerFeatureConstraints>
    	<UserStyle>
			<Name>TheLibraryModeStyle</Name>
			<IsDefault>true</IsDefault>
			<FeatureTypeStyle>
				<Rule>
					<!-- like a linesymbolizer but with a fill too -->
					<PolygonSymbolizer>
						<Fill />
					</PolygonSymbolizer>
				</Rule>
			</FeatureTypeStyle>
		</UserStyle>
	</NamedLayer>
</StyledLayerDescriptor>

