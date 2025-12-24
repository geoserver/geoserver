/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
public class GeoServerNodeDataTest {

    @Before
    public void mockAddress() throws UnknownHostException {
        InetAddress addr = InetAddress.getByAddress("test.local", new byte[] {(byte) 192, 2, 0, 42});

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

    @After
    public void tearDown() throws Exception {
        GeoServerNodeData.clearOverrideGitHeadPath();
    }

    @Test
    public void testGitBranchToken() throws Exception {
        Path tmp = Files.createTempDirectory("githead");
        Path head = tmp.resolve(".git").resolve("HEAD");
        Files.createDirectories(head.getParent());
        Files.writeString(head, "ref: refs/heads/feature/test\n");

        GeoServerNodeData.setOverrideGitHeadPath(head);
        GeoServerNodeData data = GeoServerNodeData.createFromString("id:$git_branch");
        assertEquals("feature/test", data.getId());
    }

    @Test
    public void testGitBranchFromDataDir() throws Exception {
        Path tmp = Files.createTempDirectory("data-dir");
        Path head = tmp.resolve(".git").resolve("HEAD");
        Files.createDirectories(head.getParent());
        Files.writeString(head, "ref: refs/heads/data-branch\n");

        System.setProperty("GEOSERVER_DATA_DIR", tmp.toAbsolutePath().toString());
        try {
            GeoServerNodeData data = GeoServerNodeData.createFromString("id:$git_branch");
            assertEquals("data-branch", data.getId());
        } finally {
            System.clearProperty("GEOSERVER_DATA_DIR");
        }
    }

    @Test
    public void testGitBranchFromParentOfDataDir() throws Exception {
        Path parent = Files.createTempDirectory("parent-dir");
        Path child = parent.resolve("child-data");
        Files.createDirectories(child);

        Path head = parent.resolve(".git").resolve("HEAD");
        Files.createDirectories(head.getParent());
        Files.writeString(head, "ref: refs/heads/parent-branch\n");

        System.setProperty("GEOSERVER_DATA_DIR", child.toAbsolutePath().toString());
        try {
            GeoServerNodeData data = GeoServerNodeData.createFromString("id:$git_branch");
            assertEquals("parent-branch", data.getId());
        } finally {
            System.clearProperty("GEOSERVER_DATA_DIR");
        }
    }
}
