<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0"
    xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd"
    xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
    xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
<!--
This document is used by AbstractLegendGraphicOutputFormatTest.testExternalGraphic.
-->    
    <NamedLayer>
        <Name>aircraft_report</Name>
        <UserStyle>
            <Name>external_graphic_demo</Name>
            <Title>External Graphic Demo</Title>
            <Abstract>
            </Abstract>
            <FeatureTypeStyle>
                <Rule>
                    <Name>circle</Name>
                    <Title>Circle</Title>
                    <Abstract>This rule uses a Mark</Abstract>
                    <PointSymbolizer>
                        <Graphic>
                            <Mark>
                                <WellKnownName>circle</WellKnownName>
                                <Stroke>
                                    <CssParameter name="stroke">#1111FF</CssParameter>
                                    <CssParameter name="stroke-width">0.5</CssParameter>
                                </Stroke>
                            </Mark>
                            <Size>
                                <ogc:Literal>14</ogc:Literal>
                            </Size>
                        </Graphic>
                    </PointSymbolizer>
                </Rule>
                <Rule>
                    <Name>externalGraphic</Name>
                    <Title>External Graphic</Title>
                    <Abstract>This rule uses an ExternalGraphic</Abstract>
                    <LegendGraphic>
                        <Graphic>
                            <ExternalGraphic>
                                <OnlineResource xlink:href="ExternalGraphicIcon.png"/>
                                <Format>image/png</Format>
                            </ExternalGraphic>
                        </Graphic>
                    </LegendGraphic>
                </Rule>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>
