package geoserver.catalog

import geoscript.workspace.Workspace as GeoScriptWorkspace
import org.geoserver.catalog.ResourceInfo
import org.geoserver.catalog.DataStoreInfo;

/**
 * The GeoServer Store allows access to Layers
 * @author Jared Erickson
 */
class Store {
	
	/**
	 * The Catalog
	 */
	Catalog catalog
	
	/**
	 * The Workspace
	 */
	Workspace workspace
	
	/**
	 * The DataStoreInfo
	 */
	DataStoreInfo dataStoreInfo
	
	/**
	 * Create a new Store
	 * @param store
	 * @param workspace
	 * @param catalog
	 */
	Store(def store, def workspace = null, def catalog = null) {
		
		// If we didn't get a Catalog use the default Catalog
		if (!catalog) {
			catalog = new Catalog()
		}
		this.catalog = catalog
		
		// If the Store is a GeoServer DataStoreInfo
		// use it to get the Store and Workspace
		if (store instanceof DataStoreInfo) {
			this.dataStoreInfo = store as DataStoreInfo
			this.workspace = new Workspace(dataStoreInfo.workspace)
		}
		
		// If the Workspace is not already set
		if (!this.workspace) {
			// create a new Workspace by name
			if (workspace instanceof String) {
				workspace = new Workspace(workspace as String)
			}
			// or create a new Workspace
			if (!workspace) {
				workspace = new Workspace()
			}
			this.workspace = workspace
		}
		
		// If the GeoServer DataStoreInfo is not set
		if (!this.dataStoreInfo) {
			// try looking it up by name
			if (store instanceof String) {
				def ds = catalog.catalog.getDataStoreByName(store)
				if (!ds) {
					ds = catalog.catalog.factory.createDataStore()
					if (ds) {
						ds.name = store as String
						if (this.workspace) {
							ds.workspace = this.workspace.workspace
						} else {
							ds.workspace = catalog.catalog.defaultWorkspace
						}
					}
				}
				
				this.dataStoreInfo = ds
			} else {
				throw IllegalArgumentException("Unable to create store from ${store}")
			}
		}
	}
	
	/**
	 * Get the name
	 * @return The name
	 */
	String getName() {
		dataStoreInfo.name
	}
	
	/**
	 * Get the backing GeoScript Workspace
	 * @return The backing GeoScript Workspace
	 */
	GeoScriptWorkspace getGeoScriptWorkspace() {
		new GeoScriptWorkspace(dataStoreInfo.getDataStore(null))
	}
	
	/**
	 * Get the names of the Layers in this Store
	 * @return The names of the Layers
	 */
	List<Layer> getLayerNames() {
		def featureTypes = catalog.catalog.getFeatureTypesByDataStore(dataStoreInfo)
		featureTypes.collect{ft -> ft.name}
	}
	
	/**
	 * Get the List of Layers in this Store
	 * @return The List of Layers in this Store
	 */
	List<Layer> getLayers() {
		def featureTypes = catalog.catalog.getFeatureTypesByDataStore(dataStoreInfo)
		featureTypes.collect{ft -> new Layer(ft, this)}
	}
	
	/**
	 * Get the geoscript.catalog.Layer by name
	 * @param name The name of the Layer
	 * @return A geoscript.catalog.Layer
	 */
	Layer get(String name) {
		def layer = catalog.catalog.getFeatureTypeByDataStore(dataStoreInfo, name)
		if (layer) {
			return new Layer(layer, this)
		} else {
			return null
		}
	}
	
	/**
	 * Get the geoscript.catalog.Layer by name
	 * @param name The name of the Layer
	 * @return A geoscript.catalog.Layer
	 */
	Layer getAt(String name) {
		get(name)
	}
	
	@Override
	String toString() {
		dataStoreInfo.name 
	}
}