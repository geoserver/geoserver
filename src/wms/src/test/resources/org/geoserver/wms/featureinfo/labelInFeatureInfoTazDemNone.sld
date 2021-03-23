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
                            <sld:ColorMapEntry color="#000000" opacity="0" quantity="1.0"/>
                            <sld:ColorMapEntry color="#0000FF" quantity="124.81173566700335" label="&gt;= 1 AND &lt; 124.811736"/>
                            <sld:ColorMapEntry color="#FFFF00" quantity="308.1421156004492" label="&gt;= 124.811736 AND &lt; 308.142116"/>
                            <sld:ColorMapEntry color="#FF7F00" quantity="752.1662852784135" label="&gt;= 308.142116 AND &lt; 752.166285"/>
                            <sld:ColorMapEntry color="#FF0000" quantity="55537.00000000001" label="&gt;= 752.166285 AND &lt;= 55537"/>
                        </sld:ColorMap>
                        <sld:ContrastEnhancement/>
                        <VendorOption name="labelInFeatureInfo">none</VendorOption>
                    </sld:RasterSymbolizer>
                </sld:Rule>
            </sld:FeatureTypeStyle>
        </sld:UserStyle>
    </sld:NamedLayer>
</sld:StyledLayerDescriptor>