.. _sec_auth:

Authentication
==============

There are three sets of GeoServer resources involved in authentication:

* The :ref:`web_admin` (also known as web admin) 
* :ref:`OWS <services>` services (such as WFS and WMS)
* :ref:`REST <rest_overview>` services

The following sections describe how each set of GeoServer resources administers authentication. To configure the authentication settings and providers, please see the section on :ref:`webadmin_sec_auth` in the :ref:`web_admin`.


.. toctree::
   :maxdepth: 2

   chain
   web
   owsrest
   providers