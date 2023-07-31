/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.geoserver.catalog.NamespaceInfo;
import org.junit.Before;
import org.junit.Test;

/** Test suite for {@link NamespaceInfoLookup} */
public class NamespaceInfoLookupTest {

    private static final String URI_1 = "http://gs.test.com/ns1";
    private static final String URI_2 = "http://gs.test.com/ns2";

    private NamespaceInfo uri1_1, uri1_2, uri2_1, uri2_2;

    private NamespaceInfoLookup lookup;

    @Before
    public void setUp() {
        uri1_1 = create("uri1_1", URI_1);
        uri1_2 = create("uri1_2", URI_1);
        uri2_1 = create("uri2_1", URI_2);
        uri2_2 = create("uri2_2", URI_2);
        lookup = new NamespaceInfoLookup();
    }

    private NamespaceInfo create(String prefix, String uri) {
        NamespaceInfoImpl ns = new NamespaceInfoImpl();
        ns.setId(prefix + "-id");
        ns.setPrefix(prefix);
        ns.setURI(uri);
        return ns;
    }

    private void addAll(NamespaceInfo... values) {
        for (NamespaceInfo ns : values) lookup.add(ns);
    }

    @Test
    public void testAdd() {
        lookup.add(uri1_2);
        assertEquals(List.of(uri1_2), lookup.valueList(URI_1, false));

        lookup.add(uri1_1);
        assertEquals(List.of(uri1_1, uri1_2), lookup.valueList(URI_1, false));

        assertSame(uri1_1, lookup.findById(uri1_1.getId(), NamespaceInfo.class));
        assertSame(uri1_2, lookup.findById(uri1_2.getId(), NamespaceInfo.class));
    }

    @Test
    public void testClear() {
        addAll(uri1_1, uri1_2, uri2_1, uri2_2);
        lookup.clear();
        assertEquals(List.of(), lookup.values());
    }

    @Test
    public void testRemove() {
        addAll(uri1_1, uri1_2, uri2_1, uri2_2);
        testRemove(uri1_1);
        testRemove(uri1_2);
        testRemove(uri2_1);
        testRemove(uri2_2);
        lookup.remove(uri1_1);
    }

    private void testRemove(NamespaceInfo ns) {
        NamespaceInfo current = lookup.findById(ns.getId(), NamespaceInfo.class);
        NamespaceInfo removed = lookup.remove(ns);
        assertSame(current, removed);
        assertNull(lookup.findById(ns.getId(), NamespaceInfo.class));
    }

    @Test
    public void testUpdate() {
        addAll(uri1_1, uri1_2, uri2_1, uri2_2);

        testUpdate(uri1_1, URI_2, List.of(uri1_1, uri2_1, uri2_2));
        testUpdate(uri2_2, URI_1, List.of(uri1_2, uri2_2));
    }

    private void testUpdate(NamespaceInfo ns, String newUri, List<NamespaceInfo> expected) {
        String oldUri = ns.getURI();
        assertTrue(lookup.valueList(oldUri, false).contains(ns));

        NamespaceInfo proxied = ModificationProxy.create(ns, NamespaceInfo.class);
        proxied.setURI(newUri);
        lookup.update(proxied);
        ModificationProxy.handler(proxied).commit();
        assertEquals(expected, lookup.valueList(newUri, false));

        assertFalse(lookup.valueList(oldUri, false).contains(ns));
    }

    @Test
    public void testFindAllByUri() {
        assertTrue(lookup.findAllByUri(URI_1).isEmpty());
        assertTrue(lookup.findAllByUri(URI_2).isEmpty());

        lookup.add(uri1_1);
        assertEquals(List.of(uri1_1), lookup.findAllByUri(URI_1));

        lookup.add(uri2_1);
        assertEquals(List.of(uri1_1), lookup.findAllByUri(URI_1));
        assertEquals(List.of(uri2_1), lookup.findAllByUri(URI_2));

        lookup.add(uri1_2);
        lookup.add(uri2_2);
        assertEquals(List.of(uri1_1, uri1_2), lookup.findAllByUri(URI_1));
        assertEquals(List.of(uri2_1, uri2_2), lookup.findAllByUri(URI_2));
    }

    @Test
    public void testFindAllByUri_stable_order() {
        addAll(uri1_1, uri1_2);

        final List<NamespaceInfo> expected = List.of(uri1_1, uri1_2);
        assertEquals(expected, lookup.findAllByUri(URI_1));

        lookup.clear();
        assertTrue(lookup.findAllByUri(URI_1).isEmpty());

        addAll(uri1_2, uri1_1);
        assertEquals(expected, lookup.findAllByUri(URI_1));
    }
}
