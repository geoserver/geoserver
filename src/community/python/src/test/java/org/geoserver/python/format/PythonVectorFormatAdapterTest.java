/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.format;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;

import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.WfsFactory;

import org.apache.commons.io.FileUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.python.Python;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class PythonVectorFormatAdapterTest {

    static Python py;
    static PythonVectorFormatAdapter adapter;
    
    @BeforeClass
    public static void setUpData() throws Exception {
        GeoServerResourceLoader loader = new GeoServerResourceLoader(new File("target"));
        py = new Python(loader);
        
        File f = new File("target", "foo_vectorformat.py");
        FileUtils.copyURLToFile(PythonVectorFormatAdapterTest.class.getResource("foo_vectorformat.py"), f);
        adapter = new PythonVectorFormatAdapter(f, py);
    }
    
    @Test
    public void testGetName() {
        assertEquals("Foo", adapter.getName());
    }
    
    @Test
    public void testGetMimeType() {
        assertEquals("text/plain", adapter.getMimeType());
    }
    
    @Test
    public void testWrite() throws Exception {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("feature");
        tb.add("geometry", Point.class);
        tb.add("name", String.class);
        
        SimpleFeatureType type = tb.buildFeatureType();
        FeatureCollection features = new DefaultFeatureCollection(null, null);
        
        SimpleFeatureBuilder b = new SimpleFeatureBuilder(type);
        b.add(new GeometryFactory().createPoint(new Coordinate(0,0)));
        b.add("zero");
        features.add(b.buildFeature("fid.0"));
        
        b.add(new GeometryFactory().createPoint(new Coordinate(1,1)));
        b.add("one");
        features.add(b.buildFeature("fid.1"));
        
        FeatureCollectionType fc = WfsFactory.eINSTANCE.createFeatureCollectionType();
        fc.getFeature().add(features);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        adapter.write(FeatureCollectionResponse.adapt(fc), out); 
        
        assertEquals("fid.0;fid.1;", new String(out.toByteArray()));
        
    }
}
