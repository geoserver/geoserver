package geoserver.catalog

import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import geoserver.catalog.Store

/**
 * A GeoServer Workspace/Namespace
 * @author Jared Erickson
 */
class Workspace {
	
	/**
	 * The GeoServer Catalog
	 */
	Catalog catalog
	
	/**
	 * The GeoServer WorkspaceInfo
	 */
	WorkspaceInfo workspace
	
	/**
	 * The GeoServer NamespaceInfo
	 */
	NamespaceInfo namespace = null
	
	/**
	 * Create a new Workspace with a WorkspaceInfo or name and/or a
	 * Catalog.
	 * @param workspace The GeoServer WorkspaceInfo or a name
	 * @param catalog The Catalog or null if default
	 */
	Workspace(def workspace = null, def catalog = null) {
		
		// If a Catalog was not passed in get the default Catalog
		if (!catalog) {
			catalog = new Catalog()
		}
		this.catalog = catalog
		
		// The workspace can be an existing WorkspaceInfo
		if (workspace instanceof WorkspaceInfo) {
			this.workspace = workspace as WorkspaceInfo
		} 
		// Or it can be a name
		else if (workspace instanceof String) {
			// So let's try looking up an existing Workspace by name
			def ws = catalog.catalog.getWorkspaceByName(workspace as String)
			// If one doesn't exist, let's create a new one
			if (!ws) {
				ws = catalog.catalog.factory.createWorkspace()
				ws.name = workspace as String
				this.namespace = catalog.catalog.factory.createNamespace()
				this.namespace.prefix = workspace as String
				this.namespace.URI = workspace as String
			}
			this.workspace = ws 
		} else {
			// If the given workspace is falsey, just get the default Workspace
			if (!workspace) {
				this.workspace = catalog.catalog.defaultWorkspace
			} else {
				this.workspace = workspace
			}
		}
		
		// Finally, get the namespace by the workspace name, if we haven't
		// already set it
		if (!this.namespace) {
			this.namespace = catalog.catalog.getNamespaceByPrefix(this.workspace.name)
		}
	}
	
	/**
	 * Get the name or prefix
	 * @return The name or prefix
	 */
	String getName() {
		namespace.name
	}
	
	/**
	 * Get the namespace URI
	 * @return The namespace URI
	 */
	String getUri() {
		namespace.URI
	}
	
	/**
	 * Get a List of Store names
	 * @return A List of Store names
	 */
	List<String> getStoreNames() {
		this.catalog.catalog.getDataStoresByWorkspace(this.workspace).collect { ds ->
			ds.name
		}
	}
	
	/**
	 * Get a List of Stores
	 * @return A List of Stores
	 */
	List<String> getStores() {
		this.catalog.catalog.getDataStoresByWorkspace(this.workspace).collect { ds -> 
			new Store(ds, this, this.catalog)
		}
	}
	
	/**
	 * Get a Store by name
	 * @param name The name of the Store
	 * @return A Store
	 */
	Store get(String name) {
		def ds = this.catalog.catalog.getDataStoreByName(this.workspace, name)
		if (ds) {
			return new Store(ds, this)
		} else {
			throw new IllegalArgumentException("${this.toString()} doesn't contain a Store named ${name}!")
		}
	}
	
	/**
	 * Get a Store by name
	 * @param name The name of the Store
	 * @return A Store
	 */
	Store getAt(String name) {
		get(name)
	}
	
	@Override
	String toString() {
		"${name}:${uri}"
	}
}