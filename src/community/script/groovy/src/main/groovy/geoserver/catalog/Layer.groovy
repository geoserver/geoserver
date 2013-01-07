package geoserver.catalog

import geoscript.layer.Layer as GeoScriptLayer
import org.geoserver.catalog.LayerInfo
import org.geoserver.catalog.ResourceInfo
import org.geoserver.catalog.StoreInfo
import org.geotools.feature.NameImpl

/**
 * A GeoServer Layer allows access to GeoScript Layers
 * @author Jared Erickson
 */
class Layer {
	
	/**
	 * The Catalog this Layer belongs to
	 */
	Catalog catalog
	
	/**
	 * The Store this Layer belongs to
	 */
	Store store
	
	/**
	 * The GeoServer ResourceInfo / really a FeatureTypeInfo
	 */
	ResourceInfo resourceInfo
	
	/**
	 * Create a new Layer from a GeoServer ResourceInfo/LayerInfo or by name.  The Store is optional.
	 * @param layer The GeoServer ResourceInfo/LayerInfo or name
	 * @param store The optional Store
	 */
	Layer(def layer, def store = null) {
		
		// If the Store is available get the
		// Catalog from it, otherwise create
		// a new default Catalog
		if (store instanceof Store) {
			catalog = store.catalog
		} else {
			catalog = new Catalog()
		}
		
		// Get the Layer from a GeoServer ResourceInfo
		if (layer instanceof ResourceInfo) {
			resourceInfo = layer as ResourceInfo
		} 
		// Get the Layer from a GeoServer LayerInfo
		else if (layer instanceof LayerInfo) {
			resourceInfo = (layer as LayerInfo).resource
		} 
		// Get the Layer by name
		else if (layer instanceof String) {
			// Get Layer from the Store by name
			if (store && store instanceof String) {
				StoreInfo s = catalog.catalog.getStoreByName(store, StoreInfo.class)
				resourceInfo = catalog.catalog.getResourceByStore(s, layer, ResourceInfo.class)
			} 
			// Get Layer from the Store by name
			else if (store && store instanceof Store) {
				resourceInfo = catalog.catalog.getResourceByStore(store.dataStoreInfo, layer, ResourceInfo.class)
			} 
			// Get the Layer from the Catalog by name
			else {
				def l = catalog.catalog.getLayerByName(layer)
				if (l) {
					resourceInfo = l.resource
				}
			}
		}
		
		// If the GeoServer ResourceInfo is available
		// get the Store from it
		if (resourceInfo) {
			this.store = new Store(resourceInfo.store)
		}
		
		// If we still don't have the Store, create it by name
		// or throw an Exception
		if (!this.store) {
			if (store instanceof String) {
				this.store = new Store(store as String)
			}
			if (!this.store) {
				throw new Exception("Unable to find store for layer ${layer}")
			}
		}
		
		// If we still haven't found the GeoServer ResourceInfo
		if (!resourceInfo) {
			// look for it by name 
			if (layer instanceof String) {
				def ft = this.catalog.catalog.getFeatureTypeByDataStore(store.info, layer)
				if (!ft) {
					def b = catalog.builder()
					b.store = store.info
					ft = b.buildFeatureType(new NameImpl(layer))
				}
				this.resourceInfo = ft
			} 
			// or throw an Exception
			else {
				throw new Exception("Unable to create layer from ${layer}")
			}
		}
	}
	
	/**
	 * Get the GeoScript Layer for this Layer
	 * @return A GeoScript Layer
	 */
	GeoScriptLayer getGeoScriptLayer() {
		def fs = resourceInfo.getFeatureSource(null, null)
		new GeoScriptLayer(fs)
	}
	
	/**
	 * Get the name
	 * @return The name
	 */
	String getName() {
		resourceInfo.name
	}
	
	@Override
	String toString() {
		resourceInfo.name
	}
}