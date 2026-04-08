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
                        <sld:ColorMap>
                           <sld:ColorMapEntry color="#0000FF" quantity="1.0" label="1"/>
                           <sld:ColorMapEntry color="#FFFF00" quantity="124.81173566700335" label="124.811736"/>
                           <sld:ColorMapEntry color="#FF7F00" quantity="559.2041949413946" label="559.204195"/>
                           <sld:ColorMapEntry color="#FF0000" quantity="55537.0" label="55537"/>
                         </sld:ColorMap>
                        <sld:ContrastEnhancement/>
                        <VendorOption name="labelInFeatureInfo">add</VendorOption>
                        <VendorOption name="labelAttributeName">custom name</VendorOption>
                    </sld:RasterSymbolizer>
                </sld:Rule>
            </sld:FeatureTypeStyle>
        </sld:UserStyle>
    </sld:NamedLayer>
</sld:StyledLayerDescriptor>
