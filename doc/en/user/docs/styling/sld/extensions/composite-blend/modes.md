# Composite and blending modes {: #sld-extensions_composite-blend_modes }

There are two types of modes: alpha composite and color blending.

Alpha compositing controls how two images are merged together by using the alpha levels of the two. No color mixing is being performed, only pure binary selection (either one or the other).

Color blending modes mix the colors of source and destination in various ways. Each pixel in the result will be some sort of combination between the source and destination pixels.

The following page shows the full list of available modes. (See the [syntax](syntax.md) page for more details.) To aid in comprehension, two source and two destination images will be used to show visually how each mode works:

+-----------------------------------+-----------------------------------+
| Source 1                          | Source 2                          |
+===================================+===================================+
| ![](images/map.png)               | ![](images/map2.png)              |
+-----------------------------------+-----------------------------------+

+-----------------------------------+-----------------------------------+
| Destination 1                     | Destination 2                     |
+===================================+===================================+
| ![](images/bkg.png)               | ![](images/bkg2.png)              |
+-----------------------------------+-----------------------------------+

## Alpha compositing modes

### copy

Only the source will be present in the output.

+-----------------------------------+-----------------------------------+
| Example 1                         | Example 2                         |
+===================================+===================================+
| ![](images/blend1-copy.png)       | ![](images/blend2-copy.png)       |
+-----------------------------------+-----------------------------------+

### destination

Only the destination will be present in the output

+------------------------------------+------------------------------------+
| Example 1                          | Example 2                          |
+====================================+====================================+
| ![](images/blend1-destination.png) | ![](images/blend2-destination.png) |
+------------------------------------+------------------------------------+

### source-over

The source is drawn over the destination, and the destination is visible where the source is transparent. Opposite of *destination-over*.

+------------------------------------+------------------------------------+
| Example 1                          | Example 2                          |
+====================================+====================================+
| ![](images/blend1-source-over.png) | ![](images/blend2-source-over.png) |
+------------------------------------+------------------------------------+

### destination-over

The source is drawn below the destination, and is visible only when the destination is transparent. Opposite of *source-over*.

+-----------------------------------------+-----------------------------------------+
| Example 1                               | Example 2                               |
+=========================================+=========================================+
| ![](images/blend1-destination-over.png) | ![](images/blend2-destination-over.png) |
+-----------------------------------------+-----------------------------------------+

### source-in

The source is visible only when overlapping some non-transparent pixel of the destination. This allows the background map to act as a mask for the layer/feature being drawn. Opposite of *destination-in*.

+-----------------------------------+-----------------------------------+
| Example 1                         | Example 2                         |
+===================================+===================================+
| ![](images/blend1-source-in.png)  | ![](images/blend2-source-in.png)  |
+-----------------------------------+-----------------------------------+

### destination-in

The destination is retained only when overlapping some non transparent pixel in the source. This allows the layer/feature to be drawn to act as a mask for the background map. Opposite of *source-in*.

+---------------------------------------+---------------------------------------+
| Example 1                             | Example 2                             |
+=======================================+=======================================+
| ![](images/blend1-destination-in.png) | ![](images/blend2-destination-in.png) |
+---------------------------------------+---------------------------------------+

### source-out

The source is retained only in areas where the destination is transparent. This acts as a reverse mask when compared to *source-in*.

+-----------------------------------+-----------------------------------+
| Example 1                         | Example 2                         |
+===================================+===================================+
| ![](images/blend1-source-out.png) | ![](images/blend2-source-out.png) |
+-----------------------------------+-----------------------------------+

### destination-out

The destination is retained only in areas where the source is transparent. This acts as a reverse mask when compared to *destination-in*.

+----------------------------------------+----------------------------------------+
| Example 1                              | Example 2                              |
+========================================+========================================+
| ![](images/blend1-destination-out.png) | ![](images/blend2-destination-out.png) |
+----------------------------------------+----------------------------------------+

### source-atop

The destination is drawn fully, while the source is drawn only where it intersects the destination.

+------------------------------------+------------------------------------+
| Example 1                          | Example 2                          |
+====================================+====================================+
| ![](images/blend1-source-atop.png) | ![](images/blend2-source-atop.png) |
+------------------------------------+------------------------------------+

### destination-atop

The source is drawn fully, and the destination is drawn over the source and only where it intersects it.

+-----------------------------------------+-----------------------------------------+
| Example 1                               | Example 2                               |
+=========================================+=========================================+
| ![](images/blend1-destination-atop.png) | ![](images/blend2-destination-atop.png) |
+-----------------------------------------+-----------------------------------------+

### xor

"Exclusive Or" mode. Each pixel is rendered only if either the source or the destination is not blank, but not both.

+-----------------------------------+-----------------------------------+
| Example 1                         | Example 2                         |
+===================================+===================================+
| ![](images/blend1-xor.png)        | ![](images/blend2-xor.png)        |
+-----------------------------------+-----------------------------------+

## Color blending modes

### multiply

The source color is multiplied by the destination color and replaces the destination. The resulting color is always at least as dark as either the source or destination color. Multiplying any color with black results in black. Multiplying any color with white preserves the original color.

+-----------------------------------+-----------------------------------+
| Example 1                         | Example 2                         |
+===================================+===================================+
| ![](images/blend1-multiply.png)   | ![](images/blend2-multiply.png)   |
+-----------------------------------+-----------------------------------+

### screen

Multiplies the complements of the source and destination color values, then complements the result. The end result color is always at least as light as either of the two constituent colors. Screening any color with white produces white; screening with black leaves the original color unchanged.

The effect is similar to projecting multiple photographic slides simultaneously onto a single screen.

+-----------------------------------+-----------------------------------+
| Example 1                         | Example 2                         |
+===================================+===================================+
| ![](images/blend1-screen.png)     | ![](images/blend2-screen.png)     |
+-----------------------------------+-----------------------------------+

### overlay

Multiplies (screens) the colors depending on the destination color value. Source colors overlay the destination while preserving its highlights and shadows. The backdrop color is not replaced but is mixed with the source color to reflect the lightness or darkness of the backdrop.

+-----------------------------------+-----------------------------------+
| Example 1                         | Example 2                         |
+===================================+===================================+
| ![](images/blend1-overlay.png)    | ![](images/blend2-overlay.png)    |
+-----------------------------------+-----------------------------------+

### darken

Selects the darker of the destination and source colors. The destination is replaced with the source only where the source is darker.

+-----------------------------------+-----------------------------------+
| Example 1                         | Example 2                         |
+===================================+===================================+
| ![](images/blend1-darken.png)     | ![](images/blend2-darken.png)     |
+-----------------------------------+-----------------------------------+

### lighten

Selects the lighter of the destination and source colors. The destination is replaced with the source only where the source is lighter.

+-----------------------------------+-----------------------------------+
| Example 1                         | Example 2                         |
+===================================+===================================+
| ![](images/blend1-lighten.png)    | ![](images/blend2-lighten.png)    |
+-----------------------------------+-----------------------------------+

### color-dodge

Brightens the destination color to reflect the source color. Drawing with black produces no changes.

+------------------------------------+------------------------------------+
| Example 1                          | Example 2                          |
+====================================+====================================+
| ![](images/blend1-color-dodge.png) | ![](images/blend2-color-dodge.png) |
+------------------------------------+------------------------------------+

### color-burn

Darkens the destination color to reflect the source color. Drawing with white produces no change.

+-----------------------------------+-----------------------------------+
| Example 1                         | Example 2                         |
+===================================+===================================+
| ![](images/blend1-color-burn.png) | ![](images/blend2-color-burn.png) |
+-----------------------------------+-----------------------------------+

### hard-light

Multiplies or screens the colors, depending on the source color value. The effect is similar to shining a harsh spotlight on the destination.

+-----------------------------------+-----------------------------------+
| Example 1                         | Example 2                         |
+===================================+===================================+
| ![](images/blend1-hard-light.png) | ![](images/blend2-hard-light.png) |
+-----------------------------------+-----------------------------------+

### soft-light

Darkens or lightens the colors, depending on the source color value. The effect is similar to shining a diffused spotlight on the destination.

+-----------------------------------+-----------------------------------+
| Example 1                         | Example 2                         |
+===================================+===================================+
| ![](images/blend1-soft-light.png) | ![](images/blend2-soft-light.png) |
+-----------------------------------+-----------------------------------+

### difference

Subtracts the darker of the two constituent colors from the lighter color. White inverts the destination color; black produces no change.

+-----------------------------------+-----------------------------------+
| Example 1                         | Example 2                         |
+===================================+===================================+
| ![](images/blend1-difference.png) | ![](images/blend2-difference.png) |
+-----------------------------------+-----------------------------------+

### exclusion

Produces an effect similar to that of *difference* but lower in contrast. White inverts the destination color; black produces no change.

+-----------------------------------+-----------------------------------+
| Example 1                         | Example 2                         |
+===================================+===================================+
| ![](images/blend1-exclusion.png)  | ![](images/blend2-exclusion.png)  |
+-----------------------------------+-----------------------------------+
