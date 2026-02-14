<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.opengis.net/sld
http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd" version="1.0.0">
<UserLayer>
        <Name>cqltest</Name>
        <LayerFeatureConstraints>
            <FeatureTypeConstraint/>
        </LayerFeatureConstraints>
        <UserStyle>
                <Name>cqltest</Name>
                <Title>CQL test</Title>
                <Abstract>CQL test</Abstract>
                <FeatureTypeStyle>
                        <Rule>
                                <RasterSymbolizer>
                                    <Opacity>1.0</Opacity>
                                    
                                    <ColorMap type="intervals">
                                        <ColorMapEntry color="#000000" quantity="10" opacity="1.0" label="label1"/>
                                        <ColorMapEntry color="#A80000" quantity="20" opacity="1.0" label="label2"/>
                                        <ColorMapEntry color="#FF0000" quantity="30" opacity="1.0" label="This is a very long label used to test proportional margins"/>
                                    </ColorMap>

                                </RasterSymbolizer>
                        </Rule>
                </FeatureTypeStyle>
        </UserStyle>
</UserLayer>
</StyledLayerDescriptor>

