<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
                       xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">
  <NamedLayer>
    <Name>normalized</Name>
    <UserStyle>
      <Name>normalized</Name>
      <Title>normalized</Title>
      <Abstract>Normalized</Abstract>
      <FeatureTypeStyle>
        <FeatureTypeName>Feature</FeatureTypeName>
        <Rule>
          <RasterSymbolizer> 
            <Opacity>1.0</Opacity>
            <ChannelSelection>
              <RedChannel>
                <SourceChannelName>
                  <ogc:Function name="env">
                    <ogc:Literal>rband</ogc:Literal>
                    <ogc:Literal>1</ogc:Literal>
                  </ogc:Function>
                </SourceChannelName>
                <ContrastEnhancement>
                  <Normalize>
                    <VendorOption name="algorithm">
                      <ogc:Function name="env">
                        <ogc:Literal>ralgo</ogc:Literal>
                        <ogc:Literal>StretchToMinimumMaximum</ogc:Literal>
                      </ogc:Function>
                    </VendorOption>
                    <VendorOption name="minValue">
                      <ogc:Function name="env">
                        <ogc:Literal>rmin</ogc:Literal>
                        <ogc:Literal>100</ogc:Literal>
                      </ogc:Function>
                    </VendorOption>
                    <VendorOption name="maxValue">
                      <ogc:Function name="env">
                        <ogc:Literal>rmax</ogc:Literal>
                        <ogc:Literal>765</ogc:Literal>
                      </ogc:Function>
                    </VendorOption>
                  </Normalize>
                </ContrastEnhancement>
              </RedChannel>
              <GreenChannel>
                <SourceChannelName>
                  <ogc:Function name="env">
                    <ogc:Literal>gband</ogc:Literal>
                    <ogc:Literal>2</ogc:Literal>
                  </ogc:Function>
                </SourceChannelName>
                <ContrastEnhancement>
                  <Normalize>
                    <VendorOption name="algorithm">
                      <ogc:Function name="env">
                        <ogc:Literal>galgo</ogc:Literal>
                        <ogc:Literal>StretchToMinimumMaximum</ogc:Literal>
                      </ogc:Function>
                    </VendorOption>
                    <VendorOption name="minValue">
                      <ogc:Function name="env">
                        <ogc:Literal>gmin</ogc:Literal>
                        <ogc:Literal>167</ogc:Literal>
                      </ogc:Function>
                    </VendorOption>
                    <VendorOption name="maxValue">
                      <ogc:Function name="env">
                        <ogc:Literal>gmax</ogc:Literal>
                        <ogc:Literal>1150</ogc:Literal>
                      </ogc:Function>
                    </VendorOption>
                  </Normalize>
                </ContrastEnhancement>
              </GreenChannel>
              <BlueChannel>
                <SourceChannelName>
                  <ogc:Function name="env">
                    <ogc:Literal>bband</ogc:Literal>
                    <ogc:Literal>3</ogc:Literal>
                  </ogc:Function>
                </SourceChannelName>
                <ContrastEnhancement>
                  <Normalize>
                    <VendorOption name="algorithm">
                      <ogc:Function name="env">
                        <ogc:Literal>balgo</ogc:Literal>
                        <ogc:Literal>StretchToMinimumMaximum</ogc:Literal>
                      </ogc:Function>
                    </VendorOption>
                    <VendorOption name="minValue">
                      <ogc:Function name="env">
                        <ogc:Literal>bmin</ogc:Literal>
                        <ogc:Literal>110</ogc:Literal>
                      </ogc:Function>
                    </VendorOption>
                    <VendorOption name="maxValue">
                      <ogc:Function name="env">
                        <ogc:Literal>bmax</ogc:Literal>
                        <ogc:Literal>1280</ogc:Literal>
                      </ogc:Function>
                    </VendorOption>
                  </Normalize>
                </ContrastEnhancement>
              </BlueChannel> 
            </ChannelSelection>
          </RasterSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>