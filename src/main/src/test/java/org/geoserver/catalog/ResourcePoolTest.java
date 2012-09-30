/*
 * Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.geoserver.catalog.util.ReaderUtils;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.RunTestSetup;
import org.geoserver.test.SystemTest;
import org.geotools.data.DataAccess;
import org.geotools.factory.GeoTools;
import org.geotools.feature.NameImpl;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.w3c.dom.Element;

/**
 * Tests for {@link ResourcePool}.
 * 
 * @author Ben Caradoc-Davies, CSIRO Exploration and Mining
 */
@Category(SystemTest.class)
public class ResourcePoolTest extends GeoServerSystemTestSupport {

    /**
     * Test that the {@link FeatureType} cache returns the same instance every time. This is assumed
     * by some nasty code in other places that tampers with the CRS. If a new {@link FeatureType} is
     * constructed for the same {@link FeatureTypeInfo}, Bad Things Happen (TM).
     */
    @Test public void testFeatureTypeCacheInstance() throws Exception {
        ResourcePool pool = new ResourcePool(getCatalog());
        FeatureTypeInfo info = getCatalog().getFeatureTypeByName(
                MockData.LAKES.getNamespaceURI(), MockData.LAKES.getLocalPart());
        FeatureType ft1 = pool.getFeatureType(info);
        FeatureType ft2 = pool.getFeatureType(info);
        FeatureType ft3 = pool.getFeatureType(info);
        assertSame(ft1, ft2);
        assertSame(ft1, ft3);
    }
    
    @Test public void testAttributeCache() throws Exception {
        final Catalog catalog = getCatalog();
        ResourcePool pool = new ResourcePool(catalog);
        
        // clean up the lakes type
        FeatureTypeInfo oldInfo = catalog.getFeatureTypeByName(
                MockData.LAKES.getNamespaceURI(), MockData.LAKES.getLocalPart());
        List<LayerInfo> layers = catalog.getLayers(oldInfo);
        for (LayerInfo layerInfo : layers) {
            catalog.remove(layerInfo);
        }
        catalog.remove(oldInfo);
        
        // rebuild as new
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.setStore(catalog.getStoreByName(MockData.CITE_PREFIX, MockData.CITE_PREFIX, DataStoreInfo.class));
        FeatureTypeInfo info = builder.buildFeatureType(new NameImpl(MockData.LAKES.getNamespaceURI(), MockData.LAKES.getLocalPart()));
        
        // non persisted state, caching should not occurr
        List<AttributeTypeInfo> att1 = pool.getAttributes(info);
        List<AttributeTypeInfo> att2 = pool.getAttributes(info);
        assertNotSame(att1, att2);
        assertEquals(att1, att2);
        
        // save it, making it persistent
        catalog.add(info);
        
        // first check caching actually works against persisted type infos
        List<AttributeTypeInfo> att3 = pool.getAttributes(info);
        List<AttributeTypeInfo> att4 = pool.getAttributes(info);
        assertSame(att3, att4);
        assertNotSame(att1, att3);
        assertEquals(att1, att3);
    }
    
    boolean cleared = false;
    @Test public void testCacheClearing() throws IOException {
        cleared = false;
        ResourcePool pool = new ResourcePool(getCatalog()) {
            @Override
            public void clear(FeatureTypeInfo info) {
                cleared = true;
                super.clear(info);
            }
        };
        FeatureTypeInfo info = getCatalog().getFeatureTypeByName(
                MockData.LAKES.getNamespaceURI(), MockData.LAKES.getLocalPart());
        
        assertNotNull( pool.getFeatureType( info ) );
        info.setTitle("changed");
        
        assertFalse( cleared );
        getCatalog().save( info );
        assertTrue( cleared );
        
        cleared = false;
        assertNotNull( pool.getFeatureType( info ) );
        
        for ( LayerInfo l : getCatalog().getLayers( info ) ) {
            getCatalog().remove( l );
        }
        getCatalog().remove( info );
        assertTrue( cleared );
    }

    boolean disposeCalled;

    /**
     * Make sure {@link ResourcePool#clear(DataStoreInfo)} and {@link ResourcePool#dispose()} call
     * {@link DataAccess#dispose()}
     */
    @Test public void testDispose() throws IOException {
        disposeCalled = false;
        class ResourcePool2 extends ResourcePool {
            @SuppressWarnings("serial")
            public ResourcePool2(Catalog catalog) {
                super(catalog);
                dataStoreCache = new DataStoreCache() {
                    @SuppressWarnings("unchecked")
                    @Override
                    protected void dispose(String name, DataAccess dataStore) {
                        disposeCalled = true;
                        super.dispose(name, dataStore);
                    }
                };
            }
        }

        Catalog catalog = getCatalog();
        ResourcePool pool = new ResourcePool2(catalog);
        catalog.setResourcePool(pool);

        DataStoreInfo info = catalog.getDataStores().get(0);
        // force the datastore to be created
        DataAccess<? extends FeatureType, ? extends Feature> dataStore = pool.getDataStore(info);
        assertNotNull(dataStore);
        assertFalse(disposeCalled);
        pool.clear(info);
        assertTrue(disposeCalled);

        // force the datastore to be created
        dataStore = pool.getDataStore(info);
        assertNotNull(dataStore);
        disposeCalled = false;
        pool.dispose();
        assertTrue(disposeCalled);
    }

    @Test public void testConfigureFeatureTypeCacheSize() {
        GeoServer gs = getGeoServer();
        GeoServerInfo global = gs.getGlobal();
        global.setFeatureTypeCacheSize(200);
        gs.save(global);

        Catalog catalog = getCatalog();
        assertEquals(200, catalog.getResourcePool().featureTypeCache.getHardReferencesCount());
    }
    
    @Test public void testDropCoverageStore() throws Exception {
        // build the store
        Catalog cat = getCatalog();
        CatalogBuilder cb = new CatalogBuilder(cat);
        CoverageStoreInfo store = cb.buildCoverageStore("dem");
        store.setURL(MockData.class.getResource("tazdem.tiff").toExternalForm());
        store.setType("GeoTIFF");
        cat.add(store);
        
        // build the coverage
        cb.setStore(store);
        CoverageInfo ci = cb.buildCoverage();
        cat.add(ci);
        
        // build the layer
        LayerInfo layer = cb.buildLayer(ci);
        cat.add(layer);
        
        // grab a reader just to inizialize the code
        ci.getGridCoverage(null, null);
        ci.getGridCoverageReader(null, GeoTools.getDefaultHints());
        
        // now drop the store
        CascadeDeleteVisitor visitor = new CascadeDeleteVisitor(cat);
        visitor.visit(store);
        
        // and reload (GEOS-4782 -> BOOM!)
        getGeoServer().reload();
        
    }

    @RunTestSetup
    @Test public void testGeoServerReload() throws Exception {
        Catalog cat = getCatalog();
        FeatureTypeInfo lakes = cat.getFeatureTypeByName(MockData.LAKES.getNamespaceURI(),
                MockData.LAKES.getLocalPart());
        assertFalse("foo".equals(lakes.getTitle()));

        GeoServerDataDirectory dd = new GeoServerDataDirectory(getResourceLoader());
        File info = dd.findResourceFile(lakes);
        //File info = getResourceLoader().find("featureTypes", "cite_Lakes", "info.xml");
        
        FileReader in = new FileReader(info);
        Element dom = ReaderUtils.parse(in); 
        Element title = ReaderUtils.getChildElement(dom, "title");
        title.getFirstChild().setNodeValue("foo");
        
        OutputStream output = new FileOutputStream(info);
        try {
            TransformerFactory.newInstance().newTransformer()
                    .transform(new DOMSource(dom), new StreamResult(output));
        } finally {
            output.close();
        }
        
        getGeoServer().reload();
        lakes = cat.getFeatureTypeByName(MockData.LAKES.getNamespaceURI(),
                MockData.LAKES.getLocalPart());
        assertEquals("foo", lakes.getTitle());
    }
}
