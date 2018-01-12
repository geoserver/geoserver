.. _wms_decorations:

WMS Decorations
===============

WMS Decorations provide a framework for visually annotating images from WMS with absolute, rather than spatial,
positioning.  Examples of decorations include compasses, legends, and watermarks.

Configuration
-------------

To use decorations in a :ref:`wms_getmap` request, the administrator must first configure a decoration layout.  These
layouts are stored in a subdirectory called ``layouts`` in the :ref:`datadir` as XML files, one file per layout.
Each layout file must have the extension ``.xml``.  Once a layout ``foo.xml`` is defined, users can request it by
adding ``&format_options=layout:foo`` to the request parameters.

Layout files follow a very simple XML structure; a root node named layout containing any number of decoration elements.
The order of the decoration elements is the order they are drawn so, in case they are overlapping, the first one will appear under the others.

Each decoration element has several attributes:

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * -  Attribute
     -  Meaning
   * -  ``type``
     -  the type of decoration to use (see :ref:`wms_decoration_types`)
   * -  ``affinity``
     -  the region of the map image to which the decoration is anchored
   * -  ``offset``
     -  how far from the anchor point the decoration is drawn
   * -  ``size``
     -  the maximum size to render the decoration.  Note that some decorations may dynamically resize themselves.

Each decoration element may also contain an arbitrary number of option elements providing a parameter name and value::

<option name="foo" value="bar"/>

Option interpretation depends on the type of decoration in use.

.. _wms_decoration_types:

Decoration Types
----------------

While GeoServer allows for decorations to be added via extension, there is a core set of decorations included in the
default installation.  These decorations include:

The **image** decoration (``type="image"``) overlays a static image file onto the document.  If height and width are
specified, the image will be scaled to fit, otherwise the image is displayed at full size.  

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - Option Name
     - Meaning
   * - ``url``
     - provides the URL or file path to the image to draw (relative to the GeoServer data directory)
   * - ``opacity``
     - a number from 0 to 100 indicating how opaque the image should be.

The **scaleratio** decoration (``type="scaleratio"``) overlays a text description of the map's scale ratio onto the
document.

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - Option Name
     - Meaning
   * - ``bgcolor``
     - the background color for the text.  supports RGB or RGBA colors specified as hex values.
   * - ``fgcolor``
     - the color for the text and border.  follows the color specification from bgcolor.
   * - ``format``
     - the number format pattern, specified using Java own `DecimalFormat <https://docs.oracle.com/javase/8/docs/api/java/text/DecimalFormat.html>`_ syntax
   * - ``formatLanguage``
     - the language used to drive number formatting (applies only if also ``format`` is used), using a valid Java `Locale <https://docs.oracle.com/javase/8/docs/api/java/util/Locale.html>`_
     

The **scaleline** decoration (``type="scaleline"``) overlays a graphic showing the scale of the map in world units.  

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - Option Name
     - Meaning
   * - ``bgcolor``
     - the background color, as used in scaleratio
   * - ``fgcolor``
     - the foreground color, as used in scaleratio
   * - ``fontsize``
     - the size of the font to use
   * - ``transparent``
     - if set to true, the background and border won't be painted (false by default)
   * - ``measurement-system"``
     - can be set to "metric" to only show metric units, "imperial" to only show imperial units, or "both" to show both of them (default)


The **legend** decoration (``type="legend"``) overlays a graphic containing legends for the layers in the map.

The **text** decoration (``type="text"``) overlays a parametric, single line text message on top of the map. The
parameter values can be fed via the the ``env`` request parameter, just like SLD enviroment parameters.

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - Option Name
     - Meaning
   * - ``message``
     - the message to be displayed, as plain text or Freemarker template that can use the ``env`` map contents to expand variables
   * - ``font-family``
     - the name of the font used to display the message, e.g., ``Arial``, defaults to ``Serif``
   * - ``font-size``
     - the size of the font to use (can have decimals), defaults to 12
   * - ``font-italic``
     - it ``true`` the font will be italic, defaults to ``false``
   * - ``font-bold``
     - if ``true`` the font will be bold, defaults to ``false``
   * - ``font-color``
     - the color of the message, in ``#RRGGBB`` or ``#RRGGBBAA`` format, defaults to black
   * - ``halo-radius``
     - the radius of a halo around the message, can have decimals, defaults to 0
   * - ``halo-color``
     - the halo fill color, in ``#RRGGBB`` or ``#RRGGBBAA`` format, defaults to white


Example
-------

A layout configuration file might look like this:

.. code-block:: xml

    <layout>
        <decoration type="image" affinity="bottom,right" offset="6,6" size="80,31">
            <option name="url" value="pbGS_80x31glow.png"/>
        </decoration>

        <decoration type="scaleline" affinity="bottom,left" offset="36,6"/>

        <decoration type="legend" affinity="top,left" offset="6,6" size="auto"/>
    </layout>

Used against the states layer from the default GeoServer data, this layout produces an image like the following.

.. figure:: img/decoration.png
   
   The default states layer, drawn with the decoration layout above.
