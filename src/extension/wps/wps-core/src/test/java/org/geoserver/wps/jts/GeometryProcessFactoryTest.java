package org.geoserver.wps.jts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.geotools.data.Parameter;
import org.geotools.feature.NameImpl;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.Processors;
import org.opengis.feature.type.Name;
import org.opengis.util.InternationalString;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.operation.buffer.BufferParameters;

public class GeometryProcessFactoryTest extends TestCase {

	GeometryProcessFactory factory;

	@Override
	protected void setUp() throws Exception {
		factory = new GeometryProcessFactory();
	}
	
	public void testNames() {
		Set<Name> names = factory.getNames();
		assertTrue(names.size() > 0);
		// System.out.println(names);
		assertTrue(names.contains(new NameImpl("JTS", "buffer")));
		assertTrue(names.contains(new NameImpl("JTS", "union")));
	}
	
	public void testDescribeBuffer() {
		NameImpl bufferName = new NameImpl("JTS", "buffer");
		InternationalString desc = factory.getDescription(bufferName);
		assertNotNull(desc);
		
		Map<String, Parameter<?>> params = factory.getParameterInfo(bufferName);
		assertEquals(4, params.size());
		
		Parameter<?> geom = params.get("geom");
		assertEquals(Geometry.class, geom.type);
		assertTrue(geom.required);
		
		Parameter<?> distance = params.get("distance");
		assertEquals(double.class, distance.type);
		assertTrue(distance.required);
		
		Parameter<?> quadrants = params.get("quadrantSegments");
		assertEquals(Integer.class, quadrants.type);
		assertFalse(quadrants.required);
		assertEquals(0, quadrants.minOccurs);
		assertEquals(1, quadrants.maxOccurs);
		
		Parameter<?> capStyle = params.get("capStyle");
		assertEquals(GeometryFunctions.BufferCapStyle.class, capStyle.type);
		assertFalse(capStyle.required);
		assertEquals(0, capStyle.minOccurs);
		assertEquals(1, capStyle.maxOccurs);
	}
	
	public void testExecuteBuffer() throws Exception {
		org.geotools.process.Process buffer = factory.create(new NameImpl("JTS", "Buffer"));
		
		// try less than the required params
		Map<String, Object> inputs = new HashMap<String, Object>();
		try {
			buffer.execute(inputs, null);
			fail("What!!! Should have failed big time!");
		} catch(ProcessException e) {
			// fine
		}
		
		// try out only the required params
		Geometry geom = new WKTReader().read("POINT(0 0)");
		inputs.put("geom", geom);
		inputs.put("distance", 1d);
		Map<String, Object> result = buffer.execute(inputs, null);

		assertEquals(1, result.size());
		Geometry buffered = (Geometry) result.get("result");
		assertNotNull(buffered);
		assertTrue(buffered.equals(geom.buffer(1d)));
		
		// pass in all params
		inputs.put("quadrantSegments", 12);
		inputs.put("capStyle", GeometryFunctions.BufferCapStyle.Square);
		result = buffer.execute(inputs, null);

		assertEquals(1, result.size());
		buffered = (Geometry) result.get("result");
		assertNotNull(buffered);
		assertTrue(buffered.equals(geom.buffer(1d, 12, BufferParameters.CAP_SQUARE)));
	}

	public void testSPI() throws Exception {
		NameImpl bufferName = new NameImpl("JTS", "buffer");
		ProcessFactory factory = Processors.createProcessFactory(bufferName);
		assertNotNull(factory);
		assertTrue(factory instanceof GeometryProcessFactory);
		
		org.geotools.process.Process buffer = Processors.createProcess(bufferName);
		assertNotNull(buffer);
	}
	
	public void testDescribeUnion() {
		NameImpl unionName = new NameImpl("JTS", "union");
		InternationalString desc = factory.getDescription(unionName);
		assertNotNull(desc);
		
		Map<String, Parameter<?>> params = factory.getParameterInfo(unionName);
		assertEquals(1, params.size());
		
		Parameter<?> geom = params.get("geom");
		assertEquals(Geometry.class, geom.type);
		assertTrue(geom.required);
		assertEquals(2, geom.minOccurs);
		assertEquals(Integer.MAX_VALUE, geom.maxOccurs);
	}

	public void testExecuteUnion() throws Exception {
		org.geotools.process.Process union = factory.create(new NameImpl("JTS", "union"));
		
		// try less than the required params
		Map<String, Object> inputs = new HashMap<String, Object>();
		try {
			union.execute(inputs, null);
			fail("What!!! Should have failed big time!");
		} catch(ProcessException e) {
			// fine
		}
		
		// try again with less
		Geometry geom1 = new WKTReader().read("POLYGON((0 0, 0 1, 1 1, 1 0, 0 0))");
		Geometry geom2 = new WKTReader().read("POLYGON((0 1, 0 2, 1 2, 1 1, 0 1))");
		List<Geometry> geometries = new ArrayList<Geometry>();
		geometries.add(geom1);
		inputs.put("geom", geometries);
		try {
			union.execute(inputs, null);
			fail("What!!! Should have failed big time!");
		} catch(ProcessException e) {
			// fine
		}
		
		// now with just enough
		geometries.add(geom2);
		Map<String, Object> result = union.execute(inputs, null);
		
		assertEquals(1, result.size());
		Geometry united = (Geometry) result.get("result");
		assertNotNull(united);
		assertTrue(united.equals(geom1.union(geom2)));
	}
	
	public void testExecuteHull() throws Exception {
		NameImpl hullName = new NameImpl("JTS", "convexHull");
		org.geotools.process.Process hull = factory.create(hullName);
		
		Map<String, Object> inputs = new HashMap<String, Object>();
		Geometry geom = new WKTReader().read("LINESTRING(0 0, 0 1, 1 1)");
		inputs.put("geom", geom);
		Map<String, Object> output = hull.execute(inputs, null);
		
		assertEquals(1, output.size());
		// there is no output annotation, check there is consistency between what is declared
		// and what is returned
		Geometry result = (Geometry) output.get(factory.getResultInfo(hullName, null).keySet().iterator().next());
		assertTrue(result.equals(geom.convexHull()));
	}
}
