/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wcs.test;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wcs.WCSInfo;

/**
 * Base support class for wcs tests.
 * 
 * @author Andrea Aime, TOPP
 * 
 */
public abstract class CoverageTestSupport extends GeoServerSystemTestSupport {
    protected static final String BASEPATH = "wcs";

    /**
     * @return The global wfs instance from the application context.
     */
    protected WCSInfo getWCS() {
        return getGeoServer().getService(WCSInfo.class);
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
        testData.setUpWcs10RasterLayers();
    }
}
