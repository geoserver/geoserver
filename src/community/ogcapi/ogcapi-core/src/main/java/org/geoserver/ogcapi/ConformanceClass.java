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
     * CQL filtering conformance classes, shared here to allow STAC using them, without depending on
     * ogc-api-features directly
     */
    public static final String FEATURES_FILTER =
            "http://www.opengis.net/spec/ogcapi-features-3/1.0/req/features-filter";

    public static final String FILTER =
            "http://www.opengis.net/spec/ogcapi-features-3/1.0/req/filter";

    /** Sorting conformance class from OGC API - Records. */
    public static final String SORTBY =
            "http://www.opengis.net/spec/ogcapi-records-1/1.0/req/sorting";

    /**
     * A custom conformance class for GeoServer own ECQL, not further split into parts (as only
     * GeoServer implements it anyways
     */
    public static final String ECQL = "http://geoserver.org/spec/ecql/1.0/req/gs-ecql";

    public static final String ECQL_TEXT = "http://geoserver.org/spec/ecql/1.0/req/ecql-text";

    /** CQL2 encoding conformance classes */
    public static final String CQL2_TEXT = "http://www.opengis.net/spec/cql2/1.0/req/cql2-text";

    public static final String CQL2_JSON = "http://www.opengis.net/spec/cql2/1.0/req/cql2-json";

    /** CQL2 capabilities conformance classes */
    public static final String CQL2_BASIC = "http://www.opengis.net/spec/cql2/1.0/req/basic-cql2";

    public static final String CQL2_ADVANCED =
            "http://www.opengis.net/spec/cql2/1.0/req/advanced-comparison-operators";
    // right now includes also accent insensitive under this clause
    public static final String CQL2_CASE_INSENSITIVE =
            "http://www.opengis.net/spec/cql2/1.0/req/case-insensitive-comparison";
    public static final String CQL2_BASIC_SPATIAL =
            "http://www.opengis.net/spec/cql2/1.0/req/basic-spatial-operators";
    public static final String CQL2_SPATIAL =
            "http://www.opengis.net/spec/cql2/1.0/req/spatial-operators";

    // requires implementation of all the operators, basic comparisons do work against temporal data
    // too
    public static final String CQL2_TEMPORAL =
            "http://www.opengis.net/spec/cql2/1.0/req/temporal-operators";
    public static final String CQL2_PROPERTY_PROPERTY =
            "http://www.opengis.net/spec/cql2/1.0/req/property-property";
    public static final String CQL2_FUNCTIONS =
            "http://www.opengis.net/spec/cql2/1.0/req/functions";
    public static final String CQL2_ARITHMETIC =
            "http://www.opengis.net/spec/cql2/1.0/req/arithmetic";
}
