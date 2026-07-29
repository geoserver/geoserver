/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.security.urlchecks.GeoServerURLChecker;
import org.geoserver.security.urlchecks.RegexURLCheck;
import org.geoserver.security.urlchecks.URLCheckDAO;
import org.geoserver.test.http.MockHttpClient;
import org.geoserver.test.http.MockHttpResponse;
import org.geotools.http.HTTPClient;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/** Verifies that the gs:DownloadMap Layer/Capabilities URL is subject to GeoServer's URL Checks. */
public class DownloadMapURLCheckTest extends BaseDownloadImageProcessTest {

    private static final String CAPABILITIES_URL =
            "http://geoserver.org/geoserver/wms?request=GetCapabilities&version=1.1.0";

    @Before
    public void setup() throws Exception {
        // make sure the GeoserverURLChecker gets initialized, in tests that's lazy load
        assertNotNull(applicationContext.getBean(GeoServerURLChecker.class));
        // start with empty rules
        URLCheckDAO dao = applicationContext.getBean(URLCheckDAO.class);
        dao.saveChecks(Collections.emptyList());
    }

    private String getTestRequest(String name) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(name), UTF_8);
    }

    @Test
    public void testCapabilitiesUrlDenied() throws Exception {
        URLCheckDAO dao = applicationContext.getBean(URLCheckDAO.class);
        dao.save(new RegexURLCheck("deny", "Won't match anything useful", "^abcd$"));

        String request = getTestRequest("mapRemoteSimple11.xml");
        MockHttpServletResponse response = postAsServletResponse("wps", request);
        assertEquals("text/xml", response.getContentType());
        String content = new String(response.getContentAsByteArray(), UTF_8);
        assertThat(content, containsString("Invalid Capabilities URL"));
        assertThat(content, containsString("was not accepted by external URL checks"));
    }

    @Test
    public void testCapabilitiesUrlAllowed() throws Exception {
        URLCheckDAO dao = applicationContext.getBean(URLCheckDAO.class);
        dao.save(new RegexURLCheck("allow", "Matches the test capabilities URL", Pattern.quote(CAPABILITIES_URL)));

        String request = getTestRequest("mapRemoteSimple11.xml");
        String caps111 = getTestRequest("caps111.xml");
        byte[] getMapBytes = FileUtils.readFileToByteArray(new File(SAMPLES + "mapSimple.png"));
        DownloadMapProcess process = applicationContext.getBean(DownloadMapProcess.class);
        MockHttpClient client = new MockHttpClient();
        client.expectGet(
                new URL("http://geoserver.org/geoserver/wms?service=WMS&request=GetCapabilities&version=1.1.0"),
                new MockHttpResponse(caps111, "text/xml"));
        client.expectGet(
                new URL("http://mock.test.geoserver"
                        + ".org/wms11?SERVICE=WMS&LAYERS=cite:BasicPolygons&FORMAT=image%2Fpng&HEIGHT=256&TRANSPARENT=false"
                        + "&REQUEST=GetMap&WIDTH=256&BBOX=-2.4,1.4,0.4,4.2&SRS=EPSG:4326&VERSION=1.1.1"),
                new MockHttpResponse(getMapBytes, "image/png"));
        Supplier<HTTPClient> oldSupplier = process.getHttpClientSupplier();
        try {
            process.setHttpClientSupplier(() -> client);
            MockHttpServletResponse response = postAsServletResponse("wps", request);
            assertEquals("image/png", response.getContentType());
        } finally {
            process.setHttpClientSupplier(oldSupplier);
        }
    }
}
