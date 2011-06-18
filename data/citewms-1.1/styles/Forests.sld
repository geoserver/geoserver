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
                        <GraphicFill>
                        <Graphic>
                            <Size>
                                <ogc:Literal>30</ogc:Literal>
                            </Size>
                            <Opacity>
                                <ogc:Literal>1.0</ogc:Literal>
                            </Opacity>
                            <Rotation>
                                <ogc:Literal>0.5</ogc:Literal>
                            </Rotation>
                            <ExternalGraphic>
                                <Format>image/png</Format>
                                <OnlineResource xlink:type="simple" xlink:href="forest_fill.png"/>
                            </ExternalGraphic>
                        </Graphic>
                        </GraphicFill>
                        <CssParameter name="fill">
                            <ogc:Literal>#808080</ogc:Literal>
                        </CssParameter>
                        <CssParameter name="fill-opacity">
                            <ogc:Literal>1.0</ogc:Literal>
                        </CssParameter>
                    </Fill>
                    <Stroke>
                        <CssParameter name="stroke">
                            <ogc:Literal>#000000</ogc:Literal>
                        </CssParameter>
                        <CssParameter name="stroke-linecap">
                            <ogc:Literal>butt</ogc:Literal>
                        </CssParameter>
                        <CssParameter name="stroke-linejoin">
                            <ogc:Literal>miter</ogc:Literal>
                        </CssParameter>
                        <CssParameter name="stroke-opacity">
                            <ogc:Literal>1</ogc:Literal>
                        </CssParameter>
                        <CssParameter name="stroke-width">
                            <ogc:Literal>1</ogc:Literal>
                        </CssParameter>
                        <CssParameter name="stroke-dashoffset">
                            <ogc:Literal>0</ogc:Literal>
                        </CssParameter>
                    </Stroke>
                </PolygonSymbolizer>
            </Rule>
        </FeatureTypeStyle>
    </UserStyle>
</StyledLayerDescriptor>
