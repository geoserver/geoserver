<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
                       xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd"
                       xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

  <NamedLayer>
    <Name>states-recode</Name>
    <UserStyle>
      <Title>A record based style</Title>
      <FeatureTypeStyle>
        <Rule>
          <Title>Colors states based on sub-region</Title>
          <PolygonSymbolizer>
            <Fill>
              <CssParameter name="fill">
                <ogc:Function name="Categorize">
                  <!-- Value to transform -->
                  <ogc:PropertyName>LAND_KM</ogc:PropertyName>

                  <!-- Output values and thresholds -->
                  <ogc:Literal>#87CEEB</ogc:Literal>
                  <ogc:Literal>100000</ogc:Literal>
                  <ogc:Literal>#FFFACD</ogc:Literal>
                  <ogc:Literal>200000</ogc:Literal>
                  <ogc:Literal>#F08080</ogc:Literal>

                </ogc:Function>
              </CssParameter>
            </Fill>
            <Stroke>
              <CssParameter name="stroke-width">
                <ogc:Function name="Categorize">
                  <!-- Value to transform -->
                  <ogc:PropertyName>LAND_KM</ogc:PropertyName>

                  <!-- Output values and thresholds -->
                  <ogc:Literal>1</ogc:Literal>
                  <ogc:Literal>100000</ogc:Literal>
                  <ogc:Literal>2</ogc:Literal>
                  <ogc:Literal>200000</ogc:Literal>
                  <ogc:Literal>3</ogc:Literal>

                </ogc:Function>
              </CssParameter>
            </Stroke>
          </PolygonSymbolizer>

        </Rule>
        <Rule>
          <Title>Label</Title>
          <TextSymbolizer>
            <Label>
              <ogc:PropertyName>STATE_ABBR</ogc:PropertyName>
            </Label>
            <Font>
              <CssParameter name="font-family">Arial</CssParameter>
              <CssParameter name="font-style">Normal</CssParameter>
              <CssParameter name="font-size">14</CssParameter>
            </Font>
            <LabelPlacement>
              <PointPlacement>
                <AnchorPoint>
                  <AnchorPointX>0.5</AnchorPointX>
                  <AnchorPointY>0.5</AnchorPointY>
                </AnchorPoint>
              </PointPlacement>
            </LabelPlacement>
          </TextSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>
