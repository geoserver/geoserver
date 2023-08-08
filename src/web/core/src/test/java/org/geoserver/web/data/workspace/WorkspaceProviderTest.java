/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.workspace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.junit.Before;
import org.junit.Test;

public class WorkspaceProviderTest {

    private WorkspaceProvider provider;

    private Catalog catalog;

    private WorkspaceInfo w1, w2, w3, w4;
    private SettingsInfo settings;

    @Before
    @SuppressWarnings("serial")
    public void setUp() {
        catalog = new CatalogImpl();
        w1 = add("w1");
        w2 = add("w2");
        w3 = add("w3");
        w4 = add("w4");
        settings = new SettingsInfoImpl();
        provider =
                new WorkspaceProvider() {
                    @Override
                    public Catalog getCatalog() {
                        return catalog;
                    }

                    @Override
                    protected SettingsInfo getSettings() {
                        return settings;
                    }
                };
    }

    private WorkspaceInfo add(String name) {
        WorkspaceInfo w = catalog.getFactory().createWorkspace();
        OwsUtils.set(w, "id", name + "-id");
        w.setName(name);
        catalog.add(w);
        return catalog.getWorkspaceByName(name);
    }

    @Test
    public void testSize() {
        assertEquals(4, provider.size());
        provider.setKeywords(new String[] {"w2"});
        assertEquals(1, provider.size());
    }

    @Test
    public void testFullSize() {
        assertEquals(4, provider.fullSize());
        provider.setKeywords(new String[] {"w2"});
        assertEquals(4, provider.fullSize());
    }

    @Test
    public void testGetProperties() {
        assertNotSame(WorkspaceProvider.PROPERTIES, provider.getProperties());
        assertEquals(WorkspaceProvider.PROPERTIES, provider.getProperties());

        List<Property<WorkspaceInfo>> expected = new ArrayList<>(WorkspaceProvider.PROPERTIES);

        settings.setShowCreatedTimeColumnsInAdminList(true);
        expected.add(WorkspaceProvider.CREATED_TIMESTAMP);
        assertEquals(expected, provider.getProperties());

        settings.setShowModifiedTimeColumnsInAdminList(true);
        expected.add(WorkspaceProvider.MODIFIED_TIMESTAMP);
        assertEquals(expected, provider.getProperties());
    }

    @Test
    public void testGetItems_is_unsupported() {
        assertThrows(UnsupportedOperationException.class, provider::getItems);
    }

    @Test
    public void testIterator_preconditions() {
        final Class<IllegalArgumentException> iae = IllegalArgumentException.class;
        assertThrows(iae, () -> provider.iterator(Integer.MAX_VALUE + 1L, 1L));
        assertThrows(iae, () -> provider.iterator(Integer.MIN_VALUE - 1L, 1L));
        assertThrows(iae, () -> provider.iterator(0, Integer.MAX_VALUE + 1L));
        assertThrows(iae, () -> provider.iterator(0, Integer.MIN_VALUE - 1L));
    }

    @Test
    public void testIterator_all() {
        List<WorkspaceInfo> expected = List.of(w1, w2, w3, w4);
        List<WorkspaceInfo> actual = Lists.newArrayList(provider.iterator(0, 25));
        assertEquals(expected, actual);
    }

    @Test
    public void testIterator_orderbyName() {
        provider.setSort("name", SortOrder.DESCENDING);
        List<WorkspaceInfo> actual = Lists.newArrayList(provider.iterator(0, 25));
        assertEquals(List.of(w4, w3, w2, w1), actual);
    }

    @Test
    public void testIterator_orderbyIsolated() {
        w2.setIsolated(true);
        catalog.save(w2);

        w3.setIsolated(true);
        catalog.save(w3);

        provider.setSort("isolated", SortOrder.ASCENDING);
        List<WorkspaceInfo> actual = Lists.newArrayList(provider.iterator(0, 25));
        assertEquals(List.of(w1, w4, w2, w3), actual);

        provider.setSort("isolated", SortOrder.DESCENDING);
        actual = Lists.newArrayList(provider.iterator(0, 25));
        assertEquals(List.of(w2, w3, w1, w4), actual);
    }

    @Test
    public void testIterator_orderbyDefault() {
        catalog.setDefaultWorkspace(w3);

        provider.setSort("default", SortOrder.ASCENDING);
        List<WorkspaceInfo> actual = Lists.newArrayList(provider.iterator(0, 25));
        assertEquals(List.of(w3, w1, w2, w4), actual);

        provider.setSort("default", SortOrder.DESCENDING);
        actual = Lists.newArrayList(provider.iterator(0, 25));
        assertEquals(List.of(w1, w2, w4, w3), actual);
    }

    @Test
    public void testOrderByDefaultProperty() {
        catalog.setDefaultWorkspace(w3);
        provider.setSort("name", SortOrder.ASCENDING);
        List<WorkspaceInfo> actual = Lists.newArrayList(provider.iterator(0, 25));
        assertEquals(4, actual.size());
        WorkspaceInfo decoratedWithDefault = actual.get(2);
        assertEquals(w3.getId(), decoratedWithDefault.getId());

        assertEquals(
                Boolean.TRUE, WorkspaceProvider.DEFAULT.getPropertyValue(decoratedWithDefault));
        assertEquals(Boolean.FALSE, WorkspaceProvider.DEFAULT.getPropertyValue(actual.get(0)));
        assertEquals(Boolean.FALSE, WorkspaceProvider.DEFAULT.getPropertyValue(actual.get(1)));
        assertEquals(Boolean.FALSE, WorkspaceProvider.DEFAULT.getPropertyValue(actual.get(3)));
    }
}
