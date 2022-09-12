.. _community_vector_mosaic_delegate:

Vector Mosaic Datastore Delegate Requirements
=============================================

The Vector Mosaic Datastore Delegate is a datastore that contains references to the vector granule datastores, bounding polygon or multipolygon geometry to delineate the index area, and optionally other attributes that can be queried in order to return vector granules.

The delegate datastore can be in any format that GeoServer supports but there are two required fields.  There must be a geometry field representing the index spatial area in either Polygon or MultiPolygon form.  There also must be a field called ``params`` in text format that contains either URIs pointing at granule resources like shapefiles -or- a configuration string in .properties format. (See `Java Properties file <https://en.wikipedia.org/wiki/.properties>`_ for more details about the format).

Any delegate parameters beyond the two required can serve as queryable/filterable attributes used to narrow the number of potential granule vectors that are searched by a query.  The non-required parameters will be combined with the vector granule parameters to create the output featuretype.

================================
Creating an Index with ogrtindex
================================
The `ogrtindex <https://gdal.org/programs/ogrtindex.html>`_ commandline tool from the GDAL library can be used to create a simple delegate layer.  Here is an example that generates a delegate shapefile from a directory of shapefiles.  Note that the third step below is to use ogr2ogr commandline to trim a comma and number that ogrtindex appends to the end of the granule reference and to append the URI identifier to the front of the reference.

#. Switch to directory with the shapefiles
#. ogrtindex  -write_absolute_path -tileindex "params" delegate_raw.shp \*.shp
#. ogr2ogr delegate.shp delegate_raw.shp -dialect SQLite -sql "SELECT Geometry,'file://'||SUBSTR(params,1,LENGTH(params)-2) AS params from delegate_raw"

An example of a delegate in property datastore format can be found `here <https://github.com/geotools/geotools/main/modules/unsupported/vector-mosaic/src/test/resources/org.geotools.vectormosaic.data/mosaic_delegate.properties>`_