/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.js;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.script.ScriptException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.util.IOUtils;
import org.geoserver.script.ScriptIntTestSupport;
import org.geoserver.script.wps.ScriptProcess;
import org.geoserver.script.wps.ScriptProcessFactory;
import org.geotools.data.FeatureSource;
import org.geotools.data.Parameter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.util.URLs;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;

public class JavaScriptWpsHookTest extends ScriptIntTestSupport {

    ScriptProcessFactory processFactory;

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        File fromDir = URLs.urlToFile(getClass().getResource("scripts"));
        File toDir = new File(dataDirectory.getDataDirectoryRoot(), "scripts");
        IOUtils.deepCopy(fromDir, toDir);
        super.populateDataDirectory(dataDirectory);
    }

    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();
        processFactory = new ScriptProcessFactory(getScriptManager());
    }

    private ScriptProcess createProcess(String id) {
        Name name = new NameImpl("js", id);
        Process process = processFactory.create(name);
        assertNotNull("not null: " + name.toString(), process);
        assertTrue("script process: " + name.toString(), process instanceof ScriptProcess);
        return (ScriptProcess) process;
    }

    public void testAdd() throws ScriptException, IOException {

        ScriptProcess process = createProcess("add");
        assertEquals("title", "JavaScript Addition Process", process.getTitle());
        assertEquals(
                "correct description",
                "Process that accepts two numbers and returns their sum.",
                process.getDescription());

        // test inputs
        Map<String, Parameter<?>> inputs = process.getInputs();

        assertTrue("first in inputs", inputs.containsKey("first"));
        Parameter<?> first = inputs.get("first");
        assertEquals("first title", "First Operand", first.getTitle().toString());
        assertEquals("first description", "The first operand.", first.getDescription().toString());
        assertEquals("first type", Float.class, first.getType());

        assertTrue("second in inputs", inputs.containsKey("first"));
        Parameter<?> second = inputs.get("second");
        assertEquals("second title", "Second Operand", second.getTitle().toString());
        assertEquals(
                "second description", "The second operand.", second.getDescription().toString());
        assertEquals("second type", Float.class, second.getType());

        // test outputs
        Map<String, Parameter<?>> outputs = process.getOutputs();

        assertTrue("sum in outputs", outputs.containsKey("sum"));
        Parameter<?> sumParam = outputs.get("sum");
        assertEquals("sum title", "Sum", sumParam.getTitle().toString());
        assertEquals(
                "sum description",
                "The sum of the two inputs",
                sumParam.getDescription().toString());
        assertEquals("sum type", Float.class, sumParam.getType());

        // test execute
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("first", 2.0);
        input.put("second", 4.0);
        Map<String, Object> result = process.execute(input, null);
        assertNotNull("add result", result);
        assertTrue("sum in results", result.containsKey("sum"));
        Object sum = result.get("sum");
        assertEquals("correct sum", 6.0, (Double) sum, 0.0);
    }

    public void testBuffer() throws ScriptException, IOException, ParseException {

        ScriptProcess process = createProcess("buffer");

        // test inputs
        Map<String, Parameter<?>> inputs = process.getInputs();
        assertTrue("geom in inputs", inputs.containsKey("geom"));
        Parameter<?> geomParam = inputs.get("geom");
        assertEquals("geom title", "Input Geometry", geomParam.getTitle().toString());
        assertEquals(
                "geom description", "The target geometry.", geomParam.getDescription().toString());
        assertEquals("geom type", Geometry.class, geomParam.getType());

        assertTrue("distance in inputs", inputs.containsKey("distance"));
        Parameter<?> distance = inputs.get("distance");
        assertEquals("distance title", "Buffer Distance", distance.getTitle().toString());
        assertEquals(
                "distance description",
                "The distance by which to buffer the geometry.",
                distance.getDescription().toString());
        assertEquals("distance type", Double.class, distance.getType());

        // test execute
        WKTReader wktReader = new WKTReader();
        Geometry point = wktReader.read("POINT(1 1)");
        Map<String, Object> input = new HashMap<String, Object>();
        input.put("geom", point);
        input.put("distance", 4.0);
        Map<String, Object> result = process.execute(input, null);
        assertNotNull("buffer result", result);
        assertTrue("result in results", result.containsKey("result"));
        Object obj = result.get("result");
        assertTrue("got back a geometry", obj instanceof Geometry);
        Geometry geom = (Geometry) obj;
        Double exp = Math.PI * 16;
        assertEquals("correct sum", exp, geom.getArea(), 1.0);
    }

    public FeatureCollection<? extends FeatureType, ? extends Feature> getFeatures(
            String uri, String name) {
        Catalog catalog = getCatalog();
        FeatureTypeInfo info = catalog.getResourceByName(uri, name, FeatureTypeInfo.class);
        assertNotNull(info);
        FeatureSource<? extends FeatureType, ? extends Feature> source = null;
        try {
            source = info.getFeatureSource(null, null);
        } catch (IOException e) {
            // pass
        }
        assertNotNull(source);
        FeatureCollection<? extends FeatureType, ? extends Feature> features = null;
        try {
            features = source.getFeatures();
        } catch (IOException e) {
            // pass
        }
        assertNotNull(features);
        return features;
    }

    public void testExecuteIntersectsBridgesHit() throws Exception {
        ScriptProcess process = createProcess("intersects");

        WKTReader wktReader = new WKTReader();

        Map<String, Object> input = new HashMap<String, Object>();
        input.put("geometry", wktReader.read("POINT (0.0002 0.0007)"));
        input.put("features", getFeatures("http://www.opengis.net/cite", "Bridges"));

        Map<String, Object> result = process.execute(input, null);
        assertNotNull("intersects result", result);
        assertTrue("intersects in results", result.containsKey("intersects"));
        Object obj = result.get("intersects");
        assertTrue("got back a boolean", obj instanceof Boolean);
        assertTrue("intersects", (Boolean) obj);

        // TODO: determine why this is not 1
        assertEquals("intersects one", 1.0, result.get("count"));
    }

    public void testExecuteIntersectsBridgesMiss() throws Exception {
        ScriptProcess process = createProcess("intersects");
        WKTReader wktReader = new WKTReader();

        Map<String, Object> input = new HashMap<String, Object>();
        input.put("geometry", wktReader.read("POINT (10 0.0007)"));
        input.put("features", getFeatures("http://www.opengis.net/cite", "Bridges"));

        Map<String, Object> result = process.execute(input, null);
        assertNotNull("intersects result", result);
        assertTrue("intersects in results", result.containsKey("intersects"));
        Object obj = result.get("intersects");
        assertTrue("got back a boolean", obj instanceof Boolean);
        assertFalse("intersects", (Boolean) obj);
    }

    public void testExecuteIntersectsBuildingsHit() throws Exception {
        ScriptProcess process = createProcess("intersects");
        WKTReader wktReader = new WKTReader();

        Map<String, Object> input = new HashMap<String, Object>();
        input.put("geometry", wktReader.read("POINT (0.00216 0.00084)"));
        input.put("features", getFeatures("http://www.opengis.net/cite", "Buildings"));

        Map<String, Object> result = process.execute(input, null);
        assertNotNull("intersects result", result);
        assertTrue("intersects in results", result.containsKey("intersects"));
        Object obj = result.get("intersects");
        assertTrue("got back a boolean", obj instanceof Boolean);
        assertTrue("intersects", (Boolean) obj);
    }

    public void testExecuteIntersectsBuildingsHitMultiple() throws Exception {
        ScriptProcess process = createProcess("intersects");
        WKTReader wktReader = new WKTReader();

        Map<String, Object> input = new HashMap<String, Object>();
        input.put("geometry", wktReader.read("LINESTRING (0.00216 0.00084, 0.001 0.00054)"));
        input.put("features", getFeatures("http://www.opengis.net/cite", "Buildings"));

        Map<String, Object> result = process.execute(input, null);
        assertNotNull("intersects result", result);
        assertTrue("intersects in results", result.containsKey("intersects"));
        Object obj = result.get("intersects");
        assertTrue("got back a boolean", obj instanceof Boolean);
        assertTrue("intersects", (Boolean) obj);

        // TODO: determine why this is not 2
        assertEquals("intersects one", 2.0, result.get("count"));
    }

    public void testExecuteIntersectsBuildingsMiss() throws Exception {
        ScriptProcess process = createProcess("intersects");
        WKTReader wktReader = new WKTReader();

        Map<String, Object> input = new HashMap<String, Object>();
        input.put("geometry", wktReader.read("POINT (10 0.00084)"));
        input.put("features", getFeatures("http://www.opengis.net/cite", "Buildings"));

        Map<String, Object> result = process.execute(input, null);
        assertNotNull("intersects result", result);
        assertTrue("intersects in results", result.containsKey("intersects"));
        Object obj = result.get("intersects");
        assertTrue("got back a boolean", obj instanceof Boolean);
        assertFalse("intersects", (Boolean) obj);
    }

    public void testExecuteDistbearBuildings() throws Exception {
        ScriptProcess process = createProcess("distbear");
        WKTReader wktReader = new WKTReader();

        Map<String, Object> input = new HashMap<String, Object>();
        input.put("origin", wktReader.read("POINT (10 0.00084)"));
        input.put("features", getFeatures("http://www.opengis.net/cite", "Buildings"));

        Map<String, Object> output = process.execute(input, null);
        assertNotNull("distbear output", output);
        assertTrue("result in outputs", output.containsKey("result"));
        Object obj = output.get("result");
        assertTrue("result type", obj instanceof SimpleFeatureCollection);
        SimpleFeatureCollection features = (SimpleFeatureCollection) obj;
        assertEquals("result size", 2, features.size());

        SimpleFeatureType schema = features.getSchema();
        GeometryType geomType = schema.getGeometryDescriptor().getType();
        assertEquals("geometry type", MultiPolygon.class, geomType.getBinding());

        assertNotNull("distance attribute", schema.getDescriptor("distance"));
        assertNotNull("bearing attribute", schema.getDescriptor("bearing"));
    }

    public void testExecuteBufferedUnion() throws Exception {
        ScriptProcess process = createProcess("bufferedUnion");
        WKTReader wktReader = new WKTReader();

        Map<String, Object> input = new HashMap<String, Object>();
        ArrayList<Geometry> geoms = new ArrayList<Geometry>(2);
        geoms.add(wktReader.read("POINT (0 0)"));
        geoms.add(wktReader.read("POINT (1 0)"));
        input.put("geom", geoms);
        input.put("distance", 2);

        Map<String, Object> output = process.execute(input, null);
        assertNotNull("output", output);
        assertTrue("result in outputs", output.containsKey("result"));
        Object obj = output.get("result");
        assertTrue("result type", obj instanceof Polygon);
        Polygon geom = (Polygon) obj;
        assertEquals(16.43, geom.getArea(), 0.01);
    }

    public void testExecuteBufferSplit() throws Exception {
        ScriptProcess process = createProcess("bufferSplit");
        WKTReader wktReader = new WKTReader();

        Map<String, Object> input = new HashMap<String, Object>();
        input.put("geom", wktReader.read("POINT (0 0)"));
        input.put("distance", 0.5);
        input.put("line", wktReader.read("LINESTRING (-1 -1, 1 1)"));

        Map<String, Object> output = process.execute(input, null);
        assertNotNull("output", output);
        assertTrue("result in outputs", output.containsKey("result"));
        Object obj = output.get("result");
        assertTrue("result type", obj instanceof GeometryCollection);
        GeometryCollection geom = (GeometryCollection) obj;
        assertEquals(2, geom.getNumGeometries());
        assertEquals(0.39, geom.getGeometryN(0).getArea(), 0.01);
        assertEquals(0.39, geom.getGeometryN(1).getArea(), 0.01);
    }
}
