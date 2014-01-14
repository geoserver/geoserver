/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs.json.JSONType;
import org.junit.Test;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class WFSServiceExceptionTest extends WFSTestSupport {

    @Override
    protected void setUpInternal(SystemTestData data) throws Exception {
    	WFSInfo wfs = getWFS();
        wfs.setFeatureBounding(true);
        getGeoServer().save(wfs);
    }

    @Test
    public void testJsonpException() throws Exception {

        String path = "wfs/?service=wfs" + "&version=1.1.0" + "&request=DescribeFeatureType"
                + "&typeName=foobar" + "&format_options=" + JSONType.CALLBACK_FUNCTION_KEY
                + ":myMethod";

        // JSONP
        JSONType.setJsonpEnabled(true);
        MockHttpServletResponse response = getAsServletResponse(path + "&EXCEPTIONS="
                + JSONType.jsonp);
        JSONType.setJsonpEnabled(false);

        // MimeType
        assertEquals(JSONType.jsonp, response.getContentType());

        // Content
        String content = response.getOutputStreamContent();
        testJson(testJsonP(content));

        // JSON
        response = getAsServletResponse(path + "&EXCEPTIONS=" + JSONType.json);

        // MimeType
        assertEquals(JSONType.json, response.getContentType());

        // Content
        content = response.getOutputStreamContent();
        testJson(content);

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

    /**
     * @param path
     * @throws Exception
     * 
     */
    private static void testJson(String content) {

        JSONObject jsonException = JSONObject.fromObject(content);
        assertEquals(jsonException.getString("version"), "1.1.0");
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
