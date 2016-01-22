<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0"
    xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
    xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
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
                                    <CssParameter name="fill">#666666
                                    </CssParameter>
                                </Fill>
                                <Stroke>
                                    <CssParameter name="stroke">#333333
                                    </CssParameter>
                                    <CssParameter name="stroke-width">1
                                    </CssParameter>
                                </Stroke>
                            </Mark>
                            <Size>
                              <ogc:Function name="env">
                                  <ogc:Literal>radius</ogc:Literal>
                                  <ogc:Literal>4</ogc:Literal>
                              </ogc:Function>
                            </Size>
                        </Graphic>
                    </PointSymbolizer>
                </Rule>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>

