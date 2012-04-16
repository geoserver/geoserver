.. _sld_parameter_substitution:

Variable substitution in SLD
=============================

Variable substitution in SLD is a GeoServer extension (starting in version 2.0.2) that allows passing values from WMS requests into SLD styles.
This allows dynamically setting values such as colors, fonts, sizes and filter thresholds.

Variables are specified in WMS ``GetMap`` requests by using the ``env`` request parameter followed by a list of ``name:value`` pairs separated by semicolons::

  ...&env=name1:value1;name2=value2&... 

In an SLD the variable values are accessed using the ``env`` function. 
The function retrieves the value specified for a substitution variable in the current request:
   
.. code-block:: xml 
   
   <ogc:Function name="env">
      <ogc:Literal>size</ogc:Literal>
   </ogc:Function>       
   
A default value can be provided, which will be used if the variable was not specified in the request:

.. code-block:: xml 
   
   <ogc:Function name="env">
      <ogc:Literal>size</ogc:Literal>
      <ogc:Literal>6</ogc:Literal>
   </ogc:Function>  
   
   
The ``env`` function can be used anywhere an OGC expression is allowed. 
For example, it can be used in ``CSSParameter`` elements, in size and offset elements, and in rule filter expressions. 
The GeoServer SLD parser also accepts it in some places where full expressions are not allowed, such as in the ``Mark/WellKnownName`` element.
   
Example
-------     
 
The following SLD symbolizer has been parameterized in three places, with default values provided in each case:

.. code-block:: xml

          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName><ogc:Function name="env">
                      <ogc:Literal>name</ogc:Literal>
                      <ogc:Literal>square</ogc:Literal>
                   </ogc:Function>
                </WellKnownName>
                <Fill>
                  <CssParameter name="fill">
                    #<ogc:Function name="env">
                      <ogc:Literal>color</ogc:Literal>
                      <ogc:Literal>FF0000</ogc:Literal>
                   </ogc:Function>
                  </CssParameter>
                </Fill>
              </Mark>
              <Size>
                 <ogc:Function name="env">
                    <ogc:Literal>size</ogc:Literal>
                    <ogc:Literal>6</ogc:Literal>
                 </ogc:Function>
              </Size>
            </Graphic>
          </PointSymbolizer>
          
:download:`Download the full SLD style <artifacts/parpoint.sld>`

When no variables are provided in the WMS request, the SLD uses the default values and renders the sample ``sf:bugsites`` dataset as shown:

.. figure:: images/default.png

   *Default rendering* 

If the request is changed to specify the following variable values::
  
   &env=color:00FF00;name:triangle;size:12
   
the result is instead:

.. figure:: images/triangles.png

   *Rendering with varialbes supplied* 
