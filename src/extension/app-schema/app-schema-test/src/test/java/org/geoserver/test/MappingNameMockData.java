/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

/**
 * 
 * @author Niels Charlier, Curtin University of Technology
 * 
 */

public class MappingNameMockData extends AbstractAppSchemaMockData {

    /**
     * @see org.geoserver.test.AbstractAppSchemaMockData#addContent()
     */
    @Override
    public void addContent() {
        addFeatureType(GSML_PREFIX, "MappedFeature", "MappedFeatureWithHyphen.xml",
                "MappedFeature.properties", "ObservationMethodWithHyphen.xml",
                "ObservationMethod.properties");

    }

}
