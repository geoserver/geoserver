# Supported filter languages

Data filtering in GeoServer is based on the concepts found in the [OGC Filter Encoding Specification](http://www.opengeospatial.org/standards/filter).

GeoServer accepts filters encoded in two different languages: *Filter Encoding* and *Common Query Language*.

## Filter Encoding

The **Filter Encoding** language is an XML-based method for defining filters. XML Filters can be used in the following places in GeoServer:

-   in WMS `GetMap` requests, using the `filter` parameter
-   in WFS `GetFeature` requests, using the `filter` parameter
-   in SLD Rules, in the [Filter](../styling/sld/introduction.md) element

The Filter Encoding language is defined by [OGC Filter Encoding Standards](http://www.opengeospatial.org/standards/filter):

-   Filter Encoding 1.0 is used in WFS 1.0 and SLD 1.0
-   Filter Encoding 1.1 is used in WFS 1.1
-   Filter Encoding 2.0 is used in WFS 2.0

## CQL/ECQL

**CQL (Common Query Language)** is a plain-text language created for the *OGC Catalog* specification. GeoServer has adapted it to be an easy-to-use filtering mechanism. GeoServer actually implements a more powerful extension called **ECQL (Extended CQL)**, which allows expressing the full range of filters that *OGC Filter 1.1* can encode. ECQL is accepted in many places in GeoServer:

-   in WMS `GetMap` requests, using the [cql_filter](../services/wms/vendor.md) parameter
-   in WFS `GetFeature` requests, using the [cql_filter](../services/wfs/vendor.md) parameter
-   in SLD [dynamic symbolizers](../styling/sld/extensions/pointsymbols.md)

The [ECQL Reference](ecql_reference.md) describes the features of the ECQL language. The [CQL and ECQL](../tutorials/cql/cql_tutorial.md) tutorial shows examples of defining filters.

The CQL and ECQL languages are defined in:

-   [OpenGIS Catalog Services Specification](http://portal.opengeospatial.org/files/?artifact_id=3843) contains the standard definition of CQL
-   [ECQL Grammar](https://github.com/geotools/geotools/blob/main/modules/library/cql/ECQL.md) is the grammar defining the GeoTools ECQL implementation
