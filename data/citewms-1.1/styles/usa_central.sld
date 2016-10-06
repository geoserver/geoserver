<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" 
   xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 
   xmlns="http://www.opengis.net/sld" 
   xmlns:ogc="http://www.opengis.net/ogc" 
   xmlns:xlink="http://www.w3.org/1999/xlink" 
   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
<!-- a named layer is the basic building block of an sld document -->
<NamedLayer>
<Name>A Test Layer</Name>
<title>The title of the layer</title>
<abstract>
A styling layer used for the unit tests of sldstyler
</abstract>
<!-- with in a layer you have Named Styles -->
<UserStyle>
    <!-- again they have names, titles and abstracts -->
  <Name>bbox test</Name>
    <!-- FeatureTypeStyles describe how to render different features -->
    <!-- a feature type for polygons -->
    <FeatureTypeStyle>
        <Rule>
        <!-- like a linesymbolizer but with a fill too -->
        <Filter  xmlns:gml="http://www.opengis.net/gml">
            <Not>
               <Disjoint>
                    <PropertyName>Polygons</PropertyName>
                    <gml:Box srsName="http://www.epsg.com">
                        <gml:coordinates>-110,37 -95,40</gml:coordinates>
                    </gml:Box>
                </Disjoint>
           </Not>
        </Filter>
        <PolygonSymbolizer>
           <Fill>
              <!-- CssParameters allowed are fill (the color) and fill-opacity -->
              <CssParameter name="fill">#00ff00</CssParameter>
              <CssParameter name="opacity">.5</CssParameter>
           </Fill>     
        </PolygonSymbolizer>
      </Rule>
    </FeatureTypeStyle>
</UserStyle>
</NamedLayer>
</StyledLayerDescriptor>

