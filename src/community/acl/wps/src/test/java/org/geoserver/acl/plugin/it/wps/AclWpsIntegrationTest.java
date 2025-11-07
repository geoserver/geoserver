/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license
 */

package org.geoserver.acl.plugin.it.wps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.geoserver.acl.domain.rules.GrantType;
import org.geoserver.acl.domain.rules.RuleAdminService;
import org.geoserver.acl.plugin.it.support.AclIntegrationTestSupport;
import org.geoserver.acl.plugin.wps.AclWPSHelperImpl;
import org.geoserver.acl.plugin.wps.WPSProcessListener;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wps.WPSTestSupport;
import org.geoserver.wps.resource.WPSResourceManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.w3c.dom.Document;

public class AclWpsIntegrationTest extends WPSTestSupport {

    public AclIntegrationTestSupport support;
    private RuleAdminService ruleService;

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath*:/applicationContext-test.xml");
    }

    @Before
    public void setUp() {
        support = new AclIntegrationTestSupport(() -> GeoServerSystemTestSupport.applicationContext);
        support.before();

        ruleService = applicationContext.getBean(RuleAdminService.class);
        assertNotNull("Bean RuleAdminService not found", ruleService);
        assertNotNull(applicationContext.getBean(WPSResourceManager.class));
        assertNotNull(applicationContext.getBean(WPSProcessListener.class));
        assertNotNull(applicationContext.getBean(AclWPSHelperImpl.class));

        // bottom rule: deny everything if not allowed elsewhere
        support.addRule(9999, GrantType.DENY, null, null, null, null, null, null, null);
    }

    @After
    public void clearRules() {
        logout();
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
    public void testUserAccessWithProcessNameAcccesRule() throws Exception {

        support.addRule(
                1,
                GrantType.ALLOW,
                "cite",
                null,
                "WPS",
                null,
                "GS:BOUNDS",
                MockData.BUILDINGS.getPrefix(),
                MockData.BUILDINGS.getLocalPart());

        // === no access for not authenticated user
        Document dom = runBuildingsRequest();
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
                1,
                GrantType.ALLOW,
                "cite",
                null,
                "WPS",
                null,
                "UNKNOWN_OPERATION",
                MockData.BUILDINGS.getPrefix(),
                MockData.BUILDINGS.getLocalPart());

        dom = runBuildingsRequest();
        assertEquals("1", xp.evaluate("count(//wps:ProcessFailed)", dom));

        // === access granted for proper processname
        support.addRule(
                2,
                GrantType.ALLOW,
                "cite",
                null,
                "WPS",
                null,
                "GS:BOUNDS",
                MockData.BUILDINGS.getPrefix(),
                MockData.BUILDINGS.getLocalPart());
        // GeoServerExtensions.bean(CachedRuleReader.class).invalidateAll();

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
                10,
                GrantType.ALLOW,
                "cite",
                null,
                "WPS",
                null,
                "GS:BOUNDS",
                MockData.BASIC_POLYGONS.getPrefix(),
                MockData.BASIC_POLYGONS.getLocalPart());
        // GeoServerExtensions.bean(CachedRuleReader.class).invalidateAll();

        dom = runChainedRequest();
        assertEquals("1", xp.evaluate("count(//wps:ProcessFailed)", dom));

        // === ok access for both processes allowed
        support.addRule(
                20,
                GrantType.ALLOW,
                "cite",
                null,
                "WPS",
                null,
                "GS:REPROJECT",
                MockData.BASIC_POLYGONS.getPrefix(),
                MockData.BASIC_POLYGONS.getLocalPart());
        // GeoServerExtensions.bean(CachedRuleReader.class).invalidateAll();

        dom = runChainedRequest();

        assertEquals("0", xp.evaluate("count(//wps:ProcessFailed)", dom));
        assertEquals("1", xp.evaluate("count(//wps:ProcessSucceeded)", dom));
    }

    private Document runBuildingsRequest() throws Exception {
        String xml =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <wps:Execute version="1.0.0" service="WPS" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xmlns="http://www.opengis.net/wps/1.0.0" xmlns:wfs="http://www.opengis.net/wfs" xmlns:wps="http://www.opengis.net/wps/1.0.0"
                     xmlns:ows="http://www.opengis.net/ows/1.1" xmlns:gml="http://www.opengis.net/gml" xmlns:ogc="http://www.opengis.net/ogc"
                     xmlns:wcs="http://www.opengis.net/wcs/1.1.1" xmlns:xlink="http://www.w3.org/1999/xlink"
                     xsi:schemaLocation="http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd">

                  <ows:Identifier>gs:Bounds</ows:Identifier>
                  <wps:DataInputs>
                    <wps:Input>
                      <ows:Identifier>features</ows:Identifier>
                      <wps:Reference mimeType="text/xml; subtype=wfs-collection/1.0" xlink:href="http://geoserver/wfs" method="POST">
                        <wps:Body>
                          <wfs:GetFeature service="WFS" version="1.0.0" outputFormat="GML2">
                            <wfs:Query typeName="%s"/>
                          </wfs:GetFeature>
                        </wps:Body>
                      </wps:Reference>
                    </wps:Input>
                  </wps:DataInputs>
                  <wps:ResponseForm>
                    <wps:Output>
                      <ows:Identifier>result</ows:Identifier>
                    </wps:Output>
                  </wps:ResponseForm>
                </wps:Execute>"""
                        .formatted(getLayerId(MockData.BUILDINGS));
        Document dom = postAsDOM("wps", xml);
        return dom;
    }

    private Document runChainedRequest() throws Exception {
        String xml =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <wps:Execute version="1.0.0" service="WPS" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xmlns="http://www.opengis.net/wps/1.0.0" xmlns:wfs="http://www.opengis.net/wfs" xmlns:wps="http://www.opengis.net/wps/1.0.0"
                     xmlns:ows="http://www.opengis.net/ows/1.1" xmlns:gml="http://www.opengis.net/gml" xmlns:ogc="http://www.opengis.net/ogc"
                     xmlns:wcs="http://www.opengis.net/wcs/1.1.1" xmlns:xlink="http://www.w3.org/1999/xlink"
                     xsi:schemaLocation="http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd">

                  <ows:Identifier>gs:Bounds</ows:Identifier>
                  <wps:DataInputs>
                    <wps:Input>
                      <ows:Identifier>features</ows:Identifier>
                      <wps:Reference mimeType="text/xml; subtype=wfs-collection/1.0" xlink:href="http://geoserver/wps" method="POST">
                        <wps:Body>
                          <wps:Execute version="1.0.0" service="WPS">
                            <ows:Identifier>gs:Reproject</ows:Identifier>
                            <wps:DataInputs>
                              <wps:Input>
                                <ows:Identifier>features</ows:Identifier>
                                <wps:Reference mimeType="text/xml; subtype=wfs-collection/1.0" xlink:href="http://geoserver/wfs" method="POST">
                                  <wps:Body>
                                    <wfs:GetFeature service="WFS" version="1.0.0" outputFormat="GML2">
                                      <wfs:Query typeName="%s"/>
                                    </wfs:GetFeature>
                                  </wps:Body>
                                </wps:Reference>
                              </wps:Input>
                              <wps:Input>
                                <ows:Identifier>forcedCRS</ows:Identifier>
                                <wps:Data>
                                  <wps:LiteralData>EPSG:4269</wps:LiteralData>
                                </wps:Data>
                              </wps:Input>
                            </wps:DataInputs>
                            <wps:ResponseForm>
                              <wps:RawDataOutput mimeType="text/xml; subtype=wfs-collection/1.0">
                                <ows:Identifier>result</ows:Identifier>
                              </wps:RawDataOutput>
                            </wps:ResponseForm>
                          </wps:Execute>
                        </wps:Body>
                      </wps:Reference>
                    </wps:Input>
                  </wps:DataInputs>
                  <wps:ResponseForm>
                    <wps:Output>
                      <ows:Identifier>result</ows:Identifier>
                    </wps:Output>
                  </wps:ResponseForm>
                </wps:Execute>"""
                        .formatted(getLayerId(MockData.BASIC_POLYGONS));

        Document dom = postAsDOM("wps", xml);
        return dom;
    }
}
