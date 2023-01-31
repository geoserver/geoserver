/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.web.ServiceDescription;
import org.geoserver.web.ServiceLinkDescription;
import org.geoserver.wfs.WFSResourceVoter;
import org.junit.Test;

public class WFSServiceDescriptionProviderTest extends GeoServerSystemTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpDefault();
        testData.setUpDefaultRasterLayers();
    }

    @Test
    public void serviceDescriptorAndLinks() {
        WFSServiceDescriptionProvider provider =
                GeoServerExtensions.bean(WFSServiceDescriptionProvider.class);

        List<ServiceDescription> services = provider.getServices(null, null);
        List<ServiceLinkDescription> links = provider.getServiceLinks(null, null);

        assertEquals(1, services.size());
        ServiceDescription wfs = services.get(0);

        for (ServiceLinkDescription link : links) {
            // All links should match wfs service description
            assertEquals("crosslink", wfs.getServiceType(), link.getServiceType());

            if (link.getVersion().getMajor().equals(2)) {
                assertTrue("acceptversions", link.getLink().contains("&acceptversions="));
            } else {
                assertTrue("version", link.getLink().contains("&version="));
            }
        }
    }

    @Test
    public void ignoreCoverage() {
        WFSServiceDescriptionProvider provider =
                GeoServerExtensions.bean(WFSServiceDescriptionProvider.class);
        Catalog catalog = getCatalog();
        WorkspaceInfo gs = catalog.getWorkspaceByName("gs");
        LayerInfo world = catalog.getLayerByName("World");

        WFSResourceVoter voter = new WFSResourceVoter();
        assertTrue(voter.hideService("WFS", world.getResource()));

        List<ServiceDescription> services = provider.getServices(gs, world);
        assertEquals(1, services.size());
    }
}
