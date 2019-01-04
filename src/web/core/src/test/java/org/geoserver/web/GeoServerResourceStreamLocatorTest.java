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
import org.apache.wicket.core.util.resource.locator.IResourceNameIterator;
import org.junit.Test;

public class GeoServerResourceStreamLocatorTest {

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
