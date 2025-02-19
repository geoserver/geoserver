/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AttributeTypeInfoImplTest {
    @Test
    public void testQuoting() {
        assertNotQuoted("foo");
        assertNotQuoted("foo123");
        assertNotQuoted("FOO");
        assertNotQuoted("FOO123");
        assertNotQuoted("foo_bar");
        assertNotQuoted("Foo_Bar");

        assertQuoted("123foo");
        assertQuoted("foo.bar");
        assertQuoted("point");
        assertQuoted("POINT");
        assertQuoted("BBOX");
        assertQuoted("intersects");
    }

    private void assertNotQuoted(String name) {
        AttributeTypeInfoImpl a = new AttributeTypeInfoImpl();
        a.setName(name);
        assertEquals(name, a.getSource());
    }

    private void assertQuoted(String name) {
        AttributeTypeInfoImpl a = new AttributeTypeInfoImpl();
        a.setName(name);
        assertEquals("\"" + name + "\"", a.getSource());
    }
}
