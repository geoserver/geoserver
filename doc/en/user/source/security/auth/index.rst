.. _sec_auth:

Authentication
==============

There are three sets of GeoServer resources applicable to authentication:

* The :ref:`web_admin` (aka web admin) 
* :ref:`OWS <services>` services (such as WFS and WMS)
* :ref:`REST <rest_overview>` services

Each set of resources administers authentication in a different way, as described in the following sections.  To configure the authentication settings and providers, please see the section on :ref:`webadmin_sec_auth` in the :ref:`web_admin`.

.. toctree::
   :maxdepth: 2

   chain
   web
   owsrest
   providers