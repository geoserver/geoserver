<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
    xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <!-- a Named Layer is the basic building block of an SLD document -->
    <NamedLayer>
        <UserStyle>
            <FeatureTypeStyle>
                <Rule>
                    <PointSymbolizer>
                        <Graphic>
                            <Mark>
                                <WellKnownName>circle</WellKnownName>
                                <Fill>
                                    <GraphicFill>
                                        <Graphic>
                                            <Mark>
                                                <WellKnownName>
                                                    square
                                                </WellKnownName>
                                                <Fill>
                                                    <CssParameter name="fill">#333333
                                                    </CssParameter>
                                                </Fill>
                                            </Mark>
                                            <Size>4</Size>
                                        </Graphic>
                                    </GraphicFill>
                                </Fill>
                                <Stroke>
                                    <GraphicStroke>
                                        <Graphic>
                                            <Mark>
                                                <WellKnownName>
                                                    square
                                                </WellKnownName>
                                                <Fill>
                                                    <CssParameter name="fill">#333333
                                                    </CssParameter>
                                                </Fill>
                                            </Mark>
                                            <Size>4</Size>
                                        </Graphic>
                                    </GraphicStroke>
                                </Stroke>
                            </Mark>
                            <Size>
                                64
                            </Size>
                        </Graphic>
                    </PointSymbolizer>
                </Rule>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>

