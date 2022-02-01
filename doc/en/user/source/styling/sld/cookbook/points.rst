.. _sld_cookbook_points:

Points
======

While points are seemingly the simplest type of shape, possessing only position and no other dimensions, there are many different ways that a point can be styled in SLD.

.. warning:: The code examples shown on this page are **not the full SLD code**, as they omit the SLD header and footer information for the sake of brevity.  Please use the links to download the full SLD for each example.

.. _sld_cookbook_points_attributes:

Example points layer
--------------------

The :download:`points layer <artifacts/sld_cookbook_point.zip>` used for the examples below contains name and population information for the major cities of a fictional country. For reference, the attribute table for the points in this layer is included below.

.. list-table::
   :widths: 30 40 30

   * - **fid** (Feature ID)
     - **name** (City name)
     - **pop** (Population)
   * - point.1
     - Borfin
     - 157860
   * - point.2
     - Supox City
     - 578231
   * - point.3
     - Ruckis
     - 98159
   * - point.4
     - Thisland
     - 34879
   * - point.5
     - Synopolis
     - 24567
   * - point.6
     - San Glissando
     - 76024
   * - point.7
     - Detrania
     - 205609

:download:`Download the points shapefile <artifacts/sld_cookbook_point.zip>`

.. _sld_cookbook_points_simplepoint:

Simple point
------------

This example specifies points be styled as red circles with a diameter of 6 pixels.

.. figure:: images/point_simplepoint.png
   :align: center

   *Simple point*
   
Code
~~~~

:download:`View and download the full "Simple point" SLD <artifacts/point_simplepoint.sld>`

.. code-block:: xml 
   :linenos: 

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



Details
~~~~~~~

There is one ``<Rule>`` in one ``<FeatureTypeStyle>`` for this SLD, which is the simplest possible situation.  (All subsequent examples will contain one ``<Rule>`` and one ``<FeatureTypeStyle>`` unless otherwise specified.)  Styling points is accomplished via the ``<PointSymbolizer>`` (**lines 3-13**).  **Line 6** specifies the shape of the symbol to be a circle, with **line 8** determining the fill color to be red (``#FF0000``).  **Line 11** sets the size (diameter) of the graphic to be 6 pixels.


.. _sld_cookbook_points_simplepointwithstroke:

Simple point with stroke
------------------------

This example adds a stroke (or border) around the :ref:`sld_cookbook_points_simplepoint`, with the stroke colored black and given a thickness of 2 pixels.

.. figure:: images/point_simplepointwithstroke.png
   :align: center

   *Simple point with stroke*

Code
~~~~

:download:`View and download the full "Simple point with stroke" SLD <artifacts/point_simplepointwithstroke.sld>`

.. code-block:: xml 
   :linenos: 

      <FeatureTypeStyle>
        <Rule>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>circle</WellKnownName>
                <Fill>
                  <CssParameter name="fill">#FF0000</CssParameter>
                </Fill>
                <Stroke>
                  <CssParameter name="stroke">#000000</CssParameter>
                  <CssParameter name="stroke-width">2</CssParameter>
                </Stroke>
              </Mark>
              <Size>6</Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>
      </FeatureTypeStyle>

Details
~~~~~~~

This example is similar to the :ref:`sld_cookbook_points_simplepoint` example.  **Lines 10-13** specify the stroke, with **line 11** setting the color to black (``#000000``) and **line 12** setting the width to 2 pixels.


Rotated square
--------------

This example creates a square instead of a circle, colors it green, sizes it to 12 pixels, and rotates it by 45 degrees.

.. figure:: images/point_rotatedsquare.png
   :align: center

   *Rotated square*

Code
~~~~

:download:`View and download the full "Rotated square" SLD <artifacts/point_rotatedsquare.sld>`

.. code-block:: xml 
   :linenos: 

      <FeatureTypeStyle>
        <Rule>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>square</WellKnownName>
                <Fill>
                  <CssParameter name="fill">#009900</CssParameter>
                </Fill>
              </Mark>
              <Size>12</Size>
              <Rotation>45</Rotation>
            </Graphic>
          </PointSymbolizer>
        </Rule>
      </FeatureTypeStyle>



Details
~~~~~~~

In this example, **line 6** sets the shape to be a square, with **line 8** setting the color to a dark green (``#009900``).  **Line 11** sets the size of the square to be 12 pixels, and **line 12** set the rotation is to 45 degrees.


Transparent triangle
--------------------

This example draws a triangle, creates a black stroke identical to the :ref:`sld_cookbook_points_simplepointwithstroke` example, and sets the fill of the triangle to 20% opacity (mostly transparent).

.. figure:: images/point_transparenttriangle.png
   :align: center

   *Transparent triangle*

Code
~~~~   

:download:`View and download the full "Transparent triangle" SLD <artifacts/point_transparenttriangle.sld>`

.. code-block:: xml 
   :linenos:

      <FeatureTypeStyle>
        <Rule>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>triangle</WellKnownName>
                <Fill>
                  <CssParameter name="fill">#009900</CssParameter>
                  <CssParameter name="fill-opacity">0.2</CssParameter>
                </Fill>
                <Stroke>
                  <CssParameter name="stroke">#000000</CssParameter>
                  <CssParameter name="stroke-width">2</CssParameter>
                </Stroke>
              </Mark>
              <Size>12</Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>
      </FeatureTypeStyle>



Details
~~~~~~~

In this example, **line 6** once again sets the shape, in this case to a triangle.  **Line 8** sets the fill color to a dark green (``#009900``) and **line 9** sets the opacity to 0.2 (20% opaque).  An opacity value of 1 means that the shape is drawn 100% opaque, while an opacity value of 0 means that the shape is drawn 0% opaque, or completely transparent.  The value of 0.2 (20% opaque) means that the fill of the points partially takes on the color and style of whatever is drawn beneath it.  In this example, since the background is white, the dark green looks lighter.  Were the points imposed on a dark background, the resulting color would be darker.  **Lines 12-13** set the stroke color to black (``#000000``) and width to 2 pixels.  Finally, **line 16** sets the size of the point to be 12 pixels in diameter.

Point as graphic
----------------

This example styles each point as a graphic instead of as a simple shape.

.. figure:: images/point_pointasgraphic.png
   :align: center

   *Point as graphic*

Code
~~~~

:download:`View and download the full "Point as graphic" SLD <artifacts/point_pointasgraphic.sld>`

.. code-block:: xml 
   :linenos:

      <FeatureTypeStyle>
        <Rule>
          <PointSymbolizer>
            <Graphic>
              <ExternalGraphic>
                <OnlineResource
                  xlink:type="simple"
                  xlink:href="smileyface.png" />
                <Format>image/png</Format>
              </ExternalGraphic>
              <Size>32</Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>
      </FeatureTypeStyle>
	  


Details
~~~~~~~

This style uses a graphic instead of a simple shape to render the points.  In SLD, this is known as an ``<ExternalGraphic>``, to distinguish it from the commonly-used shapes such as squares and circles that are "internal" to the renderer.  **Lines 5-10** specify the details of this graphic.  **Line 8** sets the path and file name of the graphic, while **line 9** indicates the format (MIME type) of the graphic (image/png). In this example, the graphic is contained in the same directory as the SLD, so no path information is necessary in **line 8**,  although a full URL could be used if desired.  **Line 11** determines the size of the displayed graphic; this can be set independently of the dimensions of the graphic itself, although in this case they are the same (32 pixels).  Should a graphic be rectangular, the ``<Size>`` value will apply to the *height* of the graphic only, with the width scaled proportionally.

.. figure:: images/smileyface.png
   :align: center

   *Graphic used for points*

.. _sld_cookbook_points_pointwithdefaultlabel:

Point with default label
------------------------

This example shows a text label on the :ref:`sld_cookbook_points_simplepoint` that displays the "name" attribute of the point. This is how a label will be displayed in the absence of any other customization.

.. figure:: images/point_pointwithdefaultlabel.png
   :align: center

   *Point with default label*

Code
~~~~

:download:`View and download the full "Point with default label" SLD <artifacts/point_pointwithdefaultlabel.sld>`

.. code-block:: xml 
   :linenos:

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
          <TextSymbolizer>
            <Label>
              <ogc:PropertyName>name</ogc:PropertyName>
            </Label>
            <Fill>
              <CssParameter name="fill">#000000</CssParameter>
            </Fill>
          </TextSymbolizer>
        </Rule>
      </FeatureTypeStyle>



Details
~~~~~~~

**Lines 3-13**, which contain the ``<PointSymbolizer>``, are identical to the :ref:`sld_cookbook_points_simplepoint` example above.  The label is set in the ``<TextSymbolizer>`` on **lines 14-27**.  **Lines 15-17** determine what text to display in the label, which in this case is the value of the "name" attribute.  (Refer to the attribute table in the :ref:`sld_cookbook_points_attributes` section if necessary.)  **Line 19** sets the text color.  All other details about the label are set to the renderer default, which here is Times New Roman font, font color black, and font size of 10 pixels.  The bottom left of the label is aligned with the center of the point.


.. _sld_cookbook_points_pointwithstyledlabel:

Point with styled label
-----------------------

This example improves the label style from the :ref:`sld_cookbook_points_pointwithdefaultlabel` example by centering the label above the point and providing a different font name and size.

.. figure:: images/point_pointwithstyledlabel.png
   :align: center

   *Point with styled label*

Code
~~~~   

:download:`View and download the full "Point with styled label" SLD <artifacts/point_pointwithstyledlabel.sld>`

.. code-block:: xml 
   :linenos:

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
          <TextSymbolizer>
            <Label>
              <ogc:PropertyName>name</ogc:PropertyName>
            </Label>
            <Font>
              <CssParameter name="font-family">Arial</CssParameter>
              <CssParameter name="font-size">12</CssParameter>
              <CssParameter name="font-style">normal</CssParameter>
              <CssParameter name="font-weight">bold</CssParameter>
            </Font>
            <LabelPlacement>
              <PointPlacement>
                <AnchorPoint>
                  <AnchorPointX>0.5</AnchorPointX>
                  <AnchorPointY>0.0</AnchorPointY>
                </AnchorPoint>
                <Displacement>
                  <DisplacementX>0</DisplacementX>
                  <DisplacementY>5</DisplacementY>
                </Displacement>
              </PointPlacement>
            </LabelPlacement>
            <Fill>
              <CssParameter name="fill">#000000</CssParameter>
            </Fill>
          </TextSymbolizer>
        </Rule>
      </FeatureTypeStyle>


Details
~~~~~~~

In this example, **lines 3-13** are identical to the :ref:`sld_cookbook_points_simplepoint` example above.  The ``<TextSymbolizer>`` on lines 14-39 contains many more details about the label styling than the previous example, :ref:`sld_cookbook_points_pointwithdefaultlabel`.  **Lines 15-17** once again specify the "name" attribute as text to display.  **Lines 18-23** set the font information:  **line 19** sets the font family to be "Arial", **line 20** sets the font size to 12, **line 21** sets the font style to "normal" (as opposed to "italic" or "oblique"), and **line 22** sets the font weight to "bold" (as opposed to "normal").  **Lines 24-35** (``<LabelPlacement>``) determine the placement of the label relative to the point.  The ``<AnchorPoint>`` (**lines 26-29**) sets the point of intersection between the label and point, which here (**line 27-28**) sets the point to be centered (0.5) horizontally axis and bottom aligned (0.0) vertically with the label.  There is also ``<Displacement>`` (**lines 30-33**), which sets the offset of the label relative to the line, which in this case is 0 pixels horizontally (**line 31**) and 5 pixels vertically (**line 32**).  Finally, **line 37** sets the font color of the label to black (``#000000``).

The result is a centered bold label placed slightly above each point.



Point with rotated label
------------------------

This example builds on the previous example, :ref:`sld_cookbook_points_pointwithstyledlabel`, by rotating the label by 45 degrees, positioning the labels farther away from the points, and changing the color of the label to purple.

.. figure:: images/point_pointwithrotatedlabel.png
   :align: center

   *Point with rotated label*

Code
~~~~

:download:`View and download the full "Point with rotated label" SLD <artifacts/point_pointwithrotatedlabel.sld>`

.. code-block:: xml 
   :linenos:

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
          <TextSymbolizer>
            <Label>
              <ogc:PropertyName>name</ogc:PropertyName>
            </Label>
            <Font>
              <CssParameter name="font-family">Arial</CssParameter>
              <CssParameter name="font-size">12</CssParameter>
              <CssParameter name="font-style">normal</CssParameter>
              <CssParameter name="font-weight">bold</CssParameter>
            </Font>
            <LabelPlacement>
              <PointPlacement>
                <AnchorPoint>
                  <AnchorPointX>0.5</AnchorPointX>
                  <AnchorPointY>0.0</AnchorPointY>
                </AnchorPoint>
                <Displacement>
                  <DisplacementX>0</DisplacementX>
                  <DisplacementY>25</DisplacementY>
                </Displacement>
                <Rotation>-45</Rotation>
              </PointPlacement>
            </LabelPlacement>
            <Fill>
              <CssParameter name="fill">#990099</CssParameter>
            </Fill>
          </TextSymbolizer>
        </Rule>
      </FeatureTypeStyle>



Details
~~~~~~~

This example is similar to the :ref:`sld_cookbook_points_pointwithstyledlabel`, but there are three important differences.  **Line 32** specifies 25 pixels of vertical displacement.  **Line 34** specifies a rotation of "-45" or 45 degrees counter-clockwise.  (Rotation values increase clockwise, which is why the value is negative.)  Finally, **line 38** sets the font color to be a shade of purple (``#99099``).

Note that the displacement takes effect before the rotation during rendering, so in this example, the 25 pixel vertical displacement is itself rotated 45 degrees.


Attribute-based point
---------------------

This example alters the size of the symbol based on the value of the population ("pop") attribute.  

.. figure:: images/point_attributebasedpoint.png
   :align: center

   *Attribute-based point*
   
Code
~~~~

:download:`View and download the full "Attribute-based point" SLD <artifacts/point_attribute.sld>`

.. code-block:: xml 
   :linenos:

      <FeatureTypeStyle>
        <Rule>
          <Name>SmallPop</Name>
          <Title>1 to 50000</Title>
          <ogc:Filter>
            <ogc:PropertyIsLessThan>
              <ogc:PropertyName>pop</ogc:PropertyName>
              <ogc:Literal>50000</ogc:Literal>
            </ogc:PropertyIsLessThan>
          </ogc:Filter>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>circle</WellKnownName>
                <Fill>
                  <CssParameter name="fill">#0033CC</CssParameter>
                </Fill>
              </Mark>
              <Size>8</Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>
        <Rule>
          <Name>MediumPop</Name>
          <Title>50000 to 100000</Title>
          <ogc:Filter>
            <ogc:And>
              <ogc:PropertyIsGreaterThanOrEqualTo>
                <ogc:PropertyName>pop</ogc:PropertyName>
                <ogc:Literal>50000</ogc:Literal>
              </ogc:PropertyIsGreaterThanOrEqualTo>
              <ogc:PropertyIsLessThan>
                <ogc:PropertyName>pop</ogc:PropertyName>
                <ogc:Literal>100000</ogc:Literal>
              </ogc:PropertyIsLessThan>
            </ogc:And>
          </ogc:Filter>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>circle</WellKnownName>
                <Fill>
                  <CssParameter name="fill">#0033CC</CssParameter>
                </Fill>
              </Mark>
              <Size>12</Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>
        <Rule>
          <Name>LargePop</Name>
          <Title>Greater than 100000</Title>
          <ogc:Filter>
            <ogc:PropertyIsGreaterThanOrEqualTo>
              <ogc:PropertyName>pop</ogc:PropertyName>
              <ogc:Literal>100000</ogc:Literal>
            </ogc:PropertyIsGreaterThanOrEqualTo>
          </ogc:Filter>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>circle</WellKnownName>
                <Fill>
                  <CssParameter name="fill">#0033CC</CssParameter>
                </Fill>
              </Mark>
              <Size>16</Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>
      </FeatureTypeStyle>



Details
~~~~~~~
   
.. note:: Refer to the :ref:`sld_cookbook_points_attributes` to see the attributes for this data.  This example has eschewed labels in order to simplify the style, but you can refer to the example :ref:`sld_cookbook_points_pointwithstyledlabel` to see which attributes correspond to which points.

This style contains three rules.  Each ``<Rule>`` varies the style based on the value of the population ("pop") attribute for each point, with smaller values yielding a smaller circle, and larger values yielding a larger circle.

The three rules are designed as follows:

.. list-table::
   :widths: 20 30 30 20

   * - **Rule order**
     - **Rule name**
     - **Population** ("pop")
     - **Size**
   * - 1
     - SmallPop
     - Less than 50,000
     - 8
   * - 2
     - MediumPop
     - 50,000 to 100,000
     - 12
   * - 3
     - LargePop
     - Greater than 100,000
     - 16

The order of the rules does not matter in this case, since each shape is only rendered by a single rule.

The first rule, on **lines 2-22**, specifies the styling of those points whose population attribute is less than 50,000.  **Lines 5-10** set this filter, with **lines 6-9** setting the "less than" filter, **line 7** denoting the attribute ("pop"), and **line 8** the value of 50,000.  The symbol is a circle (**line 14**), the color is dark blue (``#0033CC``, on **line 16**), and the size is 8 pixels in diameter (**line 19**).  

The second rule, on **lines 23-49**, specifies a style for points whose population attribute is greater than or equal to 50,000 and less than 100,000.  The population filter is set on **lines 26-37**.  This filter is longer than in the first rule because two criteria need to be specified instead of one: a "greater than or equal to" and a "less than" filter.  Notice the ``And`` on **line 27** and **line 36**.  This mandates that both filters need to be true for the rule to be applicable.  The size of the graphic is set to 12 pixels on **line 46**.  All other styling directives are identical to the first rule.

The third rule, on **lines 50-70**, specifies a style for points whose population attribute is greater than or equal to 100,000.  The population filter is set on **lines 53-58**, and the only other difference is the size of the circle, which in this rule (**line 67**) is 16 pixels.

The result of this style is that cities with larger populations have larger points.


Zoom-based point
----------------

This example alters the style of the points at different zoom levels.

.. figure:: images/point_zoombasedpointlarge.png
   :align: center

   *Zoom-based point: Zoomed in*

.. figure:: images/point_zoombasedpointmedium.png
   :align: center
   
   *Zoom-based point: Partially zoomed*

.. figure:: images/point_zoombasedpointsmall.png
   :align: center
   
   *Zoom-based point: Zoomed out*

   
Code
~~~~

:download:`View and download the full "Zoom-based point" SLD <artifacts/point_zoom.sld>`

.. code-block:: xml 
   :linenos:

      <FeatureTypeStyle>
        <Rule>
          <Name>Large</Name>
          <MaxScaleDenominator>160000000</MaxScaleDenominator>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>circle</WellKnownName>
                <Fill>
                  <CssParameter name="fill">#CC3300</CssParameter>
                </Fill>
              </Mark>
              <Size>12</Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>
        <Rule>
          <Name>Medium</Name>
          <MinScaleDenominator>160000000</MinScaleDenominator>
          <MaxScaleDenominator>320000000</MaxScaleDenominator>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>circle</WellKnownName>
                <Fill>
                  <CssParameter name="fill">#CC3300</CssParameter>
                </Fill>
              </Mark>
              <Size>8</Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>
        <Rule>
          <Name>Small</Name>
          <MinScaleDenominator>320000000</MinScaleDenominator>
          <PointSymbolizer>
            <Graphic>
              <Mark>
                <WellKnownName>circle</WellKnownName>
                <Fill>
                  <CssParameter name="fill">#CC3300</CssParameter>
                </Fill>
              </Mark>
              <Size>4</Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>
      </FeatureTypeStyle>




Details
~~~~~~~

It is often desirable to make shapes larger at higher zoom levels when creating a natural-looking map.  This example styles the points to vary in size based on the zoom level (or more accurately, scale denominator).  Scale denominators refer to the scale of the map.  A scale denominator of 10,000 means the map has a scale of 1:10,000 in the units of the map projection.

.. note:: Determining the appropriate scale denominators (zoom levels) to use is beyond the scope of this example.

This style contains three rules.  The three rules are designed as follows:

.. list-table::
   :widths: 25 25 25 25 

   * - **Rule order**
     - **Rule name**
     - **Scale denominator**
     - **Point size**
   * - 1
     - Large
     - 1:160,000,000 or less
     - 12
   * - 2
     - Medium
     - 1:160,000,000 to 1:320,000,000
     - 8
   * - 3
     - Small
     - Greater than 1:320,000,000
     - 4

The order of these rules does not matter since the scales denominated in each rule do not overlap.

The first rule (**lines 2-16**) is for the smallest scale denominator, corresponding to when the view is "zoomed in".  The scale rule is set on **line 4**, so that the rule will apply to any map with a scale denominator of 160,000,000 or less.  The rule draws a circle (**line 8**), colored red (``#CC3300`` on **line 10**) with a size of 12 pixels (**line 13**).

The second rule (**lines 17-32**) is the intermediate scale denominator, corresponding to when the view is "partially zoomed".  The scale rules are set on **lines 19-20**, so that the rule will apply to any map with a scale denominator between 160,000,000 and 320,000,000.  (The ``<MinScaleDenominator>`` is inclusive and the ``<MaxScaleDenominator>`` is exclusive, so a zoom level of exactly 320,000,000 would *not* apply here.)  Aside from the scale, the only difference between this rule and the first is the size of the symbol, which is set to 8 pixels on **line 29**.

The third rule (**lines 33-47**) is the largest scale denominator, corresponding to when the map is "zoomed out".  The scale rule is set on **line 35**, so that the rule will apply to any map with a scale denominator of 320,000,000 or more.  Again, the only other difference between this rule and the others is the size of the symbol, which is set to 4 pixels on **line 44**.

The result of this style is that points are drawn larger as one zooms in and smaller as one zooms out.

