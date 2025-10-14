/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import static org.geoserver.data.test.CiteTestData.BRIDGES;
import static org.geoserver.data.test.CiteTestData.BUILDINGS;
import static org.geoserver.data.test.CiteTestData.CITE_PREFIX;
import static org.geoserver.data.test.CiteTestData.FORESTS;
import static org.geoserver.data.test.CiteTestData.LAKES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.List;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.api.filter.Filter;
import org.junit.After;
import org.junit.Test;

public class CascadeDeleteVisitorTest extends CascadeVisitorAbstractTest {

    @After
    public void resetChanges() throws IOException {
        // add layers
        revertLayer(LAKES);
        revertLayer(BRIDGES);
        revertLayer(FORESTS);
        revertLayer(BUILDINGS);

        Catalog catalog = getCatalog();
        StyleInfo style = catalog.getStyleByName(WS_STYLE);
        if (style != null) {
            catalog.remove(style);
        }
        LayerGroupInfo group = catalog.getLayerGroupByName(NEST_GROUP);
        if (group != null) {
            catalog.remove(group);
        }
        group = catalog.getLayerGroupByName(LAKES_GROUP);
        if (group != null) {
            catalog.remove(group);
        }

        setupExtras(getTestData(), catalog);
    }

    @Test
    public void testCascadeLayer() {
        Catalog catalog = getCatalog();
        String name = toString(LAKES);
        LayerInfo layer = catalog.getLayerByName(name);
        assertNotNull(layer);

        CascadeDeleteVisitor visitor = new CascadeDeleteVisitor(catalog);
        visitor.visit(layer);

        LayerGroupInfo group = catalog.getLayerGroupByName(LAKES_GROUP);
        assertEquals(2, group.getLayers().size());
        assertFalse(group.getLayers().contains(layer));
    }

    @Test
    public void testCascadeLayerGroup() {
        Catalog catalog = getCatalog();
        LayerGroupInfo layerGroup = catalog.getLayerGroupByName(LAKES_GROUP);
        assertNotNull(layerGroup);

        CascadeDeleteVisitor visitor = new CascadeDeleteVisitor(catalog);
        visitor.visit(layerGroup);

        LayerGroupInfo nestedGroup = catalog.getLayerGroupByName(NEST_GROUP);
        assertNotNull(nestedGroup);
        assertEquals(1, nestedGroup.getLayers().size());
        assertEquals(1, nestedGroup.getStyles().size());
    }

    @Test
    public void testCascadeLayerDuplicate() {
        Catalog catalog = getCatalog();
        String name = toString(LAKES);
        LayerInfo layer = catalog.getLayerByName(name);
        assertNotNull(layer);

        LayerGroupInfo group = catalog.getLayerGroupByName(LAKES_GROUP);
        group.getLayers().add(layer);
        group.getStyles().add(null);
        catalog.save(group);

        CascadeDeleteVisitor visitor = new CascadeDeleteVisitor(catalog);
        visitor.visit(layer);

        group = catalog.getLayerGroupByName(LAKES_GROUP);
        assertEquals(2, group.getLayers().size());
        assertFalse(group.getLayers().contains(layer));
    }

    @Test
    public void testCascadeStore() {
        Catalog catalog = getCatalog();
        DataStoreInfo store = (DataStoreInfo)
                catalog.getLayerByName(getLayerId(LAKES)).getResource().getStore();
        new CascadeDeleteVisitor(catalog).visit(store);

        // that store actually holds all layers, so check we got empty
        assertEquals(0, catalog.count(LayerInfo.class, Filter.INCLUDE));
        assertEquals(0, catalog.count(ResourceInfo.class, Filter.INCLUDE));
        assertEquals(0, catalog.count(StoreInfo.class, Filter.INCLUDE));
        assertEquals(0, catalog.count(LayerGroupInfo.class, Filter.INCLUDE));
    }

    @Test
    public void testCascadeWorkspace() {
        Catalog catalog = getCatalog();
        WorkspaceInfo ws = catalog.getWorkspaceByName(CITE_PREFIX);
        new CascadeDeleteVisitor(catalog).visit(ws);

        // check the namespace is also gone
        assertNull(catalog.getNamespaceByPrefix(CITE_PREFIX));

        // that workspace actually holds all layers, so check we got empty
        assertEquals(0, catalog.count(LayerInfo.class, Filter.INCLUDE));
        assertEquals(0, catalog.count(ResourceInfo.class, Filter.INCLUDE));
        assertEquals(0, catalog.count(StoreInfo.class, Filter.INCLUDE));
        assertEquals(0, catalog.count(LayerGroupInfo.class, Filter.INCLUDE));

        // the workspace specific style is also gone
        assertEquals(0, catalog.getStylesByWorkspace(CITE_PREFIX).size());
        assertNull(catalog.getStyleByName(WS_STYLE));
    }

    @Test
    public void testCascadeStyle() {
        Catalog catalog = getCatalog();
        StyleInfo style = catalog.getStyleByName(LAKES.getLocalPart());
        assertNotNull(style);

        new CascadeDeleteVisitor(catalog).visit(style);
        assertNull(catalog.getStyleByName(LAKES.getLocalPart()));
        LayerInfo layer = catalog.getLayerByName(getLayerId(LAKES));
        assertEquals("polygon", layer.getDefaultStyle().getName());
    }

    @Test
    public void testCascadeLayerInGroupStyles() {

        Catalog catalog = getCatalog();

        LayerGroupInfo group = catalog.getLayerGroupByName(LAKES_GROUP);
        LayerGroupStyle groupStyle = new LayerGroupStyleImpl();
        StyleInfo groupStyleName = new StyleInfoImpl(catalog);
        groupStyleName.setName("cascadeDeleteTest");
        groupStyle.setName(groupStyleName);

        String name = toString(LAKES);
        String bridges = toString(BRIDGES);

        LayerInfo layer = catalog.getLayerByName(name);
        assertNotNull(layer);
        groupStyle.getLayers().add(layer);

        LayerInfo bridgesLi = catalog.getLayerByName(bridges);
        groupStyle.getLayers().add(bridgesLi);

        groupStyle.getStyles().add(null);
        groupStyle.getStyles().add(null);

        group.getLayerGroupStyles().add(groupStyle);
        catalog.save(group);

        CascadeDeleteVisitor visitor = new CascadeDeleteVisitor(catalog);
        visitor.visit(layer);

        group = catalog.getLayerGroupByName(LAKES_GROUP);
        List<PublishedInfo> styleLayers = group.getLayerGroupStyles().get(0).getLayers();
        assertEquals(1, styleLayers.size());
        assertEquals(bridgesLi, styleLayers.get(0));
        visitor.visit(bridgesLi);

        group = catalog.getLayerGroupByName(LAKES_GROUP);
        assertEquals(0, group.getLayerGroupStyles().size());
    }

    @Test
    public void testCascadeStylesInLayerGroupStyles() {

        Catalog catalog = getCatalog();

        LayerGroupInfo group = catalog.getLayerGroupByName(LAKES_GROUP);
        LayerGroupStyle groupStyle = new LayerGroupStyleImpl();
        StyleInfo groupStyleName = new StyleInfoImpl(catalog);
        groupStyleName.setName("cascadeDeleteTest");
        groupStyle.setName(groupStyleName);

        String name = toString(LAKES);

        LayerInfo layer = catalog.getLayerByName(name);
        StyleInfo lakeStyle = catalog.getStyleByName(WS_STYLE);
        layer.getStyles().add(lakeStyle);
        catalog.save(layer);
        layer = catalog.getLayerByName(name);

        assertNotNull(layer);

        groupStyle.getLayers().add(layer);
        groupStyle.getLayers().add(layer);

        groupStyle.getStyles().add(lakeStyle);
        groupStyle.getStyles().add(lakeStyle);

        group.getLayerGroupStyles().add(groupStyle);
        catalog.save(group);

        CascadeDeleteVisitor visitor = new CascadeDeleteVisitor(catalog);
        visitor.visit(lakeStyle);

        group = catalog.getLayerGroupByName(LAKES_GROUP);
        List<StyleInfo> styleLayers = group.getLayerGroupStyles().get(0).getStyles();
        assertEquals(2, styleLayers.size());
        assertEquals(LAKES.getLocalPart(), styleLayers.get(0).getName());
        assertEquals(LAKES.getLocalPart(), styleLayers.get(0).getName());
    }
}
