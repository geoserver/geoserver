/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs.json.JSONType;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class WFSServiceExceptionTest extends WFSTestSupport {

    @Override
    protected void setUpInternal(SystemTestData data) throws Exception {
        WFSInfo wfs = getWFS();
        wfs.setFeatureBounding(true);
        getGeoServer().save(wfs);
    }

    @Test
    public void testJsonpException() throws Exception {
        testJsonpException("1.1.0");
    }

    @Test
    public void testJsonException() throws Exception {
        testJsonException("1.1.0");
    }

    @Test
    public void testJsonpException20() throws Exception {
        testJsonpException("2.0.0");
    }

    @Test
    public void testJsonException20() throws Exception {
        testJsonException("2.0.0");
    }

    private void testJsonpException(String wfsVersion)
            throws UnsupportedEncodingException, Exception {

        String path = getPath(wfsVersion);
        JSONType.setJsonpEnabled(true);
        MockHttpServletResponse response =
                getAsServletResponse(path + "&EXCEPTIONS=" + JSONType.jsonp);
        JSONType.setJsonpEnabled(false);

        // MimeType
        assertEquals(JSONType.jsonp, response.getContentType());

        // Content
        String content = response.getContentAsString();
        testJson(testJsonP(content), wfsVersion);
    }

    private void testJsonException(String wfsVersion)
            throws UnsupportedEncodingException, Exception {

        String path = getPath(wfsVersion);
        MockHttpServletResponse response =
                getAsServletResponse(path + "&EXCEPTIONS=" + JSONType.json);

        // MimeType
        assertEquals(JSONType.json, response.getContentType());

        // Content
        String content = response.getContentAsString();
        testJson(content, wfsVersion);
    }

    private String getPath(String wfsVersion) {
        String path =
                "wfs/?service=wfs"
                        + "&version="
                        + wfsVersion
                        + "&request=DescribeFeatureType"
                        + "&typeName=foobar"
                        + "&format_options="
                        + JSONType.CALLBACK_FUNCTION_KEY
                        + ":myMethod";
        return path;
    }

    /**
     * @param content Matches: myMethod( ... )
     * @return trimmed string
     */
    private static String testJsonP(String content) {
        assertTrue(content.startsWith("myMethod("));
        assertTrue(content.endsWith(")"));
        content = content.substring("myMethod(".length(), content.length() - 1);

        return content;
    }

    /** @param path */
    private static void testJson(String content, String expectedVersion) {

        JSONObject jsonException = JSONObject.fromObject(content);
        assertEquals(jsonException.getString("version"), expectedVersion);
        JSONArray exceptions = jsonException.getJSONArray("exceptions");
        JSONObject exception = exceptions.getJSONObject(0);
        assertNotNull(exception);
        assertNotNull(exception.getString("code"));
        assertNotNull(exception.getString("locator"));
        String exceptionText = exception.getString("text");
        assertNotNull(exceptionText);
        assertEquals(exceptionText, "Could not find type: {http://geoserver.org}foobar");
    }
}
