/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

import java.io.ByteArrayInputStream;
import org.geoserver.ows.util.ResponseUtils;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class GetExecutionResultTest extends WPSTestSupport {

    @Test
    public void testGetExecutionResultWithoutMime() throws Exception {
        String request = getGetExecutionResultHref();
        request = request.substring(0, request.indexOf("&mimetype="));
        MockHttpServletResponse response = getAsServletResponse(request);
        assertEquals("text/csv", response.getContentType());
        assertEquals("inline; filename=result.csv", response.getHeader(CONTENT_DISPOSITION));
        assertEquals("foo", response.getContentAsString().trim());
    }

    @Test
    public void testGetExecutionResultWithDecodedMime() throws Exception {
        String request = getGetExecutionResultHref();
        MockHttpServletResponse response = getAsServletResponse(request);
        assertEquals("text/csv", response.getContentType());
        assertEquals("inline; filename=result.csv", response.getHeader(CONTENT_DISPOSITION));
        assertEquals("foo", response.getContentAsString().trim());
    }

    @Test
    public void testGetExecutionResultWithEncodedMime() throws Exception {
        String request = getGetExecutionResultHref();
        request = request.replace("text/csv", "text%2Fcsv");
        MockHttpServletResponse response = getAsServletResponse(request);
        assertEquals("text/csv", response.getContentType());
        assertEquals("inline; filename=result.csv", response.getHeader(CONTENT_DISPOSITION));
        assertEquals("foo", response.getContentAsString().trim());
    }

    @Test
    public void testGetExecutionResultWithInvalidMime() throws Exception {
        String request = getGetExecutionResultHref();
        request = request.replace("text/csv", "text/html");
        MockHttpServletResponse response = getAsServletResponse(request);
        assertEquals("application/xml", response.getContentType());
        assertNull(response.getHeader(CONTENT_DISPOSITION));
        Document dom = dom(new ByteArrayInputStream(response.getContentAsByteArray()));
        assertXpathExists("/ows:ExceptionReport/ows:Exception/ows:ExceptionText", dom);
        assertXpathEvaluatesTo(
                "Requested mime type does not match the output resource mime type",
                "/ows:ExceptionReport/ows:Exception/ows:ExceptionText",
                dom);
    }

    private String getGetExecutionResultHref() throws Exception {
        String request =
                "wps?service=WPS&version=1.0.0&request=Execute&Identifier=vec:Reproject"
                        + "&DataInputs=features=foo@mimetype=text/csv"
                        + "&ResponseDocument=result=@asReference=true@mimetype=text/csv";
        String expression =
                "/wps:ExecuteResponse/wps:ProcessOutputs/wps:Output/wps:Reference/@href";
        Document dom = getAsDOM(request);
        assertXpathExists(expression, dom);
        String href = xp.evaluate(expression, dom);
        href = "wps" + ResponseUtils.urlDecode(href.substring(href.indexOf("?")));
        assertThat(href, endsWith("&mimetype=text/csv"));
        return href;
    }
}
