<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
                       xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc">
    <NamedLayer>
        <Name>notransformation</Name>
        <UserStyle>
            <Title>Region</Title>
            <FeatureTypeStyle>
                <Rule>
                    <Title>Region</Title>
                    <PolygonSymbolizer>
                        <Fill>
                            <!-- No transformation function, just a direct value -->
                            <CssParameter name="fill">#00FFFF</CssParameter>
                        </Fill>
                    </PolygonSymbolizer>
                </Rule>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>
