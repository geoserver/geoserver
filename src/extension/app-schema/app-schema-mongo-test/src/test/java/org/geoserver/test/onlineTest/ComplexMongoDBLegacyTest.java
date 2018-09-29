/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest;

import org.geoserver.data.test.SystemTestData;

/**
 * Tests the integration between MongoDB and App-schema using mappings defined using the legacy full
 * paths.
 */
public class ComplexMongoDBLegacyTest extends ComplexMongoDBSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // make complex MongoDB use legacy paths
        System.setProperty("org.geotools.data.mongodb.complex.useLegacyPaths", "true");
        super.onSetUp(testData);
    }

    @Override
    protected String getPathOfMappingsToUse() {
        return "/mappings/stations_legacy.xml";
    }
}
