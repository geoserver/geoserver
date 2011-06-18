.. _filter_function:

Filter functions
================

The OGC Filter encoding specification contains a generic concept, the *filter function*.

A *filter function* is a function, with arguments, that can be called inside of a filter or, more generically, an expression, to perform specific calculations: as such it can be useful when building WFS filters or SLD style sheets. 
A filter function can be anything a trigonometric function, a string formatting one, a geometry buffer.

The filter specification does not mandate specific functions, so while the syntax to call a function is uniform, any server is free to provide whatever function it wants, so the actual invocation will work only against specific software.

Here are a couple of examples on function usage, the first is about WFS filtering, the second a way to use functions in SLD to get richer rendering.

WFS filtering example
---------------------

Let's assume we have a WFS feature type whose geometry field, ``geom``, can contain any kind of geometry. 

For a certain application we need to extract only the features whose geometry is a simple point or a multi point.
This cannot be achieved with a fully portable filter, but it can be done using a GeoServer specific filter function named ``geometryType``.
Here is how:

.. code-block:: xml 

    <wfs:GetFeature service="WFS" version="1.0.0"
      outputFormat="GML2"
      xmlns:wfs="http://www.opengis.net/wfs"
      xmlns:ogc="http://www.opengis.net/ogc"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://www.opengis.net/wfs
                          http://schemas.opengis.net/wfs/1.0.0/WFS-basic.xsd">
      <wfs:Query typeName="sf:archsites">
        <ogc:Filter>
           <ogc:PropertyIsEqualTo>
              <ogc:Function name="geometryType">
                 <ogc:PropertyName>geom</ogc:PropertyName>
              </ogc:Function>
              <ogc:Literal>Point</ogc:Literal>
           </ogc:PropertyIsEqualTo>
        </ogc:Filter>
        </wfs:Query>
    </wfs:GetFeature>
    

SLD formatting example
----------------------

We want to include elevation labels in a contour map. The label is stored as a floating point, and the resulting labelling will be something may be something like "150.0" or "149.999999". We want to avoid that and get ``150`` instead. 
To achieve this result we can use the ``numberFormat`` filter function:

.. code-block:: xml

     ...
     <TextSymbolizer>
       <Label>
         <ogc:Function name="numberFormat">
           <ogc:Literal>##</ogc:Literal>
           <ogc:PropertyName>ELEVATION</ogc:PropertyName>
         </ogc:Function>
       </Label>
       ...
     </TextSymbolizer>
     ...
     
Performance implications
------------------------

Using filter functions in SLD symbolizer expressions does not have significant overhead, unless the function is performing some very heavy computation.

However, using them in WFS or SLD filtering can take a very visible toll: this is usually because filter functions are not recognized by the native encoders, and thus the functions are not used inside the primary filters, and are performed in memory instead.

For example, given a filter like ``BBOX(geom,-10,30,20,45) and geometryType(geom) = 'Point'`` most data stores will split the filter into two separate parts, one, the bounding box filter, is actually used as a primary filter (e.g., encoded in SQL) whilst the geometry function part will be executed in memory on top of the results coming from the primary filter.