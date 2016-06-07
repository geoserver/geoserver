<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.opengis.net/sld
http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd" version="1.0.0">
<UserLayer>
        <Name>rainfallda</Name>
        <LayerFeatureConstraints>
            <FeatureTypeConstraint/>
        </LayerFeatureConstraints>
        <UserStyle>
                <Name>rainfallda1m</Name>
                <Title>Rainfall 1 month</Title>
                <Abstract>A default style for rainfall diff with avg 1month</Abstract>
                <FeatureTypeStyle>
                        <Rule>
                                <RasterSymbolizer>
                                    <Opacity>1.0</Opacity>
                                    <OverlapBehavior>
                                       <AVERAGE/>
                                    </OverlapBehavior>
                                    <ColorMap type="intervals">
<ColorMapEntry color="#732600" quantity="9888" opacity="1.0" label=""/>
<ColorMapEntry color="#A80000" quantity="9931" opacity="1.0" label=""/>
<ColorMapEntry color="#FF0000" quantity="9951" opacity="1.0" label=""/>
<ColorMapEntry color="#FFA77F" quantity="9981" opacity="1.0" label=""/>
<ColorMapEntry color="#CCCCCC" quantity="9990" opacity="1.0" label=""/>
<ColorMapEntry color="#FFEBAF" quantity="10011" opacity="1.0" label=""/>
<ColorMapEntry color="#D1FF73" quantity="10016" opacity="1.0" label=""/>
<ColorMapEntry color="#98E600" quantity="10021" opacity="1.0" label=""/>
<ColorMapEntry color="#55FF00" quantity="10031" opacity="1.0" label=""/>
<ColorMapEntry color="#38A800" quantity="10050" opacity="1.0" label=""/>
<ColorMapEntry color="#267300" quantity="10101" opacity="1.0" label=""/>
<ColorMapEntry color="#267300" quantity="49999" opacity="1.0" label=""/>
<ColorMapEntry color="#00007F" quantity="Infinity" opacity="0.0" label=""/>


                                    </ColorMap>

                                </RasterSymbolizer>
                        </Rule>
                </FeatureTypeStyle>
        </UserStyle>
</UserLayer>
</StyledLayerDescriptor>

