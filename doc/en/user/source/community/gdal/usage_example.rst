For example, the above produces the following output using gdal 1.11.2 compiled with libgeotiff 1.4.0, libpng 1.6,  libjpeg-turbo 1.3.1, libjasper 1.900.1 and libecwj2 3.3::

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
