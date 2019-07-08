/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.wfs.servlets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.servlet.ServletException;
import org.apache.commons.codec.binary.Base64;
import org.junit.Assume;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Tests the functionality of the {@link TestWfsPost} servlet on a running geoserver.
 *
 * <p>This test assumes a running GeoServer on port 8080 with the release data dir.
 *
 * @author Torben Barsballe
 */
public class TestWfsPostOnlineIntegrationTest {

    public static final String WFS_REQUEST =
            "<wfs:GetFeature service=\"WFS\" version=\"1.1.0\"\n"
                    + "  xmlns:ne=\"http://www.naturalearthdata.com\"\n"
                    + "  xmlns:wfs=\"http://www.opengis.net/wfs\"\n"
                    + "  xmlns:ogc=\"http://www.opengis.net/ogc\"\n"
                    + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                    + "  xsi:schemaLocation=\"http://www.opengis.net/wfs\n"
                    + "                      http://schemas.opengis.net/wfs/1.1.0/wfs.xsd\">\n"
                    + "  <wfs:Query typeName=\"states\">\n"
                    + "    <ogc:Filter>\n"
                    + "       <ogc:FeatureId fid=\"states.3\"/>\n"
                    + "    </ogc:Filter>\n"
                    + "    </wfs:Query>\n"
                    + "</wfs:GetFeature>";

    protected MockHttpServletResponse doWfsPost() throws ServletException, IOException {
        return doWfsPost(null, null);
    }

    protected MockHttpServletResponse doWfsPost(String username, String password)
            throws ServletException, IOException {
        return doWfsPost(username, password, false);
    }

    protected MockHttpServletResponse doWfsPost(
            String username, String password, boolean useHttpBasicAuth)
            throws ServletException, IOException {
        TestWfsPost servlet = TestWfsPostTest.buildMockServlet();
        MockHttpServletRequest request = TestWfsPostTest.buildMockRequest();
        request.setParameter("url", "http://localhost:8080/geoserver/wfs");
        request.setParameter("body", WFS_REQUEST);

        if (username != null && password != null) {
            if (useHttpBasicAuth) {
                String up = username + ":" + password;
                byte[] encoded = Base64.encodeBase64(up.getBytes());
                String authHeader = "Basic " + new String(encoded);
                request.addHeader("Authorization", authHeader);
            } else {
                request.setParameter("username", username);
                request.setParameter("password", password);
            }
        }
        request.setMethod("GET");

        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);

        return response;
    }

    private boolean isOnline() {
        try {
            URL u = new URL("http://localhost:8080/geoserver");
            HttpURLConnection huc = (HttpURLConnection) u.openConnection();
            huc.setRequestMethod("HEAD");
            huc.connect();
            return huc.getResponseCode() == 200;
        } catch (IOException e) {
            return false;
        }
    }

    @Test
    public void testWfsPost() throws ServletException, IOException {
        Assume.assumeTrue(isOnline());
        MockHttpServletResponse response = doWfsPost();

        assertTrue(response.getContentAsString().contains("wfs:FeatureCollection"));
    }

    @Test
    public void testWfsPostAuthenticated() throws ServletException, IOException {
        Assume.assumeTrue(isOnline());
        MockHttpServletResponse response = doWfsPost("admin", "geoserver");

        assertTrue(response.getContentAsString().contains("wfs:FeatureCollection"));
    }

    @Test
    public void testWfsPostInvalidAuth() throws ServletException, IOException {
        Assume.assumeTrue(isOnline());
        MockHttpServletResponse response = doWfsPost("admin", "badpassword");

        assertFalse(response.getContentAsString().contains("wfs:FeatureCollection"));
        assertTrue(response.getContentAsString().contains("HTTP response: 401"));
    }

    @Test
    public void testWfsPostNotForwardingHeader() throws IOException, ServletException {
        Assume.assumeTrue(isOnline());
        // Use a header with bad credentials, expecting it will be ignored
        MockHttpServletResponse response = doWfsPost("admin", "badpassword", true);

        assertFalse(response.getContentAsString().contains("HTTP response: 401"));
        assertTrue(response.getContentAsString().contains("wfs:FeatureCollection"));
    }
}
