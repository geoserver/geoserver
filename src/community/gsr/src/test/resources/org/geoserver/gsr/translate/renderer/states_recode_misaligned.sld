<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
                       xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd"
                       xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
                       xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

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
                <ogc:Function name="Recode">
                  <!-- Value to transform -->
                  <ogc:PropertyName>SUB_REGION</ogc:PropertyName>

                  <!-- Map of input to output values -->
                  <ogc:Literal>N Eng</ogc:Literal>
                  <ogc:Literal>#6495ED</ogc:Literal>

                  <ogc:Literal>Mid Atl</ogc:Literal>
                  <ogc:Literal>#B0C4DE</ogc:Literal>

                  <ogc:Literal>S Atl</ogc:Literal>
                  <ogc:Literal>#00FFFF</ogc:Literal>

                  <ogc:Literal>E N Cen</ogc:Literal>
                  <ogc:Literal>#9ACD32</ogc:Literal>

                  <ogc:Literal>E S Cen</ogc:Literal>
                  <ogc:Literal>#00FA9A</ogc:Literal>

                  <ogc:Literal>W N Cen</ogc:Literal>
                  <ogc:Literal>#FFF8DC</ogc:Literal>

                  <ogc:Literal>W S Cen</ogc:Literal>
                  <ogc:Literal>#F5DEB3</ogc:Literal>

                  <ogc:Literal>Mtn</ogc:Literal>
                  <ogc:Literal>#F4A460</ogc:Literal>

                  <ogc:Literal>Pacific</ogc:Literal>
                  <ogc:Literal>#87CEEB</ogc:Literal>
                </ogc:Function>
              </CssParameter>
            </Fill>
            <Stroke>
              <CssParameter name="stroke-width">
                <ogc:Function name="Recode">
                  <!-- Value to transform -->
                  <ogc:PropertyName>SUB_REGION</ogc:PropertyName>

                  <!-- Map of input to output values -->
                  <ogc:Literal>A</ogc:Literal>
                  <ogc:Literal>0</ogc:Literal>

                  <ogc:Literal>B</ogc:Literal>
                  <ogc:Literal>1</ogc:Literal>

                  <ogc:Literal>C</ogc:Literal>
                  <ogc:Literal>2</ogc:Literal>

                  <ogc:Literal>D</ogc:Literal>
                  <ogc:Literal>3</ogc:Literal>

                  <ogc:Literal>E</ogc:Literal>
                  <ogc:Literal>4</ogc:Literal>

                  <ogc:Literal>F</ogc:Literal>
                  <ogc:Literal>5</ogc:Literal>

                  <ogc:Literal>G</ogc:Literal>
                  <ogc:Literal>6</ogc:Literal>

                  <ogc:Literal>H</ogc:Literal>
                  <ogc:Literal>7</ogc:Literal>

                  <ogc:Literal>I</ogc:Literal>
                  <ogc:Literal>8</ogc:Literal>
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
