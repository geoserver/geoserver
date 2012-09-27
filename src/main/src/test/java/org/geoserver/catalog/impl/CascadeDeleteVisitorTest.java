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
import org.geoserver.data.test.MockCreator;
import org.geoserver.data.test.MockTestData;
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
        setMockCreator(new MockCreator() {
            @Override
            public Catalog createCatalog(MockTestData testData) {
                Catalog cat = createMock(Catalog.class);
                LayerGroupInfo lg = setUpMockLayerGroup(cat);
                cat.save(lg);
                expectLastCall();

                cat.remove(lg.getLayers().get(0));
                expectLastCall();

                cat.remove(lg.getLayers().get(0).getResource());
                expectLastCall();

                replay(cat);
                return cat;
            }
        });

        Catalog catalog = getCatalog();

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
        setMockCreator(new MockCreator() {
            @Override
            public Catalog createCatalog(MockTestData testData) {
                Catalog cat = createMock(Catalog.class);

                DataStoreInfo ds = createMock(DataStoreInfo.class);
                expect(cat.getStoreByName(CITE_PREFIX, DataStoreInfo.class)).andReturn(ds);

                ResourceInfo r1 = createMock(ResourceInfo.class);
                LayerInfo l1 = createMock(LayerInfo.class);
                l1.accept((CatalogVisitor)anyObject());
                expectLastCall();

                expect(cat.getResourcesByStore(ds, ResourceInfo.class)).andReturn(Arrays.asList(r1));
                expect(cat.getLayers(r1)).andReturn(Arrays.asList(l1));
                expect(cat.getLayer(null)).andReturn(l1);
                
                cat.remove(ds);
                expectLastCall();

                replay(ds, r1, l1, cat);
                return cat;
            }
        });

        Catalog catalog = getCatalog();
        DataStoreInfo ds = catalog.getStoreByName(CITE_PREFIX, DataStoreInfo.class);
        new CascadeDeleteVisitor(catalog).visit(ds);

        LayerInfo l = catalog.getLayer(null);
        verify(catalog, l);
    }

    @Test
    public void testCascadeWorkspace() {
        setMockCreator(new MockCreator() {
            @Override
            public Catalog createCatalog(MockTestData testData) {
                Catalog cat = createMock(Catalog.class);

                WorkspaceInfo ws = createMock(WorkspaceInfo.class);
                expect(ws.getName()).andReturn(CITE_PREFIX).anyTimes();
                expect(cat.getWorkspaceByName(CITE_PREFIX)).andReturn(ws).anyTimes();

                NamespaceInfo ns = createMock(NamespaceInfo.class);
                expect(cat.getNamespaceByPrefix(CITE_PREFIX)).andReturn(ns).anyTimes();
                
                StoreInfo s1 = createMock(StoreInfo.class);
                StoreInfo s2 = createMock(StoreInfo.class);
                expect(cat.getStoresByWorkspace(ws, StoreInfo.class)).andReturn(Arrays.asList(s1, s2)).anyTimes();

                ns.accept((CatalogVisitor)anyObject());
                expectLastCall();

                s1.accept((CatalogVisitor)anyObject());
                expectLastCall();

                s2.accept((CatalogVisitor)anyObject());
                expectLastCall();

                cat.remove(ws);
                expectLastCall();

                replay(ws, ns, s1, s2, cat);
                return cat;
            }
        });

        Catalog catalog = getCatalog();
        WorkspaceInfo ws = catalog.getWorkspaceByName(CITE_PREFIX);
        new CascadeDeleteVisitor(catalog).visit(ws);

        verify(catalog.getNamespaceByPrefix(CITE_PREFIX));
        for (StoreInfo s : catalog.getStoresByWorkspace(ws, StoreInfo.class)) {
            verify(s);
        }

        verify(catalog);
    }

    @Test
    public void testCascadeStyle() {
        setMockCreator(new MockCreator() {
            @Override
            public Catalog createCatalog(MockTestData testData) {
                Catalog cat = createMock(Catalog.class);

                String styleName = LAKES.getLocalPart();
                StyleInfo style = createMock(StyleInfo.class);
                expect(style.getName()).andReturn(styleName).anyTimes();
                expect(cat.getStyleByName(styleName)).andReturn(style).anyTimes();
                
                LayerInfo lakes = createMock(LayerInfo.class);
                expect(lakes.getDefaultStyle()).andReturn(style).anyTimes();
                expect(lakes.getStyles()).andReturn(new HashSet()).anyTimes();

                FeatureTypeInfo lakesFt = createNiceMock(FeatureTypeInfo.class);
                expect(lakes.getResource()).andReturn(lakesFt).anyTimes();
                
                lakes.setDefaultStyle((StyleInfo)anyObject());
                expectLastCall();

                cat.save(lakes);
                expectLastCall();

                LayerInfo buildings = createMock(LayerInfo.class);
                expect(buildings.getDefaultStyle()).andReturn(null).anyTimes();
                expect(buildings.getStyles()).andReturn(new HashSet<StyleInfo>(Arrays.asList(style))).anyTimes();

                cat.save(buildings);
                expectLastCall();

                expect(cat.getLayers()).andReturn(Arrays.asList(lakes, buildings)).anyTimes();
                expect(cat.getLayerGroups()).andReturn(new ArrayList());

                cat.remove(style);
                expectLastCall();

                replay(style, lakesFt, lakes, buildings, cat);
                return cat;
            }
        });

        Catalog catalog = getCatalog();
        StyleInfo style = catalog.getStyleByName(LAKES.getLocalPart());

        new CascadeDeleteVisitor(catalog).visit(style);

        for (LayerInfo l : catalog.getLayers()) {
            verify(l);
        }
        verify(catalog);
    }
}


