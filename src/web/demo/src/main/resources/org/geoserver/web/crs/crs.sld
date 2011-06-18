<?xml version="1.0" encoding="UTF-8"?>
<sld:StyledLayerDescriptor xmlns:sld="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
  xmlns:gml="http://www.opengis.net/gml" version="1.0.0">
  <sld:UserLayer>
    <sld:LayerFeatureConstraints>
      <sld:FeatureTypeConstraint />
    </sld:LayerFeatureConstraints>
    <sld:UserStyle>
      <sld:Name>Default Styler</sld:Name>
      <sld:Title>Default polygon style</sld:Title>
      <sld:Abstract />
      <sld:FeatureTypeStyle>
        <sld:Name>simple</sld:Name>
        <sld:Title>title</sld:Title>
        <sld:Abstract>abstract</sld:Abstract>
        <sld:FeatureTypeName>Feature</sld:FeatureTypeName>
        <sld:SemanticTypeIdentifier>generic:geometry</sld:SemanticTypeIdentifier>
        <sld:SemanticTypeIdentifier>simple</sld:SemanticTypeIdentifier>
        <sld:Rule>
          <sld:Name>name</sld:Name>
          <sld:Title>title</sld:Title>
          <sld:Abstract>Abstract</sld:Abstract>
          <sld:MaxScaleDenominator>1.7976931348623157E308</sld:MaxScaleDenominator>
          <sld:PolygonSymbolizer>
            <sld:Fill>
              <sld:CssParameter name="fill">
                <ogc:Literal>#FF0000</ogc:Literal>
              </sld:CssParameter>
              <sld:CssParameter name="fill-opacity">
                <ogc:Literal>0.5</ogc:Literal>
              </sld:CssParameter>
            </sld:Fill>
            <!--sld:Stroke>
              <sld:CssParameter name="stroke">
                <ogc:Literal>#818181</ogc:Literal>
              </sld:CssParameter>
              <sld:CssParameter name="stroke-width">
                <ogc:Literal>1</ogc:Literal>
              </sld:CssParameter>
            </sld:Stroke-->
          </sld:PolygonSymbolizer>
        </sld:Rule>
      </sld:FeatureTypeStyle>
    </sld:UserStyle>
  </sld:UserLayer>
</sld:StyledLayerDescriptor>

