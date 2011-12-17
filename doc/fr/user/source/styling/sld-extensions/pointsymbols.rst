.. _pointsymbols:

Point symbology in GeoServer
============================

Point symbology in GeoServer is supported via the SLD ``Graphic`` element. The element can appear in ``PointSymbolizer``, but also be used as a ``GraphicStroke`` to repeat symbols over lines and as a ``GraphicFill`` to fill polygons with tiled and repeated symbols. 

``Graphic`` can either contain a ``Mark`` or a ``ExternalGraphic``. 
Marks are simple vector symbols that can be stroked and filled at the SLD editor command, whilst ``ExternalGraphic`` refer to external files, such as PNG or SVG, that already contain all the information needed to paint the symbol, both shape and color.

In SLD both ``Mark`` and ``ExternalGraphic`` are defined to be pretty static, however GeoServer allows the user to embed attribute names into them for value expansion at run-time, this is known as `dynamic symbolizer support`. 

Mark support in GeoServer
-------------------------

GeoServer supports all of the standard ``Mark`` defined in the SLD standard, plus an open ended set of symbol extensions.
All the symbol names have to be placed in ``WellKnownName`` section of a ``Mark`` definition, see the :ref:`sld_reference_pointsymbolizer` reference for further details as well as the various examples in the cookbook section dedicated to :ref:`sld_cookbook_points`. 

Built-in symbols
~~~~~~~~~~~~~~~~

The SLD specification mandates the support of the following symbols:

.. list-table::
   :widths: 20 80
   
   * - **Name**
     - **Description**
   * - ``square``
     - A square
   * - ``circle``
     - A circle
   * - ``triangle``
     - A triangle pointing up
   * - ``star``
     - five-pointed star
   * - ``cross``
     - A square cross with space around (not suitable for hatch fills)
   * - ``x``
     - A square X with space around (not suitable for hatch fills)

The shape symbols
~~~~~~~~~~~~~~~~~

The shape symbols set adds a number of extra symbols that are not part of the basic set, they are all prefixed by ``shape://``

.. list-table::
   :widths: 20 80
   
   * - **Name**
     - **Description**
   * - ``shape://vertline``
     - A vertical line (suitable for hatch fills or to make railroad symbols)
   * - ``shape://horline``
     - A horizontal line (suitable for hatch fills)
   * - ``shape://slash`` 
     - A diagonal line leaning forwards like the "slash" keyboard symbol (suitable for diagonal hatches)
   * - ``shape://backslash``
     - Same as ``shape://slash``, but oriented in the opposite direction
   * - ``shape://dot``
     - A very small circle with space around
   * - ``shape://plus``
     - A + symbol, without space around (suitable for cross-hatch fills)
   * - ``shape://times``
     - A "X" symbol, without space around (suitable for cross-hatch fills)
   * - ``shape://oarrow``
     - An open arrow symbol (triangle without one side, suitable for placing arrows at the end of lines)
   * - ``shape://carrow``
     - An closed arrow symbol (closed triangle, suitable for placing arrows at the end of lines)

The TTF marks
~~~~~~~~~~~~~

It is possible to create a mark out of any decorative/symbol True Type Font, such as Wingdings, WebDings, and the many symbol fonts available on the internet using the following syntax::
   
   ttf://<fontname>#<hexcode>

where ``fontname`` is the full name of a TTF font available to GeoServer whilst ``hexcode`` is the hexadecimal code of the symbol. In order to get the hex code of a symbol it is possible to use the "char map" utilities available in major operating systems (Windows and Gnome on Linux both have one).

For example, say we want to use the "shield" symbol contained in Webdings, the Gnome charmap reports the symbol code as follows:

.. figure:: images/charmap.png
   :align: center

   *Selecting the code out of the Gnome char map*

Thus the SLD snipped to use the shield will be:

.. code-block:: xml 
   :linenos: 
 
    <PointSymbolizer>
        <Graphic>
          <Mark>
            <WellKnownName>ttf://Webdings#0x0064</WellKnownName>
            <Fill>
              <CssParameter name="fill">#AAAAAA</CssParameter>
            </Fill>
            <Stroke/>
          </Mark>
        <Size>16</Size>
      </Graphic>
    </PointSymbolizer>

This will result in the following map symbols:

.. figure:: images/shields.png
   :align: center

   *Shields on the map*

Adding your own
~~~~~~~~~~~~~~~

The mark subsystem is open ended, one has just to implement the ``MarkFactory`` interface and declare its implementation in the ``META-INF/services/org.geotools.renderer.style.MarkFactory`` file.

While there is not much documentation the javadocs of the GeoTools MarkFactory along with the following example files should suffice:
   
   * The `factory SPI registration file <http://svn.osgeo.org/geotools/trunk/modules/library/render/src/main/resources/META-INF/services/org.geotools.renderer.style.MarkFactory>`_
   * The `TTFMarkFactory <http://svn.osgeo.org/geotools/trunk/modules/library/render/src/main/java/org/geotools/renderer/style/TTFMarkFactory.java>`_ implementation
   * The `ShapeMarkFactory <http://svn.osgeo.org/geotools/trunk/modules/library/render/src/main/java/org/geotools/renderer/style/ShapeMarkFactory.java>`_ implementation  
   
External graphics in GeoServer
------------------------------

``ExternalGraphic`` is the other source of point symbology. Unlike marks these images are used as-is, so the specification is somewhat sympler, just point at the file and specify what type of file is that using its mime type:  

.. code-block:: xml 
   :linenos: 
 
    <PointSymbolizer>
        <Graphic>
           <ExternalGraphic>
              <OnlineResource xlink:type="simple" xlink:href="http://mywebsite.com/pointsymbol.png" />
              <Format>image/png</Format>
           </ExternalGraphic>
        </Graphic>
    </PointSymbolizer>

The ``size`` element can be specified as with the ``Mark``, but when using raster graphic symbols it's better to avoid resizing them as that will blur them, and use them at their natural size instead.

The location of the symbol can also be a relative one, in that case the file will be searched inside ``$GEOSERVER_DATA_DIR/styles``, such as in the following example:

.. code-block:: xml 
   :linenos: 

    <PointSymbolizer>
      <Graphic>
        <ExternalGraphic>
          <OnlineResource xlink:type="simple" xlink:href="burg02.svg" />
          <Format>image/svg+xml</Format>
        </ExternalGraphic>
        <Size>20</Size>
      </Graphic>
    </PointSymbolizer>

In this particular example a SVG image has been used. SVG is a vector description having both shapes and color, as such it scales nicely at whatever size, thus using ``size`` is possible and recommended.

Dynamic symbolizers
-------------------

Both ``Mark`` well known name and ``ExternalGraphic/OnlineResource/href`` are supposed to be, in SLD, static strings, meaning they cannot change based on the current feature.

This makes for very verbose expressions when multiple symbols need to be used based on a feature attributes, as a different ``Rule`` and ``Symbolizer`` must be used for each different symbol.

GeoServer allows to embed attribute names, and indeed, any kind of valid CQL expression, inside both ``WellKnownName`` and ``OnlineResource/@xlink:href``.

This allows for more compact styling assuming the name of the symbol to be used can be derived from the feature attribute values. For example, if we want to display the flags of the various states in the USA and assuming the file names match the state name the following style would allow to pick each and any of them using a single rule:

.. code-block:: xml 
   :linenos: 
   
   <ExternalGraphic>
      <OnlineResource xlink:type="simple" xlink:href="http://mysite.com/tn_${STATE_ABBR}.jpg"/>
      <Format>image/gif</Format>
   </ExternalGraphic>
   
If some adaptation to the name is necessary a full CQL expression can be used instead. In particular, if the name in the attribute is upper case but the URL demands a lowercase name the following could be used instead:

.. code-block:: xml 
   :linenos: 

   <ExternalGraphic>
      <OnlineResource xlink:type="simple"
      xlink:href="http://mysite.com/tn_${strToLowerCase(STATE_ABBR)}.jpg" />
      <Format>image/gif</Format>
   </ExternalGraphic>
   
Generally speaking any CQL expression can be embedded in a url or well known name by using the ``${cql expression}`` syntax, where a simple attribute name such as ``${STATE_ABBR}`` is one of the simplest expression.
