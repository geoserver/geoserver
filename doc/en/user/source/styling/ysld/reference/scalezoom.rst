.. _ysld_reference_scalezoom:

Scale and zoom
==============

It is common for different :ref:`rules <ysld_reference_rules>` to be applied at different zoom levels on a web map. 

For example, on a roads layer, you would not not want to display every single road when viewing the whole world. Or perhaps you may wish to styles the same features differently depending on the zoom level. For example: a cities layer styled using points at low zoom levels (when "zoomed out") and with polygon borders at higher zoom levels ("zoomed in").

.. todo:: ADD FIGURE

YSLD allows rules to be applied depending on the the scale or zoom level. You can specify by scale, or you can define zoom levels in terms of scales and specify by zoom level.

.. warning:: Be aware that scales for a layer (where a style is applied) may interact differently when the layer is contained in a map, if the map has a different coordinate reference system from the layer.

Scale syntax
------------

The syntax for using a scale conditional parameter in a rule is::

  rules:
  - ...
    scale: [<min>,<max>]
    ...

where:

.. list-table::
   :class: non-responsive
   :header-rows: 1
   :stub-columns: 1
   :widths: 20 10 50 20

   * - Attribute
     - Required?
     - Description
     - Default value
   * - ``min``
     - Yes
     - The minimum scale (inclusive) for which the rule will be applied. Value is a number, either decimal or integer.
     - N/A
   * - ``max``
     - Yes
     - The maximum scale (exclusive) for which the rule will be applied. Value is a number, either decimal or integer.
     - N/A

.. note:: It is not possible to use an expression for any of these values.

Use the literal strings ``min`` and ``max`` to denote where there are no lower or upper scale boundaries. For example, to denote that the scale is anything less than some ``<max>`` value::

  scale: [min,<max>]

To denote that the scale is anything greater than or equal to some ``<min>`` value::

  scale: [<min>,max]

.. note:: In the above examples, ``min`` and ``max`` are always literals, entered exactly like that, while ``<min>`` and ``<max>`` would be replaced by actual scalar values.

If the scale parameter is omitted entirely, then the rule will apply at all scales.

Scale examples
--------------

Three rules, all applicable at different scales::

  rule:
  - name: large_scale
    scale: [min,100000]
    symbolizers:
    - line:
        stroke-width: 3
        stroke-color: '#0165CD'
  - name: medium_scale
    scale: [100000,200000]
    symbolizers:
    - line:
        stroke-width: 2
        stroke-color: '#0165CD'
  - name: small_scale
    scale: [200000,max]
    symbolizers:
    - line:
        stroke-width: 1
        stroke-color: '#0165CD'

This example will display lines with:

* A stroke width of 3 at scales less than 100,000 (``large_scale``)
* A stroke width of 2 at scales between 100,000 and 200,000 (``medium_scale``)
* A stroke width of 1 at scales greater than 200,000 (``small_scale``)

Given the rules above, the following arbitrary sample scales would map to the rules as follows:

.. list-table::
   :header-rows: 1
   :stub-columns: 1

   * - Scale
     - Rule
   * - ``50000``
     - ``large_scale``
   * - ``100000``
     - ``medium_scale``
   * - ``150000``
     - ``medium_scale``
   * - ``200000``
     - ``small_scale``
   * - ``300000``
     - ``small_scale``

Note the edge cases, since the ``min`` value is inclusive and the ``max`` value is exclusive.

Scientific notation for scales
------------------------------

To make comprehension easier and to lessen the chance of errors, scale values can be expressed in scientific notation.

So a scale of ``500000000``, which is equal to 5 × 10^8 (a 5 with eight zeros), can be replaced by ``5e8``.

Relationship between scale and zoom
-----------------------------------

When working with web maps, often it is more convenient to talk about zoom levels instead of scales. The relationship between zoom and scale is context dependent.

For example, for EPSG:4326 with world boundaries, zoom level 0 (completely zoomed out) corresponds to a scale of approximately 279,541,000 with each subsequent zoom level having half the scale value. For EPSG:3857 (Web Mercator) with world boundaries, zoom level 0 corresponds to a scale of approximately 559,082,000, again with each subsequent zoom level having half the scale value.

But since zoom levels are discrete (0, 1, 2, etc.) and scale levels are continuous, it's actually a range of scale levels that corresponds to a given zoom level.

For example, if you have a situation where a zoom level 0 corresponds to a scale of 1,000,000 (and each subsequent zoom level is half that scale, as is common), you can set the scale values of your rules to be:

* ``scale: [750000,1500000]`` (includes 1,000,000)
* ``scale: [340000,750000]`` (includes 500,000)
* ``scale: [160000,340000]`` (includes 250,000)
* ``scale: [80000,160000]`` (includes 125,000)
* etc.

Also be aware of the inverse relationship between scale and zoom; **as the zoom level increases, the scale decreases.**

Zoom syntax
-----------

In certain limited cases, it can be more useful to specify scales by way of zoom levels for predefined gridsets. These can be any predefined gridsets in GeoServer.

Inside a rule, the syntax for using zoom levels is::

  rules:
  - ...
    zoom: [<min>, <max>]
    ...

where:

.. list-table::
   :class: non-responsive
   :header-rows: 1
   :stub-columns: 1
   :widths: 20 10 50 20

   * - Attribute
     - Required?
     - Description
     - Default value
   * - ``min``
     - Yes
     - The minimum zoom level for which the rule will be applied. Value is an integer.
     - N/A
   * - ``max``
     - Yes
     - The maximum zoom level for which the rule will be applied. Value is an integer.
     - N/A

.. note:: It is not possible to use an expression for any of these values.

As with scales, use the literal strings ``min`` and ``max`` to denote where there are no lower or upper scale boundaries. For example, to denote that the zoom level is anything less than some ``<max>`` value::

  zoom: [min,<max>]

To denote that the zoom level is anything greater than or equal to some ``<min>`` value::

  zoom: [<min>,max]

.. note:: In the above examples, ``min`` and ``max`` are always literals, entered exactly like that, while ``<min>`` and ``<max>`` would be replaced by actual scalar values.

The ``scale`` and ``zoom`` parameters should not be used together in a rule (but if used, ``scale`` takes priority over ``zoom``).

Specifying a grid
-----------------

While every web map can have zoom levels, the specific relationship between a zoom level and its scale is dependent on the gridset (spatial reference system, extent, etc.) used.

So when specifying zoom levels in YSLD, you should also specify the grid. 

The ``grid`` parameter should remain at the top of the YSLD content, above any :ref:`ysld_reference_featurestyles` or :ref:`ysld_reference_rules`. The syntax is::

  grid:
    name: <string>

where:

.. list-table::
   :class: non-responsive
   :header-rows: 1
   :stub-columns: 1
   :widths: 20 10 50 20

   * - Property
     - Required?
     - Description
     - Default value
   * - ``name``
     - No
     - ``WGS84``, ``WebMercator``, or a name of a predefined gridset in GeoServer.
     - ``WebMercator``

.. note:: As many web maps use "web mercator" (also known as EPSG:3857 or EPSG:900913), this is assumed to be the default if no ``grid`` is specified.

.. warning:: As multiple gridsets can contain the same SRS, we recommend naming custom gridsets by something other than the EPSG code.


Zoom examples
-------------

.. **Initial scale**

.. Defining zoom levels based on an initial scale::

..   grid:
..     initial-scale: 6000000

.. .. note::

..    Using scientific notation::

..      grid:
..        initial-scale: 6e6

.. would define zoom levels as follows:

.. .. list-table::
..    :header-rows: 1
..    :stub-columns: 1

..    * - Scale
..      - Zoom level
..    * - ``6000000``
..      - ``0``
..    * - ``3000000``
..      - ``1``
..    * - ``1500000``
..      - ``2``
..    * - ``750000``
..      - ``3``
..    * - ``<previous_scale> / 2``
..      - ``<previous_zoom> + 1``

.. One could define the following three rules::

..   rules:
..   - name: low_zoom
..     zoom: (0,2)
..     symbolizers:
..     - line:
..         stroke-width: 1
..         stroke-color: '#0165CD'       
..   - name: medium_zoom
..     zoom: (3,5)
..     symbolizers:
..     - line:
..         stroke-width: 2
..         stroke-color: '#0165CD'       
..   - name: high_zoom
..     zoom: (6,)
..     symbolizers:
..     - line:
..         stroke-width: 3
..         stroke-color: '#0165CD'

.. This example will display lines with:

.. * A stroke width of 1 at zoom levels 0-2 (``low_zoom``)
.. * A stroke width of 2 at zoom levels 3-5 (``medium_zoom``)
.. * A stroke width of 3 at zoom levels 6 and greater (``high_zoom``)

.. Adding the ``initial-level`` parameter would change the definitions of the zoom levels::

..   grid:
..     initial-scale: 6000000
..     initial-level: 2

.. .. list-table::
..    :header-rows: 1
..    :stub-columns: 1

..    * - Scale
..      - Zoom level
..    * - ``24000000``
..      - ``0``
..    * - ``12000000``
..      - ``1``
..    * - ``6000000``
..      - ``2``
..    * - ``3000000``
..      - ``3``
..    * - ``<previous_scale> / 2``
..      - ``<previous_zoom> + 1``
 
.. Setting the ratio would adjust the multiplier between scales in adjacent zoom levels::

..   grid:
..     initial-scale: 6000000
..     ratio: 4

.. .. list-table::
..    :header-rows: 1
..    :stub-columns: 1

..    * - Scale
..      - Zoom level
..    * - ``6000000``
..      - ``0``
..    * - ``1500000``
..      - ``1``
..    * - ``375000``
..      - ``2``
..    * - ``93750``
..      - ``3``
..    * - ``<previous_scale> / 4``
..      - ``<previous_zoom> + 1``

.. **List of scales**

.. Defining zoom levels based on a list of scales::

..   grid:
..     scales:
..     - 1000000
..     - 500000
..     - 100000
..     - 50000
..     - 10000

.. .. note::

..    Using scientific notation::

..      grid:
..        scales:
..        - 1e6
..        - 5e5
..        - 1e5
..        - 5e4
..        - 1e4

.. would define the list of zoom levels explicitly and completely:

.. .. list-table::
..    :header-rows: 1
..    :stub-columns: 1

..    * - Scale
..      - Zoom level
..    * - ``1000000``
..      - ``0``
..    * - ``500000``
..      - ``1``
..    * - ``100000``
..      - ``2``
..    * - ``50000``
..      - ``3``
..    * - ``10000``
..      - ``4``


Default gridset
~~~~~~~~~~~~~~~

Given the default of web mercator (also known as EPSG:3857 or EPSG:900913), which requires no ``grid`` designation, this defines zoom levels as the following scale levels (rounded to the nearest whole number below):

.. list-table::
   :header-rows: 1
   :stub-columns: 1

   * - Scale
     - Zoom level
   * - ``559082264``
     - ``0``
   * - ``279541132``
     - ``1``
   * - ``139770566``
     - ``2``
   * - ``69885283``
     - ``3``
   * - ``34942641``
     - ``4``
   * - ``17471321``
     - ``5``
   * - ``8735660``
     - ``6``
   * - ``4367830``
     - ``7``
   * - ``2183915``
     - ``8``
   * - ``<previous_scale> / 2``
     - ``<previous_zoom> + 1``

Named gridsets
~~~~~~~~~~~~~~

For the existing gridset of ``WGS84`` (often known as ``EPSG:4326``)::

  grid:
    name: WGS84

This defines zoom levels as the following scale levels (rounded to the nearest whole number below):

.. list-table::
   :header-rows: 1
   :stub-columns: 1

   * - Scale
     - Zoom level
   * - ``559082264``
     - ``0``
   * - ``279541132``
     - ``1``
   * - ``139770566``
     - ``2``
   * - ``69885283``
     - ``3``
   * - ``34942641``
     - ``4``
   * - ``17471321``
     - ``5``
   * - ``8735660``
     - ``6``
   * - ``4367830``
     - ``7``
   * - ``2183915``
     - ``8``
   * - ``<previous_scale> / 2``
     - ``<previous_zoom> + 1``

Given a custom named gridset called ``NYLongIslandFtUS``, defined by a CRS of `EPSG:2263 <http://www.spatialreference.org/ref/epsg/2263/>`_ and using its full extent::

  grid:
    name: NYLongIslandFtUS

This defines zoom levels as the following (rounded to the nearest whole number below):

.. list-table::
   :header-rows: 1
   :stub-columns: 1

   * - Scale
     - Zoom level
   * - ``4381894``
     - ``0``
   * - ``2190947``
     - ``1``
   * - ``1095473``
     - ``2``
   * - ``547736``
     - ``3``
   * - ``273868``
     - ``4``
   * - ``136934``
     - ``5``
   * - ``68467``
     - ``6``
   * - ``34234``
     - ``7``
   * - ``17117``
     - ``8``
   * - ``<previous_scale> / 2``
     - ``<previous_zoom> + 1``

.. note::

   These scale values can be verified in GeoServer on the :guilabel:`Gridsets` page under the definition for the gridset:

   .. figure:: img/scalezoom_customgridset.png

      Gridset defined in GeoServer

   Specifically, note the :guilabel:`Scale` values under :guilabel:`Tile Matrix Set`.
