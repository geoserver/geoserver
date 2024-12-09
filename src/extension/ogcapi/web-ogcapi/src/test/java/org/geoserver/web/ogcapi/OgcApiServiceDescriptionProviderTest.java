/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.ogcapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.web.ogcapi.provider.TestCaseInfo;
import org.geoserver.web.ogcapi.provider.TestCaseInfoImpl;
import org.geoserver.web.ogcapi.provider.TestCaseOgcApiServiceDescriptionProvider;
import org.geoserver.web.ogcapi.provider.TestCaseService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** ALSO SEE DGGSServiceDescriptionProviderTest for more specific tests. */
public class OgcApiServiceDescriptionProviderTest extends GeoServerSystemTestSupport {

    /** Tests really basic info in the object. */
    @Test
    public void testBasic() {
        var provider = new TestCaseOgcApiServiceDescriptionProvider(this.getGeoServer());
        assertEquals("TestCaseServiceType", provider.getServiceType());
        assertEquals("OGCAPI-TestCase", provider.getServiceName());
        assertEquals("TestCase", provider.getSpecificServiceType());

        assertEquals(TestCaseInfo.class, provider.getInfoClass());
        assertEquals(TestCaseService.class, provider.getServiceClass());
    }

    @Test
    public void testURLMangler() {
        var provider = new TestCaseOgcApiServiceDescriptionProvider(this.getGeoServer());
        var link = "../ogc/features/v1";

        var mangled = provider.ogcApiCustomCapabilitiesLinkMangler(link, null, null);
        assertEquals(link, mangled); // no change

        var wsInfo = getCatalog().getWorkspaceByName("cite");

        mangled = provider.ogcApiCustomCapabilitiesLinkMangler(link, wsInfo, null);
        assertEquals("../cite/ogc/features/v1", mangled);

        var layerInfo = getCatalog().getLayerByName("DividedRoutes");

        mangled = provider.ogcApiCustomCapabilitiesLinkMangler(link, wsInfo, layerInfo);
        assertEquals("../cite/DividedRoutes/ogc/features/v1", mangled);
    }

    @Before
    public void before() {
        GeoServerExtensionsHelper.singleton("_TestCaseInfoImpl", new TestCaseInfoImpl());
    }

    @After
    public void after() {
        GeoServerExtensionsHelper.clear();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addService(TestCaseInfoImpl.class, null, this.getGeoServer());
    }

    /** basic test of the provider* */
    @Test
    public void testProvider() {
        var provider = new TestCaseOgcApiServiceDescriptionProvider(this.getGeoServer());

        var descriptions = provider.getServices(null, null);
        assertEquals(1, descriptions.size());
        assertEquals("TestCaseServiceType", descriptions.get(0).getServiceType());
        assertTrue(descriptions.get(0).getDescriptionPriority() < 100);
        assertEquals("OGCAPI-TestCase", descriptions.get(0).getDescription().toString());
    }

    /** tests the links - esp the actual link when its WS-based of WS-and-layer based* */
    @Test
    public void testLinks() {
        var provider = new TestCaseOgcApiServiceDescriptionProvider(this.getGeoServer());

        var links = provider.getServiceLinks(null, null);
        assertEquals(1, links.size());
        assertEquals("TestCaseServiceType", links.get(0).getServiceType());
        assertEquals("TestCase", links.get(0).getSpecificServiceType());
        assertEquals("OGCAPI-TestCase", links.get(0).getProtocol());
        assertEquals("../ogc/TestCaseService/v1", links.get(0).getLink());

        var ws = getCatalog().getWorkspaceByName("cite");

        links = provider.getServiceLinks(ws, null);
        assertEquals(1, links.size());
        assertEquals("TestCaseServiceType", links.get(0).getServiceType());
        assertEquals("TestCase", links.get(0).getSpecificServiceType());
        assertEquals("OGCAPI-TestCase", links.get(0).getProtocol());
        assertEquals("../cite/ogc/TestCaseService/v1", links.get(0).getLink());

        var layer = getCatalog().getLayerByName("DividedRoutes");

        links = provider.getServiceLinks(ws, layer);
        assertEquals(1, links.size());
        assertEquals("TestCaseServiceType", links.get(0).getServiceType());
        assertEquals("TestCase", links.get(0).getSpecificServiceType());
        assertEquals("OGCAPI-TestCase", links.get(0).getProtocol());
        assertEquals("../cite/DividedRoutes/ogc/TestCaseService/v1", links.get(0).getLink());
    }
}
