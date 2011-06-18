/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest.setup;

import org.geoserver.test.AbstractAppSchemaMockData;

public class WfsOnlineTestMockData extends AbstractAppSchemaMockData {
    @Override
    protected void addContent() {
        addFeatureType(GSML_PREFIX, "ShearDisplacementStructure",
                "WfsOnlineTest/gsml_ShearDisplacementStructure/gsml_ShearDisplacementStructure.xml");
        addFeatureType(GSML_PREFIX, "DisplacementEvent",
                "WfsOnlineTest/gsml_DisplacementEvent/gsml_DisplacementEvent.xml");
    }
}
