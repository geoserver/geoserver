<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
                       xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
                       xmlns="http://www.opengis.net/sld"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <NamedLayer>
        <Name>Default Polygon</Name>
        <UserStyle>
            <FeatureTypeStyle>
                <Rule>
                    <PolygonSymbolizer>
                        <Fill>
                            <CssParameter name="fill">#AAAAAA</CssParameter>
                        </Fill>
                    </PolygonSymbolizer>
                </Rule>
                <VendorOption name="vt-coalesce">true</VendorOption>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>