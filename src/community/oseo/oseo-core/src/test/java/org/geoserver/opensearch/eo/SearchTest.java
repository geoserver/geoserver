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
        // print(dom);
        
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
        
        // check entries
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("3")));
        // ... sorted by date
        assertThat(dom, hasXPath("/at:feed/at:entry[1]/at:title", equalTo("SENTINEL2")));
        assertThat(dom, hasXPath("/at:feed/at:entry[2]/at:title", equalTo("SENTINEL1")));
        assertThat(dom, hasXPath("/at:feed/at:entry[3]/at:title", equalTo("LANDSAT8")));
        
        // check the sentinel2 one
        assertThat(dom, hasXPath("/at:feed/at:entry[1]/at:id", equalTo("http://localhost:8080/geoserver/oseo/search?uid=SENTINEL2&httpAccept=application%2Fatom%2Bxml")));
        assertThat(dom, hasXPath("/at:feed/at:entry[1]/at:updated", equalTo("2016-02-26T09:20:21Z")));
        // ... mind the lat/lon order
        assertThat(dom, hasXPath("/at:feed/at:entry[1]/georss:where/gml:Polygon/gml:exterior/gml:LinearRing/gml:posList",
                equalTo("89.0 -179.0 89.0 179.0 -89.0 179.0 -89.0 -179.0 89.0 -179.0")));
        
        // overall schema validation for good measure
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
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/at:entry[1]/at:title", equalTo("SENTINEL2")));
        
        // second page
        dom = getAsDOM("oseo/search?count=1&startIndex=2");
        assertHasLink(dom, "self", 2, 1);
        assertHasLink(dom, "first", 1, 1);
        assertHasLink(dom, "previous", 1, 1);
        assertHasLink(dom, "next", 3, 1);
        assertHasLink(dom, "last", 3, 1);
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/at:entry[1]/at:title", equalTo("SENTINEL1")));

        
        // third and last page
        dom = getAsDOM("oseo/search?count=1&startIndex=3");
        assertHasLink(dom, "self", 3, 1);
        assertHasLink(dom, "first", 1, 1);
        assertHasLink(dom, "previous", 2, 1);
        assertHasLink(dom, "last", 3, 1);
        assertThat(dom, not(hasXPath("/at:feed/at:link[@rel='next']")));
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/at:entry[1]/at:title", equalTo("LANDSAT8")));
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
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("2")));
        assertThat(dom, hasXPath("/at:feed/at:entry[1]/at:title", equalTo("SENTINEL2")));
        assertThat(dom, hasXPath("/at:feed/at:entry[2]/at:title", equalTo("SENTINEL1")));
        
        // second page
        dom = getAsDOM("oseo/search?count=2&startIndex=3");
        assertHasLink(dom, "self", 3, 2);
        assertHasLink(dom, "first", 1, 2);
        assertHasLink(dom, "previous", 1, 2);
        assertHasLink(dom, "last", 3, 2);
        assertThat(dom, not(hasXPath("/at:feed/at:link[@rel='next']")));
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/at:entry[1]/at:title", equalTo("LANDSAT8")));
    }
    
    @Test
    public void testGeoUidQuery() throws Exception {
        Document dom = getAsDOM("oseo/search?uid=LANDSAT8&httpAccept=" + AtomSearchResponse.MIME);
        print(dom);
        
        // basics
        assertThat(dom, hasXPath("/at:feed/os:totalResults", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/os:startIndex", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/os:itemsPerPage", equalTo("10")));
        assertThat(dom, hasXPath("/at:feed/os:Query"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@count='10']"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@startIndex='1']"));
        assertThat(dom, hasXPath("/at:feed/os:Query[@geo:uid='LANDSAT8']"));
        
        // pagination links (all the same)
        assertHasLink(dom, "self", 1, 10);
        assertHasLink(dom, "first", 1, 10);
        assertHasLink(dom, "last", 1, 10);
        assertThat(dom, not(hasXPath("/at:feed/at:link[@rel='previous']")));
        assertThat(dom, not(hasXPath("/at:feed/at:link[@rel='next']")));
        
        // check entries
        assertThat(dom, hasXPath("count(/at:feed/at:entry)", equalTo("1")));
        assertThat(dom, hasXPath("/at:feed/at:entry[1]/at:title", equalTo("LANDSAT8")));
        
        // overall schema validation for good measure
        checkValidAtomFeed(dom);
    }

    private void assertHasLink(Document dom, String rel, int startIndex, int count) {
        assertThat(dom, hasXPath("/at:feed/at:link[@rel='" + rel + "']"));
        assertThat(dom, hasXPath("/at:feed/at:link[@rel='" + rel + "']/@href", containsString("startIndex=" + startIndex)));
        assertThat(dom, hasXPath("/at:feed/at:link[@rel='" + rel + "']/@href", containsString("count=" + count)));
    }
}
