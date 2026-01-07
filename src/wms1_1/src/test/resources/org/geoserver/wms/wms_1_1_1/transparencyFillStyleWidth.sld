<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
                       xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">
  <NamedLayer>
    <Name>Transparency Fill</Name>
    <UserStyle>
      <Name>fill_style</Name>
      <Title>TransparencyFill style</Title>
      <Abstract>Elevation no color</Abstract>
      <FeatureTypeStyle>

        <Transformation>

          <ogc:Function name="ras:TransparencyFill">
            <ogc:Function name="parameter">
              <ogc:Literal>data</ogc:Literal>
            </ogc:Function>
            <ogc:Function name="parameter">
                 <ogc:Literal>width</ogc:Literal>
                 <ogc:Literal>4</ogc:Literal>
             </ogc:Function>
          </ogc:Function> 
        </Transformation>
        <Rule>
          <MinScaleDenominator>10000</MinScaleDenominator>
          <RasterSymbolizer>
            <Opacity>1.0</Opacity>
            <ColorMap>
                          <ColorMapEntry color="#000000" quantity="0" opacity="0.0" />
                          <ColorMapEntry color="#FF0000" quantity="1"/>
                          <ColorMapEntry color="#FF0000" quantity="500"/>
                          <ColorMapEntry color="#FF0000" quantity="800"/>
                          <ColorMapEntry color="#FF0000" quantity="1300"/>
                          <ColorMapEntry color="#FF0000" quantity="1500"/>
                          <ColorMapEntry color="#FF0000" quantity="2000"/>
                          <ColorMapEntry color="#FF0000" quantity="4000"/>
                          <ColorMapEntry color="#FF0000" quantity="5000"/>
                        </ColorMap>
          </RasterSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>
