<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0"
 xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
 xmlns="http://www.opengis.net/sld"
 xmlns:ogc="http://www.opengis.net/ogc"
 xmlns:gml="http://www.opengis.net/gml"
 xmlns:xlink="http://www.w3.org/1999/xlink"
 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

  <NamedLayer>
    <!-- Cite Streams layer -->
    <Name>Streams</Name>
    <UserStyle>
      <!-- User style, describing a 1 px blue line -->
      <FeatureTypeStyle>
        <Rule>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#0000FF</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
  <NamedLayer>
    <!-- Cite Road Segments layer -->
    <Name>RoadSegments</Name>
    <UserStyle>
      <!-- User style, describing a 2 px grey line -->
      <FeatureTypeStyle>
        <Rule>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#777777</CssParameter>
              <CssParameter name="stroke-width">2</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
    <NamedStyle>
      <!-- Default line style -->
      <Name>line</Name>
    </NamedStyle>
  </NamedLayer>
  <NamedLayer>
    <!-- Layer Group, with no style -->
    <Name>nestedLayerGroup</Name>
  </NamedLayer>
  <UserLayer>
    <!-- Inline Layer, describing a point -->
    <Name>Inline</Name>
    <InlineFeature>
      <gml:FeatureCollection>
        <gml:featureMember>
          <Dot1>
            <Type>Red_Dot</Type>
            <gml:pointProperty>
              <gml:Point srsName="http://www.opengis.net/gml/srs/epsg.xml#4326">
                <gml:coordinates>115.741666667,-64.6583333333</gml:coordinates>
              </gml:Point>
            </gml:pointProperty>
          </Dot1>
        </gml:featureMember>
      </gml:FeatureCollection>
    </InlineFeature>

    <UserStyle>
      <!-- User style, describing a red circle -->
      <FeatureTypeStyle>
        <Rule>
            <PointSymbolizer>
                <Graphic>
                    <Mark>
                        <WellKnownName>circle</WellKnownName>
                        <Fill>
                            <CssParameter name="fill">
                                <ogc:Literal>#aa0000</ogc:Literal>
                            </CssParameter>
                        </Fill>
                    </Mark>
                    <Opacity>1</Opacity>
                    <Size>4</Size>
                </Graphic>
            </PointSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </UserLayer>
</StyledLayerDescriptor>