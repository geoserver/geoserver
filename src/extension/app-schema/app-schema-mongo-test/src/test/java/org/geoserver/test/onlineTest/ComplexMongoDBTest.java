/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest;

/** Tests the integration between MongoDB and App-schema. */
public class ComplexMongoDBTest extends ComplexMongoDBSupport {

    @Override
    protected String getPathOfMappingsToUse() {
        return "/mappings/stations.xml";
    }
}
