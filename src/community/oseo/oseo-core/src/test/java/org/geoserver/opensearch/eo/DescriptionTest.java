/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class DescriptionTest extends OSEOTestSupport {
    
    @Before
    public void setupOSEOInfo() {
        OSEOInfo info = getGeoServer().getService(OSEOInfo.class);
        
    }

    @Test
    public void testGlobalDescription() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("oseo/description");
        assertEquals("text/xml", response.getContentType());
        assertEquals(200, response.getStatus());
        
        Document dom = dom(new ByteArrayInputStream(response.getContentAsByteArray()));
        print(dom);
        
        // contents check
        assertThat(dom, hasXPath("/os:OpenSearchDescription"));
        assertThat(dom, hasXPath("/os:OpenSearchDescription/os:ShortName", equalTo("OSEO")));
        assertThat(dom, hasXPath("/os:OpenSearchDescription/os:LongName", equalTo("OpenSearch for Earth Observation")));
        assertThat(dom, hasXPath("/os:OpenSearchDescription/os:Description", containsString("Earth Observation")));
        assertThat(dom, hasXPath("/os:OpenSearchDescription/os:Tags", equalTo("EarthObservation OGC")));
        assertThat(dom, hasXPath("/os:OpenSearchDescription/os:LongName", containsString("OpenSearch")));
        assertThat(dom, hasXPath("/os:OpenSearchDescription/os:SyndicationRight", equalTo("open")));
        assertThat(dom, hasXPath("/os:OpenSearchDescription/os:AdultContent", equalTo("false")));
        assertThat(dom, hasXPath("/os:OpenSearchDescription/os:Language", equalTo("en-us")));
        assertThat(dom, hasXPath("/os:OpenSearchDescription/os:OutputEncoding", equalTo("UTF-8")));
        assertThat(dom, hasXPath("/os:OpenSearchDescription/os:InputEncoding", equalTo("UTF-8")));
        

        // general validation
        checkValidOSDD(dom);
    }
    
}
