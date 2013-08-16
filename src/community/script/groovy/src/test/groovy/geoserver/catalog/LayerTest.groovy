package geoserver.catalog;

import static org.junit.Assert.*;

import geoserver.GeoServer
import org.geoserver.test.GeoServerTestSupport;

class LayerTest extends GeoServerTestSupport {

	void testConstructor() {
		
		// Get Layer by Name
		def layer = new Layer("BasicPolygons")
		assertNotNull(layer.resourceInfo)
		assertNotNull(layer.catalog)
		assertNotNull(layer.store)
		
		// Get Layer by Name and Store
		def store = new Store("cite")
		layer = new Layer("BasicPolygons", store)
		assertNotNull(layer.resourceInfo)
		println layer.resourceInfo
		assertNotNull(layer.catalog)
		assertNotNull(layer.store)
		
		// Get Layer by ResourceInfo
		layer = new Layer(layer.resourceInfo)
		assertNotNull(layer.resourceInfo)
		assertNotNull(layer.catalog)
		assertNotNull(layer.store)
	}
	
	void testGetGeoScriptLayer() {
		def layer = new Layer("BasicPolygons")
		def gsLayer = layer.geoScriptLayer
		assertNotNull(gsLayer)
		assertTrue(gsLayer instanceof geoscript.layer.Layer)
	}
	
	void testGetName() {
		def layer = new Layer("BasicPolygons")
		assertEquals("BasicPolygons", layer.name)
	}
	
	void testToString() {
		def layer = new Layer("BasicPolygons")
		assertEquals("BasicPolygons", layer.toString())
	}
}
