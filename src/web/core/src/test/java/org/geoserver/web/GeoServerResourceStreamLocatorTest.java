/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Iterators;
import java.util.Locale;
import java.util.Properties;
import org.apache.wicket.core.util.resource.locator.IResourceNameIterator;
import org.apache.wicket.util.resource.IResourceStream;
import org.junit.Test;

public class GeoServerResourceStreamLocatorTest {
    /** Test that the resource locator only returns a name for certain file types. */
    @Test
    public void testUTF8EncodingConvention() throws Exception {
        GeoServerResourceStreamLocator l = new GeoServerResourceStreamLocator();

        try (IResourceStream resourceStream =
                l.locate(
                        GeoServerResourceStreamLocatorTest.class,
                        "./GeoServerApplication.properties")) {

            Properties properties = new Properties();
            properties.load(resourceStream.getInputStream());
            assertEquals("welcome", properties.getProperty("StatusPageTest.welcome"));
        }

        try (IResourceStream resourceStream =
                l.locate(
                        GeoServerResourceStreamLocatorTest.class,
                        "./GeoServerApplication.utf8.properties")) {

            Properties properties = new Properties();
            properties.load(resourceStream.getInputStream());
            assertEquals("\u6B22\u8FCE", properties.getProperty("StatusPageTest.welcome"));
        }
    }

    @Test
    /** Test that the resource locator only returns a name for certain file types. */
    public void testNewResourceNameIterator() {
        GeoServerResourceStreamLocator l = new GeoServerResourceStreamLocator();

        IResourceNameIterator it =
                l.newResourceNameIterator(
                        "org/geoserver/Foo", Locale.US, null, null, "html", false);
        assertEquals(1, Iterators.size(it));

        it = l.newResourceNameIterator("org/geoserver/Foo", Locale.US, null, null, "css", false);
        assertEquals(1, Iterators.size(it));

        it = l.newResourceNameIterator("org/geoserver/Foo", Locale.US, null, null, "ico", false);
        assertEquals(1, Iterators.size(it));

        it = l.newResourceNameIterator("org/geoserver/Foo", Locale.US, null, null, "js", false);
        assertEquals(1, Iterators.size(it));

        it = l.newResourceNameIterator("org/geoserver/Foo", Locale.US, null, null, "baz", false);
        assertTrue(Iterators.size(it) > 1);

        it =
                l.newResourceNameIterator(
                        "org/geoserver/Foo.html", Locale.US, null, null, (String) null, false);
        assertEquals(1, Iterators.size(it));

        it =
                l.newResourceNameIterator(
                        "org/geoserver/Foo.css", Locale.US, null, null, (String) null, false);
        assertEquals(1, Iterators.size(it));

        it =
                l.newResourceNameIterator(
                        "org/geoserver/Foo.ico", Locale.US, null, null, (String) null, false);
        assertEquals(1, Iterators.size(it));

        it =
                l.newResourceNameIterator(
                        "org/geoserver/Foo.js", Locale.US, null, null, (String) null, false);
        assertEquals(1, Iterators.size(it));

        it =
                l.newResourceNameIterator(
                        "org/geoserver/Foo.baz", Locale.US, null, null, (String) null, false);
        assertTrue(Iterators.size(it) > 1);
    }
}
