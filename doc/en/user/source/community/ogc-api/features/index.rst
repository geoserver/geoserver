.. _ogcapi-features:

OGC API Features
================

An `OGC Features API <https://github.com/opengeospatial/ogcapi-features>`_ publishing feature data using an OpenAPI web service.

.. list-table::
   :widths: 30, 20, 50
   :header-rows: 1

   * - `OGC API - Features <https://github.com/opengeospatial/ogcapi-features>`__
     - Version
     - Implementation status
   * - Part 1: Core
     - `Editor's draft <http://docs.ogc.org/DRAFTS/17-069r4.html>`__
     - Additional errata and clarifications not yet reviewed.
   * - 
     - `1.0.0 <http://docs.ogc.org/is/17-069r3/17-069r3.html>`__
     - Up to date with 1.0.0 release version.
   * - Part 2: CRS by Reference
     - `1.0.0 <http://docs.ogc.org/DRAFTS/19-079r1.html>`__
     - Not yet implemented
   * - 
     - Draft
     - Draft implemented, update to final release required.
   * - Part 3: Filtering and CQL
     - `Draft <http://docs.ogc.org/DRAFTS/19-079r1.html>`__
     - Draft implemented

Service
-------

The OGC API Features Service is accessed via the :guilabel:`FEATURES` version :guilabel:`1.0` link on the home page.

Capabilities
''''''''''''

The service is self described using:

* ``html``: A collection of web pages, with links for navigation between content (and that can be indexed by search engines for discoverability).

  .. figure:: img/features.png
 
     OGC API Features service

* `application/json`: A collection of :file:`json` documents, with reference between each document for programmatic access by web developers.

  .. code-block:: json
  
     {
       "title": "GeoServer Web Feature Service",
       "description": "This is the reference implementation of WFS 1.0.0 and WFS 1.1.0, supports all WFS operations including Transaction.",
       "links": [
         {
           "href": "http://localhost:8080/geoserver/ogc/features/?f=application%2Fx-yaml",
           "rel": "alternate",
           "type": "application/x-yaml",
           "title": "This document as application/x-yaml"
         },
         {
           "href": "http://localhost:8080/geoserver/ogc/features/?f=application%2Fjson",
           "rel": "self",
           "type": "application/json",
           "title": "This document"
         },
         {
           "href": "http://localhost:8080/geoserver/ogc/features/?f=text%2Fhtml",
           "rel": "alternate",
           "type": "text/html",
           "title": "This document as text/html"
         }

* ``application/x-yaml``: A collection of :file:`yaml` documents, with references between each document for programmatic access.
 
  .. code-block:: yaml
  
     title: GeoServer Web Feature Service
     description: This is the reference implementation of WFS 1.0.0 and WFS 1.1.0, supports
       all WFS operations including Transaction.
     links:
     - href: http://localhost:8080/geoserver/ogc/features/?f=application%2Fx-yaml
       rel: self
       type: application/x-yaml
       title: This document
     - href: http://localhost:8080/geoserver/ogc/features/?f=application%2Fjson
       rel: alternate
       type: application/json
       title: This document as application/json
     - href: http://localhost:8080/geoserver/ogc/features/?f=text%2Fhtml
       rel: alternate
       type: text/html
       title: This document as text/html

The service title and description are provided by the existing :ref:`wfs` settings.

Open API
''''''''

For programmatic access an `OpenAPI <https://www.openapis.org/>`__ description of the service is provided, that may be browsed as documentation, or used to generate a client to access the web services.

.. figure:: img/features-api.png
   
   OGC API Features OpenAPI Document

Collections
'''''''''''

The collection of feature types being published by the service.

Each collection entry is described using the layer details of title, description, geographic extent.

Data can be browsed as web pages, or downloaded in a range of formats such as :file:`GeoJSON` and :file:`GML` documents.

.. figure:: img/collection.png
   
   Collection sf:roads download formats

Tile matrix sets (extension)
''''''''''''''''''''''''''''

Addition from :ref:`ogcapi-tiles` extension listing tile matrix sets, linking to their definition.

.. figure:: img/tilematrix.png
   
   Tile matrix EPSG:4326 definition

Conformance
'''''''''''

Lists the operations this service can perform, each "conformance class" documents supported functionality. 

.. figure:: img/conformance.png

   OGC API Features Conformance

Contact information
'''''''''''''''''''

Advertises contact information for the service.

Defined by defined in by :ref:`config_contact`.

Service Configuration
---------------------

The service does not require any additional configuration to use. The service is configured using:

* The existing :ref:`wfs` settings to define title, abstract, and output formats.
  
  This is why the service page is titled :kbd:`GeoServer Web Feature Service`` by default.
  
* Built-in templates used for html generation


HTML Templates
''''''''''''''

To override an OGC API Features template:

#. Create a directory :file:`ogc/features` in the location you wish to override:
   
   * :file:`GEOSERVER_DATA_DIR/templates/ogc/features`
   * :file:`GEOSERVER_DATA_DIR/workspace/{workspace}/ogc/features`
   * :file:`GEOSERVER_DATA_DIR/workspace/{workspace}/{datastore}/ogc/features` 
   * :file:`GEOSERVER_DATA_DIR/workspace/{workspace}/{datastore}/{featuretype}/ogc/features` 

#. Create a file in this location, using the GeoServer |release| examples below:

   * :download:`ogc/features/collection.ftl  </../../../../src/community/ogcapi/ogcapi-features/src/main/resources/org/geoserver/ogcapi/v1/features/collection.ftl>`
   * :download:`ogc/features/collection_include.ftl  </../../../../src/community/ogcapi/ogcapi-features/src/main/resources/org/geoserver/ogcapi/v1/features/collection_include.ftl>`
   * :download:`ogc/features/collections.ftl  </../../../../src/community/ogcapi/ogcapi-features/src/main/resources/org/geoserver/ogcapi/v1/features/collections.ftl>`
   * :download:`ogc/features/queryables.ftl  </../../../../src/community/ogcapi/ogcapi-core/src/main/resources/org/geoserver/ogcapi/queryables.ftl>`
   * :download:`ogc/features/functions.ftl  </../../../../src/community/ogcapi/ogcapi-features/src/main/resources/org/geoserver/ogcapi/v1/features/functions.ftl>`
   
   The above built-in examples are for GeoServer |release|, please check for any changes when upgrading GeoServer.

The templates for listing feature content are shared between OGC API services. To override a template used to list features:

#. Use the directory in the location you wish to override:

   * :file:`GEOSERVER_DATA_DIR/templates`
   * :file:`GEOSERVER_DATA_DIR/workspace/{workspace}`
   * :file:`GEOSERVER_DATA_DIR/workspace/{workspace}/{datastore}` 
   * :file:`GEOSERVER_DATA_DIR/workspace/{workspace}/{datastore}/{featuretype}` 
   * :download:`ogc/features/landingPage.ftl  </../../../../src/community/ogcapi/ogcapi-features/src/main/resources/org/geoserver/ogcapi/v1/features/landingPage.ftl>`

#. Create a file in this location, using the GeoServer |release| examples below:

   * :download:`ogc/features/getfeature-complex-content.ftl  </../../../../src/community/ogcapi/ogcapi-features/src/main/resources/org/geoserver/ogcapi/v1/features/getfeature-complex-content.ftl>`
   * :download:`ogc/features/getfeature-content.ftl  </../../../../src/community/ogcapi/ogcapi-features/src/main/resources/org/geoserver/ogcapi/v1/features/getfeature-content.ftl>`
   * :download:`ogc/features/getfeature-empty.ftl  </../../../../src/community/ogcapi/ogcapi-features/src/main/resources/org/geoserver/ogcapi/v1/features/getfeature-empty.ftl>`
   * :download:`ogc/features/getfeature-footer.ftl  </../../../../src/community/ogcapi/ogcapi-features/src/main/resources/org/geoserver/ogcapi/v1/features/getfeature-footer.ftl>`
   * :download:`ogc/features/getfeature-header.ftl  </../../../../src/community/ogcapi/ogcapi-features/src/main/resources/org/geoserver/ogcapi/v1/features/getfeature-header.ftl>`

   The above built-in examples are for GeoServer |release|, please check for any changes when upgrading GeoServer.

As an example customize how collections are listed:

#. The file :file:`ogc/features/collections.ftl` lists published collection:

   .. literalinclude:: /../../../../src/community/ogcapi/ogcapi-features/src/main/resources/org/geoserver/ogcapi/v1/features/collections.ftl

#. Save file to :file:`GEOSERVER_DATA_DIR/workspace/templates/ogc/collections.ftl`, and rewrite as:
   
   .. code-block::
   
      <#include "common-header.ftl">
             <h2>OGC API Feature Collections</h2>
             <p>List of collections published.</p>
             <p>See also: <#list model.getLinksExcept(null, "text/html") as link>
                <a href="${link.href}">${link.type}</a><#if link_has_next>, </#if></#list>.</p>
     
           <#list model.collections as collection>
             <h2><a href="${serviceLink("collections/${collection.id}")}">${collection.id}</a></h2>
             <#include "collection_include.ftl">
           </#list>
      <#include "common-footer.ftl">

#. Many templates are constructed using ``#include``, for example :file:`collection.ftl` above uses ``<#include "common-header.ftl">`` located next to :file:`collections.ftl`.

   Presently each family of templates manages its own :file:`common-header.ftl` (as shown in the difference between :file:`ogc/features` service templates, and getfeature templates above).

#. A restart is required, as templates are cached.
   
   .. figure:: img/template_override.png
      
      Template collections.ftl override applied
      
#. Language codes are appended for internationalization. For French create the file :file:`GEOSERVER_DATA_DIR/workspace/{workspace}/ogc/collections_fr.ftl` and translate contents:

   .. code-block::
   
      <#include "common-header.ftl">
             <h2>OGC API Feature Service</h2>
             <p>Liste des collections publiées.</p>
             <p>Voir également: <#list model.getLinksExcept(null, "text/html") as link>
                <a href="${link.href}">${link.type}</a><#if link_has_next>, </#if></#list>.</p>
     
           <#list model.collections as collection>
             <h2><a href="${serviceLink("collections/${collection.id}")}">${collection.id}</a></h2>
             <#include "collection_include.ftl">
           </#list>
      <#include "common-footer.ftl">
      
#. For details on how to write templates see :ref:`tutorial_freemarkertemplate` tutorial.
