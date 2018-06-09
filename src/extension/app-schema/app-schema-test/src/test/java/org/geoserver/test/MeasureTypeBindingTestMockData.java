/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

/** @author Xiangtan Lin, CSIRO Information Management and Technology */
public class MeasureTypeBindingTestMockData extends AbstractAppSchemaMockData {

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
                "PolymorphicFeature",
                "MeasureTypeBindingTest.xml",
                "MeasureTypeBindingTest.properties",
                "polymorphism.xsd");
    }
}
