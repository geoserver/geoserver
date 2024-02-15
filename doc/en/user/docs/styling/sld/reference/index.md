# SLD Reference

The OGC **Styled Layer Descriptor (SLD)** standard defines a language for expressing styling of geospatial data. GeoServer uses SLD as its primary styling language.

SLD 1.0.0 is defined in the following specification:

-   [OGC Styled Layer Descriptor Implementation Specification, Version 1.0.0](http://portal.opengeospatial.org/files/?artifact_id=1188)

Subsequently the functionality of SLD has been split into two specifications:

-   [OGC Symbology Encoding Implementation Specification, Version 1.1.0](http://portal.opengeospatial.org/files/?artifact_id=16700)
-   [OGC Styled Layer Descriptor profile of the Web Map Service Implementation Specification, Version 1.1.0](http://portal.opengeospatial.org/files/?artifact_id=1188)

GeoServer implements the SLD 1.0.0 standard, as well as some parts of the SE 1.1.0 and WMS-SLD 1.1.0 standards.

**Elements of SLD**

The following sections describe the SLD elements implemented in GeoServer.

The root element for an SLD is `<StyledLayerDescriptor>`. It contains a **Layers** and **Styles** elements which describe how a map is to be composed and styled.

-   [StyledLayerDescriptor](sld.md)
-   [Layers](layers.md)
-   [Styles](styles.md)

Styles contain **Rules** and **Filters** to determine sets of features to be styled with specific symbology. Rules may also specify the scale range in which the feature styling is visible.

-   [Rules](rules.md)
-   [Filters](filters.md)

Rules contain **Symbolizers** to specify how features are styled. There are 5 types of symbolizers:

-   `PointSymbolizer`, which styles features as **points**
-   `LineSymbolizer`, which styles features as **lines**
-   `PolygonSymbolizer`, which styles features as **polygons**
-   `TextSymbolizer`, which styles **text labels** for features
-   `RasterSymbolizer`, which styles **raster coverages**

Each symbolizer type has its own parameters to control styling.

-   [PointSymbolizer](pointsymbolizer.md)
-   [LineSymbolizer](linesymbolizer.md)
-   [PolygonSymbolizer](polygonsymbolizer.md)
-   [TextSymbolizer](textsymbolizer.md)
-   [Labeling](labeling.md)
-   [RasterSymbolizer](rastersymbolizer.md)
