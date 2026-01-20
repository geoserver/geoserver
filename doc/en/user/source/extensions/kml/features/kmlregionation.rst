.. _ge_feature_kml_regionation:

KML Regionation
===============

Displaying vector features on Google Earth is a very powerful way of creating nicely-styled maps. However, it is not always optimal to display all features at all times. Displaying too many features can create an unsightly map, and can adversely affect Google Earth's performance. To combat this, GeoServer's KML output includes the ability to limit features based on certain criteria. This process is known as **regionation**. Regionation is active by default when using the superoverlay KML reflector mode.


Regionation Attributes
----------------------

The most important aspect of regionation is to decide how to determine which features show up more prominently than others. This can be done either **by geometry**, or **by attribute**. One should choose the option that best exemplifies the relative "importance" of the feature. When choosing to regionate by geometry, only the larger lines and polygons will be displayed at higher zoom levels, with smaller ones being displayed when zooming in. When regionating by an attribute, the higher value of this attribute will make those features show up at higher zoom levels. (Choosing an attribute with a non-numeric value will be ignored, and will instead default to regionation by geometry.)


Regionation Strategies
----------------------

Regionation strategies sets how to determine which features should be shown at any given time or zoom level. There are five types of regionation strategies:

.. list-table::
   :widths: 20 80
   
   * - **Strategy**
     - **Description**
   * - ``best_guess``
     - (*default*) The actual strategy is determined by the type of data being operated on. If the data consists of points, the ``random`` strategy is used. If the data consists of lines or polygons, the ``geometry`` strategy is used.
   * - ``external-sorting`` 
     - Creates a temporary auxiliary database within GeoServer.  It takes slightly extra time to build the index upon first request.
   * - ``native-sorting`` 
     - Uses the default sorting algorithm of the backend where the data is hosted. It is faster than external-sorting, but will only work with PostGIS datastores.
   * - ``geometry``
     - Externally sorts by length (if lines) or area (if polygons).
   * - ``random``
     - Uses the existing order of the data and does not sort.

In most cases, the **best_guess** strategy is sufficient.


Setting Regionation Parameters
------------------------------

Regionation strategies and attributes are featuretype-specific, and therefore are set in the :ref:`data_webadmin_layers` editing page of the :ref:`web_admin`.  This can be navigated to by selecting 'Layers' on the left sidebar.
