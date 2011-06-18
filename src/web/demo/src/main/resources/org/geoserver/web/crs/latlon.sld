<?xml version="1.0" encoding="UTF-8"?>
<sld:StyledLayerDescriptor xmlns:sld="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
  xmlns:gml="http://www.opengis.net/gml" version="1.0.0">
  <sld:UserLayer>
    <sld:LayerFeatureConstraints>
      <sld:FeatureTypeConstraint />
    </sld:LayerFeatureConstraints>
    <sld:UserStyle>
      <sld:Name>Default Styler</sld:Name>
      <sld:Title>Default Styler</sld:Title>
      <sld:Abstract />
      <sld:FeatureTypeStyle>
        <sld:Name>name</sld:Name>
        <sld:Title>title</sld:Title>
        <sld:Abstract>abstract</sld:Abstract>
        <sld:FeatureTypeName>Feature</sld:FeatureTypeName>
        <sld:Rule>
          <sld:Name>degree_1</sld:Name>
          <sld:Title>1 Degree line</sld:Title>
          <sld:Abstract>Abstract</sld:Abstract>
          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
              <ogc:PropertyName>level</ogc:PropertyName>
              <ogc:Literal>1</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
          <sld:MaxScaleDenominator>25000000</sld:MaxScaleDenominator>
          <sld:LineSymbolizer>
            <sld:Stroke>
              <sld:CssParameter name="stroke">
                <ogc:Literal>#F0F0F0</ogc:Literal>
              </sld:CssParameter>
              <sld:CssParameter name="stroke-width">
                <ogc:Literal>0.1</ogc:Literal>
              </sld:CssParameter>
            </sld:Stroke>
          </sld:LineSymbolizer>
        </sld:Rule>
        <sld:Rule>
          <sld:Name>degree_2</sld:Name>
          <sld:Title>10 degree line</sld:Title>
          <sld:Abstract>Abstract</sld:Abstract>
          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
              <ogc:PropertyName>level</ogc:PropertyName>
              <ogc:Literal>10</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
          <sld:MaxScaleDenominator>50000000</sld:MaxScaleDenominator>
          <sld:LineSymbolizer>
            <sld:Stroke>
              <sld:CssParameter name="stroke">
                <ogc:Literal>#F0F0F0</ogc:Literal>
              </sld:CssParameter>
              <sld:CssParameter name="stroke-width">
                <ogc:Literal>0.5</ogc:Literal>
              </sld:CssParameter>
            </sld:Stroke>
          </sld:LineSymbolizer>
        </sld:Rule>
        <sld:Rule>
          <sld:Name>degree_30</sld:Name>
          <sld:Title>30 degree line</sld:Title>
          <sld:Abstract>Abstract</sld:Abstract>
          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
              <ogc:PropertyName>level</ogc:PropertyName>
              <ogc:Literal>30</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
          <sld:LineSymbolizer>
            <sld:Stroke>
              <sld:CssParameter name="stroke">
                <ogc:Literal>#F0F0F0</ogc:Literal>
              </sld:CssParameter>
              <sld:CssParameter name="stroke-width">
                <ogc:Literal>1.0</ogc:Literal>
              </sld:CssParameter>
            </sld:Stroke>
          </sld:LineSymbolizer>
        </sld:Rule>
      </sld:FeatureTypeStyle>
    </sld:UserStyle>
  </sld:UserLayer>
</sld:StyledLayerDescriptor>
