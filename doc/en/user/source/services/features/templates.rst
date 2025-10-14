HTML Templates
--------------

Built-in templates are used for html generation.

Template override
'''''''''''''''''

To override an OGC API Features template:

#. Create a directory :file:`ogc/features` in the location you wish to override:
   
   * :file:`GEOSERVER_DATA_DIR/templates/ogc/features/v1`
   * :file:`GEOSERVER_DATA_DIR/workspace/{workspace}/ogc/features/v1`
   * :file:`GEOSERVER_DATA_DIR/workspace/{workspace}/{datastore}/ogc/features/v1` 
   * :file:`GEOSERVER_DATA_DIR/workspace/{workspace}/{datastore}/{featuretype}/ogc/features/v1` 

#. Create a file in this location, using the GeoServer |release| examples below:

   * :download:`ogc/features/v1/landingPage.ftl  </../../../../src/extension/ogcapi/ogcapi-features/src/main/resources/org/geoserver/ogcapi/v1/features/landingPage.ftl>`
   * :download:`ogc/features/v1/collection.ftl  </../../../../src/extension/ogcapi/ogcapi-features/src/main/resources/org/geoserver/ogcapi/v1/features/collection.ftl>`
   * :download:`ogc/features/v1/collection_include.ftl  </../../../../src/extension/ogcapi/ogcapi-features/src/main/resources/org/geoserver/ogcapi/v1/features/collection_include.ftl>`
   * :download:`ogc/features/v1/collections.ftl  </../../../../src/extension/ogcapi/ogcapi-features/src/main/resources/org/geoserver/ogcapi/v1/features/collections.ftl>`
   * :download:`ogc/features/v1/queryables.ftl  </../../../../src/extension/ogcapi/ogcapi-core/src/main/resources/org/geoserver/ogcapi/queryables.ftl>`
   * :download:`ogc/features/v1/functions.ftl  </../../../../src/extension/ogcapi/ogcapi-features/src/main/resources/org/geoserver/ogcapi/v1/features/functions.ftl>`
   
   The above built-in examples are for GeoServer |release|, please check for any changes when upgrading GeoServer.

Writing templates
'''''''''''''''''

For details on how to write templates see :ref:`tutorial_freemarkertemplate` tutorial.

The following functions are specific to OGC API templates:

* ``serviceLink(path*, format)`` generates a link back to the same service. 
  The first argument, mandatory, is the extra path after the service landing page, the second argument, optional, is the format to use for the link.
* ``genericServiceLink(path*, k1, v1, k2, v2, ....)`` generates a link back to any GeoServer OGC service, with additional query parameters. 
  The first argument, mandatory, is the extra path after the GeoServer context path (usually ``/geoserver``), 
  the following arguments are key-value pairs to be added as query parameters to the link.
* ``resourceLink(path)`` links to a static resource, such as a CSS file or an image. 
  The argument is the path to the resource, relative to the GeoServer context path (usually ``/geoserver``).

List features
'''''''''''''

To override a template used to list features:

#. Use the directory in the location you wish to override (can be general, specific to a workspace, datastore, or feature type):

   * :file:`GEOSERVER_DATA_DIR/templates`
   * :file:`GEOSERVER_DATA_DIR/workspace/{workspace}`
   * :file:`GEOSERVER_DATA_DIR/workspace/{workspace}/{datastore}` 
   * :file:`GEOSERVER_DATA_DIR/workspace/{workspace}/{datastore}/{featuretype}` 

#. Create a file in this location, using the GeoServer |release| examples below:

   * :download:`ogc/features/getfeature-complex-content.ftl  </../../../../src/extension/ogcapi/ogcapi-features/src/main/resources/org/geoserver/ogcapi/v1/features/getfeature-complex-content.ftl>`
   * :download:`ogc/features/getfeature-content.ftl  </../../../../src/extension/ogcapi/ogcapi-features/src/main/resources/org/geoserver/ogcapi/v1/features/getfeature-content.ftl>`
   * :download:`ogc/features/getfeature-empty.ftl  </../../../../src/extension/ogcapi/ogcapi-features/src/main/resources/org/geoserver/ogcapi/v1/features/getfeature-empty.ftl>`
   * :download:`ogc/features/getfeature-footer.ftl  </../../../../src/extension/ogcapi/ogcapi-features/src/main/resources/org/geoserver/ogcapi/v1/features/getfeature-footer.ftl>`
   * :download:`ogc/features/getfeature-header.ftl  </../../../../src/extension/ogcapi/ogcapi-features/src/main/resources/org/geoserver/ogcapi/v1/features/getfeature-header.ftl>`

   The above built-in examples are for GeoServer |release|, please check for any changes when upgrading GeoServer.

Collection Example
''''''''''''''''''

Example showing how to customize a collections being listed:

#. The file :file:`ogc/features/collections.ftl` lists published collection:

   .. literalinclude:: /../../../../src/extension/ogcapi/ogcapi-features/src/main/resources/org/geoserver/ogcapi/v1/features/collections.ftl

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

#. A restart is not required, the system will notice when the template is updated and apply the changes automatically.
   
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

