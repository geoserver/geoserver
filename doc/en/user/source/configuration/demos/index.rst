.. _demos:

Demos
=====

This page contains helpful links to various information pages regarding GeoServer and its features. You do not need to be logged into GeoServer to access this page.

The page contains the following options

* :ref:`demos_demorequests`
* :ref:`srs_list`
* :ref:`demos_reprojectionconsole`
* :ref:`demos_wcsrequestbuilder`

.. figure:: img/demos_view.png
   
   Demos page

If you have the :ref:`WPS <wps>` extension installed, you will see an additional option: 

* :ref:`demos_wpsrequestbuilder`

.. figure:: img/demos_viewwps.png
   
   Demos page with WPS extension installed

.. _demos_demorequests:

Demo Requests
-------------

This page has example WMS, WCS, and WFS requests for GeoServer that you can use, examine, and change. Select a request from the drop down list. 

.. figure:: img/demos_requests.png

   Selecting demo requests

Both :ref:`wfs` as well as :ref:`wcs` requests will display the request URL and the XML body. :ref:`wms` requests will only display the request URL. 

.. figure:: img/demos_requests_WFS.png
   
   WFS 1.1 DescribeFeatureType sample request

Click :guilabel:`Submit` to send the request to GeoServer. For WFS and WCS requests, GeoServer will automatically generate an XML reponse.

.. figure:: img/demos_requests_schema.png
   
   XML response from a WFS 1.1 DescribeFeatureType sample request
   
Submitting a WMS GetMap request displays an image based on the provided geographic data. 

.. figure:: img/demos_requests_WMS_map.png
   
   OpenLayers WMS GetMap request
   
WMS GetFeatureInfo requests retrieve information regarding a particular feature on the map image.

.. figure:: img/demos_requests_WMS_feature.png
   
   WMS GetFeatureInfo request

.. _srs_list:

SRS List
--------

GeoServer natively supports almost 4,000 Spatial Referencing Systems (SRS), also known as **projections**, and more can be added. A spatial reference system defines an ellipsoid, a datum using that ellipsoid, and either a geocentric, geographic or projection coordinate system. This page lists all SRS info known to GeoServer.

.. figure:: img/demos_SRS.png
   
   Listing of all Spatial Referencing Systems (SRS) known to GeoServer
   
The :guilabel:`Code` column refers to the unique integer identifier defined by the author of that spatial reference system. Each code is linked to a more detailed description page, accessed by clicking on that code.

.. figure:: img/demos_SRS_page.png
   
   Details for SRS EPSG:2000

The title of each SRS is composed of the author name and the unique integer identifier (code) defined by the Author. In the above example, the author is the `European Petroleum Survey Group <http://www.epsg.org/>`_ (EPSG) and the Code is 2000. The fields are as follows:

:guilabel:`Description`—A short text description of the SRS

:guilabel:`WKT`—A string describing the SRS. WKT stands for "Well Known Text"

:guilabel:`Area of Validity`—The bounding box for the SRS


.. _demos_reprojectionconsole:

Reprojection console
--------------------

The reprojection console allows you to calculate and test coordinate transformation. You can input a single coordinate or WKT geometry, and transform it from one CRS to another.

For example, you can use the reprojection console to transform a bounding box (as a WKT polygon or line) between different CRSs.

.. figure:: img/demos_reprojectionconsole.png

   Reprojection console showing a transformed bounding box

Use :guilabel:`Forward transformation` to convert from source CRS to target CRS, and :guilabel:`Backward transformation` to convert from target CRS to source CRS.

You can also view the underlying calculation GeoServer is using to perform the transformation.

.. figure:: img/demos_reprojectionconsoledetails.png
   
   Reprojection console showing operation details

Read more about :ref:`crs_handling`. 


.. _demos_wcsrequestbuilder:

WCS Request Builder
-------------------

The WCS Request Builder is a tool for generating and executing WCS requests. Since WCS requests can be cumbersome to author, this tool can make working with WCS much easier.

Read more about the :ref:`wcs_request_builder`.


.. _demos_wpsrequestbuilder:

WPS Request Builder
-------------------

GeoServer with the :ref:`WPS extension installed <wps_install>` includes a request builder for generating and executing WPS processes. Since WPS requests can be cumbersome to author, this tool can make working with WPS much easier.

Read more about the :ref:`wps_request_builder`.
