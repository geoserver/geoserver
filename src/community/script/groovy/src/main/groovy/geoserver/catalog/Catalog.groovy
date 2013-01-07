package geoserver.catalog

import org.geoserver.catalog.Catalog as GSCatalog
import org.geoserver.platform.GeoServerExtensions;

/**
 * The GeoServer Catalog allows access to Workspaces
 * @author Jared Erickson
 */
class Catalog {
	
	/**
	 * The org.geoserver.catalog.Catalog instance
	 */
	GSCatalog catalog
	
	/**
	 * Create a new Catalog 
	 */
	Catalog() {
		this.catalog = GeoServerExtensions.bean("catalog") as GSCatalog
	}
	
	/**
	 * Get the names of Workspaces in this Catalog
	 * @return The names of the Workspaces
	 */
	List<String> getWorkspaceNames() {
		this.catalog.workspaces.collect{ ws -> ws.name }
	}
	
	/**
	 * Get a List of all Workspaces in this Catalog
	 * @return A List of all Workspaces
	 */
	List<Workspace> getWorkspaces() {
		this.catalog.workspaces.collect{ ws -> new Workspace(ws, this)}
	}
	
	/**
	 * Get a Workspace by name
	 * @param key The name
	 * @return The Workspace
	 */
	Workspace get(String key = null) {
		if (!key) {
			key = this.catalog.defaultNamespace.name
		}
		def ws = this.catalog.getWorkspaceByName(key)
		if (!ws) {
			throw new IllegalArgumentException("No such Workspace ${key}")
		}
		new Workspace(ws)
	}
	
	/**
	 * Get a Workspace by name
	 * @param key The name
	 * @return The Workspace
	 */
	Workspace getAt(String key) {
		get(key)
	}
	
	/**
	 * Get a List of all Store names in this Catalog
	 * @return A List of all Store names
	 */
	List<String> getStoreNames() {
		this.catalog.dataStores*.name
	}
	
	/**
	 * Get a Store by name
	 * @param name The name of the Store
	 * @return A Store
	 */
	Store getStore(String name) {
		new Store(this.catalog.getDataStoreByName(name))
	}
	
	/**
	 * Get a List of all Layer names in this Catalog
	 * @return A List of all Layer names
	 */
	List<String> getLayerNames() {
		this.catalog.layers*.name
	}
	
	/**
	 * Get a Layer by name
	 * @param name The layer name
	 * @return A Layer
	 */
	Layer getLayer(String name) {
		new Layer(this.catalog.getLayerByName(name))
	}
}