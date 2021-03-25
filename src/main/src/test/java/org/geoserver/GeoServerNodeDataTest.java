/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.InetAddress;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
public class GeoServerNodeDataTest {

    @Before
    public void mockAddress() {
        InetAddress addr = EasyMock.createMock(InetAddress.class);

        EasyMock.expect(addr.getHostAddress()).andStubReturn("192.2.0.42");
        EasyMock.expect(addr.getHostName()).andStubReturn("test.local");

        EasyMock.replay(addr);

        GeoServerNodeData.setMockAddress(addr);
    }

    @After
    public void cleanUp() {
        GeoServerNodeData.clearMockAddress();
    }

    @Test
    public void testCreate() {
        GeoServerNodeData data = GeoServerNodeData.createFromString("id:foo");
        assertEquals("foo", data.getId());
        assertNotNull(data.getIdStyle());
    }

    @Test
    public void testIP() {
        GeoServerNodeData data = GeoServerNodeData.createFromString("id:$host_ip");
        assertEquals("192.2.0.42", data.getId());
        assertNotNull(data.getIdStyle());
    }

    @Test
    public void testHostname() {
        GeoServerNodeData data = GeoServerNodeData.createFromString("id:$host_name");
        assertEquals("test.local", data.getId());
        assertNotNull(data.getIdStyle());
    }

    @Test
    public void testShortHostname() {
        GeoServerNodeData data = GeoServerNodeData.createFromString("id:$host_short_name");
        assertEquals("test", data.getId());
        assertNotNull(data.getIdStyle());
    }

    @Test
    public void testCompactHostname() {
        GeoServerNodeData data = GeoServerNodeData.createFromString("id:$host_compact_name");
        assertEquals("test.l", data.getId());
        assertNotNull(data.getIdStyle());
    }
}
