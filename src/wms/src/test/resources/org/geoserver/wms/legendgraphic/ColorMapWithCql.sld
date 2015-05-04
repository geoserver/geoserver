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
<ColorMapEntry color="${strConcat('#FF','0000')}" quantity="10" opacity="1.0" label="&lt;-70 mm"/>
<ColorMapEntry color="#A80000" quantity="${15+5}" opacity="1.0" label="-69 - -50 mm"/>
<ColorMapEntry color="#FF0000" quantity="30" opacity="${0.25*2}" label="-49 - -20 mm"/>
                                    </ColorMap>

                                </RasterSymbolizer>
                        </Rule>
                </FeatureTypeStyle>
        </UserStyle>
</UserLayer>
</StyledLayerDescriptor>

