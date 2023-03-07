<?xml version="1.0" encoding="UTF-8"?>
<sld:StyledLayerDescriptor xmlns="http://www.opengis.net/sld"
    xmlns:sld="http://www.opengis.net/sld"
    xmlns:ogc="http://www.opengis.net/ogc"
    xmlns:gml="http://www.opengis.net/gml"
    version="1.0.0">
  <sld:NamedLayer>
    <sld:Name>ne:countries</sld:Name>
    <sld:UserStyle>
      <sld:Name>countries_mapcolor_9</sld:Name>
      <sld:Title>Countries Mapcolor9</sld:Title>
      <sld:Abstract>Theme using mapcolor9 for ne:countries layer.</sld:Abstract>

      <sld:FeatureTypeStyle>
        <sld:Rule>
          <sld:Name>Countries</sld:Name>
          <sld:PolygonSymbolizer>
            <sld:Fill>
              <sld:CssParameter name="fill">#8DD3C7</sld:CssParameter>
            </sld:Fill>
            <sld:VendorOption name="inclusion">legendOnly</sld:VendorOption>
          </sld:PolygonSymbolizer>
        </sld:Rule>
        <sld:Rule>
          <sld:PolygonSymbolizer>
            <sld:Fill>
              <sld:CssParameter name="fill">
                <ogc:Function name="Recode">
                  <ogc:PropertyName>MAPCOLOR9</ogc:PropertyName>
                  <ogc:Literal>1</ogc:Literal>
                  <ogc:Literal>#8dd3c7</ogc:Literal>
                  <ogc:Literal>2</ogc:Literal>
                  <ogc:Literal>#ffffb3</ogc:Literal>
                  <ogc:Literal>3</ogc:Literal>
                  <ogc:Literal>#bebada</ogc:Literal>
                  <ogc:Literal>4</ogc:Literal>
                  <ogc:Literal>#fb8072</ogc:Literal>
                  <ogc:Literal>5</ogc:Literal>
                  <ogc:Literal>#80b1d3</ogc:Literal>
                  <ogc:Literal>6</ogc:Literal>
                  <ogc:Literal>#fdb462</ogc:Literal>
                  <ogc:Literal>7</ogc:Literal>
                  <ogc:Literal>#b3de69</ogc:Literal>
                  <ogc:Literal>8</ogc:Literal>
                  <ogc:Literal>#fccde5</ogc:Literal>
                  <ogc:Literal>9</ogc:Literal>
                  <ogc:Literal>#d9d9d9</ogc:Literal>
                </ogc:Function>
              </sld:CssParameter>
            </sld:Fill>
            <sld:VendorOption name="inclusion">mapOnly</sld:VendorOption>
          </sld:PolygonSymbolizer>
        </sld:Rule>
      </sld:FeatureTypeStyle>
    </sld:UserStyle>
  </sld:NamedLayer>
</sld:StyledLayerDescriptor>
