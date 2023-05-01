<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0"  xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"   xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"   xmlns:xlink="http://www.w3.org/1999/xlink"  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <UserStyle>
        <Name>Default Styler</Name>
        <Title>Default Styler</Title>
        <Abstract/>
        <FeatureTypeStyle>
            <FeatureTypeName>Feature</FeatureTypeName>
            <Rule>
                <Name>title</Name>
                <Title>Type1</Title>
                <Filter>
                    <ogc:PropertyIsEqualTo>
                        <ogc:PropertyName>Type</ogc:PropertyName>
                        <ogc:Literal>1</ogc:Literal>
                    </ogc:PropertyIsEqualTo>
                </Filter>
                <PointSymbolizer>
                    <Graphic>
                        <Mark>
                            <WellKnownName>circle</WellKnownName>
                            <Stroke>
                                <CssParameter name="stroke">#000000</CssParameter>
                            </Stroke>
                        </Mark>
                        <Size>6</Size>
                        <Opacity>1</Opacity>
                    </Graphic>
                </PointSymbolizer>
                <TextSymbolizer>
                    <Label>
                        <Function name="strConcat">
                            <Literal>ADDRESS: </Literal>
                            <PropertyName>ADDRESS</PropertyName>                            
                        </Function>
                    </Label>
                </TextSymbolizer>
            </Rule>
            <Rule>
                <Name>title</Name>
                <Title>Type2</Title>
                <Filter>
                    <ogc:PropertyIsEqualTo>
                        <ogc:PropertyName>Type</ogc:PropertyName>
                        <ogc:Literal>2</ogc:Literal>
                    </ogc:PropertyIsEqualTo>
                </Filter>
                <PointSymbolizer>
                    <Graphic>
                        <Mark>
                            <WellKnownName>square</WellKnownName>
                            <Stroke>
                                <CssParameter name="stroke">#000000</CssParameter>
                            </Stroke>
                        </Mark>
                        <Size>6</Size>
                        <Opacity>1</Opacity>
                    </Graphic>
                </PointSymbolizer>
                <TextSymbolizer>
                    <Label>
                        <Function name="strConcat">
                            <Literal>ADDRESS: </Literal>
                            <PropertyName>ADDRESS</PropertyName>                            
                        </Function>
                    </Label>
                </TextSymbolizer>
            </Rule>
            <Rule>
                <Name>title</Name>
                <Title>Type3</Title>
                <Filter>
                    <ogc:PropertyIsEqualTo>
                        <ogc:PropertyName>Type</ogc:PropertyName>
                        <ogc:Literal>3</ogc:Literal>
                    </ogc:PropertyIsEqualTo>
                </Filter>
                <PointSymbolizer>
                    <Graphic>
                        <Mark>
                            <WellKnownName>cross</WellKnownName>
                            <Stroke>
                                <CssParameter name="stroke">#000000</CssParameter>
                            </Stroke>
                        </Mark>
                        <Size>6</Size>
                        <Opacity>1</Opacity>
                    </Graphic>
                </PointSymbolizer>
                <TextSymbolizer>
                    <Label>
                        <Function name="strConcat">
                            <Literal>ADDRESS: </Literal>
                            <PropertyName>ADDRESS</PropertyName>                            
                        </Function>
                    </Label>
                </TextSymbolizer>
            </Rule>
            <Rule>
                <Name>title</Name>
                <Title>Type4</Title>
                <Filter>
                    <ogc:PropertyIsEqualTo>
                        <ogc:PropertyName>Type</ogc:PropertyName>
                        <ogc:Literal>4</ogc:Literal>
                    </ogc:PropertyIsEqualTo>
                </Filter>
                <PointSymbolizer>
                    <Graphic>
                        <Mark>
                            <WellKnownName>cross</WellKnownName>
                            <Stroke>
                                <CssParameter name="stroke">#000000</CssParameter>
                            </Stroke>
                        </Mark>
                        <Size>6</Size>
                        <Opacity>1</Opacity>
                    </Graphic>
                </PointSymbolizer>
                <TextSymbolizer>
                    <Label>
                        <Function name="strConcat">
                            <Literal>ADDRESS: </Literal>
                            <PropertyName>ADDRESS</PropertyName>                            
                        </Function>
                    </Label>
                </TextSymbolizer>
            </Rule>
        </FeatureTypeStyle>
    </UserStyle>
</StyledLayerDescriptor>