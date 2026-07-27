# Symbolizers

The basic unit of visualization is the symbolizer. There are five types of symbolizers: **Point**, **Line**, **Polygon**, **Raster**, and **Text**.

Symbolizers are contained inside [rules](../rules.md). A rule can contain one or many symbolizers.

!!! note
    The most common use case for multiple symbolizers is a geometry (point/line/polygon) symbolizer to draw the features plus a text symbolizer for labeling these features.
    
    ![](img/symbolizers.svg)

    *Use of multiple symbolizers*

## Drawing order

The order of symbolizers significant, and also the order of your data.

For each feature the rules are evaluated resulting in a list of symbolizers that will be used to draw that feature. The symbolizers are drawn in the order provided.

Consider the following two symbolizers:

```yaml
symbolizers:
- point:
    symbols:
    - mark:
        shape: square
        fill-color: '#FFCC00'
- point:
    symbols:
    - mark:
        shape: triangle
        fill-color: '#FF3300'
```

When drawing three points these symbolizers will be applied in order on each feature:

1.  Feature 1 is drawn as a square, followed by a triangle:

    ![](img/symbolizer-order1.svg)

    *Feature 1 buffer rendering*

2.  Feature 2 is drawn as a square, followed by a triangle. Notice the slight overlap with Feature 1:

    ![](img/symbolizer-order2.svg)

    *Feature 2 buffer rendering*

3.  Feature 3 is drawn as a square, followed by a triangle:

    ![](img/symbolizer-order3.svg)

    *Feature 3 buffer rendering*

!!! note
    In the final image, Feature 1 and Feature 2 have a slight overlap. This overlap is determined by data order which we have no control over. If you need to control the overlap review the [Feature Styles](../featurestyles.md) section on managing "z-order".
    
    ![](img/symbolizer-order4.svg)

    *Feature style controlling z-order*

## Matching symbolizers and geometries

It is common to match the symbolizer with the type of geometries contained in the layer, but this is not required. The following table illustrates what will happen when a geometry symbolizer is matched up with another type of geometry.

|  | Points | Lines | Polygon | Raster |
|----|----|----|----|----|
| Point Symbolizer | **Points** | Midpoint of the lines | Centroid of the polygons | Centroid of the raster |
| Line Symbolizer | n/a | **Lines** | Outline (stroke) of the polygons | Outline (stroke) of the raster |
| Polygon Symbolizer | n/a | Will "close" the line and style as a polygon | **Polygons** | Will "outline" the raster and style as a polygon |
| Raster Symbolizer | n/a | n/a | n/a | Transform raster values to color channels for display |
| Text Symbolizer | Label at point location | Label at midpoint of lines | Label at centroid of polygons | Label at centroid of raster outline |

## Syntax

The following is the basic syntax common to all symbolizers. Note that the contents of the block are not all expanded here and that each kind of symbolizer provides additional syntax.

```yaml
geometry: <cql>
uom: <text>
..
x-composite: <text>
x-composite-base: <boolean>
x-inclusion: <text>
```

Where:

| Property | Required? | Description | Default value |
|----|----|----|----|
| `geometry` | No | Specifies which attribute to use as the geometry (see [Geometry transformations in SLD](../../../sld/extensions/geometry-transformations.md)) | First geometry attribute found (usually named `geom` or `the_geom`) |
| `uom` | No | Unit of measure used for width calculations (see [Specifying symbolizer sizes in ground units](../../../sld/extensions/uom.md)) | pixel |

Additional "vendor options" properties for [Color compositing and color blending](../../../sld/extensions/composite-blend/index.md):

| Property | Required? | Description | Default value |
|----|----|----|----|
| `x-composite` | No | Allows for both alpha compositing and color blending options between symbolizers. | N/A |
| `x-composite-base` | No | Allows the rendering engine to use the symbolizer mapping to define a "base" buffer for subsequent compositing and blending using `x-composite`. See the section on [Feature Styles](../featurestyles.md#ysld_reference_featurestyles_composite) for more details. | `false` |

Additional "vendor options" properties for [Rendering Selection](../../../sld/extensions/rendering-selection.md):

| Property | Required? | Description | Default value |
|----|----|----|----|
| `x-inclusion` | No | Define if rule should be included in style for `legendOnly` or `mapOnly`. | `normal` |
