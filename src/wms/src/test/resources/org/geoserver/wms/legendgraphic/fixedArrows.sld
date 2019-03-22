<StyledLayerDescriptor version="1.0.0"
                       xmlns="http://www.opengis.net/sld" xmlns:gml="http://www.opengis.net/gml"
                       xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://www.opengis.net/sld ./StyledLayerDescriptor.xsd">
    <NamedLayer>
        <Name>contour_lines</Name>
        <UserStyle>
            <FeatureTypeStyle>
                <Rule>
                    <TextSymbolizer>
                        <Label><![CDATA[ ]]></Label> <!-- fake label -->
                        <Graphic>
                            <Mark>
                                <WellKnownName>shape://carrow</WellKnownName>
                                <Fill>
                                    <CssParameter name="fill">#000000</CssParameter>
                                </Fill>
                            </Mark>
                            <Size>10</Size>
                        </Graphic>
                        <VendorOption name="conflictResolution">false</VendorOption>
                    </TextSymbolizer>
                </Rule>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>
