/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.geoserver.catalog.CascadeRemovalReporter.ModificationType;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.GeoServerPersister;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.security.decorators.SecuredLayerGroupInfo;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.SystemTest;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Category(SystemTest.class)
@TestSetup(run=TestSetupFrequency.REPEAT)
public class CatalogIntegrationTest extends GeoServerSystemTestSupport {
    
    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
        
        GeoServerExtensions extension = GeoServerExtensions.bean(GeoServerExtensions.class);
        if( extension == null ){
            GeoServerExtensionsHelper.init( this.applicationContext );
        }
        
        File root = testData.getDataDirectoryRoot();
        File defaultWorkspace = new File(root, "workspaces/default.xml");
        if(defaultWorkspace.exists()) {
            defaultWorkspace.delete();
        }
    }
    
    @Test
    public void testWorkspaceRemoveAndReadd() {
        // remove all workspaces
        Catalog catalog = getCatalog();
        NamespaceInfo defaultNamespace = catalog.getDefaultNamespace();
        WorkspaceInfo defaultWs = catalog.getDefaultWorkspace();
        List<WorkspaceInfo> workspaces = catalog.getWorkspaces();
        CascadeDeleteVisitor visitor = new CascadeDeleteVisitor(catalog);
        for (WorkspaceInfo ws : workspaces) {
            visitor.visit(ws);
        }
        assertEquals(0, catalog.getWorkspaces().size());
        assertEquals(0, catalog.getNamespaces().size());
        
        // add back one (this would NPE)
        catalog.add(defaultNamespace);
        catalog.add(defaultWs);
        assertEquals(1, catalog.getWorkspaces().size());
        assertEquals(1, catalog.getNamespaces().size());
        
        // get back by name (this would NPE too)
        assertNotNull(catalog.getNamespaceByURI(defaultNamespace.getURI()));
    }
    
    /**
     * Checks that the namespace/workspace listener keeps on working after
     * a catalog reload
     */
    @Test
    public void testNamespaceWorkspaceListenerAttached() throws Exception {
        Catalog catalog = getCatalog();
        
        NamespaceInfo ns = catalog.getNamespaceByPrefix(MockData.CITE_PREFIX);
        String newName = "XYWZ1234";
        ns.setPrefix(newName);
        catalog.save(ns);
        assertNotNull(catalog.getWorkspaceByName(newName));
        assertNotNull(catalog.getNamespaceByPrefix(newName));
        
        // force a reload
        int listenersBefore = catalog.getListeners().size();
        getGeoServer().reload();
        int listenersAfter = catalog.getListeners().size();
        assertEquals(listenersBefore, listenersAfter);
        
        // check the NamespaceWorkspaceListener is still attached and working
        ns = catalog.getNamespaceByPrefix(newName);
        ns.setPrefix(MockData.CITE_PREFIX);
        catalog.save(ns);
        assertNotNull(catalog.getWorkspaceByName(MockData.CITE_PREFIX));
        
        // make sure we only have one resource pool listener and one catalog persister
        int countCleaner = 0;
        int countPersister = 0;
        for (CatalogListener listener : catalog.getListeners()) {
            if(listener instanceof ResourcePool.CacheClearingListener) {
                countCleaner++;
            } else if(listener instanceof GeoServerPersister) {
                countPersister++;
            }
        }
        assertEquals(1, countCleaner);
        assertEquals(1, countPersister);
    }
    
    @Test
    public void modificationProxySerializeTest() throws Exception {
        Catalog catalog = getCatalog();
        
        // workspace
        WorkspaceInfo ws = catalog.getWorkspaceByName(MockData.CITE_PREFIX);
        WorkspaceInfo ws2 = serialize(ws);
        assertSame(ModificationProxy.unwrap(ws), ModificationProxy.unwrap(ws2));
        
        // namespace
        NamespaceInfo ns = catalog.getNamespaceByPrefix(MockData.CITE_PREFIX);
        NamespaceInfo ns2 = serialize(ns);
        assertSame(ModificationProxy.unwrap(ns), ModificationProxy.unwrap(ns2));
        
        // data store and related objects
        DataStoreInfo ds = catalog.getDataStoreByName(MockData.CITE_PREFIX);
        DataStoreInfo ds2 = serialize(ds);
        assertSame(ModificationProxy.unwrap(ds), ModificationProxy.unwrap(ds2));
        assertSame(ModificationProxy.unwrap(ds.getWorkspace()), ModificationProxy.unwrap(ds2.getWorkspace()));
        
        // coverage store and related objects
        CoverageStoreInfo cs = catalog.getCoverageStoreByName(MockData.TASMANIA_DEM.getLocalPart());
        CoverageStoreInfo cs2 = serialize(cs);
        assertSame(ModificationProxy.unwrap(cs), ModificationProxy.unwrap(cs2));
        assertSame(ModificationProxy.unwrap(cs.getWorkspace()), ModificationProxy.unwrap(cs2.getWorkspace()));
        
        // feature type and related objects
        FeatureTypeInfo ft = catalog.getFeatureTypeByName(getLayerId(MockData.BRIDGES));
        FeatureTypeInfo ft2 = serialize(ft);
        assertSame(ModificationProxy.unwrap(ft), ModificationProxy.unwrap(ft2));
        assertSame(ModificationProxy.unwrap(ft.getStore()), ModificationProxy.unwrap(ft2.getStore()));
        
        // coverage and related objects
        CoverageInfo ci = catalog.getCoverageByName(getLayerId(MockData.TASMANIA_DEM));
        CoverageInfo ci2 = serialize(ci);
        assertSame(ModificationProxy.unwrap(ci), ModificationProxy.unwrap(ci2));
        assertSame(ModificationProxy.unwrap(ci.getStore()), ModificationProxy.unwrap(ci.getStore()));
        
        // style
        StyleInfo streamsStyle = catalog.getStyleByName("Streams");
        StyleInfo si2 = serialize(streamsStyle);
        assertSame(ModificationProxy.unwrap(streamsStyle), ModificationProxy.unwrap(si2));

        // layer and related objects
        LayerInfo li = catalog.getLayerByName(getLayerId(MockData.BRIDGES));
        // ... let's add an extra style
        
        li.getStyles().add(streamsStyle);
        catalog.save(li);
        LayerInfo li2 = serialize(li);
        assertSame(ModificationProxy.unwrap(li), ModificationProxy.unwrap(li2));
        assertSame(ModificationProxy.unwrap(li.getResource()), ModificationProxy.unwrap(li2.getResource()));
        assertSame(ModificationProxy.unwrap(li.getDefaultStyle()), ModificationProxy.unwrap(li2.getDefaultStyle()));
        assertSame(ModificationProxy.unwrap(li.getStyles().iterator().next()), ModificationProxy.unwrap(li2.getStyles().iterator().next()));
        
        // try a group layer
        CatalogBuilder cb = new CatalogBuilder(catalog);
        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.getLayers().add(catalog.getLayerByName(getLayerId(MockData.ROAD_SEGMENTS)));
        lg.getLayers().add(catalog.getLayerByName(getLayerId(MockData.PONDS)));
        cb.calculateLayerGroupBounds(lg);
        lg.setName("test-lg");
        catalog.add(lg);
        // ... make sure we get a proxy
        lg = catalog.getLayerGroupByName("test-lg");
        if (lg instanceof SecuredLayerGroupInfo) {
            lg = ((SecuredLayerGroupInfo) lg).unwrap(LayerGroupInfo.class);
        }
        LayerGroupInfo lg2 = serialize(lg);
        assertSame(ModificationProxy.unwrap(lg), ModificationProxy.unwrap(lg2));
        assertSame(ModificationProxy.unwrap(lg.getLayers().get(0)), ModificationProxy.unwrap(lg2.getLayers().get(0)));
        assertSame(ModificationProxy.unwrap(lg.getLayers().get(1)), ModificationProxy.unwrap(lg2.getLayers().get(1)));
        
        // now check a half modified proxy
        LayerInfo lim = catalog.getLayerByName(getLayerId(MockData.BRIDGES));
        // ... let's add an extra style
        lim.setDefaultStyle(streamsStyle);
        lim.getStyles().add(streamsStyle);
        // clone and check
        LayerInfo lim2 = serialize(lim);
        assertSame(ModificationProxy.unwrap(lim.getDefaultStyle()), ModificationProxy.unwrap(lim2.getDefaultStyle()));
        assertSame(ModificationProxy.unwrap(lim.getStyles().iterator().next()), ModificationProxy.unwrap(lim2.getStyles().iterator().next()));
        
        // mess a bit with the metadata map too
        String key = "workspaceKey";
        lim.getMetadata().put(key, ws);
        LayerInfo lim3 = serialize(lim);
        assertSame(ModificationProxy.unwrap(lim), ModificationProxy.unwrap(lim3));
        assertSame(ModificationProxy.unwrap(lim.getMetadata().get(key)), 
                ModificationProxy.unwrap(lim3.getMetadata().get(key)));
    }
    
    /**
     * Serializes and de-serializes the provided object
     * 
     * @param object
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    <T> T serialize(T object) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(object);
        oos.close();
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bis);
        return (T) ois.readObject();
        
    }

    @Test
    public void testCascadeDeleteWorkspaceSpecific() throws Exception {
        Catalog catalog = getCatalog();
        WorkspaceInfo ws = catalog.getWorkspaceByName(MockData.ROAD_SEGMENTS.getPrefix());

        // create a workspace specific group
        CatalogBuilder cb = new CatalogBuilder(catalog);
        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.getLayers().add(catalog.getLayerByName(getLayerId(MockData.ROAD_SEGMENTS)));
        lg.getLayers().add(catalog.getLayerByName(getLayerId(MockData.STREAMS)));
        cb.calculateLayerGroupBounds(lg);
        lg.setName("test-lg");
        lg.setWorkspace(ws);
        catalog.add(lg);

        // make a style a workspace specific
        StyleInfo style = catalog.getStyleByName(MockData.ROAD_SEGMENTS.getLocalPart());
        style.setWorkspace(ws);
        catalog.save(style);

        // check we are getting the groups and styles reported properly
        CascadeRemovalReporter reporter = new CascadeRemovalReporter(catalog);
        ws.accept(reporter);
        List<StyleInfo> styles = reporter.getObjects(StyleInfo.class, ModificationType.DELETE);
        assertEquals(1, styles.size());
        assertEquals(style, styles.get(0));

        List<LayerGroupInfo> groups = reporter.getObjects(LayerGroupInfo.class,
                ModificationType.DELETE);
        assertEquals(1, groups.size());
        assertEquals(lg, groups.get(0));

        // now remove for real
        CascadeDeleteVisitor remover = new CascadeDeleteVisitor(catalog);
        ws.accept(remover);
        assertNull(catalog.getWorkspaceByName(ws.getName()));
        assertNull(catalog.getStyleByName(style.getName()));
        assertNull(catalog.getLayerGroupByName(lg.getName()));
    }
    
    @Test
    public void testReprojectLayerGroup() 
            throws NoSuchAuthorityCodeException, FactoryException, Exception {
        
        Catalog catalog = getCatalog();
        
        CatalogBuilder cb = new CatalogBuilder(catalog);
        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        LayerInfo l = catalog.getLayerByName(getLayerId(MockData.ROAD_SEGMENTS));
        lg.getLayers().add(l);
        lg.setName("test-reproject");
        
        // EPSG:4901 "equivalent" but different uom
        String wkt = "GEOGCS[\"GCS_ATF_Paris\",DATUM[\"D_ATF\",SPHEROID[\"Plessis_1817\",6376523.0,308.64]],PRIMEM[\"Paris\",2.337229166666667],UNIT[\"Grad\",0.01570796326794897]]";
        CoordinateReferenceSystem lCrs = CRS.parseWKT(wkt);
        ((FeatureTypeInfo)l.getResource()).setSRS(null);
        ((FeatureTypeInfo)l.getResource()).setNativeCRS(lCrs);
        assertNull(CRS.lookupEpsgCode(lCrs, false));
        
        // Use the real thing now
        CoordinateReferenceSystem lgCrs = CRS.decode("EPSG:4901"); 
        assertNotNull(CRS.lookupEpsgCode(lgCrs, false));
        
        //Reproject our layer group to EPSG:4901. We expect it to have an EPSG code.
        cb.calculateLayerGroupBounds(lg, lgCrs);
        assertNotNull(CRS.lookupEpsgCode(lg.getBounds().getCoordinateReferenceSystem(), false));
    }
}
