/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.geoserver.data.test.CiteTestData.CITE_PREFIX;
import static org.geoserver.data.test.CiteTestData.FORESTS;
import static org.geoserver.data.test.CiteTestData.LAKES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.test.GeoServerMockTestSupport;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.junit.Test;

@TestSetup(run=TestSetupFrequency.REPEAT)
public class CascadeDeleteVisitorTest extends GeoServerMockTestSupport {
    static final String LAKES_GROUP = "lakesGroup";
    
    LayerGroupInfo setUpMockLayerGroup(final Catalog catalog) {
        LayerGroupInfo lg = createMock(LayerGroupInfo.class);
        expect(lg.getName()).andReturn(LAKES_GROUP).anyTimes();
        
        LayerInfo lakes = createNiceMock(LayerInfo.class);
        expect(lakes.getResource()).andReturn(createNiceMock(ResourceInfo.class)).anyTimes();
        expect(catalog.getLayerByName(toString(LAKES))).andReturn(lakes).anyTimes();
        
        LayerInfo forests = createNiceMock(LayerInfo.class);
        expect(forests.getResource()).andReturn(createNiceMock(ResourceInfo.class)).anyTimes();
        expect(catalog.getLayerByName(toString(FORESTS))).andReturn(forests).anyTimes();
        
        LayerInfo bridges = createNiceMock(LayerInfo.class);
        expect(bridges.getResource()).andReturn(createNiceMock(ResourceInfo.class)).anyTimes();
        expect(catalog.getLayerByName(toString(FORESTS))).andReturn(bridges).anyTimes();
        
        expect(lg.getLayers()).andReturn(new ArrayList(Arrays.asList(lakes, forests, bridges))).anyTimes();
        expect(lg.getStyles()).andReturn(new ArrayList(Arrays.asList(null, null, null))).anyTimes();
        
        expect(catalog.getLayerGroupByName(LAKES_GROUP)).andReturn(lg).anyTimes();
        replay(lakes, forests, bridges, lg);

        expect(catalog.getLayerGroups()).andReturn(Arrays.asList(lg));
        
        return lg;
    }

    @Test
    public void testCascadeLayer() {
        Catalog catalog = createMock(Catalog.class);
        LayerGroupInfo lg = setUpMockLayerGroup(catalog);
        catalog.save(lg);
        expectLastCall();

        catalog.remove((LayerInfo) lg.getLayers().get(0));
        expectLastCall();

        catalog.remove(((LayerInfo) lg.getLayers().get(0)).getResource());
        expectLastCall();

        replay(catalog);

        String name = toString(LAKES);
        LayerInfo layer = catalog.getLayerByName(name);
        assertNotNull(layer);

        CascadeDeleteVisitor visitor = new CascadeDeleteVisitor(catalog);
        visitor.visit(layer);
        verify(catalog);

        LayerGroupInfo group = catalog.getLayerGroupByName(LAKES_GROUP);
        assertEquals(2, group.getLayers().size());
        assertFalse(group.getLayers().contains(layer));
    }

    @Test
    public void testCascadeStore() {
        Catalog catalog = createMock(Catalog.class);

        DataStoreInfo ds = createMock(DataStoreInfo.class);

        ResourceInfo r1 = createMock(ResourceInfo.class);
        LayerInfo l1 = createMock(LayerInfo.class);
        l1.accept((CatalogVisitor)anyObject());
        expectLastCall();

        expect(catalog.getResourcesByStore(ds, ResourceInfo.class)).andReturn(Arrays.asList(r1));
        expect(catalog.getLayers(r1)).andReturn(Arrays.asList(l1));
        expect(catalog.getLayer(null)).andReturn(l1);
        
        catalog.remove(ds);
        expectLastCall();

        replay(ds, r1, l1, catalog);

        new CascadeDeleteVisitor(catalog).visit(ds);

        LayerInfo l = catalog.getLayer(null);
        verify(catalog, l);
    }

    @Test
    public void testCascadeWorkspace() {
        Catalog catalog = createMock(Catalog.class);

        WorkspaceInfo ws = createMock(WorkspaceInfo.class);
        expect(ws.getName()).andReturn(CITE_PREFIX).anyTimes();
        expect(catalog.getWorkspaceByName(CITE_PREFIX)).andReturn(ws).anyTimes();

        NamespaceInfo ns = createMock(NamespaceInfo.class);
        expect(catalog.getNamespaceByPrefix(CITE_PREFIX)).andReturn(ns).anyTimes();
        
        StoreInfo s1 = createMock(StoreInfo.class);
        StoreInfo s2 = createMock(StoreInfo.class);
        expect(catalog.getStoresByWorkspace(ws, StoreInfo.class)).andReturn(Arrays.asList(s1, s2)).anyTimes();

        ns.accept((CatalogVisitor)anyObject());
        expectLastCall();

        s1.accept((CatalogVisitor)anyObject());
        expectLastCall();

        s2.accept((CatalogVisitor)anyObject());
        expectLastCall();

        catalog.remove(ws);
        expectLastCall();

        replay(ws, ns, s1, s2, catalog);

        new CascadeDeleteVisitor(catalog).visit(ws);

        verify(catalog.getNamespaceByPrefix(CITE_PREFIX));
        for (StoreInfo s : catalog.getStoresByWorkspace(ws, StoreInfo.class)) {
            verify(s);
        }

        verify(catalog);
    }

    private StyleInfo createMockStyle(Catalog catalog, String styleName) {
        StyleInfo style = createMock(StyleInfo.class);
        expect(style.getName()).andReturn(styleName).anyTimes();
        expect(catalog.getStyleByName(styleName)).andReturn(style).anyTimes();
        return style;
    }
    
    @Test
    public void testCascadeStyle() {
        Catalog catalog = createMock(Catalog.class);

        createMockStyle(catalog, StyleInfo.DEFAULT_POINT);
        StyleInfo style = createMockStyle(catalog, LAKES.getLocalPart());
        
        LayerInfo lakes = createMock(LayerInfo.class);
        expect(lakes.getDefaultStyle()).andReturn(style).anyTimes();
        expect(lakes.getStyles()).andReturn(new HashSet()).anyTimes();

        FeatureTypeInfo lakesFt = createNiceMock(FeatureTypeInfo.class);
        expect(lakes.getResource()).andReturn(lakesFt).anyTimes();
        
        lakes.setDefaultStyle((StyleInfo)anyObject());
        expectLastCall();

        catalog.save(lakes);
        expectLastCall();

        LayerInfo buildings = createMock(LayerInfo.class);
        expect(buildings.getDefaultStyle()).andReturn(null).anyTimes();
        expect(buildings.getStyles()).andReturn(new HashSet<StyleInfo>(Arrays.asList(style))).anyTimes();

        catalog.save(buildings);
        expectLastCall();

        expect(catalog.getLayers()).andReturn(Arrays.asList(lakes, buildings)).anyTimes();
        expect(catalog.getLayerGroups()).andReturn(new ArrayList());

        catalog.remove(style);
        expectLastCall();

        replay(style, lakesFt, lakes, buildings, catalog);
        
        new CascadeDeleteVisitor(catalog).visit(style);

        for (LayerInfo l : catalog.getLayers()) {
            verify(l);
        }
        verify(catalog);
    }
}


