<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" 
	xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 
	xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" 
	xmlns:xlink="http://www.w3.org/1999/xlink" 
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
<!--
This document is used by AbstractLegendGraphicOutputFormatTest.testMixedGeometry.
This document contains styles for Polygons, Lines and Points.
-->
    <UserStyle>
        <Name>MixedGeometry</Name>
        <Title>Default Styler</Title>
        <Abstract></Abstract>
        <FeatureTypeStyle>            
            <Rule>
                <Name>Lines</Name>
                <Abstract>Abstract</Abstract>
                <Title>title</Title>
                <LineSymbolizer>
                    <Stroke>
                        <CssParameter name="stroke">
                            <ogc:Literal>#000000</ogc:Literal>
                        </CssParameter>
                        <CssParameter name="stroke-width">
                            <ogc:Literal>5</ogc:Literal>
                        </CssParameter>
                    </Stroke>
                </LineSymbolizer>
            </Rule>
            <Rule>
                <Name>Polygons</Name>
                <Abstract>Abstract</Abstract>
                <Title>title</Title>
                <PolygonSymbolizer>
                    <Fill>
                        <CssParameter name="fill">
                            <ogc:Literal>#0000FF</ogc:Literal>
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
        
            <Rule>
                <Name>Points</Name>
                <Abstract>Abstract</Abstract>
                <Title>title</Title>
                <PointSymbolizer>
                  <Graphic>
                    <Mark>
                      <WellKnownName>square</WellKnownName>
                      <Fill>
                        <CssParameter name="fill">#FF0000</CssParameter>
                      </Fill>
                    </Mark>
                  <Size>6</Size>
                </Graphic>
              </PointSymbolizer>
            </Rule>
        </FeatureTypeStyle>
    </UserStyle>
   </StyledLayerDescriptor>
