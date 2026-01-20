package org.geoserver.wfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.json.JSONType;
import org.springframework.mock.web.MockHttpServletResponse;

public abstract class WFSServiceExceptionTestSupport extends WFSTestSupport {

    void testJsonpException(String wfsVersion) throws UnsupportedEncodingException, Exception {
        String path = getPath(wfsVersion);
        JSONType.setJsonpEnabled(true);
        MockHttpServletResponse response = getAsServletResponse(path + "&EXCEPTIONS=" + JSONType.jsonp);
        JSONType.setJsonpEnabled(false);

        // MimeType
        assertEquals(JSONType.jsonp, getBaseMimeType(response.getContentType()));

        // Content
        String content = response.getContentAsString();
        testJson(testJsonP(content), wfsVersion);
    }

    void testJsonException(String wfsVersion) throws UnsupportedEncodingException, Exception {
        String path = getPath(wfsVersion);
        MockHttpServletResponse response = getAsServletResponse(path + "&EXCEPTIONS=" + JSONType.json);

        // MimeType
        assertEquals(JSONType.json, getBaseMimeType(response.getContentType()));

        // Content
        String content = response.getContentAsString();
        testJson(content, wfsVersion);
    }

    private String getPath(String wfsVersion) {
        String path = "wfs/?service=wfs"
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

    private static void testJson(String content, String expectedVersion) {
        JSONObject jsonException = JSONObject.fromObject(content);
        assertEquals(expectedVersion, jsonException.getString("version"));
        JSONArray exceptions = jsonException.getJSONArray("exceptions");
        JSONObject exception = exceptions.getJSONObject(0);
        assertNotNull(exception);
        assertNotNull(exception.getString("code"));
        assertNotNull(exception.getString("locator"));
        String exceptionText = exception.getString("text");
        assertNotNull(exceptionText);
        assertEquals("Could not find type: {http://geoserver.org}foobar", exceptionText);
    }
}
