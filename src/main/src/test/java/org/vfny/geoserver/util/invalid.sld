<StyledLayerDescriptor version="1.0.0" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc">
 <NamedLayer>
  <Name>sf:states</Name>
  <UserStyle>
   <Name>UserSelection</Name>
   <FeatureTypeStyle>
    <Rule>
     <ogc:Filter xmlns:gml="http://www.opengis.net/gml">
      <ogc:PropertyIsEqualTo>
       <ogc:PropertyName>STATE_ABBR</ogc:PropertyName>
       <ogc:Literal>IL</ogc:Literal>
      </ogc:PropertyIsEqualTo>
     </ogc:Filter>
     <PolygonSymbolizer>
      <Fill>
       <CssParameter name="fill">#FF0000</CssParameter>
      </Fill>
      <Font/> <!-- invalid -->
     </PolygonSymbolizer>
    </Rule>
    <Rule>
     <LineSymbolizer>
      <Stroke/>
     </LineSymbolizer>
    </Rule>
   </FeatureTypeStyle>
  </UserStyle>
 </NamedLayer>
</StyledLayerDescriptor>