<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0"
                       xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
                       xmlns="http://www.opengis.net/sld"
                       xmlns:ogc="http://www.opengis.net/ogc"
                       xmlns:xlink="http://www.w3.org/1999/xlink"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <NamedLayer>
        <Name>default_raster</Name>
        <UserStyle>
            <Title>Default Raster</Title>
            <Abstract>A sample style that draws a raster, good for displaying imagery</Abstract>
            <FeatureTypeStyle>
                <Rule>
                    <MinScaleDenominator>70000</MinScaleDenominator>
                    <MaxScaleDenominator>300000</MaxScaleDenominator>
                    <Name>rule1</Name>
                    <Title>Opaque Raster</Title>
                    <Abstract>A raster with 100% opacity</Abstract>
                    <RasterSymbolizer>
                        <Opacity>1.0</Opacity>
                    </RasterSymbolizer>
                </Rule>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>