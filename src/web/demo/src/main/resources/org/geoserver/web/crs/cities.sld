<?xml version="1.0" encoding="UTF-8"?>
<sld:StyledLayerDescriptor xmlns:sld="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
  xmlns:gml="http://www.opengis.net/gml" version="1.0.0">
  <sld:UserLayer>
    <sld:LayerFeatureConstraints>
      <sld:FeatureTypeConstraint />
    </sld:LayerFeatureConstraints>
    <sld:UserStyle>
      <sld:Name>Cities</sld:Name>
      <sld:Title>Cities</sld:Title>
      <sld:Abstract />
      <sld:FeatureTypeStyle>
        <sld:Name>simple</sld:Name>
        <sld:Title>cities</sld:Title>
        <sld:Abstract>cities</sld:Abstract>
        <sld:FeatureTypeName>Feature</sld:FeatureTypeName>
        <sld:SemanticTypeIdentifier>generic:geometry</sld:SemanticTypeIdentifier>
        <sld:SemanticTypeIdentifier>simple</sld:SemanticTypeIdentifier>
        <sld:Rule>
          <sld:Name>simple</sld:Name>
          <sld:Title>simple labeled cities</sld:Title>
          <sld:Abstract>labeled cities</sld:Abstract>
          <sld:MaxScaleDenominator>25E6</sld:MaxScaleDenominator>
          <sld:PointSymbolizer>
            <sld:Graphic>
              <sld:Mark>
                <sld:WellKnownName>circle</sld:WellKnownName>
                <sld:Fill>
                  <sld:CssParameter name="fill">
                    <ogc:Literal>#C6982F</ogc:Literal>
                  </sld:CssParameter>
                  <sld:CssParameter name="fill-opacity">
                    <ogc:Literal>1.0</ogc:Literal>
                  </sld:CssParameter>
                </sld:Fill>
                <sld:Stroke>
                  <sld:CssParameter name="stroke">
                    <ogc:Literal>#000000</ogc:Literal>
                  </sld:CssParameter>
                  <sld:CssParameter name="stroke-opacity">
                    <ogc:Literal>1.0</ogc:Literal>
                  </sld:CssParameter>
                  <sld:CssParameter name="stroke-width">
                    <ogc:Literal>1.0</ogc:Literal>
                  </sld:CssParameter>
                </sld:Stroke>
              </sld:Mark>
              <sld:Opacity>
                <ogc:Literal>1.0</ogc:Literal>
              </sld:Opacity>
              <sld:Size>
                <ogc:Literal>6.0</ogc:Literal>
              </sld:Size>
              <sld:Rotation>
                <ogc:Literal>0.0</ogc:Literal>
              </sld:Rotation>
            </sld:Graphic>
          </sld:PointSymbolizer>
          <sld:TextSymbolizer>
            <sld:Label>
              <ogc:PropertyName>NAME</ogc:PropertyName>
            </sld:Label>
            <sld:Font>
              <sld:CssParameter name="font-family">
                <ogc:Literal>Arial</ogc:Literal>
              </sld:CssParameter>
              <sld:CssParameter name="font-size">
                <ogc:Literal>11.0</ogc:Literal>
              </sld:CssParameter>
              <sld:CssParameter name="font-style">
                <ogc:Literal>normal</ogc:Literal>
              </sld:CssParameter>
              <sld:CssParameter name="font-weight">
                <ogc:Literal>normal</ogc:Literal>
              </sld:CssParameter>
            </sld:Font>
            <sld:LabelPlacement>
              <sld:PointPlacement>
                <sld:AnchorPoint>
                  <sld:AnchorPointX>
                    <ogc:Literal>0.0</ogc:Literal>
                  </sld:AnchorPointX>
                  <sld:AnchorPointY>
                    <ogc:Literal>0.5</ogc:Literal>
                  </sld:AnchorPointY>
                </sld:AnchorPoint>
                <sld:Displacement>
                  <sld:DisplacementX>
                    <ogc:Literal>7</ogc:Literal>
                  </sld:DisplacementX>
                  <sld:DisplacementY>
                    <ogc:Literal>-2</ogc:Literal>
                  </sld:DisplacementY>
                </sld:Displacement>
                <sld:Rotation>
                  <ogc:Literal>0.0</ogc:Literal>
                </sld:Rotation>
              </sld:PointPlacement>
            </sld:LabelPlacement>
            <sld:Fill>
              <sld:CssParameter name="fill">
                <ogc:Literal>#2D4079</ogc:Literal>
              </sld:CssParameter>
              <sld:CssParameter name="fill-opacity">
                <ogc:Literal>1.0</ogc:Literal>
              </sld:CssParameter>
            </sld:Fill>
            <sld:VendorOption name="spaceAround">5</sld:VendorOption>
            <sld:VendorOption name="autoWrap">100</sld:VendorOption>
            <sld:VendorOption name="maxDisplacement">10</sld:VendorOption>
          </sld:TextSymbolizer>
        </sld:Rule>
      </sld:FeatureTypeStyle>
    </sld:UserStyle>
  </sld:UserLayer>
</sld:StyledLayerDescriptor>

