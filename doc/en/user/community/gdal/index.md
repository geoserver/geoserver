# GDAL based WCS Output Format

The gdal_translate based output format leverages the availability of the gdal_translate command to allow the generation of more output formats than GeoServer can natively produce. The basic idea is to dump to the file system a file that gdal_translate can translate, invoke it, zip and return the output of the translation.

This extension is thus the equivalent of the [OGR extension](../../extensions/ogr.md) for raster data.

## Out of the box behaviour

Out of the box the plugin assumes the following:

- gdal_translate is available in the path
- the GDAL_DATA variable is pointing to the GDAL data directory (which stores the spatial reference information for GDAL)

In the default configuration the following formats are supported:

- JPEG-2000 part 1 (ISO/IEC 15444-1)
- Geospatial PDF
- Arc/Info ASCII Grid
- ASCII Gridded XYZ

The list might be shorter if gdal_translate has not been built with support for the above formats (for example, the default JPEG-2000 format relies on the [JasPer-based GDAL driver](http://www.gdal.org/frmt_jpeg2000.md)).

Once installed in GeoServer, a bunch of new supported formats will be listed in the `ServiceMetadata` section of the WCS 2.0 GetCapabilities document, e.g. `image/jp2` and `application/pdf`.

## gdal_translate conversion abilities

The gdal_translate utility is usually able to convert more formats than the default setup of this output format allows for, but the exact list depends on how the utility was built from sources. To get a full list of the formats available by your ogr2ogr build just run:

    gdal_translate --long-usage 

and you'll get the full set of options usable by the program, along with the supported formats.

For example, the above produces the following output using gdal 1.11.2 compiled with libgeotiff 1.4.0, libpng 1.6,  libjpeg-turbo 1.3.1, libjasper 1.900.1 and libecwj2 3.3:

```
Usage: gdal_translate [--help-general] [--long-usage]
    [-ot {Byte/Int16/UInt16/UInt32/Int32/Float32/Float64/
          CInt16/CInt32/CFloat32/CFloat64}] [-strict]
    [-of format] [-b band] [-mask band] [-expand {gray|rgb|rgba}]
    [-outsize xsize[%] ysize[%]]
    [-unscale] [-scale[_bn] [src_min src_max [dst_min dst_max]]]* [-exponent[_bn] exp_val]*
    [-srcwin xoff yoff xsize ysize] [-projwin ulx uly lrx lry] [-epo] [-eco]
    [-a_srs srs_def] [-a_ullr ulx uly lrx lry] [-a_nodata value]
    [-gcp pixel line easting northing [elevation]]*
    [-mo "META-TAG=VALUE"]* [-q] [-sds]
    [-co "NAME=VALUE"]* [-stats] [-norat]
    src_dataset dst_dataset

GDAL 1.11.2, released 2015/02/10

The following format drivers are configured and support output:
  VRT: Virtual Raster
  GTiff: GeoTIFF
  NITF: National Imagery Transmission Format
  HFA: Erdas Imagine Images (.img)
  ELAS: ELAS
  AAIGrid: Arc/Info ASCII Grid
  DTED: DTED Elevation Raster
  PNG: Portable Network Graphics
  JPEG: JPEG JFIF
  MEM: In Memory Raster
  GIF: Graphics Interchange Format (.gif)
  XPM: X11 PixMap Format
  BMP: MS Windows Device Independent Bitmap
  PCIDSK: PCIDSK Database File
  PCRaster: PCRaster Raster File
  ILWIS: ILWIS Raster Map
  SGI: SGI Image File Format 1.0
  SRTMHGT: SRTMHGT File Format
  Leveller: Leveller heightfield
  Terragen: Terragen heightfield
  ISIS2: USGS Astrogeology ISIS cube (Version 2)
  ERS: ERMapper .ers Labelled
  ECW: ERDAS Compressed Wavelets (SDK 3.x)
  JP2ECW: ERDAS JPEG2000 (SDK 3.x)
  FIT: FIT Image
  JPEG2000: JPEG-2000 part 1 (ISO/IEC 15444-1)
  RMF: Raster Matrix Format
  RST: Idrisi Raster A.1
  INGR: Intergraph Raster
  GSAG: Golden Software ASCII Grid (.grd)
  GSBG: Golden Software Binary Grid (.grd)
  GS7BG: Golden Software 7 Binary Grid (.grd)
  R: R Object Data Store
  PNM: Portable Pixmap Format (netpbm)
  ENVI: ENVI .hdr Labelled
  EHdr: ESRI .hdr Labelled
  PAux: PCI .aux Labelled
  MFF: Vexcel MFF Raster
  MFF2: Vexcel MFF2 (HKV) Raster
  BT: VTP .bt (Binary Terrain) 1.3 Format
  LAN: Erdas .LAN/.GIS
  IDA: Image Data and Analysis
  LCP: FARSITE v.4 Landscape File (.lcp)
  GTX: NOAA Vertical Datum .GTX
  NTv2: NTv2 Datum Grid Shift
  CTable2: CTable2 Datum Grid Shift
  KRO: KOLOR Raw
  ARG: Azavea Raster Grid format
  USGSDEM: USGS Optional ASCII DEM (and CDED)
  ADRG: ARC Digitized Raster Graphics
  BLX: Magellan topo (.blx)
  Rasterlite: Rasterlite
  PostGISRaster: PostGIS Raster driver
  SAGA: SAGA GIS Binary Grid (.sdat)
  KMLSUPEROVERLAY: Kml Super Overlay
  XYZ: ASCII Gridded XYZ
  HF2: HF2/HFZ heightfield raster
  PDF: Geospatial PDF
  ZMap: ZMap Plus Grid
```

The full list of formats that gdal_translate is able to support is available on the [GDAL site](http://www.gdal.org/formats_list.md). Mind that this output format can handle only outputs that are file based and that do support creation. So, for example, you won't be able to use the PostGIS Raster output (since it's database based) or the Arc/Info Binary Grid (creation not supported).

## Customisation

If gdal_translate is not available in the default path, the GDAL_DATA environment variable is not set, or if the output formats needs tweaking, a `gdal_translate.xml` configuration file can be created to customize the output format. The file should be put inside a `gdal` folder in the root of the GeoServer data directory.

!!! note
    GeoServer will automatically detect any change to the file and reload the configuration, without a need to restart.

The default configuration is equivalent to the following xml file:

```xml
<ToolConfiguration>
  <executable>gdal_translate</executable>
  <environment>
    <variable name="GDAL_DATA" value="/usr/local/share/gdal" />
  </environment>
  <formats>
    <Format>
      <toolFormat>JPEG2000</toolFormat>
      <geoserverFormat>GDAL-JPEG2000</geoserverFormat>
      <fileExtension>.jp2</fileExtension>
      <singleFile>true</singleFile>
      <mimeType>image/jp2</mimeType>
      <type>binary</type> <!-- not really needed, it's the default -->
 <option>-co</option>
 <option>FORMAT=JP2</option>
    </Format>
    <Format>
      <toolFormat>PDF</toolFormat>
      <geoserverFormat>GDAL-PDF</geoserverFormat>
      <fileExtension>.pdf</fileExtension>
      <singleFile>true</singleFile>
      <mimeType>application/pdf</mimeType>
      <formatAdapters>
        <GrayAlphaToRGBA/>
        <PalettedToRGB/>
      </formatAdapters>
    </Format>
    <Format>
      <toolFormat>AAIGrid</toolFormat>
      <geoserverFormat>GDAL-ArcInfoGrid</geoserverFormat>
      <fileExtension>.asc</fileExtension>
      <singleFile>false</singleFile>
    </Format>
    <Format>
      <toolFormat>XYZ</toolFormat>
      <geoserverFormat>GDAL-XYZ</geoserverFormat>
      <fileExtension>.txt</fileExtension>
      <singleFile>true</singleFile>
      <mimeType>text/plain</mimeType>
      <type>text</type>
    </Format>
  </formats>
</ToolConfiguration>
```

The file showcases all possible usage of the configuration elements:

- `executable` can be just gdal_translate if the command is in the path, otherwise it should be the full path to the executable. For example, on a Linux box with a custom build GDAL library might be:

  ```xml
<executable>/usr/local/bin/gdal_translate</executable>
  ```

- `environment` contains a list of `variable` elements, which can be used to define environment variables that should be set prior to invoking gdal_translate. For example, to setup a GDAL_DATA environment variable pointing to the GDAL data directory, the configuration might be:

  ```xml
<environment>
 <variable name="GDAL_DATA" value="/usr/local/share/gdal" />
</environment>
  ```

- `Format` defines a single format, which is defined by the following tags:

  - `toolFormat`: the name of the format to be passed to gdal_translate with the -of option (case insensitive).
  - `geoserverFormat`: is the name of the output format as advertised by GeoServer
  - `fileExtension`: is the extension of the file generated after the translation, if any (can be omitted)
  - `option`: can be used to add one or more options to the gdal_translate command line. As you can see by the JPEG2000 example, each item must be contained in its own `option` tag. You can get a full list of options by running `gdal_translate --help` or by visiting the [GDAL website](http://www.gdal.org)). Also, consider that each format supports specific creation options, listed in the description page for each format (for example, here is the [JPEG2000 one](http://www.gdal.org/frmt_jpeg2000.md)).
  - `singleFile`: if true the output of the conversion is supposed to be a single file that can be streamed directly back without the need to wrap it into a zip file
  - `mimeType`: the mime type of the file returned when using `singleFile`. If not specified `application/octet-stream` will be used as a default.
  - `formatAdapters`: transformations on the coverage that might need to be applied in order to successfully encode the output. The transformations are applied only if their input conditions are met.

The available format adapters are:

- `GrayAlphaToRGBA`: expands a gray image with alpha channel to RGBA (mandatory for geospatial PDF for example)
- `PallettedToRGB`: expands a paletted image RGB(A) (mandatory for geospatial PDF for example)
