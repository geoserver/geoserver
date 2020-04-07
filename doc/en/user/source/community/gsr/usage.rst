GSR Usage
=====================

Currently basic FeatureServer and MapServer functionality work. Each GeoServer workspace is considered
an ArcGIS® "service" for the purposes of the API. ArcGIS® URLs look like this in GeoServer:

http://localhost:8080/geoserver/gsr/services/topp/MapServer/
http://localhost:8080/geoserver/gsr/services/topp/FeatureServer/

Where topp is the workspace name.

.. note::

     Esri®, ArcGIS® and ArcGIS Online®  are trademarks, registered trademarks, or service marks of Esri in the United States, the European Community, or certain other jurisdictions. Other companies and products mentioned may be trademarks of their respective owners.


CORS
---------------------------

When using the official JS API, CORS detection does not work correctly. You will need to add
your server manually to the list of CORS enabled servers:

.. code-block:: javascript

  require(["esri/config"], function (esriConfig) {
    esriConfig.request.corsEnabledServers.push("localhost:9191");
  });

CORS support also needs to be enabled on the server. Without these settings, it
will attempt to use JSONP, which may not be as well supported.

For information on enabling CORS in GeoServer, see `here <http://suite.opengeo.org/docs/latest/sysadmin/cors/index.html>`_.

Web Mercator Spatial Reference
------------------------------

The official APIs use Spatial Reference 102100 as Web Mercator. In order for this to work,
add the following custom projection to your `data_dir/user_projections/epsg.properties` file:

``102100=PROJCS["WGS 84 / Pseudo-Mercator",GEOGCS["Popular Visualisation CRS",
DATUM["Popular_Visualisation_Datum",SPHEROID["Popular Visualisation Sphere",6378137,0,
AUTHORITY["EPSG","7059"]],TOWGS84[0,0,0,0,0,0,0],AUTHORITY["EPSG","6055"]],
PRIMEM["Greenwich",0,AUTHORITY["EPSG","8901"]],UNIT["degree",0.01745329251994328,
AUTHORITY["EPSG","9122"]],AUTHORITY["EPSG","4055"]],UNIT["metre",1,AUTHORITY["EPSG","9001"]],
PROJECTION["Mercator_1SP"],PARAMETER["central_meridian",0],PARAMETER["scale_factor",1],
PARAMETER["false_easting",0],PARAMETER["false_northing",0],AUTHORITY["EPSG","3785"],
AXIS["X",EAST],AXIS["Y",NORTH]]``
