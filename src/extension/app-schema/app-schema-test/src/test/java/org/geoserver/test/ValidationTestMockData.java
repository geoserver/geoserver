/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

/**
 * Mock data for testing validation with GeoServer.
 *
 * @author Victor Tey, CSIRO Exploration and Mining
 */
public class ValidationTestMockData extends AbstractAppSchemaMockData {
    protected static final String ER_PREFIX = "er";

    protected static final String ER_URI = "urn:cgi:xmlns:GGIC:EarthResource:1.1";

    /** @see org.geoserver.test.AbstractAppSchemaMockData#addContent() */
    @Override
    public void addContent() {
        addFeatureType(GSML_PREFIX, "GeologicUnit", "Nillable.xml", "Nillable.properties");
        addFeatureType(
                GSML_PREFIX, "MappedFeature", "NillableWithError.xml", "Nillable.properties");
        putNamespace(ER_PREFIX, ER_URI);
        addFeatureType(
                ER_PREFIX, "Commodity", "nillableSimpleContentInteger.xml", "Nillable.properties");
    }
}
