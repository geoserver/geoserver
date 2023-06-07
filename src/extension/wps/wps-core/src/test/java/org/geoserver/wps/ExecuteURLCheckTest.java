/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.geoserver.platform.ServiceException.INVALID_PARAMETER_VALUE;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.apache.commons.io.IOUtils;
import org.geoserver.security.urlchecks.GeoServerURLChecker;
import org.geoserver.security.urlchecks.RegexURLCheck;
import org.geoserver.security.urlchecks.URLCheckDAO;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.w3c.dom.Document;

/** Integration test for Execute referencing remote inputs, with URL checks enabled/disabled. */
public class ExecuteURLCheckTest extends WPSTestSupport {

    private static final boolean debugMode = true;
    private static final String STATES_COLLECTION = "states-FeatureCollection.xml";

    private static WireMockServer service;

    private static String statesGMLURL;

    @BeforeClass
    public static void beforeClass() throws Exception {
        WireMockConfiguration config = wireMockConfig().dynamicPort();
        if (debugMode) config.notifier(new ConsoleNotifier(true));
        service = new WireMockServer(config);
        service.start();

        // remote GML file
        String statesResource = getResourceAsString(STATES_COLLECTION);
        statesGMLURL = service.baseUrl() + "/" + STATES_COLLECTION;
        service.stubFor(
                WireMock.get(urlEqualTo("/" + STATES_COLLECTION))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", MediaType.TEXT_XML_VALUE)
                                        .withBody(statesResource)));
    }

    private static String getResourceAsString(String resource) throws IOException {
        try (InputStream is = ExecuteURLCheckTest.class.getResourceAsStream(resource)) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
    }

    @Before
    public void setup() throws Exception {
        // make sure the GeoserverURLChecker gets initialized, in tests that's lazy load
        assertNotNull(applicationContext.getBean(GeoServerURLChecker.class));
        // start with empty rules
        URLCheckDAO dao = applicationContext.getBean(URLCheckDAO.class);
        // enable url checks
        dao.setEnabled(true);
        dao.saveChecks(Collections.emptyList());
    }

    @Test
    public void testRemoteInputAllowed() throws Exception {
        URLCheckDAO dao = applicationContext.getBean(URLCheckDAO.class);
        dao.save(new RegexURLCheck("pointStyle", "Just the point style", "^" + statesGMLURL + "$"));

        String request = buildGetBoundsRequest();
        Document dom = postAsDOM(root(), request);

        assertXpathEvaluatesTo("0.0 0.0", "//ows:BoundingBox/ows:LowerCorner", dom);
        assertXpathEvaluatesTo("5.0 5.0", "//ows:BoundingBox/ows:UpperCorner", dom);
    }

    @Test
    public void testRemoteInputDisallowed() throws Exception {
        URLCheckDAO dao = applicationContext.getBean(URLCheckDAO.class);
        dao.save(new RegexURLCheck("deny", "Won't match", "^abcd$"));

        String request = buildGetBoundsRequest();
        Document dom = postAsDOM(root(), request);
        String message = checkOws11Exception(dom, INVALID_PARAMETER_VALUE, "features");
        assertThat(message, containsString(statesGMLURL));
    }

    public String buildGetBoundsRequest() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www"
                + ".w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www"
                + ".opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis"
                + ".net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
                + "xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll"
                + ".xsd\">\n"
                + "  <ows:Identifier>gs:Bounds</ows:Identifier>\n"
                + "  <wps:DataInputs>\n"
                + "    <wps:Input>\n"
                + "      <ows:Identifier>features</ows:Identifier>\n"
                + "      <wps:Reference mimeType=\"text/xml; subtype=wfs-collection/1.1\" "
                + "xlink:href=\""
                + statesGMLURL
                + "\" method=\"GET\"/>\n"
                + "    </wps:Input>\n"
                + "  </wps:DataInputs>\n"
                + "  <wps:ResponseForm>\n"
                + "    <wps:RawDataOutput>\n"
                + "      <ows:Identifier>bounds</ows:Identifier>\n"
                + "    </wps:RawDataOutput>\n"
                + "  </wps:ResponseForm>\n"
                + "</wps:Execute>";
    }
}
