/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import static org.junit.Assert.assertEquals;

import net.sf.json.JSON;

import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;

public class ApiTest extends WFS3TestSupport {

    @Test
    public void testApiJson() throws Exception {
        JSON json = getAsJSON("wfs3/api");
        print(json);
    }

    @Test
    public void testApiYaml() throws Exception {
        String yaml = getAsString("wfs3/api?f=application/x-yaml");
        System.out.println(yaml);
    }
    
    @Test
    public void testYamlAsAcceptsHeader() throws Exception {
        MockHttpServletRequest request = createRequest("wfs3/api");
        request.setMethod( "GET" );
        request.setContent(new byte[]{});
        request.addHeader(HttpHeaders.ACCEPT, "foo/bar, application/x-yaml, text/html");
        MockHttpServletResponse response = dispatch(request);
        assertEquals(200, response.getStatus());
        assertEquals("application/x-yaml", response.getContentType());
        String yaml = string(new ByteArrayInputStream(response.getContentAsString().getBytes()));
        
        System.out.println(yaml);
    }
}
