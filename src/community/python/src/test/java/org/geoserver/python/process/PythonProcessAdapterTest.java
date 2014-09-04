/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.python.Python;
import org.geotools.data.Parameter;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class PythonProcessAdapterTest {

    static Python py;
    static PythonProcessAdapter adapter;
    
    @BeforeClass
    public static void setUpData() throws Exception {
        GeoServerResourceLoader loader = new GeoServerResourceLoader(new File("target"));
        py = new Python(loader);
        
        File f = new File("target", "foo_process.py");
        FileUtils.copyURLToFile(PythonProcessAdapterTest.class.getResource("foo_process.py"), f);
        adapter = new PythonProcessAdapter(f, py);
    }
    
    @Test
    public void testGetNames() {
        assertTrue(adapter.getNames().contains("foo"));
    }
    
    @Test
    public void testGetVersion() {
        assertEquals("1.2.3", adapter.getVersion("foo"));
    }
    
    @Test
    public void testGetTitle() throws Exception {
        assertEquals("Foo", adapter.getTitle("foo"));
    }
    
    @Test
    public void testGetDescription() throws Exception {
        assertEquals("The foo process", adapter.getDescription("foo"));
    }
    
    @Test
    public void testGetInputParameters() throws Exception {
        Map<String,Parameter<?>> inputs = adapter.getInputParameters("foo");
        
        Parameter p = inputs.get("bar");
        assertNotNull(p);
        assertEquals("bar", p.key);
        assertEquals("bar", p.title.toString());
        assertEquals("The bar parameter", p.description.toString());
        assertEquals(String.class, p.type);
        
        p = inputs.get("baz");
        assertNotNull(p);
        assertEquals("baz", p.key);
        assertEquals("baz", p.title.toString());
        assertEquals("The baz parameter", p.description.toString());
        assertEquals(Object.class, p.type);
        
        p = inputs.get("bam");
        assertNotNull(p);
        assertEquals("bam", p.key);
        assertEquals("bam", p.title.toString());
        assertEquals("The bam parameter", p.description.toString());
        assertEquals(Point.class, p.type);
        
    }
    
    @Test
    public void testGetOutputParameters() throws Exception {
        Map<String,Parameter<?>> outputs = adapter.getOutputParameters("foo");
        
        Parameter p = outputs.get("result");
        assertNotNull(p);
        
        assertEquals("result", p.key);
        assertEquals("result", p.title.toString());
        assertEquals("The result", p.description.toString());
        assertEquals(Double.class, p.type);
    }
    
    @Test
    public void testRun() throws Exception {
        HashMap<String,Object> inputs = new HashMap();
        inputs.put("bar", "hello");
        inputs.put("baz", new Object());
        inputs.put("bam", new GeometryFactory().createPoint(new Coordinate(0,0)));
        
        Map<String,Object> result = adapter.run("foo", inputs);
        assertEquals(new Double(1.2), result.get("result"));
        
    }
}
