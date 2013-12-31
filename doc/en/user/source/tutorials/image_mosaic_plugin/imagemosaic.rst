.. _tutorial_imagemosaic_extension:

Using the ImageMosaic plugin
============================


Introduction
------------

This tutorial describes the process of creating a new coverage using the new ImageMosaic plugin. The ImageMosaic plugin is provided by `GeoTools <http://geotools.org/>`_, and allows the creation of a mosaic from a number of georeferenced rasters. The plugin can be used with Geotiffs, as well as rasters accompanied by a world file (.pgw for png files, .jgw for jpg files, etc.). In addition, if imageio-ext GDAL extensions are properly installed we can also serve all the formats supported by it like MrSID, ECW, JPEG2000, etc... See :ref:`data_gdal` for more information on how to install them.

The JAI documentation gives a good description about what a Mosaic does:

`The "Mosaic" operation creates a mosaic of two or more source images. This operation could be used for example to assemble a set of overlapping geospatially rectified images into a contiguous image. It could also be used to create a montage of photographs such as a panorama`.

Briefly the ImageMosaic plugin is responsible for composing together a set of similar raster data, which, from now on I will call *granules*. The plugin has, of course, some limitations:

  1. All the granules must share the same Coordinate Reference System, no reprojection is performed.  This will always be a constraint.
  2. All the granules must share the same ColorModel and SampleModel. This is a limitation/assumption of the underlying JAI Mosaic operator: it basically means that the granules must share the same pixel layout and photometric interpretation. It would be quite difficult to overcome this limitation, but to some extent it could be done. Notice that, in case of colormapped granules, if the various granules share the same colormap the code will do its best to retain it and try not to expand them in memory. This can also be controlled via a  parameter in the configuration file (se next sections)
  3. All the granules must share the same spatial resolution and set of overviews (if this is not true, overviews will not be used). 
  
Granule Index
-------------

In order to configure a new CoverageStore and a new Coverage with this plugin, an index file needs to be generated first in order to associate each granule to its bounding box. Currently we support only a Shapefile as a proper index, although it would be possible to extend this and use other means to persist the index.

More specifically, the following files make up the mosaic configuration:

   1. A shapefile that contains enclosing polygons for each raster file.  This shapefile needs to have a field whose values are the paths for the mosaic granules. The path can be either relative to the shapefile itself or absolute, moreover, while the default name for the shapefile attribute that contains the granules' paths is "location", such a name can be configured to be different (we'll describe this later on).
   2. A projection file (.prj) for the above-mentioned shapefile.
   3. A configuration file (.properties). This file contains properties such as cell size in x and y direction, the number of rasters for the ImageMosaic coverage, etc.. We will describe this file in the next section.
   
Normally GeoServer will create automatically those files when pointed a directory containing images, but it's important to understand them in order to get better control on how the mosaic works, and troubleshoot eventual issues.

Configuration File
-------------------   

The mosaic configuration file is used to store some configuration parameters to control the ImageMosaic plugin. It is created as part of the mosaic creation and usually do not require manual editing.
The table below describes the various elements in this configuration file.

.. list-table::
   :widths: 15 5 80

   * - **Parameter**
     - **Mandatory**
     - **Description**
   * - *Envelope2D*
     - Y
     - Contains the envelope for this mosaic formatted as LLCx,LLXy URCx,URCy (notice the space between the coordinates  of the Lower Left Corner and the coordinates of the Upper Right Corner). An example is *Envelope2D=432500.25,81999.75 439250.25,84999.75*
   * - *LevelsNum*
     - Y
     - Represents the number of reduced resolution layers that we currently have for the granules of this mosaic.
   * - *Levels*
     - Y
     - Represents the resolutions for the various levels of the granules of this mosaic. Please remember that we are currently assuming that the number of levels and the resolutions for such levels are the same across alll the granules.
   * - *Name*
     - Y
     - Represents the name for  this mosaic.
   * - *ExpandToRGB*
     - N
     - Applies to colormapped granules. Asks the internal mosaic engine to expand the colormapped granules  to RGB prior to mosaicking them. This is needed whenever the the granules do not share the same color map hence a straight composition that would retain such a color map cannot be performed.
   * - *AbsolutePath*
     - Y
     - It controls whether or not the path stored inside the "location" attribute  represents an absolute path or a path relative to the location of the shapefile index. Notice that  a relative index  ensure much more portability of the mosaic itself. Default value for this parameter is False, which means relative paths.
   * - *LocationAttribute*
     - N
     - The name of the attribute path in the shapefile index. Default value is *location*.    

   
Creating Granules Index  and Configuration File
-----------------------------------------------
   
The refactored version of the ImageMosaic plugin can be used to create the shapefile index as well as the mosaic  configuration file on the fly without having to rely on gdal or some  other similar utility. 

If you have a tree of directories containing the granules you want to be able to serve as a mosaic (and providing that you are respecting the conditions written above) all you need to do is to point the GeoServer to such a directory and it will create the proper ancillary files by inspecting all the files present in the the tree of directories starting from the provided input one.


Configuring a Coverage in GeoServer
-----------------------------------


This is a process very similar to creating a FeatureType. More specifically, one has to perform the steps highlighted in the sections here below.


Create a new CoverageStore:
'''''''''''''''''''''''''''

1. Go to "Data Panel | Stores" via the web interface and click 'Add new Store'. Finally click "ImageMosaic - Image mosaicking plugin" from "Raster Data Source":

.. figure:: img/imagemosaiccreate.png
   :align: center

   *ImageMosaic in the list of raster data stores*


2. In order to create a new mosaic is necessary:

- To chose the Workspace in the 'Basic Store Info' section.

- To give a name in the 'Basic Store Info' section.

- To fill the field URL in the 'Connection Parameters' section. You have three alternatives:

	- Inserting the absolute path of the shapefile.

	- Inserting the absolute path of the directory in which the mosaic shapefile index resides, the GeoServer will look for it and make use of it. 

	- Inserting the absolute path of a directory where the files you want to  mosaic together reside.  In this case GeoServer automatically creates the needed mosaic files (.dbf, .prj, .properties, .shp and .shx) by inspecting the data of present in the given directory (GeoServer will also find the data in the subdirectories).

Finally click the "Save" button:

.. figure:: img/imagemosaicconfigure.png
   :align: center

   *Configuring an ImageMosaic data store*


Create a new Coverage using the new ImageMosaic CoverageStore:
''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''


1. Go to "Data Panel | Layers" via the web interface and click 'Add a new resource'. Finally choose the name of the Store you just created:

.. figure:: img/newlayerchoser.png
   :align: center

*Layer Chooser*

2. Click on the layer you wish to configure and you will be presented with the Coverage Editor:

.. figure:: img/coverageeditor.png
   :align: left

*Coverage Editor*


3. Make sure there is a value for "Native SRS", then click the Submit button. If the "Native CRS" is 'UNKNOWN', you must to declare the SRS specifying him in the "Declared SRS" field. Hopefully there are no errors.

4. Click on the Save button.

Once you complete the preceding operations it is possible to access the OpenLayers map preview of the created mosaic.

.. warning:: If the created layer appears to be all black, it may be that GeoServer has not found any acceptable granules in the provided ImageMosaic index. It is also possible that the shapefile index is empty (no granules were found in in the provided directory) or it might be that the granules' paths in the shapefile index are not correct, which could happen if an existing index (using absolute paths) is moved to another place. If the shapefile index paths are not correct, then the DBF file can be opened and fixed with OpenOffice (for example). As an alternative would be to delete the index and let GeoServer recreate it from the root directory.

Tweaking an ImageMosaic CoverageStore:
''''''''''''''''''''''''''''''''''''''

The Coverage Editor gives users the possibility to set a few control parameters to further tweak and/or control the mosaic creation process. The parameters are as follows:

.. list-table::
   :widths: 20 80

   * - **Parameter**
     - **Description**
   * - *MaxAllowedTiles*
     - Set the maximum number of the tiles that can be loaded simultaneously for a request. In case of a large mosaic this parameter should be opportunely set to not saturating the server with too many granules loaded at  the same  time.
   * - *BackgroundValues*
     - Set the value of the mosaic background. Depending on the nature of the mosaic it is wise to set a value for the 'no data' area (usually -9999). This value is repeated on all the mosaic bands.
   * - *Filter* 
     - Set the default mosaic filter. It should be a valid ECQL query which will be used as default if no 'cql_filter' are specified (instead of Filter.INCLUDE). If the cql_filter is specified in the request it will be overridden.

.. note:: Do not use this filter to change time or elevation dimensions defaults. It will be added as AND condition with CURRENT for 'time' and LOWER for 'elevation'.

   * - *OutputTransparentColor*
     - Set the transparent color for the created mosaic. See below for an example:

.. figure:: img/output_color.png
   :align: left

*OutputTransparentColor parameter configured with 'no color'*

.. figure:: img/output_color2.png
   :align: left

*OutputTransparentColor parameter configured with 'no data' color*

.. list-table::
   :widths: 20 80
   
   * - *InputTransparentColor*
     - Set the transparent color for the granules prior to mosaicking them in order to control the superimposition process between them. When GeoServer composes the granules to satisfy the user request, some of them can overlap some others, therefore, setting this parameter with the opportune color avoids the overlap of 'no data' areas between granules. See below for an example:

.. figure:: img/input_color.png
   :align: left

*InputTransparentColor parameter not configured*

.. figure:: img/input_color2.png
   :align: left

*InputTransparentColor parameter configured*

.. list-table::
   :widths: 20 80
   
   * - *AllowMultithreading*
     - If true enable  tiles multithreading loading. This allows to perform parallelized loading of the granules that compose the mosaic.
   * - *USE_JAI_IMAGEREAD*
     - Controls the low level mechanism to read the granules. If 'true' GeoServer will make use of JAI ImageRead operation and its deferred loading mechanism, if  'false' GeoServer will perform direct ImageIO read calls which will result in immediate loading.
   * - *SUGGESTED_TILE_SIZE:*
     - Controls the tile size  of the input granules as well as the tile size of  the output mosaic. It consists of two positive integers separated by a comma,like 512,512.
     
.. note:: Deferred loading consumes less memory since it uses a streaming approach to load in memory only the data that is needed for the processing at each time, but, on the other side, may cause problems under heavy load since it keeps granules' files open for a long time to support deferred  loading.

.. note:: Immediate loading consumes more memory since it loads in memory the whole requested mosaic at once, but, on the other side, it usually performs faster and does not leave  room for "too many files open" error conditions as it happens for deferred loading.



Configuration examples
----------------------

Now we are going to provide a few examples of mosaic configurations to demonstrate how we can make use of the ImageMosaic parameters.


DEM/Bathymetric mosaic configuration (raw data)
'''''''''''''''''''''''''''''''''''''''''''''''

Such a mosaic can be use to serve large amount of data which represents altitude or depth and therefore does not specify colors directly while it reather needs an SLD to generate pictures. In our case we have a DEM dataset which consists of a set of raw GeoTIFF files.

The first operation is to create the CoverageStore following the three steps showed in 'Create a new CoverageStore' specifying, for example, the path of the shapefile in the 'URL' field. 
Inside the Coverage Editor, Publishing tab - Default Title section, you can specify the 'dem' default style (Default Style combo box) in order to represent the visualization style of the mosaic. The following is an example style:

.. code-block:: xml

  <?xml version="1.0" encoding="ISO-8859-1"?>
  <StyledLayerDescriptor version="1.0.0"
    xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
    xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.opengis.net/sld 	http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">
    <NamedLayer>
      <Name>gtopo</Name>
      <UserStyle>
        <Name>dem</Name>
        <Title>Simple DEM style</Title>
        <Abstract>Classic elevation color progression</Abstract>
        <FeatureTypeStyle>
          <Rule>
            <RasterSymbolizer>
              <Opacity>1.0</Opacity>
              <ColorMap>
                <ColorMapEntry color="#000000" quantity="-9999" label="nodata" opacity="1.0" />
                <ColorMapEntry color="#AAFFAA" quantity="0" label="values" />
                <ColorMapEntry color="#00FF00" quantity="1000" label="values" />
                <ColorMapEntry color="#FFFF00" quantity="1200" label="values" />
                <ColorMapEntry color="#FF7F00" quantity="1400" label="values" />
                <ColorMapEntry color="#BF7F3F" quantity="1600" label="values" />
                <ColorMapEntry color="#000000" quantity="2000" label="values" />
              </ColorMap>
            </RasterSymbolizer>
          </Rule>
        </FeatureTypeStyle>
      </UserStyle>
    </NamedLayer>
  </StyledLayerDescriptor>

In this way you have a clear distinction between the different intervals of the dataset that compose the mosaic, like the background and the 'no data' area.

.. figure:: img/vito_config_1.png
   :align: left

.. note:: The 'no data' on the sample mosaic is -9999, on the other  side the default background value is for mosaics is '0.0'.

The result is the following.


.. figure:: img/vito_1.png
   :align: left

   *Basic configuration*


By setting in opportune  ways the other configuration parameters, it is possible to improve at the same time both the appearance of the mosaic as well as the its performances. As an instance we could:

1. Make the 'no data' areas transparent and coherent with the real data. To achieve this we need to change the opacity of the 'no data' ColorMapEntry in the 'dem' style to '0.0' and set 'BackgroundValues' parameter at '-9999' so that empty areas will be filled with this value. The result is as follows:


.. figure:: img/vito_2.png
   :align: left

   *Advanced configuration*


2. Allow multithreaded granules loading. By setting the 'AllowMultiThreading' parameter to true, GeoServer will load the granules in parallel using multiple threads with a consequent increase of the performances on some architectures..


The configuration parameters are the followings:

1. MaxAllowedTiles: 2147483647

2. BackgroundValues: -9999.

3. OutputTransparentColor: 'no color'.

4. InputImageThresholdValue: NaN.

5. InputTransparentColor: 'no color'.

6. AllowMultiThreading: true.

7. USE_JAI_IMAGEREAD: true.

8. SUGGESTED_TILE_SIZE: 512,512.


Aerial Imagery mosaic configuration
'''''''''''''''''''''''''''''''''''

In this example we are going to create a mosaic that will serve aerial imagery, RGB geotiffs in this case. Noticed that since we are talking about visual data, in the Coverage Editor you can use the basic 'raster' style, as reported here below, which is just a stub SLD to instruct the  GeoServer raster renderer to not do anything particular in terms of color management:

.. code-block:: xml

  <?xml version="1.0" encoding="ISO-8859-1"?>
  <StyledLayerDescriptor version="1.0.0"
    xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
    xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.opengis.net/sld 	http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">
    <NamedLayer>
      <Name>raster</Name>
      <UserStyle>
        <Name>raster</Name>
        <Title>Raster</Title>
        <Abstract>A sample style for rasters, good for displaying imagery	</Abstract>
        <FeatureTypeStyle>
          <FeatureTypeName>Feature</FeatureTypeName>
          <Rule>
            <RasterSymbolizer>
              <Opacity>1.0</Opacity>
            </RasterSymbolizer>
          </Rule>
        </FeatureTypeStyle>
      </UserStyle>
    </NamedLayer>
  </StyledLayerDescriptor>


The result is the following.


.. figure:: img/prato_1.png
   :align: left
   
   *Basic configuration*

.. note:: Those ugly black areas, are the resulting of applying the default mosaic parameters to a mosaic that does not entirely cover its bounding box. The areas within the BBOX that are not covered with data will default to a value of 0 on each band. Since this mosaic is RGB we can simply set  the OutputTransparentColor to 0,0,0 in order to get transparent fills for the BBOX.

The  various parameters can be set as follows:

1. MaxAllowedTiles: 2147483647

2. BackgroundValues: default value.

3. OutputTransparentColor: #000000 (to make transparent the background).

4. InputImageThresholdValue: NaN.

5. InputTransparentColor: 'no color'.

6. AllowMultiThreading: true (in this way GeoServer manages the loading of the tiles in parallel mode which will increase performance).

7. USE_JAI_IMAGEREAD: true.

8. SUGGESTED_TILE_SIZE: 512,512.


The results is the following:


.. figure:: img/prato_2.png
   :align: left

   *Advanced configuration*


Scanned Maps mosaic configuration
'''''''''''''''''''''''''''''''''

In this case we want to show how to serve scanned maps (mostly B&W images) via a GeoServer mosaic.

In the Coverage Editor you can use the basic 'raster' style as shown above since there is not need to use any of the advanced RasterSymbolizer capabilities.

The result is the following.


.. figure:: img/iacovella_1.png
   :align: left

   *Basic configuration*

This mosaic, formed by two single granules,  shows a typical case where the 'no data' collar areas of the granules overlap, as it is shown in the picture above.
In this case we can use the 'InputTransparentColor' parameter to make the collar areas disappear during the superimposition process, as instance, in this case, by using the '#FFFFFF' 'InputTransparentColor'.  

This is the result:


.. figure:: img/iacovella_2.png
   :align: left

   *Advanced configuration*



The final configuration parameters are the followings:

1. MaxAllowedTiles: 2147483647

2. BackgroundValues: default value.

3. OutputTransparentColor: 'no color'.

4. InputImageThresholdValue: NaN.

5. InputTransparentColor: #FFFFFF.

6. AllowMultiThreading: true (in this way GeoServer manages the loading of the tiles in parallel mode which will increase performance)

7. USE_JAI_IMAGEREAD: true.

8. SUGGESTED_TILE_SIZE: 512,512.


