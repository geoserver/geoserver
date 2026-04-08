<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
                       xsi:schemaLocation="http://www.opengis.net/sld
http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd"
                       xmlns="http://www.opengis.net/sld"
                       xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xmlns:ogc="http://www.opengis.net/ogc">
    <NamedLayer>
        <Name>ne:countries</Name>
        <UserStyle>
            <Title>Multi-Named Layers</Title>
            <FeatureTypeStyle>
                <Rule>
                    <MinScaleDenominator>20000000</MinScaleDenominator>
                    <MaxScaleDenominator>30000000</MaxScaleDenominator>
                    <PolygonSymbolizer>
                        <Fill>
                            <CssParameter name="fill">#b5ffe4</CssParameter>
                        </Fill>
                        <Stroke>
                            <CssParameter name="stroke">#232323</CssParameter>
                            <CssParameter name="stroke-opacity">0
                            </CssParameter>
                            <CssParameter name="stroke-width">1
                            </CssParameter>
                            <CssParameter name="stroke-linejoin">bevel
                            </CssParameter>
                        </Stroke>
                    </PolygonSymbolizer>
                    <LineSymbolizer>
                        <Stroke>
                            <CssParameter name="stroke">#9ad9c2
                            </CssParameter>
                            <CssParameter name="stroke-width">1</CssParameter>
                            <CssParameter name="stroke-linejoin">bevel</CssParameter>
                            <CssParameter name="stroke-linecap">square</CssParameter>
                        </Stroke>
                    </LineSymbolizer>
                </Rule>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
    <NamedLayer>
        <Name>ne:populated_places</Name>

        <UserStyle>
            <Title>Multi-Named Layers</Title>
            <FeatureTypeStyle>
                <Rule>
                    <MinScaleDenominator>10000000</MinScaleDenominator>
                    <MaxScaleDenominator>20000000</MaxScaleDenominator>
                    <PointSymbolizer>
                        <Graphic>
                            <Mark>
                                <WellKnownName>circle</WellKnownName>
                                <Fill>
                                    <CssParameter name="fill">#777777</CssParameter>
                                </Fill>
                            </Mark>
                            <Size>3</Size>
                        </Graphic>
                    </PointSymbolizer>
                </Rule>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>