package org.geoserver.opensearch.eo;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public class OSEOFactoryExtensionTest extends GeoServerSystemTestSupport {
    @Test
    public void testDirectCreate() throws Exception {
        OSEOFactoryExtension oseo = new OSEOFactoryExtension();
        OSEOInfo oseoInfo = oseo.create(OSEOInfo.class);
        assertTrue(oseoInfo instanceof OSEOInfoImpl);
    }

    @Test
    public void testCreateViaFactory() throws Exception {
        GeoServer geoServer = getGeoServer();
        WorkspaceInfo ws = geoServer.getCatalog().getWorkspaceByName("sf");
        OSEOInfo oseoInfo = geoServer.getFactory().create(OSEOInfo.class);
        assertNotNull(oseoInfo);
    }
}
