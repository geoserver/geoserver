<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
                       xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
                       xmlns="http://www.opengis.net/sld"
                       xmlns:ogc="http://www.opengis.net/ogc"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <NamedLayer>
        <Name>Simple polygon</Name>
        <UserStyle>
            <Title>SLD Cook Book: Simple polygon</Title>
            <FeatureTypeStyle>
                <Rule>
                    <PolygonSymbolizer>
                        <Fill>
                            <CssParameter name="fill">
                                <ogc:Function name="env">
                                    <ogc:Literal>fill</ogc:Literal>
                                    <ogc:Literal>#AAAAAA</ogc:Literal>
                                </ogc:Function>
                            </CssParameter>
                        </Fill>
                    </PolygonSymbolizer>
                </Rule>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>