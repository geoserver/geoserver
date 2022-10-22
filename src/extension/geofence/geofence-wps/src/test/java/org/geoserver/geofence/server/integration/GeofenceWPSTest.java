/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.server.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.geofence.cache.CachedRuleReader;
import org.geoserver.geofence.config.GeoFenceConfigurationManager;
import org.geoserver.geofence.core.model.enums.GrantType;
import org.geoserver.geofence.services.RuleAdminService;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wps.WPSTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.w3c.dom.Document;

public class GeofenceWPSTest extends WPSTestSupport {

    public GeofenceIntegrationTestSupport support;

    private GeoFenceConfigurationManager configManager;
    private RuleAdminService ruleService;

    @Before
    public void setUp() {
        support =
                new GeofenceIntegrationTestSupport(
                        () -> GeoServerSystemTestSupport.applicationContext);
        support.before();

        configManager = applicationContext.getBean(GeoFenceConfigurationManager.class);
        assertNotNull("Bean GeoFenceConfigurationManager not found", configManager);

        ruleService = (RuleAdminService) applicationContext.getBean("ruleAdminService");
        assertNotNull("Bean ruleAdminService not found", ruleService);

        // bottom rule: deny everything if not allowed elsewhere
        if (ruleService.getRuleByPriority(9999) == null)
            support.addRule(GrantType.DENY, null, null, null, null, null, null, null, 9999);
    }

    @After
    public void clearRules() {
        support.after();
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
    }

    // ===== REAL TESTS BELOW ==================================================

    @Test
    public void testDenyAccess() throws Exception {
        Document dom = runBuildingsRequest();
        // print(dom);

        assertEquals("1", xp.evaluate("count(//wps:ProcessFailed)", dom));
        assertEquals("0", xp.evaluate("count(//wps:ProcessSucceded)", dom));
    }

    @Test
    public void testUserAccess() throws Exception {

        support.addRule(
                GrantType.ALLOW,
                "cite",
                null,
                "WPS",
                null,
                "GS:BOUNDS",
                MockData.BUILDINGS.getPrefix(),
                MockData.BUILDINGS.getLocalPart(),
                1);

        Document dom;

        // === no access for not authenticated user
        dom = runBuildingsRequest();
        assertEquals("1", xp.evaluate("count(//wps:ProcessFailed)", dom));

        // === access granted for authenticated cite user
        login("cite", "cite");
        dom = runBuildingsRequest();
        // print(dom);

        assertEquals("0", xp.evaluate("count(//wps:ProcessFailed)", dom));
        assertEquals("1", xp.evaluate("count(//wps:ProcessSucceeded)", dom));
    }

    @Test
    public void testOperationAccess() throws Exception {

        login("cite", "cite");
        Document dom;

        // === no access for not matching processname
        support.addRule(
                GrantType.ALLOW,
                "cite",
                null,
                "WPS",
                null,
                "UNKNOWN_OPERATION",
                MockData.BUILDINGS.getPrefix(),
                MockData.BUILDINGS.getLocalPart(),
                1);

        dom = runBuildingsRequest();
        assertEquals("1", xp.evaluate("count(//wps:ProcessFailed)", dom));

        // === access granted for proper processname
        support.addRule(
                GrantType.ALLOW,
                "cite",
                null,
                "WPS",
                null,
                "GS:BOUNDS",
                MockData.BUILDINGS.getPrefix(),
                MockData.BUILDINGS.getLocalPart(),
                2);
        GeoServerExtensions.bean(CachedRuleReader.class).invalidateAll();

        dom = runBuildingsRequest();
        //        dumpAllRules();
        //        print(dom);

        assertEquals("0", xp.evaluate("count(//wps:ProcessFailed)", dom));
        assertEquals("1", xp.evaluate("count(//wps:ProcessSucceeded)", dom));
    }

    @Test
    public void testChainedExecute() throws Exception {
        login("cite", "cite");
        Document dom;

        // === no access for zero rules
        dom = runChainedRequest();
        assertEquals("1", xp.evaluate("count(//wps:ProcessFailed)", dom));

        // === no access for only external process allowed
        support.addRule(
                GrantType.ALLOW,
                "cite",
                null,
                "WPS",
                null,
                "GS:BOUNDS",
                MockData.BASIC_POLYGONS.getPrefix(),
                MockData.BASIC_POLYGONS.getLocalPart(),
                10);
        GeoServerExtensions.bean(CachedRuleReader.class).invalidateAll();

        dom = runChainedRequest();
        assertEquals("1", xp.evaluate("count(//wps:ProcessFailed)", dom));

        // === ok access for both processes allowed
        support.addRule(
                GrantType.ALLOW,
                "cite",
                null,
                "WPS",
                null,
                "GS:REPROJECT",
                MockData.BASIC_POLYGONS.getPrefix(),
                MockData.BASIC_POLYGONS.getLocalPart(),
                20);
        GeoServerExtensions.bean(CachedRuleReader.class).invalidateAll();

        dom = runChainedRequest();

        assertEquals("0", xp.evaluate("count(//wps:ProcessFailed)", dom));
        assertEquals("1", xp.evaluate("count(//wps:ProcessSucceeded)", dom));
    }

    private Document runBuildingsRequest() throws Exception {
        // @formatter:off
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>gs:Bounds</ows:Identifier>\n"
                        + "  <wps:DataInputs>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>features</ows:Identifier>\n"
                        + "      <wps:Reference mimeType=\"text/xml; subtype=wfs-collection/1.0\" xlink:href=\"http://geoserver/wfs\" method=\"POST\">\n"
                        + "        <wps:Body>\n"
                        + "          <wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\">\n"
                        + "            <wfs:Query typeName=\""
                        + getLayerId(MockData.BUILDINGS)
                        + "\"/>\n"
                        + "          </wfs:GetFeature>\n"
                        + "        </wps:Body>\n"
                        + "      </wps:Reference>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:Output>\n"
                        + "      <ows:Identifier>result</ows:Identifier>\n"
                        + "    </wps:Output>\n"
                        + " </wps:ResponseForm>\n"
                        + "</wps:Execute>";
        // @formatter:on

        Document dom = postAsDOM("wps", xml);
        return dom;
    }

    private Document runChainedRequest() throws Exception {
        // @formatter:off
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>gs:Bounds</ows:Identifier>\n"
                        + "  <wps:DataInputs>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>features</ows:Identifier>\n"
                        + "      <wps:Reference mimeType=\"text/xml; subtype=wfs-collection/1.0\" xlink:href=\"http://geoserver/wps\" method=\"POST\">\n"
                        + "        <wps:Body>\n"
                        + "<wps:Execute version='1.0.0' service='WPS'>"
                        + "  <ows:Identifier>gs:Reproject</ows:Identifier>\n"
                        + "  <wps:DataInputs>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>features</ows:Identifier>\n"
                        + "      <wps:Reference mimeType=\"text/xml; subtype=wfs-collection/1.0\" xlink:href=\"http://geoserver/wfs\" method=\"POST\">\n"
                        + "        <wps:Body>\n"
                        + "          <wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\">\n"
                        + "            <wfs:Query typeName=\""
                        + getLayerId(MockData.BASIC_POLYGONS)
                        + "\"/>\n"
                        + "          </wfs:GetFeature>\n"
                        + "        </wps:Body>\n"
                        + "      </wps:Reference>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>forcedCRS</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:LiteralData>EPSG:4269</wps:LiteralData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:RawDataOutput mimeType=\"text/xml; subtype=wfs-collection/1.0\">\n"
                        + "      <ows:Identifier>result</ows:Identifier>\n"
                        + "    </wps:RawDataOutput>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>"
                        + "        </wps:Body>\n"
                        + "      </wps:Reference>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:Output>\n"
                        + "      <ows:Identifier>result</ows:Identifier>\n"
                        + "    </wps:Output>\n"
                        + " </wps:ResponseForm>\n"
                        + "</wps:Execute>";
        // @formatter:on

        Document dom = postAsDOM("wps", xml);
        return dom;
    }
}
