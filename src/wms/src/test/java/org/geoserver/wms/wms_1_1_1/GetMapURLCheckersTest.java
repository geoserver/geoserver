/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.security.urlchecks.GeoServerURLChecker;
import org.geoserver.security.urlchecks.RegexURLCheck;
import org.geoserver.security.urlchecks.StyleURLChecker;
import org.geoserver.security.urlchecks.URLCheckDAO;
import org.geoserver.wms.WMSTestSupport;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * Tests dynamic remote references in WMS GetMap requests:
 *
 * <ul>
 *   <li>SLD retrieval for WMS 1.1.1, using Wiremock to simulate a remote server
 *   <li>Dynamic WFS source
 *   <li>Local and remote icons
 * </ul>
 */
public class GetMapURLCheckersTest extends WMSTestSupport {

    private static final boolean debugMode = false;

    private static WireMockServer service;

    private static String bridgesStyleURL;
    private static String burgStyle;

    @BeforeClass
    public static void beforeClass() throws Exception {
        // pick files from the 1.1.1 directory
        WireMockConfiguration config = wireMockConfig().dynamicPort();

        if (debugMode) config.notifier(new ConsoleNotifier(true));
        service = new WireMockServer(config);
        service.start();

        // remote style
        String bridgesStyleBody = getResourceAsString("bridges.sld");
        bridgesStyleURL = "http://localhost:" + service.port() + "/styles/bridges.sld";
        service.stubFor(
                WireMock.get(urlEqualTo("/styles/bridges.sld"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", MediaType.TEXT_XML_VALUE)
                                        .withBody(bridgesStyleBody)));

        // remote icon test
        burgStyle =
                getResourceAsString("burg_remote.sld")
                        .replace("${styleBase}", "http://localhost:" + service.port() + "/styles");
        String burgSvg = getResourceAsString("burg02.svg");
        service.stubFor(
                WireMock.get(urlEqualTo("/styles/burg02.svg"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", MediaType.TEXT_XML_VALUE)
                                        .withBody(burgSvg)));

        // remote WFS
        String capabilities = getWFSResource("capabilities.xml");
        service.stubFor(
                WireMock.get(urlEqualTo("/wfs?REQUEST=GetCapabilities&SERVICE=WFS"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", MediaType.TEXT_XML_VALUE)
                                        .withBody(capabilities)));
        String describe = getWFSResource("describePoi.xml");
        Map<String, String> namespaces = Map.of("wfs", "http://www.opengis.net/wfs");
        service.stubFor(
                WireMock.post("/wfs")
                        .withRequestBody(
                                WireMock.matchingXPath(
                                        "/wfs:DescribeFeatureType[wfs:TypeName='tiger:poi']",
                                        namespaces))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", MediaType.TEXT_XML_VALUE)
                                        .withBody(describe)));

        String getFeature = getWFSResource("getFeature.xml");
        service.stubFor(
                WireMock.post("/wfs")
                        .withRequestBody(
                                WireMock.matchingXPath(
                                        "/wfs:GetFeature[wfs:Query[@typeName='tiger:poi']]",
                                        namespaces))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", MediaType.TEXT_XML_VALUE)
                                        .withBody(getFeature)));
    }

    private static String getWFSResource(String resourceName) throws IOException {
        return getResourceAsString("poiwfs/" + resourceName)
                .replace("${wfsBase}", "http://localhost:" + service.port() + "/wfs");
    }

    private static String getResourceAsString(String resource) throws IOException {
        return getResourceAsString(GetMapURLCheckersTest.class, resource);
    }

    private static String getResourceAsString(Class loader, String resource) throws IOException {
        try (InputStream is = loader.getResourceAsStream(resource)) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
    }

    @AfterClass
    public static void afterClass() throws Exception {
        service.shutdown();
    }

    @Before
    public void setup() throws Exception {
        // make sure the GeoserverURLChecker gets initialized, in tests that's lazy load
        assertNotNull(applicationContext.getBean(GeoServerURLChecker.class));
        assertNotNull(applicationContext.getBean(StyleURLChecker.class));
        // start with empty rules
        URLCheckDAO dao = applicationContext.getBean(URLCheckDAO.class);
        dao.saveChecks(Collections.emptyList());
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // pick style with relative icon reference from gs-main tests
        testData.addStyle("burg", "burg.sld", GeoServer.class, getCatalog());
        try (InputStream is = GeoServer.class.getResourceAsStream("burg02.svg")) {
            testData.copyTo(is, "styles/burg02.svg");
        }

        // this file is intentionally NOT in the styles directory
        try (InputStream is = getClass().getResourceAsStream("burg03.svg")) {
            testData.copyTo(is, "burg03.svg");
        }

        // create the absolute file URL for the data directory root
        String base = testData.getDataDirectoryRoot().getAbsolutePath().replace('\\', '/');
        base = "file://" + (base.startsWith("/") ? "" : "/") + base + "/";

        // create the style with an absolute file URL to icon outside styles directory
        testData.addStyle("burg_query", "burg_query.sld", getClass(), getCatalog());
        String sld1 = IOUtils.toString(getClass().getResource("burg_query.sld"), UTF_8);
        int index1 = sld1.indexOf("burg03");
        sld1 = sld1.substring(0, index1) + base + sld1.substring(index1);
        ByteArrayInputStream bytes1 = new ByteArrayInputStream(sld1.getBytes(UTF_8));
        testData.copyTo(bytes1, "styles/burg_query.sld");

        // create the style with an absolute file URL to icon outside styles directory
        testData.addStyle("burg_fragment", "burg_fragment.sld", getClass(), getCatalog());
        String sld2 = IOUtils.toString(getClass().getResource("burg_fragment.sld"), UTF_8);
        int index2 = sld2.indexOf("burg03");
        sld2 = sld2.substring(0, index2) + base + sld2.substring(index2);
        ByteArrayInputStream bytes2 = new ByteArrayInputStream(sld2.getBytes(UTF_8));
        testData.copyTo(bytes2, "styles/burg_fragment.sld");
    }

    @Test
    public void testRemoteStyleAllowed() throws Exception {
        URLCheckDAO dao = applicationContext.getBean(URLCheckDAO.class);
        dao.save(
                new RegexURLCheck(
                        "pointStyle", "Just the point style", "^" + bridgesStyleURL + "$"));

        // base request, no layers, no library mode
        String base =
                "wms?service=WMS&version=1.1.1&request=GetMap&bbox=-180,-90,180,90&width=256&height=256&srs=EPSG:4326&format=image/png";
        MockHttpServletResponse response = getAsServletResponse(base + "&sld=" + bridgesStyleURL);
        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
    }

    @Test
    public void testRemoteStyleDenied() throws Exception {
        URLCheckDAO dao = applicationContext.getBean(URLCheckDAO.class);
        dao.save(new RegexURLCheck("deny", "Won't match anything useful", "^abcd$"));

        // base request, no layers, no library mode
        String base =
                "wms?service=WMS&version=1.1.1&request=GetMap&bbox=-180,-90,180,90&width=256&height=256&srs=EPSG:4326&format=image/png";
        Document dom = getAsDOM(base + "&sld=" + bridgesStyleURL);
        String message = checkLegacyException(dom, "InvalidParameterValue", "sld");
        assertThat(message, containsString(bridgesStyleURL));
    }

    @Test
    public void testRemoteWFSAllowed() throws Exception {
        URLCheckDAO dao = applicationContext.getBean(URLCheckDAO.class);
        String localWfs = "http://localhost:" + service.port() + "/wfs";
        dao.save(new RegexURLCheck("localWFS", "The local wiremock WFS", "^" + localWfs + ".*$"));

        String request = getMapRemoteWFS(localWfs);
        BufferedImage image = getAsImage(request, "image/png");
        assertNotBlank("testRemoteWFSAllowed", image, Color.WHITE);
    }

    @Test
    public void testRemoveWFSDisallowed() throws Exception {
        URLCheckDAO dao = applicationContext.getBean(URLCheckDAO.class);
        String localWfs = "http://localhost:" + service.port() + "/wfs";
        dao.save(new RegexURLCheck("deny", "Won't match anything useful", "^abcd$"));

        String request = getMapRemoteWFS(localWfs);
        Document dom = getAsDOM(request);
        String message = checkLegacyException(dom, "InvalidParameterValue", "REMOTE_OWS_URL");
        assertThat(message, containsString(localWfs));
    }

    private static String getMapRemoteWFS(String localWfs) {
        return "wms?service=WMS&version=1.1.1&request=GetMap&bbox=-74.044847,40.694924,-73.963094,40.726836"
                + "&width=256&height=256&srs=EPSG:4326&format=image/png"
                + "&layers=tiger:poi&styles=point"
                + "&REMOTE_OWS_URL="
                + localWfs
                + "&REMOTE_OWS_TYPE=WFS";
    }

    @Test
    public void testRemoteIconAllowed() throws Exception {
        URLCheckDAO dao = applicationContext.getBean(URLCheckDAO.class);
        dao.save(
                new RegexURLCheck(
                        "icons",
                        "Any SVG icon",
                        "^http://localhost:" + service.port() + "/styles/.*\\.svg$"));

        // base request, no layers, no library mode
        String base =
                "wms?service=WMS&version=1.1.1&request=GetMap&bbox=-180,-90,180,90&width=256&height=256&srs=EPSG:4326&format=image/png";
        BufferedImage image =
                getAsImage(base + "&sld_body=" + ResponseUtils.urlEncode(burgStyle), "image/png");
        // has painted the red flag
        assertPixel(image, 130, 121, Color.RED);
    }

    @Test
    public void testRemoteIconNotAllowed() throws Exception {
        URLCheckDAO dao = applicationContext.getBean(URLCheckDAO.class);
        dao.save(new RegexURLCheck("noIcons", "We like it gray", "^abcd$"));

        // base request, no layers, no library mode
        String base =
                "wms?service=WMS&version=1.1.1&request=GetMap&bbox=-180,-90,180,90&width=256&height=256&srs=EPSG:4326&format=image/png";
        BufferedImage image =
                getAsImage(base + "&sld_body=" + ResponseUtils.urlEncode(burgStyle), "image/png");
        // has fallen back to the square gray default icon
        assertPixel(image, 130, 121, Color.GRAY);
    }

    @Test
    public void testLocalReference() throws Exception {
        // still disable everything
        URLCheckDAO dao = applicationContext.getBean(URLCheckDAO.class);
        dao.save(new RegexURLCheck("noIcons", "We like it gray", "^abcd$"));

        // simple GetMap with local style, should work regardless of the URL checks
        String base =
                "wms?service=WMS&version=1.1.1&request=GetMap&bbox=-180,-90,180,90&width=256&height=256&srs=EPSG:4326&format=image/png&layers=cite:Bridges&styles=burg";
        BufferedImage image = getAsImage(base, "image/png");
        // should have used the red flag icon, the relative reference is allowed
        assertPixel(image, 130, 121, Color.RED);
    }

    @Test
    public void testLocalReferenceWithBadQuery() throws Exception {
        // simple GetMap with local style, path traversal in a bad URI query should not work
        String base =
                "wms?service=WMS&version=1.1.1&request=GetMap&bbox=-180,-90,180,90&width=256&height=256&srs=EPSG:4326&format=image/png&layers=cite:Bridges&styles=burg_query";
        BufferedImage image = getAsImage(base, "image/png");
        // has fallen back to the square gray default icon
        assertPixel(image, 130, 121, Color.GRAY);
    }

    @Test
    public void testLocalReferenceWithBadFragment() throws Exception {
        // simple GetMap with local style, path traversal in a bad URI fragment should not work
        String base =
                "wms?service=WMS&version=1.1.1&request=GetMap&bbox=-180,-90,180,90&width=256&height=256&srs=EPSG:4326&format=image/png&layers=cite:Bridges&styles=burg_fragment";
        BufferedImage image = getAsImage(base, "image/png");
        // has fallen back to the square gray default icon
        assertPixel(image, 130, 121, Color.GRAY);
    }
}
