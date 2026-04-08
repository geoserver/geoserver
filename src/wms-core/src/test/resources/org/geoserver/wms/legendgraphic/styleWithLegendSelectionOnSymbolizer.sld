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
      <sld:Name>tl 2010 08013 arealm</sld:Name>
      <sld:Title/>
      <sld:FeatureTypeStyle>
        <sld:Name>group 0</sld:Name>
        <sld:FeatureTypeName>Feature</sld:FeatureTypeName>
        <sld:SemanticTypeIdentifier>generic:geometry</sld:SemanticTypeIdentifier>
        <sld:SemanticTypeIdentifier>simple</sld:SemanticTypeIdentifier>
        <sld:Rule>
          <sld:Name>nationalpark</sld:Name>
          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
              <ogc:PropertyName>MTFCC</ogc:PropertyName>
              <ogc:Literal>K2181</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
          <PolygonSymbolizer>
          						<Fill>
          							<CssParameter name="fill">#0099cc</CssParameter>
          						</Fill>
          						<Stroke>
          							<CssParameter name="stroke">#000000</CssParameter>
          							<CssParameter name="stroke-width">0.5</CssParameter>
          						</Stroke>
          					</PolygonSymbolizer>
          					<PolygonSymbolizer>
          						<Fill>
          							<GraphicFill>
          								<Graphic>
          									<Mark>
          										<WellKnownName>circle</WellKnownName>
          										<Fill>
          											<CssParameter name="fill">#ffffff</CssParameter>
          										</Fill>
          									</Mark>
          									<Size>4</Size>
          									<Rotation>
          										<ogc:Mul>
          											<ogc:PropertyName>rotation</ogc:PropertyName>
          											<ogc:Literal>-1</ogc:Literal>
          										</ogc:Mul>
          									</Rotation>
          									<Opacity>0.4</Opacity>
          								</Graphic>
          							</GraphicFill>
          						</Fill>
          						<Stroke>
          							<GraphicStroke>
          								<Graphic>
          									<Mark>
          										<WellKnownName>square</WellKnownName>
          										<Fill>
          											<CssParameter name="fill">#ffff00</CssParameter>
          										</Fill>
          									</Mark>
          									<Size>6</Size>
          									<Rotation>
          										<ogc:Mul>
          											<ogc:PropertyName>rotation</ogc:PropertyName>
          											<ogc:Literal>-1</ogc:Literal>
          										</ogc:Mul>
          									</Rotation>
          									<Opacity>0.4</Opacity>
          								</Graphic>
          							</GraphicStroke>
          						</Stroke>
          						<VendorOption name="inclusion">mapOnly</VendorOption>
          					</PolygonSymbolizer>
        </sld:Rule>
        
      </sld:FeatureTypeStyle>
    </sld:UserStyle>
  </sld:UserLayer>
</sld:StyledLayerDescriptor>