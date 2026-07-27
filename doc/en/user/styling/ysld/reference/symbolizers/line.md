# Line symbolizer

The line symbolizer is used to style linear (1-dimensional) features. It is in some ways the simplest of the symbolizers because it only contains facilities for the stroke (outline) of a feature.

## Syntax

The full syntax of a line symbolizer is:

```yaml
symbolizers:
- line:
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
    offset: <expression>
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
| `stroke-graphic` | No | A design or pattern to be used along the stroke. Output will always be a linear repeating pattern, and as such is not tied to the value of `stroke-width`. Can either be a mark consisting of a common shape or a URL that points to a graphic. The `<graphic_options>` should consist of a mapping containing `symbols:` followed by an `external:` or `mark:`, with appropriate parameters as detailed in the [Point symbolizer](point.md) section. Cannot be used with `stroke-graphic-fill`. | N/A |
| `stroke-graphic-fill` | No | A design or pattern to be used for the fill of the stroke. The area that is to be filled is tied directly to the value of `stroke-width`. Can either be a mark consisting of a common shape or a URL that points to a graphic. The `<graphic_options>` should consist of a mapping containing `symbols:` followed by an `external:` or `mark:`, with appropriate parameters as detailed in the [Point symbolizer](point.md) section. Cannot be used with `stroke-graphic`. | N/A |

| Property | Required? | Description | Default value |
|----|----|----|----|
| `offset` | No | Value in pixels for moving the drawn line relative to the location of the feature. | `0` |

| Property | Required? | Description | Default value |
|----|----|----|----|
| `geometry` | No | Specifies which attribute to use as the geometry (see [Geometry transformations in SLD](../../../sld/extensions/geometry-transformations.md)) | First geometry attribute found (usually named `geom` or `the_geom`) |
| `uom` | No | Unit of measure used for width calculations (see [Specifying symbolizer sizes in ground units](../../../sld/extensions/uom.md)) | pixel |

Additional "vendor options" property for [Label Obstacles](../../../sld/extensions/label-obstacles.md):

| Property | Required? | Description | Default value |
|----|----|----|----|
| `x-labelObstacle` | No | Marks the symbolizer as an obstacle such that labels drawn via a [text symbolizer](text.md) will not be drawn over top of these features. Options are `true` or `false`. Note that the bounding boxes of features are used when calculating obstacles, so unintended effects may occur when marking a line or polygon symbolizer as an obstacle. | `false` |

Additional "vendor options" properties for [Color compositing and color blending](../../../sld/extensions/composite-blend/index.md):

| Property | Required? | Description | Default value |
|----|----|----|----|
| `x-composite` | No | Allows for both alpha compositing and color blending options between symbolizers. | N/A |
| `x-composite-base` | No | Allows the rendering engine to use the symbolizer mapping to define a "base" buffer for subsequent compositing and blending using `x-composite`. See the section on [Feature Styles](../featurestyles.md#ysld_reference_featurestyles_composite) for more details. | `false` |

Additional "vendor options" properties for [Rendering Selection](../../../sld/extensions/rendering-selection.md):

| Property | Required? | Description | Default value |
|----|----|----|----|
| `x-inclusion` | No | Define if rule should be included in style for `legendOnly` or `mapOnly`. | `normal` |

## Examples

### Basic line with styled ends

The `linejoin` and `linecap` properties can be used to style the joins and ends of any stroke. This example draws lines with partially transparent black lines with rounded ends and sharp (mitred) corners:

```yaml
feature-styles:
- rules:
  - symbolizers:
    - line:
        stroke-color: '#000000'
        stroke-width: 8
        stroke-opacity: 0.5
        stroke-linejoin: mitre
        stroke-linecap: round
```

![](img/line_basic.png)

*Basic line with styled ends*

### Railroad pattern

!!! info "Todo"
    Fix this example

Many maps use a hatched pattern to represent railroads. This can be accomplished by using two line symbolizers, one solid and one dashed. Specifically, the `stroke-dasharray` property is used to create a dashed line of length 1 every 24 pixels:

```yaml
name: railroad
feature-styles:
- name: name
  rules:
  - symbolizers:
    - line:
        stroke-color: '#000000'
        stroke-width: 1
    - line:
        stroke-color: '#000000'
        stroke-width: 12
        stroke-dasharray: '1 24'
```

![](img/line_railroad.png)

*Railroad pattern*

### Specifying sizes in units

The units for `stroke-width`, `size`, and other similar attributes default to pixels, meaning that graphics remain a constant size at different zoom levels. Alternately, units (feet or meters) can be specified for values, so graphics will scale as you zoom in or out. This example draws roads with a fixed width of 8 meters:

```yaml
feature-styles:
- rules:
  - symbolizers:
    - line:
        stroke-color: '#000000'
        stroke-width: '8 m'
```

![](img/line_uomsmall.png)

*Line width measured in meters (zoomed out)*

![](img/line_uombig.png)

*Line width measured in meters (zoomed in)*

The default unit of measure for the symbolizer is defined using `uom`. This example uses a default of meters to supply distances for `stroke-width` and `stroke-dasharray` using meters.

```yaml
line:
  uom: metre
  stroke-color: '#000000'
  stroke-width: '8'
  stroke-dasharray: '20 3'
```

![](img/line-uom.png)

*Line width and spacing in meters*
