package geoserver.catalog;

import static org.junit.Assert.*;

import geoserver.GeoServer
import org.geoserver.test.GeoServerTestSupport;

class WorkspaceTest extends GeoServerTestSupport {

	void testConstructor() {
		
		// Get the default workspace
		def workspace = new Workspace()
		assertNotNull workspace.catalog
		assertNotNull workspace.workspace
		assertNotNull workspace.namespace
		assertEquals("gs:http://geoserver.org", workspace.toString())
		
		// Get a workspace by name
		workspace = new Workspace("gs")
		assertNotNull workspace.catalog
		assertNotNull workspace.workspace
		assertNotNull workspace.namespace
		assertEquals("gs:http://geoserver.org", workspace.toString())
		
		// Get a workspace by WorkspaceInfo
		def cat = new Catalog()
		def workspaceInfo = cat.catalog.getWorkspaceByName("cite")
		workspace = new Workspace(workspaceInfo)
		assertNotNull workspace.catalog
		assertNotNull workspace.workspace
		assertNotNull workspace.namespace
		assertEquals("cite:http://www.opengis.net/cite", workspace.toString())
		
		// Get a workspace by WorkspaceInfo and Catalog
		workspace = new Workspace(workspaceInfo, cat)
		assertNotNull workspace.catalog
		assertNotNull workspace.workspace
		assertNotNull workspace.namespace
		assertEquals("cite:http://www.opengis.net/cite", workspace.toString())
	}
	
	void testGetName() {
		def workspace = new Workspace("gs")
		assertEquals("gs", workspace.name)
	}
	
	void testGetUri() {
		def workspace = new Workspace("gs")
		assertEquals("http://geoserver.org", workspace.uri)
	}
	
	void testToString() {
		def workspace = new Workspace("gs")
		assertEquals("gs:http://geoserver.org", workspace.toString())
	}
	
	void testGetStoreNames() {
		def workspace = new Workspace("cite")
		def stores = workspace.storeNames
		assertTrue(stores.size() > 0)
	}
	
	void testGetStores() {
		def workspace = new Workspace("cite")
		def stores = workspace.stores
		assertTrue(stores.size() > 0)
	}
	
	void testGetStore() {
		def workspace = new Workspace("cite")
		def stores = workspace.storeNames
		assertTrue(stores.size() > 0)
		def storeName = stores.get(0)
		
		def store = workspace.get(storeName)
		assertNotNull(store)
		store = workspace[storeName]
		assertNotNull(store)
	}
}
