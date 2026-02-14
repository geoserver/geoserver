<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor xmlns="http://www.opengis.net/sld"
                       xmlns:sld="http://www.opengis.net/sld"
                       xmlns:ogc="http://www.opengis.net/ogc"
                       xmlns:gml="http://www.opengis.net/gml"
                       version="1.0.0">
    <NamedLayer>
        <Name>Permafrost</Name>
        <UserStyle>
            <Title>Permafrost</Title>
            <FeatureTypeStyle>
                <Rule>
                    <Name>PermafrostLegend</Name>
                    <Title>Permafrost
                        <Localized lang="de">Permafrost</Localized>
                        <Localized lang="it">Permafrost</Localized>
                    </Title>
                    <ogc:Filter>
                        <ogc:PropertyIsEqualTo>
                            <ogc:Function name="language"/>
                            <ogc:Literal></ogc:Literal>
                        </ogc:PropertyIsEqualTo>
                    </ogc:Filter>
                    <RasterSymbolizer>
                        <ColorMap type='ramp'>
                            <ColorMapEntry  opacity="0.7" quantity="1"      color="#101EAC" label="unter nahezu allen Bedingungen"/>
                            <ColorMapEntry  opacity="0.7" quantity="1.1"    color="#101EAC" label="in quasi tutte le condizioni"/>
                            <ColorMapEntry  opacity="0.7" quantity="20"     color="#481CBF" label=""/>
                            <ColorMapEntry  opacity="0.7" quantity="55"     color="#9C07E0" label=""/>
                            <ColorMapEntry  opacity="0.7" quantity="92"     color="#F207CB" label="nur unter sehr kalten Bedingungen"/>
                            <ColorMapEntry  opacity="0.7" quantity="93"     color="#F207CB" label="solo con temperature basse"/>
                            <ColorMapEntry  opacity="0.7" quantity="129"    color="#FF1700" label=""/>
                            <ColorMapEntry  opacity="0.7" quantity="155"    color="#FF8500" label=""/>
                            <ColorMapEntry  opacity="0.7" quantity="191"    color="#FFFF00" label="nur unter sehr kalten Bedingungen"/>
                            <ColorMapEntry  opacity="0.7" quantity="192"    color="#FFFF00" label="solo con temperature molto basse"/>
                            <ColorMapEntry  opacity="0.7" quantity="220"    color="#FBFB5B" label=""/>
                            <ColorMapEntry  opacity="0.7" quantity="254"    color="#F0F0A0" label=""/>
                            <ColorMapEntry  opacity="0.7" quantity="255"    color="#FFFFFF" label=""/>
                        </ColorMap>
                    </RasterSymbolizer>
                </Rule>
                <Rule>
                    <Name>PermafrostLegend-DE</Name>
                    <Title>Permafrost
                        <Localized lang="de">Permafrost</Localized>
                    </Title>
                    <ogc:Filter>
                        <ogc:PropertyIsEqualTo>
                            <ogc:Function name="language"/>
                            <ogc:Literal>de</ogc:Literal>
                        </ogc:PropertyIsEqualTo>
                    </ogc:Filter>
                    <RasterSymbolizer>
                        <ColorMap type='ramp'>
                            <ColorMapEntry  opacity="0.7" quantity="1"      color="#101EAC" label="unter nahezu allen Bedingungen"/>
                            <ColorMapEntry  opacity="0.7" quantity="1.1"    color="#101EAC" label=""/>
                            <ColorMapEntry  opacity="0.7" quantity="20"     color="#481CBF" label=""/>
                            <ColorMapEntry  opacity="0.7" quantity="55"     color="#9C07E0" label=""/>
                            <ColorMapEntry  opacity="0.7" quantity="92"     color="#F207CB" label="nur unter sehr kalten Bedingungen"/>
                            <ColorMapEntry  opacity="0.7" quantity="93"     color="#F207CB" label=""/>
                            <ColorMapEntry  opacity="0.7" quantity="129"    color="#FF1700" label=""/>
                            <ColorMapEntry  opacity="0.7" quantity="155"    color="#FF8500" label=""/>
                            <ColorMapEntry  opacity="0.7" quantity="191"    color="#FFFF00" label="nur unter sehr kalten Bedingungen"/>
                            <ColorMapEntry  opacity="0.7" quantity="192"    color="#FFFF00" label=""/>
                            <ColorMapEntry  opacity="0.7" quantity="220"    color="#FBFB5B" label=""/>
                            <ColorMapEntry  opacity="0.7" quantity="254"    color="#F0F0A0" label=""/>
                            <ColorMapEntry  opacity="0.7" quantity="255"    color="#FFFFFF" label=""/>
                        </ColorMap>
                    </RasterSymbolizer>
                </Rule>
                <Rule>
                    <Name>PermafrostLegend-IT</Name>
                    <Title>Permafrost
                        <Localized lang="it">Permafrost</Localized>
                    </Title>
                    <ogc:Filter>
                        <ogc:PropertyIsEqualTo>
                            <ogc:Function name="language"/>
                            <ogc:Literal>it</ogc:Literal>
                        </ogc:PropertyIsEqualTo>
                    </ogc:Filter>
                    <RasterSymbolizer>
                        <ColorMap type='ramp'>
                            <ColorMapEntry  opacity="0.7" quantity="1"      color="#101EAC" label="in quasi tutte le condizioni"/>
                            <ColorMapEntry  opacity="0.7" quantity="1.1"    color="#101EAC" label=""/>
                            <ColorMapEntry  opacity="0.7" quantity="20"     color="#481CBF" label=""/>
                            <ColorMapEntry  opacity="0.7" quantity="55"     color="#9C07E0" label=""/>
                            <ColorMapEntry  opacity="0.7" quantity="92"     color="#F207CB" label="solo con temperature basse"/>
                            <ColorMapEntry  opacity="0.7" quantity="93"     color="#F207CB" label=""/>
                            <ColorMapEntry  opacity="0.7" quantity="129"    color="#FF1700" label=""/>
                            <ColorMapEntry  opacity="0.7" quantity="155"    color="#FF8500" label=""/>
                            <ColorMapEntry  opacity="0.7" quantity="191"    color="#FFFF00" label="solo con temperature molto basse"/>
                            <ColorMapEntry  opacity="0.7" quantity="192"    color="#FFFF00" label=""/>
                            <ColorMapEntry  opacity="0.7" quantity="220"    color="#FBFB5B" label=""/>
                            <ColorMapEntry  opacity="0.7" quantity="254"    color="#F0F0A0" label=""/>
                            <ColorMapEntry  opacity="0.7" quantity="255"    color="#FFFFFF" label=""/>
                        </ColorMap>
                    </RasterSymbolizer>
                </Rule>
                <VendorOption name="inclusion">legendOnly</VendorOption>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>