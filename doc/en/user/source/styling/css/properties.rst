.. _css_properties:

Property listing
================

.. highlight:: css

This page lists the supported rendering properties.  See :ref:`css_valuetypes` for more
information about the value types for each.

.. _css_properties_point:

Point symbology
---------------

.. list-table::
    :widths: 15 15 60 10
    :header-rows: 1

    - * Property
      * Type
      * Meaning
      * Accepts Expression?
    - * ``mark``     
      * url, symbol
      * The image or well-known shape to render for points
      * yes
    - * ``mark-composite``
      * string 
      * The composite mode to be used and the optional opacity separated with a comma. See the :ref:`full list of available modes <sld-extensions_composite-blend_modes>`.
      * no
    - * ``mark-mime``
      * string (`MIME Type <http://en.wikipedia.org/wiki/MIME>`_)
      * The type of the image referenced by a url()
      * No, defaults to 'image/jpeg'
    - * ``mark-geometry`` 
      * expression
      * An expression to use for the geometry when rendering features
      * yes
    - * ``mark-size`` 
      * length   
      * The width to assume for the provided image.  The height will be
        adjusted to preserve the source aspect ratio. 
      * yes
    - * ``mark-rotation``
      * angle 
      * A rotation to be applied (clockwise) to the mark image.
      * yes
    - * ``z-index``
      * integer
      * Controls the z ordering of output
      * no
    - * ``mark-label-obstacle``
      * boolean
      * If true the point symbol will be consider an obstable for labels, no label will overlap it
      * no

.. _css_properties_line:

Line symbology
--------------

.. list-table:: 
    :widths: 15 15 60 10
    :header-rows: 1

    - * Property
      * Type
      * Meaning
      * Accepts Expression?
    - * ``stroke``
      * color, url, symbol
      * The color, graphic, or well-known shape to use to stroke lines or outlines
      * yes
    - * ``stroke-composite``
      * string 
      * The composite mode to be used and the optional opacity separated with a comma. See the :ref:`full list of available modes <sld-extensions_composite-blend_modes>`.
      * no
    - * ``stroke-geometry``
      * expression
      * An expression to use for the geometry when rendering features. 
      * yes
    - * ``stroke-offset``
      * expression
      * Draws a parallel line using the specified distance, positive values offset left, negative right.  
      * yes
    - * ``stroke-mime``
      * string (`MIME Type <http://en.wikipedia.org/wiki/MIME>`_)
      * The type of the image referenced by a url()
      * No, defaults to 'image/jpeg'
    - * ``stroke-opacity``   
      * percentage       
      * A value in the range of 0 (fully transparent) to 1.0 (fully opaque)  
      * yes
    - * ``stroke-width``
      * length           
      * The width to use for stroking the line.
      * yes
    - * ``stroke-size``    
      * length           
      * An image or symbol used for the stroke pattern will be stretched or
        squashed to this size before rendering.  If this value differs from the
        stroke-width, the graphic will be repeated or clipped as needed.
      * yes
    - * ``stroke-rotation``  
      * angle            
      * A rotation to be applied (clockwise) to the stroke image. See also the
        stroke- repeat property.
      * yes
    - * ``stroke-linecap``   
      * keyword: butt, square, round
      * The style to apply to the ends of lines drawn 
      * yes
    - * ``stroke-linejoin``
      * keyword: miter, round, bevel
      * The style to apply to the "elbows" where segments of multi-line features meet. 
      * yes
    - * ``stroke-dasharray`` 
      * list of lengths  
      * The lengths of segments to use in a dashed line. 
      * no
    - * ``stroke-dashoffset``
      * length           
      * How far to offset the dash pattern from the ends of the lines.  
      * yes|
    - * ``stroke-repeat``
      * keyword: repeat, stipple
      * How to use the provided graphic to paint the line.  If repeat, then the
        graphic is repeatedly painted along the length of the line (rotated
        appropriately to match the line's direction).  If stipple, then the line
        is treated as a polygon to be filled.
      * yes
    - * ``z-index``
      * integer
      * Controls the z ordering of output
      * no
    - * ``stroke-label-obstacle``
      * boolean
      * If true the line will be consider an obstable for labels, no label will overlap it
      * no

.. _css_properties_polygon:

Polygon symbology
-----------------

.. list-table:: 
    :widths: 15 15 60 10
    :header-rows: 1

    - * Property
      * Type
      * Meaning
      * Accepts Expression?
    - * ``fill``         
      * color, url, symbol 
      * The color, graphic, or well-known shape to use to stroke lines or outlines 
      * yes
    - * ``fill-composite``
      * string 
      * The composite mode to be used and the optional opacity separated with a comma. See the :ref:`full list of available modes <sld-extensions_composite-blend_modes>`.
      * no
    - * ``fill-geometry``
      * expression 
      * An expression to use for the geometry when rendering features. 
      * yes
    - * ``fill-mime``
      * string (`MIME Type <http://en.wikipedia.org/wiki/MIME>`_)
      * The type of the image referenced by a url()
      * No, defaults to 'image/jpeg'
    - * ``fill-opacity``
      * percentage        
      * A value in the range of 0 (fully transparent) to 1.0 (fully opaque) 
      * yes
    - * ``fill-size``    
      * length            
      * The width to assume for the image or graphic provided. 
      * yes
    - * ``fill-rotation``
      * angle             
      * A rotation to be applied (clockwise) to the fill image. 
      * yes
    - * ``z-index``
      * integer
      * Controls the z ordering of output
      * no
    - * ``fill-label-obstacle``
      * boolean
      * If true the polygon will be consider an obstable for labels, no label will overlap it
      * no
    - * ``graphic-margin``
      * List of lengths
      * A list of 1 to 4 values, specifying the space between repeated graphics in a texture paint. One value is uniform spacing in all directions, two values are considered top/bottom and right/left, three values are considered top, right/left, bottom, four values are read as top,right,bottom,left.
      * no
    - * ``random``
      * none,grid,free
      * Activates random distribution of symbols in a texture fill tile. See :ref:`randomized` for details. Defaults to "none"
      * no
    - * ``random-seed``
      * integer number
      * The seed for the random generator. Defaults to 0
      * no
    - * ``random-rotation``
      * none/free
      * When set to "free" activates random rotation of the symbol in addition to random distribution. Defaults to "none"
      * no
    - * ``random-symbol-count``
      * positive integer number
      * Number of suymbols to be placed in the texture fill tile. May not be respected due to location conflicts (no two symbols are allowed to overlap). Defaults to 16.
      * no
    - * ``random-tile-size``
      * positive integer number
      * Size of the texture paint tile that will be filled with the random symbols. Defaults to 256.
      * no

.. _css_properties_text1:

Text symbology (labeling) - part 1
----------------------------------

.. list-table:: 
    :widths: 15 15 60 10
    :header-rows: 1

    - * Property
      * Type
      * Meaning
      * Accepts Expression?
    - * ``label``      
      * string
      * The text to display as labels for features
      * yes
    - * ``label-geometry``
      * expression 
      * An expression to use for the geometry when rendering features. 
      * yes
    - * ``label-anchor``
      * expression 
      * The part of the label to place over the point or middle of the polygon.
        This takes 2 values - x y where x=0 is the left edge of the label, x=1 is the right edge.
        y=0 is the bottom edge of the label, y=1 is the top edge. Specify 0.5 0.5 to centre a label.
      * yes
    - * ``label-offset``
      * expression 
      * This is for fine-tuning label-anchor. x and y values specify pixels to adjust the label position. For lines, a single value will make the label be parallel to the line, at the given distance, while two values will force a point style placement, with the label painted horizonally at the center of the line (plus the given offsets)
      * yes
    - * ``label-rotation``
      * expression 
      * Clockwise rotation of label in degrees. 
      * yes
    - * ``label-z-index``
      * expression 
      * Used to determine which labels are drawn on top of other labels. Lower z-indexes are drawn on top. 
      * yes
    - * ``shield``
      * mark, symbol
      * A graphic to display behind the label, such as a highway shield.
      * yes
    - * ``shield-mime``
      * string (`MIME Type <http://en.wikipedia.org/wiki/MIME>`_)
      * The type of the image referenced by a url()
      * No, defaults to 'image/jpeg'
    - * ``font-family``
      * string
      * The name of the font or font family to use for labels
      * yes
    - * ``font-fill``
      * fill
      * The fill to use when rendering fonts
      * yes
    - * ``font-style`` 
      * keyword: normal, italic, oblique
      * The style for the lettering 
      * yes
    - * ``font-weight``
      * keyword: normal, bold
      * The weight for the lettering 
      * yes
    - * ``font-size``  
      * length
      * The size for the font to display. 
      * yes
    - * ``font-opacity``
      * percentage
      * The opacity of the text, from 0 (fully transparent) to 1.0 (fully opaque).
      * yes
    - * ``halo-radius``
      * length
      * The size of a halo to display around the lettering (to enhance
        readability). This is *required* to activate the halo feature. 
      * yes
    - * ``halo-color`` 
      * color 
      * The color for the halo 
      * yes
    - * ``halo-opacity``
      * percentage
      * The opacity of the halo, from 0 (fully transparent) to 1.0 (fully opaque). 
      * yes
    - * ``label-padding``
      * length
      * The amount of 'padding' space to provide around labels.  Labels will
        not be rendered closer together than this threshold.  This is
        equivalent to the :ref:`spaceAround<labeling_space_around>` vendor parameter.
      * no
    - * ``label-group``
      * one of: ``true`` or ``false``
      * If true, the render will treat features with the same label text as a
        single feature for the purpose of labeling.  This is equivalent to the 
        :ref:`group<labeling_group>` vendor parameter.
      * no
    - * ``label-max-displacement``
      * length
      * If set, this is the maximum displacement that the renderer will apply
        to a label.  Labels that need larger displacements to avoid collisions
        will simply be omitted.  This is equivalent to the
        :ref:`maxDisplacement<labeling_max_displacement>` vendor parameter.
      * no

.. _css_properties_text2:

Text symbology (labeling) - part 2
----------------------------------

.. list-table:: 
    :widths: 15 15 60 10
    :header-rows: 1

    - * Property
      * Type
      * Meaning
      * Accepts Expression?
    - * ``label-min-group-distance``
      * length
      * This is equivalent to the minGroupDistance vendor parameter in SLD.
      * no
    - * ``label-repeat``
      * length
      * If set, the renderer will repeat labels at this interval along a line.
        This is equivalent to the :ref:`repeat<labeling_repeat>` vendor parameter.
      * no
    - * ``label-all-group``
      * one of ``true`` or ``false``
      * when using grouping, whether to label only the longest line that could
        be built by merging the lines forming the group, or also the other
        ones.  This is equivalent to the :ref:`allGroup<labeling_all_group>`
        vendor parameter.
      * no
    - * ``label-remove-overlaps``
      * one of ``true`` or ``false``
      * If enabled, the renderer will remove overlapping lines within a group
        to avoid duplicate labels.  This is equivalent to the
        removeOverlaps vendor parameter.
      * no
    - * ``label-allow-overruns``
      * one of ``true`` or ``false``
      * Determines whether the renderer will show labels that are longer than
        the lines being labelled.  This is equivalent to the allowOverrun
        vendor parameter.
      * no
    - * ``label-follow-line``
      * one of ``true`` or ``false``
      * If enabled, the render will curve labels to follow the lines being
        labelled.  This is equivalent to the
        :ref:`followLine<labeling_follow_line>` vendor parameter.
      * no
    - * ``label-max-angle-delta``
      * one of ``true`` or ``false``
      * The maximum amount of curve allowed between two characters of a label;
        only applies when 'follow-line: true' is set.  This is equivalent
        to the :ref:`maxAngleDelta<labeling_max_angle_delta>` vendor parameter.
      * no
    - * ``label-auto-wrap``
      * length
      * Labels will be wrapped to multiple lines if they exceed this length in
        pixels.  This is equivalent to the :ref:`autoWrap<labeling_autowrap>`
        vendor parameter.
      * no
    - * ``label-force-ltr``
      * one of ``true`` or ``false``
      * By default, the renderer will flip labels whose normal orientation
        would cause them to be upside-down. Set this parameter to false if you
        are using some icon character label like an arrow to show a line's
        direction.  This is equivalent to the
        :ref:`forceLeftToRight<labeling_force_left_to_right>` vendor parameter.
      * no
    - * ``label-conflict-resolution``
      * one of ``true`` or ``false``
      * Set this to false to disable label conflict resolution, allowing
        overlapping labels to be rendered.  This is equivalent to the
        :ref:`conflictResolution<labeling_conflict_resolution>` vendor
        parameter.
      * no
    - * ``label-fit-goodness``
      * scale
      * The renderer will omit labels that fall below this "match quality"
        score.  The scoring rules differ for each geometry type.  This is
        equivalent to the :ref:`goodnessOfFit<labeling_goodness_of_fit>` vendor
        parameter.
      * no
    - * ``label-priority``
      * expression
      * Specifies an expression to use in determining which
        features to prefer if there are labeling conflicts.  This is equivalent
        to the :ref:`Priority<labeling_priority>` SLD extension.
      * yes
 
.. _css_properties_text3:

Text symbology (labeling) - part 3
----------------------------------

.. list-table:: 
    :widths: 15 15 60 10
    :header-rows: 1

    - * Property
      * Type
      * Meaning
      * Accepts Expression?
    - * ``shield-resize``
      * string, one of ``none``, ``stretch``, or ``proportional``
      * Specifies a mode for resizing label graphics (such as
        highway shields) to fit the text of the label.  The default mode,
        'none', never modifies the label graphic. In ``stretch`` mode,
        GeoServer will resize the graphic to exactly surround the label text,
        possibly modifying the image's aspect ratio.  In ``proportional`` mode,
        GeoServer will expand the image to be large enough to surround the text
        while preserving its original aspect ratio.
      * none
    - * ``shield-margin``
      * list of lengths, one to four elements long.
      * Specifies an extra margin (in pixels) to be applied to the label text when calculating label dimensions for use with the ``shield-resize`` option.  Similar to the ``margin`` shorthand property in CSS for HTML, its interpretation varies depending on how many margin values are provided: 1 = use that margin length on all sides of the label 2 = use the first for top & bottom margins and the second for left & right margins. 3 = use the first for the top margin, second for left & right margins, third for the bottom margin. 4 = use the first for the top margin, second for the right margin, third for the bottom margin, and fourth for the left margin.
      * none
    - * ``label-underline-text``
      * one of ``true`` or ``false``
      * If enabled, the renderer will underline labels. This is equivalent to the :ref:`underlineText <labeling_underline_text>` vendor parameter.
      * no
    - * ``label-strikethrough-text``
      * one of ``true`` or ``false``
      * If enabled, the renderer will strikethrough labels. This is equivalent to the :ref:`strikethroughText <labeling_strikethrough_text>` vendor parameter.
      * no
    - * ``label-char-spacing``
      * an amount of pixels, can be negative
      * If present, expands or shrinks the space between subsequent characters in a label according to the value specified
      * no
    - * ``label-word-spacing``
      * an amount of pixels, must be zero or positive
      * If present, expands the space between subsequent words in a label according to the value specified
      * no

.. _css_properties_raster:

Raster symbology 
----------------

.. list-table:: 
    :widths: 15 15 60 10
    :header-rows: 1

    - * Property
      * Type
      * Meaning
      * Accepts Expression?
    - * ``raster-channels``
      * string
      * The list of raster channels to be used in the output. It can be "auto" to make the renderer choose the best course of action, or a list of band numbers, a single one will generate a gray image, three will generate an RGB one, four will generate a RGBA one. E.g., "1 3 7" to choose the first, third and seventh band of the input raster to make a RGB image
      * no
    - * ``raster-composite``
      * string 
      * The composite mode to be used and the optional opacity separated with a comma. See the :ref:`full list of available modes <sld-extensions_composite-blend_modes>`.
      * no
    - * ``raster-geometry``
      * expression
      * The attribute containing the raster to be painted. Normally not needed, but it would work if you had a custom vector data source that contains a GridCoverage attribute, in order to select it
      * yes
    - * ``raster-opacity``
      * floating point
      * A value comprised between 0 and 1, 0 meaning completely transparent, 1 meaning completely opaque. This controls the whole raster trasparency. 
      * no
    - * ``raster-contrast-enhancement``
      * string
      * Allows to stretch the range of data/colors in order to enhance tiny differences. Possible values are 'normalize', 'histogram' and 'none'
      * no
    - * ``raster-gamma``
      * floating point
      * Gamma adjustment for the output raster
      * no
    - * ``raster-z-index``
      * integer
      * Controls the z ordering of the raster output
      * no
    - * ``raster-color-map``
      * string
      * Applies a color map to single banded input. The contents is a space separate list of ``color-map-entry(color, value)`` (opacity assumed to be 1 and label will have a null value), or ``color-map-entry(color, value, opacity, label)``. The values must be provided in increasing order.
      * no
    - * ``raster-color-map-type``
      * string
      * Controls how the color map entries are interpreted, the possible values are "ramp", "intervals" and "values", with ramp being the default if no "raster-color-map-type" is provided. The default "ramp" behavior is to linearly interpolate color between the provided values, and assign the lowest color to all values below the lowest value, and the highest color to all values above the highest value. The "intervals" behavior instead assigns solid colors between values, whilst "values" only assigns colors to the specified values, every other value in the raster is not painted at all
      * no
 
.. _css_properties_shared:

Shared
------

.. list-table:: 
    :widths: 15 15 60 10
    :header-rows: 1

    - * Property
      * Type
      * Meaning
      * Accepts Expression?
    - * ``composite``
      * string 
      * The composite mode to be used and the optional opacity separated with a comma. See the :ref:`full list of available modes <sld-extensions_composite-blend_modes>`.
      * no
    - * ``composite-base``
      * one of ``true`` or ``false`` 
      * This will tell the rendering engine to use that FeatureTypeStyle as the destination, and will compose all subsequent FeatureTypeStyle/Layers on top of it, until another base is found.
      * no
    - * ``geometry``
      * expression 
      * An expression to use for the geometry when rendering features. This
        provides a geometry for all types of symbology, but can be overridden
        by the symbol-specific geometry properties. 
      * yes
    - * ``sort-by``
      * string 
      * A comma separated list of sorting directives, "att1 A|D, att2 A|D, ..." where ``att?`` are attribute names,
        and ``A`` or ``D`` are an optional direction specification, 
        ``A`` is ascending, ``D`` is descending.
        Determines the loading, and thus painting, order of the features 
      * no
    - * ``sort-by-group``
      * string
      * Rules with the different z-index but same sort-by-group id have  their features sorted
        as a single group. Useful to z-order across layers or across different feature groups, like
        roads and rails, especially when using z-index to support casing 
      * no
    - * ``transform``
      * function
      * Applies a rendering transformationon the current level. The function syntax is ``txName(key1:value1,key1:value2)``. Values can be single ones, or space separated lists. 
      * no
    
.. _css_properties_symbol:

Symbol properties
-----------------

These properties are applied only when styling built-in symbols.  See
:ref:`css_styledmarks` for details.

.. list-table::
    :widths: 15 15 60 10
    :header-rows: 1

    - * Property
      * Type
      * Meaning
      * Accepts Expression?
    - * ``size``
      * length
      * The size at which to render the symbol. 
      * yes
    - * ``rotation``
      * angle
      * An angle through which to rotate the symbol. 
      * yes
