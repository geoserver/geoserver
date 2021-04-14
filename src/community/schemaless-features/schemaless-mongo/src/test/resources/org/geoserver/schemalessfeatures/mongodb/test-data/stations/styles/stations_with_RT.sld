<StyledLayerDescriptor version="1.0.0" xmlns="http://www.opengis.net/sld" xmlns:st="http://www.stations.org/1.0"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xmlns:ogc="http://www.opengis.net/ogc"
                       xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd">
    <NamedLayer>
        <Name>certain</Name>
        <UserStyle>
            <FeatureTypeStyle>
            <Transformation>
                        <ogc:Function name="vec:GroupCandidateSelection">
                          <ogc:Function name="parameter">
                            <ogc:Literal>data</ogc:Literal>
                          </ogc:Function>
                          <ogc:Function name="parameter">
                             <ogc:Literal>operationAttribute</ogc:Literal>
                             <ogc:Literal>numericAttribute</ogc:Literal>
                          </ogc:Function>
                          <ogc:Function name="parameter">
                            <ogc:Literal>aggregation</ogc:Literal>
                            <ogc:Literal>MIN</ogc:Literal>
                          </ogc:Function>
                          <ogc:Function name="parameter">
                              <ogc:Literal>groupingAttributes</ogc:Literal>
                              <ogc:Literal>groupAttribute</ogc:Literal>
                              <ogc:Literal>groupAttribute2</ogc:Literal>
                          </ogc:Function>
                        </ogc:Function>
                      </Transformation>
                <Rule>
                    <Name>certain</Name>

                    <PointSymbolizer>
                        <Graphic>
                            <Mark>
                                <WellKnownName>circle</WellKnownName>
                                <Fill>
                                    <CssParameter name="fill">#0000FF</CssParameter>
                                </Fill>
                            </Mark>
                            <Size>40</Size>
                        </Graphic>
                    </PointSymbolizer>
                </Rule>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>