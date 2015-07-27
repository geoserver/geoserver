/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.geoserver.platform.GeoServerResourceLoader;
import org.junit.Test;

public class WFSXStreamLoaderTest {

    @Test
    public void testGmlCreateFromScratch() throws Exception {
        WFSXStreamLoader loader = new WFSXStreamLoader(new GeoServerResourceLoader());
        WFSInfo wfs = loader.createServiceFromScratch(null);
        assertNotNull(wfs);

        assertTrue(wfs.getGML().containsKey(WFSInfo.Version.V_10));
        assertTrue(wfs.getGML().containsKey(WFSInfo.Version.V_11));
        assertTrue(wfs.getGML().containsKey(WFSInfo.Version.V_20));
    }

}
