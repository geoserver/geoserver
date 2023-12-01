.. _sld_reference_layers:

Layers
======

An SLD document contains a sequence of layer definitions indicating the 
layers to be styled.  
Each layer definition is either a **NamedLayer** reference 
or a supplied **UserLayer**.

NamedLayer
----------

A **NamedLayer** specifies an existing layer to be styled,
and the styling to apply to it.
The styling may be any combination of catalog styles and explicitly-defined styles.
If no style is specified, the default style for the layer is used.

The ``<NamedLayer>`` element contains the following elements:

.. list-table::
   :widths: 25 15 60
   
   * - **Tag**
     - **Required?**
     - **Description**
   * - ``<Name>``
     - Yes
     - The name of the layer to be styled.
       (Ignored in catalog styles.)
   * - ``<Description>``
     - No
     - The description for the layer.
   * - ``<NamedStyle>``
     - 0..N
     - The name of a catalog style to apply to the layer.
   * - ``<UserStyle>``
     - 0..N
     - The definition of a style to apply to the layer.
       See :ref:`sld_reference_styles`
       
       
       
UserLayer
---------

A **UserLayer** defines a new layer to be styled,
and the styling to apply to it. 
The data for the layer is provided directly in the layer definition
using the ``<InlineFeature>`` element.
Since the layer is not known to the server,
the styling must be explicitly specified as well.

The ``<UserLayer>`` element contains the following elements:

.. list-table::
   :widths: 25 15 60
   
   * - **Tag**
     - **Required?**
     - **Description**
   * - ``<Name>``
     - No
     - The name for the layer being defined
   * - ``<Description>``
     - No
     - The description for the layer
   * - ``<InlineFeature>``
     - No
     - One or more feature collections providing the layer data,
       specified using GML.
   * - ``<UserStyle>``
     - 1..N
     - The definition of the style(s) to use for the layer.
       See :ref:`sld_reference_styles`
      
A common use is to define a geometry to be rendered
to indicate an Area Of Interest.

.. _sld_reference_inlinefeature:

InlineFeature
-------------

An **InlineFeature** element contains data defining a layer to be styled.
The element contains one or more ``<FeatureCollection>`` elements defining
the data. 
Each Feature Collection can contain any number of ``<featureMember>`` elements, 
each containing a feature specified using GML markup.
The features can contain any type of geometry (point, line or polygon,
and collections of these).  
They may also contain scalar-valued attributes, which can be useful 
for labelling.

Example
^^^^^^^

The following style specifies a named layer using the default style,
and a user-defined layer with inline data and styling.
It displays the US States layer, with a labelled red box surrounding the Pacific NW.

.. code-block:: xml

   <sld:StyledLayerDescriptor xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
      xmlns:gml="http://www.opengis.net/gml/3.2" xmlns:ogc="http://www.opengis.net/ogc"
      xmlns:sld="http://www.opengis.net/sld" version="1.0.0">
      <sld:NamedLayer>
         <sld:Name>usa:states</sld:Name>
      </sld:NamedLayer>
      <sld:UserLayer>
         <sld:Name>Inline</sld:Name>
         <sld:InlineFeature>
            <sld:FeatureCollection>
               <sld:featureMember>
                 <feature>
                   <geometryProperty>
                     <gml:Polygon>
                        <gml:outerBoundaryIs>
                           <gml:LinearRing>
                              <gml:coordinates>
              -127.0,51.0 -110.0,51.0 -110.0,41.0 -127.0,41.0 -127.0,51.0   
                              </gml:coordinates>
                           </gml:LinearRing>
                        </gml:outerBoundaryIs>
                     </gml:Polygon>
                   </geometryProperty>
                   <title>Pacific NW </title>
                 </feature>
               </sld:featureMember>
            </sld:FeatureCollection>
         </sld:InlineFeature>
         <sld:UserStyle>
            <sld:FeatureTypeStyle>
               <sld:Rule>
	             <sld:PolygonSymbolizer>
                   <Stroke>
                     <CssParameter name="stroke">#FF0000</CssParameter>
                     <CssParameter name="stroke-width">2</CssParameter>
                   </Stroke>
                 </sld:PolygonSymbolizer>
                 <sld:TextSymbolizer>
                   <sld:Label>
                     <ogc:PropertyName>title</ogc:PropertyName>
                   </sld:Label>
                   <sld:Fill>
                     <sld:CssParameter name="fill">#FF0000</sld:CssParameter>
                   </sld:Fill>
                 </sld:TextSymbolizer>
               </sld:Rule>
            </sld:FeatureTypeStyle>
         </sld:UserStyle>
      </sld:UserLayer>
   </sld:StyledLayerDescriptor>




