/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

/**
 * Mock data for {@link EncodeIfEmptyTest}.
 *
 * @author Victor Tey (CSIRO Earth Science and Resource Engineering)
 */
public class EncodeIfEmptyMockData extends AbstractAppSchemaMockData {

    /** Prefix for om namespace. */
    protected static final String OM_PREFIX = "om";

    /** Prefix for om namespace. */
    protected static final String SWE_PREFIX = "swe";

    /** URI for om namespace. */
    protected static final String OM_URI = "http://www.opengis.net/om/2.0";

    /** URI for om namespace. */
    protected static final String SWE_URI = "http://www.opengis.net/swe/2.0";

    public EncodeIfEmptyMockData() {
        super(GML32_NAMESPACES);
        // add SchemaCatalog so validateGet() would work with unpublished schemas
        setSchemaCatalog("schemas/wml2dr_catalog.xml");
    }

    /** @see org.geoserver.test.AbstractAppSchemaMockData#addContent() */
    @Override
    public void addContent() {
        putNamespace(OM_PREFIX, OM_URI);
        putNamespace(SWE_PREFIX, SWE_URI);
        putNamespace(WaterMLTimeSeriesMockData.WML2DR_PREFIX, WaterMLTimeSeriesMockData.WML2DR_URI);
        putNamespace(WaterMLTimeSeriesMockData.GMLCOV_PREFIX, WaterMLTimeSeriesMockData.GMLCOV_URI);
        addFeatureType(
                OM_PREFIX,
                "OM_Observation",
                "Observation_2_0_EncodeIfEmpty_Test.xml",
                "timeseries.properties",
                "schemas/wml2dr_catalog.xml");
    }
}
