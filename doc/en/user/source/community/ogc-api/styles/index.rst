OGC API - Styles
================

A `OGC Styles API <https://github.com/opengeospatial/ogcapi-styles>`_ based on the early draft of this specification.

This service describes, retrieves and updates the styles present on the server. 
Styles are cross linked with their associated collections in both the Features and Tiles API.

OGC API - Styles Implementation status
--------------------------------------

.. list-table::
   :widths: 30, 20, 50
   :header-rows: 1

   * - `OGC API - Styles <https://github.com/opengeospatial/ogcapi-styles>`__
     - Version
     - Implementation status
   * - Part 1: Core
     - `Draft <http://docs.opengeospatial.org/DRAFTS/20-009.html>`__
     - Implementation based on early specification draft.
     
Installing the GeoServer OGC API - Styles module
------------------------------------------------

#. Download the OGC API nightly GeoServer community module from :download_community:`ogcapi-styles`.
   
   .. warning:: Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-|release|-ogcapi-styles-features-plugin.zip above).

#. Extract the contents of the archive into the ``WEB-INF/lib`` directory of the GeoServer installation.

#. On restart the services are listed at http://localhost:8080/geoserver

Configuration of OGC API - Styles module
----------------------------------------

The module exposes all available GeoServer styles.

Conformance classes
'''''''''''''''''''

The service always supports reading styles, as Mapbox styles, SLD 1.0 and SLD 1.1. These classes are always enabled and are listed in the conformance declaration.

The **Manage styles** conformance class allows clients to create, update and delete styles, and to update style metadata. It is disabled by default. Set the ``OGCAPI_STYLES_MANAGE`` variable to ``true`` to enable it, as a Java system property, an environment variable or a servlet context parameter.

Managing styles changes the GeoServer catalog, so decide who may do it before enabling the class. Service security rules control this, in **Security > Services** or in ``security/services.properties``. A good starting point allows only administrators::

   styles.addStyle=ROLE_ADMINISTRATOR
   styles.updateStyle=ROLE_ADMINISTRATOR
   styles.deleteStyle=ROLE_ADMINISTRATOR
   styles.updateStyleMetadata=ROLE_ADMINISTRATOR
   styles.patchStyleMetadata=ROLE_ADMINISTRATOR

Add other roles to these rules to allow more users to manage styles. Use a rule for each operation, as above: a ``styles.*`` rule also applies to the operations that read styles.
