/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.junit.Test;

public class WFSXStreamLoaderTest extends WFSTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no test data needed
    }

    @Test
    public void testGmlCreateFromScratch() throws Exception {
        WFSXStreamLoader loader = GeoServerExtensions.bean(WFSXStreamLoader.class);
        WFSInfo wfs = loader.createServiceFromScratch(null);
        assertNotNull(wfs);

        assertTrue(wfs.getGML().containsKey(WFSInfo.Version.V_10));
        assertTrue(wfs.getGML().containsKey(WFSInfo.Version.V_11));
        assertTrue(wfs.getGML().containsKey(WFSInfo.Version.V_20));
    }

    @Test
    public void testLoadVersion() throws Exception {
        XStreamPersisterFactory factory = GeoServerExtensions.bean(XStreamPersisterFactory.class);
        XStreamPersister xp = factory.createXMLPersister();
        WFSXStreamLoader loader = GeoServerExtensions.bean(WFSXStreamLoader.class);
        loader.initXStreamPersister(xp, getGeoServer());
        try (InputStream is = getClass().getResourceAsStream("wfs-test.xml")) {
            xp.load(is, WFSInfo.class);
        }
    }

    @Test
    public void testLoadMinimalConfig() throws Exception {
        XStreamPersisterFactory factory = GeoServerExtensions.bean(XStreamPersisterFactory.class);
        XStreamPersister xp = factory.createXMLPersister();
        WFSXStreamLoader loader = GeoServerExtensions.bean(WFSXStreamLoader.class);
        loader.initXStreamPersister(xp, getGeoServer());
        try (InputStream is = getClass().getResourceAsStream("wfs-minimal.xml")) {
            xp.load(is, WFSInfo.class);
        }
    }
}
