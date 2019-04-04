<?xml version="1.0" encoding="UTF-8"?>
<sld:StyledLayerDescriptor
xmlns="http://www.opengis.net/sld"
xmlns:sld="http://www.opengis.net/sld"
xmlns:ogc="http://www.opengis.net/ogc"
xmlns:gml="http://www.opengis.net/gml"
xmlns:xlink="http://www.w3.org/1999/xlink" version="1.0.0">
  
  <sld:UserLayer>
    <sld:LayerFeatureConstraints>
      <sld:FeatureTypeConstraint/>
    </sld:LayerFeatureConstraints>
    <sld:UserStyle>
      <sld:Name>tl 2010 08013 pointlm</sld:Name>
      <sld:Title/>
      <sld:FeatureTypeStyle>
        <sld:Rule>
          <sld:Name>shopping</sld:Name>
          <ogc:Filter>
            <ogc:And>
              <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>MTFCC</ogc:PropertyName>
                <ogc:Literal>C3081</ogc:Literal>
              </ogc:PropertyIsEqualTo>
              <ogc:PropertyIsLike wildCard="*" singleChar="#" escape="!">
                <ogc:PropertyName>FULLNAME</ogc:PropertyName>
                <ogc:Literal>*Shopping*</ogc:Literal>
              </ogc:PropertyIsLike>
            </ogc:And>
          </ogc:Filter>
        <!--   <sld:MaxScaleDenominator>100000</sld:MaxScaleDenominator> -->
          <sld:PointSymbolizer>
            <sld:Graphic>
              <sld:ExternalGraphic>
                <sld:OnlineResource xlink:type="simple" xlink:href="./img/landmarks/shop_supermarket.p.16.png" />
                <sld:Format>image/png</sld:Format>
              </sld:ExternalGraphic>
            </sld:Graphic>
            <VendorOption name="labelObstacle">true</VendorOption>
          </sld:PointSymbolizer>
          <sld:TextSymbolizer>
            <sld:Label>
              <ogc:PropertyName>FULLNAME</ogc:PropertyName>
            </sld:Label>
            <sld:Font>
              <sld:CssParameter name="font-family">Arial</sld:CssParameter>
              <sld:CssParameter name="font-size">12.0</sld:CssParameter>
              <sld:CssParameter name="font-style">normal</sld:CssParameter>
              <sld:CssParameter name="font-weight">normal</sld:CssParameter>
            </sld:Font>
            <sld:LabelPlacement>
              <sld:PointPlacement>
                <sld:AnchorPoint>
                  <sld:AnchorPointX>
                    <ogc:Literal>0.5</ogc:Literal>
                  </sld:AnchorPointX>
                  <sld:AnchorPointY>
                    <ogc:Literal>1.0</ogc:Literal>
                  </sld:AnchorPointY>
                </sld:AnchorPoint>
                <sld:Displacement>
                  <sld:DisplacementX>
                    <ogc:Literal>0.0</ogc:Literal>
                  </sld:DisplacementX>
                  <sld:DisplacementY>
                    <ogc:Literal>-10.0</ogc:Literal>
                  </sld:DisplacementY>
                </sld:Displacement>
                <sld:Rotation>
                  <ogc:Literal>0.0</ogc:Literal>
                </sld:Rotation>
              </sld:PointPlacement>
            </sld:LabelPlacement>
            <sld:Halo>
              <sld:Radius>
                <ogc:Literal>1.5</ogc:Literal>
              </sld:Radius>
              <sld:Fill>
                <sld:CssParameter name="fill">#FFFFFF</sld:CssParameter>
              </sld:Fill>
            </sld:Halo>
            <sld:Fill>
              <sld:CssParameter name="fill">#000033</sld:CssParameter>
            </sld:Fill>
            <sld:Priority>200000</sld:Priority>
            <sld:VendorOption name="autoWrap">100</sld:VendorOption>
          </sld:TextSymbolizer>
        </sld:Rule>
        
        <sld:Rule>
          <sld:Name>mountains</sld:Name>
          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
              <ogc:PropertyName>MTFCC</ogc:PropertyName>
              <ogc:Literal>C3022</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
          <sld:MaxScaleDenominator>100000</sld:MaxScaleDenominator>
          <sld:PointSymbolizer>
            <sld:Graphic>
              <sld:ExternalGraphic>
                <sld:OnlineResource xlink:type="simple" xlink:href="./img/landmarks/peak.png" />
                <sld:Format>image/png</sld:Format>
              </sld:ExternalGraphic>
            </sld:Graphic>
            <VendorOption name="labelObstacle">true</VendorOption>
          </sld:PointSymbolizer>
          <sld:TextSymbolizer>
            <sld:Label>
              <ogc:PropertyName>FULLNAME</ogc:PropertyName>
            </sld:Label>
            <sld:Font>
              <sld:CssParameter name="font-family">Arial</sld:CssParameter>
              <sld:CssParameter name="font-size">12.0</sld:CssParameter>
              <sld:CssParameter name="font-style">normal</sld:CssParameter>
              <sld:CssParameter name="font-weight">normal</sld:CssParameter>
            </sld:Font>
            <sld:LabelPlacement>
              <sld:PointPlacement>
                <sld:AnchorPoint>
                  <sld:AnchorPointX>
                    <ogc:Literal>0.5</ogc:Literal>
                  </sld:AnchorPointX>
                  <sld:AnchorPointY>
                    <ogc:Literal>1.0</ogc:Literal>
                  </sld:AnchorPointY>
                </sld:AnchorPoint>
                <sld:Displacement>
                  <sld:DisplacementX>
                    <ogc:Literal>0.0</ogc:Literal>
                  </sld:DisplacementX>
                  <sld:DisplacementY>
                    <ogc:Literal>-10.0</ogc:Literal>
                  </sld:DisplacementY>
                </sld:Displacement>
                <sld:Rotation>
                  <ogc:Literal>0.0</ogc:Literal>
                </sld:Rotation>
              </sld:PointPlacement>
            </sld:LabelPlacement>
            <sld:Halo>
              <sld:Radius>
                <ogc:Literal>1.5</ogc:Literal>
              </sld:Radius>
              <sld:Fill>
                <sld:CssParameter name="fill">#FFFFFF</sld:CssParameter>
              </sld:Fill>
            </sld:Halo>
            <sld:Fill>
              <sld:CssParameter name="fill">#000033</sld:CssParameter>
            </sld:Fill>
            <sld:Priority>200000</sld:Priority>
            <sld:VendorOption name="autoWrap">100</sld:VendorOption>
          </sld:TextSymbolizer>
        </sld:Rule>
        
        <sld:Rule>
          <sld:Name>jails</sld:Name>
          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
              <ogc:PropertyName>MTFCC</ogc:PropertyName>
              <ogc:Literal>K1236</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
          <sld:MaxScaleDenominator>100000</sld:MaxScaleDenominator>
          <sld:PointSymbolizer>
            <sld:Graphic>
              <sld:ExternalGraphic>
                <sld:OnlineResource xlink:type="simple" xlink:href="./img/landmarks/amenity_prison.p.20.png" />
                <sld:Format>image/png</sld:Format>
              </sld:ExternalGraphic>
            </sld:Graphic>
            <VendorOption name="labelObstacle">true</VendorOption>
          </sld:PointSymbolizer>
          <sld:PointSymbolizer>
            <sld:Graphic>
              <sld:ExternalGraphic>
                <sld:OnlineResource xlink:type="simple" xlink:href="./img/landmarks/school.png" />
                <sld:Format>image/png</sld:Format>
              </sld:ExternalGraphic>
            </sld:Graphic>
          </sld:PointSymbolizer>
          <sld:TextSymbolizer>
            <sld:Label>
              <ogc:PropertyName>FULLNAME</ogc:PropertyName>
            </sld:Label>
            <sld:Font>
              <sld:CssParameter name="font-family">Arial</sld:CssParameter>
              <sld:CssParameter name="font-size">12.0</sld:CssParameter>
              <sld:CssParameter name="font-style">normal</sld:CssParameter>
              <sld:CssParameter name="font-weight">normal</sld:CssParameter>
            </sld:Font>
            <sld:LabelPlacement>
              <sld:PointPlacement>
                <sld:AnchorPoint>
                  <sld:AnchorPointX>
                    <ogc:Literal>0.5</ogc:Literal>
                  </sld:AnchorPointX>
                  <sld:AnchorPointY>
                    <ogc:Literal>1.0</ogc:Literal>
                  </sld:AnchorPointY>
                </sld:AnchorPoint>
                <sld:Displacement>
                  <sld:DisplacementX>
                    <ogc:Literal>0.0</ogc:Literal>
                  </sld:DisplacementX>
                  <sld:DisplacementY>
                    <ogc:Literal>-10.0</ogc:Literal>
                  </sld:DisplacementY>
                </sld:Displacement>
                <sld:Rotation>
                  <ogc:Literal>0.0</ogc:Literal>
                </sld:Rotation>
              </sld:PointPlacement>
            </sld:LabelPlacement>
            <sld:Halo>
              <sld:Radius>
                <ogc:Literal>1.5</ogc:Literal>
              </sld:Radius>
              <sld:Fill>
                <sld:CssParameter name="fill">#FFFFFF</sld:CssParameter>
              </sld:Fill>
            </sld:Halo>
            <sld:Fill>
              <sld:CssParameter name="fill">#000033</sld:CssParameter>
            </sld:Fill>
            <sld:Priority>200000</sld:Priority>
            <sld:VendorOption name="autoWrap">100</sld:VendorOption>
          </sld:TextSymbolizer>
        </sld:Rule>
        
        <sld:Rule>
          <sld:Name>governement</sld:Name>
          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
              <ogc:PropertyName>MTFCC</ogc:PropertyName>
              <ogc:Literal>K2165</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
          <sld:MaxScaleDenominator>100000</sld:MaxScaleDenominator>
          <sld:PointSymbolizer>
            <sld:Graphic>
              <sld:ExternalGraphic>
                <sld:OnlineResource xlink:type="simple" xlink:href="./img/landmarks/museum.p.16.png" />
                <sld:Format>image/png</sld:Format>
              </sld:ExternalGraphic>
            </sld:Graphic>
            <VendorOption name="labelObstacle">true</VendorOption>
          </sld:PointSymbolizer>
          <sld:TextSymbolizer>
            <sld:Label>
              <ogc:PropertyName>FULLNAME</ogc:PropertyName>
            </sld:Label>
            <sld:Font>
              <sld:CssParameter name="font-family">Arial</sld:CssParameter>
              <sld:CssParameter name="font-size">12.0</sld:CssParameter>
              <sld:CssParameter name="font-style">normal</sld:CssParameter>
              <sld:CssParameter name="font-weight">normal</sld:CssParameter>
            </sld:Font>
            <sld:LabelPlacement>
              <sld:PointPlacement>
                <sld:AnchorPoint>
                  <sld:AnchorPointX>
                    <ogc:Literal>0.5</ogc:Literal>
                  </sld:AnchorPointX>
                  <sld:AnchorPointY>
                    <ogc:Literal>1.0</ogc:Literal>
                  </sld:AnchorPointY>
                </sld:AnchorPoint>
                <sld:Displacement>
                  <sld:DisplacementX>
                    <ogc:Literal>0.0</ogc:Literal>
                  </sld:DisplacementX>
                  <sld:DisplacementY>
                    <ogc:Literal>-10.0</ogc:Literal>
                  </sld:DisplacementY>
                </sld:Displacement>
                <sld:Rotation>
                  <ogc:Literal>0.0</ogc:Literal>
                </sld:Rotation>
              </sld:PointPlacement>
            </sld:LabelPlacement>
            <sld:Halo>
              <sld:Radius>
                <ogc:Literal>1.5</ogc:Literal>
              </sld:Radius>
              <sld:Fill>
                <sld:CssParameter name="fill">#FFFFFF</sld:CssParameter>
              </sld:Fill>
            </sld:Halo>
            <sld:Fill>
              <sld:CssParameter name="fill">#000033</sld:CssParameter>
            </sld:Fill>
            <sld:Priority>200000</sld:Priority>
            <sld:VendorOption name="autoWrap">100</sld:VendorOption>
          </sld:TextSymbolizer>
        </sld:Rule>
        
        <sld:Rule>
          <sld:Name>airport</sld:Name>
          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
              <ogc:PropertyName>MTFCC</ogc:PropertyName>
              <ogc:Literal>K2451</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
          <sld:MaxScaleDenominator>100000</sld:MaxScaleDenominator>
          <VendorOption name="labelObstacle">true</VendorOption>
          <sld:PointSymbolizer>
            <sld:Graphic>
              <sld:ExternalGraphic>
                <sld:OnlineResource xlink:type="simple" xlink:href="./img/landmarks/airport.p.16.png" />
                <sld:Format>image/png</sld:Format>
              </sld:ExternalGraphic>
            </sld:Graphic>
          </sld:PointSymbolizer>
          <sld:TextSymbolizer>
            <sld:Label>
              <ogc:PropertyName>FULLNAME</ogc:PropertyName>
            </sld:Label>
            <sld:Font>
              <sld:CssParameter name="font-family">Arial</sld:CssParameter>
              <sld:CssParameter name="font-size">12.0</sld:CssParameter>
              <sld:CssParameter name="font-style">normal</sld:CssParameter>
              <sld:CssParameter name="font-weight">normal</sld:CssParameter>
            </sld:Font>
            <sld:LabelPlacement>
              <sld:PointPlacement>
                <sld:AnchorPoint>
                  <sld:AnchorPointX>
                    <ogc:Literal>0.5</ogc:Literal>
                  </sld:AnchorPointX>
                  <sld:AnchorPointY>
                    <ogc:Literal>1.0</ogc:Literal>
                  </sld:AnchorPointY>
                </sld:AnchorPoint>
                <sld:Displacement>
                  <sld:DisplacementX>
                    <ogc:Literal>0.0</ogc:Literal>
                  </sld:DisplacementX>
                  <sld:DisplacementY>
                    <ogc:Literal>-10.0</ogc:Literal>
                  </sld:DisplacementY>
                </sld:Displacement>
                <sld:Rotation>
                  <ogc:Literal>0.0</ogc:Literal>
                </sld:Rotation>
              </sld:PointPlacement>
            </sld:LabelPlacement>
            <sld:Fill>
              <sld:CssParameter name="fill">#000033</sld:CssParameter>
            </sld:Fill>
            <sld:Priority>200000</sld:Priority>
            <sld:VendorOption name="autoWrap">100</sld:VendorOption>
          </sld:TextSymbolizer>
        </sld:Rule>
        
        <sld:Rule>
          <sld:Name>school</sld:Name>
          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
              <ogc:PropertyName>MTFCC</ogc:PropertyName>
              <ogc:Literal>K2543</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
          <sld:MaxScaleDenominator>100000</sld:MaxScaleDenominator>
          <VendorOption name="labelObstacle">true</VendorOption>
          <sld:PointSymbolizer>
            <sld:Graphic>
              <sld:ExternalGraphic>
                <sld:OnlineResource xlink:type="simple" xlink:href="./img/landmarks/school.png" />
                <sld:Format>image/png</sld:Format>
              </sld:ExternalGraphic>
            </sld:Graphic>
          </sld:PointSymbolizer>
          <sld:TextSymbolizer>
            <sld:Label>
              <ogc:PropertyName>FULLNAME</ogc:PropertyName>
            </sld:Label>
            <sld:Font>
              <sld:CssParameter name="font-family">Arial</sld:CssParameter>
              <sld:CssParameter name="font-size">12.0</sld:CssParameter>
              <sld:CssParameter name="font-style">normal</sld:CssParameter>
              <sld:CssParameter name="font-weight">normal</sld:CssParameter>
            </sld:Font>
            <sld:LabelPlacement>
              <sld:PointPlacement>
                <sld:AnchorPoint>
                  <sld:AnchorPointX>
                    <ogc:Literal>0.5</ogc:Literal>
                  </sld:AnchorPointX>
                  <sld:AnchorPointY>
                    <ogc:Literal>1.0</ogc:Literal>
                  </sld:AnchorPointY>
                </sld:AnchorPoint>
                <sld:Displacement>
                  <sld:DisplacementX>
                    <ogc:Literal>0.0</ogc:Literal>
                  </sld:DisplacementX>
                  <sld:DisplacementY>
                    <ogc:Literal>-10.0</ogc:Literal>
                  </sld:DisplacementY>
                </sld:Displacement>
                <sld:Rotation>
                  <ogc:Literal>0.0</ogc:Literal>
                </sld:Rotation>
              </sld:PointPlacement>
            </sld:LabelPlacement>
            <sld:Halo>
              <sld:Radius>
                <ogc:Literal>1.5</ogc:Literal>
              </sld:Radius>
              <sld:Fill>
                <sld:CssParameter name="fill">#FFFFFF</sld:CssParameter>
              </sld:Fill>
            </sld:Halo>
            <sld:Fill>
              <sld:CssParameter name="fill">#000033</sld:CssParameter>
            </sld:Fill>
            <sld:Priority>200000</sld:Priority>
            <sld:VendorOption name="autoWrap">100</sld:VendorOption>
          </sld:TextSymbolizer>
        </sld:Rule>
        
        <sld:Rule>
          <sld:Name>cemetery</sld:Name>
          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
              <ogc:PropertyName>MTFCC</ogc:PropertyName>
              <ogc:Literal>K2582</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
          <sld:MaxScaleDenominator>100000</sld:MaxScaleDenominator>
          <sld:PointSymbolizer>
            <sld:Graphic>
              <sld:ExternalGraphic>
                <sld:OnlineResource xlink:type="simple" xlink:href="./img/landmarks/christian3.p.14.png" />
                <sld:Format>image/png</sld:Format>
              </sld:ExternalGraphic>
            </sld:Graphic>
          </sld:PointSymbolizer>
          <VendorOption name="labelObstacle">true</VendorOption>
          <sld:TextSymbolizer>
            <sld:Label>
              <ogc:PropertyName>FULLNAME</ogc:PropertyName>
            </sld:Label>
            <sld:Font>
              <sld:CssParameter name="font-family">Arial</sld:CssParameter>
              <sld:CssParameter name="font-size">12.0</sld:CssParameter>
              <sld:CssParameter name="font-style">normal</sld:CssParameter>
              <sld:CssParameter name="font-weight">normal</sld:CssParameter>
            </sld:Font>
            <sld:LabelPlacement>
              <sld:PointPlacement>
                <sld:AnchorPoint>
                  <sld:AnchorPointX>
                    <ogc:Literal>0.5</ogc:Literal>
                  </sld:AnchorPointX>
                  <sld:AnchorPointY>
                    <ogc:Literal>1.0</ogc:Literal>
                  </sld:AnchorPointY>
                </sld:AnchorPoint>
                <sld:Displacement>
                  <sld:DisplacementX>
                    <ogc:Literal>0.0</ogc:Literal>
                  </sld:DisplacementX>
                  <sld:DisplacementY>
                    <ogc:Literal>-10.0</ogc:Literal>
                  </sld:DisplacementY>
                </sld:Displacement>
                <sld:Rotation>
                  <ogc:Literal>0.0</ogc:Literal>
                </sld:Rotation>
              </sld:PointPlacement>
            </sld:LabelPlacement>
            <sld:Fill>
              <sld:CssParameter name="fill">#000033</sld:CssParameter>
            </sld:Fill>
            <sld:Priority>200000</sld:Priority>
            <sld:VendorOption name="autoWrap">100</sld:VendorOption>
          </sld:TextSymbolizer>
        </sld:Rule>
        
        <sld:Rule>
          <sld:Name>gate</sld:Name>
          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
              <ogc:PropertyName>MTFCC</ogc:PropertyName>
              <ogc:Literal>K3066</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
          <sld:MaxScaleDenominator>100000</sld:MaxScaleDenominator>
          <sld:PointSymbolizer>
            <sld:Graphic>
              <sld:ExternalGraphic>
                <sld:OnlineResource xlink:type="simple" xlink:href="./img/landmarks/gate2.png" />
                <sld:Format>image/png</sld:Format>
              </sld:ExternalGraphic>
            </sld:Graphic>
            <VendorOption name="labelObstacle">true</VendorOption>
          </sld:PointSymbolizer>
          <sld:TextSymbolizer>
            <sld:Label>
              <ogc:PropertyName>FULLNAME</ogc:PropertyName>
            </sld:Label>
            <sld:Font>
              <sld:CssParameter name="font-family">Arial</sld:CssParameter>
              <sld:CssParameter name="font-size">12.0</sld:CssParameter>
              <sld:CssParameter name="font-style">normal</sld:CssParameter>
              <sld:CssParameter name="font-weight">normal</sld:CssParameter>
            </sld:Font>
            <sld:LabelPlacement>
              <sld:PointPlacement>
                <sld:AnchorPoint>
                  <sld:AnchorPointX>
                    <ogc:Literal>0.5</ogc:Literal>
                  </sld:AnchorPointX>
                  <sld:AnchorPointY>
                    <ogc:Literal>1.0</ogc:Literal>
                  </sld:AnchorPointY>
                </sld:AnchorPoint>
                <sld:Displacement>
                  <sld:DisplacementX>
                    <ogc:Literal>0.0</ogc:Literal>
                  </sld:DisplacementX>
                  <sld:DisplacementY>
                    <ogc:Literal>-10.0</ogc:Literal>
                  </sld:DisplacementY>
                </sld:Displacement>
                <sld:Rotation>
                  <ogc:Literal>0.0</ogc:Literal>
                </sld:Rotation>
              </sld:PointPlacement>
            </sld:LabelPlacement>
            <sld:Halo>
              <sld:Radius>
                <ogc:Literal>1.5</ogc:Literal>
              </sld:Radius>
              <sld:Fill>
                <sld:CssParameter name="fill">#FFFFFF</sld:CssParameter>
              </sld:Fill>
            </sld:Halo>
            <sld:Fill>
              <sld:CssParameter name="fill">#000033</sld:CssParameter>
            </sld:Fill>
            <sld:Priority>200000</sld:Priority>
            <sld:VendorOption name="autoWrap">100</sld:VendorOption>
          </sld:TextSymbolizer>
        </sld:Rule>
        
      </sld:FeatureTypeStyle>
    </sld:UserStyle>
  </sld:UserLayer>
</sld:StyledLayerDescriptor>
