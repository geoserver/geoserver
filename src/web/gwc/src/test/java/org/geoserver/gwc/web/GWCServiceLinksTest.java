/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.wmts.WMTSInfo;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.ServiceDescription;
import org.geoserver.web.ServiceLinkDescription;
import org.junit.Test;

public class GWCServiceLinksTest extends GeoServerWicketTestSupport {

    @Test
    public void testCapabilitiesLinks() {
        GeoServerHomePage page = new GeoServerHomePage();

        tester.startPage(page);
        tester.assertRenderedPage(GeoServerHomePage.class);

        Page lastPage = tester.getLastRenderedPage();
        final List<String> services = new ArrayList<>();
        lastPage.visitChildren(ExternalLink.class, (component, visit) -> {
            String url = (String) component.getDefaultModelObject();
            if (url != null) {
                if (url.startsWith("../gwc/service/")) {
                    int idx = url.indexOf("?");
                    String service;
                    if (idx > 0) {
                        service = url.substring("./gwc/service/".length() + 1, idx);
                    } else {
                        service = url.substring("./gwc/service/".length() + 1);
                    }
                    if (service != null) {
                        services.add(service);
                    }
                } else if (url.contains("GetCapabilities")) {
                    Map<String, Object> params = KvpUtils.parseQueryString(url);
                    String service = (String) params.get("service");
                    if (service != null) {
                        services.add(service);
                    }
                }
            }
        });

        // GEOS-5886
        assertFalse(services.contains("gwc"));
        // these come from the custom provider
        assertTrue(services.contains("wmts"));
        assertTrue(services.contains("wms"));
        assertTrue(services.contains("tms/1.0.0"));
    }

    @Test
    public void serviceDescriptorAndLinks() {
        GWCServiceDescriptionProvider provider = GeoServerExtensions.bean(GWCServiceDescriptionProvider.class);

        List<ServiceDescription> services = provider.getServices(null, null);
        List<ServiceLinkDescription> links = provider.getServiceLinks(null, null);

        assertEquals(1, services.size());
        ServiceDescription wfs = services.get(0);

        for (ServiceLinkDescription link : links) {
            // All links should match wfs service description
            assertEquals("crosslink", wfs.getServiceType(), link.getServiceType());

            if (link.getProtocol().equals("wms")) {
                assertTrue("version", link.getLink().contains("&version="));
                assertTrue("service", link.getLink().contains("&service=WMS"));
            } else if (link.getProtocol().equals("wmts")) {
                assertTrue("version", link.getLink().contains("&version="));
                assertTrue("service", link.getLink().contains("&service=WMTS"));
            }
        }
    }

    @Test
    public void availableServiceCheck() {
        GWCServiceDescriptionProvider provider = GeoServerExtensions.bean(GWCServiceDescriptionProvider.class);
        Catalog catalog = getCatalog();
        GeoServer geoServer = getGeoServer();
        GeoServerInfo global = geoServer.getGlobal();
        WMTSInfo wmts = geoServer.getService(WMTSInfo.class);
        GWC gwc = GWC.get();

        WorkspaceInfo cite = catalog.getWorkspaceByName("cite");
        LayerInfo buildings = catalog.getLayerByName("Buildings");
        String tileLayerName = buildings.getResource().prefixedName();
        GeoServerTileLayer tileLayer = (GeoServerTileLayer) gwc.getTileLayerByName(tileLayerName);
        try {
            // check enable/disable global services
            List<ServiceDescription> services;
            services = provider.getServices(null, null).stream()
                    .filter(s -> s.isAvailable())
                    .collect(Collectors.toList());
            assertEquals(1, services.size());
            services = provider.getServices(cite, null).stream()
                    .filter(s -> s.isAvailable())
                    .collect(Collectors.toList());
            assertEquals(1, services.size());
            services = provider.getServices(cite, buildings).stream()
                    .filter(s -> s.isAvailable())
                    .collect(Collectors.toList());
            assertEquals(1, services.size());

            global.setGlobalServices(false);
            geoServer.save(global);
            services = provider.getServices(null, null).stream()
                    .filter(s -> s.isAvailable())
                    .collect(Collectors.toList());
            assertEquals(0, services.size());
            services = provider.getServices(cite, null).stream()
                    .filter(s -> s.isAvailable())
                    .collect(Collectors.toList());
            assertEquals(1, services.size());
            services = provider.getServices(cite, buildings).stream()
                    .filter(s -> s.isAvailable())
                    .collect(Collectors.toList());
            assertEquals(1, services.size());

            // check enable/disable WFSInfo
            global.setGlobalServices(true);
            geoServer.save(global);
            services = provider.getServices(null, null).stream()
                    .filter(s -> s.isAvailable())
                    .collect(Collectors.toList());
            assertEquals(1, services.size());
            services = provider.getServices(cite, null).stream()
                    .filter(s -> s.isAvailable())
                    .collect(Collectors.toList());
            assertEquals(1, services.size());
            services = provider.getServices(cite, buildings).stream()
                    .filter(s -> s.isAvailable())
                    .collect(Collectors.toList());
            assertEquals(1, services.size());

            wmts.setEnabled(false);
            geoServer.save(wmts);
            services = provider.getServices(null, null).stream()
                    .filter(s -> s.isAvailable())
                    .collect(Collectors.toList());
            assertEquals(0, services.size());
            services = provider.getServices(cite, null).stream()
                    .filter(s -> s.isAvailable())
                    .collect(Collectors.toList());
            assertEquals(0, services.size());
            services = provider.getServices(cite, buildings).stream()
                    .filter(s -> s.isAvailable())
                    .collect(Collectors.toList());
            assertEquals(0, services.size());
            wmts.setEnabled(true);
            geoServer.save(wmts);

            // checks based on tile layer availability
            assertTrue("has tilt layer", gwc.hasTileLayer(buildings));
            services = provider.getServices(cite, buildings).stream()
                    .filter(s -> s.isAvailable())
                    .collect(Collectors.toList());
            assertEquals(1, services.size());

            gwc.removeTileLayers(Arrays.asList("cite:Buildings"));
            assertFalse("no tilelayer", gwc.hasTileLayer(buildings));
            services = provider.getServices(cite, buildings).stream()
                    .filter(s -> s.isAvailable())
                    .collect(Collectors.toList());
            assertEquals(0, services.size());

        } finally {
            global.setGlobalServices(true);
            geoServer.save(global);
            wmts.setEnabled(true);
            geoServer.save(wmts);

            if (!gwc.tileLayerExists(tileLayerName)) gwc.add(tileLayer);
        }
    }
}
