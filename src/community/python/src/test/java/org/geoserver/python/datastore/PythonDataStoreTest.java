/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.python.Python;
import org.geoserver.python.datastore.PythonDataStore;
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureReader;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.h2.tools.DeleteDbFiles;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class PythonDataStoreTest {

    static PythonDataStore dataStore;
    
    @BeforeClass
    public static void setUpData() throws Exception {
        DeleteDbFiles.execute("target", "foobar", true);
        
        GeoServerResourceLoader loader = new GeoServerResourceLoader(new File("target"));
        PythonInterpreter pi = new Python(loader).interpreter();
        pi.exec("from geoscript.workspace import H2");
        pi.exec("from geoscript.geom import Point");
        pi.exec("h2 = H2('foobar', 'target')");
        pi.exec("l = h2.create('bar', [('geom', Point, 'epsg:4326'), ('baz', str)])");
        pi.exec("l.add([Point(10,10), 'ten'])");
        pi.exec("l.add([Point(20,20), 'twenty'])");
        
        final PyObject workspace = pi.get("h2");
        dataStore = new PythonDataStore(null, null) {
            PyObject getWorkspace() {
                return workspace;
            }
        };
    }
    
    @Test
    public void testGetTypeNames() throws Exception {
        String[] typeNames = dataStore.getTypeNames();
        
        assertEquals(1, typeNames.length);
        assertEquals("bar", typeNames[0]);
    }
    
    @Test
    public void testGetFeatureSource() throws Exception {
        SimpleFeatureSource source = dataStore.getFeatureSource("bar");
        assertNotNull(source);
        
        SimpleFeatureType schema = source.getSchema();
        assertNotNull(schema);
        
        assertEquals(2, schema.getAttributeCount());
        assertEquals("geom", schema.getDescriptor(0).getLocalName());
        assertTrue(Geometry.class.isAssignableFrom(schema.getDescriptor(0).getType().getBinding()));
        
        assertEquals("baz", schema.getDescriptor(1).getLocalName());
        assertTrue(String.class.isAssignableFrom(schema.getDescriptor(1).getType().getBinding()));
    
        assertEquals(2, source.getCount(Query.ALL));
        assertEquals(1, source.getCount(new Query("bar", CQL.toFilter("baz = 'ten'"))));
        
        ReferencedEnvelope box = source.getBounds();
        assertNotNull(box);
        assertTrue(box.contains(new Coordinate(10,10)));
        assertTrue(box.contains(new Coordinate(20,20)));
        
        box = source.getBounds(new Query("bar", CQL.toFilter("baz = 'ten'")));
        assertNotNull(box);
        assertTrue(box.contains(new Coordinate(10,10)));
        assertFalse(box.contains(new Coordinate(20,20)));
        
        SimpleFeatureReader r = (SimpleFeatureReader) 
            dataStore.getFeatureReader(new Query("bar"), Transaction.AUTO_COMMIT);
        
        assertNotNull(r);
        assertTrue(r.hasNext());
       
        SimpleFeature f = r.next();
        assertNotNull(f);
        assertTrue(f.getDefaultGeometry() instanceof Point);
        assertTrue(new Coordinate(10,10).equals2D(((Point)f.getDefaultGeometry()).getCoordinate()));
        assertEquals("ten", f.getAttribute("baz"));
        
        assertTrue(r.hasNext());
        f = r.next();
        assertNotNull(f);
        assertTrue(f.getDefaultGeometry() instanceof Point);
        assertTrue(new Coordinate(20,20).equals2D(((Point)f.getDefaultGeometry()).getCoordinate()));
        assertEquals("twenty", f.getAttribute("baz"));
        r.close();
    }
    
    public void testGetFeatureReader() throws Exception {
        FeatureReader r = dataStore.getFeatureReader(new Query("bar"), Transaction.AUTO_COMMIT);
        assertNotNull(r);
        
        assertTrue(r.hasNext());
    }
}
