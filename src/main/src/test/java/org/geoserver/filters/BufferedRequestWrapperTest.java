/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filters;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.util.WebUtils;

public class BufferedRequestWrapperTest extends RequestWrapperTestSupport {

    @Test
    public void testGetInputStream() throws Exception {
        for (String testString : testStrings) {
            doInputStreamTest(testString);
        }
    }

    @Test
    public void testGetReader() throws Exception {
        for (String testString : testStrings) {
            doGetReaderTest(testString);
        }
    }

    public void doInputStreamTest(String testString) throws Exception {
        HttpServletRequest req = makeRequest(testString, null);

        BufferedRequestWrapper wrapper =
                new BufferedRequestWrapper(req, WebUtils.DEFAULT_CHARACTER_ENCODING, testString.getBytes());
        byte[] b = new byte[32];
        try (ServletInputStream sis = req.getInputStream()) {
            while ((sis.readLine(b, 0, 32)) > 0) { // NOPMD
                // clear out the request body
            }
        }

        try (ServletInputStream sis = wrapper.getInputStream()) {
            StringBuffer buff = new StringBuffer();
            int amountRead;
            while ((amountRead = sis.readLine(b, 0, 32)) != 0) {
                buff.append(new String(b, 0, amountRead));
            }

            assertEquals(buff.toString(), testString);
        }
    }

    public void doGetReaderTest(String testString) throws Exception {
        HttpServletRequest req = makeRequest(testString, null);

        clearOutBody(req);

        BufferedRequestWrapper wrapper =
                new BufferedRequestWrapper(req, WebUtils.DEFAULT_CHARACTER_ENCODING, testString.getBytes());
        StringBuffer buff = new StringBuffer();
        int c;
        try (BufferedReader br = wrapper.getReader()) {
            while ((c = br.read()) != -1) {
                buff.append((char) c);
            }

            assertEquals(buff.toString(), testString);
        }
    }

    @Test
    public void testMixedRequest() throws Exception {
        String body = "a=1&b=2";
        String queryString = "c=3&d=4";
        HttpServletRequest req = makeRequest(body, queryString);

        clearOutBody(req);

        BufferedRequestWrapper wrapper = new BufferedRequestWrapper(req, "UTF-8", body.getBytes());
        Map params = wrapper.getParameterMap();
        assertEquals(4, params.size());
        assertEquals("1", ((String[]) params.get("a"))[0]);
        assertEquals("2", ((String[]) params.get("b"))[0]);
        assertEquals("3", ((String[]) params.get("c"))[0]);
        assertEquals("4", ((String[]) params.get("d"))[0]);
    }

    private void clearOutBody(HttpServletRequest req) throws IOException {
        try (BufferedReader br = req.getReader()) {
            while ((br.readLine()) != null) { // NOPMD
                // clear out the request body
            }
        }
    }

    @Test
    public void testNoContentType() throws Exception {
        String body = "a=1&b=2";
        String queryString = "c=3&d=4";
        MockHttpServletRequest req = makeRequest(body, queryString);
        // reset the content type
        req.setContentType(null);
        clearOutBody(req);

        // should not NPE like it did
        BufferedRequestWrapper wrapper = new BufferedRequestWrapper(req, "UTF-8", body.getBytes());
        Map params = wrapper.getParameterMap();
        assertEquals(0, params.size());
    }

    @Test
    public void testEmptyPost() throws Exception {
        MockHttpServletRequest req = makeRequest("", "");
        // reset the content type
        req.setContentType(null);

        clearOutBody(req);

        // should not NPE like it did
        BufferedRequestWrapper wrapper = new BufferedRequestWrapper(req, "UTF-8", "".getBytes());
        Map params = wrapper.getParameterMap();
        assertEquals(0, params.size());
    }
}
