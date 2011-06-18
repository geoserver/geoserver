.. _data_dir_structure:

Structure of the Data Directory
===============================

Introduction
------------

The structure of the data directory at this point is likely only of interest to core developers.  Previously users would often modify their data directory directly to programmatically make changes to their GeoServer configuration.  The new route to do this is with the :ref:`rest_extension` API, and is the only recommended option.

The following figure shows the structure of a GeoServer data directory::

   data_directory/
      global.xml
      logging.xml
      wms.xml
      wfs.xml
      wcs.xml
      data/
      demo/
      geosearch/
      gwc/
      layergroups/
      palettes/
      plugIns/
      security/
      styles/
      templates/
      user_projections/
      workspaces
      www/

The .xml files
--------------

The top level xml files save the information about the services and various global options. 

.. list-table::
   :widths: 20 80

   * - **File**
     - **Description**
   * - ``global.xml``
     - Contains settings that go across services, including contact information, JAI settings, character sets and verbosity.
   * - ``logging.xml``
     - Specifies the logging level, location, and whether it should log to std out.  
   * - ``wcs.xml`` 
     - Contains the service metadata and various settings for the WCS service.
   * - ``wfs.xml`` 
     - Contains the service metadata and various settings for the WFS service.
   * - ``wms.xml`` 
     - Contains the service metadata and various settings for the WMS service.


workspaces
----------

	The various workspaces directories contain metadata about "layers" which are published by GeoServer.  Each layer will have a layer.xml file associated with it, as well as either a coverage.xml or a featuretype.xml file depending on whether it's a *raster* or *vector* .

data
----
Not to the confused with the "GeoServer data directory" itself, the ``data`` directory is a location where actual data can be stored. This directory is commonly used to store shapefiles and raster files but can be used for any data that is file based.

The main benefit of storing data files inside of the ``data`` directory is portability. Consider a shapefile located external to the data directory at a location ``C:\gis_data\foo.shp``. The ``datastore`` entry in ``catalog.xml`` for this shapefile would like the following::

   <datastore id="foo_shapefile">
      <connectionParams>
        <parameter name="url" value="file://C:/gis_data/foo.shp" />
      </connectionParams>
    </datastore>

Now consider trying to port this data directory to another host running GeoServer. The problem exists in that the location ``C:\gis_data\foo.shp`` probably does not exist on the second host. So either the file must be copied to the new host, or ``catalog.xml`` must be changed to reflect a new location.

Such steps can be avoided by storing ``foo.shp`` inside of the ``data`` directory. In such a case the ``datastore`` entry in ``catalog.xml`` becomes::

    <datastore id="foo_shapefile">
      <connectionParams>
        <parameter name="url" value="file:data/foo.shp"/>
      </connectionParams>
    </datastore>

The ``value`` attribute is re-written to be relative. In this way the entire data directory can be archived, copied to the new host, un-archived, and used directly with no additional changes.

demo
----

The ``demo`` directory contains files which define the *sample requests* available in the *Sample Request Tool* (http://localhost:8080/geoserver/demoRequest.do). For more information see the :ref:`webadmin_demos` page for more information.

geosearch
---------
The geosearch directory is not named quite correctly.  It contains information for regionation of KML files.

gwc
---
This directory holds the cache created by the embedded GeoWebCache service.

layergroups
-----------
Contains information on the layer groups configurations.

palettes
--------

The ``palettes`` directory is used to store pre-computed *Image Palettes*. Image palettes are used by the GeoServer WMS as way to reduce the size of produced images while maintaining image quality.

security
--------
The ``security`` directory contains all the files used to configure the GeoServer security subsystem. This includes a set of property files which define *access roles*, along with the services and data each role is authorized to access. See the :ref:`security` section for more information.

styles
------

The ``styles`` directory contains a number of Styled Layer Descriptor (SLD) files which contain styling information used by the GeoServer WMS. For each file in this directory there is a corresponding entry in ``catalog.xml``::

   <style id="point_style" file="default_point.sld"/>

See the :ref:`styling` for more information about styling and SLD .

templates
---------

The ``template`` directory contains files used by the GeoServer *templating subsystem*. Templates are used to customize the output of various GeoServer operations.

user_projections
----------------

The ``user_projections`` directory contains a single file called ``epsg.properties`` which is used to define *custom* spatial reference systems which are not part of the official `EPSG database <http://www.epsg.org/CurrentDB.html>`_.

www
---

The ``www`` directory is used to allow GeoServer to act like a regular web server and serve regular files. While not a replacement for a full blown web server the ``www`` directory can be useful for serving `OpenLayers <http://openlayers.org>`_ map applications.


