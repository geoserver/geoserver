<StyledLayerDescriptor xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" version="1.0.0" xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd">
    <NamedLayer>
        <Name>Redacted name</Name>
        <UserStyle>
            <Title>Redacted title</Title>
            <FeatureTypeStyle>
                <Transformation>
                    <ogc:Function name="ras:Jiffle">
                        <ogc:Function name="parameter">
                            <ogc:Literal>coverage</ogc:Literal>
                        </ogc:Function>
                        <ogc:Function name="parameter">
                            <ogc:Literal>script</ogc:Literal>
                            <ogc:Literal>
                                if( src[2] > 2 ) dest = src[0];
                            </ogc:Literal>
                        </ogc:Function>
                    </ogc:Function>
                </Transformation>
                <Rule>
                    <Name>color scale</Name>
                    <RasterSymbolizer>
                        <ChannelSelection>
                            <GrayChannel>
                                <SourceChannelName>1</SourceChannelName>
                            </GrayChannel>
                        </ChannelSelection>
                        <Opacity>1.0</Opacity>
                        <ColorMap type="ramp">
                            <ColorMapEntry color="#ff0000" quantity="0"/>
                            <ColorMapEntry color="#00ff00" quantity="100" />
                            <ColorMapEntry color="#0000ff" quantity="255" />
                        </ColorMap>
                    </RasterSymbolizer>
                </Rule>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>