<?xml version="1.0" encoding="UTF-8"?>
<sld:StyledLayerDescriptor xmlns="http://www.opengis.net/sld"
    xmlns:sld="http://www.opengis.net/sld"
    xmlns:ogc="http://www.opengis.net/ogc"
    xmlns:gml="http://www.opengis.net/gml"
    version="1.0.0">
  <sld:NamedLayer>
    <sld:Name></sld:Name>
    <sld:UserStyle>

      <sld:FeatureTypeStyle>
        <sld:Rule>
          <sld:Name></sld:Name>
          <sld:PolygonSymbolizer>
            <sld:Fill>
              <sld:CssParameter name="fill">#8DD3C7</sld:CssParameter>
              <sld:CssParameter name="fill-opacity">0.5</sld:CssParameter>
            </sld:Fill>
          </sld:PolygonSymbolizer>
          
          <sld:TextSymbolizer>
            <sld:Geometry>
              <ogc:Function name="geomFromWKT">
                <ogc:Function name="Concatenate">
                  <ogc:Literal>POINT(</ogc:Literal>
                  <ogc:PropertyName>x</ogc:PropertyName>
                  <ogc:Literal><![CDATA[ ]]></ogc:Literal>
                  <ogc:PropertyName>y</ogc:PropertyName>
                  <ogc:Literal>)</ogc:Literal>
                </ogc:Function>
              </ogc:Function>
            </sld:Geometry>
            <sld:Label>
              <ogc:PropertyName>id</ogc:PropertyName>
            </sld:Label>
          </sld:TextSymbolizer>        
          
        </sld:Rule>
      </sld:FeatureTypeStyle>
    </sld:UserStyle>
  </sld:NamedLayer>
</sld:StyledLayerDescriptor>