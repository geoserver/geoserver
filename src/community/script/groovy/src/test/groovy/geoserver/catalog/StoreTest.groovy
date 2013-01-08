package geoserver.catalog;

import static org.junit.Assert.*;

import geoserver.GeoServer
import org.geoserver.test.GeoServerTestSupport;

class StoreTest extends GeoServerTestSupport {

	void testConstructor() {
		
		// Get Store by name
		def store = new Store("cite")
		assertNotNull(store.dataStoreInfo)
		assertNotNull(store.workspace)
		assertNotNull(store.catalog)
		
		// Get Store by DataStoreInfo
		def cat = new Catalog()
		def wk = cat.get("cite")
		def ds = cat.catalog.getDataStoreByName(wk.workspace, "cite")
		store = new Store(ds, wk, cat)
		assertNotNull(store.dataStoreInfo)
		assertNotNull(store.workspace)
		assertNotNull(store.catalog)
		
		// Get Store by just DataStoreInfo
		store = new Store(ds)
		assertNotNull(store.dataStoreInfo)
		assertNotNull(store.workspace)
		assertNotNull(store.catalog)
	}
	
	void testGetName() {
		def store = new Store("cite")
		assertEquals("cite", store.name)
	}
	
	void testGetGeoScriptWorkspace() {
		def store = new Store("cite")
		def workspace = store.geoScriptWorkspace
		assertNotNull(workspace)
		assertTrue(workspace instanceof geoscript.workspace.Workspace)
		println "${workspace.format} ${workspace.names}"
	}
	
	void testGetLayerNames() {
		def store = new Store("cite")
		def layers = store.layerNames
		assertNotNull(layers)
		assertTrue(layers.size() > 0)
		assertTrue(layers.contains("Lakes"))
	}
	
	void testGetLayers() {
		def store = new Store("cite")
		def layers = store.layers
		assertNotNull(layers)
		assertTrue(layers.size() > 0)
	}
	
	void testToString() {
		def store = new Store("cite")
		assertEquals("cite", store.toString())
	}
}
