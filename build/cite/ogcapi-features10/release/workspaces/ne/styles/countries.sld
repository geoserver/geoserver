<?xml version="1.0" encoding="UTF-8"?>
<sld:StyledLayerDescriptor xmlns="http://www.opengis.net/sld" xmlns:sld="http://www.opengis.net/sld" xmlns:gml="http://www.opengis.net/gml" xmlns:ogc="http://www.opengis.net/ogc" version="1.0.0">
  <sld:NamedLayer>
    <sld:Name>ne:countries</sld:Name>
    <sld:UserStyle>
      <sld:Name>countries_transparent</sld:Name>
      <sld:Title>Countries</sld:Title>
      <sld:Abstract>Alternate on mapcolor9 theme for ne:countries layer. This presentation uses a slight transparencyto allow background to show through as part of a layer group or map. Labeling is done at a lower priority than populated places to allow cities to take precedence</sld:Abstract>
      <sld:FeatureTypeStyle>
        <sld:Rule>
          <sld:Name>Countries</sld:Name>
          <sld:PolygonSymbolizer>
            <sld:Fill>
              <sld:CssParameter name="fill">#8DD3C7</sld:CssParameter>
            </sld:Fill>
            <sld:VendorOption name="inclusion">legendOnly</sld:VendorOption>
          </sld:PolygonSymbolizer>
        </sld:Rule>
        <sld:Rule>
          <sld:PolygonSymbolizer>
            <sld:Fill>
              <sld:CssParameter name="fill">
                <ogc:Function name="Recode">
                  <ogc:PropertyName>MAPCOLOR9</ogc:PropertyName>
                  <ogc:Literal>1</ogc:Literal>
                  <ogc:Literal>#8dd3c7</ogc:Literal>
                  <ogc:Literal>2</ogc:Literal>
                  <ogc:Literal>#ffffb3</ogc:Literal>
                  <ogc:Literal>3</ogc:Literal>
                  <ogc:Literal>#bebada</ogc:Literal>
                  <ogc:Literal>4</ogc:Literal>
                  <ogc:Literal>#fb8072</ogc:Literal>
                  <ogc:Literal>5</ogc:Literal>
                  <ogc:Literal>#80b1d3</ogc:Literal>
                  <ogc:Literal>6</ogc:Literal>
                  <ogc:Literal>#fdb462</ogc:Literal>
                  <ogc:Literal>7</ogc:Literal>
                  <ogc:Literal>#b3de69</ogc:Literal>
                  <ogc:Literal>8</ogc:Literal>
                  <ogc:Literal>#fccde5</ogc:Literal>
                  <ogc:Literal>9</ogc:Literal>
                  <ogc:Literal>#d9d9d9</ogc:Literal>
                </ogc:Function>
              </sld:CssParameter>
              <sld:CssParameter name="fill-opacity">0.75</sld:CssParameter>
            </sld:Fill>
            <sld:VendorOption name="inclusion">mapOnly</sld:VendorOption>
          </sld:PolygonSymbolizer>
        </sld:Rule>
        
        <sld:Rule>
          <ogc:Filter>
            <ogc:PropertyIsLessThan>
              <ogc:PropertyName>MIN_ZOOM</ogc:PropertyName>
              <ogc:Literal>2</ogc:Literal>
            </ogc:PropertyIsLessThan>
          </ogc:Filter>
          <sld:MinScaleDenominator>14.0E7</sld:MinScaleDenominator>
          <sld:TextSymbolizer>
            <sld:Label>
              <ogc:Function name="if_then_else">
                <ogc:Function name="lessThan">
                  <ogc:PropertyName>NAME_LEN</ogc:PropertyName>
                  <ogc:Literal>8</ogc:Literal>
                </ogc:Function>
                <ogc:Function name="Recode">
                  <ogc:Function name="language"/>
                  <ogc:Literal/>
                  <ogc:PropertyName>NAME</ogc:PropertyName>
                  <ogc:Literal>en</ogc:Literal>
                  <ogc:PropertyName>NAME</ogc:PropertyName>
                  <ogc:Literal>it</ogc:Literal>
                  <ogc:PropertyName>NAME_IT</ogc:PropertyName>
                  <ogc:Literal>fr</ogc:Literal>
                  <ogc:PropertyName>NAME_FR</ogc:PropertyName>
                </ogc:Function>
                <ogc:PropertyName>ABBREV</ogc:PropertyName>
              </ogc:Function>
            </sld:Label>
            <sld:Font>
              <sld:CssParameter name="font-family">SansSerif</sld:CssParameter>
              <sld:CssParameter name="font-size">10</sld:CssParameter>
            </sld:Font>
            <sld:LabelPlacement>
              <sld:PointPlacement>
                <sld:AnchorPoint>
                  <sld:AnchorPointX>0.5</sld:AnchorPointX>
                  <sld:AnchorPointY>0.5</sld:AnchorPointY>
                </sld:AnchorPoint>
              </sld:PointPlacement>
            </sld:LabelPlacement>
            <sld:Fill>
              <CssParameter name="fill">#777777</CssParameter>
            </sld:Fill>
            <sld:Priority>
              <ogc:Sub>
                <ogc:Literal>50</ogc:Literal>
                <ogc:PropertyName>LABELRANK</ogc:PropertyName>
              </ogc:Sub>
            </sld:Priority>
            <sld:VendorOption name="maxDisplacement">20</sld:VendorOption>
            <sld:VendorOption name="spaceAround">8</sld:VendorOption>
            <sld:VendorOption name="charSpacing">1</sld:VendorOption>
            <sld:VendorOption name="autoWrap">70</sld:VendorOption>
            <sld:VendorOption name="goodnessOfFit">0.95</sld:VendorOption>
            <sld:VendorOption name="inclusion">mapOnly</sld:VendorOption>
          </sld:TextSymbolizer>
        </sld:Rule>
        <sld:Rule>
          <ogc:Filter>
            <ogc:PropertyIsLessThan>
              <ogc:PropertyName>MIN_ZOOM</ogc:PropertyName>
              <ogc:Literal>3</ogc:Literal>
            </ogc:PropertyIsLessThan>
          </ogc:Filter>
          <sld:MinScaleDenominator>7.0E7</sld:MinScaleDenominator>
          <sld:MaxScaleDenominator>14.0E7</sld:MaxScaleDenominator>
          <sld:TextSymbolizer>
            <sld:Label>
              <ogc:Function name="Recode">
                <ogc:Function name="language"/>
                <ogc:Literal/>
                <ogc:PropertyName>NAME</ogc:PropertyName>
                <ogc:Literal>en</ogc:Literal>
                <ogc:PropertyName>NAME</ogc:PropertyName>
                <ogc:Literal>it</ogc:Literal>
                <ogc:PropertyName>NAME_IT</ogc:PropertyName>
                <ogc:Literal>fr</ogc:Literal>
                <ogc:PropertyName>NAME_FR</ogc:PropertyName>
              </ogc:Function>
            </sld:Label>
            <sld:Font>
              <sld:CssParameter name="font-family">SansSerif</sld:CssParameter>
              <sld:CssParameter name="font-size">12</sld:CssParameter>
            </sld:Font>
            <sld:LabelPlacement>
              <sld:PointPlacement>
                <sld:AnchorPoint>
                  <sld:AnchorPointX>0.5</sld:AnchorPointX>
                  <sld:AnchorPointY>0.5</sld:AnchorPointY>
                </sld:AnchorPoint>
              </sld:PointPlacement>
            </sld:LabelPlacement>
            <sld:Fill>
              <CssParameter name="fill">#777777</CssParameter>
            </sld:Fill>
            <sld:Priority>
              <ogc:Sub>
                <ogc:Literal>5</ogc:Literal>
                <ogc:PropertyName>LABELRANK</ogc:PropertyName>
              </ogc:Sub>
            </sld:Priority>
            <sld:VendorOption name="maxDisplacement">20</sld:VendorOption>
            <sld:VendorOption name="spaceAround">8</sld:VendorOption>
            <sld:VendorOption name="charSpacing">1</sld:VendorOption>
            <sld:VendorOption name="autoWrap">70</sld:VendorOption>
            <sld:VendorOption name="goodnessOfFit">0.95</sld:VendorOption>
            <sld:VendorOption name="inclusion">mapOnly</sld:VendorOption>
          </sld:TextSymbolizer>
        </sld:Rule>
        <sld:Rule>
          <ogc:Filter>
            <ogc:PropertyIsLessThan>
              <ogc:PropertyName>MIN_ZOOM</ogc:PropertyName>
              <ogc:Literal>50</ogc:Literal>
            </ogc:PropertyIsLessThan>
          </ogc:Filter>
          <sld:MinScaleDenominator>3.5E7</sld:MinScaleDenominator>
          <sld:MaxScaleDenominator>7.0E7</sld:MaxScaleDenominator>
          <sld:TextSymbolizer>
            <sld:Label>
              <ogc:Function name="Recode">
                <ogc:Function name="language"/>
                <ogc:Literal/>
                <ogc:PropertyName>NAME</ogc:PropertyName>
                <ogc:Literal>en</ogc:Literal>
                <ogc:PropertyName>NAME</ogc:PropertyName>
                <ogc:Literal>it</ogc:Literal>
                <ogc:PropertyName>NAME_IT</ogc:PropertyName>
                <ogc:Literal>fr</ogc:Literal>
                <ogc:PropertyName>NAME_FR</ogc:PropertyName>
              </ogc:Function>
            </sld:Label>
            <sld:Font>
              <sld:CssParameter name="font-family">SansSerif</sld:CssParameter>
              <sld:CssParameter name="font-size">14</sld:CssParameter>
              <sld:CssParameter name="font-weight">bold</sld:CssParameter>
            </sld:Font>
            <sld:LabelPlacement>
              <sld:PointPlacement>
                <sld:AnchorPoint>
                  <sld:AnchorPointX>0.5</sld:AnchorPointX>
                  <sld:AnchorPointY>0.5</sld:AnchorPointY>
                </sld:AnchorPoint>
              </sld:PointPlacement>
            </sld:LabelPlacement>
            <sld:Fill>
              <CssParameter name="fill">#777777</CssParameter>
            </sld:Fill>
            <sld:Priority>
              <ogc:Sub>
                <ogc:Literal>50</ogc:Literal>
                <ogc:PropertyName>LABELRANK</ogc:PropertyName>
              </ogc:Sub>
            </sld:Priority>
            <sld:VendorOption name="maxDisplacement">40</sld:VendorOption>
            <sld:VendorOption name="spaceAround">8</sld:VendorOption>
            <sld:VendorOption name="charSpacing">1</sld:VendorOption>
            <sld:VendorOption name="autoWrap">90</sld:VendorOption>
            <sld:VendorOption name="goodnessOfFit">1.0</sld:VendorOption>
            <sld:VendorOption name="inclusion">mapOnly</sld:VendorOption>
          </sld:TextSymbolizer>
        </sld:Rule>
        <sld:Rule>
          <sld:MaxScaleDenominator>3.5E7</sld:MaxScaleDenominator>
          <sld:TextSymbolizer>
            <sld:Label>
              <ogc:Function name="Recode">
                <ogc:Function name="language"/>
                <ogc:Literal/>
                <ogc:PropertyName>NAME</ogc:PropertyName>
                <ogc:Literal>en</ogc:Literal>
                <ogc:PropertyName>NAME</ogc:PropertyName>
                <ogc:Literal>it</ogc:Literal>
                <ogc:PropertyName>NAME_IT</ogc:PropertyName>
                <ogc:Literal>fr</ogc:Literal>
                <ogc:PropertyName>NAME_FR</ogc:PropertyName>
              </ogc:Function>
            </sld:Label>
            <sld:Font>
              <sld:CssParameter name="font-family">SansSerif</sld:CssParameter>
              <sld:CssParameter name="font-size">16</sld:CssParameter>
              <sld:CssParameter name="font-weight">bold</sld:CssParameter>
            </sld:Font>
            <sld:LabelPlacement>
              <sld:PointPlacement>
                <sld:AnchorPoint>
                  <sld:AnchorPointX>0.5</sld:AnchorPointX>
                  <sld:AnchorPointY>0.5</sld:AnchorPointY>
                </sld:AnchorPoint>
              </sld:PointPlacement>
            </sld:LabelPlacement>
            <sld:Fill>
              <CssParameter name="fill">#777777</CssParameter>
            </sld:Fill>
            <sld:Priority>
              <ogc:Sub>
                <ogc:Literal>50</ogc:Literal>
                <ogc:PropertyName>LABELRANK</ogc:PropertyName>
              </ogc:Sub>
            </sld:Priority>
            <sld:VendorOption name="maxDisplacement">50</sld:VendorOption>
            <sld:VendorOption name="spaceAround">10</sld:VendorOption>
            <sld:VendorOption name="charSpacing">1</sld:VendorOption>
            <sld:VendorOption name="autoWrap">100</sld:VendorOption>
            <sld:VendorOption name="goodnessOfFit">1.00</sld:VendorOption>
            <sld:VendorOption name="inclusion">mapOnly</sld:VendorOption>
          </sld:TextSymbolizer>
        </sld:Rule>            
        
      </sld:FeatureTypeStyle>
    </sld:UserStyle>
  </sld:NamedLayer>
</sld:StyledLayerDescriptor>
