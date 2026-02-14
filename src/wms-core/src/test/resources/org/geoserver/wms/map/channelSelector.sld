<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.opengis.net/sld
http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd" version="1.0.0">
    <NamedLayer>
        <Name>channel selector</Name>
        <UserStyle>
            <Title>channel selector</Title>
            <FeatureTypeStyle>
                <Rule>
                    <RasterSymbolizer>
                        <Opacity>1.0</Opacity>
                        <ChannelSelection>
                            <GrayChannel>
                                <SourceChannelName>
                                    <ogc:Function name="env">
                                        <ogc:Literal>band</ogc:Literal>
                                    </ogc:Function>
                                </SourceChannelName>
                            </GrayChannel>
                        </ChannelSelection>
                    </RasterSymbolizer>
                </Rule>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>
