/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.geoserver.data.test.MockData;

/**
 * Mock data for testing TimeSeries with list value in app-schema {@link WaterMLTimeSeriesWfsTest}
 *
 * <p>Inspired by {@link MockData}.
 *
 * @author Rini Angreani (CSIRO Earth Science and Resource Engineering)
 */
public class WaterMLTimeSeriesMockData extends AbstractAppSchemaMockData {

    /** Prefix for waterml namespace. */
    protected static final String WML2DR_PREFIX = "wml2dr";
    /** URI for waterml namespace. */
    protected static final String WML2DR_URI = "http://www.opengis.net/waterml/DR/2.0";
    /** Prefix for gmlcov namespace. */
    protected static final String GMLCOV_PREFIX = "gmlcov";

    /** URI for gmlcov namespace. */
    protected static final String GMLCOV_URI = "http://www.opengis.net/gmlcov/1.0";
    /** Prefix for swe 2.0 namespace. */
    protected static final String SWE2_PREFIX = "swe";

    /** URI for swe 2.0 namespace. */
    protected static final String SWE2_URI = "http://www.opengis.net/swe/2.0";

    public WaterMLTimeSeriesMockData() {
        super(GML32_NAMESPACES);
        // add SchemaCatalog so validateGet() would work with unpublished schemas
        setSchemaCatalog("schemas/wml2dr_catalog.xml");
    }

    /** @see org.geoserver.test.AbstractAppSchemaMockData#addContent() */
    @Override
    public void addContent() {
        putNamespace(WML2DR_PREFIX, WML2DR_URI);
        putNamespace(GMLCOV_PREFIX, GMLCOV_URI);
        addFeatureType(
                WML2DR_PREFIX,
                "MeasurementTimeseriesDomainRange",
                "WaterMLTimeSeriesTest.xml",
                "timeseries.properties",
                "schemas/wml2dr_catalog.xml");
    }
}
