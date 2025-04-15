.. _coverage_views:

Coverage Views
==============

Starting with GeoServer 2.6.0, You can define a new raster layer as a Coverage View.
Coverage Views allow defining a View made of different bands originally available inside coverages (either bands of the same coverage or different coverages) of the same Coverage Store.

Creating a Coverage View
------------------------

In order to create a Coverage View the administrator invokes the :guilabel:`Create new layer` page.
When a Coverage store is selected, the usual list of coverages available for publication appears.
A link :guilabel:`Configure new Coverage view...` also appears:

.. figure:: images/coverageviewnewlayer.png
   :align: center

Selecting the :guilabel:`Configure new Coverage view...` link opens a new page where you can configure the coverage view.

Band select mode
----------------

By default, the CoverageView are composed in Band select mode.

.. figure:: images/coverageviewbandselect.png
   :align: center

Allowing the user to select certain coverages/bands from the provided list to be part of the output.

.. figure:: images/newcoverageview.png
   :align: center

The upper text box allows to specify the name to be assigned to this coverage view. (In the following picture we want to create as example, a **currents** view merging together both u and v components of the currents, which are exposed as separated 1band coverages).

.. figure:: images/coverageviewname.png
   :align: center

Next step is defining the output bands to be put in the coverage view.
It is possible to specify which input coverage bands need to be put on the view by selecting them from the :guilabel:`Composing coverages/bands...`.

.. figure:: images/coverageviewselectbands.png
   :align: center

Once selected, they needs to be added to the output bands of the coverage view, using the :guilabel:`add` button.

.. figure:: images/coverageviewaddbands.png
   :align: center

Optionally, is it possible to remove the newly added bands using the :guilabel:`remove` and :guilabel:`remove all` buttons.
Once done, clicking on the :guilabel:`save` button will redirect to the standard Layer configuration page.

.. figure:: images/coveragevieweditlayer.png
   :align: center

Scrolling down to the end of the page, is it possible to see the bands composing the coverage (and verify they are the one previously selected).

.. figure:: images/coverageviewbandsdetails.png
   :align: center


At any moment, the Coverage View can be refined and updated by selecting the :guilabel:`Edit Coverage view...` link available before the Coverage Bands details section.

.. figure:: images/coveragevieweditlink.png
   :align: center

Once all the properties of the layer have been configured, by selecting the :guilabel:`Save` button, the coverage will be saved in the catalog and it will become visible as a new layer.

.. figure:: images/coverageviewavailablelayers.png
   :align: center

Jiffle expressions to create coverage views
-------------------------------------------
A dropdown selector allows users to choose an alternative band composition mode: Jiffle.

.. figure:: images/coverageviewjiffleselect.png
   :align: center

This mode enables support for Jiffle expressions in the coverage view setup. When this mode is selected, a summary of all available coverages and bands is displayed.

For instance, an ImageMosaic based on Sentinel-2 coverages would produce a summary similar to the example below:

.. figure:: images/coverageviewsentinelbands.png
   :align: center


Whilst a simple True Marble Image stored on a world.tif file, with 3 RGB bands will be reported like this:

.. figure:: images/coverageviewworldbands.png
   :align: center

Defining the Jiffle Expression
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
The Output Name text area allows to define the output variable to be produced.

The Jiffle Script text area allows to define a Jiffle expression to produce that output.

For example, using the Sentinel-2 dataset, an NDVI single output band can be defined by combining the B04 and B08 bands using the following formula

.. figure:: images/coverageviewndvijiffle.png
   :align: center


Upon saving, the output band will be reported in the coverage band details:

.. figure:: images/coverageviewndvidetails.png
   :align: center


If the output needs to consist of multiple bands, an index-based syntax should be used, as shown below:

.. figure:: images/coverageview3bandsout.png
   :align: center

Where the same output name is used for all the bands (i.e. result) and bands are specified by index.

In that case, the band details of the coverage view will look like this:

.. figure:: images/coverageview3bandsdetails.png
   :align: center


Heterogeneous coverage views
----------------------------

In case the various coverages bound in the view have different resolution, the UI will present
two extra controls:

.. figure:: images/coverageviewhetero.png
   :align: center

The **coverage envelope policy** defines how the bounding box of the output is calculated for metadata
purposes. Having different resolutions, the coverages are unlikely to share the same bounding box. The possible values are:

* **Intersect envelopes**: Use the intersection of all input coverage envelopes
* **Union envelopes**: Use the union of all input coverage envelopes

The **coverage resolution policy** defines which target resolution is used when generating outputs:

* **Best**: Use the best resolution available among the chosen bands (e.g., in a set having 60m, 20m and 10m the 10m resolution will be chosen)
* **Worst**: Use the worst resolution available among the chosen bands (e.g., in a set having 60m, 20m and 10m the 60m resolution will be chosen)

The coverage resolution policy is *context sensitive*. Assume the input is a 12 bands Sentinel 2 dataset at three different
resolution, 10, 20 and 30 meters, and a false color image is generated by performing a band selection in the SLD.
If the policy is *best* and the SLD selects only bands at 20 and 60 meters, the output will be at 20 meters instead of
10 meters.

Coverage View in action
-----------------------

A Layer preview of the newly created coverage view will show the rendering of the view. Note that clicking on a point on the map will result into a GetFeatureInfo call which will report
the values of the bands composing the coverage view.

.. figure:: images/coverageviewlayerpreview.png
   :align: center
