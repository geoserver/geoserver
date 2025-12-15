<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" 
	xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 
	xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" 
	xmlns:xlink="http://www.w3.org/1999/xlink" 
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <UserStyle>
        <Name>Default Styler</Name>
        <Title>Default Styler</Title>
        <Abstract></Abstract>
        <FeatureTypeStyle>
            <FeatureTypeName>Feature</FeatureTypeName>
            <Rule>
                <Name>name</Name>
                <Abstract>Abstract</Abstract>
                <Title>title</Title>
                <PolygonSymbolizer>
                    <Fill>
                        <CssParameter name="fill">
                            <ogc:Function name="env">
                              <ogc:Literal>color</ogc:Literal>
                              <ogc:Literal>#FFFFFF</ogc:Literal>
                            </ogc:Function>
                        </CssParameter>
                    </Fill>
                    <Stroke/>
                </PolygonSymbolizer>
            </Rule>
        </FeatureTypeStyle>
    </UserStyle>
</StyledLayerDescriptor>
