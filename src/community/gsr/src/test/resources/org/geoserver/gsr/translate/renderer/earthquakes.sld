<?xml version="1.0" encoding="UTF-8"?>
<sld:StyledLayerDescriptor xmlns="http://www.opengis.net/sld" xmlns:sld="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:gml="http://www.opengis.net/gml" version="1.0.0">
    <sld:UserLayer>
        <sld:LayerFeatureConstraints>
            <sld:FeatureTypeConstraint/>
        </sld:LayerFeatureConstraints>
        <sld:UserStyle>
            <sld:Name>d2p2 eq 48hr vwPoint</sld:Name>
            <sld:Title/>
            <sld:IsDefault>1</sld:IsDefault>
            <sld:FeatureTypeStyle>
                <sld:Name>name</sld:Name>
                <sld:FeatureTypeName>Feature</sld:FeatureTypeName>
                <sld:SemanticTypeIdentifier>generic:geometry</sld:SemanticTypeIdentifier>
                <sld:SemanticTypeIdentifier>colorbrewer:quantile:custom</sld:SemanticTypeIdentifier>
                <sld:Rule>
                    <sld:Name>Less than 4.0</sld:Name>
                    <sld:Title>Less than 4.0</sld:Title>
                    <ogc:Filter>
                        <ogc:And>
                            <ogc:PropertyIsGreaterThanOrEqualTo>
                                <ogc:PropertyName>magnitude</ogc:PropertyName>
                                <ogc:Literal>2.5</ogc:Literal>
                            </ogc:PropertyIsGreaterThanOrEqualTo>
                            <ogc:PropertyIsLessThan>
                                <ogc:PropertyName>magnitude</ogc:PropertyName>
                                <ogc:Literal>4.0</ogc:Literal>
                            </ogc:PropertyIsLessThan>
                        </ogc:And>
                    </ogc:Filter>
                    <sld:PointSymbolizer>
                        <sld:Graphic>
                            <sld:Mark>
                            <WellKnownName>circle</WellKnownName>
                                <sld:Fill>
                                    <sld:CssParameter name="fill">#33FF00</sld:CssParameter>
                                    <sld:CssParameter name="fill-opacity">1.0</sld:CssParameter>
                                </sld:Fill>
                                <sld:Stroke/>
                            </sld:Mark>
                            <sld:Size>6.0</sld:Size>
                        </sld:Graphic>
                    </sld:PointSymbolizer>
                </sld:Rule>
                <sld:Rule>
                    <sld:Name>4.0 - 4.5</sld:Name>
                    <sld:Title>4.0 - 4.5</sld:Title>
                    <ogc:Filter>
                        <ogc:And>
                            <ogc:PropertyIsGreaterThanOrEqualTo>
                                <ogc:PropertyName>magnitude</ogc:PropertyName>
                                <ogc:Literal>4.0</ogc:Literal>
                            </ogc:PropertyIsGreaterThanOrEqualTo>
                            <ogc:PropertyIsLessThanOrEqualTo>
                                <ogc:PropertyName>magnitude</ogc:PropertyName>
                                <ogc:Literal>4.5</ogc:Literal>
                            </ogc:PropertyIsLessThanOrEqualTo>
                        </ogc:And>
                    </ogc:Filter>
                    <sld:PointSymbolizer>
                        <sld:Graphic>
                            <sld:Mark>
                            <WellKnownName>circle</WellKnownName>
                                <sld:Fill>
                                    <sld:CssParameter name="fill">#FFFF00</sld:CssParameter>
                                    <sld:CssParameter name="fill-opacity">1.0</sld:CssParameter>
                                </sld:Fill>
                                <sld:Stroke/>
                            </sld:Mark>
                            <sld:Size>8.0</sld:Size>
                        </sld:Graphic>
                    </sld:PointSymbolizer>
                </sld:Rule>
                <sld:Rule>
                    <sld:Name>4.5 - 5.0</sld:Name>
                    <sld:Title>4.5 - 5.0</sld:Title>
                    <ogc:Filter>
                        <ogc:And>
                            <ogc:PropertyIsGreaterThanOrEqualTo>
                                <ogc:PropertyName>magnitude</ogc:PropertyName>
                                <ogc:Literal>4.5</ogc:Literal>
                            </ogc:PropertyIsGreaterThanOrEqualTo>
                            <ogc:PropertyIsLessThanOrEqualTo>
                                <ogc:PropertyName>magnitude</ogc:PropertyName>
                                <ogc:Literal>5.0</ogc:Literal>
                            </ogc:PropertyIsLessThanOrEqualTo>
                        </ogc:And>
                    </ogc:Filter>
                    <sld:PointSymbolizer>
                        <sld:Graphic>
                            <sld:Mark>
                            <WellKnownName>circle</WellKnownName>
                                <sld:Fill>
                                    <sld:CssParameter name="fill">#FF9900</sld:CssParameter>
                                    <sld:CssParameter name="fill-opacity">1.0</sld:CssParameter>
                                </sld:Fill>
                                <sld:Stroke/>
                            </sld:Mark>
                            <sld:Size>10.0</sld:Size>
                        </sld:Graphic>
                    </sld:PointSymbolizer>
                </sld:Rule>
                <sld:Rule>
                    <sld:Name>Greater than 5.0</sld:Name>
                    <sld:Title>Greater than 5.0</sld:Title>
                    <ogc:Filter>
                        <ogc:And>
                            <ogc:PropertyIsGreaterThanOrEqualTo>
                                <ogc:PropertyName>magnitude</ogc:PropertyName>
                                <ogc:Literal>5.0</ogc:Literal>
                            </ogc:PropertyIsGreaterThanOrEqualTo>
                            <ogc:PropertyIsLessThanOrEqualTo>
                                <ogc:PropertyName>magnitude</ogc:PropertyName>
                                <ogc:Literal>10.1</ogc:Literal>
                            </ogc:PropertyIsLessThanOrEqualTo>
                        </ogc:And>
                    </ogc:Filter>
                    <sld:PointSymbolizer>
                        <sld:Graphic>
                            <sld:Mark>
                            <WellKnownName>circle</WellKnownName>
                                <sld:Fill>
                                    <sld:CssParameter name="fill">#FF0000</sld:CssParameter>
                                    <sld:CssParameter name="fill-opacity">1.0</sld:CssParameter>
                                </sld:Fill>
                                <sld:Stroke/>
                            </sld:Mark>
                            <sld:Size>12.0</sld:Size>
                        </sld:Graphic>
                    </sld:PointSymbolizer>
                </sld:Rule>
            </sld:FeatureTypeStyle>
        </sld:UserStyle>
    </sld:UserLayer>
</sld:StyledLayerDescriptor>

