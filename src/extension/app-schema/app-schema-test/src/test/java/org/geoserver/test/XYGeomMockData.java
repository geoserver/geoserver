/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.geoserver.data.test.MockData;
import org.geotools.data.complex.AppSchemaDataAccess;

/**
 * Mock data for testing use of geometryless data sources {@link AppSchemaDataAccess} with
 * GeoServer.
 *
 * <p>Inspired by {@link MockData}.
 *
 * @author Rob Atkinson
 * @author Ben Caradoc-Davies, CSIRO Exploration and Mining
 */
public class XYGeomMockData extends AbstractAppSchemaMockData {

    /** Prefix of the test namespace. */
    public static final String TEST_PREFIX = "test";

    /** The test namespace URI. */
    public static final String TEST_URI = "http://test";

    /** @see org.geoserver.test.AbstractAppSchemaMockData#addContent() */
    public void addContent() {
        putNamespace(TEST_PREFIX, TEST_URI);
        addFeatureType(
                TEST_PREFIX,
                "PointFeature",
                "PointFeature.xml",
                "PointFeatureGeomPropertyfile.properties",
                "GeometrylessTest.xsd");
    }
}
