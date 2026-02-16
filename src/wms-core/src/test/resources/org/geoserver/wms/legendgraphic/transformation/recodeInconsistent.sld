<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
                       xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd"
                       xmlns="http://www.opengis.net/sld"
                       xmlns:ogc="http://www.opengis.net/ogc"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <NamedLayer>
        <Name>recode</Name>
        <UserStyle>
            <Title>Region</Title>
            <FeatureTypeStyle>
                <Rule>
                    <Title>Region</Title>
                    <PolygonSymbolizer>
                        <Fill>
                            <CssParameter name="fill">
                                <ogc:Function name="Recode">
                                    <ogc:PropertyName>REGION</ogc:PropertyName>

                                    <ogc:Literal>First</ogc:Literal>
                                    <ogc:Literal>#6495ED</ogc:Literal>

                                    <ogc:Literal>Second</ogc:Literal>
                                    <ogc:Literal>#B0C4DE</ogc:Literal>

                                    <ogc:Literal>Third</ogc:Literal>
                                    <ogc:Literal>#00FFFF</ogc:Literal>
                                </ogc:Function>
                            </CssParameter>
                        </Fill>
                        <Stroke>
                            <CssParameter name="stroke">
                                <ogc:Function name="Recode">
                                    <ogc:PropertyName>REGION</ogc:PropertyName>

                                    <ogc:Literal>First</ogc:Literal>
                                    <ogc:Literal>#6495ED</ogc:Literal>

                                    <ogc:Literal>Second</ogc:Literal>
                                    <ogc:Literal>#B0C4DE</ogc:Literal>

                                    <ogc:Literal>Third</ogc:Literal>
                                    <ogc:Literal>#00FFFF</ogc:Literal>

                                    <!-- Inconsistent entry compared to Fill above -->
                                    <ogc:Literal>Fourth</ogc:Literal>
                                    <ogc:Literal>#AAAAAA</ogc:Literal>
                                </ogc:Function>
                            </CssParameter>
                            <CssParameter name="stroke-width">1</CssParameter>
                        </Stroke>
                    </PolygonSymbolizer>

                </Rule>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>
