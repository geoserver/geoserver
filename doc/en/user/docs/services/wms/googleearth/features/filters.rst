.. _ge_feature_filters:

Filters
=======

Though not specific to Google Earth, GeoServer has the ability to filter data returned from the :ref:`wms`. The KML Reflector will pass through any WMS ``filter`` or ``cql_filter`` parameter to GeoServer to constrain the response. 

.. note:: Filters are basically a translation of a SQL "WHERE" statement into web form.  Though limited to a single table, this allows users to do logical filters like "AND" and "OR" to make very complex queries, leveraging numerical and string comparisons, geometric operations ("bbox", "touches", "intersects", "disjoint"), "LIKE" statements, nulls, and more.

Filter
------

There simplest filter is very easy to include. It is called the ``featureid`` filter, and it lets you filter to a single feature by its ID.  The syntax is::

   featureid=<feature>
   
where <feature> is the feature and its ID.  An example would be::

   http://localhost:8080/geoserver/wms/kml?layers=topp:states&featureid=states.5

This request will output only the state of Maryland. The feature IDs of your data are most easily found by doing WFS or KML requests and examining the resulting output.


CQL Filter
----------

Using filters in a URL can be very unwieldy, as one needs to include URL-encoded XML::

   http:/localhost:8080/geoserver/wms/kml?layers=topp:states&FILTER=%3CFilter%3E%3CPropertyIsBetween%3E%3CPropertyName%3Etopp:LAND_KM%3C/PropertyName%3E%3CLowerBoundary%3E%3CLiteral%3E100000%3C/Literal%3E%3C/LowerBoundary%3E%3CUpperBoundary%3E%3CLiteral%3E150000%3C/Literal%3E%3C/UpperBoundary%3E%3C/PropertyIsBetween%3E%3C/Filter%3E

Instead, one can use Common Query Language (CQL), which allows one to specify the same statement more succinctly::

   http://localhost:8080/geoserver/wms/kml?layers=topp:states&CQL_FILTER=LAND_KM+BETWEEN+100000+AND+150000

This query will return all the states in the US with areas between 100,000 and 150,000 km^2.

