/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.impl.AuthorityURL;
import org.junit.Test;

public class AuthorityURLInfoInfoListConverterTest {

    @Test
    public void testFromString() {
        final String serialized =
                "[{\"name\":\"auth1\",\"href\":\"http://geoserver.org/auth1?\"},{\"name\":\"auth2\",\"href\":\"http://geoserver.org/auth2;someparam=somevalue&\"}]";
        List<AuthorityURLInfo> expected = new ArrayList<AuthorityURLInfo>();

        AuthorityURLInfo auth1 = new AuthorityURL();
        auth1.setName("auth1");
        auth1.setHref("http://geoserver.org/auth1?");
        expected.add(auth1);

        AuthorityURLInfo auth2 = new AuthorityURL();
        auth2.setName("auth2");
        auth2.setHref("http://geoserver.org/auth2;someparam=somevalue&");
        expected.add(auth2);

        List<AuthorityURLInfo> actual;
        actual = AuthorityURLInfoInfoListConverter.fromString(serialized);

        assertEquals(expected, actual);
    }

    @Test
    public void testFromInvalidString() {
        final String serialized = "[{\"name:\"auth1\",\"href\":\"http://geoserver.org/auth1?\"},]";

        try {
            AuthorityURLInfoInfoListConverter.fromString(serialized);
            fail("Expected IAE");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testToString() {
        List<AuthorityURLInfo> list = new ArrayList<AuthorityURLInfo>();

        AuthorityURLInfo auth1 = new AuthorityURL();
        auth1.setName("auth1");
        auth1.setHref("http://geoserver.org/auth1?");
        list.add(auth1);

        AuthorityURLInfo auth2 = new AuthorityURL();
        auth2.setName("auth2");
        auth2.setHref("http://geoserver.org/auth2;someparam=somevalue&");
        list.add(auth2);

        String actual = AuthorityURLInfoInfoListConverter.toString(list);
        String expected =
                "[{\"name\":\"auth1\",\"href\":\"http://geoserver.org/auth1?\"},{\"name\":\"auth2\",\"href\":\"http://geoserver.org/auth2;someparam=somevalue&\"}]";
        assertEquals(expected, actual);
    }

    @Test
    public void testToStringListWithNullElement() {
        List<AuthorityURLInfo> list = new ArrayList<AuthorityURLInfo>();

        AuthorityURLInfo auth1 = new AuthorityURL();
        auth1.setName("auth1");
        auth1.setHref("http://geoserver.org/auth1?");
        list.add(auth1);

        list.add(null);

        String actual = AuthorityURLInfoInfoListConverter.toString(list);
        String expected = "[{\"name\":\"auth1\",\"href\":\"http://geoserver.org/auth1?\"}]";
        assertEquals(expected, actual);
    }

    @Test
    public void testToStringListWithOnlyNullElements() {
        List<AuthorityURLInfo> list = new ArrayList<AuthorityURLInfo>();
        list.add(null);
        list.add(null);
        list.add(null);

        assertNull(AuthorityURLInfoInfoListConverter.toString(list));
    }

    @Test
    public void testToStringEmptyList() {
        List<AuthorityURLInfo> list = new ArrayList<AuthorityURLInfo>();

        String actual = AuthorityURLInfoInfoListConverter.toString(list);
        assertNull(actual);
    }
}
