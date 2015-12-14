<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" 
 xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 
 xmlns="http://www.opengis.net/sld" 
 xmlns:ogc="http://www.opengis.net/ogc" 
 xmlns:xlink="http://www.w3.org/1999/xlink" 
 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <!-- a Named Layer is the basic building block of an SLD document -->
  <NamedLayer>
    <UserStyle>
      <FeatureTypeStyle>
        <Rule>
          <LineSymbolizer>
            <Stroke>
               <GraphicStroke>
                 <Graphic>
                   <Mark>
                     <WellKnownName>circle</WellKnownName>
                     <!-- 
                     <Fill>
                       <CssParameter name="fill">#666666</CssParameter>
                     </Fill>
                      -->
                     <Stroke>
                       <CssParameter name="stroke">#333333</CssParameter>
                       <CssParameter name="stroke-width">1</CssParameter>
                     </Stroke>
                   </Mark>
                   <Size>2</Size>
                 </Graphic>
               </GraphicStroke>
              <CssParameter name="stroke-dasharray">2 2</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>

