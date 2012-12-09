package geoserver.catalog;

import static org.junit.Assert.*;

import geoserver.GeoServer
import org.geoserver.test.GeoServerTestSupport;

class CatalogTest extends GeoServerTestSupport {

	void testConstructor() {
		def cat = new Catalog()
		assertNotNull(cat.catalog)
		
		def gs = new GeoServer()
		cat = gs.catalog
		assertNotNull(cat.catalog)
	}
	
	void testGetWorkspaceNames() {
		def cat = new Catalog()
		def workspaces = cat.workspaceNames
		assertTrue(workspaces.size() > 0)
		assertTrue(workspaces.contains("cite"))
	}
	
	void testGet() {
		def cat = new Catalog()
		// By name
		def workspace = cat.get("cite")
		assertNotNull(workspace)
		// Default
		workspace = cat.get()
		assertNotNull(workspace)
	}
	
	void testGetAt() {
		def cat = new Catalog()
		def workspace = cat["cite"]
		assertNotNull(workspace)
	}
	
	void testGetStoreNames() {
		def cat = new Catalog()
		def storeNames = cat.storeNames
		assertTrue(storeNames.size() > 0)
		assertTrue(storeNames.contains("cite"))
	}
	
	void testGetStore() {
		def cat = new Catalog()
		def store = cat.getStore("cite")
		assertNotNull(store)
	}
	
	void testGetLayerNames() {
		def cat = new Catalog()
		def layerNames = cat.layerNames
		assertTrue(layerNames.size() > 0)
		assertTrue(layerNames.contains("Lines"))
	}
	
	void testGetLayer() {
		def cat = new Catalog()
		def layer = cat.getLayer("Lines")
		assertNotNull(layer)
	}
}
