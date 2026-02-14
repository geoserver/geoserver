<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0" xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
    xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <!-- a Named Layer is the basic building block of an SLD document -->
    <NamedLayer>
        <Name>default_line</Name>
        <UserStyle>
            <FeatureTypeStyle>
                <Transformation>
                    <ogc:Function name="ras:Contour">
                        <ogc:Function name="parameter">
                            <ogc:Literal>data</ogc:Literal>
                        </ogc:Function>
                        <ogc:Function name="parameter">
                            <!--  anything above sea level -->
                            <ogc:Literal>levels</ogc:Literal>
                            <ogc:Literal>1.0</ogc:Literal>
                            <ogc:Literal>100.0</ogc:Literal>
                        </ogc:Function>
                    </ogc:Function>
                </Transformation>
                <Rule>
                    <LineSymbolizer>
                        <Fill>
                            <CssParameter name="stroke">#FF0000</CssParameter>
                            <CssParameter name="stroke-opacity">0.5</CssParameter>
                        </Fill>
                        <Stroke />
                    </LineSymbolizer>
                </Rule>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>