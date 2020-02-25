.. _styling_workshop_mbstyle_quickstart:

MBStyle Quickstart
==================

In the last section, we saw how the OGC defines style using XML documents (called SLD files).

We will now explore GeoServer styling in greater detail using a tool to generate our SLD files. The **MBStyle** GeoServer extension is used to generate SLD files using the **MabBox Style** styling language. Styles written in this language can also be used to style :ref:`vector tiles <vectortiles>` in client-side applications.

Using the MBStyle extension to define styles results in shorter examples that are easier to understand. At any point we will be able to review the generated SLD file.

Reference:

* :ref:`MBStyle Reference <mbstyle_reference>`

MBStyle Syntax
--------------

This section provides a quick introduction to MBStyle syntax for mapping professionals who may not be familiar with JSON.

JSON Syntax
^^^^^^^^^^^

All MBStyles consist of a JSON document. There are three types of structures in a JSON document:

#. Object, a collection of key-value pairs. All JSON documents are JSON objects.

#. Array, a collection of values.

#. Value, the value in a key-value pair, or an entry in an array. Values can be objects, arrays, strings, numbers, `true`, `false`, or `null`.

=========== ========================================================================
Object      A collection of key-value pairs, enclosed by curly braces and delimited by commas. Keys are surronded by quotes and seperarted from values by a colon.
Array       A collection values, enclosed by square brackets and delimited by commas.
String      Text value.  Must be surrounded by quotes.
Number      Numerical value. Must not be surrounded by quotes.
Boolean     `true` or `false`.
Null        `null`. Represents an undefined or unset value.
=========== ========================================================================

MBStyle Specification
^^^^^^^^^^^^^^^^^^^^^

The `Mapbox Style specification <https://www.mapbox.com/mapbox-gl-js/style-spec/>`_ defines a number of additional rules that MBStyles must follow.

Root-level Properties
~~~~~~~~~~~~~~~~~~~~~

Root level properties of a Mapbox style specify the map's layers, tile sources and other resources, and default values for the initial camera position when not specified elsewhere.

The following root-level properties are required for all MBStyles. Additional root-level properties which are supported but not required can be found in the spec.

=========== ========================================================================
`version`   The version of the Mapbox Style specification to use. Must be set to `8`.
`name`      The name of the style.
`sources`   An object defining the source data. Not used by GeoServer.
`layers`    An array of layer style objects
=========== ========================================================================

For example: ::

    {
        "version": 8,
        "name": "Streets",
        "sources": {...},
        "layers": [...]
    }

Sources
~~~~~~~

The sources parameter consists of a collection of named sources which define vector tile data the style is to be applied to. This is only used for MBStyles used in client-side applications, and is ignored by GeoServer. If you are only using MBStyles to style your layers within GeoServer, you don't need a sources parameter. However, if you also want to use your MBStyles for client-side styling, you will need the sources parameter.

A GeoServer vector tile source would be defined like this:

 .. code-block:: json

    {
      "cookbook": {
        "type": "vector",
        "tiles": [
          "http://localhost:8080/geoserver/gwc/service/wmts?REQUEST=GetTile&SERVICE=WMTS&VERSION=1.0.0&LAYER=cookbook&STYLE=&TILEMATRIX=EPSG:900913:{z}&TILEMATRIXSET=EPSG:900913&FORMAT=application/vnd.mapbox-vector-tile&TILECOL={x}&TILEROW={y}"
        ],
        "minZoom": 0,
        "maxZoom": 14
      }
    }

Layers
~~~~~~

The layers parameter contains the primary layout and styling information in the MBStyle. Each layer in the layers list is a self-contained block of styling information. Layers are applied in order, so the last layer in the layers list will be rendered at the top of the image.

.. note :: A layer in an MBStyle is not the same as a layer in GeoServer. A GeoServer layer is a raster or vector dataset that represents a collection of geographic features. A MBStyle layer is a block of styling information, similar to a SLD Symbolizer.

<Example>


Reference:

* :ref:`MBStyle Styling <mbstyle_styling>` (User Guide)
* `Mapbox Style specification <https://www.mapbox.com/mapbox-gl-js/style-spec/>`_


Compare MBStyle to SLD
----------------------

The MBStyle extension is built with the same GeoServer rendering engine in mind, providing access to most of the functionality of SLD. The two approaches use slightly different terminology: SLD uses terms familiar to mapping professionals, while MBStyle uses ideas more familiar to web developers.

SLD Style
^^^^^^^^^

Here is an example :download:`SLD file <../files/airports0.sld>` for reference: 

.. literalinclude:: ../files/airports0.sld
   :language: xml
   :linenos:

MBStyle Style
^^^^^^^^^^^^^

Here is the same example as :download:`MBStyle <../files/airports0.json>`: 

.. literalinclude:: ../files/airports0.json
   :language: json
   :linenos:

We use a point symbolizer to indicate we want this content drawn as a **Point** (line 16 in the SLD, line 8 in the MBStyle). The point symbolizer declares an external graphic, which contains the URL :kbd:`airports.svg` indicating the image that should be drawn (line 20 in the SLD, line 10 in the MBStyle).

.. note:: Rather than refer to many diffferent icons seperatly, MBStyles use a single spritesheet containing all the necessary icons for the style. This is defined by the ``sprite`` property at the top-level of the style.

Tour
----

To confirm everything works, let's reproduce the airports style above.

#. Navigate to the **Styles** page.

#. Each time we edit a style, the contents of the associated SLD file are replaced. Rather then disrupt any of our existing styles we will create a new style. Click :guilabel:`Add a new style` and choose the following:

   .. list-table:: 
      :widths: 30 70
      :header-rows: 0

      * - Name:
        - :kbd:`airports0`
      * - Workspace:
        - (leave empty)
      * - Format:
        - :kbd:`MBStyle`

#. Replace the initial MBStyle definition with with our airport MBStyle example and click :guilabel:`Apply`:

    .. literalinclude:: ../files/airports0.json
       :language: json

#. Click the :guilabel:`Layer Preview` tab to preview the style. We want to preview on the aiports layer, so click the name of the current layer and select :kbd:`ne:airports` from the list that appears. You can use the mouse buttons to pan and scroll wheel to change scale.

   .. figure:: ../style/img/css_02_choose_data.png

      Choosing the airports layer

   .. figure:: ../style/img/css_06_preview.png

      Layer preview

#. Click :guilabel:`Layer Data` for a summary of the selected data.

   .. figure:: ../style/img/css_07_data.png

      Layer attributes

Bonus
-----

Finished early? For now please help your neighbour so we can proceed with the workshop.

If you are really stuck please consider the following challenge rather than skipping ahead.

Explore Data
^^^^^^^^^^^^

#. Return to the :guilabel:`Data` tab and use the :guilabel:`Compute` link to determine the minimum and maximum for the **scalerank** attribute.

   .. only:: instructor
 
      .. admonition:: Instructor Notes

         Should be 2 and 9 respectively.

Challenge Compare SLD Generation
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

#. The rest API can be used to review your YAML file directly.
   
  Browser:
  
  * `view-source:http://localhost:8080/geoserver/rest/styles/airport0.json <view-source:http://localhost:8080/geoserver/rest/styles/airport0.json>`__

  Command line::

     curl -v -u admin:geoserver -XGET http://localhost:8080/geoserver/rest/styles/airports0.json

#. The REST API can also be used generate an SLD file:
   
   Browser:
   
   * `view-source:http://localhost:8080/geoserver/rest/styles/airport0.sld?pretty=true <view-source:http://localhost:8080/geoserver/rest/styles/airport0.sld?pretty=true>`__

  Command line::

     curl -v -u admin:geoserver -XGET http://localhost:8080/geoserver/rest/styles/airports0.sld?pretty=true

#. Compare the generated SLD differ above with the hand written :download:`SLD file <../files/airports0.sld>` used as an example?
   
   **Challenge:** What differences can you spot?
   
   .. only:: instructor
    
      .. admonition:: Instructor Notes      

         Generated SLD does not include name or title information; this can of course be added. Please check the MBStyle reference for details.

         The second difference is with the use of a fallback Mark when defining a PointSymbolizer.