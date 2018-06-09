/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.geoserver.data.test.MockData;

/**
 * Mock data for testing GML32
 *
 * <p>Inspired by {@link MockData}.
 *
 * @author Victor Tey, CSIRO Exploration and Mining
 */
public class FeatureGML32ResolveMockData extends AbstractAppSchemaMockData {

    public FeatureGML32ResolveMockData() {
        super(GML32_NAMESPACES);
    }

    /** Prefix for ex namespace. */
    protected static final String EX_PREFIX = "ex";

    /** URI for ex namespace. */
    protected static final String EX_URI = "http://example.com";

    /** @see org.geoserver.test.AbstractAppSchemaMockData#addContent() */
    @Override
    public void addContent() {
        putNamespace(EX_PREFIX, EX_URI);

        addFeatureType(
                GSML_PREFIX,
                "MappedFeature",
                "MappedFeature32Resolve.xml",
                "MappedFeaturePropertyfile.properties");
        addFeatureType(
                GSML_PREFIX,
                "GeologicUnit",
                "GeologicUnit32Resolve.xml",
                "GeologicUnit.properties");
    }
}
