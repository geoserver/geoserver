/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.io.InputStream;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.junit.Test;

public class WMSXStreamLoaderTest extends WMSTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no test data needed
    }

    @Test
    public void testLoadWatermark() throws Exception {
        XStreamPersisterFactory factory = GeoServerExtensions.bean(XStreamPersisterFactory.class);
        XStreamPersister xp = factory.createXMLPersister();
        WMSXStreamLoader loader = GeoServerExtensions.bean(WMSXStreamLoader.class);
        loader.initXStreamPersister(xp, getGeoServer());
        try (InputStream is = getClass().getResourceAsStream("wms-test.xml")) {
            xp.load(is, WMSInfo.class);
        }
    }
}
