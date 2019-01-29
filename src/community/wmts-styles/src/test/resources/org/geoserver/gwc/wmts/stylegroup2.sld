<?xml version="1.0" encoding="ISO-8859-1"?>
<sld:StyledLayerDescriptor xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
                           xmlns="http://www.opengis.net/sld" xmlns:sld="http://www.opengis.net/sld"
                           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" version="1.0.0">

    <sld:NamedLayer>
        <sld:Name>DividedRoutes</sld:Name>
        <sld:UserStyle>
            <sld:FeatureTypeStyle>
                <sld:Rule>
                    <sld:LineSymbolizer>
                        <sld:Stroke>
                            <sld:CssParameter name="stroke">#FF00FF</sld:CssParameter>
                        </sld:Stroke>
                    </sld:LineSymbolizer>
                </sld:Rule>
            </sld:FeatureTypeStyle>
        </sld:UserStyle>
    </sld:NamedLayer>

    <sld:NamedLayer>
        <sld:Name>Lakes</sld:Name>
        <sld:UserStyle>
            <sld:FeatureTypeStyle>
                <sld:Rule>
                    <sld:PolygonSymbolizer>
                        <sld:Fill>
                            <sld:CssParameter name="fill">#000000</sld:CssParameter>
                        </sld:Fill>
                    </sld:PolygonSymbolizer>
                </sld:Rule>
            </sld:FeatureTypeStyle>
        </sld:UserStyle>
    </sld:NamedLayer>

</sld:StyledLayerDescriptor>