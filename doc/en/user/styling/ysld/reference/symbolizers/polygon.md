# Polygon symbolizer

The polygon symbolizer styles polygon (2-dimensional) features. It contains facilities for the stroke (outline) of a feature as well as the fill (inside) of a feature.

## Syntax

The full syntax of a polygon symbolizer is:

```yaml
symbolizers:
- polygon:
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
    offset: <expression>
    displacement: <expression>
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
| `offset` | No | Value in pixels for moving the drawn line relative to the location of the feature. | `0` |
| `displacement` | No | Specifies a distance to which to move the symbol relative to the feature. Value is an `[x,y]` tuple with values expressed in pixels, so [10,5] will displace the symbol 10 pixels to the right, and 5 pixels down. | `[0,0]` |

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

### Basic polygon

Polygon symbolizers have both a stroke and a fill, similar to marks for point symbolizers. The following example draws a polygon symbolizer with a red fill and black stroke with beveled line joins for the stroke:

```yaml
feature-styles:
- name: name
  rules:
  - title: fill-graphic
    symbolizers:  
    - polygon:
        fill-color: '#FF0000'
        fill-opacity: 0.9
        stroke-color: '#000000'
        stroke-width: 8
        stroke-opacity: 1
        stroke-linejoin: bevel
```

![](img/polygon_basic.png)

### Fill with graphic

The `fill-graphic` property is used to fill a geometry with a repeating graphic. This can be a mark or an external image. The `x-graphic-margin` option can be used to specify top, right, bottom, and left margins around the graphic used in the fill. This example uses two sets of repeating squares with different offset values to draw a checkerboard pattern:

```yaml
name: checkers
feature-styles:
- name: name
  rules:
  - title: fill-graphic
    symbolizers:  
    - polygon:
        stroke-width: 1
        fill-graphic:
          symbols:
          - mark:
              shape: square
              fill-color: '#000000'
          size: 8
        x-graphic-margin: 16 16 0 0
    - polygon:
        stroke-width: 1
        fill-graphic:
          symbols:
          - mark:
              shape: square
              fill-color: '#000000'
          size: 8
        x-graphic-margin: 0 0 16 16
```

![](img/polygon_checkers.png)

*Checkered fill*

### Randomized graphic fill

Normally, the graphic used for the `fill-graphic` property is tiled. Alternatively, one can scatter this image randomly across the fill area using the `x-random` option and associated other options. This could be used to create a speckled pattern, as in the following example:

```yaml
name: speckles
feature-styles:
- name: name
  rules:
  - title: fill-graphic
    symbolizers:  
    - polygon:
        stroke-width: 1
        fill-graphic:
          symbols:
            - mark:
                shape: circle
                fill-color: '#000000'
          size: 3
          x-random: grid
          x-random-seed: 2
          x-random-tile-size: 1000
          x-random-rotation: free
          x-random-symbol-count: 1000
```

![](img/polygon_random.png)

*Randomized graphic fill*
