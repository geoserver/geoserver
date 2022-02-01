.. _ge_feature_customizing_placemarks:

Customizing Placemarks
======================

KML output can leverage some powerful visualization abilities in Google Earth. **Titles** can be displayed on top of the features. **Descriptions** (custom HTML shown when clicking on a feature) can be added to customize the views of the attribute data. In addition, using Google Earth's time slider, **time**-based animations can be created. Finally, **height** of features can be set, as opposed to the default ground overlay. All of these can be accomplished by creating Freemarker templates.  Freemarker templates are text files (with limited HTML code), saved in the :ref:`datadir`, that utilize variables that link to specific attributes in the data.

Titles
------

Specifying labels via a template involves creating a special text file called ``title.ftl`` and placing it into the featuretypes directory inside the :ref:`datadir` for the dataset to be labeled. For instance, to create a template to label the ``states`` layer by state name, one would create the file: ``<data_dir>/workspaces/topp/states_shapefile/states/title.ftl``. The content of the file would be::

   ${STATE_NAME.value}

.. warning:  Add SS:  Using a Freemarker template to display the value of STATE_NAME

Descriptions
------------

When working with KML, each feature is linked to a description, accessible when the feature is clicked on. By default, GeoServer creates a list of all the attributes and values for the particular feature.

.. warning:  Add SS:  Default description for a feature

It is possible to modify this default behavior. Much like with featuretype titles, which are edited by creating a ``title.ftl`` template, specifying descriptions via a template involves creating a special text file called ``description.ftl`` and placing it into the featuretypes directory inside the :ref:`datadir` for the dataset to be labeled. For instance, a sample description template would be saved here: ``<data_dir>/workspaces/topp/states_shapefile/states/description.ftl``. The content of the file could be::

   This is the state of ${STATE_NAME.value}.

The resulting description will look like this:

.. warning:: Add SS:  A custom description

It is also possible to create one description template for all layers in a given namespace. To do this, create a ``description.ftl`` file as above, and save it here::

   <data_dir>/templates/<namespace>/description.ftl.

Please note that if a description template is created for a specific layer that also has an associated namespace description template, the layer template (i.e. the most specific template) will take priority.
