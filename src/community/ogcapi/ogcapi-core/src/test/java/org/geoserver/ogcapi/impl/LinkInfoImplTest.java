/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class LinkInfoImplTest {

    private LinkInfoImpl linkInfo;

    @Before
    public void setUp() {
        linkInfo = new LinkInfoImpl();
    }

    @Test
    public void testRel() {
        linkInfo.setRel("self");
        assertEquals("self", linkInfo.getRel());
    }

    @Test
    public void testType() {
        linkInfo.setType("application/json");
        assertEquals("application/json", linkInfo.getType());
    }

    @Test
    public void testTitle() {
        linkInfo.setTitle("Example Title");
        assertEquals("Example Title", linkInfo.getTitle());
    }

    @Test
    public void testHref() {
        linkInfo.setHref("http://example.com");
        assertEquals("http://example.com", linkInfo.getHref());
    }

    @Test
    public void testService() {
        linkInfo.setService("Example Service");
        assertEquals("Example Service", linkInfo.getService());
    }

    @Test
    public void testEquals() {
        LinkInfoImpl anotherLinkInfo = new LinkInfoImpl();
        anotherLinkInfo.setRel("self");
        anotherLinkInfo.setType("application/json");
        anotherLinkInfo.setTitle("Example Title");
        anotherLinkInfo.setHref("http://example.com");
        anotherLinkInfo.setService("Example Service");

        linkInfo.setRel("self");
        linkInfo.setType("application/json");
        linkInfo.setTitle("Example Title");
        linkInfo.setHref("http://example.com");
        linkInfo.setService("Example Service");

        assertEquals(linkInfo, anotherLinkInfo);
    }

    @Test
    public void testHashCode() {
        LinkInfoImpl anotherLinkInfo = new LinkInfoImpl();
        anotherLinkInfo.setRel("self");
        anotherLinkInfo.setType("application/json");
        anotherLinkInfo.setTitle("Example Title");
        anotherLinkInfo.setHref("http://example.com");
        anotherLinkInfo.setService("Example Service");

        linkInfo.setRel("self");
        linkInfo.setType("application/json");
        linkInfo.setTitle("Example Title");
        linkInfo.setHref("http://example.com");
        linkInfo.setService("Example Service");

        assertEquals(linkInfo.hashCode(), anotherLinkInfo.hashCode());
    }

    @Test
    public void testClone() throws CloneNotSupportedException {
        linkInfo.setRel("self");
        linkInfo.setType("application/json");
        linkInfo.setTitle("Example Title");
        linkInfo.setHref("http://example.com");
        linkInfo.setService("Example Service");

        LinkInfoImpl clonedLinkInfo = (LinkInfoImpl) linkInfo.clone();

        assertEquals(linkInfo, clonedLinkInfo);
        assertEquals(linkInfo.hashCode(), clonedLinkInfo.hashCode());
    }
}
