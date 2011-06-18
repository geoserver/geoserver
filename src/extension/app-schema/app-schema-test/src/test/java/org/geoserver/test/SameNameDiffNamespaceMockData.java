/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

/**
 * 
 * @author Niels Charlier, Curtin University Of Technology
 * 
 */

public class SameNameDiffNamespaceMockData extends AbstractAppSchemaMockData {
    
    /**
     * Prefix for ex namespace.
     */
    protected static final String EX_PREFIX = "ex";

    /**
     * URI for ex namespace.
     */
    protected static final String EX_URI = "http://example.com";
    
    

    /**
     * @see org.geoserver.test.AbstractAppSchemaMockData#addContent()
     */
    @Override
    public void addContent() {
        putNamespace(EX_PREFIX, EX_URI);
        
       addFeatureType(EX_PREFIX, "MyTestFeature", "SameNameDiffNamespace.xml", "SameNameDiffNamespace.properties",
                "SameNameDiffNamespace.xsd");

    }

}
