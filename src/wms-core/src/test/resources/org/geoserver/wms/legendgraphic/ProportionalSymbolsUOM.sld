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
        <Name>SymbolSize</Name>
        <Title>Default Styler</Title>
        <Abstract></Abstract>
        <FeatureTypeStyle>
            <Rule>
                <Name>Size1</Name>
                <Abstract>Abstract</Abstract>
                <Title>title</Title>
                <PointSymbolizer uom="http://www.opengeospatial.org/se/units/metre">
                  <Graphic>
                    <Mark>
                      <WellKnownName>circle</WellKnownName>
                      <Fill>
                        <CssParameter name="fill">#FF0000</CssParameter>
                      </Fill>
                    </Mark>
                  <Size>40</Size>
                </Graphic>
              </PointSymbolizer>
            </Rule>
            <Rule>
                <Name>Size2</Name>
                <Abstract>Abstract</Abstract>
                <Title>title</Title>
                <PointSymbolizer uom="http://www.opengeospatial.org/se/units/metre">
                  <Graphic>
                    <Mark>
                      <WellKnownName>circle</WellKnownName>
                      <Fill>
                        <CssParameter name="fill">#FF0000</CssParameter>
                      </Fill>
                    </Mark>
                  <Size>20</Size>
                </Graphic>
              </PointSymbolizer>
            </Rule>
            <Rule>
                <Name>Size3</Name>
                <Abstract>Abstract</Abstract>
                <Title>title</Title>
                <PointSymbolizer uom="http://www.opengeospatial.org/se/units/metre">
                  <Graphic>
                    <Mark>
                      <WellKnownName>circle</WellKnownName>
                      <Fill>
                        <CssParameter name="fill">#FF0000</CssParameter>
                      </Fill>
                    </Mark>
                  <Size>10</Size>
                </Graphic>
              </PointSymbolizer>
            </Rule>
            <Rule>
                <Name>Size4</Name>
                <Abstract>Abstract</Abstract>
                <Title>title</Title>
                <PointSymbolizer uom="http://www.opengeospatial.org/se/units/metre">
                  <Graphic>
                    <Mark>
                      <WellKnownName>circle</WellKnownName>
                      <Fill>
                        <CssParameter name="fill">#FF0000</CssParameter>
                      </Fill>
                    </Mark>
                  <Size>1</Size>
                </Graphic>
              </PointSymbolizer>
            </Rule>
        </FeatureTypeStyle>
    </UserStyle>
   </StyledLayerDescriptor>
