/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import java.io.IOException;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.data.test.SystemTestData;

/**
 * Test data for GeoServerImpl trailing slash match, necessary to set the system property before
 * dispatcher is initialized
 */
public class GeoServerImplTrailingSlashTestData extends SystemTestData {

    public GeoServerImplTrailingSlashTestData() throws IOException {
        super();
    }

    @Override
    @SuppressWarnings("PMD.JUnit4TestShouldUseBeforeAnnotation")
    public void setUp() throws Exception {
        System.setProperty(GeoServerInfoImpl.TRAILING_SLASH_MATCH_KEY, "false");
        super.setUp();
    }
}
