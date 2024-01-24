<StyledLayerDescriptor xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd"
  version="1.0.0">
  <NamedLayer>
    <Name>ExternalReferenceGraphic</Name>
    <UserStyle>
      <Name>ExternalReferenceGraphic</Name>
      <Title>ExternalReferenceGraphic</Title>
      <FeatureTypeStyle>
        <Name>ExternalReferenceGraphic</Name>
        <Rule>
        <Title>Red flag</Title>
          <PointSymbolizer>
            <Graphic>
              <ExternalGraphic>
                 <OnlineResource 
                    xlink:type="simple"
                    xlink:href="ExternalGraphicIcon.png" />
               <Format>image/png</Format>
             </ExternalGraphic>
             <Size>25</Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>
