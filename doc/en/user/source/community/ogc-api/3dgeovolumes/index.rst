.. _ogcapi-3d-geovolumes:

OGC API - 3D GeoVolumes
=======================

An `OGC API - 3D GeoVolumes <https://ogcapi.ogc.org/geovolumes/>`_ service publishing 3D conents data using an OpenAPI web service.

3D GeoVolumes Implementation status
-----------------------------------

The 3D GeoVolumes implementation is based on August 2024 draft of the specification,
implementing the core conformance class only (the ``bbox`` search class is not implemented).

Installing the GeoServer OGC API 3D GeoVolumes module
-----------------------------------------------------

#. Download the OGC API nightly GeoServer community module from ::nightly_community:`ogcapi-3d-geovolumes`.
   
   .. warning:: Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-|release|-ogcapi-3dgeovolumes-plugin.zip above).

#. Extract the contents of the archive into the ``WEB-INF/lib`` directory of the GeoServer installation.

#. On restart the services are listed at http://localhost:8080/geoserver

Configuring the 3D GeoVolumes service
-------------------------------------

The service is based on a set of static configuration files that can be found either in:

* The ``geovolumes`` directory found inside a GeoServer data directory
* An external directory, specified by the ``GEOVOLUMES_LOCATION`` property (either enviromnet variable, system property, or servlet context property in ``web.xml``). 

The directory needs to contain a single file, a ``collections.json`` that represents a simplified
version of what GeoServer will return on the collections resource.

Here is an example with a single collection defined:

.. code-block:: json
    
    {
        "collections": [
          {
            "id": "NewYork",
            "title": "NewYork",
            "description": "All Supported 3D Containers for the city of NewYork. Please zoom on Manhattan when using the Cesium viewer, only have zoom level 14.",
            "collectiontype": "3d-container",
            "extent": {
              "spatial": {
                "bbox": [
                  [
                    -74.01900887327089,
                    40.700475291581974,
                    -11.892070104139751,
                    -73.9068954348699,
                    40.880256294183646,
                    547.7591871983744
                  ]
                ],
                "crs": "http://www.opengis.net/def/crs/OGC/0/CRS84h"
              }
            },
            "content": [
              {
                "title": "NewYork - 3D Buildings Manhattan: 3D Tiles",
                "rel": "original",
                "href": "NewYork/3dtiles/tileset.json",
                "type": "application/json+3dtiles"
              },
              {
                "title": "NewYork - 3D Buildings Manhattan: i3s",
                "rel": "original",
                "href": "https://tiles.arcgis.com/tiles/P3ePLMYs2RVChkJx/arcgis/rest/services/Buildings_NewYork_17/SceneServer/layers/0/",
                "type": "application/json+i3s"
              }
            ]
          }
        ]
    }
    
Highlights:

* This configuration file can be updated while GeoServer is running, and it will pick up the new configuration automatically (make sure it's valid JSON when you save it).

* The mandatory elements for the HTML templates to work are  ``id``, ``title``, ``description``, a 3D ``bbox`` and at least one ``content`` entry.

* There is no need to create a "links" array, GeoServer will automatically fill links with
  the correct values based on the current request. It's however possible to add a "links" array, if 
  custom links are desired.

* Any href found in ``links``, ``contents`` and eventual ``children`` arrays will be kept as is,
  if it happens to be an absolute reference to some web resource. If instead a relative path inside
  the ``geovolumes`` directory is found, it will be resolved and made available as a web resource in
  the service, replacing it with a link that can be followed in all collections representations.0
  
* The relative references must be inside the ``geovolumes`` directory, usage of ".." is disallowed
  to prevent path traversal attacks from admins, on hosted GeoServer deploys.

* The typical contents of the ``geovolumes`` subdirectories will be either Cesium 3D tiles, or i3s tiles.

The contents of the ``collections.json`` file will be exposed both in ``collections`` and single 
``collection`` resources. If Cesium 3D tiles are found, a Cesium viewer link will be added to the
collection HTML representation, if i3s tiles are found, a i3s viewer link will be added too.

Here is an example of collection HTML rendering and its associated Cesium view:

  .. figure:: img/collection.png

  .. figure:: img/newYork.png
      
.. note:: The Cesium viewer needs a Cesium access token to use the terrain layer. By default it's missing,
    which may result rendering "flying buildings" in mountain areas. See templates customization below on how to add your token.

Quick start
'''''''''''

A quick start example containing a ``geovolumes`` directory can be found `here <https://www.dropbox.com/scl/fi/19z00lvfglqoqlylxew5t/geovolumes.zip?rlkey=fe4itc6y6xzjezyvs0veitjju&st=r24tzpjx&dl=1>`_. Just unpack it into the GeoServer data directory, add the 3D GeoVolumes service, and
play with the New York and Stuttgart 3D models.

HTML Templates
''''''''''''''

To override an OGC API 3D Feature template:

#. Create a directory for template overrides at :file:`GEOSERVER_DATA_DIR/templates/ogc/3dgeovolumes/v1`

#. Create a file in this location, using the GeoServer |release| examples below:

   * :download:`ogc/3dgeovolumes/v1/landingpage.ftl  </../../../../src/community/ogcapi/ogcapi-3d-geovolumes/src/main/resources/org/geoserver/ogcapi/v1/geovolumes/landingPage.ftl>`
   * :download:`ogc/3dgeovolumes/v1/collections.ftl  </../../../../src/community/ogcapi/ogcapi-3d-geovolumes/src/main/resources/org/geoserver/ogcapi/v1/geovolumes/collections.ftl>`
   * :download:`ogc/3dgeovolumes/v1/collection.ftl  </../../../../src/community/ogcapi/ogcapi-3d-geovolumes/src/main/resources/org/geoserver/ogcapi/v1/geovolumes/collection.ftl>`
   * :download:`ogc/3dgeovolumes/v1/i3sclient.ftl  </../../../../src/community/ogcapi/ogcapi-3d-geovolumes/src/main/resources/org/geoserver/ogcapi/v1/geovolumes/i3sclient.ftl>`
   * :download:`ogc/3dgeovolumes/v1/cesium.ftl  </../../../../src/community/ogcapi/ogcapi-3d-geovolumes/src/main/resources/org/geoserver/ogcapi/v1/geovolumes/cesium.ftl>`
   
In particular, overriding the Cesium template allows to specify a custom access token, which will then enable the 3D terrain support. For example:

.. code-block:: html

   <html lang="en">
    <head>
        <meta charset="utf-8">
        <script src="https://cesium.com/downloads/cesiumjs/releases/1.83/Build/Cesium/Cesium.js"></script>
        <link href="https://cesium.com/downloads/cesiumjs/releases/1.83/Build/Cesium/Widgets/widgets.css" rel="stylesheet">
        <script src="${baseURL}webresources/ogcapi/3d/cesium.js" type="text/javascript"></script>
    </head>

    <body>
        <div id="cesiumContainer" style="width: 100%; height:100%; margin:0;"></div>
        <input type="hidden" id="cesiumAccessToken" value="yourCustomAccessTokenHere"/>
    </body>
  </html>

Making Cesium and i3s demos work with GeoServer CSP
'''''''''''''''''''''''''''''''''''''''''''''''''''

GeoServer ships with a Content Security Policy (CSP) that will prevent Cesium and i3s viewers
from working out of the box. To enable them, you need to either:

* Disable CSP fully (recommended only in demo environments):

  1. Go to GeoServer web admin page
  2. Select "Security" -> "Content Security Policy"
  3. Disable "Enable Content Security Policy"
  4. Save

* Configure CSP to allow downloads from Cesium and i3s servers. This a bit complex, you will need to modify the GeoServer CSP configuration.

  1. Go to GeoServer web admin page
  2. Select "Security" -> "Content Security Policy"
  3. In the "Policy List" section, open the "geoserver-csp" policy

  .. figure:: img/geoserver-csp.png

  4. In the Rule List, click on "Add new" to configure a new policy for Cesium:

     * Name: ``3dgeovolumes-cesium```
     * Request Filter: ``PATH(^/ogc/3dgeovolumes/v1/cesium$)``
     * Header Directives:

        .. code-block::

             base-uri 'self';
             default-src 'none';
             frame-ancestors 'self';
             form-action 'self';

             script-src 'self' https://cesium.com https://*.cesium.com 'unsafe-inline' 'unsafe-eval';
             style-src  'self' https://cesium.com https://*.cesium.com 'unsafe-inline';
             img-src    'self' https://cesium.com https://*.cesium.com https://tile.openstreetmap.org https://*.tile.openstreetmap.org data:;
             font-src   'self' https://cesium.com https://*.cesium.com data:;
             connect-src 'self' https://cesium.com https://*.cesium.com https://tile.openstreetmap.org https://*.tile.openstreetmap.org;

             worker-src 'self' https://cesium.com https://*.cesium.com blob:;
             child-src  'self' https://cesium.com https://*.cesium.com blob:;

  5. Save the rule, and proceed to add a new one for i3s:

     * Name: ``3dgeovolumes-i3s```
     * Request Filter: PATH(^/ogc/3dgeovolumes/v1/i3s$)
     * Header Directives:

        .. code-block::

             base-uri 'self';
             default-src 'none';
             frame-ancestors 'self';
             form-action 'self';

             script-src 'self' https://*.arcgis.com https://services.arcgisonline.com 'unsafe-inline' 'unsafe-eval';
             style-src  'self' https://*.arcgis.com https://services.arcgisonline.com 'unsafe-inline';
             img-src    'self' https://*.arcgis.com https://services.arcgisonline.com data:;
             font-src   'self' https://*.arcgis.com https://services.arcgisonline.com data:;
             connect-src 'self' https://*.arcgis.com https://services.arcgisonline.com;

             worker-src 'self' https://*.arcgis.com https://services.arcgisonline.com blob:;
             child-src  'self' https://*.arcgis.com https://services.arcgisonline.com blob:;

    6. Save the i3s rule, and then use the arrows to place the Cesium and i3s rules above the "other requests" line, it should look as follows:

    .. figure:: img/geoserver-csp-order.png

    7. Finally click Save on the geoserver-csp policy page, and then again on the main Content Security Policy main page.
       The Cesium and i3s viewers should now work correctly.