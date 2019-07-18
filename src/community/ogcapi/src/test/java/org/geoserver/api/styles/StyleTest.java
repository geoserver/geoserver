/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.styles;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.geoserver.catalog.SLDHandler;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class StyleTest extends StylesTestSupport {

    @Test
    public void testGetStyleNative() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("ogc/styles/styles/" + POLYGON_COMMENT);
        assertEquals(200, response.getStatus());
        assertEquals(SLDHandler.MIMETYPE_10, response.getContentType());
        // the native comment got preserved, this is not a re-encoding
        assertThat(response.getContentAsString(), containsString("This is a testable comment"));
    }

    @Test
    public void testGetStyleNativeExplicitFormat() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "ogc/styles/styles/"
                                + POLYGON_COMMENT
                                + "?f=application%2Fvnd.ogc.sld%2Bxml");
        assertEquals(200, response.getStatus());
        assertEquals(SLDHandler.MIMETYPE_10, response.getContentType());
        // the native comment got preserved, this is not a re-encoding
        assertThat(response.getContentAsString(), containsString("This is a testable comment"));
    }

    @Test
    public void testGetStyleConverted() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "ogc/styles/styles/"
                                + POLYGON_COMMENT
                                + "?f=application%2Fvnd.ogc.se%2Bxml");
        assertEquals(200, response.getStatus());
        assertEquals(SLDHandler.MIMETYPE_11, response.getContentType());
        // the native comment did not get preserved, this is a re-encoding
        assertThat(
                response.getContentAsString(), not(containsString("This is a testable comment")));
        // cannot test further because the SLD 1.1 encoding of styles is not actually available....
    }
}
