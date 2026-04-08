# Rendering Selection

GeoServer provides a `VendorOption` to define whether a particular element `Rule`, `FeatureTypeStyle` or `Symbolizer` should be applied to a `getLegendGraphic` output or to a `getMap` output.

This allows generating legends from the SLD that can be better looking and more expressive, without the underlying complexity of the actual rendered style. Other systems have a dedicated language to build legends instead. The advantage of using the same language is that dynamic behaviors, like rule removal based on the area being rendered, can be easily retained.

The vendor option is named `inclusion`, for example:

* `<VendorOption name="inclusion">legendOnly</VendorOption>`
* `<VendorOption name="inclusion">mapOnly</VendorOption>`

Valid values are:

* `legendOnly` — the element will be skipped when applying the style to the data to render the map.
* `mapOnly` — the element will be skipped when applying the style to the data to render the legend.
* `normal` — has the same effect as omitting the vendor option: the SLD element will be used for both map and legend.

## Examples

The following style defines two symbolizers for each rule. One is skipped when rendering the legend, and one is skipped when rendering the map.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" version="1.0.0" xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">
  <NamedLayer>
    <Name>Style example</Name>
    <UserStyle>
      <FeatureTypeStyle>
        <Rule>
          <ogc:Filter>
            <ogc:PropertyIsLessThan>
              <ogc:PropertyName>numericValue</ogc:PropertyName>
              <ogc:Literal>90</ogc:Literal>
            </ogc:PropertyIsLessThan>
          </ogc:Filter>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>circle</WellKnownName>
                <Fill>
                  <CssParameter name="fill">0xFF0000</CssParameter>
                </Fill>
              </Mark>
              <Size>32</Size>
            </Graphic>
            <VendorOption name="inclusion">mapOnly</VendorOption>
          </PointSymbolizer>
          <PointSymbolizer>
            <Graphic>
              <ExternalGraphic>
                <OnlineResource xlink:type="simple" xlink:href="my-custom-legend-icon1.svg" />
                <Format>image/svg+xml</Format>
              </ExternalGraphic>
              <Size>20</Size>
            </Graphic>
            <VendorOption name="inclusion">legendOnly</VendorOption>
          </PointSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>
```

The same behavior can also be expressed by defining the vendor options at the `FeatureTypeStyle` level.
