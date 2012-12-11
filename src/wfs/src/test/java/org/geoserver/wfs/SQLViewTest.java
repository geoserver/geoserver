/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.io.File;
import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geotools.data.DataStore;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.VirtualTable;
import org.geotools.jdbc.VirtualTableParameter;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;
import org.w3c.dom.Document;

import com.vividsolutions.jts.geom.Point;

import static org.custommonkey.xmlunit.XMLAssert.*;

public class SQLViewTest extends WFSTestSupport {

    static final String tableTypeName = "gs:pgeo";
    static final String viewTypeName = "gs:pgeo_view";
    
    @Override
    protected void setUpInternal(SystemTestData data) throws Exception {
        // run all the tests against a store that can do sql views 
        Catalog cat = getCatalog();
        DataStoreInfo ds = cat.getFactory().createDataStore();
        ds.setName("sqlviews");
        WorkspaceInfo ws = cat.getDefaultWorkspace();
        ds.setWorkspace(ws);
        
        Map params = ds.getConnectionParameters(); 
        params.put("dbtype", "h2");
        File dbFile = new File(getTestData().getDataDirectoryRoot().getAbsolutePath(), "data/h2test");
        params.put("database", dbFile.getAbsolutePath());
        cat.add(ds);
        
        SimpleFeatureSource fsp = getFeatureSource(SystemTestData.PRIMITIVEGEOFEATURE);
        
        DataStore store = (DataStore) ds.getDataStore(null);
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        
        tb.init(fsp.getSchema());
        tb.remove("surfaceProperty"); // the store cannot create multi-geom tables it seems
        tb.remove("curveProperty"); // the store cannot create multi-geom tables it seems
        tb.remove("uriProperty"); // this would render the store read only
        tb.setName("pgeo");
        SimpleFeatureType schema = tb.buildFeatureType();
        store.createSchema(schema);
        SimpleFeatureStore featureStore = (SimpleFeatureStore) store.getFeatureSource("pgeo");
        featureStore.addFeatures(fsp.getFeatures());
        
        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(ds);
        FeatureTypeInfo tft = cb.buildFeatureType(featureStore);
        cat.add(tft);
        
        // create the sql view
        JDBCDataStore jds = (JDBCDataStore) ds.getDataStore(null);
        VirtualTable vt = new VirtualTable("pgeo_view", "select \"name\", \"pointProperty\" from \"pgeo\" where \"booleanProperty\" = %bool% and \"name\" = '%name%'");
        vt.addParameter(new VirtualTableParameter("bool", "true"));
        vt.addParameter(new VirtualTableParameter("name", "name-f001"));
        vt.addGeometryMetadatata("pointProperty", Point.class, 4326);
        jds.addVirtualTable(vt);
        
        FeatureTypeInfo vft = cb.buildFeatureType(jds.getFeatureSource(vt.getName()));
        vft.getMetadata().put(FeatureTypeInfo.JDBC_VIRTUAL_TABLE, vt);
        cat.add(vft);
    }
    
    /**
     * Checks the setup did the expected job
     * @throws Exception
     */
    @Test
    public void testStoreSetup() throws Exception {
        FeatureTypeInfo tableTypeInfo = getCatalog().getFeatureTypeByName(tableTypeName);
        assertNotNull(tableTypeInfo);
        assertEquals(5, tableTypeInfo.getFeatureSource(null, null).getCount(Query.ALL));
        
        FeatureTypeInfo viewTypeInfo = getCatalog().getFeatureTypeByName(viewTypeName);
        assertNotNull(viewTypeInfo);
        assertEquals(1, viewTypeInfo.getFeatureSource(null, null).getCount(Query.ALL));
    }
    
    @Test
    public void testViewParams() throws Exception {
        Document dom = getAsDOM("wfs?service=WFS&request=GetFeature&typename=" + viewTypeName + "&version=1.1&viewparams=bool:true;name:name-f003");
        print(dom);
        
        assertXpathEvaluatesTo("name-f003", "//gs:pgeo_view/gml:name", dom);
        assertXpathEvaluatesTo("1", "count(//gs:pgeo_view)", dom);
    }

    
}
