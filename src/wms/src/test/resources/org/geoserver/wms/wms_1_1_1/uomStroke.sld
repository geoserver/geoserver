<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0" xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
<!-- a named layer is the basic building block of an sld document -->
<NamedLayer>
<Name>A Test Layer</Name>

<!-- with in a layer you have Named Styles -->
<UserStyle>
  <Name>stroke</Name>
  <Title>A stroke expressed in meters</Title>
    <FeatureTypeStyle>
      <Rule>
        <LineSymbolizer uom="http://www.opengeospatial.org/se/units/metre"> 
           <Stroke>  
             <CssParameter name="stroke">#0000FF</CssParameter> 
             <CssParameter name="stroke-width">1000</CssParameter>
           </Stroke> 
        </LineSymbolizer>
      </Rule>
    </FeatureTypeStyle>
</UserStyle>
</NamedLayer>
</StyledLayerDescriptor>

