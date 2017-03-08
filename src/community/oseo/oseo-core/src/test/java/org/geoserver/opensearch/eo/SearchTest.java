/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import java.io.ByteArrayInputStream;

import org.geoserver.opensearch.eo.response.AtomSearchResponse;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class SearchTest extends OSEOTestSupport {

    @Test
    public void testAllCollection() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("oseo/search?httpAccept=" + AtomSearchResponse.MIME);
        assertEquals(AtomSearchResponse.MIME, response.getContentType());
        assertEquals(200, response.getStatus());
        
        Document dom = dom(new ByteArrayInputStream(response.getContentAsByteArray()));
        print(dom);
        
        // basics
        assertThat(dom, hasXPath("/at:feed/os:totalResults", equalTo("3")));
        assertThat(dom, hasXPath("/at:feed/os:startIndex", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/os:itemsPerPage", equalTo("10")));
        assertThat(dom, hasXPath("/at:feed/os:Query"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@count='10']"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@startIndex='1']"));
        assertThat(dom, hasXPath("/at:feed/at:author/at:name", equalTo("GeoServer")));
        assertThat(dom, hasXPath("/at:feed/at:updated"));
        
        // pagination links (all the same)
        assertHasLink(dom, "self", 1, 10);
        assertHasLink(dom, "first", 1, 10);
        assertHasLink(dom, "last", 1, 10);
        assertThat(dom, not(hasXPath("/at:feed/at:link[@rel='previous']")));
        assertThat(dom, not(hasXPath("/at:feed/at:link[@rel='next']")));
        
        checkValidAtomFeed(dom);
    }

    @Test
    public void testPagingFullPages() throws Exception {
        // first page
        Document dom = getAsDOM("oseo/search?count=1");
        assertHasLink(dom, "self", 1, 1);
        assertHasLink(dom, "first", 1, 1);
        assertHasLink(dom, "next", 2, 1);
        assertHasLink(dom, "last", 3, 1);
        assertThat(dom, not(hasXPath("/at:feed/at:link[@rel='previous']")));
        
        // second page
        dom = getAsDOM("oseo/search?count=1&startIndex=2");
        assertHasLink(dom, "self", 2, 1);
        assertHasLink(dom, "first", 1, 1);
        assertHasLink(dom, "previous", 1, 1);
        assertHasLink(dom, "next", 3, 1);
        assertHasLink(dom, "last", 3, 1);
        
        // third and last page
        dom = getAsDOM("oseo/search?count=1&startIndex=3");
        assertHasLink(dom, "self", 3, 1);
        assertHasLink(dom, "first", 1, 1);
        assertHasLink(dom, "previous", 2, 1);
        assertHasLink(dom, "last", 3, 1);
        assertThat(dom, not(hasXPath("/at:feed/at:link[@rel='next']")));
    }
    
    @Test
    public void testPagingPartialPages() throws Exception {
        // first page
        Document dom = getAsDOM("oseo/search?count=2");
        assertHasLink(dom, "self", 1, 2);
        assertHasLink(dom, "first", 1, 2);
        assertHasLink(dom, "next", 3, 2);
        assertHasLink(dom, "last", 3, 2);
        assertThat(dom, not(hasXPath("/at:feed/at:link[@rel='previous']")));
        
        // second page
        dom = getAsDOM("oseo/search?count=2&startIndex=3");
        assertHasLink(dom, "self", 3, 2);
        assertHasLink(dom, "first", 1, 2);
        assertHasLink(dom, "previous", 1, 2);
        assertHasLink(dom, "last", 3, 2);
        assertThat(dom, not(hasXPath("/at:feed/at:link[@rel='next']")));
    }
    
    

    private void assertHasLink(Document dom, String rel, int startIndex, int count) {
        assertThat(dom, hasXPath("/at:feed/at:link[@rel='" + rel + "']"));
        assertThat(dom, hasXPath("/at:feed/at:link[@rel='" + rel + "']/@href", containsString("startIndex=" + startIndex)));
        assertThat(dom, hasXPath("/at:feed/at:link[@rel='" + rel + "']/@href", containsString("count=" + count)));
    }
}
