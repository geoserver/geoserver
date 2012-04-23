/* 
 * Copyright (c) 2001 - 20089 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.geoserver.data.test.MockData;

/**
 * Mock data for testing TimeSeries with list value in app-schema {@link WaterMLTimeSeriesWfsTest}
 * 
 * Inspired by {@link MockData}.
 * 
 * @author Rini Angreani (CSIRO Earth Science and Resource Engineering)
 */
public class WaterMLTimeSeriesMockData extends AbstractAppSchemaMockData {

    /**
     * Prefix for waterml namespace.
     */
    protected static final String WML2DR_PREFIX = "wml2dr";
    /**
     * URI for waterml namespace.
     */
    protected static final String WML2DR_URI = "http://www.opengis.net/waterml/DR/2.0";
    /**
     * Prefix for gmlcov namespace.
     */
    protected static final String GMLCOV_PREFIX = "gmlcov";

    /**
     * URI for gmlcov namespace.
     */
    protected static final String GMLCOV_URI = "http://www.opengis.net/gmlcov/1.0";
    /**
     * Prefix for swe 2.0 namespace.
     */
    protected static final String SWE2_PREFIX = "swe";

    /**
     * URI for swe 2.0 namespace.
     */
    protected static final String SWE2_URI = "http://www.opengis.net/swe/2.0";

    public WaterMLTimeSeriesMockData() {
        super(GML32_NAMESPACES);
    }

    /**
     * @see org.geoserver.test.AbstractAppSchemaMockData#addContent()
     */
    @Override
    public void addContent() {
        putNamespace(WML2DR_PREFIX, WML2DR_URI);
        addFeatureType(WML2DR_PREFIX, "MeasurementTimeseriesDomainRange",
                "WaterMLTimeSeriesTest.xml", "timeseries.properties", "schemas/wml2dr_catalog.xml",
                "schemas/gmlcov/1.0/coverage.xsd",
                "schemas/waterml/DR/2.0/timeseries-domain-range.xsd",
                "schemas/sweCommon/2.0/swe.xsd", "schemas/sweCommon/2.0/simple_encodings.xsd",
                "schemas/sweCommon/2.0/advanced_encodings.xsd",
                "schemas/sweCommon/2.0/basic_types.xsd",
                "schemas/sweCommon/2.0/block_components.xsd",
                "schemas/sweCommon/2.0/choice_components.xsd",
                "schemas/sweCommon/2.0/simple_components.xsd",
                "schemas/sweCommon/2.0/record_components.xsd");
    }
}
