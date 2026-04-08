<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
  xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" xmlns="http://www.opengis.net/sld"
  xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
>
  <NamedLayer>
    <Name>Default Polygon</Name>
    <UserStyle>
      <FeatureTypeStyle>
        <Rule>
          <PolygonSymbolizer>
          	<Fill>
          	 <CssParameter name="fill">
          		<ogc:Literal>#AAAAAA</ogc:Literal>
          	 </CssParameter>
          	</Fill>
          <Stroke>
          	<CssParameter name="stroke">
          	  <ogc:Literal>#000000</ogc:Literal>
          	</CssParameter>
          </Stroke>
          </PolygonSymbolizer>
          <TextSymbolizer>
                 <Label>
                   <ogc:Function name="Recode">
                     <ogc:Function name="language"/>
                     <ogc:Literal/>
                     <ogc:Literal>default_lang</ogc:Literal>

                     <ogc:Literal>en</ogc:Literal>
                     <ogc:Literal>name_en</ogc:Literal>

                     <ogc:Literal>it</ogc:Literal>
                     <ogc:Literal>name_it</ogc:Literal>
          
                     <ogc:Literal>fr</ogc:Literal>
                     <ogc:Literal>name_fr</ogc:Literal>
                   </ogc:Function>
                 </Label>
                 <Fill>
                   <CssParameter name="fill">#000000</CssParameter>
                 </Fill>
         </TextSymbolizer>
        </Rule>

      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>

