/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.geoserver.data.test.MockData;

/**
 * Mock data for PolymorphismWfsTest.
 *
 * <p>Inspired by {@link MockData}.
 *
 * @author Rini Angreani, CSIRO Earth Science and Resource Engineering
 */
public class PolymorphismMockData extends AbstractAppSchemaMockData {

    /** Prefix for ex namespace. */
    protected static final String EX_PREFIX = "ex";

    /** URI for ex namespace. */
    protected static final String EX_URI = "http://example.com";

    /** @see org.geoserver.test.AbstractAppSchemaMockData#addContent() */
    @Override
    public void addContent() {
        putNamespace(EX_PREFIX, EX_URI);
        putNamespace(GSML_PREFIX, GSML_URI);
        addFeatureType(
                EX_PREFIX,
                "PolymorphicFeature",
                "polymorphism.xml",
                "PolymorphicFeature.properties",
                "CGITermValue.xml",
                "CGITermValue.properties",
                "exposureColor.properties",
                "polymorphism.xsd");
    }
}
