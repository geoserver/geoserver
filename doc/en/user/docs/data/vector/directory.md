# Directory of spatial files {: #data_shapefiles_directory }

The directory store automates the process of loading multiple shapefiles into GeoServer. Loading a directory that contains multiple shapefiles will automatically add each shapefile to GeoServer.

!!! note

    While GeoServer has robust support for the shapefile format, it is not the recommended format of choice in a production environment. Databases such as PostGIS are more suitable in production and offer better performance and scalability. See the section on [Running in a production environment](../../production/index.md) for more information.

## Adding a directory

To begin, navigate to **Stores --> Add a new store --> Directory of spatial files**.

![](images/directory.png)
*Adding a directory of spatial files as a store*

  ---------------------- -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  **Option**             **Description**

  **Workspace**          Name of the workspace to contain the store. This will also be the prefix of all of the layer names created from shapefiles in the store.

  **Data Source Name**   Name of the store as known to GeoServer.

  **Description**        Description of the directory store.

  **Enabled**            Enables the store. If disabled, no data in any of the shapefiles will be served.

  **URL**                Location of the directory. Can be an absolute path (such as **`file:C:\Data\shapefile_directory`**) or a path relative to the data directory (such as **`file:data/shapefile_directory`**.

  **namespace**          Namespace to be associated with the store. This field is altered by changing the workspace name.

  **skip scan**          Skip scan of alternative shapefile extensions (i.e. .SHP, .shp.XML, .CPG, \...) on Not-Windows systems. This can be useful when you have a directory containing several thousands of shapefiles. By Default, the shapefile plugin will look for all the shapefile extensions (.shp, .dbf, .shx, .prj, .qix, .fix, .shp.xml, .cpg). As soon as one of these is missing, it will do a search on the directory for the missing file, ignoring the case. This might be time consuming on directories with a huge number of files.
  ---------------------- -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

When finished, click **Save**.

## Configuring shapefiles

All of the shapefiles contained in the directory store will be loaded as part of the directory store, but they will need to be individually configured as new layers they can be served by GeoServer. See the section on [Layers](../webadmin/layers.md) for how to add and edit new layers.
