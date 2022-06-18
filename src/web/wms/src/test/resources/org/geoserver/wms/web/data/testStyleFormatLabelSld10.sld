<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
   xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
   xmlns="http://www.opengis.net/sld"
   xmlns:ogc="http://www.opengis.net/ogc"
   xmlns:xlink="http://www.w3.org/1999/xlink"
   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

   <NamedLayer>
      <Name>OCEANSEA_1M:Foundation</Name>
      <UserStyle>
         <Name>GEOSYM</Name>
         <IsDefault>1</IsDefault>
         <FeatureTypeStyle>
            <Rule>
               <Name>main</Name>
               <PolygonSymbolizer>
                  <Geometry>
                     <ogc:PropertyName>GEOMETRY</ogc:PropertyName>
                  </Geometry>
                  <Fill>
                     <CssParameter name="fill">#96C3F5</CssParameter>
                  </Fill>
               </PolygonSymbolizer>
            </Rule>
         </FeatureTypeStyle>
      </UserStyle>
   </NamedLayer>

</StyledLayerDescriptor>
