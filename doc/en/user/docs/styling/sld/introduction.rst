.. _sld_intro:

Introduction to SLD
===================

Geospatial data has no intrinsic visual component.  
In order to see data, it must be styled.  
Styling specifies color, thickness, and other visible attributes used to render data on a map.  

In GeoServer, styling is accomplished using a markup language called `Styled Layer Descriptor <http://www.opengeospatial.org/standards/sld>`_, or SLD for short.  
SLD is an XML-based markup language and is very powerful, although somewhat complex.  
This page gives an introduction to the capabilities of SLD and how it works within GeoServer.

.. note:: Since GeoServer uses SLD exclusively for styling, the terms "SLD" and "style" will often be used interchangeably.

SLD Concepts
------------

In GeoServer styling is most often specified using XML **SLD style documents**.  
Style documents are associated with GeoServer **layers** (**featuretypes**)
to specify how they should be **rendered**.
A style document specifies a single **named layer**
and a **user style** for it.
The layer and style can have metadata elements
such as a **name** identifying them,
a **title** for displaying them,
and an **abstract** describing them in detail.
Within the top-level style are one or more **feature type styles**,
which act as "virtual layers" to provide control over
rendering order (allowing styling effects such as cased lines for roads).
Each feature type style contains one or more **rules**,   
which control how styling is applied based
on feature attributes and zoom level.
Rules select applicable features by using 
**filters**, which are logical conditions containing **predicates**, **expressions** 
and **filter functions**.
To specify the details of styling for individual features,
rules contain any number of **symbolizers**.
Symbolizers specify styling for **points**, **lines** and **polygons**,
as well as **rasters** and **text labels**.

For more information refer to the :ref:`sld_reference`.

Types of styling
----------------

Vector data that GeoServer can serve consists of three classes of shapes:  **Points, lines, and polygons**.  
Lines (one dimensional shapes) are the simplest, as they have only the edge to style (also known as "stroke").  
Polygons, two dimensional shapes, have an edge and an inside (also known as a "fill"), both of which can be styled differently.  
Points, even though they lack dimension, have both an edge and a fill (not to mention a size) that can be styled.  
For fills, color can be specified; for strokes, color and thickness can be specified.  

GeoServer also serves raster data.  This can be styled with a wide variety of
control over color palette, opacity, contrast and other parameters.

More advanced styling is possible as well.  
Points can be specified with well-known shapes like circles, squares, stars, and even custom graphics or text.  
Lines can be styled with a dash styles and hashes.  
Polygons can be filled with a custom tiled graphics.  
Styling can be based on attributes in the data, so that certain features are styled differently.  
Text labels on features are possible as well.  
Styling can also be determined by zoom level, so that features are displayed in a way appropriate to 
their apparent size.  
The possibilities are vast.

A basic style example
---------------------

A good way to learn about SLD is to study styling examples.
The following is a simple SLD that can be applied to a layer that contains points, 
to style them as red circles with a size of 6 pixels.  
(This is the first example in the :ref:`sld_cookbook_points` section of the :ref:`sld_cookbook`.)

.. code-block:: xml 
   :linenos: 

   <?xml version="1.0" encoding="ISO-8859-1"?>
   <StyledLayerDescriptor version="1.0.0" 
       xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 
       xmlns="http://www.opengis.net/sld" 
       xmlns:ogc="http://www.opengis.net/ogc" 
       xmlns:xlink="http://www.w3.org/1999/xlink" 
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
     <NamedLayer>
       <Name>Simple point</Name>
       <UserStyle>
         <Title>GeoServer SLD Cook Book: Simple point</Title>
         <FeatureTypeStyle>
           <Rule>
             <PointSymbolizer>
               <Graphic>
                 <Mark>
                   <WellKnownName>circle</WellKnownName>
                   <Fill>
                     <CssParameter name="fill">#FF0000</CssParameter>
                   </Fill>
                 </Mark>
                 <Size>6</Size>
               </Graphic>
             </PointSymbolizer>
           </Rule>
         </FeatureTypeStyle>
       </UserStyle>
     </NamedLayer>
   </StyledLayerDescriptor>

   
Although the example looks long, only a few lines are really important to understand.  
**Line 14** states that a "PointSymbolizer" is to be used to style data as points.  
**Lines 15-17** state that points are to be styled using a graphic shape specified by a "well known name", in this case a circle.  
SLD provides names for many shapes such as "square", "star", "triangle", etc.  
**Lines 18-20** specify the shape should be filled with a color of ``#FF0000`` (red).  
This is an RGB color code, written in hexadecimal, in the form of #RRGGBB.  
Finally, **line 22** specifies that the size of the shape is 6 pixels in width.  
The rest of the structure contains metadata about the style, such as a name identifying the style
and a title for use in legends.

.. note:: In SLD documents some tags have prefixes, such as ``ogc:``.  
          This is because they are defined in **XML namespaces**.  
          The top-level ``StyledLayerDescriptor`` tag (**lines 2-7**) specifies two XML namespaces, one called ``xmlns``, and one called ``xmlns:ogc``.  
          The first namespace is the default for the document, so tags belonging to it do not need a prefix.
          Tags belonging to the second require the prefix ``ogc:``.  
          In fact, the namespace prefixes can be any identifier.  
          The first namespace could be called ``xmlns:sld`` (as it often is) and then all the tags in this example would require an ``sld:`` prefix.  
          The key point is that tags need to have the prefix for the namespace they belong to.

See the :ref:`sld_cookbook` for more examples of styling with SLD.
