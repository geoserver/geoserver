# Point symbolizer

The point symbolizer is used to style point features or centroids of non-point features.

## Syntax

The full syntax of a point symbolizer is:

```yaml
symbolizers:
- point:
    symbols:
    - external:
        url: <text>
        format: <text>
    - mark:
        shape: <shape>
        fill-color: <color>
        fill-opacity: <expression>
        fill-graphic: 
          <graphic_options>
        stroke-color: <color>
        stroke-width: <expression>
        stroke-opacity: <expression>
        stroke-linejoin: <expression>
        stroke-linecap: <expression>
        stroke-dasharray: <float list>
        stroke-dashoffset: <expression>
        stroke-graphic: 
          <graphic_options>
        stroke-graphic-fill: 
          <graphic_options>
    size: <expression>
    anchor: <tuple>
    displacement: <tuple>
    opacity: <expression>
    rotation: <expression>
    geometry: <expression>
    uom: <text>
    x-labelObstacle: <boolean>
    x-composite-base: <boolean>
    x-composite: <text>
    x-inclusion: <text>
```

where:

| Property | Required? | Description | Default value |
|----|----|----|----|
| `stroke-color` | No | Color of line features. | `'#000000'` (black) |
| `stroke-width` | No | Width of line features, measured in pixels. | `1` |
| `stroke-opacity` | No | Opacity of line features. Valid values are a decimal value between `0` (completely transparent) and `1` (completely opaque). | `1` |
| `stroke-linejoin` | No | How line segments are joined together. Options are `mitre` (sharp corner), `round` (round corner), and `bevel` (diagonal corner). | `mitre` |
| `stroke-linecap` | No | How line features are rendered at their ends. Options are `butt` (sharp square edge), `round` (rounded edge), and `square` (slightly elongated square edge). | `butt` |
| `stroke-dasharray` | No | A numeric list signifying length of lines and gaps, creating a dashed effect. Units are pixels, so `"2 3"` would be a repeating pattern of 2 pixels of drawn line followed by 3 pixels of blank space. If only one number is supplied, this will mean equal amounts of line and gap. | No dash |
| `stroke-dashoffset` | No | Number of pixels into the dasharray to offset the drawing of the dash, used to shift the location of the lines and gaps in a dash. | `0` |
| `stroke-graphic` | No | A design or pattern to be used along the stroke. Output will always be a linear repeating pattern, and as such is not tied to the value of `stroke-width`. Can either be a mark consisting of a common shape or a URL that points to a graphic. The `<graphic_options>` should consist of a mapping containing `symbols:` followed by an `external:` or `mark:`, with appropriate parameters as detailed in the [Point symbolizer](../point.md) section. Cannot be used with `stroke-graphic-fill`. | N/A |
| `stroke-graphic-fill` | No | A design or pattern to be used for the fill of the stroke. The area that is to be filled is tied directly to the value of `stroke-width`. Can either be a mark consisting of a common shape or a URL that points to a graphic. The `<graphic_options>` should consist of a mapping containing `symbols:` followed by an `external:` or `mark:`, with appropriate parameters as detailed in the [Point symbolizer](../point.md) section. Cannot be used with `stroke-graphic`. | N/A |

| Property | Required? | Description | Default value |
|----|----|----|----|
| `fill-color` | No | Color of inside of features. | `'#808080'` (gray) |
| `fill-opacity` | No | Opacity of the fill. Valid values are a decimal value between `0` (completely transparent) and `1` (completely opaque). | `1` |
| `fill-graphic` | No | A design or pattern to be used for the fill of the feature. Can either be a mark consisting of a common shape or a URL that points to a graphic. The `<graphic_options>` should consist of a mapping containing `symbols:` followed by an `external:` or `mark:`, with appropriate parameters as detailed in the [Point symbolizer](../point.md) section. | None |

The use of `fill-graphic` allows for the following extra options:

| Property | Required? | Description | Default value |
|----|----|----|----|
| `x-graphic-margin` | No | Used to specify margins (in pixels) around the graphic used in the fill. Possible values are a list of four (`top, right, bottom, left`), a list of three (`top, right and left, bottom`), a list of two (`top and bottom, right and left`), or a single value. | N/A |
| `x-random` | No | Activates random distribution of symbols. Possible values are `free` or `grid`. `free` generates a completely random distribution, and `grid` will generate a regular grid of positions, and only randomize the position of the symbol around the cell centers, providing a more even distribution. | N/A |
| `x-random-tile-size` | No | When used with `x-random`, determines the size of the grid (in pixels) that will contain the randomly distributed symbols. | `256` |
| `x-random-rotation` | No | When used with `x-random`, activates random symbol rotation. Possible values are `none` or `free`. | `none` |
| `x-random-symbol-count` | No | When used tih `x-random`, determines the number of symbols drawn. Increasing this number will generate a more dense distribution of symbols | `16` |
| `x-random-seed` | No | Determines the "seed" used to generate the random distribution. Changing this value will result in a different symbol distribution. | `0` |

<!-- admonition follows -->


<!-- admonition follows -->

!!! info "Todo"
    Add examples using random

| Property | Required? | Description | Default value |
|----|----|----|----|
| `external` | No | Specifies an image to use to style the point. | N/A |
| `url` | Yes | Location of the image. Can either be an actual URL or a file path (relative to where the style file is saved in the GeoServer data directory). Should be enclosed in single quotes. | N/A |
| `format` | Yes | Format of the image. Must be a valid MIME type (such as `image/png` for PNG, `image/jpeg` for JPG, `image/svg+xml` for SVG) | N/A |
| `mark` | No | Specifies a regular shape to use to style the point. | N/A |
| `shape` | No | Shape of the mark. Options are `square`, `circle`, `triangle`, `cross`, `x`, and `star`. | `square` |
| `size` | No | Size of the mark in pixels. If the aspect ratio of the mark is not 1:1 (square), will apply to the *height* of the graphic only, with the width scaled proportionally. | 16 |
| `anchor` | No | Specify the center of the symbol relative to the feature location. Value is an `[x,y]` tuple with decimal values from 0-1, with `[0,0]` meaning that the symbol is anchored to the top left, and `[1,1]` meaning anchored to bottom right. | `[0.5,0.5]` |
| `displacement` | No | Specifies a distance to which to move the symbol relative to the feature. Value is an `[x,y]` tuple with values expressed in pixels, so [10,5] will displace the symbol 10 pixels to the right and 5 pixels down. | `[0,0]` |
| `opacity` | No | Specifies the level of transparency. Value of `0` means entirely transparent, while `1` means entirely opaque. Only affects graphics referenced by the `external` parameter; the opacity of `mark` symbols is controlled by the `fill-opacity` and `stroke-opacity` of the mark. | `1` |
| `rotation` | No | Value (in degrees) or rotation of the mark. Larger values increase counter-clockwise rotation. A value of `180` will make the mark upside-down. | `0` |

| Property | Required? | Description | Default value |
|----|----|----|----|
| `geometry` | No | Specifies which attribute to use as the geometry (see [Geometry transformations in SLD](../../../../sld/extensions/geometry-transformations.md)) | First geometry attribute found (usually named `geom` or `the_geom`) |
| `uom` | No | Unit of measure used for width calculations (see [Specifying symbolizer sizes in ground units](../../../../sld/extensions/uom.md)) | pixel |

The following properties are equivalent to SLD "vendor options".

Additional "vendor options" property for [Label Obstacles](../../../../sld/extensions/label-obstacles.md):

| Property | Required? | Description | Default value |
|----|----|----|----|
| `x-labelObstacle` | No | Marks the symbolizer as an obstacle such that labels drawn via a [text symbolizer](../text.md) will not be drawn over top of these features. Options are `true` or `false`. Note that the bounding boxes of features are used when calculating obstacles, so unintended effects may occur when marking a line or polygon symbolizer as an obstacle. | `false` |

Additional "vendor options" properties for [Color compositing and color blending](../../../sld/extensions/composite-blend/index.md):

Additional "vendor options" properties for [Color compositing and color blending](../../../../sld/extensions/composite-blend/index.md):

| Property | Required? | Description | Default value |
|----|----|----|----|
| `x-composite` | No | Allows for both alpha compositing and color blending options between symbolizers. | N/A |
| `x-composite-base` | No | Allows the rendering engine to use the symbolizer mapping to define a "base" buffer for subsequent compositing and blending using `x-composite`. See the section on [Feature Styles](../../featurestyles.md#ysld_reference_featurestyles_composite) for more details. | `false` |

Additional "vendor options" properties for [Rendering Selection](../../../sld/extensions/rendering-selection.md):

Additional "vendor options" properties for [Rendering Selection](../../../../sld/extensions/rendering-selection.md):

| Property | Required? | Description | Default value |
|----|----|----|----|
| `x-inclusion` | No | Define if rule should be included in style for `legendOnly` or `mapOnly`. | `normal` |

## Examples

### Basic point

A point symbolizer draws a point at the center of any geometry. It is defined by an external image or a symbol, either of which can be sized and rotated. A mark is a pre-defined symbol that can be drawn at the location of a point. Similar to polygons, marks have both a fill and a stroke. This example shows a point symbolizer that draws semi-transparent red diamonds with black outlines:

```yaml
feature-styles:
- name: name
  rules:
  - title: red point
    symbolizers:
    - point:
        symbols:
        - mark:
            shape: square
            fill-color: '#FF0000'
            fill-opacity: 0.75
            stroke-color: '#000000'
            stroke-width: 1.5
            stroke-opacity: 1
        size: 20
        rotation: 45
```

![](img/point_basic.png)

*Basic point*

### Point as image

Sometimes it may be useful to use an image to represent certain points. This can be accomplished using the `external` symbol property, which requires a `url` and a `format`. The `url` should be enclosed in single quotes. The `format` property is a [MIME type image](http://en.wikipedia.org/wiki/Internet_media_type#Type_image). This example shows a point symbolizer that draws an image centered on each point:

```yaml
name: point
feature-styles:
- name: name
  rules:
  - symbolizers:
    - point:
        symbols:
        - external:
            url: 'geoserver.png'
            format: image/png
        size: 16
```

![](img/point_graphic.png)

*Point as image*

### Point composition

Using more than one point symbolizer allows the composition of more complex symbology. This example shows two symbolizers along with the `x-composite` parameter in order to *subtract* a shape from a square mark, allowing the background to show through.

``` yaml
symbolizers:
- point:
    symbols:
    - mark:
        shape: square
        fill-color: '#222222'
    size: 40
- point:
    symbols:
    - external:
        url: 'stamp.png'
        format: image/png
    x-composite: xor
    size: 40
```

![](img/point_composition.png)

*Point composition*

### Points as arrow heads

Sometimes it is useful to generate a point using a CQL expression. The following example generates a point at the end of each line in the shape of an arrow, rotated such that it matches the orientation of the line.

``` yaml
name: arrow
symbolizers:
- line:
   stroke-color: '#808080'
   stroke-width: 3
- point:
    geometry: ${endPoint(the_geom)}
    symbols:
    - mark:
        shape: shape://oarrow
        fill-color: '#808080'
    size: 30
    rotation: ${endAngle(the_geom)}
```

![](img/arrow.png)

*Point as arrow head*
