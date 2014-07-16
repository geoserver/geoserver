.. _get_legend_graphic: 

GetLegendGraphic
================

This chapter describes whether to use the GetLegendGraphics request. The SLD Specifications 1.0.0 gives a good description about GetLegendGraphic requests:

`The GetLegendGraphic operation itself is optional for an SLD-enabled WMS. It provides a general mechanism for acquiring legend symbols, beyond the LegendURL reference of WMS Capabilities. Servers supporting the GetLegendGraphic call might code LegendURL references as GetLegendGraphic for interface consistency. Vendor-specific parameters may be added to GetLegendGraphic requests and all of the usual OGC-interface options and rules apply. No XML-POST method for GetLegendGraphic is presently defined`.

Here is an example invocation::

	http://localhost:8080/geoserver/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=20&HEIGHT=20&LAYER=topp:states

which would produce four 20x20 icons that graphically represent the rules of the default style of the topp:states layer.

.. figure:: img/samplelegend.png
   :align: center

   *Sample legend*

In the following table the whole set of GetLegendGraphic parameters that can be used.

.. list-table::
   :widths: 15 5 80

   * - **Parameter**
     - **Required**
     - **Description**
   * - *REQUEST*
     - Required
     - Value must be "GetLegendRequest".
   * - *LAYER*
     - Required
     - Layer for which to produce legend graphic.
   * - *STYLE*
     - Optional
     - Style of layer for which to produce legend graphic. If not present, the default style is selected. The style may be any valid style available for a layer, including non-SLD internally-defined styles.
   * - *FEATURETYPE*
     - Optional
     - Feature type for which to produce the legend graphic. This is not needed if the layer has only a single feature type.
   * - *RULE*
     - Optional
     - Rule of style to produce legend graphic for, if applicable. In the case that a style has multiple rules but no specific rule is selected, then the map server is obligated to produce a graphic that is representative of all of the rules of the style.
   * - *SCALE*
     - Optional
     - In the case that a RULE is not specified for a style, this parameter may assist the server in selecting a more appropriate representative graphic by eliminating internal rules that are out-of-scope. This value is a standardized scale denominator, defined in Section 10.2. Specifying the scale will also make the symbolizers using Unit Of Measure resize according to the specified scale.
   * - *SLD*
     - Optional
     - This parameter specifies a reference to an external SLD document. It works in the same way as the SLD= parameter of the WMS GetMap operation.   
   * - *SLD_BODY*
     - Optional
     - This parameter allows an SLD document to be included directly in an HTTP-GET request. It works in the same way as the SLD_BODY= parameter of the WMS GetMap operation.
   * - *FORMAT*
     - Required
     - This gives the MIME type of the file format in which to return the legend graphic. Allowed values are the same as for the FORMAT= parameter of the WMS GetMap request.
   * - *WIDTH*
     - Optional
     - This gives a hint for the width of the returned graphic in pixels. Vector-graphics can use this value as a hint for the level of detail to include.
   * - *HEIGHT*
     - Optional
     - This gives a hint for the height of the returned graphic in pixels.
   * - *EXCEPTIONS*
     - Optional
     - This gives the MIME type of the format in which to return exceptions. Allowed values are the same as for the EXCEPTIONS= parameter of the WMS GetMap request.
     
Controlling legend appearance with LEGEND_OPTIONS
-------------------------------------------------

GeoServer allows finer control over the legend appearance via the vendor parameter ``LEGEND_OPTIONS``.
The general format of ``LEGEND_OPTIONS`` is the same as ``FORMAT_OPTIONS``, that is::

  ...&LEGEND_OPTIONS=key1:v1;key2:v2;...;keyn:vn
  
Here is a description of the various parameters that can be used in ``LEGEND_OPTIONS``:

    - **fontName (string)** the name of the font to be used when generating rule titles. The font must be available on the server
    - **fontStyle (string)** can be set to italic or bold to control the text style. Other combination are not allowed right now but we could implement that as well.
    - **fontSize (integer)** allows us to set the Font size for the various text elements. Notice that default size is 12.
    - **fontColor (hex)** allows us to set the color for the text of rules and labels (see above for recommendation on how to create values). Values are expressed in ``0xRRGGBB`` format
    - **fontAntiAliasing (true/false)** when true enables antialiasing for rule titles
    - **bgColor (hex)** background color for the generated legend, values are expressed in ``0xRRGGBB`` format
    - **dpi (integer)** sets the DPI for the current request, in the same way as it is supported by GetMap. Setting a DPI larger than 91 (the default) makes all fonts, symbols and line widths grow without changing the current scale, making it possible to get a high resolution version of the legend suitable for inclusion in printouts 
    - **forceLabels** "on" means labels will always be drawn, even if only one rule is available. "off" means labels will never be drawn, even if multiple rules are available. Off by default.

Here is a sample request sporting all the options::

  http://localhost:8080/geoserver/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=20&HEIGHT=20&LAYER=topp:states&legend_options=fontName:Times%20New%20Roman;fontAntiAliasing:true;fontColor:0x000033;fontSize:14;bgColor:0xFFFFEE;dpi:180
  
.. figure:: img/legendoptions.png
   :align: center

   *Using LEGEND_OPTIONS to control the output*



Raster Legends Explained
------------------------

This chapter aim to briefly describe the work that I have performed in order to support legends for raster data that draw information taken from the various bits of the SLD 1.0 RasterSymbolizer element. Recall, that up to now there was no way to create legends for raster data, therefore we have tried to fill the gap by providing an implementation of the getLegendGraphic request that would work with the ColorMap element of the SLD 1.0 RasterSymbolizer. Notice that some "debug" info about the style, like colormap type and band used are printed out as well.

What's a raster legend
'''''''''''''''''''''''

Here below I have drawn the structure of a typical legend, where some elements of interests are parameterized.

.. figure:: img/rasterlegend1.png
   :align: center

   *The structure of a typical legend*

Take as an instance one of the SLD files attached to this page, each row in the above table draws its essence from the  ColorMapEntry element as shown here below:

.. code-block:: xml

	<ColorMapEntry color="#732600" quantity="9888" opacity="1.0" label="<-70 mm"/>

The producer for the raster legend will make use of this elements in order to build the legend, with this regards, notice that:

    - the width of the Color element is driven by the requested width for the GetLegendGraphic request
    - the width and height of label and rules is computed accordingly to the used Font and Font size for the prepared text (**no new line management for the moment**) 
    - the height of the Color element is driven by the requested width for the GetLegendGraphic request, but notice that for ramps we expand this a little since the goal is to turn the various Color elements into a single long strip
    - the height of each row is set to the maximum height of the single elements
    - the width of each row is set to the sum of the width of the various elements plus the various paddings
    - **dx,dy** the spaces between elements and rows are set to the 15% of the requested width and height. Notice that **dy** is ignored for the colormaps of type **ramp** since they must create a continous color strip.
    - **mx,my** the margins from the border of the legends are set to the 1.5% of the total size of the legend

Just to jump right to the conclusions (which is a bad practice I know, but no one is perfect ), here below I am adding an image of a sample legend with all the various options at work. The request that generated it is the following::

	http://localhost:8081/geoserver/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=100&HEIGHT=20&LAYER=it.geosolutions:di08031_da&LEGEND_OPTIONS=forceRule:True;dx:0.2;dy:0.2;mx:0.2;my:0.2;fontStyle:bold;borderColor:0000ff;border:true;fontColor:ff0000;fontSize:18

Do not worry if it seems like something written in ancient dead language, I am going to explain the various params here below. Nevertheless it is important to point out that basic info on how to create and set params can be found in this `page <http://rancor.boundlessgeo.com:8080/display/GEOSDOC/GetLegendGraphic+Improvements>`_.

.. figure:: img/rasterlegend2.png
   :align: center 

   *Example of a raster legend*

Raster legends' types
'''''''''''''''''''''

As you may know (well, actually you might not since I never wrote any real docs about the RasterSymbolizer work I did) GeoServer supports three types of ColorMaps:

    - **ramp** this is what SLD 1.0 dictates, which means a linear interpolation weighted on values between the colors of the various ColorMapEntries.
    - **values** this is an extensions that allows link quantities to colors as specified by the ColorMapEntries quantities. Values not specified are translated into transparent pixels.
    - **classes** this is an extensions that allows pure classifications based o intervals created from the  ColorMapEntries quantities. Values not specified are translated into transparent pixels.

Here below I am going to list various examples that use the attached styles on a rainfall floating point geotiff.

ColorMap type is VALUES
'''''''''''''''''''''''

Refer to the SLD rainfall.sld in attachment.

.. figure:: img/rasterlegend3.png
   :align: center 

   *Raster legend - VALUES type*

ColorMap type is CLASSES
''''''''''''''''''''''''

Refer to the SLD rainfall_classes.sld in attachment.  

.. figure:: img/rasterlegend4.png
   :align: center 

   *Raster legend - CLASSES type*


ColorMap type is RAMP
'''''''''''''''''''''

Refer to the SLD rainfall_classes.sld in attachment. Notice that the first legend show the default border behavior while the second has been force to draw a border for the breakpoint color of the the colormap entry quantity described by the rendered text. Notice that each color element has a part that show the fixed color from the colormap entry it depicts (the lowest part of it, the one that has been outlined by the boder in the second legend here below) while the upper part of the element has a gradient tha connects each element to the previous one to point out the fact that we are using linear interpolation. 

.. figure:: img/rasterlegend5.png
   :align: center 

   *Raster legend - RAMP type*

The various control parameters and how to set them
''''''''''''''''''''''''''''''''''''''''''''''''''

I am now going to briefly explain the various parameters tht we can use to control the layout and content of the legend (refer also to this `page <http://rancor.boundlessgeo.com:8080/display/GEOSDOC/GetLegendGraphic+Improvements>`_). Here below I have put a request that puts all the various options at tow::

	http://localhost:8081/geoserver/wms?REQUEST=GetLegendGraphic&VERSION=1.0.0&FORMAT=image/png&WIDTH=100&HEIGHT=20&LAYER=it.geosolutions:di08031_da&LEGEND_OPTIONS=forceRule:True;dx:0.2;dy:0.2;mx:0.2;my:0.2;fontStyle:bold;borderColor:0000ff;border:true;fontColor:ff0000;fontSize:18

Let's now examine all the interesting elements, one by one. Notice that I am not going to discuss the mechanics of the  GetLegendGraphic operation, for that you may want to refer to the SLD 1.0 spec, my goal is to briefly discuss the LEGEND_OPTIONS parameter.

    - **forceRule (boolean)** by defaul rules for a ColorMapEntry are not drawn to keep the legend small and compact, unless there are not labels at all. You can change this behaviour by setting this parameter to true.
    - **dx,dy,mx,my (double)** can be used to set the margin and the buffers between elements
    - **border (boolean)** activates or deactivates the boder on the color elements in order to make the separations cleare. Notice that I decided to **always** have a line that would split the various color elements for the ramp type of colormap.
    - **borderColor (hex)** allows us to set the color for the border in 0xRRGGBB format
