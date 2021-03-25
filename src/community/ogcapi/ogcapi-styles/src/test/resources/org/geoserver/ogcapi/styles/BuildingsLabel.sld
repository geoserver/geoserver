<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0"
                       xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
                       xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
                       xmlns:xlink="http://www.w3.org/1999/xlink"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <NamedLayer>
        <Name>Buildings</Name>
        <UserStyle>
            <Name></Name>
            <FeatureTypeStyle>
                <FeatureTypeName>Feature</FeatureTypeName>
                <Rule>
                    <Name>name</Name>
                    <PolygonSymbolizer>
                        <Fill/>
                        <Stroke/>
                    </PolygonSymbolizer>
                    <TextSymbolizer>
                        <Label>
                            <ogc:PropertyName>FID</ogc:PropertyName>
                            <ogc:PropertyName>ADDRESS</ogc:PropertyName>
                            <ogc:PropertyName>DATE</ogc:PropertyName>
                            <ogc:PropertyName>YESNO</ogc:PropertyName>
                        </Label>
                        <Fill>
                            <CssParameter name="fill">#990099</CssParameter>
                        </Fill>
                    </TextSymbolizer>
                </Rule>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>
