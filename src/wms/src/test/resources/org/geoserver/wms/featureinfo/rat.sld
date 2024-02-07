<?xml version="1.0" encoding="UTF-8"?>
<sld:StyledLayerDescriptor xmlns="http://www.opengis.net/sld" xmlns:sld="http://www.opengis.net/sld" xmlns:gml="http://www.opengis.net/gml" xmlns:ogc="http://www.opengis.net/ogc" version="1.0.0">
    <sld:NamedLayer>
        <sld:UserStyle>
            <sld:FeatureTypeStyle>
                <sld:Rule>
                    <sld:MinScaleDenominator>1</sld:MinScaleDenominator>
                    <sld:MaxScaleDenominator>1000000</sld:MaxScaleDenominator>
                    <sld:RasterSymbolizer>
                        <sld:ColorMap >
                            <sld:ColorMapEntry color="#000000" quantity="1"/>
                            <sld:ColorMapEntry color="#FF0000" quantity="2" />
                        </sld:ColorMap>
                        <sld:VendorOption name="addAttributeTable">true</sld:VendorOption>
                    </sld:RasterSymbolizer>
                </sld:Rule>
            </sld:FeatureTypeStyle>
        </sld:UserStyle>
    </sld:NamedLayer>
</sld:StyledLayerDescriptor>