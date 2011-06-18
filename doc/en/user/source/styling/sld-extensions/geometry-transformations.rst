.. _geometry_transformations:

Geometry transformations in SLD
===============================

Each symbolizer in SLD 1.0 contains a `<Geometry>` element allowing the user to specify which geometry is to be used for rendering. In the most common case it is not specified, but it becomes useful in the case a feature has multiple geometries inside.

SLD 1.0 forces the `<Geometry>` content to be a `<ogc:PropertyName>`, GeoServer relaxes this constraint and allows a generic `sld:expression` to be used instead. Common expressions cannot manipulate geometries, but GeoServer provides a number of filter functions that can actually manipulate geometries by transforming them into something different: this is what we call *geometry transformations* in SLD.

A full list of transformations is available in the :ref:`filter_function_reference`.

Transformations are pretty flexible, the major limitation of them is that they happen in the geometry own reference system and unit, before any reprojection and rescaling to screen happens.

Let's look into some examples.

Extracting vertices
-------------------

Here is an example that allows one to extract all the vertices of a geometry, and make them visible in a map, using the `vertices` function:

.. code-block:: xml 
   :linenos: 

      <PointSymbolizer>
        <Geometry>
          <ogc:Function name="vertices">
             <ogc:PropertyName>the_geom</ogc:PropertyName>
          </ogc:Function>
        </Geometry>
        <Graphic>
          <Mark>
            <WellKnownName>square</WellKnownName>
            <Fill>
              <CssParameter name="fill">#FF0000</CssParameter>
            </Fill>
          </Mark>
          <Size>6</Size>
        </Graphic>
     </PointSymbolizer>

:download:`View the full "Vertices" SLD <artifacts/vertices.sld>`

Applied to the sample `tasmania_roads` layer this will result in:

.. figure:: images/vertices.png
   :align: center
   
   *Extracting and showing the vertices out of a geometry*
   
   
Start and end point
-------------------

The `startPoint` and `endPoint` functions can be used to extract the start and end point of a line. 

.. code-block:: xml
   :linenos:
     
   <PointSymbolizer>
     <Geometry>
       <ogc:Function name="startPoint">
         <ogc:PropertyName>the_geom</ogc:PropertyName>
       </ogc:Function>
     </Geometry>
     <Graphic>
       <Mark>
         <WellKnownName>square</WellKnownName>
         <Stroke>
           <CssParameter name="stroke">0x00FF00</CssParameter>
           <CssParameter name="stroke-width">1.5</CssParameter>
         </Stroke>
       </Mark>
       <Size>8</Size>
     </Graphic>
    </PointSymbolizer>
    <PointSymbolizer>
      <Geometry>
        <ogc:Function name="endPoint">
          <ogc:PropertyName>the_geom</ogc:PropertyName>
        </ogc:Function>
      </Geometry>
      <Graphic>
        <Mark>
          <WellKnownName>circle</WellKnownName>
          <Fill>
             <CssParameter name="fill">0xFF0000</CssParameter>
          </Fill>
        </Mark>
        <Size>4</Size>
      </Graphic>
    </PointSymbolizer>

:download:`View the full "StartEnd" SLD <artifacts/startend.sld>`

Applied to the sample `tasmania_roads` layer this will result in:

.. figure:: images/startend.png
   :align: center
   
   *Extracting start and end point of a line*


Drop shadow
-----------

The `offset` function can be used to create drop shadow effects below polygons. Notice the odd offset value, set this way because the data used in the example is in geographic coordinates.

.. code-block:: xml 
   :linenos: 
   
     <PolygonSymbolizer>
       <Geometry>
          <ogc:Function name="offset">
             <ogc:PropertyName>the_geom</ogc:PropertyName>
             <ogc:Literal>0.00004</ogc:Literal>
             <ogc:Literal>-0.00004</ogc:Literal>
          </ogc:Function>
       </Geometry>
       <Fill>
         <CssParameter name="fill">#555555</CssParameter>
       </Fill>
     </PolygonSymbolizer>

:download:`View the full "Shadow" SLD <artifacts/shadow.sld>`

Applied to the sample `tasmania_roads` layer this will result in:

.. figure:: images/shadow.png
   :align: center
   
   *Dropping building shadows*

Other possibilities
-------------------

GeoServer set of transformations functions also contains a number of set related or constructive transformations, such as buffer, intersection, difference and so on. However, those functions are quite heavy in terms of CPU consumption so it is advise to use them with care, activating them only at the higher zoom levels.

Buffering can often be approximated by adopting very large strokes and round line joins and line caps, without actually have to perform the geometry transformation.

Adding new transformations
--------------------------

Filter functions are pluggable, meaning it's possible to build new ones in Java and then drop the resulting .jar file in GeoServer as a plugin. A guide is not available at this time, but have a look into the GeoTools main module for examples.