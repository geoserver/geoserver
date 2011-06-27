/* 
 * Copyright (c) 2001 - 20089 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

/**
 * @author Niels Charlier (Curtin University Of Technology)
 * 
 */
public class IdNotEncodedMockData extends AbstractAppSchemaMockData {

    /**
     * Prefix for gwml namespace.
     */
    protected static final String GWML_PREFIX = "gwml";

    /**
     * URI for gwml namespace.
     */
    protected static final String GWML_URI = "http://www.nrcan.gc.ca/xml/gwml/1";

    /**
     * @see org.geoserver.test.AbstractAppSchemaMockData#addContent()
     */
    @Override
    public void addContent() {
        putNamespace(GWML_PREFIX, GWML_URI);
        addFeatureType(GSML_PREFIX, "MappedInterval", "MappedInterval.xml",  "HydrostratigraphicUnit.xml",
                "mappedinterval.properties", "HydrogeologicUnit.xsd");
    }

}
