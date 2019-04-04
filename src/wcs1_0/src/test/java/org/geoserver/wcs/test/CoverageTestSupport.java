/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.test;

import javax.xml.namespace.QName;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.TestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wcs.WCSInfo;

/**
 * Base support class for wcs tests.
 *
 * @author Andrea Aime, TOPP
 */
public abstract class CoverageTestSupport extends GeoServerSystemTestSupport {
    protected static final String BASEPATH = "wcs";

    protected static final boolean SpatioTemporalRasterTests = false;

    public static QName WATTEMP = new QName(MockData.WCS_URI, "watertemp", MockData.WCS_PREFIX);

    protected static QName TIMERANGES =
            new QName(MockData.SF_URI, "timeranges", MockData.SF_PREFIX);

    /** @return The global wcs instance from the application context. */
    protected WCSInfo getWCS() {
        WCSInfo wcs = getGeoServer().getService(WCSInfo.class);
        return wcs;
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpSecurity();
        testData.setUpDefaultRasterLayers();
        if (SpatioTemporalRasterTests) {
            testData.setUpRasterLayer(WATTEMP, "watertemp.zip", null, null, TestData.class);
            //            dataDirectory.addCoverage(WATTEMP,
            // TestData.class.getResource("watertemp.zip"),
            //                    null, styleName);
        }
        testData.setUpWcs10RasterLayers();
        // dataDirectory.addWcs10Coverages();
    }

    //    @Override
    //    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
    //        dataDirectory.addWellKnownCoverageTypes();
    //        URL style = MockData.class.getResource("raster.sld");
    //        String styleName = "raster";
    //        dataDirectory.addStyle(styleName, style);
    //        if(SpatioTemporalRasterTests)
    //        	dataDirectory.addCoverage(WATTEMP, TestData.class.getResource("watertemp.zip"),
    //	                null, styleName);
    //        dataDirectory.addWcs10Coverages();
    //    }
}
