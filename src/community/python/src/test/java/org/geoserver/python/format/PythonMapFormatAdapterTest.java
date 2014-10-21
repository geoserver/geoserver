/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.format;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;

import org.apache.commons.io.FileUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.python.Python;
import org.geoserver.wms.WMSMapContent;
import org.geotools.data.DataUtilities;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.WKTReader;

public class PythonMapFormatAdapterTest {

    static Python py;
    static PythonMapFormatAdapter adapter;
    
    @BeforeClass
    public static void setUpData() throws Exception {
        GeoServerResourceLoader loader = new GeoServerResourceLoader(new File("target"));
        py = new Python(loader);
        
        File f = new File("target", "foo_mapformat.py");
        FileUtils.copyURLToFile(PythonVectorFormatAdapterTest.class.getResource("foo_mapformat.py"), f);
        adapter = new PythonMapFormatAdapter(f, py);
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
        tb.setName("feature1");
        tb.add("geometry", Point.class);
        tb.add("name", String.class);
        SimpleFeatureType type1 = tb.buildFeatureType();
        
        tb.setName("feature2");
        tb.add("geometry", LineString.class);
        tb.add("name", String.class);
        SimpleFeatureType type2 = tb.buildFeatureType();
        
        
        WKTReader wkt = new WKTReader();
        
        FeatureCollection features1 = new DefaultFeatureCollection(null, null);
        
        SimpleFeatureBuilder b = new SimpleFeatureBuilder(type1);
        b.add(wkt.read("POINT(0 0)"));
        b.add("zero");
        features1.add(b.buildFeature("fid.0"));
        
        FeatureCollection features2 = new DefaultFeatureCollection(null, null);
        
        b = new SimpleFeatureBuilder(type2);
        b.add(wkt.read("LINESTRING(0 0, 1 1)"));
        b.add("one");
        features2.add(b.buildFeature("fid.1"));
        
        WMSMapContent context = new WMSMapContent();
        context.addLayer(new FeatureLayer(DataUtilities.source(features1), null));
        context.addLayer(new FeatureLayer(DataUtilities.source(features2), null));
        context.setMapWidth(500);
        context.setMapHeight(500);
        context.getViewport().setBounds(new ReferencedEnvelope(-180, 180, -90, 90, DefaultGeographicCRS.WGS84));
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        adapter.write(context, out);
        
        String result = "500;500\n" + 
        "(-180.0, -90.0, 180.0, 90.0, EPSG:4326)\n" +
        "POINT (0 0)\n" +
        "LINESTRING (0 0, 1 1)\n";
        
        assertEquals(result, new String(out.toByteArray()));
    }
    
}
