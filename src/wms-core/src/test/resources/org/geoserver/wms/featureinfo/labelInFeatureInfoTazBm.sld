<?xml version="1.0" encoding="UTF-8"?>
<sld:StyledLayerDescriptor xmlns="http://www.opengis.net/sld" xmlns:sld="http://www.opengis.net/sld" xmlns:gml="http://www.opengis.net/gml" xmlns:ogc="http://www.opengis.net/ogc" version="1.0.0">
    <sld:NamedLayer>
        <sld:Name>nurc:geotiff_coverage</sld:Name>
        <sld:UserStyle>
            <sld:Name>Default Styler</sld:Name>
            <sld:FeatureTypeStyle>
                <sld:Name>name</sld:Name>
                <sld:Rule>
                    <sld:RasterSymbolizer>
                        <sld:ChannelSelection>
                            <sld:GrayChannel>
                                <sld:SourceChannelName>1</sld:SourceChannelName>
                            </sld:GrayChannel>
                        </sld:ChannelSelection>
                        <sld:ColorMap>
                            <sld:ColorMapEntry color="#0000FF" quantity="0.0" label="0"/>
                            <sld:ColorMapEntry color="#FFFF00" quantity="13.0" label="13"/>
                            <sld:ColorMapEntry color="#FFAA00" quantity="21.0" label="21"/>
                            <sld:ColorMapEntry color="#FF5500" quantity="33.0" label="33"/>
                            <sld:ColorMapEntry color="#FF0000" quantity="145.0" label="145"/>
                        </sld:ColorMap>
                        <sld:ContrastEnhancement/>
                        <VendorOption name="labelInFeatureInfo">add</VendorOption>
                    </sld:RasterSymbolizer>
                </sld:Rule>
            </sld:FeatureTypeStyle>
        </sld:UserStyle>
    </sld:NamedLayer>
</sld:StyledLayerDescriptor>