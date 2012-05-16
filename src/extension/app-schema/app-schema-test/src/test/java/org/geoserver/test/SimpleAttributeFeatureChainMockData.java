/*
 * Copyright (c) 2001 - 2010 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

/**
 * Mock data for testing feature chaining with simple content type (gml:name).
 * @author Rini Angreani, CSIRO Earth Science and Resource Engineering
 *
 */
public class SimpleAttributeFeatureChainMockData extends AbstractAppSchemaMockData {

    /**
     * @see org.geoserver.test.AbstractAppSchemaMockData#addContent()
     */
    public void addContent() {
        addFeatureType(GSML_PREFIX, "MappedFeature", "SimpleAttributeFeatureChainTest.xml",
                "MappedFeatureWithNestedName.properties", "MappedFeatureNameOne.properties",
                "MappedFeatureNameTwo.properties");
    }
}
