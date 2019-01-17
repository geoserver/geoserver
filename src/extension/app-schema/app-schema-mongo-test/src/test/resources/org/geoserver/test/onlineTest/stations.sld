<StyledLayerDescriptor version="1.0.0" xmlns="http://www.opengis.net/sld" xmlns:st="http://www.stations.org/1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd">
    <NamedLayer>
        <Name>certain</Name>
        <UserStyle>
            <FeatureTypeStyle>
                <Rule>
                    <Filter>
                        <PropertyIsEqualTo>
                            <PropertyName>st:measurement/st:Measurement/st:values/st:Value/st:value</PropertyName>
                            <Literal>1015</Literal>
                        </PropertyIsEqualTo>
                    </Filter>
                    <Name>certain</Name>
                    <PointSymbolizer>
                        <Graphic>
                            <Mark>
                                <WellKnownName>circle</WellKnownName>
                                <Fill>
                                    <CssParameter name="fill">#FF0000</CssParameter>
                                </Fill>
                            </Mark>
                            <Size>5</Size>
                        </Graphic>
                    </PointSymbolizer>
                    <TextSymbolizer>
                        <Label>
                            <PropertyName>st:name</PropertyName>
                        </Label>
                        <Fill>
                            <CssParameter name="fill">#FF0000</CssParameter>
                        </Fill>
                    </TextSymbolizer>
                </Rule>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>