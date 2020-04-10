<StyledLayerDescriptor version="1.0.0" xmlns="http://www.opengis.net/sld" xmlns:st="http://www.stations.org/1.0"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd">
    <NamedLayer>
        <Name>certain</Name>
        <UserStyle>
            <FeatureTypeStyle>
                <Rule>
                    <Filter>
                        <PropertyIsEqualTo>
                            <PropertyName>st:name</PropertyName>
                            <Literal>station 2</Literal>
                        </PropertyIsEqualTo>
                    </Filter>
                    <Name>certain</Name>
                    <PointSymbolizer>
                        <Graphic>
                            <Mark>
                                <WellKnownName>circle</WellKnownName>
                                <Fill>
                                    <CssParameter name="fill">#00FF00</CssParameter>
                                </Fill>
                            </Mark>
                            <Size>20</Size>
                        </Graphic>
                    </PointSymbolizer>
                </Rule>
                <Rule>
                    <Filter>
                        <PropertyIsEqualTo>
                            <PropertyName>st:name</PropertyName>
                            <Literal>station 4</Literal>
                        </PropertyIsEqualTo>
                    </Filter>
                    <Name>certain</Name>
                    <PointSymbolizer>
                        <Graphic>
                            <Mark>
                                <WellKnownName>circle</WellKnownName>
                                <Fill>
                                    <CssParameter name="fill">#0000FF</CssParameter>
                                </Fill>
                            </Mark>
                            <Size>40</Size>
                        </Graphic>
                    </PointSymbolizer>
                </Rule>
                <VendorOption name="sortBy">st:name D</VendorOption>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>