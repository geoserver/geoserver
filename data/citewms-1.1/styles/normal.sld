<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
<!-- a named layer is the basic building block of an sld document -->
<UserStyle>
    <!-- again they have names, titles and abstracts -->
  <Name>normal</Name>
  <Title>A simple default style</Title>
  <Abstract>A sample style that just prints out a black line for everything</Abstract>
    <!-- FeatureTypeStyles describe how to render different features -->
    <!-- a feature type for polygons -->
    <FeatureTypeStyle>
      <FeatureTypeName>Feature</FeatureTypeName>
      <Rule>
        <!-- like a linesymbolizer but with a fill too -->
        <PolygonSymbolizer>
            <Stroke/>
        </PolygonSymbolizer>
      </Rule>
    </FeatureTypeStyle>
</UserStyle>
</StyledLayerDescriptor>

