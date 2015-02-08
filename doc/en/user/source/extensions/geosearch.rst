.. _geosearch_extension:

GeoSearch
=========

GeoSearch Indexing Module
*************************

The GeoSearch indexing module adds support to GeoServer for exposing your data to Google's GeoSearch. This makes it so more people can find your data, by searching directly on Google Maps or Google Earth. The format exposed is KML, so other search engines will also be able to crawl it when they are ready - Google is just the first to support it for sure. By default no data is published, but we highly encourage you to if your data can be publicly available, to help grow the wider geospatial web. Publishing is easy, as it is a part of the administration interface. For more information about geosearch see `this blog <http://googlemapsapi.blogspot.com/2008/05/geo-search-20-data-in-data-out.html>`_.

How It Works
************

The GeoSearch module adds a sitemap.xml endpoint in the GeoServer REST API; that is, 
`http://localhost:8080/geoserver/rest/sitemap.xml <http://localhost:8080/geoserver/rest/sitemap.xml>`_ is your sitemap.  By submitting the sitemap through Google's webmaster tools, you can get your map layers to show up in searches on http://maps.google.com/.

Step By Step
************

A more explicit guide to using the GeoSearch module follows.

   1. Load your data as normal.
   2. Go to the Layer configuration page in GeoServer's admin console for each layer you would like to expose, and check the 'enable searching' checkbox on the *Publishing* tab.
   3. Submit your sitemap.xml using Google's webmaster tools. From your dashboard, pick the domain on which your server lives. In the menu on the left, click on "Sitemaps" and then "Add Sitemap". You are adding a "General Web Sitemap", and provide the URL equivalent http://localhost:8080/geoserver/rest/sitemap.xml .

The reason we are using "General Web Sitemap", as opposed to a "Geo Sitemap", is that sitemap.xml is really a sitemap index that links to a geo sitemap for each layer.

Behind the Scenes
*****************

GeoServer already has support for breaking up a dataset into regionated tiles. The information about what features belong in each tile is stored in an H2 database in $GEOSERVER_DATA_DIR/geosearch . We use this information when creating the sitemaps for Google. However, since the hierarchy may not be fully explored by the time a sitemap is submitted, the sitemaps also contain links to tiles deeper in the hierarchy, thereby expanding it. Some of these tiles may be empty, in which case Googlebot will receive a 204 response.

Big datasets
************

If you are making big datasets available (between 50000 and 2000000 individual features), you should consider doing the following. The main burden is to sort the features according to an attribute, so that they are output in order of importance and included in exactly one tile.

   1. Use a backend that supports queries, such as Postgis. You can use shp2psql to convert from a Shapefile to a SQL format supported by Postgis. Be sure to specify that you want a GIST (geospatial index) to be created, and provide the SRS. (-I and -s)
   2. Make sure your database has a primary index (an auto-incrementing integer is fine) and a spatial index on the geometry column
   3. Put an index on the column that you are going to sort the feature by. If you are using the size of the geometry, consider making an auxilliary column that contains the precalculated value and put an index on that. Note that GeoServer always sorts in descending order, so features you consider important should have a high value.
   4. In GeoServer's feature type configuration, be sure to use "native-sorting" for the regionating strategy, and your chosen column as the regionating attribute.
   5. KML Feature Limit should generally be set to 50. It's a balancing act between too much information per tile (Googlebot prefers document that are less than 1 megabyte) and a big hierarchy that takes long to build.
