.. _sld_reference_labeling:

Labeling
========

Controlling Label Placement
---------------------------

Controlling where the WMS server places labels with SLD is bit complex. The SLD specification only defines the most basic way of controlling placement explicitly says that defining more control is "a real can of worms". Geoserver fully supports the SLD specification plus adds a few extra parameters so you can make pretty maps.


Basic SLD Placement
-------------------

The SLD specification indicates two types of LabelPlacement:

  * for Point Geometries ("PointPlacement")
  * for Linear (line) geometries ("LinePlacement")

.. note:: Relative to Where?

  See below for the actual algorithm details, but:
    * Polygons are intersected with the viewport and the centroid is used.
    * Lines are intersected with the viewport and the middle of the line is used.


Code
````

.. code-block:: xml

  <xsd:element name="PointPlacement">
      <xsd:complexType>
        <xsd:sequence>
          <xsd:element ref="sld:AnchorPoint" minOccurs="0"/>
          <xsd:element ref="sld:Displacement" minOccurs="0"/>
          <xsd:element ref="sld:Rotation" minOccurs="0"/>
        </xsd:sequence>
      </xsd:complexType>
    </xsd:element>
    ...
    <xsd:element name="LinePlacement">    
        <xsd:complexType>
          <xsd:sequence>
            <xsd:element ref="sld:PerpendicularOffset" minOccurs="0"/>
          </xsd:sequence>
        </xsd:complexType>
    </xsd:element>


PointPlacement
--------------

When you use a <PointPlacement> element, the geometry you are labeling will be reduced to a single point (usually the "middle" of the geometry - see algorithm below for details). You can control where the label is relative to this point using the options:

.. list-table::
   :widths: 30 70 

   * - **Option** 
     - **Meaning** (Name)
   * - AnchorPoint
     - This is relative to the LABEL. Using this you can do things such as center the label on top of the point, have the label to the left of the point, or have the label centered under the point.
   * - Displacement
     - This is in PIXELS and lets you fine-tune the location of the label.
   * - Rotation
     - This is the clockwise rotation of the label in degrees.
 	
The best way to understand these is with examples:

AnchorPoint
```````````

The anchor point determines where the label is placed relative to the label point. These measurements are relative to the bounding box of the label. The (x,y) location inside the label's bounding box (specified by the AnchorPoint) is placed at the label point.

.. figure:: img/label_bbox.png
   :align: center

The anchor point is defined relative to the label's bounding box. The bottom left is (0,0), the top left is (1,1), and the middle is (0.5,0.5).

.. code-block:: xml 

  <PointPlacement>
    <AnchorPoint>
       <AnchorPointX>
       0.5
       </AnchorPointX>
       <AnchorPointY>
       0.5
       </AnchorPointY>
    </AnchorPoint>
  </PointPlacement>	

By changing the values, you can control where the label is placed.
	
	
.. figure:: img/point_x0y0_5.png	

(x=0,y=0.5) DEFAULT - place the label to the right of the label point 	

.. figure:: img/point_x0_5y0_5.png

(x=0.5,y=0.5) - place the centre of the label at the label point

.. figure:: img/point_x15y0_5.png

(x=1,y=0.5) - place the label to the left of the label point 	

.. figure:: img/point_x0_5y0.png

(x=0.5,y=0) - place the label centered above the label point


Displacement
````````````

Displacement allows fine control of the placement of the label. The displacement values are in pixels and simply move the location of the label on the resulting image.

.. code-block:: xml 

  <PointPlacement>
   <Displacement>
     <DisplacementX>
        10
     </DisplacementX>
     <DisplacementY>
         0
     </DisplacementY>
   </Displacement>
  </PointPlacement>


.. figure:: img/point_x0y0_5_displacex10.png
   :align: center
	
displacement of x=10 pixels, compare with anchor point (x=0,y=0.5) above 	

.. figure:: img/point_x0y1_displacey10.png
   :align: center

displacement of y=-10 pixels, compare with anchor point (x=0.5,y=1.0) not shown


Rotation
````````

Rotation is simple - it rotates the label clockwise the number of degrees you specify. See the examples below for how it interacts with AnchorPoints and displacements.

.. code-block:: xml
  
  <Rotation>
    45
  </Rotation>

.. figure:: img/rot1.png

simple 45 degrees rotation 	

.. figure:: img/rot2.png

45 degrees rotation with anchor point (x=0.5,y=0.5)
	
.. figure:: img/rot3.png
	
45 degrees with 40 pixel X displacement 	

.. figure:: img/rot4.png

45 degrees rotation with 40 pixel Y displacement with anchor point (x=0.5,y=0.5)


LinePlacement
`````````````

When you are labeling a line (i.e. a road or river), you can specify a <LinePlacement> element. This tells the labeling system two things:
(a) that you want Geoserver to determine the best rotation and placement for the label (b) a minor option to control how the label is placed relative to the line.

The line placement option is very simple - it only allows you to move a label up-and-down from a line.

.. code-block:: xml 

  <xs:elementname="LinePlacement">
   <xs:complexType>
     <xs:sequence>
       <xs:element ref="sld:PerpendicularOffset" minOccurs="0"/>
     </xs:sequence>
   </xs:complexType>
  </xs:element>
  ...
  <xs:element name="PerpendicularOffset" type="sld:ParameterValueType"/>

This is very similiar to the DisplacementY option (see above).

.. code-block:: xml 

  <LabelPlacement>
    <LinePlacement>
      <PerpendicularOffset>
         10
      </PerpendicularOffset>	       
    </LinePlacement>
  </LabelPlacement>

.. figure:: img/lp_1.png
	

PerpendicularOffset=0 	

.. figure:: img/lp_2.png


PerpendicularOffset=10 pixels


Composing labels from multiple attributes
`````````````````````````````````````````

The <Label> element in TextSymbolizer is said to be mixed, that is, its content can be a mixture of plain text and OGC Expressions. The mix gets interepreted as a concatenation, this means you can leverage it to get complex labels out of multiple attributes.

For example, if you want both a state name and its abbreviation to appear in a label, you can do the following:

.. code-block:: xml 

  <Label>
    <ogc:PropertyName>STATE_NAME</ogc:PropertyName> (<ogc:PropertyName>STATE_ABBR</ogc:PropertyName>)
  </Label>

and you'll get a label such as **Texas (TX)**.

If you need to add extra white space or newline, you'll stumble into an xml oddity.  The whitespace handling in the Label element is following a XML mandated rule called "collapse", in which all leading and trailing whitespaces have to be removed, whilst all whitespaces (and newlines) in the middle of the xml element are collapsed into a single whitespace.

So, what if you need to insert a newline or a sequence of two or more spaces between your property names? Enter CDATA. CDATA is a special XML section that has to be returned to the interpreter as-is, without following any whitespace handling rule.
So, for example, if you wanted to have the state abbreviation sitting on the next line you'd use the following:

.. code-block:: xml 

  <Label>
    <ogc:PropertyName>STATE_NAME</ogc:PropertyName><![CDATA[
  ]]>(<ogc:PropertyName>STATE_ABBR</ogc:PropertyName>)
  </Label>

Geoserver Specific Enhanced Options
-----------------------------------

The following options are all extensions of the SLD specification.  Using these options gives much more control over how the map looks, since the SLD standard isn't expressive enough to handle all the options one might want.  In time we hope to have them be an official part of the specification.  

.. _labeling_priority:

Priority Labeling (<Priority>)
``````````````````````````````

GeoServer has extended the standard SLD to also include priority labeling. This allows you to control which labels are rendered in preference to other labels.

For example, lets assume you have a data set like this::

   City Name   | population
   ------------+------------
   Yonkers     |     197,818
   Jersey City |     237,681
   Newark      |     280,123
   New York    |   8,107,916

Most people don't know where "Yonkers" city is, but do know where "New York" city is. On our map, we want to give "New York" priority so its more likely to be labeled when it's in conflict (overlapping) "Yonkers".

.. note:: **Standard SLD Behavior**

  If you do not have a <Priority> tag in your SLD then you get the default SLD labeling behavior. This basically means that if there's a conflict between two labels, there is no 'dispute' mechanism and its random which label will be displayed.

In our TextSymbolizer we can put an Expression to retreive or calculate the priority for each feature:

.. code-block:: xml 

  <Priority>
      <PropertyName>population</PropertyName>
  </Priority>


.. figure:: img/priority_all.png
   :align: center


Location of the cities (see population data above)

.. figure:: img/priority_some.png
   :align: center


New York is labeled in preference to the less populated cities. Without priority labeling, "Yonkers" could be labeled in preference to New York, making a difficult to interpret map.

.. figure:: img/priority_lots.png
   :align: center

Notice that larger cities are more readily named than smaller cities.

.. _labeling_group:

Grouping Geometries (<VendorOption name="group">)
`````````````````````````````````````````````````

Sometimes you will have a set of related features that you only want a single label for. The grouping option groups all features with the same label text, then finds a representative geometry for the group.

Roads data is an obvious example - you only want a single label for all of "main street", not a label for every piece of "main street."

.. figure:: img/group_not.png
   :align: center

When the grouping option is off (default), grouping is not performed and each geometry is labeled (space permitting).

.. figure:: img/group_yes.png
   :align: center

With the grouping option on, all the geometries with the same label are grouped together and the label position is determined from ALL the geometries.

.. list-table::
   :widths: 30 70 

   * - **Geometry** 
     - **Representative Geometry**
   * - Point Set
     - 	first point inside the view rectangle is used.
   * - Line Set
     - lines are (a) networked together (b) clipped to the view rectangle (c) middle of the longest network path is used.
   * - Polygon Set
     - polygons are (a) clipped to the view rectangle (b) the centroid of the largest polygon is used.

.. code-block:: xml
 
  <VendorOption name="group">yes</VendorOption>


.. warning::  Watch out - you could group together two sets of features by accident. For example, you could create a single group for "Paris" which contains features for Paris (France) and Paris (Texas).

.. _labeling_space_around:

Overlapping and Separating Labels (<VendorOption name="spaceAround">)
`````````````````````````````````````````````````````````````````````

By default geoserver will not put labels "on top of each other". By using the spaceAround option you can allow labels to overlap and you can also add extra space around a label.

.. code-block:: xml
 
  <VendorOption name="spaceAround">10</VendorOption>

.. figure:: img/space_0.png
   :align: center

Default behavior ("0") - the bounding box of a label cannot overlap the bounding box of another label.

.. figure:: img/space_neg.png
   :align: center

With a negative spaceAround value, overlapping is allowed.

.. figure:: img/space_10.png
   :align: center

With a spaceAround value of 10 for all TextSymbolizers, each label will be 20 pixels apart from each other (see below).

**NOTE**: the value you specify (an integer in pixels) actually provides twice the space that you might expect. This is because you can specify a spaceAround for one label as 5, and for another label (in another TextSymbolizer) as 3. The distance between them will be 8. For two labels in the first symbolizer ("5") they will each be 5 pixels apart from each other, for a total of 10 pixels!

.. note:: **Interaction with different values in different TextSymbolizers**

  You can have multiple TextSymbolizers in your SLD file, each with a different spaceAround option. This will normally do what you would think if all your spaceAround options are >=0. If you have negative values ('allow overlap') then these labels can overlap labels that you've said should not be overlapping. If you dont like this behavior, its not too difficult to change - feel free to submit a patch!

.. _labeling_follow_line:

followLine
``````````

The **followLine** option forces a label to follow the curve of the line. To use this option place the following in your *<TextSymbolizer>*.

.. code-block:: xml
  
  <VendorOption name="followLine">true</VendorOption>  

It is required to use *<LinePlacement>* along with this option to ensure that all labels are correctly following the lines:

.. code-block:: xml

  <LabelPlacement>
    <LinePlacement/>
  </LabelPlacement>

.. _labeling_max_displacement:

maxDisplacement
```````````````

The **maxDisplacement** option controls the displacement of the label along a line. Normally GeoServer would label a line at its center point only, provided the location is not busy with another label, and not label it at all otherwise. When set, the labeller will search for another location within **maxDisplacement** pixels from the pre-computed label point.

When used in conjunction with **repeat**, the value for **maxDisplacement** should always be lower than the value for repeat.

.. code-block:: xml

  <VendorOption name="maxDisplacement">10</VendorOption> 

.. _labeling_repeat:

repeat
``````

The **repeat** option determines how often GeoServer labels a line. Normally GeoServer would label each line only once, regardless of their length. Specify a positive value to make it draw the label every **repeat** pixels.

.. code-block:: xml

  <VendorOption name="repeat">100</VendorOption>


.. _labeling_all_group:

labelAllGroup
`````````````

The **labelAllGroup** option makes sure that all of the segments in a line group are labeled instead of just the longest one.

.. code-block:: xml

  <VendorOption name="labelAllGroup">true</VendorOption>

.. _labeling_max_angle_delta:

maxAngleDelta
`````````````

Designed to use used in conjuection with **followLine**, the **maxAngleDelta** option sets the maximum angle, in degrees, between two subsequent characters in a curved label. Large angles create either visually disconnected words or overlapping characters. It is advised not to use angles larger than 30.

.. code-block:: xml

  <VendorOption name="maxAngleDelta">15</VendorOption>

.. _labeling_autowrap:

autoWrap
`````````

The **autoWrap** option wraps labels when they exceed the given value, given in pixels. Make sure to give a dimension wide enough to accommodate the longest word other wise this option will split words over multiple lines.

.. code-block:: xml

  <VendorOption name="autoWrap">50</VendorOption>

.. _labeling_force_left_to_right:

forceLeftToRight
````````````````

The labeller always tries to draw labels so that they can be read, meaning the label does not always follow the line orientation, but sometimes it's flipped 180° instead to allow for normal reading. This may get in the way if the label is a directional arrow, and you're trying to show one way directions (assuming the geometry is oriented along the one way, and that you have a flag to discern one ways from streets with both circulations).

The following setting disables label flipping, making the label always follow the natural orientation of the line being labelled:

.. code-block:: xml

  <VendorOption name="forceLeftToRigth">false</VendorOption>

.. _labeling_conflict_resolution:

conflictResolution
````````````````````

By default labels are subjected to conflict resolution, meaning the renderer will not allow any label to overlap with a label that has been drawn already. Setting this parameter to false pull the label out of the conflict resolution game, meaning the label will be drawn even if it overlaps with other labels, and other labels drawn after it won't mind overlapping with it.

.. code-block:: xml

  <VendorOption name="conflictResolution">false</VendorOption>

.. _labeling_goodness_of_fit:

Goodness of Fit
````````````````

Geoserver will remove labels if they are a particularly bad fit for the geometry they are labeling.

.. list-table::
   :widths: 30 70 

   * - **Geometry** 
     - **Goodness of Fit Algorithm**
   * - Point
     - Always returns 1.0 since the label is at the point
   * - Line
     - Always returns 1.0 since the label is always placed on the line.
   * - Polygon
     - The label is sampled approximately at every letter. The distance from these points to the polygon is determined and each sample votes based on how close it is to the polygon. (see LabelCacheDefault#goodnessOfFit())

The default value is 0.5, but it can be modified using:

.. code-block:: xml

  <VendorOption name="goodnessOfFit">0.3</VendorOption>
  
Polygon alignment
````````````````````

GeoServer normally tries to place horizontal labels within a polygon, and give up in case the label position is busy or if the label does not fit enough in the polygon. This options allows GeoServer to try alternate rotations for the labels.

.. code-block:: xml

  <VendorOption name="polygonAlign">mbr</VendorOption>


.. list-table::
   :widths: 30 70 

   * - **Option** 
     - **Description**
   * - manual
     - The default value, only the rotation manually specified in the ``<Rotation>`` tag will be used
   * - ortho
     - If the label does not fit horizontally and the polygon is taller than wider the vertical alignement will also be tried
   * - mbr
     - If the label does not fit horizontally the minimum bounding rectangle will be computed and a label aligned to it will be tried out as well
     
     
     