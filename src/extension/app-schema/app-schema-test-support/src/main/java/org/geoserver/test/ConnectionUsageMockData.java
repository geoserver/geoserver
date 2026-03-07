/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

/**
 * Mock data for testing App-Schema connection usage.
 *
 * @author Stefano Costa, GeoSolutions
 */
public class ConnectionUsageMockData extends AbstractAppSchemaMockData {

    /** Prefix for ex namespace. */
    protected static final String EX_PREFIX = "ex";

    /** URI for ex namespace. */
    protected static final String EX_URI = "http://example.com";

    /** @see org.geoserver.test.AbstractAppSchemaMockData#addContent() */
    @Override
    public void addContent() {
        putNamespace(EX_PREFIX, EX_URI);
        addFeatureType(
                EX_PREFIX,
                "ConnectionUsageParent",
                "ConnectionUsageTest.xml",
                "ConnectionUsageTest.xsd",
                "ConnectionUsageParent.properties",
                "ConnectionUsageFirstNested.properties",
                "ConnectionUsageSecondNested.properties",
                "ConnectionUsageThirdNested.properties");
    }
}
