/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.datastore;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.python.Python;
import org.geotools.data.Parameter;
import org.geotools.data.DataAccessFactory.Param;
import org.junit.BeforeClass;
import org.junit.Test;

public class PythonDataStoreAdapterTest {

    static Python py;
    static PythonDataStoreAdapter adapter;
    
    @BeforeClass
    public static void setUpData() throws Exception {
        GeoServerResourceLoader loader = new GeoServerResourceLoader(new File("target"));
        py = new Python(loader);
        
        File f = new File("target", "foo_process.py");
        FileUtils.copyURLToFile(PythonDataStoreAdapterTest.class.getResource("foo_datastore.py"), f);
        adapter = new PythonDataStoreAdapter(f, py);
    }
    
    @Test
    public void testGetTitle() {
        assertEquals("Foo", adapter.getTitle());
    }
    
    @Test
    public void testGetDescription() {
        assertEquals("The foo datastore", adapter.getDescription());
    }
    
    @Test
    public void testGetParameters() throws Exception {
        List<Param> params = adapter.getParameters();
        assertEquals(2, params.size());
        
        Param p = params.get(0);
        assertEquals("bar", p.key);
        assertEquals("bar", p.title.toString());
        assertEquals("The bar param", p.description.toString());
        assertEquals(String.class, p.type);
        
        p = params.get(1);
        assertEquals("baz", p.key);
        assertEquals("baz", p.title.toString());
        assertEquals("The baz param", p.description.toString());
    }
    
    @Test
    public void testGetDataStore() throws Exception {
        HashMap<String,Object> params = new HashMap();
        params.put("bar", "boom");
        params.put("baz", "bomb");
        
        PythonDataStore datastore = adapter.getDataStore(params);
        assertEquals("boom", datastore.getWorkspace().__getattr__("bar").toString());
        assertEquals("bomb", datastore.getWorkspace().__getattr__("baz").toString());
    }
}
