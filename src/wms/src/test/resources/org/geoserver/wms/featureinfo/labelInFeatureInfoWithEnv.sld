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
                        <sld:ColorMap type="intervals">
                            <sld:ColorMapEntry color="#0000FF" quantity="0" label="${env('minLabel', 'LowerRange')}"/>
                            <sld:ColorMapEntry color="#FFFF00" quantity="100" label="${env('midLabel', 'MidRange')}"/>
                            <sld:ColorMapEntry color="#FF7F00" quantity="1000" label="${env('maxLabel', 'HigherRange')}"/>
                        </sld:ColorMap>
                        <sld:ContrastEnhancement/>
                        <VendorOption name="labelInFeatureInfo">add</VendorOption>
                    </sld:RasterSymbolizer>
                </sld:Rule>
            </sld:FeatureTypeStyle>
        </sld:UserStyle>
    </sld:NamedLayer>
</sld:StyledLayerDescriptor>