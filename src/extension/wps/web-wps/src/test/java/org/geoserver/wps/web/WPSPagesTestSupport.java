/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.GeoServerWicketTestSupport;

public abstract class WPSPagesTestSupport extends GeoServerWicketTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no data needed
        testData.setUpSecurity();
    }
}
