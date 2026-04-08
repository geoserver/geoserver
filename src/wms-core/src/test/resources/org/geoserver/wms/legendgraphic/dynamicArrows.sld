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
                            <Size>
                                <ogc:Mul>
                                    <ogc:Function name="sqrt">
                                        <ogc:Add>
                                            <ogc:Mul>
                                                <ogc:PropertyName>Band1</ogc:PropertyName>
                                                <ogc:PropertyName>Band1</ogc:PropertyName>
                                            </ogc:Mul>
                                            <ogc:Mul>
                                                <ogc:PropertyName>Band2</ogc:PropertyName>
                                                <ogc:PropertyName>Band2</ogc:PropertyName>
                                            </ogc:Mul>
                                        </ogc:Add>
                                    </ogc:Function>
                                    <ogc:Literal>200</ogc:Literal>
                                </ogc:Mul>
                            </Size>
                            <Rotation>
                                <ogc:Function name="toDegrees">
                                    <ogc:Function name="atan2">
                                        <ogc:PropertyName>Band2</ogc:PropertyName>
                                        <ogc:PropertyName>Band1</ogc:PropertyName>
                                    </ogc:Function>
                                </ogc:Function>
                            </Rotation>
                        </Graphic>
                        <Priority>
                            <ogc:Add>
                                <ogc:Mul>
                                    <ogc:PropertyName>Band1</ogc:PropertyName>
                                    <ogc:PropertyName>Band1</ogc:PropertyName>
                                </ogc:Mul>
                                <ogc:Mul>
                                    <ogc:PropertyName>Band2</ogc:PropertyName>
                                    <ogc:PropertyName>Band2</ogc:PropertyName>
                                </ogc:Mul>
                            </ogc:Add>
                        </Priority>
                        <VendorOption name="conflictResolution">false</VendorOption>
                    </TextSymbolizer>
                </Rule>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>
