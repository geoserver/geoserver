package org.geoserver.catalog.impl;

import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerTestSupport;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;

public class CascadeDeleteVisitorTest extends GeoServerTestSupport {
    
    static final String LAKES_GROUP = "lakesGroup";
    CascadeDeleteVisitor visitor;
    Catalog catalog;
    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        
        catalog = getCatalog();
        visitor = new CascadeDeleteVisitor(catalog);
        
        // setup a group, see GEOS-3040
        Catalog catalog = getCatalog();
        String lakes = MockData.LAKES.getLocalPart();
        String forests = MockData.FORESTS.getLocalPart();
        String bridges = MockData.BRIDGES.getLocalPart();
        
        setNativeBox(catalog, lakes);
        setNativeBox(catalog, forests);
        setNativeBox(catalog, bridges);
        
        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName(LAKES_GROUP);
        lg.getLayers().add(catalog.getLayerByName(lakes));
        lg.getStyles().add(catalog.getStyleByName(lakes));
        lg.getLayers().add(catalog.getLayerByName(forests));
        lg.getStyles().add(catalog.getStyleByName(forests));
        lg.getLayers().add(catalog.getLayerByName(bridges));
        lg.getStyles().add(catalog.getStyleByName(bridges));
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.calculateLayerGroupBounds(lg);
        catalog.add(lg);
    }
    
    public void setNativeBox(Catalog catalog, String name) throws Exception {
        FeatureTypeInfo fti = catalog.getFeatureTypeByName(name);
        fti.setNativeBoundingBox(fti.getFeatureSource(null, null).getBounds());
        fti.setLatLonBoundingBox(new ReferencedEnvelope(fti.getNativeBoundingBox(), DefaultGeographicCRS.WGS84));
        catalog.save(fti);
    }

    public void testCascadeLayer() {
        String name = getLayerId(MockData.LAKES);
        LayerInfo layer = catalog.getLayerByName(name);
        assertNotNull(layer);
        layer.accept(visitor);
        assertNull(catalog.getLayerByName(name));
        assertNull(catalog.getResourceByName(name, ResourceInfo.class));
        LayerGroupInfo group = catalog.getLayerGroupByName(LAKES_GROUP);
        assertEquals(2, group.getLayers().size());
        assertFalse(group.getLayers().contains(layer));
    }
    
    public void testCascadeStore() {
        String citeStore = MockData.CITE_PREFIX;
        StoreInfo store = catalog.getStoreByName(citeStore, StoreInfo.class);
        String buildings = getLayerId(MockData.BUILDINGS);
        String lakes = getLayerId(MockData.LAKES);
        assertNotNull(store);
        assertNotNull(catalog.getLayerByName(buildings));
        assertNotNull(catalog.getResourceByName(buildings, ResourceInfo.class));
        assertNotNull(catalog.getLayerByName(lakes));
        assertNotNull(catalog.getResourceByName(lakes, ResourceInfo.class));
        assertNotNull(catalog.getLayerGroupByName(LAKES_GROUP));
        
        store.accept(visitor);
        
        assertNull(catalog.getStoreByName(citeStore, StoreInfo.class));
        assertNull(catalog.getLayerByName(buildings));
        assertNull(catalog.getResourceByName(buildings, ResourceInfo.class));
        assertNull(catalog.getLayerByName(lakes));
        assertNull(catalog.getResourceByName(lakes, ResourceInfo.class));
        assertNull(catalog.getLayerGroupByName(LAKES_GROUP));
    }
    
    public void testCascadeWorkspace() {
        WorkspaceInfo ws = catalog.getWorkspaceByName(MockData.CITE_PREFIX);
        assertNotNull(ws);
        assertTrue(getCatalog().getStoresByWorkspace(ws, StoreInfo.class).size() > 0);
        
        ws.accept(visitor);
        assertEquals(0, getCatalog().getStoresByWorkspace(ws, StoreInfo.class).size());
    }
    
    public void testCascadeStyle() {
        String styleName = MockData.LAKES.getLocalPart();
        String layerName = getLayerId(MockData.LAKES);
        StyleInfo style = catalog.getStyleByName(styleName);
        assertNotNull(style);
        
        // add the lakes style to builds as an alternate style
        LayerInfo buildings = catalog.getLayerByName(getLayerId(MockData.BUILDINGS));
        buildings.getStyles().add(style);
        catalog.save(buildings);
        buildings = catalog.getLayerByName(getLayerId(MockData.BUILDINGS));
        assertTrue(buildings.getStyles().contains(style));
        
        style.accept(visitor);
        
        // test style reset
        assertNull(catalog.getStyleByName(styleName));
        assertEquals(StyleInfo.DEFAULT_POLYGON, catalog.getLayerByName(layerName).getDefaultStyle().getName());
        
        // test style removal
        buildings = catalog.getLayerByName(getLayerId(MockData.BUILDINGS));
        assertFalse(buildings.getStyles().contains(style));
    }
}
