/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class DescriptionTest extends OSEOTestSupport {

    @Test
    public void testGlobalDescription() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("oseo/description");
        assertEquals("text/xml", response.getContentType());
        assertEquals(200, response.getStatus());
        
        Document dom = dom(new ByteArrayInputStream(response.getContentAsByteArray()));
        print(dom);
        
        assertThat(dom, hasXPath("/os:OpenSearchDescription"));
        
    }
    
}
