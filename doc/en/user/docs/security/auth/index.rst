.. _security_auth:

Authentication
==============

There are three sets of GeoServer resources involved in authentication:

* The :ref:`web_admin` (also known as web admin) 
* :ref:`OWS <services>` services (such as WFS and WMS)
* :ref:`REST <rest>` services

The following sections describe how each set of GeoServer resources administers authentication. To configure the authentication settings and providers, please see the section on :ref:`security_webadmin_auth` in the :ref:`web_admin`.


.. toctree::
   :maxdepth: 2

   chain
   web
   owsrest
   providers