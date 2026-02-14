<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor xmlns="http://www.opengis.net/sld"
                       xmlns:ogc="http://www.opengis.net/ogc"
                       xmlns:xlink="http://www.w3.org/1999/xlink"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       version="1.0.0"
                       xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd">
    <NamedLayer>
        <Name>test_layer</Name>
        <UserStyle>
            <FeatureTypeStyle>
                <Transformation>
                    <ogc:Function name="footprints"/>
                </Transformation>
                <Rule>
                    <PolygonSymbolizer/>
                </Rule>
            </FeatureTypeStyle>
            <FeatureTypeStyle>
                <Rule>
                    <RasterSymbolizer/>
                </Rule>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>