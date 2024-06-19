/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.geoserver.ogcapi.v1.dggs.DGGSTestSupport;
import org.geoserver.web.data.store.dggs.DGGSServiceDescriptionProvider;
import org.junit.Test;

/** Tests DGGSServiceDescriptionProvider and (indirectly) OgcApiServiceDescriptionProvider */
public class DGGSServiceDescriptionProviderTest extends DGGSTestSupport {

    /** basic test of the description* */
    @Test
    public void testProvider() {
        var provider = new DGGSServiceDescriptionProvider(this.getGeoServer());

        var descriptions = provider.getServices(null, null);
        assertEquals(1, descriptions.size());
        assertEquals("DGGS", descriptions.get(0).getServiceType());
        assertTrue(descriptions.get(0).getDescriptionPriority() < 100);
    }

    /** tests the links - esp the actual link when its WS-based of WS-and-layer based* */
    @Test
    public void testLinks() {
        var provider = new DGGSServiceDescriptionProvider(this.getGeoServer());

        var links = provider.getServiceLinks(null, null);
        assertEquals(1, links.size());
        assertEquals("DGGS", links.get(0).getServiceType());
        assertEquals("DGGS", links.get(0).getSpecificServiceType());
        assertEquals("DGGS", links.get(0).getProtocol());
        assertEquals("../ogc/dggs/v1", links.get(0).getLink());

        var ws = getCatalog().getWorkspaceByName("cite");

        links = provider.getServiceLinks(ws, null);
        assertEquals(1, links.size());
        assertEquals("DGGS", links.get(0).getServiceType());
        assertEquals("DGGS", links.get(0).getSpecificServiceType());
        assertEquals("DGGS", links.get(0).getProtocol());
        assertEquals("../cite/ogc/dggs/v1", links.get(0).getLink());

        var layer = getCatalog().getLayerByName("DividedRoutes");

        links = provider.getServiceLinks(ws, layer);
        assertEquals(1, links.size());
        assertEquals("DGGS", links.get(0).getServiceType());
        assertEquals("DGGS", links.get(0).getSpecificServiceType());
        assertEquals("DGGS", links.get(0).getProtocol());
        assertEquals("../cite/DividedRoutes/ogc/dggs/v1", links.get(0).getLink());
    }
}
