.. _ge_feature_kml_height_time:

KML Height and Time
===================

Height
------

GeoServer by default creates two dimensional overlays in Google Earth.  However, GeoServer can output features with
height information (also called "KML extrudes") if desired. This can have the effect of having features "float" above
the ground, or create bar graph style structures in the shape of the features. The height of features can be linked to
an attribute of the data.

Setting the height of features is determined by using a KML Freemarker template. Create a file called ``height.ftl``,
and save it in the same directory as the featuretype in your :ref:`datadir`. For example, to create a height
template for the ``states`` layer, the file should be saved in
``<data_dir>/workspaces/topp/states_shapefile/states/height.ftl``.

To set the height based on an attribute, the syntax is::

   ${ATTRIBUTE.value}

Replace the word ``ATTRIBUTE`` with the name of the height attribute in your data set.  For a complete tutorial on
working with the height templates see :ref:`tutorials_heights`.

Time
----

Google Earth also contains a "time slider", which can allow animations of data, and show changes over time.  As with
height, time can be linked to an attribute of the data, as long as the data set has a date/time attribute. Linking this
date/time attribute to the time slider in Google Earth is accomplished by creating a Freemarker template. Create a file
called ``time.ftl``, and save it in the same  directory that contains your data's ``info.xml``.

To set the time based on an attribute the syntax is::

   ${DATETIME_ATTRIBUTE.value}

Replace the word ``DATETIME_ATTRIBUTE`` with the name of the date/time attribute. When creating KML, GeoServer will
automatically link the data to the time element in Google Earth. If set successfully, the time slider will
automatically appear.

For a full tutorial on using GeoServer with Google Earth's time slider see :ref:`tutorials_time`
