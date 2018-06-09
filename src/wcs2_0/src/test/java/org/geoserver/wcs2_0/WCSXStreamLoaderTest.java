/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import java.io.InputStream;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs.WCSXStreamLoader;
import org.junit.Test;

public class WCSXStreamLoaderTest extends WCSTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no test data needed
    }

    @Test
    public void testLoadFromXML() throws Exception {
        XStreamPersisterFactory factory = GeoServerExtensions.bean(XStreamPersisterFactory.class);
        XStreamPersister xp = factory.createXMLPersister();
        WCSXStreamLoader loader = GeoServerExtensions.bean(WCSXStreamLoader.class);
        loader.initXStreamPersister(xp, getGeoServer());
        try (InputStream is = getClass().getResourceAsStream("/wcs-test.xml")) {
            xp.load(is, WCSInfo.class);
        }
    }
}
