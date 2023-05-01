.. _imagemap_extension:

Imagemap
========

HTML ImageMaps have been used for a long time to create interactive images in a light way. Without using Flash, SVG or VML you can simply associate different links or tooltips to different regions of an image.
Why can't we use this technique to achieve the same result on a GeoServer map?
The idea is to combine a raster map (png, gif, jpeg, ...) with an HTML ImageMap overlay to add links, tooltips, or mouse events behavior to the map.

An example of an ImageMap adding tooltips to a map:

.. code-block:: xml

   <img src="..." usemap="#mymap"/>
   <map name="mymap">
        <area shape="poly" coords="536,100 535,100 534,101 533,101 532,102"  title="This is a tooltip"/>
        <area shape="poly" coords="518,113 517,114 516,115 515,114"  title="Another tooltip"/>
   </map>

An example of an ImageMap adding links to a map:

.. code-block:: xml

   <img src="..." usemap="#mymap"/>
   <map name="mymap">
        <area shape="poly" coords="536,100 535,100 534,101 533,101 532,102"  href="http://www.mylink.com"/>
        <area shape="poly" coords="518,113 517,114 516,115 515,114"  href="http://www.mylink2.com"/>
   </map>

A more complex example adding interactive behaviour on mouse events:

.. code-block:: xml

   <img src="..." usemap="#mymap"/>
   <map name="mymap">
        <area shape="poly" coords="536,100 535,100 534,101 533,101 532,102"  onmouseover="onOver('<featureid>')" onmouseout="onOut('<featureid>')"/>
        <area shape="poly" coords="518,113 517,114 516,115 515,114"   onmouseover="onOver('<featureid>')" onmouseout="onOut('<featureid>')"/>
   </map>

To realize this in GeoServer some great community contributors developed an HTMLImageMap GetMapProducer for GeoServer, able to render an HTMLImageMap in response to a WMS GetMap request. 

The GetMapProducer is associated to the text/html mime type. It produces, for each requested layer, a <map>...</map> section containing the geometries of the layer as distinct <area> tags.
Due to the limitations in the shape types supported by the <area> tag, a single geometry can be split into multiple ones. This way almost any complex geometry can be rendered transforming it into simpler ones.

To add interactive attributes we use styling. In particular, an SLD Rule containing a TextSymbolizer with a Label definition can be used to define dynamic values for the <area> tags attributes. The Rule name will be used as the attribute name.

As an example, to define a title attribute (associating a tooltip to the geometries of the layer) you can use a rule like the following one:

.. code-block:: xml

   <Rule>
      <Name>title</Name>
      <TextSymbolizer>
         <Label><PropertyName>MYPROPERTY</PropertyName></Label>
      </TextSymbolizer>
   </Rule>

To render multiple attributes, just define multiple rules, with different names (href, onmouseover, etc.)

Styling support is not limited to TextSymbolizers, you can currently use other symbolizers to detail <area> rendering. For example you can:

    * use a PointSymbolizer with a Size property to define point sizes.
    * use LineSymbolizer with a stroke-width CssParameter to create thick lines.
