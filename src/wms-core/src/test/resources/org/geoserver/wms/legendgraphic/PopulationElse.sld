<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0" xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
                       xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
                       xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <NamedLayer>
    <Name>A Test Layer</Name>
    <title>The title of the layer</title>
    <abstract>
      A styling layer used for the unit tests of sldstyler
    </abstract>
    <UserStyle>
      <Name>population</Name>
      <Title>Population in the United States</Title>
      <Abstract>A sample filter that filters the United States into three
        categories of population, drawn in different colors
      </Abstract>
      <FeatureTypeStyle>
        <Rule>
          <Name>2-4M</Name>
          <Title>2M - 4M</Title>
          <ogc:Filter xmlns:gml="http://www.opengis.net/gml">
            <PropertyIsBetween>
              <PropertyName>PERSONS</PropertyName>
              <LowerBoundary>
                <Literal>2000000</Literal>
              </LowerBoundary>
              <UpperBoundary>
                <Literal>4000000</Literal>
              </UpperBoundary>
            </PropertyIsBetween>
          </ogc:Filter>
          <PolygonSymbolizer>
            <Fill>
              <CssParameter name="fill">#FF0000</CssParameter>
            </Fill>
          </PolygonSymbolizer>
        </Rule>
        <Rule>
          <Title>&lt; 2M</Title>
          <Filter xmlns:gml="http://www.opengis.net/gml">
            <PropertyIsLessThan>
              <PropertyName>PERSONS</PropertyName>
              <Literal>2000000</Literal>
            </PropertyIsLessThan>
          </Filter>
          <PolygonSymbolizer>
            <Fill>
              <CssParameter name="fill">#00FF00</CssParameter>
            </Fill>
          </PolygonSymbolizer>
        </Rule>
        <Rule>
          <Title>Others</Title>
          <ElseFilter/>
          <PolygonSymbolizer>
            <Fill>
              <!-- CssParameters allowed are fill (the color) and fill-opacity -->
              <CssParameter name="fill">#0000FF</CssParameter>
            </Fill>
          </PolygonSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>

