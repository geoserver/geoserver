.. _sld_intro:

Introduction to SLD
===================

Geospatial data has no intrinsic visual component.  In order to see data, it must be styled.  This means to specify color, thickness, and other visible attributes.  In GeoServer, this styling is accomplished using a markup language called `Styled Layer Descriptor <http://www.opengeospatial.org/standards/sld>`_, or SLD for short.  SLD is an XML-based markup language and is very powerful, though it can be intimidating.  This page will give a basic introduction to what one can do with SLD and how GeoServer handles it.

.. note:: Since GeoServer uses SLD exclusively for styling, the terms "SLD" and "style" will often be used interchangeably.

Types of styling
----------------

Data that GeoServer can serve consists of three classes of shapes:  **Points, lines, and polygons**.  Lines (one dimensional shapes) are the simplest, as they have only the edge to style (also known as "stroke").  Polygons, two dimensional shapes, have an edge and an inside (also known as a "fill"), both of which can be styled differently.  Points, even though they lack dimension, have both an edge and a fill (not to mention a size) that can be styled.  For fills, color can be specified; for strokes, color and thickness can be specified.  

More advanced styling is possible than just color and thickness.  Points can be specified with well-known shapes like circles, squares, stars, and even custom graphics or text.  Lines can be styled with a dash styles and hashes.  Polygons can be filled with a custom tiled graphics.  Styles can be based on attributes in the data, so that certain features are styled differently.  Text labels on features are possible as well.  Features can be styled based on zoom level, with the size of the feature determining how it is displayed.  The possibilities are vast.

Style metadata
--------------

GeoServer and SLD
-----------------

Every layer (featuretype) registered with GeoServer needs to have at least one style associated with it.  GeoServer comes bundled with a few basic styles, and any number of new styles can be added.  It is possible to change any layer's associated style at any time in the :ref:`webadmin_layers` page of the :ref:`web_admin`.  When adding a layer and a style to GeoServer at the same time, the style should be added first, so that the new layer can be associated with the style immediately.  You can add a style in the :ref:`webadmin_styles` menu of the :ref:`web_admin`.  

Definitions
-----------

Symbolizer
``````````

Rule
````

FeatureTypeStyle
````````````````


A basic style
-------------

This SLD takes a layer that contains points, and styles them as red circles with a size of 6 pixels.  (This is the first example in the :ref:`sld_cookbook_points` section of the :ref:`sld_cookbook`.)

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

   
Don't let the lengthy nature of this simple example intimidate; only a few lines are really important to understand.  **Line 14** states that we are using a "PointSymbolizer", a style for point data.  **Line 17** states that we are using a "well known name", a circle, to style the points.  There are many well known names for shapes such as "square", "star", "triangle", etc.  **Lines 18-20** states to fill the shape with a color of ``#FF0000`` (red).  This is an RGB color code, written in hexadecimal, in the form of #RRGGBB.  Finally, **line 22** specifies that the size of the shape is 6 pixels in width.  The rest of the structure contains metadata about the style, such as Name/Title/Abstract.

Many more examples can be found in the :ref:`sld_cookbook`.
 
.. note:: You will find that some tags have prefixes, such as ``ogc:`` in front of them.  The reason for this is because they are **XML namespaces**.  In the tag on **lines 2-7**, there are two XML namespaces, one called ``xmlns``, and one called ``xmlns:ogc``.  Tags corresponding to the first namespace do not need a prefix, but tags corresponding to the second require a prefix of ``ogc:``.  It should be pointed out that the name of the namespaces are not important:  The first namespace could be ``xmlns:sld`` (as it often is) and then all of the tags in this example would require an ``sld:`` prefix.  The important part is that the namespaces need to match the tags.

Troubleshooting
---------------

SLD is a type of programming language, not unlike creating a web page or building a script.  As such, problems may arise that may require troubleshooting.  When adding a style into GeoServer, it is automatically checked for validation with the OGC SLD specification (although that may be bypassed), but it will not be checked for errors.  It is very easy to have syntax errors creep into a valid SLD.  Most of the time this will result in a map displaying no features (a blank map), but sometimes errors will prevent the map from even loading at all.

The easiest way to fix errors in an SLD is to try to isolate the error.  If the SLD is long and incorporates many different rules and filters, try temporarily removing some of them to see if the errors go away.

To minimize errors when creating the SLD, it is recommended to use a text editor that is designed to work with XML.  Editors designed for XML can make finding and removing errors much easier by providing syntax highlighting and (sometimes) built-in error checking.