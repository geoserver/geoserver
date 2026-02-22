/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.geoserver.data.test.MockData;

/**
 * Mock data for testing SRS reprojection in app-schema {@link SRSReprojectionTest}
 *
 * <p>Inspired by {@link MockData}.
 *
 * @author Victor Tey, CSIRO Exploration and Mining
 */
public class SRSReprojectionMockData extends AbstractAppSchemaMockData {
    /** Prefix for ex namespace. */
    protected static final String EX_PREFIX = "ex";

    /** URI for ex namespace. */
    protected static final String EX_URI = "http://example.com";

    /** @see org.geoserver.test.AbstractAppSchemaMockData#addContent() */
    @Override
    public void addContent() {
        addFeatureType(
                GSML_PREFIX,
                "MappedFeature",
                "MappedFeature.xml",
                "MappedFeature.properties",
                "ObservationMethod.xml",
                "ObservationMethod.properties");
        putNamespace(EX_PREFIX, EX_URI);
        addFeatureType(
                EX_PREFIX,
                "geomContainer",
                "SRSReprojectionTest.xml",
                "SRSTestPropertyfile.properties",
                "NestedGeometry.xsd");
    }
}
