/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/** @author Marcus Sen, British Geological Survey */
public class DataLinkInfoImplTest {
    @Rule public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testSetAbsoluteHttp() {
        DataLinkInfoImpl info = new DataLinkInfoImpl();

        info.setContent("http://example.com/foo");
    }

    @Test
    public void testSetAbsoluteHttps() {
        DataLinkInfoImpl info = new DataLinkInfoImpl();

        info.setContent("https://example.com/foo");
    }

    @Test
    public void testSetAbsoluteFtp() {
        DataLinkInfoImpl info = new DataLinkInfoImpl();

        info.setContent("ftp://example.com/foo");
    }

    @Test
    public void testSetAbsoluteTelnet() {
        DataLinkInfoImpl info = new DataLinkInfoImpl();

        thrown.expect(IllegalArgumentException.class);

        info.setContent("telnet:example.com");
    }

    @Test
    public void testSetRelativeUrlAbsolutePath() {
        DataLinkInfoImpl info = new DataLinkInfoImpl();

        info.setContent("/foo");
    }

    @Test
    public void testSetRelativeUrlRelativePath() {
        DataLinkInfoImpl info = new DataLinkInfoImpl();

        info.setContent("foo/bar");
    }

    @Test
    public void testSetRelativeUrlRelativeCurrentPath() {
        DataLinkInfoImpl info = new DataLinkInfoImpl();

        info.setContent("./foo");
    }

    @Test
    public void testSetRelativeUrlRelativeParentPath() {
        DataLinkInfoImpl info = new DataLinkInfoImpl();

        info.setContent("../foo");
    }

    @Test
    public void testSetNotAURL() {
        DataLinkInfoImpl info = new DataLinkInfoImpl();

        thrown.expect(IllegalArgumentException.class);

        info.setContent("::^%/[*] FOO ::");
    }

    @Test
    public void testNotAURLButStartsOK() {
        DataLinkInfoImpl info = new DataLinkInfoImpl();

        thrown.expect(IllegalArgumentException.class);

        info.setContent("https://example.com/::^%/[*] FOO ::");
    }
}
