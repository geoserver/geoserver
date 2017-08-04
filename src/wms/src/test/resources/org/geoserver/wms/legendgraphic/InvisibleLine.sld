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
        <Name>InvisibleRaster</Name>
        <Title>Default Styler</Title>
        <Abstract></Abstract>
        <FeatureTypeStyle>            
            <Rule>
                <Name>raster</Name>
                <Abstract>Abstract</Abstract>
                <Title>title</Title>
                <MaxScaleDenominator>500</MaxScaleDenominator>
                <PolygonSymbolizer>
                  <Fill/>
                  <Stroke/>
                </PolygonSymbolizer>                    
            </Rule>
            
        </FeatureTypeStyle>
    </UserStyle>
   </StyledLayerDescriptor>
