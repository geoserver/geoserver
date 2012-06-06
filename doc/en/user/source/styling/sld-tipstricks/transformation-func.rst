.. _transformation_func:

Styling using Transformation Functions
======================================

The Symbology Encoding 1.1 specification defines the following **transformation functions**:

* ``Recode`` transforms a set of discrete attribute values into another set of values
* ``Categorize`` transforms a continuous-valued attribute into a set of discrete values
* ``Interpolate`` transforms a continuous-valued attribute into another continuous range of values

These functions provide a concise way to compute styling parameters from feature attribute values.
Geoserver implements them as :ref:`filter_function` with the same names.

.. note::

   The GeoServer function syntax is slightly different to the SE 1.1 definition,
   since the specification defines extra syntax elements 
   which are not available in GeoServer functions. 

These functions can make style documents more concise,
since they express logic which would otherwise require
many separate rules or complex Filter expressions,
They even allow logic which is impossible to express any other way.
A further advantage is that they often provide superior performance
to explicit rules.

One disadvantage of using these functions for styling is that 
they are not displayed in WMS legend graphics.

Recode
------

The ``Recode`` filter function transforms a set of discrete values for an attribute
into another set of values.
The function can be used within SLD styling parameters 
to convert the value of a feature attribute
into specific values for a parameter such as color, size, width, opacity, etc.

The transformation is defined by a set of *(input, output)* value pairs.

Example
^^^^^^^

Consider a chloropleth map of the US states dataset 
using the fill color to indicate the topographic regions for the states.  
The dataset has an attribute ``SUB_REGION`` which contains the region code for each state.
The ``Recode`` function is used to map each region code into a different color.

The symbolizer for this style is:

.. code-block:: xml

          <PolygonSymbolizer>
             <Fill>
               <CssParameter name="fill">
                 <ogc:Function name="Recode">
                   <!-- Value to transform -->
                   <ogc:Function name="strTrim">
                     <ogc:PropertyName>SUB_REGION</ogc:PropertyName>
                   </ogc:Function>
                   
                   <!-- Map of input to output values -->
                   <ogc:Literal>N Eng</ogc:Literal>
                   <ogc:Literal>#6495ED</ogc:Literal>
                   
                   <ogc:Literal>Mid Atl</ogc:Literal>
                   <ogc:Literal>#B0C4DE</ogc:Literal>
                   
                   <ogc:Literal>S Atl</ogc:Literal>
                   <ogc:Literal>#00FFFF</ogc:Literal>  
                   
                   <ogc:Literal>E N Cen</ogc:Literal>
                   <ogc:Literal>#9ACD32</ogc:Literal>
                   
                   <ogc:Literal>E S Cen</ogc:Literal>
                   <ogc:Literal>#00FA9A</ogc:Literal>
                   
                   <ogc:Literal>W N Cen</ogc:Literal>
                   <ogc:Literal>#FFF8DC</ogc:Literal>
                   
                   <ogc:Literal>W S Cen</ogc:Literal>
                   <ogc:Literal>#F5DEB3</ogc:Literal>
                   
                   <ogc:Literal>Mtn</ogc:Literal>
                   <ogc:Literal>#F4A460</ogc:Literal>
                   
                   <ogc:Literal>Pacific</ogc:Literal>
                   <ogc:Literal>#87CEEB</ogc:Literal>
                 </ogc:Function>  
               </CssParameter>
             </Fill>
          </PolygonSymbolizer>
   
This style produces the following output:

.. figure:: images/recode_usa_region.png


Categorize
----------

The ``Categorize`` filter function transforms a continuous-valued attribute
into a set of discrete values.
The function can be used within SLD styling parameters 
to convert the value of a feature attribute
into specific values for a parameter such as color, size, width, opacity, etc.

The transformation is defined by a set of *(input, output)* value pairs
specifying how ranges in the input map to output values.

Example
^^^^^^^

Consider a chloropleth map of the US states dataset 
using the fill color to indicate a categorization of the states by population.  
The dataset has attributes ``PERSONS`` and ``LAND_KM`` from which the population density 
can be computed using the ``Div`` operator.
The ``Categorize`` function is used to map density ranges code into a set of colors.

The symbolizer for this style is:

.. code-block:: xml

          <PolygonSymbolizer>
             <Fill>
               <CssParameter name="fill">
                 <ogc:Function name="Categorize">
                   <!-- Value to transform -->
                   <ogc:Div>
                     <ogc:PropertyName>PERSONS</ogc:PropertyName>
                     <ogc:PropertyName>LAND_KM</ogc:PropertyName>
                   </ogc:Div>
                   
                   <!-- Output values and thresholds -->
                   <ogc:Literal>#87CEEB</ogc:Literal>
                   <ogc:Literal>20</ogc:Literal>
                   <ogc:Literal>#FFFACD</ogc:Literal>
                   <ogc:Literal>100</ogc:Literal>
                   <ogc:Literal>#F08080</ogc:Literal>
                   
                 </ogc:Function>  
               </CssParameter>
             </Fill>
          </PolygonSymbolizer>


This style produces the following output:

.. figure:: images/categorize_usa_popdensity.png



Interpolate
-----------

The ``Interpolate`` filter function transforms a continuous-valued attribute
into another continuous range of values.
The function can be used within SLD styling parameters 
to convert the value of a feature attribute
into a continuous-valued parameter
such as color, size, width, opacity, etc.

The transformation is defined by a set of *(input, output)* control points 
chosen along a desired mapping curve.
Input values are interpolated along the curve to compute output values.

The function is able to compute either numeric or color values as output.
This is known as the **interpolation method**, 
and is specified by an optional parameter with a value of ``numeric`` (the default) or ``color``.

The *shape* of the mapping curve between control points is specified by the **interpolation mode**,
which is an optional parameter with values of 
``linear`` (the default), ``cubic``, or ``cosine``.

Example
^^^^^^^

Interpolating over color ranges allows concisely specifying 
continuously-varying colors for chloropleth (thematic) maps.
As an example, consider a chloropleth map of the US states dataset 
using the fill color to indicate the population of the states.  
The dataset has an attribute ``PERSONS`` which contains the population of each state.
The population values lie in the range 0 to around 30,000,000.  
(The range used for interpolation is 0 - 23,000,000, to create a better spread of values through the range.) 
The population values are interpolated across a curve mapping population count into colors.
In this case three number/color pairs are used to define the mapping curve, 
int order to reveal the population difference in low-population states
as well as accomodate the states with very large populations.
Because the interpolation is being performed over color values, 
the method parameter is supplied, with a value of ```color``.
No interpolation mode is supplied, so the default linear interpolation is used.

The symbolizer for this style is:

.. code-block:: xml

       <PolygonSymbolizer>
          <Fill>
            <CssParameter name="fill">
              <ogc:Function name="Interpolate">
                <!-- Property to transform -->
                <ogc:PropertyName>PERSONS</ogc:PropertyName>
                  
                <!-- Mapping curve definition pairs (input, output) -->
                <ogc:Literal>0</ogc:Literal>
                <ogc:Literal>#fefeee</ogc:Literal>
                   
                <ogc:Literal>9000000</ogc:Literal>
                <ogc:Literal>#00ff00</ogc:Literal>
                   
                <ogc:Literal>23000000</ogc:Literal>
                <ogc:Literal>#ff0000</ogc:Literal>
                   
                <!-- Interpolation method -->
                <ogc:Literal>color</ogc:Literal>
                
                <!-- Interpolation mode - defaults to linear -->
              </ogc:Function>  
            </CssParameter>
          </Fill>
       </PolygonSymbolizer>
   
This symbolizer produces the following output:

.. figure:: images/interpolate_usa_pop.png






