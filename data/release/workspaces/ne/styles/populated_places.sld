<?xml version="1.0" encoding="UTF-8"?>
<sld:StyledLayerDescriptor version="1.0.0" xmlns="http://www.opengis.net/sld" xmlns:gml="http://www.opengis.net/gml" xmlns:ogc="http://www.opengis.net/ogc" xmlns:sld="http://www.opengis.net/sld">
  <sld:NamedLayer>
    <sld:Name>populated_places</sld:Name>
    <sld:Title>Populated Places</sld:Title>
    <sld:Abstract>Dynamic presentation of populated places with level of detail depending on scale. Dymanic styling is used to determine mark symbol shown based on feature classification. Label is introduced at lower scales.</sld:Abstract>
    <sld:UserStyle>
      <sld:Name/>
      <sld:FeatureTypeStyle>
        <sld:Name>name</sld:Name>

        <sld:Rule>
          <ogc:Filter>
            <ogc:PropertyIsLessThan>
              <ogc:PropertyName>scalerank</ogc:PropertyName>
              <ogc:Literal>2</ogc:Literal>
            </ogc:PropertyIsLessThan>
          </ogc:Filter>
          <sld:MinScaleDenominator>14.0E7</sld:MinScaleDenominator>
          <sld:PointSymbolizer>
            <sld:Graphic>
              <sld:Mark>
                <sld:WellKnownName>circle</sld:WellKnownName>
                <sld:Fill>
                  <CssParameter name="fill">#999999</CssParameter>
                </sld:Fill>
              </sld:Mark>
              <sld:Size>2</sld:Size>
            </sld:Graphic>
          </sld:PointSymbolizer>
        </sld:Rule>
        
        <!-- dynamic symbolization using rules -->
        <sld:Rule>
          <ogc:Filter>
            <ogc:PropertyIsLessThan>
              <ogc:PropertyName>scalerank</ogc:PropertyName>
              <ogc:Literal>3</ogc:Literal>
            </ogc:PropertyIsLessThan>
          </ogc:Filter>
          <sld:MaxScaleDenominator>14.0E7</sld:MaxScaleDenominator>
          <sld:MinScaleDenominator>7.0E7</sld:MinScaleDenominator>
          <sld:PointSymbolizer>
            <sld:Graphic>
              <sld:Mark>
                <sld:WellKnownName>circle</sld:WellKnownName>
                <sld:Fill>
                  <CssParameter name="fill">#999999</CssParameter>
                </sld:Fill>
              </sld:Mark>
              <sld:Size>4</sld:Size>
            </sld:Graphic>
          </sld:PointSymbolizer>
        </sld:Rule>
        
        <sld:Rule>
          <ogc:Filter>
            <ogc:And>
              <ogc:PropertyIsLessThan>
                <ogc:PropertyName>scalerank</ogc:PropertyName>
                <ogc:Literal>3</ogc:Literal>
              </ogc:PropertyIsLessThan>
              <ogc:PropertyEquals>
                <ogc:PropertyName>featurecla</ogc:PropertyName>
                <ogc:Literal>Admin-0</ogc:Literal>
              </ogc:PropertyEquals>
            </ogc:And>
          </ogc:Filter>
          <sld:MaxScaleDenominator>14.0E7</sld:MaxScaleDenominator>
          <sld:MinScaleDenominator>7.0E7</sld:MinScaleDenominator>
          <sld:PointSymbolizer>
            <sld:Graphic>
              <sld:Mark>
                <sld:WellKnownName>star</sld:WellKnownName>
                <sld:Fill>
                  <CssParameter name="fill">#999999</CssParameter>
                </sld:Fill>
              </sld:Mark>
              <sld:Size>5</sld:Size>
            </sld:Graphic>
          </sld:PointSymbolizer>
        </sld:Rule>
        

        <!-- dynamic symbolization using expression -->
        <sld:Rule>
          <ogc:Filter>
            <ogc:PropertyIsLessThan>
              <ogc:PropertyName>scalerank</ogc:PropertyName>
              <ogc:Literal>5</ogc:Literal>
            </ogc:PropertyIsLessThan>
          </ogc:Filter>
          <sld:MinScaleDenominator>3.5E7</sld:MinScaleDenominator>
          <sld:MaxScaleDenominator>7.0E7</sld:MaxScaleDenominator>
          <sld:PointSymbolizer>
            <sld:Graphic>
              <sld:Mark>
                <sld:WellKnownName>${if_then_else(equalTo(featurecla,'Admin-0 capital'),'star','circle')}</sld:WellKnownName>
                <sld:Fill>
                  <CssParameter name="fill">#999999</CssParameter>
                </sld:Fill>
                <sld:Stroke>
                  <sld:CssParameter name="stroke-width">0.5</sld:CssParameter>
                </sld:Stroke>
              </sld:Mark>
              <sld:Size>
                <ogc:Function name="if_then_else">
                  <ogc:Function name="equalTo">
                    <ogc:PropertyName>featurecla</ogc:PropertyName>
                    <ogc:Literal>Admin-0 capital</ogc:Literal>
                  </ogc:Function>
                  <ogc:Literal>7</ogc:Literal>
                  <ogc:Literal>5</ogc:Literal>
                </ogc:Function>
              </sld:Size>
            </sld:Graphic>
            <VendorOption name="labelObstacle">true</VendorOption>
          </sld:PointSymbolizer>
        </sld:Rule>
        <sld:Rule>
          <sld:MaxScaleDenominator>3.5E7</sld:MaxScaleDenominator>
          <sld:PointSymbolizer>
            <sld:Graphic>
              <sld:Mark>
                <sld:WellKnownName>${if_then_else(equalTo(featurecla,'Admin-0 capital'),'star','circle')}</sld:WellKnownName>
                <sld:Fill>
                  <CssParameter name="fill">#999999</CssParameter>
                </sld:Fill>
                <sld:Stroke>
                  <sld:CssParameter name="stroke-width">0.75</sld:CssParameter>
                </sld:Stroke>
              </sld:Mark>
              <sld:Size>
                <ogc:Function name="if_then_else">
                  <ogc:Function name="equalTo">
                    <ogc:PropertyName>featurecla</ogc:PropertyName>
                    <ogc:Literal>Admin-0 capital</ogc:Literal>
                  </ogc:Function>
                  <ogc:Literal>8</ogc:Literal>
                  <ogc:Literal>6</ogc:Literal>
                </ogc:Function>
              </sld:Size>
            </sld:Graphic>
            <VendorOption name="labelObstacle">true</VendorOption>
          </sld:PointSymbolizer>
        </sld:Rule>

        <!-- labels -->
        <sld:Rule>
          <ogc:Filter>
            <ogc:PropertyIsLessThanOrEqualTo>
              <ogc:PropertyName>labelrank</ogc:PropertyName>
              <ogc:Literal>4</ogc:Literal>
            </ogc:PropertyIsLessThanOrEqualTo>
          </ogc:Filter>
          <sld:MinScaleDenominator>3.5E7</sld:MinScaleDenominator>
          <sld:MaxScaleDenominator>7.0E7</sld:MaxScaleDenominator>
          <sld:TextSymbolizer>
            <sld:Label>
              <ogc:PropertyName>name</ogc:PropertyName>
            </sld:Label>
            <Halo>
              <Radius>0.75</Radius>
              <Fill>
                <CssParameter name="fill">#FFFFFF</CssParameter>
                <CssParameter name="fill-opacity">0.50</CssParameter>
              </Fill>
            </Halo>
            <sld:Font>
              <sld:CssParameter name="font-family">serif</sld:CssParameter>
              <sld:CssParameter name="font-size">12</sld:CssParameter>
              <sld:CssParameter name="font-style">normal</sld:CssParameter>
              <sld:CssParameter name="font-weight">normal</sld:CssParameter>
            </sld:Font>
            <sld:LabelPlacement>
              <sld:PointPlacement>
                <sld:AnchorPoint>
                  <sld:AnchorPointX>0.5</sld:AnchorPointX>
                  <sld:AnchorPointY>1</sld:AnchorPointY>
                </sld:AnchorPoint>
                <sld:Displacement>
                  <sld:DisplacementX>0</sld:DisplacementX>
                  <sld:DisplacementY>-5</sld:DisplacementY>
                </sld:Displacement>
              </sld:PointPlacement>
            </sld:LabelPlacement>
            <sld:Fill>
              <sld:CssParameter name="fill">#000000</sld:CssParameter>
            </sld:Fill>
            <sld:Priority>10000</sld:Priority>
            <sld:VendorOption name="maxDisplacement">10</sld:VendorOption>
          </sld:TextSymbolizer>
          <sld:VendorOption name="include">mapOnly</sld:VendorOption>
        </sld:Rule>
        <sld:Rule>
          <ogc:Filter>
            <ogc:PropertyIsLessThanOrEqualTo>
              <ogc:PropertyName>labelrank</ogc:PropertyName>
              <ogc:Literal>6</ogc:Literal>
            </ogc:PropertyIsLessThanOrEqualTo>
          </ogc:Filter>
          <sld:MaxScaleDenominator>3.5E7</sld:MaxScaleDenominator>
          <sld:TextSymbolizer>
            <sld:Label>
              <ogc:PropertyName>name</ogc:PropertyName>
            </sld:Label>
            <Halo>
              <Radius>1.5</Radius>
              <Fill>
                <CssParameter name="fill">#FFFFFF</CssParameter>
                <CssParameter name="fill-opacity">0.50</CssParameter>
              </Fill>
            </Halo>
            <sld:Font>
              <sld:CssParameter name="font-family">serif</sld:CssParameter>
              <sld:CssParameter name="font-size">14</sld:CssParameter>
              <sld:CssParameter name="font-style">normal</sld:CssParameter>
              <sld:CssParameter name="font-weight">normal</sld:CssParameter>
            </sld:Font>
            <sld:LabelPlacement>
              <sld:PointPlacement>
                <sld:AnchorPoint>
                  <sld:AnchorPointX>0.5</sld:AnchorPointX>
                  <sld:AnchorPointY>1</sld:AnchorPointY>
                </sld:AnchorPoint>
                <sld:Displacement>
                  <sld:DisplacementX>0</sld:DisplacementX>
                  <sld:DisplacementY>-5</sld:DisplacementY>
                </sld:Displacement>
              </sld:PointPlacement>
            </sld:LabelPlacement>
            <sld:Fill>
              <sld:CssParameter name="fill">#000000</sld:CssParameter>
            </sld:Fill>
            <sld:Priority>100</sld:Priority>
            <sld:VendorOption name="maxDisplacement">10</sld:VendorOption>
            <VendorOption name="spaceAround">2</VendorOption>
          </sld:TextSymbolizer>
          <sld:VendorOption name="include">mapOnly</sld:VendorOption>
        </sld:Rule>
        <sld:Rule>
          <ogc:Filter>
            <ogc:PropertyIsGreaterThan>
              <ogc:PropertyName>labelrank</ogc:PropertyName>
              <ogc:Literal>6</ogc:Literal>
            </ogc:PropertyIsGreaterThan>
          </ogc:Filter>
          <sld:MaxScaleDenominator>2.0E7</sld:MaxScaleDenominator>
          <sld:TextSymbolizer>
            <sld:Label>
              <ogc:PropertyName>name</ogc:PropertyName>
            </sld:Label>
            <Halo>
              <Radius>1.5</Radius>
              <Fill>
                <CssParameter name="fill">#FFFFFF</CssParameter>
                <CssParameter name="fill-opacity">0.50</CssParameter>
              </Fill>
            </Halo>
            <sld:Font>
              <sld:CssParameter name="font-family">serif</sld:CssParameter>
              <sld:CssParameter name="font-size">14</sld:CssParameter>
              <sld:CssParameter name="font-style">normal</sld:CssParameter>
              <sld:CssParameter name="font-weight">normal</sld:CssParameter>
            </sld:Font>
            <sld:LabelPlacement>
              <sld:PointPlacement>
                <sld:AnchorPoint>
                  <sld:AnchorPointX>0.5</sld:AnchorPointX>
                  <sld:AnchorPointY>1</sld:AnchorPointY>
                </sld:AnchorPoint>
                <sld:Displacement>
                  <sld:DisplacementX>0</sld:DisplacementX>
                  <sld:DisplacementY>-5</sld:DisplacementY>
                </sld:Displacement>
              </sld:PointPlacement>
            </sld:LabelPlacement>
            <sld:Fill>
              <sld:CssParameter name="fill">#000000</sld:CssParameter>
            </sld:Fill>
            <sld:Priority>100</sld:Priority>
            <sld:VendorOption name="maxDisplacement">12</sld:VendorOption>
            <VendorOption name="spaceAround">2</VendorOption>
          </sld:TextSymbolizer>
          <sld:VendorOption name="include">mapOnly</sld:VendorOption>
        </sld:Rule>
      </sld:FeatureTypeStyle>
    </sld:UserStyle>
  </sld:NamedLayer>
</sld:StyledLayerDescriptor>