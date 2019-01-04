/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import org.geoserver.catalog.*;
import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.impl.LayerGroupContainmentCache.LayerGroupSummary;
import org.geotools.data.property.PropertyDataStore;
import org.geotools.util.URLs;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.feature.type.Name;

/** Tests {@link LayerGroupContainmentCache} udpates in face of catalog setup and changes */
public class LayerGroupContainmentCacheTest {

    private static final String WS = "ws";

    private static final String ANOTHER_WS = "anotherWs";

    private static final String NATURE_GROUP = "nature";

    private static final String CONTAINER_GROUP = "containerGroup";

    private LayerGroupContainmentCache cc;

    private LayerGroupInfo nature;

    private LayerGroupInfo container;

    private static Catalog catalog;

    @BeforeClass
    public static void setupBaseCatalog() throws Exception {
        catalog = new CatalogImpl();
        catalog.setResourceLoader(new GeoServerResourceLoader());

        // the workspace
        addWorkspaceNamespace(WS);
        addWorkspaceNamespace(ANOTHER_WS);

        // the builder
        CatalogBuilder cb = new CatalogBuilder(catalog);
        final WorkspaceInfo defaultWorkspace = catalog.getDefaultWorkspace();
        cb.setWorkspace(defaultWorkspace);

        // setup the store
        String nsURI = catalog.getDefaultNamespace().getURI();
        URL buildings = MockData.class.getResource("Buildings.properties");
        File testData = URLs.urlToFile(buildings).getParentFile();
        DataStoreInfo storeInfo = cb.buildDataStore("store");
        storeInfo.getConnectionParameters().put("directory", testData);
        storeInfo.getConnectionParameters().put("namespace", nsURI);
        catalog.save(storeInfo);

        // setup all the layers
        PropertyDataStore store = new PropertyDataStore(testData);
        store.setNamespaceURI(nsURI);
        cb.setStore(catalog.getDefaultDataStore(defaultWorkspace));
        for (Name name : store.getNames()) {
            FeatureTypeInfo ft = cb.buildFeatureType(name);
            cb.setupBounds(ft);
            catalog.add(ft);
            LayerInfo layer = cb.buildLayer(ft);
            catalog.add(layer);
        }
    }

    private static void addWorkspaceNamespace(String wsName) {
        WorkspaceInfoImpl ws = new WorkspaceInfoImpl();
        ws.setName(wsName);
        catalog.add(ws);
        NamespaceInfo ns = new NamespaceInfoImpl();
        ns.setPrefix(wsName);
        ns.setURI("http://www.geoserver.org/" + wsName);
        catalog.add(ns);
    }

    @Before
    public void setupLayerGrups() throws Exception {
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        LayerInfo forests = catalog.getLayerByName(getLayerId(MockData.FORESTS));
        LayerInfo roads = catalog.getLayerByName(getLayerId(MockData.ROAD_SEGMENTS));
        WorkspaceInfo ws = catalog.getDefaultWorkspace();

        this.nature = addLayerGroup(NATURE_GROUP, Mode.SINGLE, ws, lakes, forests);
        this.container = addLayerGroup(CONTAINER_GROUP, Mode.CONTAINER, null, nature, roads);

        cc = new LayerGroupContainmentCache(catalog);
    }

    @After
    public void clearLayerGroups() throws Exception {
        CascadeDeleteVisitor remover = new CascadeDeleteVisitor(catalog);
        for (LayerGroupInfo lg : catalog.getLayerGroups()) {
            if (catalog.getLayerGroup(lg.getId()) != null) {
                remover.visit(lg);
            }
        }
    }

    private LayerGroupInfo addLayerGroup(
            String name, Mode mode, WorkspaceInfo ws, PublishedInfo... layers) throws Exception {
        CatalogBuilder cb = new CatalogBuilder(catalog);

        LayerGroupInfo group = catalog.getFactory().createLayerGroup();
        group.setName(name);
        group.setMode(mode);
        if (ws != null) {
            group.setWorkspace(ws);
        }
        if (layers != null) {
            for (PublishedInfo layer : layers) {
                group.getLayers().add(layer);
                group.getStyles().add(null);
            }
        }
        cb.calculateLayerGroupBounds(group);
        catalog.add(group);
        if (ws != null) {
            return catalog.getLayerGroupByName(ws.getName(), name);
        } else {
            return catalog.getLayerGroupByName(name);
        }
    }

    private Set<String> set(String... names) {
        if (names == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(Arrays.asList(names));
    }

    private Set<String> containerNamesForGroup(LayerGroupInfo lg) {
        Collection<LayerGroupSummary> summaries = cc.getContainerGroupsFor(lg);
        return summaries.stream().map(gs -> gs.prefixedName()).collect(Collectors.toSet());
    }

    private Set<String> containerNamesForResource(QName name) {
        Collection<LayerGroupSummary> summaries = cc.getContainerGroupsFor(getResource(name));
        return summaries.stream().map(gs -> gs.prefixedName()).collect(Collectors.toSet());
    }

    private FeatureTypeInfo getResource(QName name) {
        return catalog.getResourceByName(getLayerId(name), FeatureTypeInfo.class);
    }

    private String getLayerId(QName name) {
        return "ws:" + name.getLocalPart();
    }

    @Test
    public void testInitialSetup() throws Exception {
        // nature
        Collection<LayerGroupSummary> natureContainers = cc.getContainerGroupsFor(nature);
        assertEquals(1, natureContainers.size());
        assertThat(natureContainers, contains(new LayerGroupSummary(container)));
        LayerGroupSummary summary = natureContainers.iterator().next();
        assertNull(summary.getWorkspace());
        assertEquals(CONTAINER_GROUP, summary.getName());
        assertThat(summary.getContainerGroups(), empty());

        // container has no contaning groups
        assertThat(cc.getContainerGroupsFor(container), empty());

        // now check the groups containing the layers (nature being SINGLE, not a container)
        assertThat(containerNamesForResource(MockData.LAKES), equalTo(set(CONTAINER_GROUP)));
        assertThat(containerNamesForResource(MockData.FORESTS), equalTo(set(CONTAINER_GROUP)));
        assertThat(
                containerNamesForResource(MockData.ROAD_SEGMENTS), equalTo(set(CONTAINER_GROUP)));
    }

    @Test
    public void testAddLayerToNature() throws Exception {
        LayerInfo neatline = catalog.getLayerByName(getLayerId(MockData.MAP_NEATLINE));
        nature.getLayers().add(neatline);
        nature.getStyles().add(null);
        catalog.save(nature);

        assertThat(containerNamesForResource(MockData.MAP_NEATLINE), equalTo(set(CONTAINER_GROUP)));
    }

    @Test
    public void testAddLayerToContainer() throws Exception {
        LayerInfo neatline = catalog.getLayerByName(getLayerId(MockData.MAP_NEATLINE));
        container.getLayers().add(neatline);
        container.getStyles().add(null);
        catalog.save(container);

        assertThat(containerNamesForResource(MockData.MAP_NEATLINE), equalTo(set(CONTAINER_GROUP)));
    }

    @Test
    public void testRemoveLayerFromNature() throws Exception {
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        nature.getLayers().remove(lakes);
        nature.getStyles().remove(0);
        catalog.save(nature);

        assertThat(containerNamesForResource(MockData.LAKES), empty());
        assertThat(containerNamesForResource(MockData.FORESTS), equalTo(set(CONTAINER_GROUP)));
        assertThat(
                containerNamesForResource(MockData.ROAD_SEGMENTS), equalTo(set(CONTAINER_GROUP)));
    }

    @Test
    public void testRemoveLayerFromContainer() throws Exception {
        LayerInfo roads = catalog.getLayerByName(getLayerId(MockData.ROAD_SEGMENTS));
        container.getLayers().remove(roads);
        container.getStyles().remove(0);
        catalog.save(container);

        assertThat(containerNamesForResource(MockData.LAKES), equalTo(set(CONTAINER_GROUP)));
        assertThat(containerNamesForResource(MockData.FORESTS), equalTo(set(CONTAINER_GROUP)));
        assertThat(containerNamesForResource(MockData.ROAD_SEGMENTS), empty());
    }

    @Test
    public void testRemoveNatureFromContainer() throws Exception {
        container.getLayers().remove(nature);
        container.getStyles().remove(0);
        catalog.save(container);

        assertThat(containerNamesForGroup(nature), empty());
        assertThat(containerNamesForResource(MockData.LAKES), empty());
        assertThat(containerNamesForResource(MockData.FORESTS), empty());
        assertThat(
                containerNamesForResource(MockData.ROAD_SEGMENTS), equalTo(set(CONTAINER_GROUP)));
    }

    @Test
    public void testRemoveAllGrups() throws Exception {
        catalog.remove(container);
        catalog.remove(nature);

        assertThat(containerNamesForGroup(nature), empty());
        assertThat(containerNamesForResource(MockData.LAKES), empty());
        assertThat(containerNamesForResource(MockData.FORESTS), empty());
        assertThat(containerNamesForResource(MockData.ROAD_SEGMENTS), empty());
    }

    @Test
    public void testAddRemoveNamed() throws Exception {
        final String NAMED_GROUP = "named";
        LayerInfo neatline = catalog.getLayerByName(getLayerId(MockData.MAP_NEATLINE));
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));

        // add and check containment
        LayerGroupInfo named = addLayerGroup(NAMED_GROUP, Mode.NAMED, null, lakes, neatline);
        assertThat(
                containerNamesForResource(MockData.LAKES),
                equalTo(set(CONTAINER_GROUP, NAMED_GROUP)));
        assertThat(containerNamesForResource(MockData.MAP_NEATLINE), equalTo(set(NAMED_GROUP)));
        assertThat(containerNamesForGroup(named), empty());

        // delete and check containment
        catalog.remove(named);
        assertThat(containerNamesForResource(MockData.LAKES), equalTo(set(CONTAINER_GROUP)));
        assertThat(containerNamesForResource(MockData.MAP_NEATLINE), empty());
        assertThat(containerNamesForGroup(named), empty());
    }

    @Test
    public void testAddRemoveNestedNamed() throws Exception {
        final String NESTED_NAMED = "nestedNamed";
        LayerInfo neatline = catalog.getLayerByName(getLayerId(MockData.MAP_NEATLINE));
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));

        // add, nest, and check containment
        LayerGroupInfo nestedNamed = addLayerGroup(NESTED_NAMED, Mode.NAMED, null, lakes, neatline);
        container.getLayers().add(nestedNamed);
        container.getStyles().add(null);
        catalog.save(container);
        assertThat(
                containerNamesForResource(MockData.LAKES),
                equalTo(set(CONTAINER_GROUP, NESTED_NAMED)));
        assertThat(
                containerNamesForResource(MockData.MAP_NEATLINE),
                equalTo(set(CONTAINER_GROUP, NESTED_NAMED)));
        assertThat(containerNamesForGroup(nestedNamed), equalTo(set(CONTAINER_GROUP)));

        // delete and check containment
        new CascadeDeleteVisitor(catalog).visit(nestedNamed);
        assertThat(containerNamesForResource(MockData.LAKES), equalTo(set(CONTAINER_GROUP)));
        assertThat(containerNamesForResource(MockData.MAP_NEATLINE), empty());
        assertThat(containerNamesForGroup(nestedNamed), empty());
    }

    @Test
    public void testRenameGroup() throws Exception {
        nature.setName("renamed");
        catalog.save(nature);

        LayerGroupSummary summary = cc.groupCache.get(nature.getId());
        assertEquals("renamed", summary.getName());
        assertEquals(WS, summary.getWorkspace());
    }

    @Test
    public void testRenameWorkspace() throws Exception {
        WorkspaceInfo ws = catalog.getDefaultWorkspace();
        ws.setName("renamed");
        try {
            catalog.save(ws);

            LayerGroupSummary summary = cc.groupCache.get(nature.getId());
            assertEquals(NATURE_GROUP, summary.getName());
            assertEquals("renamed", summary.getWorkspace());
        } finally {
            ws.setName(WS);
            catalog.save(ws);
        }
    }

    @Test
    public void testChangeWorkspace() throws Exception {
        DataStoreInfo store = catalog.getDataStores().get(0);
        try {
            WorkspaceInfo aws = catalog.getWorkspaceByName(ANOTHER_WS);
            store.setWorkspace(aws);
            catalog.save(store);
            nature.setWorkspace(aws);
            catalog.save(nature);

            LayerGroupSummary summary = cc.groupCache.get(nature.getId());
            assertEquals(NATURE_GROUP, summary.getName());
            assertEquals(ANOTHER_WS, summary.getWorkspace());
        } finally {
            WorkspaceInfo ws = catalog.getWorkspaceByName(WS);
            store.setWorkspace(ws);
            catalog.save(store);
        }
    }

    @Test
    public void testChangeGroupMode() throws Exception {
        LayerGroupSummary summary = cc.groupCache.get(nature.getId());
        assertEquals(Mode.SINGLE, summary.getMode());

        nature.setMode(Mode.OPAQUE_CONTAINER);
        catalog.save(nature);

        summary = cc.groupCache.get(nature.getId());
        assertEquals(Mode.OPAQUE_CONTAINER, summary.getMode());
    }
}
