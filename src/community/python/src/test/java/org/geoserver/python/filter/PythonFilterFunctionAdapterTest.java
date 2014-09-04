/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.python.Python;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory;

public class PythonFilterFunctionAdapterTest {

    static Python py;
    static PythonFilterFunctionAdapter adapter;
    
    @BeforeClass
    public static void setUpData() throws Exception {
        GeoServerResourceLoader loader = new GeoServerResourceLoader(new File("target"));
        py = new Python(loader);
        
        File f = new File("target", "foo_function.py");
        FileUtils.copyURLToFile(PythonFilterFunctionAdapterTest.class.getResource("foo_function.py"), f);
        adapter = new PythonFilterFunctionAdapter(f, py);
    }
    
    @Test
    public void testGetNames() {
        assertTrue(adapter.getNames().contains("foo"));
    }
    
    @Test
    public void testGetParameterNames() {
        assertTrue(adapter.getParameterNames("foo").contains("bar"));
        assertTrue(adapter.getParameterNames("foo").contains("baz"));
    }
    
    @Test
    public void testEvaluate() {
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        
        List args = Arrays.asList(ff.literal("one"), ff.literal("two"));
        Object result = adapter.evaluate("foo", null, args );
        assertEquals("bam", result);
    }
    
    @Test
    public void testEvaluateFeature() {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("geotools");
        tb.add("foo", String.class);
        
        SimpleFeatureBuilder b = new SimpleFeatureBuilder(tb.buildFeatureType());
        b.add("baz");
        
        SimpleFeature f = b.buildFeature(null);
        assertEquals(false, adapter.evaluate("acme", f, null ));
        
        b.add("bar");
        f = b.buildFeature(null);
        assertEquals(true, adapter.evaluate("acme", f, null ));
    }
}
