<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" 
	xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 
	xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" 
	xmlns:xlink="http://www.w3.org/1999/xlink" 
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
<!--
This document is used by AbstractLegendGraphicOutputFormatTest.testExternalGraphic.
-->
    <UserStyle>
        <Name>SymbolSize</Name>
        <Title>Default</Title>
        <Abstract></Abstract>
        <FeatureTypeStyle>                        
            <Rule>
                <Name>Rule</Name>
                <Abstract>Abstract</Abstract>
                <Title>title</Title>
                <LegendGraphic>
            		<Graphic>
              			<ExternalGraphic>
                			<OnlineResource xlink:type="simple" xlink:href="bridge.png"/>
                			<Format>image/png</Format>
              			</ExternalGraphic>
            		</Graphic>
          		</LegendGraphic>
            </Rule>
        </FeatureTypeStyle>
    </UserStyle>
   </StyledLayerDescriptor>
