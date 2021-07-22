/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

/** Conformance statements for the conformance page. */
public class ConformanceClass {
    public static final String CORE = "http://www.opengis.net/spec/ogcapi-common-1/1.0/conf/core";
    public static final String HTML = "http://www.opengis.net/spec/ogcapi-common-1/1.0/conf/html";
    public static final String JSON = "http://www.opengis.net/spec/ogcapi-common-1/1.0/conf/json";
    public static final String OAS3 = "http://www.opengis.net/spec/ogcapi-common/1.0/req/oas30";

    public static final String COLLECTIONS =
            "http://www.opengis.net/spec/ogcapi-common-2/1.0/conf/collections";
    public static final String GEODATA =
            "http://www.opengis.net/spec/ogcapi-common-2/1.0/conf/geodata";

    /**
     * CQL filtering conformance classes, shared here to allow STAC usaging them, without depending
     * on ogc-api-features directly
     */
    public static final String FEATURES_FILTER =
            "http://www.opengis.net/spec/ogcapi-features-3/1.0/req/features-filter";

    public static final String FILTER =
            "http://www.opengis.net/spec/ogcapi-features-3/1.0/req/filter";
    public static final String FILTER_SPATIAL_OPS =
            "http://www.opengis.net/spec/ogcapi-features-3/1.0/req/enhanced-spatial-operators";
    public static final String FILTER_TEMPORAL =
            "http://www.opengis.net/spec/ogcapi-features-3/1.0/req/enhanced-temporal-operators";
    public static final String FILTER_FUNCTIONS =
            "http://www.opengis.net/spec/ogcapi-features-3/1.0/req/functions";
    public static final String FILTER_ARITHMETIC =
            "http://www.opengis.net/spec/ogcapi-features-3/1.0/req/arithmetic";
    public static final String FILTER_CQL_TEXT =
            "http://www.opengis.net/spec/ogcapi-features-3/1.0/req/arithmetic";
    public static final String FILTER_CQL_JSON =
            "http://www.opengis.net/spec/ogcapi-features-3/1.0/req/cql-json";
}
