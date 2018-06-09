/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MetadataLinkInfoImplTest {
    @Rule public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testSetAbsoluteHttp() {
        MetadataLinkInfoImpl info = new MetadataLinkInfoImpl();

        info.setContent("http://example.com/foo");
    }

    @Test
    public void testSetAbsoluteHttps() {
        MetadataLinkInfoImpl info = new MetadataLinkInfoImpl();

        info.setContent("https://example.com/foo");
    }

    @Test
    public void testSetAbsoluteFtp() {
        MetadataLinkInfoImpl info = new MetadataLinkInfoImpl();

        info.setContent("ftp://example.com/foo");
    }

    @Test
    public void testSetAbsoluteTelnet() {
        MetadataLinkInfoImpl info = new MetadataLinkInfoImpl();

        thrown.expect(IllegalArgumentException.class);

        info.setContent("telnet:example.com");
    }

    @Test
    public void testSetRelativeUrlAbsolutePath() {
        MetadataLinkInfoImpl info = new MetadataLinkInfoImpl();

        info.setContent("/foo");
    }

    @Test
    public void testSetRelativeUrlRelativePath() {
        MetadataLinkInfoImpl info = new MetadataLinkInfoImpl();

        info.setContent("foo/bar");
    }

    @Test
    public void testSetRelativeUrlRelativeCurrentPath() {
        MetadataLinkInfoImpl info = new MetadataLinkInfoImpl();

        info.setContent("./foo");
    }

    @Test
    public void testSetRelativeUrlRelativeParentPath() {
        MetadataLinkInfoImpl info = new MetadataLinkInfoImpl();

        info.setContent("../foo");
    }

    @Test
    public void testSetNotAURL() {
        MetadataLinkInfoImpl info = new MetadataLinkInfoImpl();

        thrown.expect(IllegalArgumentException.class);

        info.setContent("::^%/[*] FOO ::");
    }

    @Test
    public void testNotAURLButStartsOK() {
        MetadataLinkInfoImpl info = new MetadataLinkInfoImpl();

        thrown.expect(IllegalArgumentException.class);

        info.setContent("https://example.com/::^%/[*] FOO ::");
    }
}
