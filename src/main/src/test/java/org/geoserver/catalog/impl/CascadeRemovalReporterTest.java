/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createNiceMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.geoserver.catalog.CascadeRemovalReporter;
import org.geoserver.catalog.CascadeRemovalReporter.ModificationType;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockCatalogBuilder;
import org.geoserver.data.test.MockCreator;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.MockTestData;
import org.geoserver.test.GeoServerMockTestSupport;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

public class CascadeRemovalReporterTest extends GeoServerMockTestSupport {
    
    static final String LAKES_GROUP = "lakesGroup";

    @Override
    protected void setUp(MockTestData testData) throws Exception {
        super.setUp(testData);
        setMockCreator(new MockCreator() {
            @Override
            protected void addToCatalog(Catalog catalog, MockCatalogBuilder b) {
                String lakes = MockData.LAKES.getLocalPart();
                String forests = MockData.FORESTS.getLocalPart();
                String bridges = MockData.BRIDGES.getLocalPart();

                b.layerGroup(LAKES_GROUP, Arrays.asList(lakes, forests, bridges), 
                    Arrays.asList(lakes, forests, bridges));
            } 
        });
    }

    public void setNativeBox(Catalog catalog, String name) throws Exception {
        FeatureTypeInfo fti = catalog.getFeatureTypeByName(name);
        fti.setNativeBoundingBox(fti.getFeatureSource(null, null).getBounds());
        fti.setLatLonBoundingBox(new ReferencedEnvelope(fti.getNativeBoundingBox(), DefaultGeographicCRS.WGS84));
        catalog.save(fti);
    }

    @Test
    public void testCascadeLayer() {
        Catalog catalog = getCatalog();
        CascadeRemovalReporter visitor = new CascadeRemovalReporter(catalog);

        String name = getLayerId(MockData.LAKES);
        LayerInfo layer = catalog.getLayerByName(name);
        assertNotNull(layer);
        visitor.visit(layer);
        //layer.accept(visitor);
        
        // we expect a layer, a resource and a group
        assertEquals(3, visitor.getObjects(null).size());
        
        // check the layer and resource have been marked to delete (and
        assertEquals(catalog.getLayerByName(name), 
                visitor.getObjects(LayerInfo.class, ModificationType.DELETE).get(0));
        assertEquals(catalog.getResourceByName(name, ResourceInfo.class), 
                visitor.getObjects(ResourceInfo.class, ModificationType.DELETE).get(0));
        
        // the group has been marked to update? (we need to compare by id as the
        // objects won't compare properly by equality)
        LayerGroupInfo group = catalog.getLayerGroupByName(LAKES_GROUP);
        assertEquals(group.getId(), visitor.getObjects(LayerGroupInfo.class, 
                ModificationType.GROUP_CHANGED).get(0).getId());
    }
    
    @Test
    public void testCascadeStore() {
        Catalog catalog = getCatalog();
        CascadeRemovalReporter visitor = new CascadeRemovalReporter(catalog);

        String citeStore = MockData.CITE_PREFIX;
        StoreInfo store = catalog.getStoreByName(citeStore, StoreInfo.class);
        String buildings = getLayerId(MockData.BUILDINGS);
        String lakes = getLayerId(MockData.LAKES);
        LayerInfo bl = catalog.getLayerByName(buildings);
        ResourceInfo br = catalog.getResourceByName(buildings, ResourceInfo.class);
        LayerInfo ll = catalog.getLayerByName(lakes);
        ResourceInfo lr = catalog.getResourceByName(lakes, ResourceInfo.class);
        
        visitor.visit((DataStoreInfo)store);
        
        assertEquals(store, visitor.getObjects(StoreInfo.class, ModificationType.DELETE).get(0));
        List<LayerInfo> layers = visitor.getObjects(LayerInfo.class, ModificationType.DELETE);
        assertTrue(layers.contains(bl));
        assertTrue(layers.contains(ll));
        List<ResourceInfo> resources = visitor.getObjects(ResourceInfo.class, ModificationType.DELETE);
        assertTrue(resources.contains(br));
        assertTrue(resources.contains(lr));
    }
    
    @Test
    public void testCascadeWorkspace() {
        Catalog catalog = getCatalog();
        CascadeRemovalReporter visitor = new CascadeRemovalReporter(catalog);

        WorkspaceInfo ws = catalog.getWorkspaceByName(MockData.CITE_PREFIX);
        assertNotNull(ws);
        List<StoreInfo> stores = getCatalog().getStoresByWorkspace(ws, StoreInfo.class);
        List<StyleInfo> styles = getCatalog().getStylesByWorkspace(ws);
        List<LayerGroupInfo> layerGroups = getCatalog().getLayerGroupsByWorkspace(ws);
        
        ws.accept(visitor);
        
        assertTrue(stores.containsAll(visitor.getObjects(StoreInfo.class, ModificationType.DELETE)));
        assertTrue(styles.containsAll(visitor.getObjects(StyleInfo.class, ModificationType.DELETE)));
        assertTrue(layerGroups.containsAll(visitor.getObjects(LayerGroupInfo.class,
                ModificationType.DELETE)));
    }
    
    @Test
    public void testCascadeStyle() {
        setMockCreator(new MockCreator() {
            @Override
            public Catalog createCatalog(MockTestData testData) throws Exception {
                Catalog catalog = createNiceMock(Catalog.class);

                StyleInfo s = createNiceMock(StyleInfo.class);
                expect(catalog.getStyleByName((String)anyObject())).andReturn(s).anyTimes();

                LayerInfo l1 = createNiceMock(LayerInfo.class);
                expect(l1.getDefaultStyle()).andReturn(s).anyTimes();
                expect(catalog.getLayerByName(getLayerId(MockData.LAKES))).andReturn(l1);

                LayerInfo l2 = createNiceMock(LayerInfo.class);

                // add the lakes style to builds as an alternate style
                Set<StyleInfo> styles = createNiceMock(Set.class);
                expect(styles.contains(s)).andReturn(true).anyTimes();
                replay(styles);

                expect(l2.getStyles()).andReturn(styles).anyTimes();
                expect(catalog.getLayerByName(getLayerId(MockData.BUILDINGS))).andReturn(l2);
                
                expect(catalog.getLayers()).andReturn(Arrays.asList(l1, l2)).anyTimes();
                expect(catalog.getLayerGroups()).andReturn((List)Collections.emptyList()).anyTimes();
                replay(s, l1, l2, catalog);
                return catalog;
            }
        });

        Catalog catalog = getCatalog();
        CascadeRemovalReporter visitor = new CascadeRemovalReporter(catalog);

        StyleInfo style = catalog.getStyleByName("foo");
        LayerInfo buildings = catalog.getLayerByName(getLayerId(MockData.BUILDINGS));
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        
        visitor.visit(style);

        // test style reset
        assertEquals(style, visitor.getObjects(StyleInfo.class, ModificationType.DELETE).get(0));
        assertEquals(lakes, visitor.getObjects(LayerInfo.class, ModificationType.STYLE_RESET).get(0));
        assertEquals(buildings, visitor.getObjects(LayerInfo.class, ModificationType.EXTRA_STYLE_REMOVED).get(0));
    }

    String getLayerId(QName name) {
        return toString(name);
    }
}
