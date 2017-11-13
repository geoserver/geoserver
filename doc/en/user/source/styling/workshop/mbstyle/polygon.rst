.. _styling_workshop_mbstyle_polygon:

Polygons
========

Next we look at how MBStyle styling can be used to represent polygons.

.. figure:: ../style/img/PolygonSymbology.svg
   
   Polygon Geometry

Review of polygon symbology:

* Polygons offer a direct representation of physical extent or the output of analysis.

* The visual appearance of polygons reflects the current scale.

* Polygons are recorded as a LinearRing describing the polygon boundary. Further LinearRings can be used to describe any holes in the polygon if present.
  
  The Simple Feature for SQL Geometry model (used by GeoJSON) represents these areas as Polygons, the ISO 19107 geometry model (used by GML3) represents these areas as Surfaces.

* SLD uses a **PolygonSymbolizer** to describe how the shape of a polygon is drawn. The primary characteristic documented is the **Fill** used to shade the polygon interior. The use of a **Stroke** to describe the polygon boundary is optional.

* Labeling of a polygon is anchored to the centroid of the polygon. GeoServer provides a vendor-option to allow labels to line wrap to remain within the polygon boundaries.

For our Polygon exercises we will try and limit our MBStyle documents to a single rule, in order to showcase the properties used for rendering.

Reference:

* :ref:`MBStyle Reference <mbstyle_reference>`
* `MapBox Style Spec Fill Layer <https://www.mapbox.com/mapbox-gl-js/style-spec/#layers-fill>`_
* :ref:`Polygons <sld_reference_polygonsymbolizer>` (User Manual | SLD Reference )

This exercise makes use of the ``ne:states_provinces_shp`` layer.

#. Navigate to :menuselection:`Styles`.

#. Create a new style :kbd:`polygon_example`.

   .. list-table:: 
      :widths: 30 70
      :stub-columns: 1

      * - Name:
        - :kbd:`polygon_example`
      * - Workspace:
        - :kbd:`No workspace`
      * - Format:
        - :kbd:`MBStyle`
     
   .. image:: ../style/img/polygon_02_create.png

#. Enter the following style and click :menuselection:``Apply`` to save:

   .. code-block:: json
   
      {
        "version": 8,
        "name": "polygon_example",
        "layers": [
          {
            "id": "polygon_example",
            "source-layer": "ne:states_provinces_shp",
            "type": "fill",
            "paint": {
              "fill-color": "lightgrey"
            }
          }
        ]
      }

#. Click on the tab :guilabel:`Layer Preview` to preview.

   .. image:: ../style/img/polygon_04_preview.png

#. Set ``ne:states_provinces_shp`` as the preview layer.

   .. image:: ../style/img/polygon_01_preview.png


Fill and Outline
----------------

The **fill** layer controls the display of polygon data.

.. image:: ../style/img/PolygonFill.svg

The **fill-color** property is used to provide the color used to draw the interior of a polygon.


#. Replace the contents of ``polygon_example`` with the following **fill** example:

   .. code-block:: json
   
      {
        "version": 8,
        "name": "polygon_example",
        "layers": [
          {
            "id": "polygon_example",
            "source-layer": "ne:states_provinces_shp",
            "type": "fill",
            "paint": {
              "fill-color": "gray"
            }
          }
        ]
      }

#. The :guilabel:`Map` tab can be used preview the change:

   .. image:: ../style/img/polygon_fill_1.png

#. To draw the boundary of the polygon the **fill-outline** property is used:

   The **fill-outline** property is used to provide the color of the polygon boundary. For more advanced boundary styling, use a seperate line layer.
   
   .. code-block:: json
      :emphasize-lines: 11
      
      {
        "version": 8,
        "name": "polygon_example",
        "layers": [
          {
            "id": "polygon_example",
            "source-layer": "ne:states_provinces_shp",
            "type": "fill",
            "paint": {
              "fill-color": "gray",
              "fill-outline-color": "black"
            }
          }
        ]
      }
   
   .. note:: Technically the boundary of a polygon is a specific case of a LineString where the first and last vertex are the same, forming a closed LinearRing.

#. The effect of adding **fill-outline** is shown in the map preview:
   
   .. image:: ../style/img/polygon_fill_2.png

#. An interesting technique when styling polygons in conjunction with background information is to control the fill opacity.

   The **fill-opacity** property is used to adjust transparency (provided as range from 0.0 to 1.0). Use of **fill-opacity** to render polygons works well in conjunction with a raster base map. This approach allows details of the base map to shown through. **fill-opacity** affects both the fill and the fill outline.

   The **stroke-opacity** property is used in a similar fashion, as a range from 0.0 to 1.0.

   .. code-block:: json
      :emphasize-lines: 10
      
      {
        "version": 8,
        "name": "polygon_example",
        "layers": [
          {
            "id": "polygon_example",
            "source-layer": "ne:states_provinces_shp",
            "type": "fill",
            "paint": {
              "fill-opacity": 0.5,
              "fill-color": "white",
              "fill-outline-color": "lightgrey"
            }
          }
        ]
      }

#. As shown in the map preview:

   .. image:: ../style/img/polygon_fill_3.png
   
#. This effect can be better appreciated using a layer group.
   
   .. image:: ../style/img/polygon_fill_4.png
   
   Where the transparent polygons is used lighten the landscape provided by the base map.

   .. image:: ../style/img/polygon_fill_5.png
   
.. only:: instructor
     
   .. admonition:: Instructor Notes 
    
      In this example we want to ensure readers know the key property for polygon data.
    
      It is also our first example of using opacity.

Pattern
-------

The **fill-pattern** property can be used to provide a pattern. 

.. image:: ../style/img/PolygonPattern.svg

The fill pattern is defined by repeating an image defined in a spritesheet.

#. Update `polygon_example` with the following sprite as a repeating fill pattern:

   .. code-block:: json
      :emphasize-lines: 4,11
      
      {
        "version": 8,
        "name": "polygon_example",
        "sprite": "http://localhost:8080/geoserver/styles/sprites"
        "layers": [
          {
            "id": "polygon_example",
            "source-layer": "ne:states_provinces_shp",
            "type": "fill",
            "paint": {
              "fill-pattern": "grey_square16" 
            }
          }
        ]
      }

#. The map preview (and legend) will show the result:
   
   .. image:: ../style/img/polygon_pattern_0.png


#. You can view the names of all the icons in the spritesheet by looking at its json definition, at `http://localhost:8080/geoserver/styles/sprites.json <http://localhost:8080/geoserver/styles/sprites.json>`_.

   .. literalinclude:: ../files/sprites.json
      :language: json

   Update the example to use **grey_diag16** for a pattern of left hatching. 

   .. code-block:: json

      {
        "version": 8,
        "name": "polygon_example",
        "sprite": "http://localhost:8080/geoserver/styles/sprites"
        "layers": [
          {
            "id": "polygon_example",
            "source-layer": "ne:states_provinces_shp",
            "type": "fill",
            "paint": {
              "fill-pattern": "grey_diag16" 
            }
          }
        ]
      }

#. This approach is well suited to printed output or low color devices.
   
   .. image:: ../style/img/polygon_pattern_4.png

#. Multiple fills can be applied by using a seperate layer for each fill.
   
   .. code-block:: json

      {
        "version": 8,
        "name": "polygon_example",
        "sprite": "http://localhost:8080/geoserver/styles/sprites"
        "layers": [
          {
            "id": "polygon_background",
            "source-layer": "ne:states_provinces_shp",
            "type": "fill",
            "paint": {
              "fill-color": "#DDDDFF",
              "fill-outline-color": "black"
            }
          },
          {
            "id": "polygon_pattern",
            "source-layer": "ne:states_provinces_shp",
            "type": "fill",
            "paint": {
              "fill-pattern": "grey_diag8" 
            }
          }
        ]
      }

#. The resulting image has a solid fill, with a pattern drawn overtop.

   .. image:: ../style/img/polygon_pattern_6.png

Label
-----

Labeling polygons follows the same approach used for LineStrings. 

.. image:: ../style/img/PolygonLabel.svg
   
#. By default labels are drawn starting at the centroid of each polygon.
   
   .. image:: ../style/img/LabelSymbology.svg

#. Try out **text-field** and **text-color** by replacing our ``polygon_example`` with the following:

   .. code-block:: json

      {
        "version": 8,
        "name": "polygon_example",
        "layers": [
          {
            "id": "polygon_fill",
            "source-layer": "ne:states_provinces_shp",
            "type": "fill",
            "paint": {
              "fill-color": "#7EB5D3",
              "fill-outline-color": "blue"
            }
          },
          {
            "id": "polygon_label",
            "source-layer": "ne:states_provinces_shp",
            "type": "symbol",
            "layout": {
              "text-field": "{name}" 
            },
            "paint": {
              "text-color": "black" 
            }
          }
        ]
      }

#. Each label is drawn from the lower-left corner as shown in the ``Map`` preview.
   
   .. image:: ../style/img/polygon_label_0.png

#. We can adjust how the label is drawn at the polygon centroid.

   .. image:: ../style/img/LabelAnchorPoint.svg

   The property **text-anchor** provides two numbers expressing how a label is aligned with respect to the centroid. Adjusting the **text-anchor** is the recommended approach to positioning your labels.

#. Using the **text-anchor** property we can center our labels with respect to geometry centroid.
   
   To align the center of our label we select "center" below:
   
   .. code-block:: json
      :emphasize-lines: 20
      
      {
        "version": 8,
        "name": "polygon_example",
        "layers": [
          {
            "id": "polygon_fill",
            "source-layer": "ne:states_provinces_shp",
            "type": "fill",
            "paint": {
              "fill-color": "#7EB5D3",
              "fill-outline-color": "blue"
            }
          },
          {
            "id": "polygon_label",
            "source-layer": "ne:states_provinces_shp",
            "type": "symbol",
            "layout": {
              "text-field": "{name}",
              "text-anchor": "center"
            },
            "paint": {
              "text-color": "black" 
            }
          }
        ]
      }

         
#. The labeling position remains at the polygon centroid. We adjust alignment by controlling which part of the label we are "snapping" into position.

   .. image:: ../style/img/polygon_label_1.png
   
#. The property **text-translate** can be used to provide an initial displacement using and x and y offset.

   .. image:: ../style/img/LabelDisplacement.svg
   
#. This offset is used to adjust the label position relative to the geometry centroid resulting in the starting label position.
   
   .. code-block:: json
      :emphasize-lines: 23
      
      {
        "version": 8,
        "name": "polygon_example",
        "layers": [
          {
            "id": "polygon_fill",
            "source-layer": "ne:states_provinces_shp",
            "type": "fill",
            "paint": {
              "fill-color": "#7EB5D3",
              "fill-outline-color": "blue"
            }
          },
          {
            "id": "polygon_label",
            "source-layer": "ne:states_provinces_shp",
            "type": "symbol",
            "layout": {
              "text-field": "{name}",
            },
            "paint": {
              "text-color": "black",
              "text-translate": [0, -7]
            }
          }
        ]
      }

#. Confirm this result in the map preview.
   
   .. image:: ../style/img/polygon_label_2.png

#. These two settings can be used together.

   .. image:: ../style/img/LabelBoth.svg
    
   The rendering engine starts by determining the label position generated from the geometry centroid and the **text-translate** displacement. The bounding box of the label is used with the **text-anchor** setting align the label to this location.

   **Step 1**: starting label position = centroid + displacement
   
   **Step 2**: snap the label anchor to the starting label position

#. To move our labels down (allowing readers to focus on each shape) we can use displacement combined with followed by horizontal alignment.
   
   .. code-block:: json
      :emphasize-lines: 20,24
      
      {
        "version": 8,
        "name": "polygon_example",
        "layers": [
          {
            "id": "polygon_fill",
            "source-layer": "ne:states_provinces_shp",
            "type": "fill",
            "paint": {
              "fill-color": "#7EB5D3",
              "fill-outline-color": "blue"
            }
          },
          {
            "id": "polygon_label",
            "source-layer": "ne:states_provinces_shp",
            "type": "symbol",
            "layout": {
              "text-field": "{name}",
              "text-anchor": "left"
            },
            "paint": {
              "text-color": "black",
              "text-translate": [0, -7]
            }
          }
        ]
      }

#. As shown in the map preview.
   
   .. image:: ../style/img/polygon_label_3.png
   
Legibility
----------

When working with labels a map can become busy very quickly, and difficult to read.

#. MBStyle extensive proterties for controlling the labelling process.

   One common property for controlling labeling is **text-max-width**, which allows any labels extending past the provided width will be wrapped into multiple lines.

#. Using this we can make a small improvement in our example:

   .. code-block:: json
      :emphasize-lines: 21
      
      {
        "version": 8,
        "name": "polygon_example",
        "layers": [
          {
            "id": "polygon_fill",
            "source-layer": "ne:states_provinces_shp",
            "type": "fill",
            "paint": {
              "fill-color": "#7EB5D3",
              "fill-outline-color": "blue"
            }
          },
          {
            "id": "polygon_label",
            "source-layer": "ne:states_provinces_shp",
            "type": "symbol",
            "layout": {
              "text-field": "{name}",
              "text-anchor": "center"
              "text-max-width": 14
            },
            "paint": {
              "text-color": "black",
            }
          }
        ]
      }

#. As shown in the following preview.
   
   .. image:: ../style/img/polygon_label_4.png

#. Even with this improved spacing between labels, it is difficult to read the result against the complicated line work.
   
   Use of a halo to outline labels allows the text to stand out from an otherwise busy background. In this case we will make use of the fill color, to provide some space around our labels. We will also change the font to Arial.

   .. code-block:: json
      :emphasize-lines: 22,26-27
      
      {
        "version": 8,
        "name": "polygon_example",
        "layers": [
          {
            "id": "polygon_fill",
            "source-layer": "ne:states_provinces_shp",
            "type": "fill",
            "paint": {
              "fill-color": "#7EB5D3",
              "fill-outline-color": "blue"
            }
          },
          {
            "id": "polygon_label",
            "source-layer": "ne:states_provinces_shp",
            "type": "symbol",
            "layout": {
              "text-field": "{name}",
              "text-anchor": "center"
              "text-max-width": 14,
              "text-font": ["Arial"]
            },
            "paint": {
              "text-color": "black",
              "text-halo-color": "#7EB5D3",
              "text-halo-width": 2
              
            }
          }
        ]
      }

   .. image:: ../style/img/polygon_label_5.png

Theme
-----

A thematic map (rather than focusing on representing the shape of the world) uses elements of style to illustrate differences in the data under study.  This section is a little more advanced and we will take the time to look at the generated SLD file.

.. only:: instructor

   .. admonition:: Instructor Notes   

      This instruction section follows our pattern with LineString. Building on the examples and exploring how selectors can be used.

      * For LineString we explored the use of @scale, in this section we are going to look at theming by attribute.

      * We also unpack how cascading occurs, and what the result looks like in the generated XML.

      * care is being taken to introduce the symbology encoding functions as an option for theming (placing equal importance on their use).
  
      Checklist:

      * filter vs function for theming
      * Cascading

#. We can use a site like `ColorBrewer <http://www.colorbrewer2.com>`_ to explore the use of color theming for polygon symbology. In this approach the the fill color of the polygon is determined by the value of the attribute under study.

   .. image:: ../style/img/polygon_06_brewer.png

   This presentation of a dataset is known as "theming" by an attribute.

#. For our ``ne:states_provinces_shp`` dataset, a **mapcolor9** attribute has been provided for this purpose. Theming by **mapcolor9** results in a map where neighbouring countries are visually distinct.

   +-----------------------------+
   |  Qualitative 9-class Set3   |
   +---------+---------+---------+
   | #8dd3c7 | #fb8072 | #b3de69 |
   +---------+---------+---------+
   | #ffffb3 | #80b1d3 | #fccde5 |
   +---------+---------+---------+
   | #bebada | #fdb462 | #d9d9d9 |
   +---------+---------+---------+

   If you are unfamiliar with theming you may wish to visit http://colorbrewer2.org to learn more. The **i** icons provide an adequate background on theming approaches for qualitative, sequential and diverging datasets.
  
#. The first approach we will take is to directly select content based on **colormap**, providing a color based on the **9-class Set3** palette above:

   .. code-block:: json

      {
        "version": 8,
        "name": "polygon_example",
        "layers": [
          {
            "id": "polygon_1",
            "filter": ["==", "mapcolor9", 1],
            "source-layer": "ne:states_provinces_shp",
            "type": "fill",
            "paint": {
              "fill-color": "#8DD3C7",
              "fill-outline-color": "gray"
            }
          },
          {
            "id": "polygon_2",
            "filter": ["==", "mapcolor9", 2],
            "source-layer": "ne:states_provinces_shp",
            "type": "fill",
            "paint": {
              "fill-color": "#FFFFB3",
              "fill-outline-color": "gray"
            }
          },
          {
            "id": "polygon_3",
            "filter": ["==", "mapcolor9", 3],
            "source-layer": "ne:states_provinces_shp",
            "type": "fill",
            "paint": {
              "fill-color": "#BEBADA",
              "fill-outline-color": "gray"
            }
          },
          {
            "id": "polygon_4",
            "filter": ["==", "mapcolor9", 4],
            "source-layer": "ne:states_provinces_shp",
            "type": "fill",
            "paint": {
              "fill-color": "#FB8072",
              "fill-outline-color": "gray"
            }
          },
          {
            "id": "polygon_5",
            "filter": ["==", "mapcolor9", 5],
            "source-layer": "ne:states_provinces_shp",
            "type": "fill",
            "paint": {
              "fill-color": "#80B1D3",
              "fill-outline-color": "gray"
            }
          },
          {
            "id": "polygon_6",
            "filter": ["==", "mapcolor9", 6],
            "source-layer": "ne:states_provinces_shp",
            "type": "fill",
            "paint": {
              "fill-color": "#FDB462",
              "fill-outline-color": "gray"
            }
          },
          {
            "id": "polygon_7",
            "filter": ["==", "mapcolor9", 7],
            "source-layer": "ne:states_provinces_shp",
            "type": "fill",
            "paint": {
              "fill-color": "#B3DE69",
              "fill-outline-color": "gray"
            }
          },
          {
            "id": "polygon_8",
            "filter": ["==", "mapcolor9", 8],
            "source-layer": "ne:states_provinces_shp",
            "type": "fill",
            "paint": {
              "fill-color": "#FCCDE5",
              "fill-outline-color": "gray"
            }
          },
          {
            "id": "polygon_9",
            "filter": ["==", "mapcolor9", 9],
            "source-layer": "ne:states_provinces_shp",
            "type": "fill",
            "paint": {
              "fill-color": "#D9D9D9",
              "fill-outline-color": "gray"
            }
          }
        ]
      }

#. The :guilabel:`Map` tab can be used to preview this result.

   .. image:: ../style/img/polygon_09_selector_theme.png

#. Property functions can be used to make theming substantially easier, by directly mapping property values to style values using an array of stops. MBStyle supports three types of function interpolation, which is used to define the behavior between these stops:

   * **categorical**: Used the theme qualitative data. Attribute values are directly mapped to styling property such as **fill** or **stroke-width**. Equvalent to the SLD **Recode** function.

   * **interval**: Used the theme quantitative data. Categories are defined using min and max ranges, and values are sorted into the appropriate category. Equvalent to the SLD **Categorize** function.

   * **exponential**: Used to smoothly theme quantitative data by calculating a styling property based on an attribute value. Supports a **base** attribute for controlling the steepness of interpolation. When **base** is 1, this is equivalent to the SLD **Interpolate** function.

   Theming is an activity, producing a visual result allow map readers to learn more about how an attribute is distributed spatially. We are free to produce this visual in the most efficient way possible. 

#. Swap out **mapcolor9** theme to use the **categorical** function:

   .. code-block:: json

      {
        "version": 8,
        "name": "polygon_example",
        "layers": [
          {
            "id": "polygon",
            "source-layer": "ne:states_provinces_shp",
            "type": "fill",
            "paint": {
              "fill-color": {
                "property": "mapcolor9",
                "type": "categorical",
                "stops": [
                  [1, "#8dd3c7"],
                  [2, "#ffffb3"],
                  [3, "#bebada"],
                  [4, "#fb8072"],
                  [5, "#80b1d3"],
                  [6, "#fdb462"],
                  [7, "#b3de69"],
                  [8, "#fccde5"],
                  [9, "#d9d9d9"]
                ]
              },
              "fill-outline-color": "gray"
            }
          }
        ]
      }

#. The :guilabel:`Map` tab provides the same preview.

   .. image:: ../style/img/polygon_10_recode_theme.png

#. The :guilabel:`Generated SLD` tab shows where things get interesting. Our generated style now consists of a single **Rule**:

   .. code-block:: xml

      <sld:Rule>
         <sld:PolygonSymbolizer>
            <sld:Fill>
               <sld:CssParameter name="fill">
                  <ogc:Function name="Recode">
                     <ogc:PropertyName>mapcolor9</ogc:PropertyName>
                     <ogc:Literal>1</ogc:Literal>
                        <ogc:Literal>#8dd3c7</ogc:Literal>
                     <ogc:Literal>2</ogc:Literal>
                        <ogc:Literal>#ffffb3</ogc:Literal>
                     <ogc:Literal>3</ogc:Literal>
                        <ogc:Literal>#bebada</ogc:Literal>
                     <ogc:Literal>4</ogc:Literal>
                        <ogc:Literal>#fb8072</ogc:Literal>
                     <ogc:Literal>5</ogc:Literal>
                        <ogc:Literal>#80b1d3</ogc:Literal>
                     <ogc:Literal>6</ogc:Literal>
                        <ogc:Literal>#fdb462</ogc:Literal>
                     <ogc:Literal>7</ogc:Literal>
                        <ogc:Literal>#b3de69</ogc:Literal>
                     <ogc:Literal>8</ogc:Literal>
                        <ogc:Literal>#fccde5</ogc:Literal>
                     <ogc:Literal>9</ogc:Literal>
                        <ogc:Literal>#d9d9d9</ogc:Literal>
               </ogc:Function>
               </sld:CssParameter>
            </sld:Fill>
         </sld:PolygonSymbolizer>
         <sld:LineSymbolizer>
            <sld:Stroke>
               <sld:CssParameter name="stroke">#808080</sld:CssParameter>
               <sld:CssParameter name="stroke-width">0.5</sld:CssParameter>
            </sld:Stroke>
         </sld:LineSymbolizer>
      </sld:Rule>

Bonus
-----

The following optional explore and challenge activities offer a chance to review and apply the ideas introduced here. The challenge activities equire a bit of creativity and research to complete.

In a classroom setting you are encouraged to team up into groups, with each group taking on a different challenge.

.. _mbstyle.polygon.q2:

Explore Interval
^^^^^^^^^^^^^^^^

.. only:: instructor

   .. admonition:: Instructor Notes   

      This section reviews use of the Symbology Encoding Categorize function for something else other than color. Goal is to have readers reach for SE Functions as often as selectors when styling.

      Additional exercise ideas:

      * Control size using Interpolate: While Recode offers an alternative for selectors (matching discrete values) Interpolate brings something new to the table - gradual color (or value) progression. The best of example of this is controlling width using the ``ne:rivers`` data layer (which is not yet available).

#. The **interval** function can be used to generate property values based on quantitative information. Here is an example using interval to color states according to size.

    .. code-block:: json

       {
        "version": 8,
        "name": "polygon_example",
        "layers": [
          {
            "id": "polygon",
            "source-layer": "ne:states_provinces_shp",
            "type": "fill",
            "paint": {
              "fill-color": {
                "property": "Shape_Area",
                "type": "interval",
                "stops": [
                  [0, "#08519c"],
                  [0.5, "#3182bd"],
                  [1, "#6baed6"],
                  [5, "#9ecae1"],
                  [60, "#c6dbef"],
                  [80, "#eff3ff"]
                ]
              }
            }
          }
        ]
      }
   
   .. image:: ../style/img/polygon_area.png

#. An exciting use of the GeoServer **shape** symbols is the theming by changing the **size** used for pattern density.

#. **Explore:** Use the **interval** function to theme by **datarank**.

   .. image:: ../style/img/polygon_categorize.png

   .. note:: Answer :ref:`provided <mbstyle.polygon.a2>` at the end of the workbook.

.. _mbstyle.polygon.q4:

Challenge Halo
^^^^^^^^^^^^^^

#. The halo example used the fill color and opacity for a muted halo, while this improved readability it did not bring attention to our labels.

   A common design choice for emphasis is to outline the text in a contrasting color.
   
#. **Challenge:** Produce a map that uses a white halo around black text.

   .. note:: Answer :ref:`provided <mbstyle.polygon.a4>` at the end of the workbook.

.. _mbstyle.polygon.q5:

Challenge Theming using Multiple Attributes
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. A powerful tool is theming using multiple attributes. This is an important concept allowing map readers to perform "integration by eyeball" (detecting correlations between attribute values information).

#. **Challenge:** Combine the **mapcolor9** and **datarank** examples to reproduce the following map.

   .. image:: ../style/img/polygon_multitheme.png

   .. note:: Answer :ref:`provided <mbstyle.polygon.a5>` at the end of the workbook.

.. _mbstyle.polygon.q6:

Challenge Use of Z-Index
^^^^^^^^^^^^^^^^^^^^^^^^

#. Earlier we looked at using multiple **layers** to simulate line string casing. The line work was drawn twice, once with thick line, and then a second time with a thinner line. The resulting effect is similar to text halos - providing breathing space around complex line work allowing it to stand out.
   
#. **Challenge:** Use what you know of rendering order to reproduce the following map:

   .. image:: ../style/img/polygon_zorder.png

   .. note:: Answer :ref:`provided <mbstyle.polygon.a6>` at the end of the workbook.
   

