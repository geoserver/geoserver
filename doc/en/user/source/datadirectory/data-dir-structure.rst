.. _data_dir_structure:

Structure of the Data Directory
===============================

This section gives an overview of the structure and contents of the GeoServer data directory. 
It is not intended to be a complete reference, 
and in general the data directory contents should not be accessed directly.
The :ref:`web_admin` can be used to view and modify the configuration manually. 
For programmatic access and manipulation 
the :ref:`rest_extension` API should be used.

The following figure shows the structure of the GeoServer data directory::

   <data_directory>/
   
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
      workspaces/
        |
        +- workspace dirs...
           |
           +- datastore dirs...
              |
              +- layer dirs...
      www/

The .xml files
--------------

The top-level ``.xml`` files contain information about the services and various global options for the server instance. 

.. list-table::
   :widths: 20 80

   * - **File**
     - **Description**
   * - ``global.xml``
     - Contains settings common to all services, such as contact information, JAI settings, character sets and verbosity.
   * - ``logging.xml``
     - Specifies logging parameters, such as level, location, and whether to log to stdout.  
   * - ``wcs.xml`` 
     - Contains the service metadata and various settings for the WCS service.
   * - ``wfs.xml`` 
     - Contains the service metadata and various settings for the WFS service.
   * - ``wms.xml`` 
     - Contains the service metadata and various settings for the WMS service.


workspaces
----------

The ``workspaces`` directory contain metadata about the layers published by GeoServer.
It contains a directory for each defined **workspace**.
Each workspace directory contains directories for the **datastores** defined in it.
Each datastore directory contains directories for the **layers** defined for the datastore.
Each layer directory contains a ``layer.xml`` file, and 
either a ``coverage.xml`` or a ``featuretype.xml`` file 
depending on whether the layer represents a *raster* or *vector* dataset.

data
----

The ``data`` directory can be used to store file-based geospatial datasets being served as layers.
(This should not be confused this with the main "GeoServer data directory".)
This directory is commonly used to store shapefiles and raster files, 
but can be used for any data that is file-based.

The main benefit of storing data files under the ``data`` directory is portability. 
Consider a shapefile located external to the data directory at a location ``C:\gis_data\foo.shp``. 
The ``datastore`` entry in ``catalog.xml`` for this shapefile would look like the following::

   <datastore id="foo_shapefile">
      <connectionParams>
        <parameter name="url" value="file://C:/gis_data/foo.shp" />
      </connectionParams>
    </datastore>

Now consider trying to port this data directory to another host running GeoServer. 
The location ``C:\gis_data\foo.shp`` probably does not exist on the second host. 
So either the file must be copied to this location on the new host, or ``catalog.xml`` must be changed to reflect a new location.

This problem can be avoided by storing ``foo.shp`` in the ``data`` directory. 
In this case the ``datastore`` entry in ``catalog.xml`` becomes::

    <datastore id="foo_shapefile">
      <connectionParams>
        <parameter name="url" value="file:data/foo.shp"/>
      </connectionParams>
    </datastore>

The ``value`` attribute is rewritten to be relative to the ``data`` directory. 
Because of this location-independence the entire data directory can be archived, copied to a new host, 
un-archived, and used directly with no additional changes.

demo
----

The ``demo`` directory contains files which define the *sample requests* available in the *Sample Request Tool* (http://localhost:8080/geoserver/demoRequest.do). 
See the :ref:`webadmin_demos` page for more information.

geosearch
---------

The ``geosearch`` directory contains information for regionation of KML files.

gwc
---

The ``gwc`` directory holds the cache created by the embedded GeoWebCache service.

layergroups
-----------

The ``layergroups`` directory contains configuration information for the defined layergroups.

palettes
--------

The ``palettes`` directory is used to store pre-computed *Image Palettes*. 
Image palettes are used by the GeoServer WMS as way to reduce the size of produced images while maintaining image quality.

security
--------

The ``security`` directory contains the files used to configure the GeoServer security subsystem. This includes a set of property files which define *access roles*, along with the services and data each role is authorized to access. See the :ref:`security` section for more information.

styles
------

The ``styles`` directory contains Styled Layer Descriptor (SLD) files which contain styling information used by the GeoServer WMS. For each file in this directory there is a corresponding entry in ``catalog.xml``::

   <style id="point_style" file="default_point.sld"/>

See the :ref:`styling` section for more information about styling and SLD .

templates
---------

The ``templates`` directory contains files used by the GeoServer **templating** subsystem. Templates are used to customize the output of various GeoServer operations.

user_projections
----------------

The ``user_projections`` directory contains a file called ``epsg.properties`` which is used to define custom spatial reference systems that are not part of the official `EPSG database <http://www.epsg.org/CurrentDB.html>`_.

www
---

The ``www`` directory is used to allow GeoServer to serve files like a regular web server. 
While not a replacement for a full blown web server, the ``www`` directory can be useful for serving `OpenLayers <http://openlayers.org>`_ map applications.


